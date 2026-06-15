package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Task
import com.example.data.TaskDatabase
import com.example.data.TaskRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

enum class CalendarMode {
    WEEK, MONTH
}

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TaskRepository

    // Central state tracking
    val selectedDate = MutableStateFlow<LocalDate>(LocalDate.now())
    val currentMonth = MutableStateFlow<YearMonth>(YearMonth.now())
    val calendarMode = MutableStateFlow<CalendarMode>(CalendarMode.WEEK)
    val selectedCategory = MutableStateFlow<String?>(null)
    val searchQuery = MutableStateFlow<String>("")

    // All tasks from repository
    private val _allTasks = MutableStateFlow<List<Task>>(emptyList())
    val allTasks: StateFlow<List<Task>> = _allTasks.asStateFlow()

    // Tasks specifically filtered for the selected date & filter category / search query
    val tasksForSelectedDate: StateFlow<List<Task>> = combine(
        selectedDate,
        _allTasks,
        selectedCategory,
        searchQuery
    ) { date, tasks, category, query ->
        tasks.filter { task ->
            task.getLocalDate() == date &&
                    (category == null || task.category == category) &&
                    (query.isEmpty() || task.title.contains(query, ignoreCase = true) || task.description.contains(query, ignoreCase = true))
        }
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        val database = TaskDatabase.getDatabase(application)
        repository = TaskRepository(database.taskDao())

        // Fetch all tasks reactively
        viewModelScope.launch {
            repository.allTasks.collect { tasks ->
                _allTasks.value = tasks
                if (tasks.isEmpty()) {
                    createSampleData()
                }
            }
        }
    }

    private fun createSampleData() {
        viewModelScope.launch {
            val today = LocalDate.now()
            val sampleTasks = listOf(
                Task(
                    title = "Haftalık Ekip Toplantısı",
                    description = "Gelecek hafta planlaması ve iş dağılımı konuşulacak.",
                    dateEpochDays = today.toEpochDay(),
                    timeString = "10:00",
                    category = "İş",
                    priority = "Yüksek",
                    isCompleted = false
                ),
                Task(
                    title = "Market Alışverişi",
                    description = "Meyve, süt, yumurta ve taze sebzeler alınacak.",
                    dateEpochDays = today.toEpochDay(),
                    timeString = "17:30",
                    category = "Alışveriş",
                    priority = "Orta",
                    isCompleted = false
                ),
                Task(
                    title = "Günlük Spor Antrenmanı",
                    description = "30 dakika kardiyo ve fonksiyonel egzersiz.",
                    dateEpochDays = today.toEpochDay(),
                    timeString = "08:00",
                    category = "Sağlık",
                    priority = "Düşük",
                    isCompleted = true
                ),
                Task(
                    title = "Diş Muayenesi",
                    description = "Diş hekimi randevusu kontrolü.",
                    dateEpochDays = today.plusDays(1).toEpochDay(),
                    timeString = "14:15",
                    category = "Sağlık",
                    priority = "Yüksek",
                    isCompleted = false
                ),
                Task(
                    title = "Kitap Okuma & Meditasyon",
                    description = "Yeni kitaptan 30 sayfa oku ve zihnini dinlendir.",
                    dateEpochDays = today.minusDays(1).toEpochDay(),
                    timeString = "21:30",
                    category = "Kişisel",
                    priority = "Düşük",
                    isCompleted = true
                )
            )
            for (task in sampleTasks) {
                repository.insertTask(task)
            }
        }
    }

    fun selectDate(date: LocalDate) {
        selectedDate.value = date
        currentMonth.value = YearMonth.from(date)
    }

    fun changeMonth(offsetMonths: Long) {
        val nextMonth = currentMonth.value.plusMonths(offsetMonths)
        currentMonth.value = nextMonth
        // Set selected date to the first day of that month or stay on same day index if exists
        val dayToSelect = if (selectedDate.value.dayOfMonth > nextMonth.lengthOfMonth()) {
            nextMonth.atDay(nextMonth.lengthOfMonth())
        } else {
            nextMonth.atDay(selectedDate.value.dayOfMonth)
        }
        selectedDate.value = dayToSelect
    }

    fun toggleCalendarMode() {
        calendarMode.value = if (calendarMode.value == CalendarMode.WEEK) CalendarMode.MONTH else CalendarMode.WEEK
    }

    fun selectCategory(category: String?) {
        selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun insertTask(
        title: String,
        description: String,
        date: LocalDate,
        time: String?,
        category: String,
        priority: String
    ) {
        viewModelScope.launch {
            val task = Task(
                title = title,
                description = description,
                dateEpochDays = date.toEpochDay(),
                timeString = time,
                category = category,
                priority = priority,
                isCompleted = false
            )
            repository.insertTask(task)
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            repository.updateStatus(task.id, !task.isCompleted)
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            repository.updateTask(task)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }
}
