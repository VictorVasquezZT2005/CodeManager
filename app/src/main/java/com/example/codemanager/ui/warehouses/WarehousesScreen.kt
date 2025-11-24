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

@Composable
fun WarehousesScreen(
    viewModel: WarehousesViewModel = viewModel(factory = WarehousesViewModelFactory())
) {
    val uiState by viewModel.uiState.collectAsState()
    val message by viewModel.message.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        if (message != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearMessage()
        }
    }

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

    if (uiState.showEditDialog && uiState.selectedWarehouse != null) {
        EditWarehouseDialog(
            warehouse = uiState.selectedWarehouse!!,
            onDismiss = { viewModel.hideEditDialog() },
            onConfirm = { warehouse ->
                viewModel.updateWarehouse(warehouse.id, warehouse)
            }
        )
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Gestión de Almacenes",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Selector de tipo
            WarehouseTypeSelector(
                selectedType = uiState.selectedType,
                onTypeSelected = viewModel::setSelectedType
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Botón para agregar
            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.fillMaxWidth(),
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
                    Icon(Icons.Default.Add, contentDescription = "Agregar almacén")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Agregar ${Warehouse.getTypeDisplayName(uiState.selectedType)}")
            }

            if (uiState.nextAvailableLocation == null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "⚠️ No hay espacios disponibles para ${Warehouse.getTypeDisplayName(uiState.selectedType)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mensaje
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

            // Lista
            if (uiState.isLoading && uiState.filteredWarehouses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
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
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hay ${Warehouse.getTypeDisplayName(uiState.selectedType).lowercase()}s registrados",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun WarehouseTypeSelector(
    selectedType: String,
    onTypeSelected: (String) -> Unit
) {
    Column {
        Text(
            text = "Seleccionar tipo de almacén:",
            style = MaterialTheme.typography.titleSmall,
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
                    label = { Text(Warehouse.getTypeDisplayName(type)) }
                )
            }
        }
    }
}

@Composable
fun WarehouseFilters(
    searchQuery: String,
    selectedLevel: Int?,
    onSearchChange: (String) -> Unit,
    onLevelFilterChange: (Int?) -> Unit
) {
    Column {
        Text(
            text = "Buscar almacenes:",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Buscar por código o nombre...") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Filtrar por nivel:",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (level in 1..5) {
                FilterChip(
                    selected = selectedLevel == level,
                    onClick = {
                        if (selectedLevel == level) {
                            onLevelFilterChange(null)
                        } else {
                            onLevelFilterChange(level)
                        }
                    },
                    label = { Text("Nivel $level") }
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
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = warehouse.getFormattedLocation(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Edit, "Editar", tint = MaterialTheme.colorScheme.primary)
                    }

                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Delete, "Eliminar", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar almacén") },
            text = { Text("¿Estás seguro de que deseas eliminar este almacén?") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteDialog = false }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Agregar ${Warehouse.getTypeDisplayName(selectedType)}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                nextLocation?.let { (level, itemNumber) ->
                    val nextCode = Warehouse.generateCode(level, itemNumber)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Ubicación asignada automáticamente:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Código: $nextCode",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Divider(modifier = Modifier.padding(vertical = 8.dp))

                            // --- CORRECCIÓN VISUAL: Invertido para coincidir con la lógica ---
                            Text(text = "${Warehouse.getTypeDisplayName(selectedType)}: ${itemNumber.toString().padStart(2, '0')}")
                            Text(text = "Nivel: ${level.toString().padStart(2, '0')}")
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                val isFormValid = name.isNotBlank() && nextLocation != null

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { onConfirm(name) },
                        enabled = isFormValid
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Editar ${Warehouse.getTypeDisplayName(warehouse.type)}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                val isFormValid = name.isNotBlank()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val updatedWarehouse = warehouse.copy(name = name)
                            onConfirm(updatedWarehouse)
                        },
                        enabled = isFormValid
                    ) {
                        Text("Guardar")
                    }
                }
            }
        }
    }
}