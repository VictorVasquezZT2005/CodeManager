package com.example.codemanager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

// Repositorios
import com.example.codemanager.data.repository.AuthRepository
import com.example.codemanager.data.repository.CodeRepository
import com.example.codemanager.data.repository.CategoryRepository
import com.example.codemanager.data.repository.WarehouseRepository

// Utils
import com.example.codemanager.utils.FirstRunManager

// Auth
import com.example.codemanager.ui.auth.AuthViewModel
import com.example.codemanager.ui.auth.AuthViewModelFactory
import com.example.codemanager.ui.auth.LoginScreen

// Codes
import com.example.codemanager.ui.codes.CodesScreen
import com.example.codemanager.ui.codes.CodesViewModel
import com.example.codemanager.ui.codes.CodesViewModelFactory

// Dashboard & Components
import com.example.codemanager.ui.components.NavBar
import com.example.codemanager.ui.components.PermissionsScreen
import com.example.codemanager.ui.dashboard.DashboardScreen

// Categories
import com.example.codemanager.ui.categories.CategoriesScreen
import com.example.codemanager.ui.categories.CategoriesViewModel
import com.example.codemanager.ui.categories.CategoriesViewModelFactory

// Users
import com.example.codemanager.ui.users.UsersScreen

// Warehouses
import com.example.codemanager.ui.warehouses.WarehousesScreen
import com.example.codemanager.ui.warehouses.WarehousesViewModel
import com.example.codemanager.ui.warehouses.WarehousesViewModelFactory

// Theme
import com.example.codemanager.ui.theme.CodeManagerTheme

class MainActivity : ComponentActivity() {

    private val authRepository by lazy { AuthRepository() }
    private val codeRepository by lazy { CodeRepository() }
    private val categoryRepository by lazy { CategoryRepository() }
    private val warehouseRepository by lazy { WarehouseRepository() }

    private val authViewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(authRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()

        val firstRunManager = FirstRunManager(this)

        // --- SOLUCIÓN: VERIFICAR SESIÓN ACTIVA AL INICIAR ---
        // Preguntamos a Firebase si hay un usuario guardado en caché
        if (authRepository.isUserLoggedIn()) {
            // Si existe, forzamos el estado de autenticación a TRUE
            authViewModel.setAuthenticated(true)
        }
        // ----------------------------------------------------

        setContent {
            CodeManagerTheme {
                // Estado para controlar si es la primera vez (Leemos de prefs)
                var isFirstRun by remember { mutableStateOf(firstRunManager.isFirstRun()) }

                // Observamos el estado de autenticación
                val uiState by authViewModel.uiState.collectAsState()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when {
                        // CASO 1: Es la primera vez que abre la app (Permisos)
                        // Nota: Si ya está logueado pero reinstaló la app, podría pedir permisos de nuevo,
                        // lo cual es correcto.
                        isFirstRun -> {
                            PermissionsScreen(
                                onPermissionResult = {
                                    firstRunManager.markAsFinished()
                                    isFirstRun = false
                                }
                            )
                        }

                        // CASO 2: No es primera vez y NO está autenticado -> Login
                        !uiState.isAuthenticated -> {
                            LoginScreen(viewModel = authViewModel)
                        }

                        // CASO 3: Autenticado -> Mostrar la App Principal
                        else -> {
                            MainAppContent(
                                authViewModel = authViewModel,
                                authRepository = authRepository,
                                codeRepository = codeRepository,
                                categoryRepository = categoryRepository,
                                warehouseRepository = warehouseRepository
                            )
                        }
                    }
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Actualizaciones"
            val descriptionText = "Notificaciones de nuevas versiones disponibles"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("updates_channel", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}

// Esta función contiene SOLO la navegación interna cuando ya estás logueado
@Composable
fun MainAppContent(
    authViewModel: AuthViewModel,
    authRepository: AuthRepository,
    codeRepository: CodeRepository,
    categoryRepository: CategoryRepository,
    warehouseRepository: WarehouseRepository
) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavBar(navController = navController)
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(paddingValues)
        ) {
            // --- DASHBOARD ---
            composable("dashboard") {
                DashboardScreen(
                    authViewModel = authViewModel,
                    onLogout = { authViewModel.signOut() }
                )
            }

            // --- CÓDIGOS ---
            composable("codes") {
                val codesViewModel: CodesViewModel = viewModel(
                    factory = CodesViewModelFactory(codeRepository)
                )
                CodesScreen(
                    authViewModel = authViewModel,
                    viewModel = codesViewModel
                )
            }

            // --- CATEGORÍAS ---
            composable("groups") {
                val categoriesViewModel: CategoriesViewModel = viewModel(
                    factory = CategoriesViewModelFactory(categoryRepository)
                )
                CategoriesScreen(viewModel = categoriesViewModel)
            }

            // --- ALMACENES ---
            composable("warehouses") {
                val warehousesViewModel: WarehousesViewModel = viewModel(
                    factory = WarehousesViewModelFactory(warehouseRepository)
                )
                WarehousesScreen(viewModel = warehousesViewModel)
            }

            // --- USUARIOS ---
            composable("users") {
                UsersScreen(
                    authRepository = authRepository,
                    authViewModel = authViewModel
                )
            }
        }
    }
}