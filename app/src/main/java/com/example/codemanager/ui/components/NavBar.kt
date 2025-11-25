package com.example.codemanager.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun NavBar(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val navBarColor = MaterialTheme.colorScheme.surface

    NavigationBar(
        modifier = modifier,
        containerColor = navBarColor,
        tonalElevation = 0.dp   // <- SUPER IMPORTANTE
    ) {
        val navBackStackEntry = navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry.value?.destination

        val items = listOf(
            NavItem("dashboard", "Dashboard", Icons.Default.Home),
            NavItem("codes", "Códigos", Icons.Default.Code),
            // --- CAMBIO: Grupos -> Categorías ---
            NavItem("groups", "Categorías", Icons.Default.Category),
            NavItem("warehouses", "Almacenes", Icons.Default.Storage),
            NavItem("users", "Usuarios", Icons.Default.Person)
        )

        items.forEach { item ->
            NavBarItem(
                icon = item.icon,
                label = item.label,
                route = item.route,
                navController = navController,
                currentDestination = currentDestination
            )
        }
    }
}

data class NavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun RowScope.NavBarItem(
    icon: ImageVector,
    label: String,
    route: String,
    navController: NavHostController,
    currentDestination: NavDestination?
) {
    val selected = currentDestination?.hierarchy?.any { it.route == route } == true

    val scale by animateFloatAsState(
        targetValue = if (selected) 1.2f else 1f,
        label = ""
    )

    val iconColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = ""
    )

    val textColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = ""
    )

    NavigationBarItem(
        selected = selected,
        onClick = {
            navController.navigate(route) {
                popUpTo(navController.graph.startDestinationId) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        },
        icon = {
            Box(modifier = Modifier.size(26.dp)) {
                Icon(
                    modifier = Modifier.scale(scale),
                    imageVector = icon,
                    tint = iconColor,
                    contentDescription = label
                )
            }
        },
        label = {
            Text(
                text = label,
                color = textColor,
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
        },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        )
    )
}