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
import kotlinx.coroutines.CoroutineScope // <--- ESTE IMPORT FALTABA
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    val filteredCategoriesForSelection = _categories.combine(_selectedType) { all, type ->
        val filterKey = if (type == CodeType.MEDICINES) "MED" else if (type == CodeType.DISPOSABLES) "DESC" else ""
        if (filterKey.isNotEmpty()) all.filter { it.type == filterKey } else emptyList()
    }.asStateFlow(viewModelScope, emptyList())

    private val _warehouses = MutableStateFlow<List<Warehouse>>(emptyList())
    private val _warehouseTypeFilter = MutableStateFlow("estante")
    val warehouseTypeFilter = _warehouseTypeFilter.asStateFlow()

    val filteredWarehousesForSelection = _warehouses.combine(_warehouseTypeFilter) { all, type ->
        all.filter { it.type == type }
    }.asStateFlow(viewModelScope, emptyList())

    private val _selectedCategory = MutableStateFlow<Category?>(null)
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _selectedWarehouse = MutableStateFlow<Warehouse?>(null)
    val selectedWarehouse = _selectedWarehouse.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

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
        _selectedWarehouse.value = null
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

    fun setWarehouseTypeFilter(type: String) {
        _warehouseTypeFilter.value = type
        _selectedWarehouse.value = null
    }

    fun setSelectedCategory(category: Category) { _selectedCategory.value = category }
    fun setSelectedWarehouse(warehouse: Warehouse) { _selectedWarehouse.value = warehouse }

    fun generateCode(description: String, createdBy: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val type = _selectedType.value

            val upperDescription = description.trim().uppercase()
            val finalDescription = if (type == CodeType.EMERGENCY) "// $upperDescription" else upperDescription

            val result = if (type.isComposite) {
                val category = _selectedCategory.value
                val warehouse = _selectedWarehouse.value

                if (category != null && warehouse != null) {
                    val internalPrefix = if (type == CodeType.MEDICINES) "MED" else "DESC"
                    codeRepository.generateCompositeCode(
                        rootPrefix = type.prefix,
                        category = category,
                        warehouse = warehouse,
                        description = finalDescription,
                        createdBy = createdBy,
                        internalPrefix = internalPrefix
                    )
                } else {
                    Result.failure(Exception("Faltan datos"))
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

    fun importData(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { _uiState.value = _uiState.value.copy(isLoading = true) }
            try {
                val codes = CsvUtils.parseCodesFromCsv(context, uri)
                var count = 0
                codes.forEach { code ->
                    codeRepository.importCode(code)
                    count++
                }
                codeRepository.loadCodes()
                withContext(Dispatchers.Main) {
                    _message.value = "Importados $count códigos"
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