package com.example.codemanager.ui.users

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.codemanager.data.model.User
import com.example.codemanager.data.repository.AuthRepository
import com.example.codemanager.ui.auth.AuthViewModel

@Composable
fun UsersScreen(
    authRepository: AuthRepository,
    authViewModel: AuthViewModel,
    usersViewModel: UsersViewModel = viewModel(
        factory = UsersViewModelFactory(authRepository)
    )
) {
    val users by usersViewModel.users.collectAsState()
    val isLoading by usersViewModel.isLoading.collectAsState()
    val showDialog by usersViewModel.showDialog.collectAsState()
    val editingUser by usersViewModel.editingUser.collectAsState()
    val errorMessage by usersViewModel.errorMessage.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()

    // Verificar si el usuario actual es Administrador
    if (currentUser?.rol != "Administrador") {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Sin permisos",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = "Acceso denegado",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "No tienes permisos de administrador para acceder a esta sección",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    // Mostrar mensaje de error si existe
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            // Podrías mostrar un Snackbar aquí en lugar de solo en el dialog
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { usersViewModel.showAddUserDialog() },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar usuario")
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    Text("Cargando usuarios...")
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Header
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Gestión de Usuarios",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Administra los usuarios del sistema desde Firebase",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }

                // Lista de usuarios
                if (users.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = "Sin usuarios",
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "No hay usuarios registrados",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Button(
                                onClick = { usersViewModel.showAddUserDialog() }
                            ) {
                                Text("Agregar primer usuario")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(users) { user ->
                            UserCard(
                                user = user,
                                onEdit = { usersViewModel.showEditUserDialog(user) },
                                onDelete = { usersViewModel.deleteUser(user) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog para agregar/editar usuario
    if (showDialog) {
        UserDialog(
            isEditing = editingUser != null,
            onDismiss = { usersViewModel.hideDialog() },
            onConfirm = {
                if (editingUser != null) {
                    usersViewModel.updateUser()
                } else {
                    usersViewModel.createUser()
                }
            },
            viewModel = usersViewModel,
            errorMessage = errorMessage,
            onErrorDismiss = { usersViewModel.clearError() }
        )
    }
}

@Composable
fun UserCard(
    user: User,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Badge(
                    containerColor = if (user.rol == "Administrador") MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.secondary
                ) {
                    Text(
                        text = user.rol,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun UserDialog(
    isEditing: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    viewModel: UsersViewModel,
    errorMessage: String?,
    onErrorDismiss: () -> Unit
) {
    val name by viewModel.newUserName.collectAsState()
    val email by viewModel.newUserEmail.collectAsState()
    val password by viewModel.newUserPassword.collectAsState()
    val rol by viewModel.newUserRol.collectAsState()

    // Mostrar error si existe
    if (errorMessage != null) {
        LaunchedEffect(errorMessage) {
            // Podrías mostrar un Snackbar o mantenerlo en el dialog
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isEditing) "Editar Usuario" else "Agregar Usuario")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Mostrar mensaje de error
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { viewModel.updateNewUserName(it) },
                    label = { Text("Nombre completo") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = name.isBlank()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { viewModel.updateNewUserEmail(it) },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isEditing,
                    isError = email.isBlank() && !isEditing
                )

                if (!isEditing) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { viewModel.updateNewUserPassword(it) },
                        label = { Text("Contraseña") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = password.length < 6 && password.isNotBlank()
                    )
                    if (password.isNotBlank() && password.length < 6) {
                        Text(
                            text = "La contraseña debe tener al menos 6 caracteres",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                // Selector de rol
                Column {
                    Text(
                        text = "Rol",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = rol == "Usuario",
                            onClick = { viewModel.updateNewUserRol("Usuario") },
                            label = { Text("Usuario") }
                        )
                        FilterChip(
                            selected = rol == "Administrador",
                            onClick = { viewModel.updateNewUserRol("Administrador") },
                            label = { Text("Administrador") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = name.isNotBlank() &&
                        (isEditing || (email.isNotBlank() && password.length >= 6))
            ) {
                Text(if (isEditing) "Actualizar" else "Crear")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}