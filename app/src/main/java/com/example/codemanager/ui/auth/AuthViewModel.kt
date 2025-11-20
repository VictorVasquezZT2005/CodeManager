package com.example.codemanager.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.codemanager.data.model.User
import com.example.codemanager.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    // Cambia esto de MutableStateFlow a StateFlow para exposición pública
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()  // ✅ Cambiado a StateFlow

    init {
        loadCurrentUser()
    }

    fun onEmailChange(email: String) {
        _uiState.value = _uiState.value.copy(
            email = email,
            emailError = null
        )
    }

    fun onPasswordChange(password: String) {
        _uiState.value = _uiState.value.copy(
            password = password,
            passwordError = null
        )
    }

    fun login() {
        val currentState = _uiState.value

        // Validaciones
        val emailError = if (currentState.email.isBlank()) "El email es requerido"
        else if (!isValidEmail(currentState.email)) "Email no válido"
        else null

        val passwordError = if (currentState.password.isBlank()) "La contraseña es requerida"
        else if (currentState.password.length < 6) "Mínimo 6 caracteres"
        else null

        if (emailError != null || passwordError != null) {
            _uiState.value = currentState.copy(
                emailError = emailError,
                passwordError = passwordError,
                error = null
            )
            return
        }

        _uiState.value = currentState.copy(
            isLoading = true,
            error = null
        )

        viewModelScope.launch {
            try {
                val result = authRepository.signIn(currentState.email, currentState.password)
                if (result.isSuccess) {
                    loadCurrentUser()
                    _uiState.value = _uiState.value.copy(
                        isAuthenticated = true,
                        isLoading = false,
                        error = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = result.exceptionOrNull()?.message ?: "Error al iniciar sesión",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Error desconocido",
                    isLoading = false
                )
            }
        }
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            _currentUser.value = authRepository.getCurrentUser()
        }
    }

    fun setAuthenticated(authenticated: Boolean) {
        _uiState.value = _uiState.value.copy(
            isAuthenticated = authenticated
        )
        if (authenticated) {
            loadCurrentUser()
        } else {
            _currentUser.value = null
        }
    }

    fun resetAuthState() {
        _uiState.value = LoginUiState()
        _currentUser.value = null
    }

    fun signOut() {
        authRepository.signOut()
        resetAuthState()
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val error: String? = null
)