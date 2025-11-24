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
import androidx.compose.ui.window.Dialog
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
    val selectedRole by usersViewModel.selectedRole.collectAsState()

    // 1. Verificación de Permisos
    if (currentUser?.rol != "Administrador") {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Sin permisos",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = "Acceso Restringido",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Esta sección es exclusiva para administradores.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    // 2. Diálogo de Agregar/Editar
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
            errorMessage = errorMessage
        )
    }

    // 3. Contenido Principal
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Text(
                text = "Gestión de Usuarios",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Selector de Rol (Filtro)
            UserRoleSelector(
                selectedRole = selectedRole,
                onRoleSelected = usersViewModel::setSelectedRole
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Botón de Acción Principal
            Button(
                onClick = { usersViewModel.showAddUserDialog() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Agregar usuario")
                }
                Spacer(modifier = Modifier.width(8.dp))
                // El texto del botón confirma qué tipo de usuario se creará
                Text("Agregar $selectedRole")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tarjeta de Error
            if (errorMessage != null && !showDialog) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = { usersViewModel.clearError() }) {
                            Icon(Icons.Default.Close, "Cerrar", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            // Lista de Usuarios
            if (isLoading && users.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (users.isNotEmpty()) {
                Text(
                    text = "${selectedRole}s registrados (${users.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(users) { user ->
                        // VERIFICACIÓN: ¿Es este el usuario actual?
                        val isCurrentUser = user.id == currentUser?.id

                        UserItem(
                            user = user,
                            isCurrentUser = isCurrentUser,
                            onEdit = { usersViewModel.showEditUserDialog(user) },
                            onDelete = { usersViewModel.deleteUser(user) }
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
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.GroupOff,
                            contentDescription = "Sin usuarios",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No hay ${selectedRole.lowercase()}s registrados",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UserRoleSelector(
    selectedRole: String,
    onRoleSelected: (String) -> Unit
) {
    Column {
        Text(
            text = "Filtrar por rol:",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedRole == "Usuario",
                onClick = { onRoleSelected("Usuario") },
                label = { Text("Usuario") },
                leadingIcon = if (selectedRole == "Usuario") {
                    { Icon(Icons.Default.Check, null) }
                } else null
            )

            FilterChip(
                selected = selectedRole == "Administrador",
                onClick = { onRoleSelected("Administrador") },
                label = { Text("Administrador") },
                leadingIcon = if (selectedRole == "Administrador") {
                    { Icon(Icons.Default.Check, null) }
                } else null
            )
        }
    }
}

@Composable
fun UserItem(
    user: User,
    isCurrentUser: Boolean,
    onEdit: () -> Unit,
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
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Nombre
                    Text(
                        text = user.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Email
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Email,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = user.email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Badge de Rol
                    Surface(
                        color = if (user.rol == "Administrador")
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = user.rol,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (user.rol == "Administrador")
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    // INDICADOR "(Tú)"
                    if (isCurrentUser) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "(Tú)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Editar",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // SOLO MOSTRAR ELIMINAR SI NO ES EL USUARIO ACTUAL
                    if (!isCurrentUser) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Eliminar",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    } else {
                        Box(modifier = Modifier.size(24.dp))
                    }
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
    errorMessage: String?
) {
    val name by viewModel.newUserName.collectAsState()
    val email by viewModel.newUserEmail.collectAsState()
    val password by viewModel.newUserPassword.collectAsState()
    // Obtenemos el rol actual (que ya viene preconfigurado desde el ViewModel)
    val rol by viewModel.newUserRol.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (isEditing) "Editar $rol" else "Agregar Nuevo $rol",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                if (errorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { viewModel.updateNewUserName(it) },
                    label = { Text("Nombre completo *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { viewModel.updateNewUserEmail(it) },
                    label = { Text("Correo electrónico *") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isEditing,
                    singleLine = true
                )

                if (!isEditing) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { viewModel.updateNewUserPassword(it) },
                        label = { Text("Contraseña *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        supportingText = { Text("Mínimo 6 caracteres") }
                    )
                }

                // --- SELECCIÓN DE ROL ELIMINADA ---
                // Solo mostramos un texto informativo fijo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Rol asignado: ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = rol,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onConfirm,
                        enabled = name.isNotBlank() &&
                                (isEditing || (email.isNotBlank() && password.length >= 6))
                    ) {
                        Text(if (isEditing) "Guardar" else "Crear")
                    }
                }
            }
        }
    }
}