// ui/codes/CodesViewModel.kt
package com.example.codemanager.ui.codes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.codemanager.data.model.Code
import com.example.codemanager.data.repository.CodeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CodesViewModel(private val codeRepository: CodeRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<CodesUiState>(CodesUiState())
    val uiState: StateFlow<CodesUiState> = _uiState.asStateFlow()

    private val _selectedPrefix = MutableStateFlow("62")
    val selectedPrefix: StateFlow<String> = _selectedPrefix.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        loadCodes()
    }

    fun loadCodes() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            try {
                codeRepository.loadCodes()
                // Observar cambios en el flow
                codeRepository.codes.collect { codes ->
                    _uiState.value = _uiState.value.copy(
                        codes = codes,
                        filteredCodes = if (_selectedPrefix.value == "ALL") {
                            codes
                        } else {
                            codes.filter { it.prefix == _selectedPrefix.value }
                        },
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                _message.value = "Error al cargar códigos: ${e.message}"
            }
        }
    }

    fun generateNewCode(description: String = "", createdBy: String = "") {
        viewModelScope.launch {
            val prefix = _selectedPrefix.value
            if (prefix != "ALL") {
                _uiState.value = _uiState.value.copy(isLoading = true)
                val result = codeRepository.generateCode(prefix, description, createdBy)
                _uiState.value = _uiState.value.copy(isLoading = false)

                if (result.isSuccess) {
                    _message.value = "Código ${result.getOrNull()?.code} generado exitosamente"
                } else {
                    _message.value = "Error al generar código: ${result.exceptionOrNull()?.message}"
                }
            }
        }
    }

    fun setSelectedPrefix(prefix: String) {
        _selectedPrefix.value = prefix
        loadCodes()
    }

    fun deleteCode(codeId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = codeRepository.deleteCode(codeId)
            _uiState.value = _uiState.value.copy(isLoading = false)

            if (result.isSuccess) {
                _message.value = "Código eliminado exitosamente"
            } else {
                _message.value = "Error al eliminar código: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}

data class CodesUiState(
    val codes: List<Code> = emptyList(),
    val filteredCodes: List<Code> = emptyList(),
    val isLoading: Boolean = false
)