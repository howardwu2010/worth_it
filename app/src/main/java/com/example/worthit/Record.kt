package com.example.worthit

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "record_table")
data class Record(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val timestamp: Long,
    val isSpent: Boolean = false,
    val isRegret: Boolean = false
)