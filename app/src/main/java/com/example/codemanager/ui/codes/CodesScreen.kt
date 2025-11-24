package com.example.codemanager.ui.codes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check // Icono check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.codemanager.data.model.Code
import com.example.codemanager.data.model.TherapeuticGroup
import com.example.codemanager.data.model.Warehouse
import com.example.codemanager.data.repository.CodeRepository
import com.example.codemanager.ui.auth.AuthViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodesScreen(
    authViewModel: AuthViewModel,
    viewModel: CodesViewModel = viewModel(factory = CodesViewModelFactory(CodeRepository()))
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedType by viewModel.selectedType.collectAsState()
    val message by viewModel.message.collectAsState()

    val groups by viewModel.groups.collectAsState()
    // OJO: Ahora observamos la lista FILTRADA, no todas las warehouses
    val warehouses by viewModel.filteredWarehousesForSelection.collectAsState()

    // Nuevo estado para el filtro
    val warehouseTypeFilter by viewModel.warehouseTypeFilter.collectAsState()

    val selectedGroup by viewModel.selectedGroup.collectAsState()
    val selectedWarehouse by viewModel.selectedWarehouse.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()

    var showGenerateDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        if (message != null) {
            snackbarHostState.showSnackbar(message!!)
            viewModel.clearMessage()
        }
    }

    if (showGenerateDialog) {
        GenerateCodeDialog(
            codeType = selectedType,
            groups = groups,
            warehouses = warehouses, // Pasamos la lista ya filtrada
            selectedGroup = selectedGroup,
            selectedWarehouse = selectedWarehouse,
            warehouseTypeFilter = warehouseTypeFilter, // Pasamos el filtro actual
            onWarehouseTypeFilterChange = viewModel::setWarehouseTypeFilter, // Acción para cambiar filtro
            onGroupSelected = viewModel::setSelectedGroup,
            onWarehouseSelected = viewModel::setSelectedWarehouse,
            onConfirm = { description ->
                val userName = currentUser?.name ?: "Usuario Desconocido"
                viewModel.generateCode(description.ifBlank { "Sin descripción" }, userName)
                showGenerateDialog = false
            },
            onDismiss = { showGenerateDialog = false }
        )
    }

    // ... (El Scaffold y el resto de la UI principal se mantienen IGUAL que antes) ...
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text("Gestión de Códigos", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))

            Text("Seleccionar Categoría:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TypeFilterChip(CodeType.EMERGENCY, selectedType, viewModel::selectType)
                TypeFilterChip(CodeType.SERVICES, selectedType, viewModel::selectType)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TypeFilterChip(CodeType.MEDICINES, selectedType, viewModel::selectType)
                TypeFilterChip(CodeType.DISPOSABLES, selectedType, viewModel::selectType)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { showGenerateDialog = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Procesando...")
                } else {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generar Código ${selectedType.label}")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.filteredCodes.isNotEmpty()) {
                Text("${selectedType.label} (${uiState.filteredCodes.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.filteredCodes, key = { it.id }) { code ->
                        CodeItem(code = code, onDelete = { viewModel.deleteCode(code.id) })
                    }
                }
            } else if (!uiState.isLoading) {
                Box(modifier = Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay códigos registrados", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Box(modifier = Modifier.weight(1f).fillMaxSize())
            }
        }
    }
}

// ... (TypeFilterChip sigue igual) ...
@Composable
fun TypeFilterChip(type: CodeType, selectedType: CodeType, onSelect: (CodeType) -> Unit) {
    FilterChip(
        selected = type == selectedType,
        onClick = { onSelect(type) },
        label = { Text(type.label) },
        modifier = Modifier.wrapContentWidth()
    )
}

// --- ACTUALIZADO: GENERATE CODE DIALOG CON FILTRO ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerateCodeDialog(
    codeType: CodeType,
    groups: List<TherapeuticGroup>,
    warehouses: List<Warehouse>,
    selectedGroup: TherapeuticGroup?,
    selectedWarehouse: Warehouse?,
    warehouseTypeFilter: String, // Nuevo parámetro
    onWarehouseTypeFilterChange: (String) -> Unit, // Nuevo parámetro
    onGroupSelected: (TherapeuticGroup) -> Unit,
    onWarehouseSelected: (Warehouse) -> Unit,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var descriptionText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Nuevo Código: ${codeType.label}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (codeType.isComposite) {
                    Text("Configuración:", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(8.dp))

                    // 1. Grupo Terapéutico
                    ExposedDropdownItem(
                        label = "Grupo Terapéutico (00)",
                        options = groups,
                        selectedOption = selectedGroup,
                        onOptionSelected = onGroupSelected,
                        optionText = { "${it.code} - ${it.name}" }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // --- NUEVO: SELECTOR DE TIPO DE ALMACÉN ---
                    Text("Tipo de Almacén:", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = warehouseTypeFilter == "estante",
                            onClick = { onWarehouseTypeFilterChange("estante") },
                            label = { Text("Estantes") },
                            leadingIcon = { if (warehouseTypeFilter == "estante") Icon(Icons.Default.Check, null) }
                        )
                        FilterChip(
                            selected = warehouseTypeFilter == "refrigerador",
                            onClick = { onWarehouseTypeFilterChange("refrigerador") },
                            label = { Text("Refrigeradores") },
                            leadingIcon = { if (warehouseTypeFilter == "refrigerador") Icon(Icons.Default.Check, null) }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 2. Dropdown Almacén (Ahora muestra solo los filtrados)
                    ExposedDropdownItem(
                        label = "Seleccionar ${if(warehouseTypeFilter == "estante") "Estante" else "Refrigerador"}",
                        options = warehouses,
                        selectedOption = selectedWarehouse,
                        onOptionSelected = onWarehouseSelected,
                        optionText = { "${it.code} - ${it.name}" }
                    )

                    // Mensaje de ayuda si no hay items
                    if (warehouses.isEmpty()) {
                        Text(
                            text = "No hay ${if(warehouseTypeFilter == "estante") "estantes" else "refrigeradores"} registrados.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                OutlinedTextField(
                    value = descriptionText,
                    onValueChange = { descriptionText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Descripción / Detalle") },
                    placeholder = { Text("Ej: Compra urgente...") },
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    Spacer(modifier = Modifier.width(8.dp))

                    val isValid = if (codeType.isComposite) {
                        selectedGroup != null && selectedWarehouse != null
                    } else true

                    Button(onClick = { onConfirm(descriptionText) }, enabled = isValid) {
                        Text("Generar")
                    }
                }
            }
        }
    }
}

// ... (ExposedDropdownItem y CodeItem siguen igual) ...
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> ExposedDropdownItem(
    label: String,
    options: List<T>,
    selectedOption: T?,
    onOptionSelected: (T) -> Unit,
    optionText: (T) -> String
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = if (selectedOption != null) optionText(selectedOption) else "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(text = optionText(option)) },
                    onClick = { onOptionSelected(option); expanded = false }
                )
            }
        }
    }
}

@Composable
fun CodeItem(code: Code, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = code.code, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    if (code.description.isNotBlank()) Text(text = code.description, style = MaterialTheme.typography.bodyMedium)
                }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error) }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Por: ${code.createdBy}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (code.createdAt > 0) {
                    val dateFormat = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
                    Text(text = dateFormat.format(Date(code.createdAt)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}