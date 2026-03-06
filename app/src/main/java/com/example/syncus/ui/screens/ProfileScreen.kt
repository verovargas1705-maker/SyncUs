package com.example.syncus.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.syncus.ui.navigation.FirebasePaths
import com.example.syncus.ui.navigation.Routes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }

    val uid = auth.currentUser?.uid

    var userName by remember { mutableStateOf("Usuario") }
    var email by remember { mutableStateOf("") }
    var totalTasks by remember { mutableIntStateOf(0) }
    var pendingTasks by remember { mutableIntStateOf(0) }
    var doneTasks by remember { mutableIntStateOf(0) }

    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uid) {
        if (uid == null) {
            loading = false
            error = "No hay usuario autenticado"
            return@LaunchedEffect
        }

        loading = true
        error = null

        runCatching {
            val user = auth.currentUser
            email = user?.email.orEmpty()

            val authName = user?.displayName.orEmpty()
            userName = if (authName.isNotBlank()) {
                authName
            } else {
                val doc = db.collection(FirebasePaths.USERS)
                    .document(uid)
                    .get()
                    .await()

                doc.getString("displayName")
                    ?.takeIf { it.isNotBlank() }
                    ?: email.substringBefore("@").ifBlank { "Usuario" }
            }

            val tasksSnap = db.collection(FirebasePaths.TASKS)
                .whereArrayContains("members", uid)
                .get()
                .await()

            totalTasks = tasksSnap.size()
            pendingTasks = tasksSnap.documents.count { !(it.getBoolean("done") ?: false) }
            doneTasks = tasksSnap.documents.count { it.getBoolean("done") ?: false }

        }.onFailure {
            error = it.message ?: "Error cargando perfil"
        }

        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Perfil") },
                actions = {
                    IconButton(onClick = { navController.navigate(Routes.SETTINGS) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Ajustes")
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (error != null) {
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = userName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (email.isNotBlank()) {
                        Text(
                            text = email,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Resumen",
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("Tareas totales: $totalTasks")
                    Text("Pendientes: $pendingTasks")
                    Text("Completadas: $doneTasks")
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { navController.navigate(Routes.TASKS) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ver mis tareas")
            }

            Button(
                onClick = { navController.navigate(Routes.CALENDAR) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ver calendario")
            }

            Button(
                onClick = { navController.navigate(Routes.SETTINGS) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ir a ajustes")
            }
        }
    }
}