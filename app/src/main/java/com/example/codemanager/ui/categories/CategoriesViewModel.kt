package com.example.codemanager.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.codemanager.data.model.Category
import com.example.codemanager.data.repository.CategoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Estado de la UI
data class CategoriesUiState(
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val selectedType: String = "MED" // MED o DESC
)

class CategoriesViewModel(private val repository: CategoryRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoriesUiState())
    val uiState = _uiState.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    init {
        loadCategories("MED")
    }

    fun setCategoryType(type: String) {
        _uiState.value = _uiState.value.copy(selectedType = type)
        loadCategories(type)
    }

    private fun loadCategories(type: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.loadCategoriesByType(type)
            repository.categories.collect { items ->
                _uiState.value = _uiState.value.copy(categories = items, isLoading = false)
            }
        }
    }

    fun createCategory(name: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val type = _uiState.value.selectedType
            val result = repository.createCategory(name, type)
            _uiState.value = _uiState.value.copy(isLoading = false)

            if (result.isSuccess) {
                _message.value = "Categoría creada: ${result.getOrNull()?.code}"
            } else {
                _message.value = "Error: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun updateCategory(id: String, name: String) {
        viewModelScope.launch {
            val result = repository.updateCategory(id, name, _uiState.value.selectedType)
            if (result.isSuccess) _message.value = "Categoría actualizada"
        }
    }

    fun deleteCategory(id: String) {
        viewModelScope.launch {
            val result = repository.deleteCategory(id, _uiState.value.selectedType)
            if (result.isSuccess) _message.value = "Categoría eliminada"
        }
    }

    fun clearMessage() { _message.value = null }
}

// Factory para inyección de dependencias
class CategoriesViewModelFactory(private val repository: CategoryRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CategoriesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CategoriesViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}