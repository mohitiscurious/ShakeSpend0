package com.shakespend.app

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Insert suspend fun insert(expense: Expense)
    @Query("SELECT * FROM expenses ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Expense>>
    @Query("SELECT COALESCE(SUM(amountPaise), 0) FROM expenses WHERE createdAt >= :start AND createdAt < :end")
    fun observeMonthTotal(start: Long, end: Long): Flow<Long>
}
