package xyz.zt.codemanager.ui.codes

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import xyz.zt.codemanager.data.model.Code
import xyz.zt.codemanager.data.model.Category
import xyz.zt.codemanager.data.model.Warehouse
import xyz.zt.codemanager.data.repository.CodeRepository
import xyz.zt.codemanager.utils.CsvUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// DEFINICIÓN DE CODETYPE
enum class CodeType(val label: String, val prefix: String, val isComposite: Boolean) {
    EMERGENCY("Emergencia", "62", false),
    SERVICES("Servicios", "70", false),
    MEDICINES("Medicamentos", "00", true),
    DISPOSABLES("Descartables", "01", true)
}

data class CodesUiState(
    val codes: List<Code> = emptyList(),
    val filteredCodes: List<Code> = emptyList(),
    val categories: List<Category> = emptyList(),
    val warehouses: List<Warehouse> = emptyList(),
    val isLoading: Boolean = false,
    val selectedType: CodeType = CodeType.EMERGENCY,
    val searchQuery: String = "",
    val filterCategory: Category? = null,
    val warehouseTypeFilter: String = "estante",
    val selectedCategory: Category? = null,
    val message: String? = null
)

class CodesViewModel(private val codeRepository: CodeRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(CodesUiState())
    val uiState = _uiState.asStateFlow()

    // Granular flows for the UI to observe only what's needed
    val isLoading = _uiState.map { it.isLoading }.distinctUntilChanged()
    val filteredCodes = _uiState.map { it.filteredCodes }.distinctUntilChanged()
    val selectedType = _uiState.map { it.selectedType }.distinctUntilChanged()
    val searchQuery = _uiState.map { it.searchQuery }.distinctUntilChanged()
    val filterCategory = _uiState.map { it.filterCategory }.distinctUntilChanged()
    val selectedCategory = _uiState.map { it.selectedCategory }.distinctUntilChanged()
    val warehouseTypeFilter = _uiState.map { it.warehouseTypeFilter }.distinctUntilChanged()
    val message = _uiState.map { it.message }.distinctUntilChanged()

    // Auxiliary flows for combine
    private val _selectedType = MutableStateFlow(CodeType.EMERGENCY)
    private val _searchQuery = MutableStateFlow("")
    private val _filterCategory = MutableStateFlow<Category?>(null)

    val filteredCategoriesForSelection = _uiState.map { state ->
        val filterKey = when (state.selectedType) {
            CodeType.MEDICINES -> "MED"
            CodeType.DISPOSABLES -> "DESC"
            else -> ""
        }
        if (filterKey.isNotEmpty()) state.categories.filter { it.type == filterKey } else emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredWarehousesForSelection = _uiState.map { state ->
        state.warehouses.filter { it.type == state.warehouseTypeFilter }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val categoriesDeferred = async { codeRepository.getCategories() }
            val warehousesDeferred = async { codeRepository.getWarehouses() }
            
            val categories = categoriesDeferred.await()
            val warehouses = warehousesDeferred.await()
            
            _uiState.update { it.copy(categories = categories, warehouses = warehouses) }

            codeRepository.codes.collect { codes ->
                _uiState.update { it.copy(codes = codes, isLoading = false) }
            }
        }

        // Optimized reactive filtering with debounce and Default dispatcher
        combine(
            codeRepository.codes,
            _selectedType,
            _searchQuery.debounce(300),
            _filterCategory
        ) { allCodes, currentType, query, catFilter ->
            // Perform heavy filtering on Default dispatcher
            kotlinx.coroutines.withContext(Dispatchers.Default) {
                filterCodesInternal(allCodes, currentType, query, catFilter)
            }
        }.onEach { filtered ->
            _uiState.update { it.copy(filteredCodes = filtered) }
        }.launchIn(viewModelScope)
    }

    private fun filterCodesInternal(
        allCodes: List<Code>,
        currentType: CodeType,
        query: String,
        catFilter: Category?
    ): List<Code> {
        var filtered = allCodes.filter { code ->
            when (currentType) {
                CodeType.EMERGENCY -> code.prefix == "62" || code.rootPrefix == "62" || code.code.startsWith("62")
                CodeType.SERVICES -> code.prefix == "70" || code.rootPrefix == "70" || code.code.startsWith("70")
                CodeType.MEDICINES -> code.rootPrefix == "00" || code.prefix == "MED"
                CodeType.DISPOSABLES -> code.rootPrefix == "01" || code.prefix == "DESC"
            }
        }

        if (query.isNotEmpty()) {
            val trimmedQuery = query.trim()
            filtered = filtered.filter { code ->
                code.description.contains(trimmedQuery, ignoreCase = true) ||
                        code.code.contains(trimmedQuery, ignoreCase = true)
            }
        }

        if (currentType.isComposite && catFilter != null) {
            filtered = filtered.filter { code ->
                code.categoryCode == catFilter.code
            }
        }
        return filtered
    }

    fun selectType(type: CodeType) {
        _selectedType.value = type
        _filterCategory.value = null
        _searchQuery.value = ""
        _uiState.update { it.copy(
            selectedType = type,
            filterCategory = null,
            searchQuery = "",
            selectedCategory = null
        ) }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onFilterCategoryChanged(category: Category?) {
        _filterCategory.value = category
        _uiState.update { it.copy(filterCategory = category) }
    }

    fun setWarehouseTypeFilter(type: String) {
        _uiState.update { it.copy(warehouseTypeFilter = type) }
    }

    fun setSelectedCategory(category: Category) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun generateCode(description: String, createdBy: String, category: Category?, warehouseCodeInput: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val type = _uiState.value.selectedType
            val cleanDesc = description.trim()
            val cleanWarehouseCode = warehouseCodeInput.trim().uppercase()

            if (cleanDesc.isEmpty()) {
                _uiState.update { it.copy(message = "Error: La descripción es obligatoria.", isLoading = false) }
                return@launch
            }

            val finalDescription = formatDescription(cleanDesc, type)

            val result = if (type.isComposite) {
                generateComposite(type, category, cleanWarehouseCode, finalDescription, createdBy)
            } else {
                codeRepository.generateStandardCode(type.prefix, finalDescription, createdBy)
            }

            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    message = if (result.isSuccess) "Código Creado: ${result.getOrNull()?.code}" else "Error: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }

    private fun formatDescription(description: String, type: CodeType): String {
        val upper = description.uppercase()
        return if (type == CodeType.EMERGENCY) "//$upper" else upper
    }

    private suspend fun generateComposite(
        type: CodeType,
        category: Category?,
        warehouseCode: String,
        description: String,
        createdBy: String
    ): Result<Code> {
        if (category == null) return Result.failure(Exception("Debes seleccionar una categoría."))
        if (warehouseCode.isEmpty()) return Result.failure(Exception("Debes escribir el código del almacén."))

        val foundWarehouse = _uiState.value.warehouses.find { it.code == warehouseCode }
            ?: return Result.failure(Exception("El almacén con código '$warehouseCode' no existe."))

        val internalPrefix = if (type == CodeType.MEDICINES) "MED" else "DESC"
        return codeRepository.generateCompositeCode(
            rootPrefix = type.prefix,
            category = category,
            warehouse = foundWarehouse,
            description = description,
            createdBy = createdBy,
            internalPrefix = internalPrefix
        )
    }

    fun updateCode(code: Code) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                codeRepository.updateCode(code)
                _uiState.update { it.copy(message = "Código actualizado correctamente") }
            } catch (e: Exception) {
                _uiState.update { it.copy(message = "Error al actualizar: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun exportData(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val csvContent = CsvUtils.exportCodesToCsv(_uiState.value.filteredCodes)
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(csvContent.toByteArray())
                }
                _uiState.update { it.copy(message = "Exportado exitosamente") }
            } catch (e: Exception) {
                _uiState.update { it.copy(message = "Error exportando: ${e.message}") }
            }
        }
    }

    fun importData(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val parsedCodes = CsvUtils.parseCodesFromCsv(context, uri)
                val existingCodeStrings = _uiState.value.codes.map { it.code }.toHashSet()
                val validCategoryCodes = _uiState.value.categories.map { it.code }.toHashSet()

                var importedCount = 0
                var duplicateCount = 0
                var unknownCategoryCount = 0

                val maxSequences = mutableMapOf<String, Int>()

                parsedCodes.forEach { code ->
                    if (existingCodeStrings.contains(code.code)) {
                        duplicateCount++
                        return@forEach
                    }

                    if (code.categoryCode.isNotEmpty() && !validCategoryCodes.contains(code.categoryCode)) {
                        unknownCategoryCount++
                        return@forEach
                    }

                    codeRepository.importCode(code)
                    importedCount++
                    existingCodeStrings.add(code.code)

                    val sequenceKey = if (code.categoryCode.isNotEmpty()) {
                        "${code.rootPrefix}-${code.categoryCode}"
                    } else {
                        code.rootPrefix
                    }
                    val currentMax = maxSequences[sequenceKey] ?: 0
                    if (code.sequence > currentMax) {
                        maxSequences[sequenceKey] = code.sequence
                    }
                }

                maxSequences.forEach { (key, maxSeq) ->
                    codeRepository.updateSequenceMax(key, maxSeq)
                }

                val parts = mutableListOf("Importados: $importedCount")
                if (duplicateCount > 0) parts.add("Duplicados omitidos: $duplicateCount")
                if (unknownCategoryCount > 0) parts.add("Categorías desconocidas omitidas: $unknownCategoryCount")

                _uiState.update { it.copy(isLoading = false, message = parts.joinToString(". ")) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, message = "Error importando: ${e.message}") }
            }
        }
    }

    fun deleteCode(id: String) {
        viewModelScope.launch { codeRepository.deleteCode(id) }
    }

    fun clearMessage() { _uiState.update { it.copy(message = null) } }
}

class CodesViewModelFactory(private val repository: CodeRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CodesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CodesViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}