package com.example.codemanager.ui.codes

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.codemanager.R
import com.example.codemanager.data.model.Code
import com.example.codemanager.data.model.Category
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

    val context = LocalContext.current
    val categories by viewModel.filteredCategoriesForSelection.collectAsState()

    // Esta lista YA viene filtrada por el ViewModel según si eliges "estante" o "refrigerador"
    val warehouses by viewModel.filteredWarehousesForSelection.collectAsState()

    val warehouseTypeFilter by viewModel.warehouseTypeFilter.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()

    var showGenerateDialog by remember { mutableStateOf(false) }
    var codeToEdit by remember { mutableStateOf<Code?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    val isAdmin = currentUser?.rol == "Administrador"

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> uri?.let { viewModel.exportData(context, it) } }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.importData(context, it) } }

    LaunchedEffect(message) {
        if (message != null) {
            snackbarHostState.showSnackbar(message!!)
            viewModel.clearMessage()
        }
    }

    // --- DIÁLOGO GENERAR ---
    if (showGenerateDialog) {
        GenerateCodeDialog(
            codeType = selectedType,
            categories = categories,
            warehouses = warehouses, // Pasamos la lista real para validar
            selectedCategory = selectedCategory,
            warehouseTypeFilter = warehouseTypeFilter,
            onWarehouseTypeFilterChange = viewModel::setWarehouseTypeFilter,
            onCategorySelected = viewModel::setSelectedCategory,
            onConfirm = { description, warehouseCode ->
                val userName = currentUser?.name ?: "Usuario Desconocido"
                // Enviamos el código validado
                viewModel.generateCode(
                    description = description,
                    createdBy = userName,
                    category = selectedCategory,
                    warehouseCodeInput = warehouseCode
                )
                showGenerateDialog = false
            },
            onDismiss = { showGenerateDialog = false }
        )
    }

    // --- DIÁLOGO EDITAR ---
    if (codeToEdit != null) {
        EditCodeDialog(
            code = codeToEdit!!,
            onDismiss = { codeToEdit = null },
            onConfirm = { newDescription ->
                val updatedCode = codeToEdit!!.copy(description = newDescription)
                viewModel.updateCode(updatedCode)
                codeToEdit = null
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Gestión de Códigos",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                if (isAdmin) {
                    IconButton(onClick = { exportLauncher.launch("Codigos_${selectedType.prefix}.csv") }) {
                        Icon(painterResource(id = R.drawable.file_export_solid_full), "Exportar", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(24.dp))
                    }
                    IconButton(onClick = { importLauncher.launch(arrayOf("text/*", "text/csv")) }) {
                        Icon(painterResource(id = R.drawable.file_import_solid_full), "Importar", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(24.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Filtros Superiores
            Text(text = "Seleccionar Categoría:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
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

            // Botón Generar
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

            // Lista de Códigos
            if (uiState.filteredCodes.isNotEmpty()) {
                Text(text = "${selectedType.label} (${uiState.filteredCodes.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.filteredCodes, key = { it.id }) { code ->
                        CodeItem(
                            code = code,
                            isAdmin = isAdmin,
                            onDelete = { viewModel.deleteCode(code.id) },
                            onEdit = { codeToEdit = code }
                        )
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

// --- DIÁLOGO GENERAR CON VALIDACIÓN ESTRICTA DE EXISTENCIA ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerateCodeDialog(
    codeType: CodeType,
    categories: List<Category>,
    warehouses: List<Warehouse>, // Lista filtrada de DB
    selectedCategory: Category?,
    warehouseTypeFilter: String,
    onWarehouseTypeFilterChange: (String) -> Unit,
    onCategorySelected: (Category) -> Unit,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var descriptionText by remember { mutableStateOf("") }
    var warehouseCodeText by remember { mutableStateOf("") }

    // Estados de error visual
    var isCategoryError by remember { mutableStateOf(false) }
    var isWarehouseError by remember { mutableStateOf(false) }
    var warehouseErrorMsg by remember { mutableStateOf("") }
    var isDescriptionError by remember { mutableStateOf(false) }

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
                    // 1. SELECCIÓN DE CATEGORÍA
                    Text("Paso 1: Categoría", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(4.dp))

                    val catLabel = if (codeType == CodeType.MEDICINES) "Categoría (Medicamentos)" else "Categoría (Descartables)"

                    Column {
                        ExposedDropdownItem(
                            label = catLabel,
                            options = categories,
                            selectedOption = selectedCategory,
                            onOptionSelected = {
                                onCategorySelected(it)
                                isCategoryError = false
                            },
                            optionText = { "${it.code} - ${it.name}" },
                            isError = isCategoryError
                        )
                        if (isCategoryError) {
                            Text("⚠ Debes seleccionar una categoría", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 16.dp, top = 2.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. TIPO DE ALMACÉN (Estante vs Refri)
                    Text("Paso 2: Almacén", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = warehouseTypeFilter == "estante",
                            onClick = {
                                // Al cambiar filtro, limpiamos el input para evitar confusión
                                if (warehouseTypeFilter != "estante") warehouseCodeText = ""
                                onWarehouseTypeFilterChange("estante")
                            },
                            label = { Text("Estantes") },
                            leadingIcon = { if (warehouseTypeFilter == "estante") Icon(Icons.Default.Check, null) }
                        )
                        FilterChip(
                            selected = warehouseTypeFilter == "refrigerador",
                            onClick = {
                                if (warehouseTypeFilter != "refrigerador") warehouseCodeText = ""
                                onWarehouseTypeFilterChange("refrigerador")
                            },
                            label = { Text("Refrigeradores") },
                            leadingIcon = { if (warehouseTypeFilter == "refrigerador") Icon(Icons.Default.Check, null) }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 3. INPUT CÓDIGO (Validación contra DB)
                    OutlinedTextField(
                        value = warehouseCodeText,
                        onValueChange = {
                            warehouseCodeText = it
                            isWarehouseError = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Código exacto (Ej: 0702)") },
                        placeholder = { Text(if(warehouseTypeFilter == "estante") "Ej: 0702" else "Ej: R-01") },
                        singleLine = true,
                        isError = isWarehouseError,
                        supportingText = {
                            if (isWarehouseError) Text(warehouseErrorMsg, color = MaterialTheme.colorScheme.error)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 4. DESCRIPCIÓN
                OutlinedTextField(
                    value = descriptionText,
                    onValueChange = {
                        descriptionText = it
                        isDescriptionError = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Descripción / Detalle") },
                    placeholder = { Text("Ej: Compra urgente...") },
                    maxLines = 3,
                    isError = isDescriptionError,
                    supportingText = {
                        if (isDescriptionError) {
                            Text("⚠ La descripción es obligatoria", color = MaterialTheme.colorScheme.error)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            var valid = true

                            // Validar Descripción
                            if (descriptionText.isBlank()) {
                                isDescriptionError = true
                                valid = false
                            }

                            // Validaciones específicas para compuestos (Med/Desc)
                            if (codeType.isComposite) {
                                if (selectedCategory == null) {
                                    isCategoryError = true
                                    valid = false
                                }

                                val inputCode = warehouseCodeText.trim() // NO hacemos uppercase forzado, respetamos lo que escribe para comparar

                                if (inputCode.isBlank()) {
                                    isWarehouseError = true
                                    warehouseErrorMsg = "⚠ Campo requerido"
                                    valid = false
                                } else {
                                    // --- VALIDACIÓN CRÍTICA ---
                                    // Buscamos ignorando mayúsculas/minúsculas para ser amigables,
                                    // pero verificamos que el código exista en la lista `warehouses` cargada de Firebase.
                                    // Como `warehouses` ya viene filtrada por tipo, si buscas un código de refri estando en estante, dará false.
                                    val exists = warehouses.any { it.code.equals(inputCode, ignoreCase = true) }

                                    if (!exists) {
                                        isWarehouseError = true
                                        warehouseErrorMsg = "⚠ Código no encontrado en BD ($warehouseTypeFilter)"
                                        valid = false
                                    }
                                }
                            }

                            if (valid) {
                                onConfirm(descriptionText, warehouseCodeText.trim())
                            }
                        }
                    ) {
                        Text("Generar")
                    }
                }
            }
        }
    }
}

// ... (EditCodeDialog, CodeItem, ExposedDropdownItem y TypeFilterChip siguen igual)

@Composable
fun EditCodeDialog(
    code: Code,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var descriptionText by remember { mutableStateOf(code.description) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Código") },
        text = {
            Column {
                Text("Código: ${code.code}", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = descriptionText,
                    onValueChange = {
                        descriptionText = it
                        isError = false
                    },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = isError,
                    supportingText = { if(isError) Text("La descripción no puede estar vacía", color = MaterialTheme.colorScheme.error) }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (descriptionText.isBlank()) {
                    isError = true
                } else {
                    onConfirm(descriptionText)
                }
            }) {
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

@Composable
fun CodeItem(
    code: Code,
    isAdmin: Boolean,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = code.code, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    if (code.description.isNotBlank()) Text(text = code.description, style = MaterialTheme.typography.bodyMedium)
                }
                if (isAdmin) {
                    Row {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.secondary)
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> ExposedDropdownItem(
    label: String,
    options: List<T>,
    selectedOption: T?,
    onOptionSelected: (T) -> Unit,
    optionText: (T) -> String,
    isError: Boolean = false
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
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            isError = isError
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
fun TypeFilterChip(type: CodeType, selectedType: CodeType, onSelect: (CodeType) -> Unit) {
    FilterChip(
        selected = type == selectedType,
        onClick = { onSelect(type) },
        label = { Text(type.label) },
        modifier = Modifier.wrapContentWidth()
    )
}