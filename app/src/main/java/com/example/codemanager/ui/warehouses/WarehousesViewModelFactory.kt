package com.example.codemanager.ui.warehouses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class WarehousesViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WarehousesViewModel::class.java)) {
            return WarehousesViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}