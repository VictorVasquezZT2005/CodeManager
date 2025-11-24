package com.example.codemanager.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp // <--- IMPORTANTE: Importar sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.codemanager.data.repository.AuthRepository
import com.example.codemanager.ui.auth.AuthViewModel
import com.example.codemanager.ui.auth.AuthViewModelFactory

@Composable
fun DashboardScreen(
    onLogout: () -> Unit,
    authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(AuthRepository()))
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Cálculo de iniciales
    val userInitials = remember(currentUser?.name) {
        val name = currentUser?.name?.trim() ?: ""
        if (name.isNotEmpty()) {
            val parts = name.split("\\s+".toRegex())
            val first = parts.getOrNull(0)?.take(1) ?: ""
            val second = parts.getOrNull(1)?.take(1) ?: ""
            (first + second).uppercase()
        } else {
            "U"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // --- HEADER MATERIAL YOU ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(bottom = 32.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, start = 24.dp, end = 24.dp, bottom = 16.dp)
            ) {
                // Título pequeño "Dashboard"
                Text(
                    text = "DASHBOARD",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp // <--- CORREGIDO: Uso correcto de .sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // --- ICONO DE USUARIO MATERIAL YOU ---
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userInitials,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    Column {
                        Text(
                            text = "Hola, ${currentUser?.name?.split(" ")?.first() ?: "Usuario"}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )

                        // Badge para el rol
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = MaterialTheme.shapes.extraSmall,
                            modifier = Modifier.padding(top = 6.dp)
                        ) {
                            Text(
                                text = (currentUser?.rol ?: "...").uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // --- CONTENIDO PRINCIPAL ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .offset(y = (-20).dp)
        ) {
            // Tarjeta de información del usuario
            // CORREGIDO: Usamos ElevatedCard porque querías elevación sin borde visible.
            // OutlinedCard obliga a tener borde.
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Datos de la cuenta",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    UserInfoRow(
                        icon = Icons.Default.Badge,
                        label = "Nombre completo",
                        value = currentUser?.name ?: "No disponible"
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    UserInfoRow(
                        icon = Icons.Default.AlternateEmail,
                        label = "Correo electrónico",
                        value = currentUser?.email ?: "No disponible"
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    UserInfoRow(
                        icon = Icons.Default.VerifiedUser,
                        label = "Nivel de acceso",
                        value = currentUser?.rol ?: "No disponible"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tarjeta de Bienvenida / Info
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.TipsAndUpdates,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Bienvenido al Sistema",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Navega por las pestañas inferiores para gestionar inventarios, códigos y equipos.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Botón de cerrar sesión
            Button(
                onClick = { showLogoutDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Cerrar Sesión",
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Diálogo de confirmación
    if (showLogoutDialog) {
        LogoutConfirmDialog(
            onConfirm = onLogout,
            onDismiss = { showLogoutDialog = false }
        )
    }
}

// --- Componentes Auxiliares ---

@Composable
private fun UserInfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun LogoutConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Logout, contentDescription = null) },
        title = {
            Text(text = "Cerrar Sesión", textAlign = TextAlign.Center)
        },
        text = {
            Text(
                text = "¿Estás seguro de que deseas salir del sistema?",
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Cerrar Sesión")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}