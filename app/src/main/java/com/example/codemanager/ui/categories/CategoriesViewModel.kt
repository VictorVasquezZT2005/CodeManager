package com.example.codemanager.ui.categories

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.codemanager.data.model.Category
import com.example.codemanager.data.repository.CategoryRepository
import com.example.codemanager.utils.CsvUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
            val upperName = name.trim().uppercase()
            val type = _uiState.value.selectedType
            val result = repository.createCategory(upperName, type)

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
            val upperName = name.trim().uppercase()
            val result = repository.updateCategory(id, upperName, _uiState.value.selectedType)
            if (result.isSuccess) _message.value = "Categoría actualizada"
        }
    }

    fun deleteCategory(id: String) {
        viewModelScope.launch {
            val result = repository.deleteCategory(id, _uiState.value.selectedType)
            if (result.isSuccess) _message.value = "Categoría eliminada"
        }
    }

    fun exportData(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val csvContent = CsvUtils.exportCategoriesToCsv(_uiState.value.categories)
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(csvContent.toByteArray())
                }
                withContext(Dispatchers.Main) { _message.value = "Datos exportados correctamente" }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { _message.value = "Error al exportar: ${e.message}" }
            }
        }
    }

    fun importData(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { _uiState.value = _uiState.value.copy(isLoading = true) }
            try {
                val names = CsvUtils.parseCategoriesFromCsv(context, uri)
                var count = 0
                val type = _uiState.value.selectedType

                names.forEach { name ->
                    val upperName = name.trim().uppercase()
                    repository.createCategory(upperName, type)
                    count++
                }

                withContext(Dispatchers.Main) {
                    _message.value = "Se importaron $count registros"
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    _message.value = "Error importando: ${e.message}"
                }
            }
        }
    }

    fun clearMessage() { _message.value = null }
}

class CategoriesViewModelFactory(private val repository: CategoryRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CategoriesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CategoriesViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}