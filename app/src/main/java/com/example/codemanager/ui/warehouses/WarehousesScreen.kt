package com.example.codemanager.ui.warehouses

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.codemanager.data.model.Warehouse
import com.example.codemanager.data.repository.WarehouseRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarehousesScreen(
    viewModel: WarehousesViewModel = viewModel(factory = WarehousesViewModelFactory(WarehouseRepository()))
) {
    val uiState by viewModel.uiState.collectAsState()
    val message by viewModel.message.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showBulkConfirmDialog by remember { mutableStateOf(false) }

    // Manejo de mensajes temporales
    LaunchedEffect(message) {
        if (message != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearMessage()
        }
    }

    // --- DIÁLOGOS ---

    if (showAddDialog) {
        AddWarehouseDialog(
            selectedType = uiState.selectedType,
            nextLocation = uiState.nextAvailableLocation,
            onDismiss = { showAddDialog = false },
            onConfirm = { name ->
                viewModel.generateNewWarehouse(name)
                showAddDialog = false
            }
        )
    }

    if (showBulkConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showBulkConfirmDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("¿Llenar todos los espacios?") },
            text = {
                val typeName = Warehouse.getTypeDisplayName(uiState.selectedType)
                Text("Esta acción creará automáticamente todos los $typeName faltantes hasta completar la capacidad máxima (300 items).\n\nEsto puede generar muchos registros de una sola vez.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBulkConfirmDialog = false
                        viewModel.generateAllRemainingWarehouses()
                    }
                ) {
                    Text("Confirmar Llenado")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (uiState.showEditDialog && uiState.selectedWarehouse != null) {
        EditWarehouseDialog(
            warehouse = uiState.selectedWarehouse!!,
            onDismiss = { viewModel.hideEditDialog() },
            onConfirm = { warehouse ->
                viewModel.updateWarehouse(warehouse.id, warehouse)
            }
        )
    }

    // --- UI PRINCIPAL ---

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Título
            Text(
                text = "Gestión de Almacenes",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Selector de Tipo (Estante / Refrigerador)
            WarehouseTypeSelector(
                selectedType = uiState.selectedType,
                onTypeSelected = viewModel::setSelectedType
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- BOTONERA ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Botón Agregar Individual
                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isLoading && uiState.nextAvailableLocation != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Default.Add, contentDescription = "Agregar")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Nuevo")
                    }
                }

                // Botón Llenar Todo (Masivo)
                OutlinedButton(
                    onClick = { showBulkConfirmDialog = true },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isLoading && uiState.nextAvailableLocation != null,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    // Usamos PlaylistAdd que es común, o AutoMode si tienes las dependencias extendidas
                    Icon(Icons.Default.PlaylistAdd, contentDescription = "Llenar Todo")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Llenar Todo")
                }
            }

            // Aviso si no hay espacio
            if (uiState.nextAvailableLocation == null && !uiState.isLoading) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(
                        text = "⚠️ Almacenamiento lleno. No hay ubicaciones disponibles.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mensaje de éxito/error (Banner)
            if (message != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = message!!,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Lista de Almacenes
            if (uiState.isLoading && uiState.filteredWarehouses.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.filteredWarehouses.isNotEmpty()) {
                Text(
                    text = "${Warehouse.getTypeDisplayName(uiState.selectedType)}s (${uiState.filteredWarehouses.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.filteredWarehouses, key = { it.id }) { warehouse ->
                        WarehouseItem(
                            warehouse = warehouse,
                            onEdit = { viewModel.showEditDialog(warehouse) },
                            onDelete = { viewModel.deleteWarehouse(warehouse.id) }
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.surfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No hay registros",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// --- COMPONENTES AUXILIARES ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarehouseTypeSelector(
    selectedType: String,
    onTypeSelected: (String) -> Unit
) {
    Column {
        Text(
            text = "Tipo de Almacenamiento:",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Warehouse.WAREHOUSE_TYPES.forEach { type ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = { onTypeSelected(type) },
                    label = { Text(Warehouse.getTypeDisplayName(type)) },
                    leadingIcon = {
                        if (selectedType == type) Icon(Icons.Default.Check, null)
                    }
                )
            }
        }
    }
}

@Composable
fun WarehouseItem(
    warehouse: Warehouse,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = warehouse.getDisplayCode(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = warehouse.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = warehouse.getFormattedLocation(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, "Editar", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, "Eliminar", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar Almacén") },
            text = { Text("¿Estás seguro? Se eliminará la referencia de ubicación.") },
            confirmButton = {
                Button(
                    onClick = { onDelete(); showDeleteDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
fun AddWarehouseDialog(
    selectedType: String,
    nextLocation: Pair<Int, Int>?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Nuevo ${Warehouse.getTypeDisplayName(selectedType)}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (nextLocation != null) {
                    val (level, itemNumber) = nextLocation
                    val nextCode = Warehouse.generateCode(level, itemNumber)

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Ubicación Sugerida:", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = nextCode,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Divider(color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Identificador", style = MaterialTheme.typography.labelSmall)
                                    Text(itemNumber.toString().padStart(2, '0'), style = MaterialTheme.typography.titleMedium)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Nivel", style = MaterialTheme.typography.labelSmall)
                                    Text(level.toString().padStart(2, '0'), style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre descriptivo *") },
                    placeholder = { Text("Ej: Lado Izquierdo, Zona A") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(name) },
                        enabled = name.isNotBlank() && nextLocation != null
                    ) {
                        Text("Crear")
                    }
                }
            }
        }
    }
}

@Composable
fun EditWarehouseDialog(
    warehouse: Warehouse,
    onDismiss: () -> Unit,
    onConfirm: (Warehouse) -> Unit
) {
    var name by remember { mutableStateOf(warehouse.name) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Editar Almacén",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Código: ${warehouse.code}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(warehouse.copy(name = name)) },
                        enabled = name.isNotBlank()
                    ) {
                        Text("Guardar")
                    }
                }
            }
        }
    }
}