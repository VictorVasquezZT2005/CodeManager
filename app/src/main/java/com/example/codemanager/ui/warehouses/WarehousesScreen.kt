package com.example.codemanager.ui.warehouses

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.codemanager.data.model.Warehouse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarehousesScreen(
    viewModel: WarehousesViewModel = viewModel(factory = WarehousesViewModelFactory())
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Almacenes") },
                actions = {
                    IconButton(onClick = { viewModel.showAddDialog() }) {
                        Icon(Icons.Default.Add, "Agregar almacén")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddDialog() }
            ) {
                Icon(Icons.Default.Add, "Agregar")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Barra de búsqueda y filtros
            SearchAndFiltersSection(
                searchQuery = uiState.searchQuery,
                selectedType = uiState.selectedType,
                selectedLevel = uiState.selectedLevel,
                onSearchChange = { viewModel.searchWarehouses(it) },
                onTypeFilterChange = { viewModel.filterByType(it) },
                onLevelFilterChange = { viewModel.filterByLevel(it) }
            )

            // Lista de almacenes
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.error != null) {
                ErrorMessage(
                    message = uiState.error!!,
                    onDismiss = { viewModel.clearError() }
                )
            } else {
                WarehousesList(
                    warehouses = uiState.filteredWarehouses,
                    onWarehouseClick = { viewModel.showEditDialog(it) },
                    onDeleteClick = { viewModel.deleteWarehouse(it.id) }
                )
            }
        }
    }

    // Diálogos
    if (uiState.showAddDialog) {
        AddWarehouseDialog(
            onDismiss = { viewModel.hideAddDialog() },
            onConfirm = { warehouse ->
                viewModel.createWarehouse(warehouse)
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

    // Mensajes de éxito
    uiState.successMessage?.let { message ->
        LaunchedEffect(message) {
            kotlinx.coroutines.delay(2000)
            viewModel.clearSuccessMessage()
        }
        Snackbar(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(message)
        }
    }
}

@Composable
fun SearchAndFiltersSection(
    searchQuery: String,
    selectedType: String?,
    selectedLevel: Int?,
    onSearchChange: (String) -> Unit,
    onTypeFilterChange: (String?) -> Unit,
    onLevelFilterChange: (Int?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Búsqueda
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Buscar por código, nombre o tipo...") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Close, "Limpiar")
                    }
                }
            },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Filtros
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Filtro por tipo
            FilterChip(
                selected = selectedType != null,
                onClick = {
                    if (selectedType != null) {
                        onTypeFilterChange(null)
                    } else {
                        // Aquí podrías mostrar un diálogo para seleccionar el tipo
                        onTypeFilterChange("estante")
                    }
                },
                label = {
                    Text(selectedType ?: "Tipo")
                },
                leadingIcon = {
                    Icon(
                        if (selectedType != null) Icons.Default.CheckCircle else Icons.Default.FilterList,
                        null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )

            // Filtro por nivel
            FilterChip(
                selected = selectedLevel != null,
                onClick = {
                    if (selectedLevel != null) {
                        onLevelFilterChange(null)
                    } else {
                        onLevelFilterChange(1)
                    }
                },
                label = {
                    Text(selectedLevel?.let { "Nivel $it" } ?: "Nivel")
                },
                leadingIcon = {
                    Icon(
                        if (selectedLevel != null) Icons.Default.CheckCircle else Icons.Default.FilterList,
                        null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }
    }
}

@Composable
fun WarehousesList(
    warehouses: List<Warehouse>,
    onWarehouseClick: (Warehouse) -> Unit,
    onDeleteClick: (Warehouse) -> Unit
) {
    if (warehouses.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Inventory2,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "No hay almacenes",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(warehouses) { warehouse ->
                WarehouseCard(
                    warehouse = warehouse,
                    onClick = { onWarehouseClick(warehouse) },
                    onDelete = { onDeleteClick(warehouse) }
                )
            }
        }
    }
}

@Composable
fun WarehouseCard(
    warehouse: Warehouse,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Encabezado con código y acciones
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = warehouse.code,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = warehouse.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tipo y ubicación
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoChip(
                    icon = Icons.Default.Category,
                    label = warehouse.type,
                    color = getTypeColor(warehouse.type)
                )

                InfoChip(
                    icon = Icons.Default.LocationOn,
                    label = warehouse.getFormattedLocation(),
                    color = MaterialTheme.colorScheme.secondaryContainer
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Capacidad
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.Inventory,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Capacidad: ${warehouse.capacity}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Condiciones ambientales
            if (warehouse.hasEnvironmentalControl()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    warehouse.temperature?.let { temp ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Thermostat,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                "$temp°C",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    warehouse.humidity?.let { hum ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.WaterDrop,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF2196F3)
                            )
                            Text(
                                "$hum%",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // Descripción
            if (warehouse.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    warehouse.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar almacén") },
            text = { Text("¿Estás seguro de que deseas eliminar este almacén?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
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
fun InfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(color)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
fun getTypeColor(type: String): Color {
    return when (type.lowercase()) {
        "estante" -> Color(0xFFE3F2FD)
        "bodega" -> Color(0xFFFFF3E0)
        "refrigerado" -> Color(0xFFE0F2F1)
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
}

@Composable
fun ErrorMessage(message: String, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Cerrar",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
fun AddWarehouseDialog(
    onDismiss: () -> Unit,
    onConfirm: (Warehouse) -> Unit
) {
    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("estante") }
    var description by remember { mutableStateOf("") }
    var capacity by remember { mutableStateOf("") }
    var levelNumber by remember { mutableStateOf("1") }
    var shelfNumber by remember { mutableStateOf("1") }
    var temperature by remember { mutableStateOf("") }
    var humidity by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar Almacén") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("Código *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = type,
                        onValueChange = { type = it },
                        label = { Text("Tipo *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("estante, bodega, refrigerado...") }
                    )
                }

                item {
                    OutlinedTextField(
                        value = capacity,
                        onValueChange = { capacity = it },
                        label = { Text("Capacidad *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = levelNumber,
                            onValueChange = { levelNumber = it },
                            label = { Text("Nivel") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = shelfNumber,
                            onValueChange = { shelfNumber = it },
                            label = { Text("Estante") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Descripción") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 3
                    )
                }

                item {
                    Text(
                        "Condiciones Ambientales (Opcional)",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = temperature,
                            onValueChange = { temperature = it },
                            label = { Text("Temp. °C") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = humidity,
                            onValueChange = { humidity = it },
                            label = { Text("Humedad %") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (code.isNotBlank() && name.isNotBlank() && capacity.isNotBlank()) {
                        val warehouse = Warehouse(
                            code = code,
                            name = name,
                            type = type,
                            description = description,
                            capacity = capacity,
                            levelNumber = levelNumber.toIntOrNull() ?: 1,
                            shelfNumber = shelfNumber.toIntOrNull() ?: 1,
                            temperature = temperature.toDoubleOrNull(),
                            humidity = humidity.toDoubleOrNull(),
                            createdBy = "current_user_id"
                        )
                        onConfirm(warehouse)
                    }
                },
                enabled = code.isNotBlank() && name.isNotBlank() && capacity.isNotBlank()
            ) {
                Text("Crear")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun EditWarehouseDialog(
    warehouse: Warehouse,
    onDismiss: () -> Unit,
    onConfirm: (Warehouse) -> Unit
) {
    var code by remember { mutableStateOf(warehouse.code) }
    var name by remember { mutableStateOf(warehouse.name) }
    var type by remember { mutableStateOf(warehouse.type) }
    var description by remember { mutableStateOf(warehouse.description) }
    var capacity by remember { mutableStateOf(warehouse.capacity) }
    var levelNumber by remember { mutableStateOf(warehouse.levelNumber.toString()) }
    var shelfNumber by remember { mutableStateOf(warehouse.shelfNumber.toString()) }
    var temperature by remember { mutableStateOf(warehouse.temperature?.toString() ?: "") }
    var humidity by remember { mutableStateOf(warehouse.humidity?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Almacén") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("Código *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = type,
                        onValueChange = { type = it },
                        label = { Text("Tipo *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = capacity,
                        onValueChange = { capacity = it },
                        label = { Text("Capacidad *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = levelNumber,
                            onValueChange = { levelNumber = it },
                            label = { Text("Nivel") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = shelfNumber,
                            onValueChange = { shelfNumber = it },
                            label = { Text("Estante") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Descripción") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 3
                    )
                }

                item {
                    Text(
                        "Condiciones Ambientales",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = temperature,
                            onValueChange = { temperature = it },
                            label = { Text("Temp. °C") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = humidity,
                            onValueChange = { humidity = it },
                            label = { Text("Humedad %") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (code.isNotBlank() && name.isNotBlank() && capacity.isNotBlank()) {
                        val updatedWarehouse = warehouse.copy(
                            code = code,
                            name = name,
                            type = type,
                            description = description,
                            capacity = capacity,
                            levelNumber = levelNumber.toIntOrNull() ?: 1,
                            shelfNumber = shelfNumber.toIntOrNull() ?: 1,
                            temperature = temperature.toDoubleOrNull(),
                            humidity = humidity.toDoubleOrNull()
                        )
                        onConfirm(updatedWarehouse)
                    }
                },
                enabled = code.isNotBlank() && name.isNotBlank() && capacity.isNotBlank()
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}