package xyz.zt.codemanager.ui.codes

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import xyz.zt.codemanager.R
import xyz.zt.codemanager.data.model.Code
import xyz.zt.codemanager.data.model.Category
import xyz.zt.codemanager.data.model.Warehouse
import xyz.zt.codemanager.data.repository.CodeRepository
import xyz.zt.codemanager.ui.auth.AuthViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// --- FUNCIÓN DE UTILIDAD PARA RECONSTRUIR EL CÓDIGO VISUAL ---
fun updateCodeString(oldFullCode: String, newCategory: String, newWarehouse: String): String {
    val parts = oldFullCode.split("-").toMutableList()
    // Si el formato es TIPO-CATEGORIA-ALMACEN-CORRELATIVO (ej: 00-17-1203-0006)
    if (parts.size >= 4) {
        parts[1] = newCategory
        parts[2] = newWarehouse
        return parts.joinToString("-")
    }
    return oldFullCode
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodesScreen(
    authViewModel: AuthViewModel,
    viewModel: CodesViewModel = viewModel(factory = CodesViewModelFactory(CodeRepository()))
) {
    // Collect specific flows to avoid full-screen recompositions
    val isLoading by viewModel.isLoading.collectAsState(initial = false)
    val filteredCodes by viewModel.filteredCodes.collectAsState(initial = emptyList())
    val selectedType by viewModel.selectedType.collectAsState(initial = CodeType.EMERGENCY)
    val searchQuery by viewModel.searchQuery.collectAsState(initial = "")
    val filterCategory by viewModel.filterCategory.collectAsState(initial = null)
    val selectedCategory by viewModel.selectedCategory.collectAsState(initial = null)
    val warehouseTypeFilter by viewModel.warehouseTypeFilter.collectAsState(initial = "estante")
    val message by viewModel.message.collectAsState(initial = null)

    val categories by viewModel.filteredCategoriesForSelection.collectAsState()
    val warehouses by viewModel.filteredWarehousesForSelection.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val isAdmin = remember(currentUser) { currentUser?.rol == "Administrador" }

    var showGenerateDialog by remember { mutableStateOf(false) }
    var codeToEdit by remember { mutableStateOf<Code?>(null) }
    var codeToDelete by remember { mutableStateOf<Code?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> uri?.let { viewModel.exportData(context, it) } }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.importData(context, it) } }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    // --- DIÁLOGOS ---
    if (showGenerateDialog) {
        GenerateCodeDialog(
            codeType = selectedType,
            categories = categories,
            warehouses = warehouses,
            selectedCategory = selectedCategory,
            warehouseTypeFilter = warehouseTypeFilter,
            onWarehouseTypeFilterChange = viewModel::setWarehouseTypeFilter,
            onCategorySelected = viewModel::setSelectedCategory,
            onConfirm = { description, warehouseCode ->
                val userName = currentUser?.name ?: "Usuario Desconocido"
                viewModel.generateCode(description, userName, selectedCategory, warehouseCode)
                showGenerateDialog = false
            },
            onDismiss = { showGenerateDialog = false }
        )
    }

    if (codeToEdit != null) {
        EditCodeDialog(
            code = codeToEdit!!,
            categories = categories,
            warehouses = warehouses,
            onDismiss = { codeToEdit = null },
            onConfirm = { newDescription, newCategory, newWarehouseCode ->
                val categoryCode = newCategory?.code ?: codeToEdit!!.categoryCode
                val updatedFullCode = if (codeToEdit!!.categoryCode.isNotEmpty()) {
                    updateCodeString(codeToEdit!!.code, categoryCode, newWarehouseCode)
                } else {
                    codeToEdit!!.code
                }

                viewModel.updateCode(codeToEdit!!.copy(
                    code = updatedFullCode,
                    description = newDescription,
                    categoryCode = categoryCode,
                    warehouseCode = newWarehouseCode
                ))
                codeToEdit = null
            }
        )
    }

    if (codeToDelete != null) {
        AlertDialog(
            onDismissRequest = { codeToDelete = null },
            title = { Text("Eliminar Código") },
            text = { Text("¿Estás seguro de que deseas eliminar el código ${codeToDelete!!.code}? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCode(codeToDelete!!.id)
                        codeToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { codeToDelete = null }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CodesTopBar(
                isAdmin = isAdmin,
                selectedType = selectedType,
                onExport = { exportLauncher.launch("Codigos_${selectedType.prefix}.csv") },
                onImport = { importLauncher.launch(arrayOf("text/*", "text/csv")) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            TypeSelectionSection(
                selectedType = selectedType,
                onTypeSelected = viewModel::selectType
            )

            Spacer(modifier = Modifier.height(16.dp))

            SearchBar(
                query = searchQuery,
                onQueryChange = viewModel::onSearchQueryChanged
            )

            if (selectedType.isComposite) {
                Spacer(modifier = Modifier.height(12.dp))
                CategoryFilterDropdown(
                    selectedCategory = filterCategory,
                    categories = categories,
                    onCategorySelected = viewModel::onFilterCategoryChanged
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            GenerateCodeButton(
                isLoading = isLoading,
                label = selectedType.label,
                onClick = { showGenerateDialog = true }
            )

            Spacer(modifier = Modifier.height(16.dp))

            CodesList(
                codes = filteredCodes,
                isLoading = isLoading,
                selectedTypeLabel = selectedType.label,
                filterCategoryName = filterCategory?.name,
                searchQuery = searchQuery,
                isAdmin = isAdmin,
                onEdit = { codeToEdit = it },
                onDelete = { codeToDelete = it }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodesTopBar(
    isAdmin: Boolean,
    selectedType: CodeType,
    onExport: () -> Unit,
    onImport: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = { Text("Gestión de Códigos", style = MaterialTheme.typography.titleLarge) },
        actions = {
            if (isAdmin) {
                IconButton(onClick = onExport) {
                    Icon(painterResource(id = R.drawable.file_export_solid_full), "Exportar", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                }
                IconButton(onClick = onImport) {
                    Icon(painterResource(id = R.drawable.file_import_solid_full), "Importar", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
fun TypeSelectionSection(
    selectedType: CodeType,
    onTypeSelected: (CodeType) -> Unit
) {
    Column {
        Text(text = "Seleccionar Grupo:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CodeTypeChip(CodeType.EMERGENCY, selectedType, onTypeSelected)
            CodeTypeChip(CodeType.SERVICES, selectedType, onTypeSelected)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CodeTypeChip(CodeType.MEDICINES, selectedType, onTypeSelected)
            CodeTypeChip(CodeType.DISPOSABLES, selectedType, onTypeSelected)
        }
    }
}

@Composable
fun CodeTypeChip(type: CodeType, selectedType: CodeType, onSelect: (CodeType) -> Unit) {
    FilterChip(
        selected = type == selectedType,
        onClick = { onSelect(type) },
        label = { Text(type.label) },
        shape = MaterialTheme.shapes.medium
    )
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Buscar descripción o código...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                }
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.medium
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryFilterDropdown(
    selectedCategory: Category?,
    categories: List<Category>,
    onCategorySelected: (Category?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedCategory?.let { "${it.code} - ${it.name}" } ?: "Todas las categorías",
            onValueChange = {},
            readOnly = true,
            label = { Text("Filtrar por Categoría") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Todas las categorías", fontWeight = FontWeight.Bold) },
                onClick = {
                    onCategorySelected(null)
                    expanded = false
                }
            )
            HorizontalDivider()
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text("${category.code} - ${category.name}") },
                    onClick = {
                        onCategorySelected(category)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun GenerateCodeButton(
    isLoading: Boolean,
    label: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        enabled = !isLoading,
        shape = MaterialTheme.shapes.medium
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Text("Procesando...")
        } else {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Generar Código $label")
        }
    }
}

@Composable
fun CodesList(
    codes: List<Code>,
    isLoading: Boolean,
    selectedTypeLabel: String,
    filterCategoryName: String?,
    searchQuery: String,
    isAdmin: Boolean,
    onEdit: (Code) -> Unit,
    onDelete: (Code) -> Unit
) {
    if (codes.isNotEmpty()) {
        val filterText = if (filterCategoryName != null) " - $filterCategoryName" else ""
        Text(
            text = "$selectedTypeLabel$filterText (${codes.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(codes, key = { it.id }) { code ->
                // Pre-format the date to avoid overhead during scroll
                val formattedDate = remember(code.createdAt) { formatDate(code.createdAt) }
                CodeListItem(
                    modifier = Modifier.animateItem(),
                    code = code,
                    formattedDate = formattedDate,
                    isAdmin = isAdmin,
                    onEdit = { onEdit(code) },
                    onDelete = { onDelete(code) }
                )
            }
        }
    } else if (!isLoading) {
        EmptyState(searchQuery)
    }
}

@Composable
fun CodeListItem(
    modifier: Modifier = Modifier,
    code: Code,
    formattedDate: String,
    isAdmin: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = code.code,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            supportingContent = {
                Column {
                    if (code.description.isNotBlank()) {
                        Text(text = code.description, style = MaterialTheme.typography.bodyMedium)
                    }
                    if (code.warehouseCode.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                text = " UBICACIÓN: ${code.warehouseCode} ",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Por ${code.createdBy} • $formattedDate",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            trailingContent = {
                if (isAdmin) {
                    Row {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, "Editar", tint = MaterialTheme.colorScheme.outline)
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, "Eliminar", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun EmptyState(searchQuery: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = if (searchQuery.isNotEmpty()) Icons.Default.SearchOff else Icons.Default.Inventory,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (searchQuery.isNotEmpty()) "No hay coincidencias para \"$searchQuery\"" else "No hay códigos registrados",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

fun formatDate(timestamp: Long): String {
    if (timestamp <= 0) return "Fecha desconocida"
    val dateFormat = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
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
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = if (selectedOption != null) optionText(selectedOption) else "",
            onValueChange = {}, readOnly = true, label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier.menuAnchor().fillMaxWidth(), 
            isError = isError,
            shape = MaterialTheme.shapes.medium
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionText(option)) }, 
                    onClick = { onOptionSelected(option); expanded = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerateCodeDialog(
    codeType: CodeType,
    categories: List<Category>,
    warehouses: List<Warehouse>,
    selectedCategory: Category?,
    warehouseTypeFilter: String,
    onWarehouseTypeFilterChange: (String) -> Unit,
    onCategorySelected: (Category) -> Unit,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var descriptionText by remember { mutableStateOf("") }
    var warehouseCodeText by remember { mutableStateOf("") }
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
            Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
                Text(
                    text = "Nuevo Código: ${codeType.label}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (codeType.isComposite) {
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
                        if (isCategoryError) Text("⚠ Debes seleccionar una categoría", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Paso 2: Almacén", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = warehouseTypeFilter == "estante",
                            onClick = {
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
                        supportingText = { if (isWarehouseError) Text(warehouseErrorMsg, color = MaterialTheme.colorScheme.error) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = descriptionText,
                    onValueChange = {
                        descriptionText = it
                        isDescriptionError = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Descripción / Detalle") },
                    maxLines = 3,
                    isError = isDescriptionError,
                    supportingText = { if (isDescriptionError) Text("⚠ La descripción es obligatoria", color = MaterialTheme.colorScheme.error) }
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            var valid = true
                            if (descriptionText.isBlank()) {
                                isDescriptionError = true
                                valid = false
                            }
                            if (codeType.isComposite) {
                                if (selectedCategory == null) {
                                    isCategoryError = true
                                    valid = false
                                }
                                val inputCode = warehouseCodeText.trim()
                                if (inputCode.isBlank()) {
                                    isWarehouseError = true
                                    warehouseErrorMsg = "⚠ Campo requerido"
                                    valid = false
                                } else {
                                    val exists = warehouses.any { it.code.equals(inputCode, ignoreCase = true) }
                                    if (!exists) {
                                        isWarehouseError = true
                                        warehouseErrorMsg = "⚠ Código no encontrado en BD ($warehouseTypeFilter)"
                                        valid = false
                                    }
                                }
                            }
                            if (valid) onConfirm(descriptionText, warehouseCodeText.trim())
                        }
                    ) { Text("Generar") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCodeDialog(
    code: Code,
    categories: List<Category>,
    warehouses: List<Warehouse>,
    onDismiss: () -> Unit,
    onConfirm: (String, Category?, String) -> Unit
) {
    var descriptionText by remember { mutableStateOf(code.description) }
    var selectedCategory by remember { mutableStateOf(categories.find { it.code == code.categoryCode }) }
    var warehouseTypeFilter by remember {
        mutableStateOf(if (code.warehouseCode.startsWith("R-", ignoreCase = true)) "refrigerador" else "estante")
    }
    var warehouseCodeText by remember { mutableStateOf(code.warehouseCode) }

    var isDescriptionError by remember { mutableStateOf(false) }
    var isWarehouseError by remember { mutableStateOf(false) }
    var warehouseErrorMsg by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Código") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Código actual: ${code.code}", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = descriptionText,
                    onValueChange = { descriptionText = it; isDescriptionError = false },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = isDescriptionError,
                    supportingText = { if(isDescriptionError) Text("Requerido", color = MaterialTheme.colorScheme.error) }
                )

                if (code.categoryCode.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Categoría", style = MaterialTheme.typography.labelLarge)
                    ExposedDropdownItem(
                        label = "Categoría",
                        options = categories,
                        selectedOption = selectedCategory,
                        onOptionSelected = { selectedCategory = it },
                        optionText = { "${it.code} - ${it.name}" }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Ubicación Almacén", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = warehouseTypeFilter == "estante",
                            onClick = { warehouseTypeFilter = "estante" },
                            label = { Text("Estantes") }
                        )
                        FilterChip(
                            selected = warehouseTypeFilter == "refrigerador",
                            onClick = { warehouseTypeFilter = "refrigerador" },
                            label = { Text("Refrigeradores") }
                        )
                    }

                    OutlinedTextField(
                        value = warehouseCodeText,
                        onValueChange = { warehouseCodeText = it; isWarehouseError = false },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Código de Almacén") },
                        isError = isWarehouseError,
                        supportingText = { if (isWarehouseError) Text(warehouseErrorMsg, color = MaterialTheme.colorScheme.error) },
                        shape = MaterialTheme.shapes.medium
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                var valid = true
                if (descriptionText.isBlank()) {
                    isDescriptionError = true
                    valid = false
                }

                if (code.categoryCode.isNotEmpty()) {
                    val inputCode = warehouseCodeText.trim()
                    val exists = warehouses.any { it.code.equals(inputCode, ignoreCase = true) }
                    if (inputCode.isBlank()) {
                        isWarehouseError = true
                        warehouseErrorMsg = "Requerido"
                        valid = false
                    } else if (!exists) {
                        isWarehouseError = true
                        warehouseErrorMsg = "No existe en BD"
                        valid = false
                    }
                }

                if (valid) onConfirm(descriptionText, selectedCategory, warehouseCodeText.trim())
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
