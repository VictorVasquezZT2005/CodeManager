// ui/warehouses/WarehousesViewModel.kt
package com.example.codemanager.ui.warehouses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.codemanager.data.model.Warehouse
import com.example.codemanager.data.repository.WarehouseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WarehousesViewModel(
    private val warehouseRepository: WarehouseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WarehousesUiState())
    val uiState: StateFlow<WarehousesUiState> = _uiState.asStateFlow()

    private val _selectedType = MutableStateFlow("estante")
    val selectedType: StateFlow<String> = _selectedType.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        println("DEBUG: WarehousesViewModel inicializado")
        loadAllWarehouses()
        setupWarehousesObserver()
    }

    private fun setupWarehousesObserver() {
        viewModelScope.launch {
            warehouseRepository.warehouses.collect { allWarehouses ->
                println("DEBUG: Observer - Recibidos ${allWarehouses.size} almacenes")

                val filteredWarehouses = if (_selectedType.value.isNotEmpty()) {
                    allWarehouses.filter { it.type == _selectedType.value }
                } else {
                    allWarehouses
                }

                _uiState.value = _uiState.value.copy(
                    warehouses = filteredWarehouses,
                    isLoading = false
                )
            }
        }
    }

    fun loadAllWarehouses() {
        println("DEBUG: Solicitando carga de almacenes")
        _uiState.value = _uiState.value.copy(isLoading = true)

        viewModelScope.launch {
            val result = warehouseRepository.loadWarehouses()
            if (result.isFailure) {
                val error = result.exceptionOrNull()?.message ?: "Error desconocido"
                println("DEBUG: Error en loadAllWarehouses: $error")
                _message.value = "Error al cargar almacenes: $error"
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun createWarehouse(
        shelfNumber: Int,
        levelNumber: Int,
        name: String,
        description: String,
        type: String,
        temperature: String?,
        humidity: String?,
        capacity: String?,
        createdBy: String
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val result = warehouseRepository.createWarehouse(
                shelfNumber = shelfNumber,
                levelNumber = levelNumber,
                name = name,
                description = description,
                type = type,
                temperature = temperature,
                humidity = humidity,
                capacity = capacity,
                createdBy = createdBy
            )

            if (result.isSuccess) {
                _message.value = "✅ Almacén creado exitosamente: ${result.getOrNull()?.code}"
            } else {
                _message.value = "❌ Error: ${result.exceptionOrNull()?.message}"
            }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun setSelectedType(type: String) {
        println("DEBUG: Cambiando tipo a: $type")
        _selectedType.value = type
    }

    fun deleteWarehouse(warehouseId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val result = warehouseRepository.deleteWarehouse(warehouseId)

            if (result.isSuccess) {
                _message.value = "✅ Almacén eliminado exitosamente"
            } else {
                _message.value = "❌ Error al eliminar almacén: ${result.exceptionOrNull()?.message}"
            }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    fun testFirebaseConnection() {
        viewModelScope.launch {
            val isConnected = warehouseRepository.testConnection()
            if (isConnected) {
                _message.value = "✅ Conexión con Firebase establecida"
            } else {
                _message.value = "❌ Error de conexión con Firebase"
            }
        }
    }
}

data class WarehousesUiState(
    val warehouses: List<Warehouse> = emptyList(),
    val isLoading: Boolean = false
)