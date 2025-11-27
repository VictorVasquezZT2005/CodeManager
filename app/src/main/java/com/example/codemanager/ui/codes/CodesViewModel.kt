package com.example.codemanager.ui.codes

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.codemanager.data.model.Code
import com.example.codemanager.data.model.Category
import com.example.codemanager.data.model.Warehouse
import com.example.codemanager.data.repository.CodeRepository
import com.example.codemanager.utils.CsvUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// 1. DEFINICIÓN DE CODETYPE
enum class CodeType(val label: String, val prefix: String, val isComposite: Boolean) {
    EMERGENCY("Emergencia", "62", false),
    SERVICES("Servicios", "70", false),
    MEDICINES("Medicamentos", "00", true),
    DISPOSABLES("Descartables", "01", true)
}

class CodesViewModel(private val codeRepository: CodeRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(CodesUiState())
    val uiState = _uiState.asStateFlow()

    private val _selectedType = MutableStateFlow(CodeType.EMERGENCY)
    val selectedType = _selectedType.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    private val _warehouses = MutableStateFlow<List<Warehouse>>(emptyList())

    private val _warehouseTypeFilter = MutableStateFlow("estante")
    val warehouseTypeFilter = _warehouseTypeFilter.asStateFlow()

    private val _selectedCategory = MutableStateFlow<Category?>(null)
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    val filteredCategoriesForSelection = _categories.combine(_selectedType) { all, type ->
        val filterKey = if (type == CodeType.MEDICINES) "MED" else if (type == CodeType.DISPOSABLES) "DESC" else ""
        if (filterKey.isNotEmpty()) all.filter { it.type == filterKey } else emptyList()
    }.asStateFlow(viewModelScope, emptyList<Category>())

    val filteredWarehousesForSelection = _warehouses.combine(_warehouseTypeFilter) { all, type ->
        all.filter { it.type == type }
    }.asStateFlow(viewModelScope, emptyList<Warehouse>())

    init {
        loadCodes()
        loadAuxiliaryData()
    }

    private fun loadAuxiliaryData() {
        viewModelScope.launch {
            _categories.value = codeRepository.getCategories()
            _warehouses.value = codeRepository.getWarehouses()
        }
    }

    fun loadCodes() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            codeRepository.loadCodes()
            codeRepository.codes.collect { codes ->
                _uiState.value = _uiState.value.copy(codes = codes, isLoading = false)
                filterCodes()
            }
        }
    }

    fun selectType(type: CodeType) {
        _selectedType.value = type
        _selectedCategory.value = null
        filterCodes()
    }

    private fun filterCodes() {
        val currentType = _selectedType.value
        val allCodes = _uiState.value.codes

        val filtered = allCodes.filter { code ->
            when (currentType) {
                CodeType.EMERGENCY -> code.prefix == "62" || code.rootPrefix == "62" || code.code.startsWith("62")
                CodeType.SERVICES -> code.prefix == "70" || code.rootPrefix == "70" || code.code.startsWith("70")
                CodeType.MEDICINES -> code.rootPrefix == "00" || code.prefix == "MED"
                CodeType.DISPOSABLES -> code.rootPrefix == "01" || code.prefix == "DESC"
            }
        }
        _uiState.value = _uiState.value.copy(filteredCodes = filtered)
    }

    fun setWarehouseTypeFilter(type: String) { _warehouseTypeFilter.value = type }
    fun setSelectedCategory(category: Category) { _selectedCategory.value = category }

    fun generateCode(description: String, createdBy: String, category: Category?, warehouseCodeInput: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val type = _selectedType.value
            val cleanDesc = description.trim()
            val cleanWarehouseCode = warehouseCodeInput.trim().uppercase()

            if (cleanDesc.isEmpty()) {
                _message.value = "Error: La descripción es obligatoria."
                _uiState.value = _uiState.value.copy(isLoading = false)
                return@launch
            }

            val upperDescription = cleanDesc.uppercase()
            val finalDescription = if (type == CodeType.EMERGENCY) "// $upperDescription" else upperDescription

            val result = if (type.isComposite) {
                if (category == null) {
                    Result.failure(Exception("Debes seleccionar una categoría."))
                } else if (cleanWarehouseCode.isEmpty()) {
                    Result.failure(Exception("Debes escribir el código del almacén."))
                } else {
                    val foundWarehouse = _warehouses.value.find { it.code == cleanWarehouseCode }
                    if (foundWarehouse != null) {
                        val internalPrefix = if (type == CodeType.MEDICINES) "MED" else "DESC"
                        codeRepository.generateCompositeCode(
                            rootPrefix = type.prefix,
                            category = category,
                            warehouse = foundWarehouse,
                            description = finalDescription,
                            createdBy = createdBy,
                            internalPrefix = internalPrefix
                        )
                    } else {
                        Result.failure(Exception("El almacén con código '$cleanWarehouseCode' no existe."))
                    }
                }
            } else {
                codeRepository.generateStandardCode(type.prefix, finalDescription, createdBy)
            }

            _uiState.value = _uiState.value.copy(isLoading = false)

            if (result.isSuccess) {
                _message.value = "Código Creado: ${result.getOrNull()?.code}"
            } else {
                _message.value = "Error: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun updateCode(code: Code) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                codeRepository.updateCode(code)
                codeRepository.loadCodes()
                _message.value = "Código actualizado correctamente"
            } catch (e: Exception) {
                _message.value = "Error al actualizar: ${e.message}"
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
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
                withContext(Dispatchers.Main) { _message.value = "Exportado exitosamente" }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { _message.value = "Error exportando: ${e.message}" }
            }
        }
    }

    // --- IMPORTAR DATOS (CON VALIDACIÓN DE CATEGORÍA EXISTENTE) ---
    fun importData(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { _uiState.value = _uiState.value.copy(isLoading = true) }
            try {
                val parsedCodes = CsvUtils.parseCodesFromCsv(context, uri)

                // 1. Cargamos códigos existentes para chequear duplicados
                val existingCodeStrings = _uiState.value.codes.map { it.code }.toHashSet()

                // 2. Cargamos los códigos de las CATEGORÍAS VÁLIDAS de la base de datos
                // Esto nos permite saber si "05", "10", etc. existen realmente.
                val validCategoryCodes = _categories.value.map { it.code }.toHashSet()

                var importedCount = 0
                var duplicateCount = 0
                var unknownCategoryCount = 0 // Nuevo contador para errores de categoría

                val maxSequences = mutableMapOf<String, Int>()

                parsedCodes.forEach { code ->
                    // A. Validación: ¿Ya existe el código?
                    if (existingCodeStrings.contains(code.code)) {
                        duplicateCount++
                        return@forEach
                    }

                    // B. Validación: ¿Existe la categoría en BD?
                    // Solo aplica si el código tiene una categoría asignada (no está vacía)
                    if (code.categoryCode.isNotEmpty() && !validCategoryCodes.contains(code.categoryCode)) {
                        unknownCategoryCount++
                        return@forEach // Saltamos este código porque su categoría no existe
                    }

                    // Si pasa las validaciones, guardamos
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
                codeRepository.loadCodes()

                withContext(Dispatchers.Main) {
                    // Construimos un mensaje detallado
                    val parts = mutableListOf("Importados: $importedCount")
                    if (duplicateCount > 0) parts.add("Duplicados omitidos: $duplicateCount")
                    if (unknownCategoryCount > 0) parts.add("Categorías desconocidas omitidas: $unknownCategoryCount")

                    _message.value = parts.joinToString(". ")
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    _message.value = "Error importando: ${e.message}"
                }
            }
        }
    }

    fun deleteCode(id: String) {
        viewModelScope.launch { codeRepository.deleteCode(id) }
    }

    fun clearMessage() { _message.value = null }
}

fun <T1, T2, R> Flow<T1>.combine(flow: Flow<T2>, transform: suspend (T1, T2) -> R): Flow<R> = kotlinx.coroutines.flow.combine(this, flow, transform)

fun <T> Flow<T>.asStateFlow(scope: CoroutineScope, initialValue: T): StateFlow<T> = this.stateIn(scope, SharingStarted.WhileSubscribed(5000), initialValue)

data class CodesUiState(
    val codes: List<Code> = emptyList(),
    val filteredCodes: List<Code> = emptyList(),
    val isLoading: Boolean = false
)

class CodesViewModelFactory(private val repository: CodeRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CodesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CodesViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}