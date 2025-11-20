package com.example.codemanager.ui.warehouses

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun WarehousesScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Gestión de Almacenes",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Administra almacenes, refrigeradores y áreas controladas",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Ejemplo de tipos de almacenes
        val warehouseTypes = listOf(
            "📚 Almacén de Estantes",
            "❄️ Almacén Refrigerado",
            "🔒 Almacén Controlado",
            "📦 Almacén General"
        )

        warehouseTypes.forEach { warehouse ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                onClick = { /* TODO: Navegar a detalles del almacén */ }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = warehouse,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}