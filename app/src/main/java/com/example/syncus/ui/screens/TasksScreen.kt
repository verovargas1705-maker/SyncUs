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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.syncus.ui.navigation.FirebasePaths
import com.example.syncus.ui.navigation.Routes
import com.example.syncus.ui.sameMinute
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TaskUi(
    val id: String = "",
    val title: String = "",
    val done: Boolean = false,
    val priority: String = "Media",
    val dueAt: Timestamp? = null,
    val ownerId: String = "",
    val members: List<String> = emptyList(),
    val sharedByName: String = "",
    val hasConflict: Boolean = false
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

        if (uid == null) {
            loading = false
            error = "No hay usuario autenticado"
            return
        }

        loading = true
        error = null

        runCatching {
            val snap = db.collection(FirebasePaths.TASKS)
                .whereArrayContains("members", uid)
                .get()
                .await()

            val rawTasks = snap.documents.map { doc ->
                TaskUi(
                    id = doc.id,
                    title = doc.getString("title").orEmpty(),
                    done = doc.getBoolean("done") ?: false,
                    priority = doc.getString("priority") ?: "Media",
                    dueAt = doc.getTimestamp("dueAt"),
                    ownerId = doc.getString("ownerId").orEmpty(),
                    members = doc.get("members") as? List<String> ?: emptyList(),
                    sharedByName = doc.getString("sharedByName").orEmpty()
                )
            }

            val taskListWithConflicts = rawTasks.map { task ->
                val due = task.dueAt?.toDate()

                val conflict = if (due == null) {
                    false
                } else {
                    rawTasks.any { other ->
                        other.id != task.id &&
                                other.dueAt?.toDate()?.let { otherDate ->
                                    sameMinute(due, otherDate)
                                } == true
                    }
                }

                task.copy(hasConflict = conflict)
            }

            tasks = taskListWithConflicts.sortedWith(
                compareBy<TaskUi> { it.done }
                    .thenBy { it.dueAt?.toDate()?.time ?: Long.MAX_VALUE }
                    .thenBy { priorityOrder(it.priority) }
                    .thenBy { it.title.lowercase() }
            )
        }.onFailure {
            error = it.message ?: "Error cargando tareas"
        }

        loading = false
    }

    LaunchedEffect(uid) {
        loadTasks()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tareas") },
                actions = {
                    IconButton(
                        onClick = { scope.launch { loadTasks() } }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Routes.ADD_TASK) }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Añadir tarea")
            }
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

            if (!loading && tasks.isEmpty()) {
                Card {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "No hay tareas aún",
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text("Pulsa + para crear tu primera tarea.")
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(tasks, key = { it.id }) { task ->

                        TaskCard(
                            task = task,
                            currentUid = uid.orEmpty(),
                            onToggleDone = { checked ->
                                scope.launch {
                                    runCatching {
                                        db.collection(FirebasePaths.TASKS)
                                            .document(task.id)
                                            .update("done", checked)
                                            .await()
                                        loadTasks()
                                    }.onFailure {
                                        error = it.message ?: "Error actualizando tarea"
                                    }
                                }
                            },
                            onDelete = {
                                scope.launch {
                                    runCatching {
                                        db.collection(FirebasePaths.TASKS)
                                            .document(task.id)
                                            .delete()
                                            .await()
                                        loadTasks()
                                    }.onFailure {
                                        error = it.message ?: "Error borrando tarea"
                                    }
                                }
                            },
                            onShare = {
                                selectedTask = task
                                showShareDialog = true
                            }
                        )
                    }
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
    currentUid: String,
    onToggleDone: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {

    val dueDate = task.dueAt?.toDate()
    val isOwner = task.ownerId == currentUid
    val isShared = task.members.size > 1

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Checkbox(
                checked = task.done,
                onCheckedChange = onToggleDone
            )

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = task.title,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration =
                            if (task.done) TextDecoration.LineThrough
                            else TextDecoration.None
                    )

                    if (isShared) {
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Tarea compartida",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (task.hasConflict) {
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Conflicto de horario",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = formatDate(task.dueAt?.toDate()),
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    text = "Prioridad: ${task.priority}",
                    style = MaterialTheme.typography.bodySmall
                )

                if (!isOwner && task.sharedByName.isNotBlank()) {
                    Text(
                        text = "Te la ha compartido: ${task.sharedByName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (task.hasConflict) {
                    Text(
                        text = "Conflicto con otra tarea en la misma fecha y hora",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (isOwner) {
                IconButton(onClick = onShare) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Compartir"
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Borrar"
                )
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
    val auth = FirebaseAuth.getInstance()

    var email by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!loading) onDismiss() },
        title = { Text("Compartir tarea") },
        text = {
            Column {
                Text("Comparte \"${task.title}\" con otro usuario")

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true
                )

                if (error != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = email.isNotBlank() && !loading,
                onClick = {
                    loading = true
                    error = null

                    val typedEmail = email.trim().lowercase()

                    db.collection(FirebasePaths.USERS)
                        .whereEqualTo("email", typedEmail)
                        .limit(1)
                        .get()
                        .addOnSuccessListener { usersSnap ->

                            val userDoc = usersSnap.documents.firstOrNull()

                            if (userDoc == null) {
                                error = "No existe un usuario con ese email"
                                loading = false
                                return@addOnSuccessListener
                            }

                            val sharedUid = userDoc.id
                            val sharedByName =
                                auth.currentUser?.displayName
                                    ?: auth.currentUser?.email?.substringBefore("@")
                                    ?: "Usuario"

                            db.collection(FirebasePaths.TASKS)
                                .document(task.id)
                                .update(
                                    mapOf(
                                        "members" to FieldValue.arrayUnion(sharedUid),
                                        "sharedByName" to sharedByName
                                    )
                                )
                                .addOnSuccessListener {
                                    loading = false
                                    onShared()
                                }
                                .addOnFailureListener {
                                    error = it.message ?: "Error compartiendo tarea"
                                    loading = false
                                }
                        }
                        .addOnFailureListener {
                            error = it.message ?: "Error buscando usuario"
                            loading = false
                        }
                }
            ) {
                Text(if (loading) "Compartiendo..." else "Compartir")
            }
        },
        dismissButton = {
            TextButton(
                enabled = !loading,
                onClick = onDismiss
            ) {
                Text("Cancelar")
            }
        }
    )
}

private fun priorityOrder(priority: String): Int = when (priority.lowercase()) {
    "alta" -> 0
    "media" -> 1
    "baja" -> 2
    else -> 3
}

private fun formatDate(date: Date?): String {
    if (date == null) return "Sin fecha"

    return SimpleDateFormat(
        "dd/MM/yyyy HH:mm",
        Locale.getDefault()
    ).format(date)
}