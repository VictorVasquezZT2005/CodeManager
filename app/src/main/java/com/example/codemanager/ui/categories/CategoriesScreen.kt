package com.example.codemanager.ui.categories

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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.codemanager.data.model.Category
import com.example.codemanager.data.repository.CategoryRepository
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    viewModel: CategoriesViewModel = viewModel(factory = CategoriesViewModelFactory(CategoryRepository()))
) {
    val uiState by viewModel.uiState.collectAsState()
    val message by viewModel.message.collectAsState()

    val isMed = uiState.selectedType == "MED"
    val subTitle = if (isMed) "Medicamentos" else "Descartables"
    val itemLabel = if (isMed) "Grupo" else "Clasificación"

    var showDialog by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var currentCategory by remember { mutableStateOf<Category?>(null) }
    var categoryName by remember { mutableStateOf("") }

    LaunchedEffect(message) {
        if (message != null) {
            delay(3000)
            viewModel.clearMessage()
        }
    }

    // Dialogo único para Crear/Editar
    if (showDialog) {
        CategoryDialog(
            title = if (isEditing) "Editar $itemLabel" else "Nueva $itemLabel",
            initialName = categoryName,
            label = "Nombre de $itemLabel",
            placeholder = if (isMed) "Ej: Analgésicos" else "Ej: Jeringas",
            onDismiss = { showDialog = false },
            onConfirm = { name ->
                if (isEditing && currentCategory != null) {
                    viewModel.updateCategory(currentCategory!!.id, name)
                } else {
                    viewModel.createCategory(name)
                }
                showDialog = false
            }
        )
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Gestión de Categorías", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)

            Spacer(modifier = Modifier.height(16.dp))

            // Selectores (Chips)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = isMed,
                    onClick = { viewModel.setCategoryType("MED") },
                    label = { Text("Medicamentos") },
                    leadingIcon = { if (isMed) Icon(Icons.Default.Check, null) }
                )
                FilterChip(
                    selected = !isMed,
                    onClick = { viewModel.setCategoryType("DESC") },
                    label = { Text("Descartables") },
                    leadingIcon = { if (!isMed) Icon(Icons.Default.Check, null) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón Crear
            Button(
                onClick = {
                    isEditing = false
                    categoryName = ""
                    showDialog = true
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Crear $itemLabel")
            }

            Spacer(modifier = Modifier.height(8.dp))
            if (message != null) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Text(message!!, modifier = Modifier.padding(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Lista
            if (uiState.isLoading) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else if (uiState.categories.isNotEmpty()) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    items(uiState.categories, key = { it.id }) { cat ->
                        CategoryItem(
                            category = cat,
                            label = itemLabel,
                            onEdit = {
                                currentCategory = cat
                                categoryName = cat.name
                                isEditing = true
                                showDialog = true
                            },
                            onDelete = { viewModel.deleteCategory(cat.id) }
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No hay $subTitle registrados")
                }
            }
        }
    }
}

// --- COMPONENTES REUTILIZABLES ---

@Composable
fun CategoryDialog(
    title: String,
    initialName: String,
    label: String,
    placeholder: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(label) },
                placeholder = { if (placeholder.isNotEmpty()) Text(placeholder) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun CategoryItem(
    category: Category,
    label: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("$label ${category.code}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(category.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Row {
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary) }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}