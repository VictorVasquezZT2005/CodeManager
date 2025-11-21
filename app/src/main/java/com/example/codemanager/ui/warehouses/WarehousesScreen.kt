// ui/warehouses/WarehousesScreen.kt
package com.example.codemanager.ui.warehouses

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import com.example.codemanager.data.model.Warehouse

@Composable
fun WarehousesScreen(
    viewModel: WarehousesViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedType by viewModel.selectedType.collectAsState()
    val message by viewModel.message.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }

    // Efectos
    LaunchedEffect(Unit) {
        viewModel.loadAllWarehouses()
    }

    LaunchedEffect(message) {
        if (message != null) {
            delay(5000)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Crear almacén")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Header
            Text(
                text = "Almacenes",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Código: Estante (EE) - Nivel (NN)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Selector de tipo
            WarehouseTypeSelector(
                selectedType = selectedType,
                onTypeSelected = viewModel::setSelectedType
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Mensajes
            if (message != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            message!!.contains("✅") -> MaterialTheme.colorScheme.primaryContainer
                            message!!.contains("❌") -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Text(
                        text = message!!,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Contenido
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Cargando almacenes...")
                        }
                    }
                }
                uiState.warehouses.isNotEmpty() -> {
                    Text(
                        text = "Almacenes ${getTypeDisplayName(selectedType)} (${uiState.warehouses.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.warehouses, key = { it.id }) { warehouse ->
                            WarehouseItem(
                                warehouse = warehouse,
                                onDelete = { viewModel.deleteWarehouse(warehouse.id) }
                            )
                        }
                    }
                }
                else -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Storage,
                                contentDescription = "Sin almacenes",
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No hay almacenes ${getTypeDisplayName(selectedType)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }

    // Diálogo
    if (showCreateDialog) {
        CreateWarehouseDialog(
            selectedType = selectedType,
            viewModel = viewModel,
            onDismiss = { showCreateDialog = false }
        )
    }
}

@Composable
fun WarehouseTypeSelector(
    selectedType: String,
    onTypeSelected: (String) -> Unit
) {
    Column {
        Text(
            text = "Tipo de Almacén:",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("refrigerado", "controlado", "estante").forEach { type ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = { onTypeSelected(type) },
                    label = {
                        Text(
                            text = getTypeDisplayName(type),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun CreateWarehouseDialog(
    selectedType: String,
    viewModel: WarehousesViewModel,
    onDismiss: () -> Unit
) {
    var shelfNumber by remember { mutableStateOf("1") }
    var levelNumber by remember { mutableStateOf("1") }
    var warehouseName by remember { mutableStateOf("") }
    var warehouseDescription by remember { mutableStateOf("") }
    var temperature by remember { mutableStateOf("") }
    var humidity by remember { mutableStateOf("") }
    var capacity by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Crear Almacén ${getTypeDisplayName(selectedType)}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Información del código
                Text(
                    text = "Código generado: ${shelfNumber.padStart(2, '0')}${levelNumber.padStart(2, '0')}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Campos de número de estante y nivel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = shelfNumber,
                        onValueChange = { newValue ->
                            if (newValue.all { it.isDigit() } && newValue.toIntOrNull() in 1..99) {
                                shelfNumber = newValue
                            }
                        },
                        modifier = Modifier.weight(1f),
                        label = { Text("N° Estante (1-99)") },
                        placeholder = { Text("1") },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = levelNumber,
                        onValueChange = { newValue ->
                            if (newValue.all { it.isDigit() } && newValue.toIntOrNull() in 1..99) {
                                levelNumber = newValue
                            }
                        },
                        modifier = Modifier.weight(1f),
                        label = { Text("N° Nivel (1-99)") },
                        placeholder = { Text("1") },
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = warehouseName,
                    onValueChange = { warehouseName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nombre del almacén *") },
                    placeholder = { Text("Ej: Almacén Principal ${getTypeDisplayName(selectedType)}") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = warehouseDescription,
                    onValueChange = { warehouseDescription = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Descripción") },
                    placeholder = { Text("Ej: Almacén para ${getTypeDescription(selectedType)}") },
                    singleLine = false,
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Campos específicos según el tipo
                when (selectedType) {
                    "refrigerado" -> {
                        OutlinedTextField(
                            value = temperature,
                            onValueChange = { temperature = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Temperatura (°C) *") },
                            placeholder = { Text("Ej: 2-8") },
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    "controlado" -> {
                        OutlinedTextField(
                            value = humidity,
                            onValueChange = { humidity = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Humedad (%) *") },
                            placeholder = { Text("Ej: 40-60") },
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                OutlinedTextField(
                    value = capacity,
                    onValueChange = { capacity = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Capacidad") },
                    placeholder = { Text("Ej: 100 unidades, 50 cajas") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Botones de acción
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val shelf = shelfNumber.toIntOrNull() ?: 1
                            val level = levelNumber.toIntOrNull() ?: 1

                            if (warehouseName.isNotBlank() && shelf in 1..99 && level in 1..99) {
                                viewModel.createWarehouse(
                                    shelfNumber = shelf,
                                    levelNumber = level,
                                    name = warehouseName,
                                    description = warehouseDescription,
                                    type = selectedType,
                                    temperature = if (temperature.isNotBlank()) temperature else null,
                                    humidity = if (humidity.isNotBlank()) humidity else null,
                                    capacity = if (capacity.isNotBlank()) capacity else null,
                                    createdBy = "current_user_id"
                                )
                            }
                            onDismiss()
                        },
                        enabled = warehouseName.isNotBlank() &&
                                shelfNumber.toIntOrNull() in 1..99 &&
                                levelNumber.toIntOrNull() in 1..99 &&
                                when (selectedType) {
                                    "refrigerado" -> temperature.isNotBlank()
                                    "controlado" -> humidity.isNotBlank()
                                    else -> true
                                }
                    ) {
                        Text("Crear Almacén")
                    }
                }
            }
        }
    }
}

@Composable
fun WarehouseItem(
    warehouse: Warehouse,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Almacén ${warehouse.code}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Estante ${warehouse.shelfNumber.toString().padStart(2, '0')} - Nivel ${warehouse.levelNumber.toString().padStart(2, '0')}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = warehouse.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    if (warehouse.description.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = warehouse.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Tipo: ${getTypeDisplayName(warehouse.type)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    when (warehouse.type) {
                        "refrigerado" -> {
                            warehouse.temperature?.let { temp ->
                                Text(
                                    text = "Temperatura: $temp°C",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        "controlado" -> {
                            warehouse.humidity?.let { hum ->
                                Text(
                                    text = "Humedad: $hum%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    warehouse.capacity?.let { cap ->
                        Text(
                            text = "Capacidad: $cap",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (warehouse.createdAt > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Creado: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(java.util.Date(warehouse.createdAt))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar almacén",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

// Funciones helper
private fun getTypeDisplayName(type: String): String {
    return when (type) {
        "refrigerado" -> "Refrigerado"
        "controlado" -> "Controlado"
        "estante" -> "Estante"
        else -> type
    }
}

private fun getTypeDescription(type: String): String {
    return when (type) {
        "refrigerado" -> "productos refrigerados"
        "controlado" -> "productos con control de humedad"
        "estante" -> "productos de estantería"
        else -> "productos"
    }
}