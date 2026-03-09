package com.example.syncus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.syncus.ui.navigation.FirebasePaths
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.layout.height

data class CalendarTaskUi(
    val id: String,
    val title: String,
    val priority: String,
    val dueAt: Timestamp?,
    val done: Boolean
)
//Añadida pantalla de calendario
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(navController: NavController) {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val uid = auth.currentUser?.uid

    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var tasks by remember { mutableStateOf<List<CalendarTaskUi>>(emptyList()) }

    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    suspend fun loadCalendarTasks() {
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

            tasks = snap.documents.map { doc ->
                CalendarTaskUi(
                    id = doc.id,
                    title = doc.getString("title").orEmpty(),
                    priority = doc.getString("priority") ?: "Media",
                    dueAt = doc.getTimestamp("dueAt"),
                    done = doc.getBoolean("done") ?: false
                )
            }.sortedWith(
                compareBy<CalendarTaskUi> { it.done }
                    .thenBy { it.dueAt?.toDate()?.time ?: Long.MAX_VALUE }
                    .thenBy { priorityOrder(it.priority) }
                    .thenBy { it.title.lowercase() }
            )
        }.onFailure {
            error = it.message ?: "Error cargando calendario"
        }

        loading = false
    }

    LaunchedEffect(uid) {
        loadCalendarTasks()
    }

    val tasksByDate = remember(tasks) {
        tasks.groupBy { task ->
            task.dueAt?.toDate()
                ?.toInstant()
                ?.atZone(ZoneId.systemDefault())
                ?.toLocalDate()
        }
    }

    val selectedDayTasks = tasksByDate[selectedDate]
        .orEmpty()
        .sortedWith(
            compareBy<CalendarTaskUi> { it.done }
                .thenBy { it.dueAt?.toDate()?.time ?: Long.MAX_VALUE }
                .thenBy { priorityOrder(it.priority) }
        )

    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfMonth = currentMonth.atDay(1)
    val firstWeekdayIndex = firstDayOffsetMonday(firstDayOfMonth.dayOfWeek)

    val monthCells = buildList<LocalDate?> {
        repeat(firstWeekdayIndex) { add(null) }
        for (day in 1..daysInMonth) add(currentMonth.atDay(day))
        while (size % 7 != 0) add(null)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendario") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
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

            Card(shape = MaterialTheme.shapes.large) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Mes anterior")
                    }

                    Text(
                        text = formatMonthYear(currentMonth),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Mes siguiente")
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("L", "M", "X", "J", "V", "S", "D").forEach { day ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(day, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                monthCells.chunked(7).forEach { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        week.forEach { date ->
                            CalendarDayCell(
                                date = date,
                                selectedDate = selectedDate,
                                tasksByDate = tasksByDate,
                                onClick = {
                                    if (date != null) selectedDate = date
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            Text(
                text = "Agenda del ${formatSelectedDate(selectedDate)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            if (!loading && selectedDayTasks.isEmpty()) {
                Card(shape = MaterialTheme.shapes.large) {
                    Column(Modifier.padding(16.dp)) {
                        Text("No hay tareas")
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(selectedDayTasks, key = { it.id }) { task ->
                        CalendarAgendaTaskCard(task = task)
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    date: LocalDate?,
    selectedDate: LocalDate,
    tasksByDate: Map<LocalDate?, List<CalendarTaskUi>>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dayTasks = if (date != null) tasksByDate[date].orEmpty() else emptyList()
    val hasTasks = dayTasks.isNotEmpty()
    val hasHighPriority = dayTasks.any { it.priority.equals("Alta", ignoreCase = true) && !it.done }
    val isSelected = date == selectedDate
    val isToday = date == LocalDate.now()

    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(enabled = date != null) { onClick() },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.primaryContainer
                isToday -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (date != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = date.dayOfMonth.toString(),
                        fontWeight = if (isToday || isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )

                    if (hasTasks) {
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(
                                    if (hasHighPriority) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.primary
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarAgendaTaskCard(task: CalendarTaskUi) {
    Card(shape = MaterialTheme.shapes.large) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = {},
                    label = { Text(task.priority) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = priorityContainerColor(task.priority)
                    )
                )

                AssistChip(
                    onClick = {},
                    label = { Text(formatTimeOnly(task.dueAt?.toDate())) }
                )

                if (task.done) {
                    AssistChip(
                        onClick = {},
                        label = { Text("Hecha") }
                    )
                }
            }
        }
    }
}

@Composable
private fun priorityContainerColor(priority: String) = when (priority.lowercase()) {
    "alta" -> MaterialTheme.colorScheme.errorContainer
    "media" -> MaterialTheme.colorScheme.tertiaryContainer
    "baja" -> MaterialTheme.colorScheme.secondaryContainer
    else -> MaterialTheme.colorScheme.surfaceVariant
}

private fun priorityOrder(priority: String): Int = when (priority.lowercase()) {
    "alta" -> 0
    "media" -> 1
    "baja" -> 2
    else -> 3
}

private fun firstDayOffsetMonday(dayOfWeek: DayOfWeek): Int {
    return when (dayOfWeek) {
        DayOfWeek.MONDAY -> 0
        DayOfWeek.TUESDAY -> 1
        DayOfWeek.WEDNESDAY -> 2
        DayOfWeek.THURSDAY -> 3
        DayOfWeek.FRIDAY -> 4
        DayOfWeek.SATURDAY -> 5
        DayOfWeek.SUNDAY -> 6
    }
}

private fun formatMonthYear(month: YearMonth): String {
    val monthName = month.month.getDisplayName(TextStyle.FULL, Locale("es"))
    return monthName.replaceFirstChar { it.uppercase() } + " ${month.year}"
}

private fun formatSelectedDate(date: LocalDate): String {
    val dayName = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("es"))
    return "${dayName.replaceFirstChar { it.uppercase() }} ${date.dayOfMonth}/${date.monthValue}/${date.year}"
}

private fun formatTimeOnly(date: Date?): String {
    if (date == null) return "Sin hora"
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
}