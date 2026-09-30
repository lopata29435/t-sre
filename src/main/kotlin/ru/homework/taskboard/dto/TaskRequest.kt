package ru.homework.taskboard.dto

import ru.homework.taskboard.model.TaskDraft
import ru.homework.taskboard.model.TaskStatus

data class TaskRequest(
    val title: String,
    val status: TaskStatus,
    val description: String? = null,
) {
    fun toDraft(): TaskDraft = TaskDraft(title, description.orEmpty(), status)
}
