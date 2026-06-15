package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY dateEpochDays ASC, CASE WHEN timeString IS NULL THEN 1 ELSE 0 END, timeString ASC")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE dateEpochDays = :dateEpochDays ORDER BY CASE WHEN timeString IS NULL THEN 1 ELSE 0 END, timeString ASC")
    fun getTasksOnDate(dateEpochDays: Long): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("UPDATE tasks SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun updateStatus(id: Int, isCompleted: Boolean)
}
