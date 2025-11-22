package com.example.codemanager.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.codemanager.ui.auth.AuthViewModel

@Composable
fun NavBar(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    // Usar collectAsStateWithLifecycle en lugar de collectAsState
    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .padding(horizontal = 12.dp, vertical = 8.dp) // Reducido horizontal para más espacio
            .systemBarsPadding()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(25.dp),
                clip = true
            )
    ) {
        NavigationBar(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp) // Un poco más alto para mejor distribución
                .clip(RoundedCornerShape(25.dp)),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 0.dp
        ) {
            val navBackStackEntry = navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry.value?.destination

            // Calcular si hay 4 o 5 elementos para ajustar el peso
            val totalItems = if (currentUser?.rol == "Administrador") 5 else 4

            NavBarItem(
                icon = Icons.Default.Home,
                label = "Dashboard",
                route = "dashboard",
                navController = navController,
                currentDestination = currentDestination,
                weight = 1f / totalItems
            )

            NavBarItem(
                icon = Icons.Default.Code,
                label = "Códigos",
                route = "codes",
                navController = navController,
                currentDestination = currentDestination,
                weight = 1f / totalItems
            )

            NavBarItem(
                icon = Icons.Default.Group,
                label = "Grupos",
                route = "groups",
                navController = navController,
                currentDestination = currentDestination,
                weight = 1f / totalItems
            )

            NavBarItem(
                icon = Icons.Default.Storage,
                label = "Almacenes",
                route = "warehouses",
                navController = navController,
                currentDestination = currentDestination,
                weight = 1f / totalItems
            )

            // Solo mostrar usuarios si es admin
            if (currentUser?.rol == "Administrador") {
                NavBarItem(
                    icon = Icons.Default.Person,
                    label = "Usuarios",
                    route = "users",
                    navController = navController,
                    currentDestination = currentDestination,
                    weight = 1f / totalItems
                )
            }
        }
    }
}

@Composable
fun RowScope.NavBarItem(
    icon: ImageVector,
    label: String,
    route: String,
    navController: NavHostController,
    currentDestination: NavDestination?,
    weight: Float = 1f
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
                contentDescription = label,
                modifier = Modifier.size(20.dp) // Reducido para más espacio
            )
        },
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis // Evita que el texto se desborde
            )
        },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        ),
        modifier = Modifier.weight(weight) // Distribución equitativa del espacio
    )
}