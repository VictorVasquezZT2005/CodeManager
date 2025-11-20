package com.example.codemanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.codemanager.data.repository.AuthRepository
import com.example.codemanager.data.repository.CodeRepository
import com.example.codemanager.ui.auth.AuthViewModel
import com.example.codemanager.ui.auth.AuthViewModelFactory
import com.example.codemanager.ui.auth.LoginScreen
import com.example.codemanager.ui.codes.CodesScreen
import com.example.codemanager.ui.codes.CodesViewModel
import com.example.codemanager.ui.codes.CodesViewModelFactory
import com.example.codemanager.ui.components.NavBar
import com.example.codemanager.ui.dashboard.DashboardScreen
import com.example.codemanager.ui.groups.GroupsScreen
import com.example.codemanager.ui.warehouses.WarehousesScreen
import com.example.codemanager.ui.users.UsersScreen
import com.example.codemanager.ui.users.UsersViewModelFactory
import com.example.codemanager.ui.theme.CodeManagerTheme

class MainActivity : ComponentActivity() {

    private val authRepository by lazy { AuthRepository() }
    private val codeRepository by lazy { CodeRepository() }
    private val authViewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(authRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CodeManagerApp(
                authViewModel = authViewModel,
                authRepository = authRepository,
                codeRepository = codeRepository
            )
        }
    }
}

@Composable
fun CodeManagerApp(
    authViewModel: AuthViewModel,
    authRepository: AuthRepository,
    codeRepository: CodeRepository
) {
    val uiState by authViewModel.uiState.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    val navController = rememberNavController()

    // Verificar si ya hay un usuario logueado al iniciar la app
    LaunchedEffect(Unit) {
        if (authRepository.isUserLoggedIn()) {
            authViewModel.setAuthenticated(true)
        }
    }

    CodeManagerTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            if (uiState.isAuthenticated) {
                // Layout principal con NavBar
                Scaffold(
                    bottomBar = {
                        NavBar(
                            navController = navController,
                            authViewModel = authViewModel
                        )
                    }
                ) { paddingValues ->
                    NavHost(
                        navController = navController,
                        startDestination = "dashboard",
                        modifier = Modifier.padding(paddingValues)
                    ) {
                        composable("dashboard") {
                            DashboardScreen(
                                onLogout = {
                                    authViewModel.signOut()
                                }
                            )
                        }
                        composable("codes") {
                            // Pasar el codeRepository para crear el CodesViewModel
                            val codesViewModel: CodesViewModel = viewModel(
                                factory = CodesViewModelFactory(codeRepository)
                            )
                            CodesScreen(viewModel = codesViewModel)
                        }
                        composable("groups") {
                            GroupsScreen()
                        }
                        composable("warehouses") {
                            WarehousesScreen()
                        }
                        composable("users") {
                            // Pasar el authRepository para crear el UsersViewModel
                            UsersScreen(
                                authRepository = authRepository,
                                authViewModel = authViewModel
                            )
                        }
                    }
                }
            } else {
                LoginScreen(viewModel = authViewModel)
            }
        }
    }
}