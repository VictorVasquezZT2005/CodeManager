package com.example.codemanager.ui.warehouses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.codemanager.data.repository.WarehouseRepository

class WarehousesViewModelFactory(
    private val repository: WarehouseRepository = WarehouseRepository()
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WarehousesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WarehousesViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}