// ui/groups/GroupsViewModel.kt
package com.example.codemanager.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.codemanager.data.model.Group
import com.example.codemanager.data.repository.GroupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GroupsViewModel(private val groupRepository: GroupRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<GroupsUiState>(GroupsUiState())
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
                // Observar cambios en el flow
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

    fun createGroup(name: String, description: String = "", createdBy: String = "") {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = groupRepository.createGroup(name, description, createdBy)
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

    fun updateGroup(groupId: String, name: String, description: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = groupRepository.updateGroup(groupId, name, description)
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

data class GroupsUiState(
    val groups: List<Group> = emptyList(),
    val isLoading: Boolean = false
)