package com.example.syncus.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.syncus.ui.navigation.FirebasePaths
import com.example.syncus.ui.navigation.Routes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class HomeTaskUi(
    val id: String,
    val title: String,
    val done: Boolean,
    val priority: String,
    val dueAt: Date?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {

    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val scope = rememberCoroutineScope()

    val uid = auth.currentUser?.uid

    var userName by remember { mutableStateOf("Usuario") }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    var totalTasks by remember { mutableStateOf(0) }
    var todayTasks by remember { mutableStateOf(0) }
    var todayPending by remember { mutableStateOf(0) }
    var nextTask by remember { mutableStateOf<HomeTaskUi?>(null) }

    var showHelp by rememberSaveable { mutableStateOf(true) }

    var sharedTasksAlert by remember { mutableStateOf(false) }
    var sharedTasksCount by remember { mutableStateOf(0) }
    var sharedByName by remember { mutableStateOf("") }

    suspend fun loadHome() {

        if (uid == null) {
            loading = false
            error = "No hay usuario autenticado"
            return
        }

        loading = true
        error = null

        runCatching {
            val u = auth.currentUser

            if (u != null) {
                val authName = u.displayName.orEmpty()

                userName =
                    if (authName.isNotBlank()) {
                        authName
                    } else {
                        val doc = db.collection(FirebasePaths.USERS)
                            .document(u.uid)
                            .get()
                            .await()

                        doc.getString("displayName")
                            ?: auth.currentUser?.email?.substringBefore("@")
                            ?: "Usuario"
                    }
            }
        }.onFailure {
            userName = auth.currentUser?.email?.substringBefore("@") ?: "Usuario"
        }

        runCatching {
            val snap = db.collection(FirebasePaths.TASKS)
                .whereArrayContains("members", uid)
                .get()
                .await()

            val allTasks = snap.documents.map { doc ->
                HomeTaskUi(
                    id = doc.id,
                    title = doc.getString("title").orEmpty(),
                    done = doc.getBoolean("done") ?: false,
                    priority = doc.getString("priority") ?: "Media",
                    dueAt = doc.getTimestamp("dueAt")?.toDate()
                )
            }

            totalTasks = allTasks.size

            val startCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val endCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }

            val todayList = allTasks.filter { task ->
                val due = task.dueAt ?: return@filter false
                due.time in startCal.timeInMillis..endCal.timeInMillis
            }

            todayTasks = todayList.size
            todayPending = todayList.count { !it.done }

            nextTask = allTasks
                .filter { !it.done && it.dueAt != null }
                .sortedBy { it.dueAt!!.time }
                .firstOrNull()

            val sharedTasks = snap.documents.filter { doc ->
                val owner = doc.getString("ownerId")
                val seenBy = doc.get("seenBy") as? List<String> ?: emptyList()

                owner != uid && !seenBy.contains(uid)
            }

            if (sharedTasks.isNotEmpty()) {
                sharedTasksAlert = true
                sharedTasksCount = sharedTasks.size
                sharedByName = sharedTasks.first().getString("sharedByName") ?: "Alguien"
            }
        }.onFailure {
            error = it.message ?: "Error cargando Home"
        }

        loading = false
    }

    suspend fun markSharedTasksAsSeen() {

        if (uid == null) return

        val snap = db.collection(FirebasePaths.TASKS)
            .whereArrayContains("members", uid)
            .get()
            .await()

        val unseenSharedTasks = snap.documents.filter { doc ->
            val owner = doc.getString("ownerId")
            val seenBy = doc.get("seenBy") as? List<String> ?: emptyList()

            owner != uid && !seenBy.contains(uid)
        }

        unseenSharedTasks.forEach { doc ->
            db.collection(FirebasePaths.TASKS)
                .document(doc.id)
                .update("seenBy", FieldValue.arrayUnion(uid))
                .await()
        }
    }

    LaunchedEffect(uid) {
        loadHome()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Home") },
                actions = {
                    IconButton(
                        onClick = { navController.navigate(Routes.SETTINGS) }
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Ajustes"
                        )
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            Text(
                text = "Hola, $userName 👋",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

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
                        text = "Hoy",
                        fontWeight = FontWeight.SemiBold
                    )

                    if (todayTasks == 0) {
                        Text("No tienes tareas para hoy 🎉")
                    } else {
                        Text("Tareas hoy: $todayTasks")
                        Text("Pendientes hoy: $todayPending")
                    }

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = "Tareas totales: $totalTasks",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (nextTask != null) {
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "Próxima tarea",
                            style = MaterialTheme.typography.labelLarge
                        )

                        Text(nextTask!!.title)

                        Text(
                            text = formatDateTime(nextTask!!.dueAt),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { navController.navigate(Routes.TASKS) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Ver tareas")
                }

                OutlinedButton(
                    onClick = { navController.navigate(Routes.CALENDAR) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Calendario")
                }
            }

            if (!loading && totalTasks == 0 && showHelp) {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row {
                            Text(
                                text = "Cómo empezar",
                                modifier = Modifier.weight(1f),
                                fontWeight = FontWeight.SemiBold
                            )

                            IconButton(
                                onClick = { showHelp = false }
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Cerrar"
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Text("1️⃣ Pulsa + para crear una tarea")
                        Text("2️⃣ Elige prioridad, fecha y hora")
                        Text("3️⃣ Comparte tareas con otras personas")
                        Text("4️⃣ Revisa tus tareas en calendario o lista")
                    }
                }
            }
        }
    }

    if (sharedTasksAlert) {
        AlertDialog(
            onDismissRequest = { sharedTasksAlert = false },

            title = {
                Text("Nueva tarea compartida")
            },

            text = {
                if (sharedTasksCount == 1) {
                    Text("$sharedByName ha compartido una tarea contigo")
                } else {
                    Text("$sharedByName ha compartido $sharedTasksCount tareas contigo")
                }
            },

            confirmButton = {
                TextButton(
                    onClick = {
                        sharedTasksAlert = false
                        scope.launch {
                            markSharedTasksAsSeen()
                            navController.navigate(Routes.TASKS)
                        }
                    }
                ) {
                    Text("Ver tareas")
                }
            },

            dismissButton = {
                TextButton(
                    onClick = {
                        sharedTasksAlert = false
                        scope.launch {
                            markSharedTasksAsSeen()
                        }
                    }
                ) {
                    Text("Cerrar")
                }
            }
        )
    }
}

private fun formatDateTime(date: Date?): String {
    if (date == null) return "Sin fecha"

    return SimpleDateFormat(
        "dd/MM/yyyy HH:mm",
        Locale.getDefault()
    ).format(date)
}