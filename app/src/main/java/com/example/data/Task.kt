package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val description: String,
    val dateEpochDays: Long, // LocalDate.toEpochDay()
    val timeString: String? = null, // "HH:MM" format or null for all-day
    val category: String = "Genel", // e.g. "İş", "Kişisel", "Alışveriş", "Sağlık"
    val priority: String = "Orta", // "Düşük", "Orta", "Yüksek"
    val isCompleted: Boolean = false
) {
    fun getLocalDate(): LocalDate {
        return LocalDate.ofEpochDay(dateEpochDays)
    }
}
