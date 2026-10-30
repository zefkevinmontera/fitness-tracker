package com.example.fitnesstracker

data class Workout(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val durationMinutes: Int = 0,
    val caloriesBurned: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)