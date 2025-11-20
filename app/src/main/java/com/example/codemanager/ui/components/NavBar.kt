package com.example.codemanager.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun NavBar(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier
    ) {
        val navBackStackEntry = navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry.value?.destination

        NavBarItem(
            icon = Icons.Default.Home,
            label = "Dashboard",
            route = "dashboard",
            navController = navController,
            currentDestination = currentDestination
        )

        NavBarItem(
            icon = Icons.Default.Code,
            label = "Códigos",
            route = "codes",
            navController = navController,
            currentDestination = currentDestination
        )

        NavBarItem(
            icon = Icons.Default.Group,
            label = "Grupos",
            route = "groups",
            navController = navController,
            currentDestination = currentDestination
        )

        NavBarItem(
            icon = Icons.Default.Storage,
            label = "Almacenes",
            route = "warehouses",
            navController = navController,
            currentDestination = currentDestination
        )

        NavBarItem(
            icon = Icons.Default.Person,
            label = "Usuarios",
            route = "users",
            navController = navController,
            currentDestination = currentDestination
        )
    }
}

@Composable
fun RowScope.NavBarItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    route: String,
    navController: NavHostController,
    currentDestination: NavDestination?
) {
    val selected = currentDestination?.hierarchy?.any { it.route == route } == true

    NavigationBarItem(
        selected = selected,
        onClick = {
            navController.navigate(route) {
                launchSingleTop = true
                restoreState = true
            }
        },
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = label
            )
        },
        label = {
            Text(text = label)
        }
    )
}