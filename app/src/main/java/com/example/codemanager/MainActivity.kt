package com.example.codemanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.codemanager.ui.theme.CodeManagerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CodeManagerTheme(
                // Esto automáticamente detectará si el celular está en modo oscuro
                darkTheme = isSystemInDarkTheme(),
                dynamicColor = true // Usará los colores dinámicos del sistema si está disponible
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WelcomeScreen()
                }
            }
        }
    }
}

// Data class para la información del usuario
data class User(
    val name: String,
    val role: String,
    val id: String,
    val department: String = "Hospital"
)

@Composable
fun WelcomeScreen() {
    // Datos de ejemplo - luego vendrán de tu base de datos
    val user = User(
        name = "Dr. Juan Pérez",
        role = "Médico Administrativo",
        id = "HSP-2024-001"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header con logo/icono
        Icon(
            imageVector = Icons.Default.Storage,
            contentDescription = "CodeManager Logo",
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Título de bienvenida
        Text(
            text = "Bienvenido a CodeManager",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Sistema de Gestión de Códigos Hospitalarios",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Tarjeta de información del usuario
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Información del usuario
                UserInfoItem(
                    icon = Icons.Default.Person,
                    title = "Nombre",
                    value = user.name
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                UserInfoItem(
                    icon = Icons.Default.Business,
                    title = "Rol",
                    value = user.role
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                UserInfoItem(
                    icon = Icons.Default.VpnKey,
                    title = "ID de Empleado",
                    value = user.id
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Botones de navegación principal
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NavigationButton(
                text = "Administrar Códigos",
                onClick = { /* Navegar a gestión de códigos */ }
            )

            NavigationButton(
                text = "Administrar Estantes",
                onClick = { /* Navegar a gestión de estantes */ }
            )

            NavigationButton(
                text = "Grupos Terapéuticos",
                onClick = { /* Navegar a grupos terapéuticos */ }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Información del hospital
        Text(
            text = "Hospital Central - Sistema CodeManager v1.0",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun UserInfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun NavigationButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}