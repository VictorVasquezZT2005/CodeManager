package com.example.codemanager.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.codemanager.ui.codes.CodesScreen
import com.example.codemanager.ui.dashboard.DashboardScreen
import com.example.codemanager.ui.groups.GroupsScreen
import com.example.codemanager.ui.warehouses.WarehousesScreen // Cambiado el import
import com.example.codemanager.ui.users.UsersScreen

@Composable
fun AppNavigation(
    navController: NavHostController,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = "dashboard",
        modifier = modifier
    ) {
        composable("dashboard") {
            DashboardScreen(onLogout = onLogout)
        }
        composable("codes") {
            CodesScreen()
        }
        composable("groups") {
            GroupsScreen()
        }
        composable("warehouses") { // Cambiado de "shelves" a "warehouses"
            WarehousesScreen() // Cambiado de ShelvesScreen() a WarehousesScreen()
        }
        composable("users") {
            UsersScreen()
        }
    }
}