package com.example.codemanager.ui.codes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.codemanager.data.model.Code
import com.example.codemanager.data.model.TherapeuticGroup
import com.example.codemanager.data.model.Warehouse
import com.example.codemanager.data.repository.CodeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// --- CONFIGURACIÓN DE TIPOS Y PREFIJOS ---
enum class CodeType(val label: String, val prefix: String, val isComposite: Boolean) {
    EMERGENCY("Emergencia", "62", false),
    SERVICES("Servicios", "70", false),
    MEDICINES("Medicamentos", "00", true),  // Raíz 00
    DISPOSABLES("Descartables", "01", true) // Raíz 01 (CORREGIDO)
}

class CodesViewModel(private val codeRepository: CodeRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(CodesUiState())
    val uiState = _uiState.asStateFlow()

    // Selección de pestaña
    private val _selectedType = MutableStateFlow(CodeType.EMERGENCY)
    val selectedType = _selectedType.asStateFlow()

    // Datos Auxiliares
    private val _groups = MutableStateFlow<List<TherapeuticGroup>>(emptyList())
    val groups = _groups.asStateFlow()

    private val _warehouses = MutableStateFlow<List<Warehouse>>(emptyList())
    // Filtro para el tipo de almacén (estante vs refrigerador)
    private val _warehouseTypeFilter = MutableStateFlow("estante")
    val warehouseTypeFilter = _warehouseTypeFilter.asStateFlow()

    // Lista de almacenes filtrada dinámicamente para el Dropdown
    val filteredWarehousesForSelection = _warehouses.combine(_warehouseTypeFilter) { all, type ->
        all.filter { it.type == type }
    }.asStateFlow(viewModelScope, emptyList())

    // Selecciones del usuario
    private val _selectedGroup = MutableStateFlow<TherapeuticGroup?>(null)
    val selectedGroup = _selectedGroup.asStateFlow()

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
            _groups.value = codeRepository.getGroups()
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
        // Resetear selecciones al cambiar de pestaña
        _selectedGroup.value = null
        _selectedWarehouse.value = null
        filterCodes()
    }

    private fun filterCodes() {
        val currentType = _selectedType.value
        val allCodes = _uiState.value.codes
        val filtered = allCodes.filter { code ->
            when (currentType) {
                CodeType.EMERGENCY -> code.prefix == "62"
                CodeType.SERVICES -> code.prefix == "70"
                CodeType.MEDICINES -> code.prefix == "MED"
                CodeType.DISPOSABLES -> code.prefix == "DESC"
            }
        }
        _uiState.value = _uiState.value.copy(filteredCodes = filtered)
    }

    fun setWarehouseTypeFilter(type: String) {
        _warehouseTypeFilter.value = type
        _selectedWarehouse.value = null
    }

    fun setSelectedGroup(group: TherapeuticGroup) { _selectedGroup.value = group }
    fun setSelectedWarehouse(warehouse: Warehouse) { _selectedWarehouse.value = warehouse }

    fun generateCode(description: String, createdBy: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val type = _selectedType.value

            val result = if (type.isComposite) {
                // Lógica para 00 (Med) y 01 (Desc)
                val group = _selectedGroup.value
                val warehouse = _selectedWarehouse.value

                if (group != null && warehouse != null) {
                    // Prefijo interno para la base de datos (para filtrar después)
                    val internalPrefix = if (type == CodeType.MEDICINES) "MED" else "DESC"

                    codeRepository.generateCompositeCode(
                        rootPrefix = type.prefix, // Aquí pasa "00" o "01"
                        group = group,
                        warehouse = warehouse,
                        description = description,
                        createdBy = createdBy,
                        internalPrefix = internalPrefix
                    )
                } else {
                    Result.failure(Exception("Faltan datos"))
                }
            } else {
                // Lógica para 62 y 70
                codeRepository.generateStandardCode(type.prefix, description, createdBy)
            }

            _uiState.value = _uiState.value.copy(isLoading = false)

            if (result.isSuccess) {
                _message.value = "Código Creado: ${result.getOrNull()?.code}"
            } else {
                _message.value = "Error: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun deleteCode(id: String) {
        viewModelScope.launch { codeRepository.deleteCode(id) }
    }

    fun clearMessage() { _message.value = null }
}

// --- EXTENSION FUNCTIONS PARA ARREGLAR ERRORES DE STATEFLOW ---

// Permite usar .combine como función de extensión
fun <T1, T2, R> Flow<T1>.combine(
    flow: Flow<T2>,
    transform: suspend (T1, T2) -> R
): Flow<R> = kotlinx.coroutines.flow.combine(this, flow, transform)

// Permite convertir cualquier Flow a StateFlow fácilmente
fun <T> Flow<T>.asStateFlow(
    scope: CoroutineScope,
    initialValue: T
): StateFlow<T> = this.stateIn(
    scope,
    SharingStarted.WhileSubscribed(5000),
    initialValue
)

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