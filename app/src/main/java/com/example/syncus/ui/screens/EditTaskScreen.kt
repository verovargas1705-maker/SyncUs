package com.example.syncus.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.syncus.ui.navigation.FirebasePaths
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskScreen(
    navController: NavController,
    taskId: String
) {
    val db = remember { FirebaseFirestore.getInstance() }

    var title by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("Media") }
    var dueDateText by remember { mutableStateOf("") }

    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    var expanded by remember { mutableStateOf(false) }

    val priorities = listOf("Alta", "Media", "Baja")

    suspend fun loadTask() {
        runCatching {
            val doc = db.collection(FirebasePaths.TASKS)
                .document(taskId)
                .get()
                .await()

            title = doc.getString("title").orEmpty()
            priority = doc.getString("priority") ?: "Media"

            val due = doc.getTimestamp("dueAt")?.toDate()

            dueDateText = if (due != null) {
                SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(due)
            } else {
                ""
            }
        }.onFailure {
            error = it.message
        }

        loading = false
    }

    LaunchedEffect(taskId) {
        loadTask()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar tarea") }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Título") },
                modifier = Modifier.fillMaxWidth()
            )

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = priority,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Prioridad") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    priorities.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                priority = option
                                expanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = dueDateText,
                onValueChange = { dueDateText = it },
                label = { Text("Fecha (dd/MM/yyyy HH:mm)") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                enabled = title.isNotBlank() && !saving,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    saving = true
                    error = null

                    runCatching {
                        val date = if (dueDateText.isNotBlank()) {
                            SimpleDateFormat(
                                "dd/MM/yyyy HH:mm",
                                Locale.getDefault()
                            ).parse(dueDateText)
                        } else null

                        val updates = mutableMapOf<String, Any>(
                            "title" to title,
                            "priority" to priority
                        )

                        if (date != null) {
                            updates["dueAt"] = Timestamp(date)
                        }

                        db.collection(FirebasePaths.TASKS)
                            .document(taskId)
                            .update(updates)
                            .addOnSuccessListener {
                                navController.popBackStack()
                            }
                    }.onFailure {
                        error = it.message
                    }

                    saving = false
                }
            ) {
                Text(if (saving) "Guardando..." else "Guardar cambios")
            }
        }
    }
}