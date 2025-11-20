// ui/codes/CodesViewModelFactory.kt
package com.example.codemanager.ui.codes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.codemanager.data.repository.CodeRepository

class CodesViewModelFactory(
    private val codeRepository: CodeRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CodesViewModel::class.java)) {
            return CodesViewModel(codeRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}