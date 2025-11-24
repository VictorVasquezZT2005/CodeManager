package com.example.codemanager.ui.warehouses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.codemanager.data.model.Warehouse
import com.example.codemanager.data.repository.WarehouseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
    private val repository: WarehouseRepository = WarehouseRepository()
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
                    println("📦 DEBUG: Almacenes cargados: ${warehouses.size}")

                    // Actualizar el estado con todos los almacenes
                    _uiState.value = _uiState.value.copy(
                        warehouses = warehouses,
                        isLoading = false
                    )

                    // Aplicar filtros y calcular ubicación
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

    fun createWarehouse(warehouse: Warehouse) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            repository.createWarehouse(warehouse).fold(
                onSuccess = {
                    _message.value = "${Warehouse.getTypeDisplayName(warehouse.type)} creado exitosamente"
                    loadWarehouses() // Recargar para actualizar lista y siguiente ubicación
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

    fun updateWarehouse(warehouseId: String, warehouse: Warehouse) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            repository.updateWarehouse(warehouseId, warehouse).fold(
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

    fun generateNewWarehouse(name: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

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

            val warehouseId = java.util.UUID.randomUUID().toString()
            // generateCode ahora usa la lógica invertida del Modelo (Item + Nivel)
            val code = Warehouse.generateCode(level, itemNumber)

            val warehouse = Warehouse(
                id = warehouseId,
                code = code,
                name = name,
                type = currentType,
                levelNumber = level,
                itemNumber = itemNumber,
                createdBy = "current_user_id"
            )

            createWarehouse(warehouse)
        }
    }

    // --- CORRECCIÓN CLAVE AQUÍ ---
    private fun calculateNextAvailableLocation() {
        val currentType = _uiState.value.selectedType
        val warehousesOfType = _uiState.value.warehouses.filter { it.type == currentType }

        println("📍 DEBUG: Calculando siguiente ubicación (Llenado vertical)")

        // Bucle Principal: Estantes/Items (1 al 30)
        for (itemNumber in 1..Warehouse.MAX_ITEMS_PER_LEVEL) {

            // Bucle Secundario: Niveles (1 al 10)
            // Esto asegura que llenamos el Estante 1 completo antes de pasar al Estante 2
            for (level in 1..Warehouse.MAX_LEVELS) {

                // Verificamos si existe un almacén con este item Y este nivel
                val exists = warehousesOfType.any {
                    it.itemNumber == itemNumber && it.levelNumber == level
                }

                if (!exists) {
                    println("📍 DEBUG: Disponible encontrado -> Estante $itemNumber, Nivel $level")
                    _uiState.value = _uiState.value.copy(
                        nextAvailableLocation = Pair(level, itemNumber)
                    )
                    return
                }
            }
        }

        println("📍 DEBUG: No hay ubicaciones disponibles")
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