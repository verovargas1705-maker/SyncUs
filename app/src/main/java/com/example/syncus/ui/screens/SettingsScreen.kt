package com.example.syncus.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.syncus.ui.navigation.FirebasePaths
import com.example.syncus.ui.navigation.Routes
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val user = auth.currentUser
    val scope = rememberCoroutineScope()

    var displayName by remember { mutableStateOf(user?.displayName.orEmpty()) }
    var email by remember { mutableStateOf(user?.email.orEmpty()) }

    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    var showChangeName by remember { mutableStateOf(false) }
    var showChangePass by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Ajustes") })
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            ElevatedCard {
                Column(Modifier.padding(16.dp)) {
                    Text("Cuenta", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text(if (displayName.isBlank()) "Sin nombre" else displayName)
                    if (email.isNotBlank()) {
                        Text(email, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (message != null) {
                AssistChip(
                    onClick = { message = null },
                    label = { Text(message!!) }
                )
            }

            ElevatedCard(
                onClick = { showChangeName = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Cambiar nombre", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Tu nombre visible en la app",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text("Editar")
                }
            }

            ElevatedCard(
                onClick = { showChangePass = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Cambiar contraseña", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Actualiza tu contraseña",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text("Editar")
                }
            }

            ElevatedCard(
                onClick = {
                    auth.signOut()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.HOME) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Cerrar sesión", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Salir de tu cuenta",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text("Salir")
                }
            }

            OutlinedCard(
                onClick = { showDeleteConfirm = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Eliminar cuenta", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                        Text(
                            "Esta acción es irreversible",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            }

            if (loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }

    if (showChangeName) {
        var newName by remember { mutableStateOf(displayName) }

        AlertDialog(
            onDismissRequest = { if (!loading) showChangeName = false },
            title = { Text("Cambiar nombre") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Nombre") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    enabled = newName.trim().isNotEmpty() && !loading,
                    onClick = {
                        val finalName = newName.trim()
                        loading = true

                        scope.launch {
                            runCatching {
                                val req = UserProfileChangeRequest.Builder()
                                    .setDisplayName(finalName)
                                    .build()

                                auth.currentUser?.updateProfile(req)?.await()

                                val uid = auth.currentUser?.uid
                                val emailValue = auth.currentUser?.email.orEmpty()

                                if (uid != null) {
                                    db.collection(FirebasePaths.USERS)
                                        .document(uid)
                                        .set(
                                            mapOf(
                                                "displayName" to finalName,
                                                "email" to emailValue
                                            )
                                        )
                                        .await()
                                }

                                auth.currentUser?.reload()?.await()
                            }.onSuccess {
                                displayName = finalName
                                message = "Nombre actualizado ✅"
                                showChangeName = false
                            }.onFailure {
                                message = it.message ?: "Error actualizando nombre"
                            }
                            loading = false
                        }
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(enabled = !loading, onClick = { showChangeName = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showChangePass) {
        var currentPass by remember { mutableStateOf("") }
        var newPass by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { if (!loading) showChangePass = false },
            title = { Text("Cambiar contraseña") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = currentPass,
                        onValueChange = { currentPass = it },
                        label = { Text("Contraseña actual") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newPass,
                        onValueChange = { newPass = it },
                        label = { Text("Nueva contraseña") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = currentPass.isNotBlank() && newPass.length >= 6 && !loading,
                    onClick = {
                        val u = auth.currentUser
                        if (u == null || u.email.isNullOrBlank()) {
                            message = "No hay usuario autenticado"
                            showChangePass = false
                            return@Button
                        }

                        loading = true
                        scope.launch {
                            runCatching {
                                val cred = EmailAuthProvider.getCredential(u.email!!, currentPass)
                                u.reauthenticate(cred).await()
                                u.updatePassword(newPass).await()
                            }.onSuccess {
                                message = "Contraseña actualizada ✅"
                                showChangePass = false
                            }.onFailure {
                                message = it.message ?: "Error cambiando contraseña"
                            }
                            loading = false
                        }
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(enabled = !loading, onClick = { showChangePass = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { if (!loading) showDeleteConfirm = false },
            title = { Text("Eliminar cuenta") },
            text = { Text("¿Seguro? Se borrará tu cuenta de forma permanente.") },
            confirmButton = {
                Button(
                    enabled = !loading,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        val u = auth.currentUser
                        if (u == null) {
                            message = "No hay usuario"
                            showDeleteConfirm = false
                            return@Button
                        }

                        loading = true
                        scope.launch {
                            runCatching {
                                db.collection(FirebasePaths.USERS)
                                    .document(u.uid)
                                    .delete()
                                    .await()

                                u.delete().await()
                            }.onSuccess {
                                message = "Cuenta eliminada"
                                navController.navigate(Routes.LOGIN) {
                                    popUpTo(Routes.HOME) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }.onFailure {
                                message = it.message ?: "No se pudo eliminar"
                            }
                            loading = false
                        }
                    }
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(enabled = !loading, onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}