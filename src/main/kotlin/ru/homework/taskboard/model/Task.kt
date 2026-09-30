package ru.homework.taskboard.model

import java.time.Instant

data class Task(
    val id: Long,
    val title: String,
    val description: String,
    val status: TaskStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
)
