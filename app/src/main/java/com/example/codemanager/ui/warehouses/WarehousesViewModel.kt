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
    val searchQuery: String = "",
    val selectedType: String? = null,
    val selectedLevel: Int? = null,
    val showAddDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val selectedWarehouse: Warehouse? = null,
    val successMessage: String? = null
)

class WarehousesViewModel(
    private val repository: WarehouseRepository = WarehouseRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(WarehousesUiState())
    val uiState: StateFlow<WarehousesUiState> = _uiState.asStateFlow()

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
                        filteredWarehouses = applyFilters(warehouses),
                        isLoading = false
                    )
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
                    _uiState.value = _uiState.value.copy(
                        showAddDialog = false,
                        successMessage = "Almacén creado exitosamente"
                    )
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
                        successMessage = "Almacén actualizado exitosamente"
                    )
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
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Almacén eliminado exitosamente"
                    )
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

    fun updateEnvironmentalConditions(warehouseId: String, temperature: Double?, humidity: Double?) {
        viewModelScope.launch {
            repository.updateEnvironmentalConditions(warehouseId, temperature, humidity).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Condiciones actualizadas"
                    )
                    loadWarehouses()
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        error = "Error al actualizar: ${exception.message}"
                    )
                }
            )
        }
    }

    fun searchWarehouses(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)

        if (query.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                filteredWarehouses = applyFilters(_uiState.value.warehouses)
            )
        } else {
            viewModelScope.launch {
                repository.searchWarehouses(query).fold(
                    onSuccess = { warehouses ->
                        _uiState.value = _uiState.value.copy(
                            filteredWarehouses = applyFilters(warehouses)
                        )
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            error = "Error en búsqueda: ${exception.message}"
                        )
                    }
                )
            }
        }
    }

    fun filterByType(type: String?) {
        _uiState.value = _uiState.value.copy(selectedType = type)
        _uiState.value = _uiState.value.copy(
            filteredWarehouses = applyFilters(_uiState.value.warehouses)
        )
    }

    fun filterByLevel(level: Int?) {
        _uiState.value = _uiState.value.copy(selectedLevel = level)
        _uiState.value = _uiState.value.copy(
            filteredWarehouses = applyFilters(_uiState.value.warehouses)
        )
    }

    private fun applyFilters(warehouses: List<Warehouse>): List<Warehouse> {
        var filtered = warehouses

        _uiState.value.selectedType?.let { type ->
            filtered = filtered.filter { it.type == type }
        }

        _uiState.value.selectedLevel?.let { level ->
            filtered = filtered.filter { it.levelNumber == level }
        }

        if (_uiState.value.searchQuery.isNotEmpty()) {
            val query = _uiState.value.searchQuery
            filtered = filtered.filter {
                it.code.contains(query, ignoreCase = true) ||
                        it.name.contains(query, ignoreCase = true) ||
                        it.type.contains(query, ignoreCase = true)
            }
        }

        return filtered
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

    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }
}