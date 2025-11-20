package com.example.codemanager.ui.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.codemanager.data.model.User
import com.example.codemanager.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UsersViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _showDialog = MutableStateFlow(false)
    val showDialog: StateFlow<Boolean> = _showDialog.asStateFlow()

    private val _editingUser = MutableStateFlow<User?>(null)
    val editingUser: StateFlow<User?> = _editingUser.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Datos para nuevo usuario
    private val _newUserName = MutableStateFlow("")
    val newUserName: StateFlow<String> = _newUserName.asStateFlow()

    private val _newUserEmail = MutableStateFlow("")
    val newUserEmail: StateFlow<String> = _newUserEmail.asStateFlow()

    private val _newUserPassword = MutableStateFlow("")
    val newUserPassword: StateFlow<String> = _newUserPassword.asStateFlow()

    private val _newUserRol = MutableStateFlow("Usuario")
    val newUserRol: StateFlow<String> = _newUserRol.asStateFlow()

    init {
        loadUsersFromFirestore()
    }

    fun loadUsersFromFirestore() {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                // Obtener usuarios reales de Firestore
                val usersList = authRepository.getAllUsers()
                _users.value = usersList
            } catch (e: Exception) {
                _errorMessage.value = "Error al cargar usuarios: ${e.message}"
                _users.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun showAddUserDialog() {
        _editingUser.value = null
        _newUserName.value = ""
        _newUserEmail.value = ""
        _newUserPassword.value = ""
        _newUserRol.value = "Usuario"
        _showDialog.value = true
        _errorMessage.value = null
    }

    fun showEditUserDialog(user: User) {
        _editingUser.value = user
        _newUserName.value = user.name
        _newUserEmail.value = user.email
        _newUserRol.value = user.rol
        _showDialog.value = true
        _errorMessage.value = null
    }

    fun hideDialog() {
        _showDialog.value = false
        _editingUser.value = null
        _errorMessage.value = null
    }

    fun updateNewUserName(name: String) {
        _newUserName.value = name
    }

    fun updateNewUserEmail(email: String) {
        _newUserEmail.value = email
    }

    fun updateNewUserPassword(password: String) {
        _newUserPassword.value = password
    }

    fun updateNewUserRol(rol: String) {
        _newUserRol.value = rol
    }

    fun createUser() {
        if (_newUserEmail.value.isBlank() || _newUserPassword.value.isBlank() || _newUserName.value.isBlank()) {
            _errorMessage.value = "Todos los campos son requeridos"
            return
        }

        if (_newUserPassword.value.length < 6) {
            _errorMessage.value = "La contraseña debe tener al menos 6 caracteres"
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = authRepository.createUser(
                    email = _newUserEmail.value,
                    password = _newUserPassword.value,
                    name = _newUserName.value,
                    rol = _newUserRol.value
                )

                if (result.isSuccess) {
                    // Recargar la lista de usuarios
                    loadUsersFromFirestore()
                    hideDialog()
                } else {
                    _errorMessage.value = "Error al crear usuario: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateUser() {
        val user = _editingUser.value ?: return

        if (_newUserName.value.isBlank()) {
            _errorMessage.value = "El nombre es requerido"
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = authRepository.updateUser(
                    userId = user.id,
                    name = _newUserName.value,
                    rol = _newUserRol.value
                )

                if (result.isSuccess) {
                    // Recargar la lista de usuarios
                    loadUsersFromFirestore()
                    hideDialog()
                } else {
                    _errorMessage.value = "Error al actualizar usuario: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteUser(user: User) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = authRepository.deleteUser(user.id)

                if (result.isSuccess) {
                    // Recargar la lista de usuarios
                    loadUsersFromFirestore()
                } else {
                    _errorMessage.value = "Error al eliminar usuario: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}