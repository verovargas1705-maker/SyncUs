package com.example.syncus.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskScreen(navController: NavController) {

    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val scope = rememberCoroutineScope()

    val uid = auth.currentUser?.uid

    var title by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("Media") }
    var creating by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val calendar = remember { Calendar.getInstance() }

    var dateText by remember {
        mutableStateOf(
            "%02d/%02d/%04d".format(
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.YEAR)
            )
        )
    }

    var timeText by remember {
        mutableStateOf(
            "%02d:%02d".format(
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE)
            )
        )
    }

    val datePicker = DatePickerDialog(
        context,
        { _, year, month, day ->

            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, day)

            dateText = String.format(
                Locale.getDefault(),
                "%02d/%02d/%04d",
                day,
                month + 1,
                year
            )
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    val timePicker = TimePickerDialog(
        context,
        { _, hour, minute ->

            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            timeText = String.format(
                Locale.getDefault(),
                "%02d:%02d",
                hour,
                minute
            )
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        true
    )

    Scaffold(

        topBar = {

            TopAppBar(

                title = { Text("Nueva tarea") },

                navigationIcon = {

                    IconButton(
                        onClick = { navController.popBackStack() }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
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

            OutlinedTextField(

                value = title,
                onValueChange = { title = it },

                label = { Text("Nombre de la tarea") },

                modifier = Modifier.fillMaxWidth(),

                singleLine = true
            )

            Text("Prioridad")

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                listOf("Alta", "Media", "Baja").forEach { option ->

                    FilterChip(

                        selected = priority == option,

                        onClick = { priority = option },

                        label = { Text(option) }
                    )
                }
            }

            Text("Fecha y hora")

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

                enabled = title.isNotBlank() && !creating,

                onClick = {

                    if (uid == null) {
                        error = "No hay usuario autenticado"
                        return@Button
                    }

                    creating = true
                    error = null

                    scope.launch {

                        runCatching {

                            val dueAt = Timestamp(calendar.time)

                            val displayName =
                                auth.currentUser?.displayName
                                    ?: auth.currentUser?.email?.substringBefore("@")
                                    ?: "Usuario"

                            db.collection(FirebasePaths.TASKS)
                                .add(
                                    mapOf(
                                        "ownerId" to uid,
                                        "members" to listOf(uid),
                                        "seenBy" to listOf(uid),
                                        "sharedByName" to displayName,
                                        "title" to title.trim(),
                                        "done" to false,
                                        "priority" to priority,
                                        "dueAt" to dueAt,
                                        "createdAt" to FieldValue.serverTimestamp()
                                    )
                                )
                                .await()

                            navController.navigate(Routes.TASKS) {

                                popUpTo(Routes.ADD_TASK) { inclusive = true }

                                launchSingleTop = true
                            }

                        }.onFailure {

                            error = it.message ?: "Error creando tarea"
                        }

                        creating = false
                    }
                },

                modifier = Modifier.fillMaxWidth()
            ) {

                Text(
                    if (creating) "Creando..."
                    else "Crear tarea"
                )
            }
        }
    }
}