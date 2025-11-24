package com.example.codemanager.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.codemanager.data.model.TherapeuticGroup // <-- Importación corregida
import com.example.codemanager.data.repository.GroupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GroupsViewModel(private val groupRepository: GroupRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupsUiState())
    val uiState: StateFlow<GroupsUiState> = _uiState.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        loadGroups()
    }

    fun loadGroups() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            try {
                groupRepository.loadGroups()
                // Observamos la lista de TherapeuticGroup
                groupRepository.groups.collect { groups ->
                    _uiState.value = _uiState.value.copy(
                        groups = groups,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                _message.value = "Error al cargar grupos: ${e.message}"
            }
        }
    }

    fun createGroup(name: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = groupRepository.createGroup(name)
            _uiState.value = _uiState.value.copy(isLoading = false)

            if (result.isSuccess) {
                _message.value = "Grupo ${result.getOrNull()?.code} creado exitosamente"
            } else {
                _message.value = "Error al crear grupo: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun deleteGroup(groupId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = groupRepository.deleteGroup(groupId)
            _uiState.value = _uiState.value.copy(isLoading = false)

            if (result.isSuccess) {
                _message.value = "Grupo eliminado exitosamente"
            } else {
                _message.value = "Error al eliminar grupo: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun updateGroup(groupId: String, name: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = groupRepository.updateGroup(groupId, name)
            _uiState.value = _uiState.value.copy(isLoading = false)

            if (result.isSuccess) {
                _message.value = "Grupo actualizado exitosamente"
            } else {
                _message.value = "Error al actualizar grupo: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}

// Actualizamos el State para usar TherapeuticGroup
data class GroupsUiState(
    val groups: List<TherapeuticGroup> = emptyList(),
    val isLoading: Boolean = false
)

// --- FACTORY INCLUIDA AQUÍ MISMO ---
class GroupsViewModelFactory(private val repository: GroupRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GroupsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GroupsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}