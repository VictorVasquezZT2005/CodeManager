package com.example.codemanager.ui.groups

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group // Icono genérico
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.codemanager.data.model.TherapeuticGroup // <-- IMPORTACIÓN CLAVE
import com.example.codemanager.data.repository.GroupRepository
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun GroupsScreen(
    // Inyección usando la Factory correcta
    viewModel: GroupsViewModel = viewModel(factory = GroupsViewModelFactory(GroupRepository()))
) {
    val uiState by viewModel.uiState.collectAsState()
    val message by viewModel.message.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    // Estado tipado con TherapeuticGroup
    var currentGroup by remember { mutableStateOf<TherapeuticGroup?>(null) }
    var groupName by remember { mutableStateOf("") }

    // Mostrar mensajes
    val snackbarHostState = remember { SnackbarHostState() }

    // Si prefieres usar SnackbarHost en un Scaffold (recomendado), o mantener tu lógica manual:
    // Mantengo tu lógica de LaunchedEffect con delay para no romper tu estructura actual
    LaunchedEffect(message) {
        if (message != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearMessage()
        }
    }

    // --- DIÁLOGO CREAR ---
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Crear Grupo Terapéutico") },
            text = {
                Column {
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Nombre del grupo *") },
                        placeholder = { Text("Ej: Analgésicos") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (groupName.isNotBlank()) {
                            viewModel.createGroup(name = groupName)
                            groupName = ""
                        }
                        showCreateDialog = false
                    },
                    enabled = groupName.isNotBlank()
                ) {
                    Text("Crear")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    groupName = ""
                    showCreateDialog = false
                }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // --- DIÁLOGO EDITAR ---
    if (showEditDialog && currentGroup != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Editar Grupo") },
            text = {
                Column {
                    Text(
                        text = "Código: ${currentGroup!!.code}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Nombre del grupo") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (groupName.isNotBlank() && currentGroup != null) {
                            viewModel.updateGroup(
                                groupId = currentGroup!!.id,
                                name = groupName
                            )
                        }
                        showEditDialog = false
                        currentGroup = null
                    },
                    enabled = groupName.isNotBlank()
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showEditDialog = false
                    currentGroup = null
                }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Text(
                text = "Grupos Terapéuticos",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Botón Crear
            Button(
                onClick = { showCreateDialog = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Default.Add, contentDescription = null)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Crear Nuevo Grupo")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mensaje de Error/Éxito (Banner)
            if (message != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Text(
                        text = message!!,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Lista
            if (uiState.isLoading && uiState.groups.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.groups.isNotEmpty()) {
                Text(
                    text = "Total: ${uiState.groups.size}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.groups, key = { it.id }) { group ->
                        GroupItem(
                            group = group,
                            onEdit = {
                                currentGroup = group
                                groupName = group.name
                                showEditDialog = true
                            },
                            onDelete = { viewModel.deleteGroup(group.id) }
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.surfaceVariant)
                        Text("No hay grupos registrados", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun GroupItem(
    group: TherapeuticGroup, // <-- TIPO ACTUALIZADO
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Código: ${group.code}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = group.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Borrar", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    }
                }
            }

            if (group.createdAt > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                val dateFormat = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
                Text(
                    text = "Creado: ${dateFormat.format(Date(group.createdAt))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}