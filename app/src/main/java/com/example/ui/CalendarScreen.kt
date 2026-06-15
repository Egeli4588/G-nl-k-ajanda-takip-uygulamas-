package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.Task
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.*

// Category configurations
data class CategoryConfig(
    val name: String,
    val color: Color,
    val iconName: String
)

val Categories = listOf(
    CategoryConfig("İş", Color(0xFF1E88E5), "💼"),
    CategoryConfig("Kişisel", Color(0xFF43A047), "🧘"),
    CategoryConfig("Alışveriş", Color(0xFFFB8C00), "🛒"),
    CategoryConfig("Sağlık", Color(0xFFE53935), "❤️"),
    CategoryConfig("Genel", Color(0xFF8E24AA), "🎯")
)

fun getCategoryConfig(category: String): CategoryConfig {
    return Categories.find { it.name.lowercase() == category.lowercase() }
        ?: CategoryConfig(category, Color(0xFF757575), "📝")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: TaskViewModel,
    modifier: Modifier = Modifier
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val currentMonth by viewModel.currentMonth.collectAsState()
    val calendarMode by viewModel.calendarMode.collectAsState()
    val selectedCategoryByState by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val allTasks by viewModel.allTasks.collectAsState()
    val tasksForDay by viewModel.tasksForSelectedDate.collectAsState()

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    var showDatePickerDialogForForm by remember { mutableStateOf(false) }

    val turkishMonths = listOf(
        "Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran",
        "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık"
    )

    fun getMonthAndYearString(yearMonth: YearMonth): String {
        val monthName = if (yearMonth.monthValue in 1..12) turkishMonths[yearMonth.monthValue - 1] else ""
        return "$monthName ${yearMonth.year}"
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                    .statusBarsPadding()
                    .padding(bottom = 8.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = getMonthAndYearString(currentMonth),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = selectedDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("tr"))),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Week vs Month Toggles & Navigation
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (calendarMode == CalendarMode.WEEK) {
                                    viewModel.selectDate(selectedDate.minusWeeks(1))
                                } else {
                                    viewModel.changeMonth(-1)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Geri"
                            )
                        }

                        // Mode Selector Button
                        Button(
                            onClick = { viewModel.toggleCalendarMode() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("calendar_mode_toggle")
                        ) {
                            Icon(
                                imageVector = if (calendarMode == CalendarMode.WEEK) Icons.Default.DateRange else Icons.Default.Menu,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (calendarMode == CalendarMode.WEEK) "Haftalık" else "Aylık",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = {
                                if (calendarMode == CalendarMode.WEEK) {
                                    viewModel.selectDate(selectedDate.plusWeeks(1))
                                } else {
                                    viewModel.changeMonth(1)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "İleri"
                            )
                        }
                    }
                }

                // Calendar Viewport
                AnimatedContent(
                    targetState = calendarMode,
                    transitionSpec = {
                        slideInVertically { height -> -height } + fadeIn() togetherWith
                                slideOutVertically { height -> height } + fadeOut()
                    },
                    label = "CalendarAnimation"
                ) { mode ->
                    when (mode) {
                        CalendarMode.WEEK -> {
                            WeeklyCalendarComponent(
                                selectedDate = selectedDate,
                                allTasks = allTasks,
                                onDateSelected = { viewModel.selectDate(it) }
                            )
                        }
                        CalendarMode.MONTH -> {
                            MonthlyCalendarComponent(
                                selectedDate = selectedDate,
                                currentMonth = currentMonth,
                                allTasks = allTasks,
                                onDateSelected = { viewModel.selectDate(it) }
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    taskToEdit = null
                    showAddTaskDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("add_task_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Yeni Görev Ekle")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Stats Indicator Widget
            StatsBanner(tasksForSelectedDate = tasksForDay)

            // Search Bar Component
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Görevlerde ara...", style = MaterialTheme.typography.bodyMedium) },
                prefix = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("search_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
            )

            // Category Filter Widget
            CategoryFilterBar(
                selectedCategory = selectedCategoryByState,
                onCategorySelected = { viewModel.selectCategory(it) }
            )

            // Tasks List
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (tasksForDay.isEmpty()) {
                    EmptyTaskState(
                        isFiltered = selectedCategoryByState != null || searchQuery.isNotEmpty(),
                        onClearFilters = {
                            viewModel.selectCategory(null)
                            viewModel.setSearchQuery("")
                        }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("task_list"),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(tasksForDay, key = { it.id }) { task ->
                            TaskRowItem(
                                task = task,
                                onToggleComplete = { viewModel.toggleTaskCompletion(task) },
                                onDelete = { viewModel.deleteTask(task) },
                                onEdit = {
                                    taskToEdit = task
                                    showAddTaskDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Task Add/Edit Modal Dialog
    if (showAddTaskDialog) {
        TaskAddEditDialog(
            task = taskToEdit,
            initialDate = selectedDate,
            onDismiss = { showAddTaskDialog = false },
            onSave = { title, desc, date, time, cat, priority ->
                if (taskToEdit == null) {
                    viewModel.insertTask(title, desc, date, time, cat, priority)
                } else {
                    val updated = taskToEdit!!.copy(
                        title = title,
                        description = desc,
                        dateEpochDays = date.toEpochDay(),
                        timeString = time,
                        category = cat,
                        priority = priority
                    )
                    viewModel.updateTask(updated)
                }
                showAddTaskDialog = false
            }
        )
    }
}

@Composable
fun WeeklyCalendarComponent(
    selectedDate: LocalDate,
    allTasks: List<Task>,
    onDateSelected: (LocalDate) -> Unit
) {
    // Generate the week containing the selected date
    val startOfWeek = selectedDate.minusDays((selectedDate.dayOfWeek.value - 1).toLong())
    val days = (0..6).map { startOfWeek.plusDays(it.toLong()) }

    val turkishWeekdays = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("weekly_calendar"),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        days.forEach { date ->
            val isSelected = date == selectedDate
            val isToday = date == LocalDate.now()
            val dayOfWeekIndex = date.dayOfWeek.value - 1
            val dayLabel = turkishWeekdays[getValidIndex(dayOfWeekIndex, 7)]

            // Gather tasks for this day to draw indicator dots
            val tasksForDay = allTasks.filter { it.getLocalDate() == date }
            val hasIncomplete = tasksForDay.any { !it.isCompleted }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 3.dp)
                    .clickable { onDateSelected(date) }
                    .testTag("week_day_${date.dayOfMonth}"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else if (isToday) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    contentColor = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else if (isToday) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = dayLabel,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = date.dayOfMonth.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    // Dots layout
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.height(6.dp)
                    ) {
                        if (tasksForDay.isNotEmpty()) {
                            val displayTasks = tasksForDay.take(3)
                            displayTasks.forEach { task ->
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (task.isCompleted) Color.Gray.copy(alpha = 0.5f)
                                            else getCategoryConfig(task.category).color
                                        )
                                )
                            }
                            if (tasksForDay.size > 3) {
                                Box(
                                    modifier = Modifier
                                        .size(3.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color.White else Color.Gray)
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.size(5.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MonthlyCalendarComponent(
    selectedDate: LocalDate,
    currentMonth: YearMonth,
    allTasks: List<Task>,
    onDateSelected: (LocalDate) -> Unit
) {
    val turkishWeekdays = listOf("Pt", "Sa", "Ça", "Pe", "Cu", "Ct", "Pz")

    // Calculation of grid dates
    val firstOfMonth = currentMonth.atDay(1)
    val startDayOfWeek = firstOfMonth.dayOfWeek.value // 1 = Monday, 7 = Sunday
    val daysToBefore = startDayOfWeek - 1

    val gridDates = remember(currentMonth) {
        val list = mutableListOf<LocalDate>()
        val prevMonth = currentMonth.minusMonths(1)
        val prevMonthLen = prevMonth.lengthOfMonth()
        for (i in daysToBefore - 1 downTo 0) {
            list.add(prevMonth.atDay(prevMonthLen - i))
        }
        for (i in 1..currentMonth.lengthOfMonth()) {
            list.add(currentMonth.atDay(i))
        }
        val totalCells = if (list.size <= 35) 35 else 42
        val nextMonth = currentMonth.plusMonths(1)
        var nextDay = 1
        while (list.size < totalCells) {
            list.add(nextMonth.atDay(nextDay++))
        }
        list
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("monthly_calendar")
    ) {
        // Week Days Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            turkishWeekdays.forEach { day ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }

        // Days Grid
        val rows = gridDates.chunked(7)
        rows.forEach { weekRow ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weekRow.forEach { date ->
                    val isSelected = date == selectedDate
                    val isToday = date == LocalDate.now()
                    val isCurrentMonth = YearMonth.from(date) == currentMonth

                    val dayTasks = allTasks.filter { it.getLocalDate() == date }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(2.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when {
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                    else -> Color.Transparent
                                }
                            )
                            .clickable { onDateSelected(date) }
                            .testTag("month_day_${date.dayOfMonth}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxHeight().padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = date.dayOfMonth.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                                color = when {
                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                    isCurrentMonth -> MaterialTheme.colorScheme.onSurface
                                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                }
                            )

                            // Tiny indicators
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier.height(4.dp)
                            ) {
                                if (dayTasks.isNotEmpty()) {
                                    dayTasks.take(3).forEach { task ->
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (task.isCompleted) Color.Gray.copy(alpha = 0.4f)
                                                    else if (isSelected) Color.White
                                                    else getCategoryConfig(task.category).color
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatsBanner(tasksForSelectedDate: List<Task>) {
    val totalCount = tasksForSelectedDate.size
    val completedCount = tasksForSelectedDate.count { it.isCompleted }
    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Günün İlerlemesi",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Text(
                    text = "$completedCount/$totalCount Tamamlandı",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            )
        }
    }
}

@Composable
fun CategoryFilterBar(
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) },
                label = { Text("Hepsi") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }

        items(Categories) { config ->
            FilterChip(
                selected = selectedCategory == config.name,
                onClick = { onCategorySelected(config.name) },
                label = { Text(config.name) },
                leadingIcon = {
                    Text(text = config.iconName, fontSize = 14.sp)
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = config.color.copy(alpha = 0.2f),
                    selectedLabelColor = config.color,
                    selectedLeadingIconColor = config.color
                ),
                border = if (selectedCategory == config.name) {
                    FilterChipDefaults.filterChipBorder(
                        selected = true,
                        enabled = true,
                        selectedBorderColor = config.color,
                        selectedBorderWidth = 1.5.dp
                    )
                } else null
            )
        }
    }
}

@Composable
fun TaskRowItem(
    task: Task,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val categoryConfig = getCategoryConfig(task.category)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .testTag("task_item_${task.id}"),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            if (task.isCompleted) Color.Transparent else categoryConfig.color.copy(alpha = 0.3f)
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (task.isCompleted) 0.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Task status circle checkbox
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        if (task.isCompleted) categoryConfig.color.copy(alpha = 0.15f)
                        else Color.Transparent
                    )
                    .border(
                        2.dp,
                        if (task.isCompleted) categoryConfig.color else MaterialTheme.colorScheme.outline,
                        CircleShape
                    )
                    .clickable { onToggleComplete() }
                    .testTag("checkbox_${task.id}"),
                contentAlignment = Alignment.Center
            ) {
                if (task.isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Tamamlandı",
                        tint = categoryConfig.color,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Text contents
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onEdit() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (task.isCompleted) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Priority indicator badge
                    if (!task.isCompleted) {
                        val priorityColor = when (task.priority) {
                            "Yüksek" -> Color(0xFFE53935)
                            "Orta" -> Color(0xFFFBC02D)
                            else -> Color(0xFF43A047)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(priorityColor.copy(alpha = 0.15f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = task.priority,
                                color = priorityColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (task.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Bottom badges row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Category Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(categoryConfig.color.copy(alpha = 0.1f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = categoryConfig.iconName, fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = task.category,
                                color = categoryConfig.color,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Time badge
                    if (!task.timeString.isNullOrEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow, // Using play arrow as clocks substitute securely
                                    contentDescription = null,
                                    modifier = Modifier.size(10.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = task.timeString,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Options/Delete
            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Düzenle",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_task_${task.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Sil",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyTaskState(
    isFiltered: Boolean,
    onClearFilters: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (isFiltered) Icons.Default.Info else Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isFiltered) "Arama kriterlerine uygun görev bulunamadı" else "Harika! Bugün için tüm görevler bitti.",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.testTag("empty_state_title")
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isFiltered) {
                "Filtreleri sıfırlayarak tüm görevleri görmeyi deneyebilirsiniz."
            } else {
                "Yeni ve heyecan verici planlar eklemek için sağ alttaki seçeneğe dokunun."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp).testTag("empty_state_desc")
        )

        if (isFiltered) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onClearFilters) {
                Text("Filtreleri Temizle")
            }
        }
    }
}

@Composable
fun TaskAddEditDialog(
    task: Task?,
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onSave: (String, String, LocalDate, String?, String, String) -> Unit
) {
    var title by remember { mutableStateOf(task?.title ?: "") }
    var description by remember { mutableStateOf(task?.description ?: "") }
    var selectedDateState by remember { mutableStateOf(task?.getLocalDate() ?: initialDate) }
    var timeString by remember { mutableStateOf(task?.timeString ?: "") }
    var selectedCategoryState by remember { mutableStateOf(task?.category ?: "İş") }
    var selectedPriorityState by remember { mutableStateOf(task?.priority ?: "Orta") }

    var titleError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("add_task_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text(
                        text = if (task == null) "Yeni Görev Oluştur" else "Görevi Düzenle",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                // Title Input
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            if (it.trim().isNotEmpty()) titleError = false
                        },
                        label = { Text("Başlık") },
                        isError = titleError,
                        supportingText = {
                            if (titleError) {
                                Text("Lütfen bir başlık girin.", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("task_title_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Description Input
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Açıklama (İsteğe bağlı)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("task_desc_input"),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 3
                    )
                }

                // Date Picker Input Setup (Manual text representation to avoid dynamic dialog issues)
                item {
                    var pickerExpanded by remember { mutableStateOf(false) }
                    Column {
                        OutlinedTextField(
                            value = selectedDateState.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale("tr"))),
                            onValueChange = {},
                            label = { Text("Tarih") },
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { pickerExpanded = !pickerExpanded }) {
                                    Icon(imageVector = Icons.Default.DateRange, contentDescription = "Tarih Seç")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { pickerExpanded = !pickerExpanded },
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Inline date picker to select +/- 7 days safely
                        if (pickerExpanded) {
                            Text(
                                text = "Hızlı Tarih Seçimi",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
                            )
                            LazyRow(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val datesOffset = (-3..7).map { LocalDate.now().plusDays(it.toLong()) }
                                items(datesOffset) { dateVal ->
                                    val isToday = dateVal == LocalDate.now()
                                    val isSelectedDate = dateVal == selectedDateState
                                    val formatStr = dateVal.format(DateTimeFormatter.ofPattern("d MMM", Locale("tr")))
                                    FilterChip(
                                        selected = isSelectedDate,
                                        onClick = {
                                            selectedDateState = dateVal
                                            pickerExpanded = false
                                        },
                                        label = {
                                            Text(
                                                text = if (isToday) "Bugün ($formatStr)" else formatStr,
                                                fontSize = 11.sp
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Time picker (Using a simplified standard formatted text box like "09:00", "13:30")
                item {
                    OutlinedTextField(
                        value = timeString,
                        onValueChange = { timeString = it },
                        label = { Text("Saat (Örn: 14:30 veya boş)") },
                        placeholder = { Text("SAAT:DAKİKA / Yok") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("task_time_input"),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
                        )
                    )
                }

                // Category badges picker
                item {
                    Text(
                        text = "Kategori",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(Categories) { config ->
                            val isSelected = selectedCategoryState == config.name
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) config.color else config.color.copy(alpha = 0.12f)
                                    )
                                    .border(
                                        2.dp,
                                        if (isSelected) config.color else Color.Transparent,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedCategoryState = config.name }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = config.iconName, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = config.name,
                                        color = if (isSelected) Color.White else config.color,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Priority switcher
                item {
                    Text(
                        text = "Öncelik Seviyesi",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Düşük", "Orta", "Yüksek").forEach { level ->
                            val isSelected = selectedPriorityState == level
                            val baseColor = when (level) {
                                "Yüksek" -> Color(0xFFE53935)
                                "Orta" -> Color(0xFFFBC02D)
                                else -> Color(0xFF43A047)
                            }
                            Button(
                                onClick = { selectedPriorityState = level },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) baseColor else baseColor.copy(alpha = 0.12f),
                                    contentColor = if (isSelected) Color.White else baseColor
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(level, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Action Buttons to Save or Dismiss
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Vazgeç")
                        }
                        Button(
                            onClick = {
                                if (title.trim().isEmpty()) {
                                    titleError = true
                                } else {
                                    onSave(
                                        title.trim(),
                                        description.trim(),
                                        selectedDateState,
                                        timeString.trim().ifEmpty { null },
                                        selectedCategoryState,
                                        selectedPriorityState
                                    )
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_task_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Kaydet")
                        }
                    }
                }
            }
        }
    }
}

// Utility to wrap calendar weekday calculations safely
private fun getValidIndex(index: Int, modulo: Int): Int {
    val r = index % modulo
    return if (r < 0) r + modulo else r
}
