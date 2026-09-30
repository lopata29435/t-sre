package ru.homework.taskboard.repository

import ru.homework.taskboard.model.Task
import ru.homework.taskboard.model.TaskDraft

interface TaskRepository {
    fun findAll(): List<Task>
    fun findById(id: Long): Task?
    fun create(draft: TaskDraft): Task
    fun update(id: Long, draft: TaskDraft): Task?
    fun delete(id: Long): Boolean
}
