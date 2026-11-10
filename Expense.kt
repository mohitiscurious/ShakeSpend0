package com.shakespend.app

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountPaise: Long,
    val category: String,
    val note: String,
    val createdAt: Long = System.currentTimeMillis()
)
