// ui/codes/CodesScreen.kt
package com.example.codemanager.ui.codes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.codemanager.data.repository.CodeRepository

@Composable
fun CodesScreen(
    viewModel: CodesViewModel = viewModel(factory = CodesViewModelFactory(CodeRepository()))
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedPrefix by viewModel.selectedPrefix.collectAsState()
    val message by viewModel.message.collectAsState()

    var showDescriptionDialog by remember { mutableStateOf(false) }
    var descriptionText by remember { mutableStateOf("") }

    // Mostrar mensajes
    LaunchedEffect(message) {
        if (message != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearMessage()
        }
    }

    // Diálogo para ingresar descripción
    if (showDescriptionDialog) {
        DescriptionDialog(
            description = descriptionText,
            onDescriptionChange = { descriptionText = it },
            onConfirm = {
                if (descriptionText.isNotBlank()) {
                    viewModel.generateNewCode(
                        description = descriptionText,
                        createdBy = "current_user_id" // Aquí deberías pasar el ID del usuario actual
                    )
                } else {
                    viewModel.generateNewCode(
                        description = "Código generado automáticamente",
                        createdBy = "current_user_id"
                    )
                }
                descriptionText = ""
                showDescriptionDialog = false
            },
            onDismiss = {
                descriptionText = ""
                showDescriptionDialog = false
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Text(
                text = "Gestión de Códigos",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Selector de prefijo
            PrefixSelector(
                selectedPrefix = selectedPrefix,
                onPrefixSelected = viewModel::setSelectedPrefix
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Botón para generar nuevo código (siempre visible ya que no hay "ALL")
            Button(
                onClick = {
                    showDescriptionDialog = true
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
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
                    Icon(Icons.Default.Add, contentDescription = "Generar código")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generar Código $selectedPrefix-XXXXX")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mostrar mensaje
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

            // Lista de códigos
            if (uiState.isLoading && uiState.filteredCodes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.filteredCodes.isNotEmpty()) {
                Text(
                    text = "Códigos generados (${uiState.filteredCodes.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.filteredCodes, key = { it.id }) { code ->
                        CodeItem(
                            code = code,
                            onDelete = { viewModel.deleteCode(code.id) }
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
                        text = "No hay códigos generados",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun DescriptionDialog(
    description: String,
    onDescriptionChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
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
                    text = "Agregar Descripción",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Campo de descripción
                OutlinedTextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Descripción del código") },
                    placeholder = { Text("Ej: Compra de materiales de oficina") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = "Descripción"
                        )
                    },
                    singleLine = false,
                    maxLines = 3
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
                        onClick = onConfirm,
                        enabled = description.isNotBlank()
                    ) {
                        Text("Generar Código")
                    }
                }
            }
        }
    }
}

@Composable
fun PrefixSelector(
    selectedPrefix: String,
    onPrefixSelected: (String) -> Unit
) {
    Column {
        Text(
            text = "Seleccionar tipo de código:",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("62", "70").forEach { prefix ->
                FilterChip(
                    selected = selectedPrefix == prefix,
                    onClick = { onPrefixSelected(prefix) },
                    label = {
                        Text(
                            text = when (prefix) {
                                "62" -> "Compras Emergencia"
                                "70" -> "Servicios"
                                else -> prefix
                            }
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun CodeItem(
    code: com.example.codemanager.data.model.Code,
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = code.code,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (code.description.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = code.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Secuencia: ${code.sequence}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (code.createdBy.isNotEmpty()) {
                        Text(
                            text = "Creado por: ${code.createdBy}",
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
                        contentDescription = "Eliminar código",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Mostrar la fecha de creación si está disponible
            if (code.createdAt > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Creado: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(java.util.Date(code.createdAt))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}