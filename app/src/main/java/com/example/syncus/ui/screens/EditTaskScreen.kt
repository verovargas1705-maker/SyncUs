package com.example.syncus.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.syncus.ui.navigation.FirebasePaths
import com.example.syncus.ui.navigation.Routes
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*
import com.example.syncus.ui.sameMinute


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskScreen(
    navController: NavController,
    taskId: String
) {

    val context = LocalContext.current
    val db = remember { FirebaseFirestore.getInstance() }
    val auth = remember { FirebaseAuth.getInstance() }
    val scope = rememberCoroutineScope()

    val uid = auth.currentUser?.uid

    var title by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("Media") }

    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val calendar = remember { Calendar.getInstance() }

    var dateText by remember { mutableStateOf("") }
    var timeText by remember { mutableStateOf("") }

    LaunchedEffect(taskId) {

        runCatching {

            val doc = db.collection(FirebasePaths.TASKS)
                .document(taskId)
                .get()
                .await()

            title = doc.getString("title") ?: ""
            priority = doc.getString("priority") ?: "Media"

            val due = doc.getTimestamp("dueAt")?.toDate()

            if (due != null) {
                calendar.time = due
            }

            dateText = "%02d/%02d/%04d".format(
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.YEAR)
            )

            timeText = "%02d:%02d".format(
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE)
            )

        }.onFailure {

            error = it.message ?: "Error cargando tarea"
        }

        loading = false
    }

    val datePicker = DatePickerDialog(
        context,
        { _, y, m, d ->

            calendar.set(Calendar.YEAR, y)
            calendar.set(Calendar.MONTH, m)
            calendar.set(Calendar.DAY_OF_MONTH, d)

            dateText = "%02d/%02d/%04d".format(d, m + 1, y)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    val timePicker = TimePickerDialog(
        context,
        { _, h, m ->

            calendar.set(Calendar.HOUR_OF_DAY, h)
            calendar.set(Calendar.MINUTE, m)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            timeText = "%02d:%02d".format(h, m)
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        true
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar tarea") }
            )
        }
    ) { padding ->

        if (loading) {

            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth()
            )

            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Título") },
                modifier = Modifier.fillMaxWidth()
            )

            Text("Prioridad")

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                listOf("Alta", "Media", "Baja").forEach {

                    FilterChip(
                        selected = priority == it,
                        onClick = { priority = it },
                        label = { Text(it) }
                    )
                }
            }

            OutlinedButton(
                onClick = { datePicker.show() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Fecha: $dateText")
            }

            OutlinedButton(
                onClick = { timePicker.show() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Hora: $timeText")
            }

            if (error != null) {

                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(

                enabled = !saving && title.isNotBlank(),

                onClick = {

                    saving = true
                    error = null

                    scope.launch {

                        runCatching {

                            val dueAt = Timestamp(calendar.time)

                            val snap = db.collection(FirebasePaths.TASKS)
                                .whereArrayContains("members", uid ?: "")
                                .get()
                                .await()

                            val hasConflict = snap.documents.any { doc ->

                                if (doc.id == taskId) return@any false

                                val other =
                                    doc.getTimestamp("dueAt")?.toDate()
                                        ?: return@any false

                                sameMinute(
                                    other,
                                    dueAt.toDate()
                                )
                            }

                            if (hasConflict) {

                                error =
                                    "⚠ Ya tienes otra tarea en esa misma fecha y hora"

                                saving = false
                                return@launch
                            }

                            db.collection(FirebasePaths.TASKS)
                                .document(taskId)
                                .update(
                                    mapOf(
                                        "title" to title.trim(),
                                        "priority" to priority,
                                        "dueAt" to dueAt
                                    )
                                )
                                .await()

                            navController.navigate(Routes.TASKS) {

                                popUpTo(Routes.TASKS) {
                                    inclusive = true
                                }

                                launchSingleTop = true
                            }

                        }.onFailure {

                            error =
                                it.message ?: "Error guardando cambios"
                        }

                        saving = false
                    }
                },

                modifier = Modifier.fillMaxWidth()
            ) {

                Text("Guardar cambios")
            }
        }
    }
}

