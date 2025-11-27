package com.example.codemanager.ui.dashboard

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.codemanager.data.repository.AuthRepository
import com.example.codemanager.ui.auth.AuthViewModel
import com.example.codemanager.ui.auth.AuthViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun DashboardScreen(
    onLogout: () -> Unit,
    authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(AuthRepository()))
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }

    val userInitials = remember(currentUser?.name) {
        val name = currentUser?.name?.trim() ?: ""
        if (name.isNotEmpty()) {
            val p = name.split("\\s+".toRegex())
            "${p.getOrNull(0)?.take(1) ?: ""}${p.getOrNull(1)?.take(1) ?: ""}".uppercase()
        } else "U"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // --- HEADER ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 24.dp, end = 24.dp, bottom = 24.dp)
        ) {
            Text(
                text = "DASHBOARD",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userInitials,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.width(20.dp))
                Column {
                    Text(
                        text = "Hola, ${currentUser?.name?.split(" ")?.first() ?: "Usuario"}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.extraSmall,
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        Text(
                            text = (currentUser?.rol ?: "...").uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // --- CONTENIDO ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(text = "Datos de la cuenta", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(24.dp))
                    UserInfoRow(icon = Icons.Default.Badge, label = "Nombre completo", value = currentUser?.name ?: "No disponible")
                    Spacer(modifier = Modifier.height(16.dp))
                    UserInfoRow(icon = Icons.Default.AlternateEmail, label = "Correo electrónico", value = currentUser?.email ?: "No disponible")
                    Spacer(modifier = Modifier.height(16.dp))
                    UserInfoRow(icon = Icons.Default.VerifiedUser, label = "Nivel de acceso", value = currentUser?.rol ?: "No disponible")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- ACTUALIZACIONES ---
            UpdateCard(
                githubOwner = "VictorVasquezZT2005",
                githubRepo = "CodeManager"
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { showLogoutDialog = true },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
            ) {
                Icon(imageVector = Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cerrar Sesión", fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showLogoutDialog) {
        LogoutConfirmDialog(onConfirm = onLogout, onDismiss = { showLogoutDialog = false })
    }
}

// -----------------------------------------------------
// COMPONENTE ACTUALIZADO (NOTIFICACIONES FORZADAS)
// -----------------------------------------------------
@Composable
fun UpdateCard(
    githubOwner: String,
    githubRepo: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 1. GESTIÓN DE PERMISOS (Android 13+)
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> hasNotificationPermission = isGranted }
    )

    // Pedir permiso al iniciar si no lo tiene
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasNotificationPermission) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Datos de versión
    val currentVersion = remember(context) {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0"
        } catch (e: Exception) { "1.0" }
    }

    // Estados UI
    var latestVersion by remember { mutableStateOf<String?>(null) }
    var downloadUrl by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(false) }
    var updateAvailable by remember { mutableStateOf(false) }

    // Control para no spamear notificaciones (solo 1 vez por sesión)
    var notificationSent by remember { mutableStateOf(false) }

    // Estados Descarga
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadedFile by remember { mutableStateOf<File?>(null) }
    var downloadError by remember { mutableStateOf<String?>(null) }

    // Función para enviar la notificación
    fun sendNotification(version: String) {
        // Intenta enviar incluso si el permiso parece falso, por si acaso
        try {
            // Icono: Usamos uno del sistema para asegurar que no falle por falta de recursos
            val builder = NotificationCompat.Builder(context, "updates_channel")
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("Actualización Disponible")
                .setContentText("Nueva versión $version lista para descargar.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)

            with(NotificationManagerCompat.from(context)) {
                // Verificación final de permisos antes de notificar
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    notify(999, builder.build())
                }
                // Feedback visual (Toast) para confirmar que la lógica se ejecutó
                Toast.makeText(context, "Nueva versión detectada: $version", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Consultar GitHub
    LaunchedEffect(Unit) {
        checking = true
        try {
            withContext(Dispatchers.IO) {
                val url = "https://api.github.com/repos/$githubOwner/$githubRepo/releases/latest"
                val jsonString = URL(url).readText()
                val json = JSONObject(jsonString)

                val tagName = json.getString("tag_name").removePrefix("v")
                val assets = json.getJSONArray("assets")

                val apkUrl = if (assets.length() > 0) {
                    assets.getJSONObject(0).getString("browser_download_url")
                } else { "" }

                latestVersion = tagName
                downloadUrl = apkUrl
            }

            // Lógica de Notificación
            if (latestVersion != null && latestVersion != currentVersion && !downloadUrl.isNullOrEmpty()) {
                updateAvailable = true
                if (!notificationSent) {
                    sendNotification(latestVersion!!)
                    notificationSent = true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            checking = false
        }
    }

    // Función Instalar APK
    fun installApk(file: File) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            downloadError = "Error al instalar: ${e.message}"
        }
    }

    // Función Descargar
    fun startDownload() {
        if (downloadUrl == null) return
        isDownloading = true
        downloadProgress = 0f
        downloadError = null

        scope.launch(Dispatchers.IO) {
            try {
                val url = URL(downloadUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connect()
                val fileLength = connection.contentLength
                val input = connection.inputStream
                val storageDir = context.getExternalFilesDir(null)
                val outputFile = File(storageDir, "update.apk")
                val output = FileOutputStream(outputFile)
                val data = ByteArray(4096)
                var total: Long = 0
                var count: Int

                while (input.read(data).also { count = it } != -1) {
                    total += count.toLong()
                    if (fileLength > 0) downloadProgress = (total.toFloat() / fileLength.toFloat())
                    output.write(data, 0, count)
                }
                output.flush(); output.close(); input.close()

                withContext(Dispatchers.Main) {
                    isDownloading = false
                    downloadedFile = outputFile
                    // Auto-instalar al terminar la descarga (Opcional)
                    installApk(outputFile)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isDownloading = false
                    downloadError = "Error descarga: ${e.message}"
                }
            }
        }
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SystemUpdate, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(16.dp))
                Text("Versión del Sistema", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Versión Instalada:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(currentVersion, fontWeight = FontWeight.Bold)
            }
            if (latestVersion != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Última disponible:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("v$latestVersion", fontWeight = FontWeight.Bold, color = if (updateAvailable) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            if (checking) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Verificando...", style = MaterialTheme.typography.bodySmall)
                }
            } else if (downloadedFile != null) {
                Button(onClick = { installApk(downloadedFile!!) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)) {
                    Icon(Icons.Default.Android, null); Spacer(modifier = Modifier.width(8.dp)); Text("Instalar Ahora")
                }
            } else if (isDownloading) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    LinearProgressIndicator(progress = { downloadProgress }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape))
                    Text("${(downloadProgress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.End))
                }
            } else if (updateAvailable) {
                Button(onClick = { startDownload() }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.CloudDownload, null); Spacer(modifier = Modifier.width(8.dp)); Text("Descargar Actualización")
                }
            } else {
                OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth(), enabled = false) { Text("Sistema Actualizado") }
            }
            if (downloadError != null) Text(downloadError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

// -----------------------------------------------------
// HELPERS
// -----------------------------------------------------
@Composable
private fun UserInfoRow(icon: ImageVector, label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape), contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun LogoutConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Logout, contentDescription = null) },
        title = { Text("Cerrar Sesión", textAlign = TextAlign.Center) },
        text = { Text("¿Estás seguro de que deseas salir del sistema?", textAlign = TextAlign.Center) },
        confirmButton = { TextButton(onClick = onConfirm, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Cerrar Sesión") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}