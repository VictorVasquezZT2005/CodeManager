// ui/warehouses/WarehousesViewModelFactory.kt
package com.example.codemanager.ui.warehouses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.codemanager.data.repository.WarehouseRepository

class WarehousesViewModelFactory(
    private val warehouseRepository: WarehouseRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WarehousesViewModel::class.java)) {
            return WarehousesViewModel(warehouseRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}