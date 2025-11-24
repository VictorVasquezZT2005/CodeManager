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
                    warehouses.forEach { w ->
                        println("  - ${w.type} ${w.code}: ${w.name}")
                    }

                    // Actualizar el estado con todos los almacenes
                    _uiState.value = _uiState.value.copy(
                        warehouses = warehouses,
                        isLoading = false
                    )

                    // Aplicar filtros inmediatamente después de cargar
                    applyFiltersAndUpdateState(warehouses)
                    calculateNextAvailableLocation()
                },
                onFailure = { exception ->
                    println("❌ DEBUG: Error al cargar: ${exception.message}")
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
                    // Recargar inmediatamente después de crear
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

            // Generar ID con UUID
            val warehouseId = java.util.UUID.randomUUID().toString()
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

    private fun calculateNextAvailableLocation() {
        val currentType = _uiState.value.selectedType
        val warehousesOfType = _uiState.value.warehouses.filter { it.type == currentType }

        println("📍 DEBUG: Calculando siguiente ubicación para $currentType")
        println("📍 DEBUG: Almacenes del tipo: ${warehousesOfType.size}")

        if (warehousesOfType.isEmpty()) {
            println("📍 DEBUG: Primer almacén - Nivel 1, Item 1")
            _uiState.value = _uiState.value.copy(
                nextAvailableLocation = Pair(1, 1)
            )
            return
        }

        for (level in 1..Warehouse.MAX_LEVELS) {
            val itemsInLevel = warehousesOfType.filter { it.levelNumber == level }
            println("📍 DEBUG: Nivel $level - Items: ${itemsInLevel.size}")

            if (itemsInLevel.size < Warehouse.MAX_ITEMS_PER_LEVEL) {
                val usedNumbers = itemsInLevel.map { it.itemNumber }.toSortedSet()
                println("📍 DEBUG: Números usados en nivel $level: $usedNumbers")

                for (itemNumber in 1..Warehouse.MAX_ITEMS_PER_LEVEL) {
                    if (!usedNumbers.contains(itemNumber)) {
                        println("📍 DEBUG: Siguiente ubicación disponible: Nivel $level, Item $itemNumber")
                        _uiState.value = _uiState.value.copy(
                            nextAvailableLocation = Pair(level, itemNumber)
                        )
                        return
                    }
                }
            }
        }

        println("📍 DEBUG: No hay ubicaciones disponibles")
        _uiState.value = _uiState.value.copy(nextAvailableLocation = null)
    }

    fun setSelectedType(type: String) {
        println("🎯 DEBUG: Cambiando tipo a: $type")
        _uiState.value = _uiState.value.copy(selectedType = type)
        applyFiltersAndUpdateState()
        calculateNextAvailableLocation()
    }

    private fun applyFiltersAndUpdateState(warehouses: List<Warehouse>? = null) {
        val warehousesToFilter = warehouses ?: _uiState.value.warehouses
        val filtered = applyFilters(warehousesToFilter)

        println("🔍 DEBUG: Aplicando filtros")
        println("🔍 DEBUG: Tipo seleccionado: ${_uiState.value.selectedType}")
        println("🔍 DEBUG: Total almacenes: ${warehousesToFilter.size}")
        println("🔍 DEBUG: Almacenes filtrados: ${filtered.size}")
        filtered.forEach { w ->
            println("  - ${w.type} ${w.code}: ${w.name}")
        }

        _uiState.value = _uiState.value.copy(filteredWarehouses = filtered)
    }

    private fun applyFilters(warehouses: List<Warehouse>): List<Warehouse> {
        return warehouses.filter { it.type == _uiState.value.selectedType }
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