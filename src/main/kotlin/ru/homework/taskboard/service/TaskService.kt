package ru.homework.taskboard.service

import ru.homework.taskboard.exception.TaskNotFoundException
import ru.homework.taskboard.model.Task
import ru.homework.taskboard.model.TaskDraft
import ru.homework.taskboard.repository.TaskRepository

class TaskService(private val tasks: TaskRepository) {
    fun list(): List<Task> = tasks.findAll()

    fun get(id: Long): Task = tasks.findById(id) ?: throw TaskNotFoundException()

    fun create(draft: TaskDraft): Task = tasks.create(draft)

    fun update(id: Long, draft: TaskDraft): Task =
        tasks.update(id, draft) ?: throw TaskNotFoundException()

    fun delete(id: Long) {
        if (!tasks.delete(id)) {
            throw TaskNotFoundException()
        }
    }
}
