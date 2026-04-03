package com.example.codemanager.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        // Al iniciar el ViewModel, intentamos cargar el usuario si ya existe sesión
        if (authRepository.isUserLoggedIn()) {
            loadCurrentUser()
        }
    }

    // --- Login Fields ---
    fun onEmailChange(email: String) {
        _uiState.value = _uiState.value.copy(email = email, emailError = null)
    }

    fun onPasswordChange(password: String) {
        _uiState.value = _uiState.value.copy(password = password, passwordError = null)
    }

    // --- Reset Password Fields ---
    fun onResetEmailChange(email: String) {
        _uiState.value = _uiState.value.copy(resetEmail = email)
    }

    fun showResetDialog() {
        _uiState.value = _uiState.value.copy(
            showResetDialog = true,
            resetEmail = _uiState.value.email,
            resetMessage = null,
            resetError = null
        )
    }

    fun hideResetDialog() {
        _uiState.value = _uiState.value.copy(showResetDialog = false)
    }

    // --- ACTIONS ---

    fun login() {
        val currentState = _uiState.value

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

        _uiState.value = currentState.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                val result = authRepository.signIn(currentState.email, currentState.password)
                if (result.isSuccess) {
                    loadCurrentUser() // <--- Esto carga el rol automáticamente
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

    fun sendResetPasswordEmail() {
        val email = _uiState.value.resetEmail
        if (!isValidEmail(email)) {
            _uiState.value = _uiState.value.copy(resetError = "Ingresa un correo válido")
            return
        }

        _uiState.value = _uiState.value.copy(resetLoading = true, resetError = null, resetMessage = null)

        viewModelScope.launch {
            val result = authRepository.sendPasswordResetEmail(email)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    resetLoading = false,
                    resetMessage = "Se ha enviado un correo de recuperación a $email",
                    resetError = null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    resetLoading = false,
                    resetError = result.exceptionOrNull()?.message ?: "Error al enviar correo"
                )
            }
        }
    }

    // --- CARGAR USUARIO Y ROL ---
    private fun loadCurrentUser() {
        viewModelScope.launch {
            // Obtenemos el objeto usuario completo
            val user = authRepository.getCurrentUser()
            _currentUser.value = user

            // ACTUALIZAMOS EL UI STATE CON EL ROL
            _uiState.value = _uiState.value.copy(
                userRole = user?.rol // <--- AQUÍ SE ASIGNA EL ROL
            )
        }
    }

    // Llamado desde MainActivity
    fun setAuthenticated(authenticated: Boolean) {
        _uiState.value = _uiState.value.copy(isAuthenticated = authenticated)
        if (authenticated) {
            loadCurrentUser() // Si forzamos auth, cargamos datos (y rol)
        } else {
            _currentUser.value = null
            _uiState.value = _uiState.value.copy(userRole = null)
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

// --- STATE ACTUALIZADO (Con userRole) ---
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val userRole: String? = null, // <--- ESTE CAMPO ES EL QUE FALTABA
    val emailError: String? = null,
    val passwordError: String? = null,
    val error: String? = null,

    // Estados para Reset Password
    val showResetDialog: Boolean = false,
    val resetEmail: String = "",
    val resetLoading: Boolean = false,
    val resetMessage: String? = null,
    val resetError: String? = null
)

class AuthViewModelFactory(
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}