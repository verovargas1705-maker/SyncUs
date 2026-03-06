package com.example.syncus.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.syncus.ui.navigation.FirebasePaths
import com.example.syncus.ui.navigation.Routes
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

data class TaskUi(
    val id: String = "",
    val title: String = "",
    val done: Boolean = false,
    val priority: String = "Media",
    val dueAt: Timestamp? = null,
    val ownerId: String = "",
    val members: List<String> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(navController: NavController) {

    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val scope = rememberCoroutineScope()

    val uid = auth.currentUser?.uid

    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var tasks by remember { mutableStateOf<List<TaskUi>>(emptyList()) }

    var selectedTask by remember { mutableStateOf<TaskUi?>(null) }
    var showShareDialog by remember { mutableStateOf(false) }

    suspend fun loadTasks() {

        if (uid == null) return

        loading = true

        runCatching {

            val snap = db.collection(FirebasePaths.TASKS)
                .whereArrayContains("members", uid)
                .get()
                .await()

            tasks = snap.documents.map {

                TaskUi(
                    id = it.id,
                    title = it.getString("title") ?: "",
                    done = it.getBoolean("done") ?: false,
                    priority = it.getString("priority") ?: "Media",
                    dueAt = it.getTimestamp("dueAt"),
                    ownerId = it.getString("ownerId") ?: "",
                    members = it.get("members") as? List<String> ?: emptyList()
                )
            }

        }.onFailure {
            error = it.message
        }

        loading = false
    }

    LaunchedEffect(Unit) {
        loadTasks()
    }

    Scaffold(

        topBar = {
            TopAppBar(
                title = { Text("Tareas") },
                actions = {
                    IconButton(onClick = { scope.launch { loadTasks() } }) {
                        Icon(Icons.Default.Refresh, "Actualizar")
                    }
                }
            )
        },

        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Routes.ADD_TASK) }
            ) {
                Icon(Icons.Default.Add, "Nueva tarea")
            }
        }

    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {

            if (loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error)
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                items(tasks) { task ->

                    TaskCard(

                        task = task,

                        onToggleDone = { checked ->

                            scope.launch {

                                db.collection(FirebasePaths.TASKS)
                                    .document(task.id)
                                    .update("done", checked)
                                    .await()

                                loadTasks()
                            }
                        },

                        onDelete = {

                            scope.launch {

                                db.collection(FirebasePaths.TASKS)
                                    .document(task.id)
                                    .delete()
                                    .await()

                                loadTasks()
                            }
                        },

                        onShare = {

                            selectedTask = task
                            showShareDialog = true
                        },

                        onEdit = {

                            navController.navigate(
                                Routes.editTask(task.id)
                            )
                        }
                    )
                }
            }
        }
    }

    if (showShareDialog && selectedTask != null) {

        ShareTaskDialog(
            task = selectedTask!!,
            onDismiss = {
                showShareDialog = false
                selectedTask = null
            },
            onShared = {
                showShareDialog = false
                selectedTask = null
                scope.launch { loadTasks() }
            }
        )
    }
}

@Composable
private fun TaskCard(
    task: TaskUi,
    onToggleDone: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onEdit: () -> Unit
) {

    val date = task.dueAt?.toDate()

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Checkbox(
                    checked = task.done,
                    onCheckedChange = onToggleDone
                )

                Spacer(Modifier.width(8.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        task.title,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration =
                            if (task.done) TextDecoration.LineThrough
                            else TextDecoration.None
                    )

                    if (date != null) {

                        Text(
                            SimpleDateFormat(
                                "dd/MM/yyyy HH:mm",
                                Locale.getDefault()
                            ).format(date)
                        )
                    }

                    if (task.members.size > 1) {

                        Text(
                            "Compartida",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {

                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, "Editar")
                }

                IconButton(onClick = onShare) {
                    Icon(Icons.Default.Share, "Compartir")
                }

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Borrar")
                }
            }
        }
    }
}

@Composable
private fun ShareTaskDialog(
    task: TaskUi,
    onDismiss: () -> Unit,
    onShared: () -> Unit
) {

    val db = FirebaseFirestore.getInstance()

    var email by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(

        onDismissRequest = onDismiss,

        title = {
            Text("Compartir tarea")
        },

        text = {

            Column {

                Text("Introduce el email del usuario")

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") }
                )

                if (error != null) {
                    Text(
                        error!!,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },

        confirmButton = {

            Button(

                onClick = {

                    db.collection(FirebasePaths.USERS)
                        .whereEqualTo("email", email.trim())
                        .limit(1)
                        .get()
                        .addOnSuccessListener { snap ->

                            val userDoc = snap.documents.firstOrNull()

                            if (userDoc == null) {

                                error = "Usuario no encontrado"
                                return@addOnSuccessListener
                            }

                            val uid = userDoc.id

                            db.collection(FirebasePaths.TASKS)
                                .document(task.id)
                                .update(
                                    "members",
                                    FieldValue.arrayUnion(uid)
                                )
                                .addOnSuccessListener {

                                    onShared()
                                }
                        }
                }
            ) {

                Text("Compartir")
            }
        },

        dismissButton = {

            TextButton(onClick = onDismiss) {

                Text("Cancelar")
            }
        }
    )
}