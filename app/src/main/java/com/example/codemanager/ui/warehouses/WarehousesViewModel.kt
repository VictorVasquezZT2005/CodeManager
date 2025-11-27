package com.example.codemanager.ui.warehouses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.codemanager.data.model.Warehouse
import com.example.codemanager.data.repository.WarehouseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class WarehousesUiState(
    val warehouses: List<Warehouse> = emptyList(),
    val filteredWarehouses: List<Warehouse> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedType: String = "estante",
    val showAddDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val selectedWarehouse: Warehouse? = null,
    val nextAvailableLocation: Pair<Int, Int>? = null
)

class WarehousesViewModel(
    private val repository: WarehouseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WarehousesUiState())
    val uiState: StateFlow<WarehousesUiState> = _uiState.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        loadWarehouses()
    }

    fun loadWarehouses() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            repository.getAllWarehouses().fold(
                onSuccess = { warehouses ->
                    _uiState.value = _uiState.value.copy(
                        warehouses = warehouses,
                        isLoading = false
                    )
                    applyFiltersAndUpdateState(warehouses)
                    calculateNextAvailableLocation()
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Error al cargar almacenes: ${exception.message}"
                    )
                }
            )
        }
    }

    // Esta función recibe el objeto ya listo, pero por seguridad,
    // la conversión principal se hace en generateNewWarehouse
    fun createWarehouse(warehouse: Warehouse) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            repository.createWarehouse(warehouse).fold(
                onSuccess = {
                    _message.value = "${Warehouse.getTypeDisplayName(warehouse.type)} creado exitosamente"
                    loadWarehouses()
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message
                    )
                }
            )
        }
    }

    // --- CAMBIO 1: ACTUALIZAR (Nombre a Mayúsculas) ---
    fun updateWarehouse(warehouseId: String, warehouse: Warehouse) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            // Aseguramos mayúsculas antes de enviar a la BD
            val upperName = warehouse.name.trim().uppercase()
            val warehouseToUpdate = warehouse.copy(name = upperName)

            repository.updateWarehouse(warehouseId, warehouseToUpdate).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        showEditDialog = false,
                        selectedWarehouse = null,
                        isLoading = false
                    )
                    _message.value = "${Warehouse.getTypeDisplayName(warehouse.type)} actualizado exitosamente"
                    loadWarehouses()
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message
                    )
                }
            )
        }
    }

    fun deleteWarehouse(warehouseId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            repository.deleteWarehouse(warehouseId).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    _message.value = "Almacén eliminado exitosamente"
                    loadWarehouses()
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Error al eliminar: ${exception.message}"
                    )
                }
            )
        }
    }

    // --- CAMBIO 2: GENERAR NUEVO (Nombre a Mayúsculas) ---
    fun generateNewWarehouse(name: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            // Convertimos el nombre entrante a mayúsculas
            val upperName = name.trim().uppercase()

            val currentType = _uiState.value.selectedType
            val nextLocation = _uiState.value.nextAvailableLocation

            if (nextLocation == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "No hay ubicaciones disponibles para ${Warehouse.getTypeDisplayName(currentType)}"
                )
                return@launch
            }

            val (level, itemNumber) = nextLocation
            val warehouseId = UUID.randomUUID().toString()
            val code = Warehouse.generateCode(level, itemNumber)

            val warehouse = Warehouse(
                id = warehouseId,
                code = code,
                name = upperName, // Usamos el nombre en mayúsculas
                type = currentType,
                levelNumber = level,
                itemNumber = itemNumber,
                createdBy = "Admin"
            )

            createWarehouse(warehouse)
        }
    }

    private fun calculateNextAvailableLocation() {
        val currentType = _uiState.value.selectedType
        val warehousesOfType = _uiState.value.warehouses.filter { it.type == currentType }

        for (itemNumber in 1..Warehouse.MAX_ITEMS_PER_LEVEL) {
            for (level in 1..Warehouse.MAX_LEVELS) {
                val exists = warehousesOfType.any {
                    it.itemNumber == itemNumber && it.levelNumber == level
                }
                if (!exists) {
                    _uiState.value = _uiState.value.copy(
                        nextAvailableLocation = Pair(level, itemNumber)
                    )
                    return
                }
            }
        }
        _uiState.value = _uiState.value.copy(nextAvailableLocation = null)
    }

    fun setSelectedType(type: String) {
        _uiState.value = _uiState.value.copy(selectedType = type)
        applyFiltersAndUpdateState()
        calculateNextAvailableLocation()
    }

    private fun applyFiltersAndUpdateState(warehouses: List<Warehouse>? = null) {
        val warehousesToFilter = warehouses ?: _uiState.value.warehouses
        val filtered = warehousesToFilter.filter { it.type == _uiState.value.selectedType }
        _uiState.value = _uiState.value.copy(filteredWarehouses = filtered)
    }

    fun showAddDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = true)
    }

    fun hideAddDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = false)
    }

    fun showEditDialog(warehouse: Warehouse) {
        _uiState.value = _uiState.value.copy(
            showEditDialog = true,
            selectedWarehouse = warehouse
        )
    }

    fun hideEditDialog() {
        _uiState.value = _uiState.value.copy(
            showEditDialog = false,
            selectedWarehouse = null
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearMessage() {
        _message.value = null
    }
}

class WarehousesViewModelFactory(private val repository: WarehouseRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WarehousesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WarehousesViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}