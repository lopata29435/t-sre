package ru.homework.taskboard.service

import io.micrometer.core.instrument.MeterRegistry
import ru.homework.taskboard.exception.TaskNotFoundException
import ru.homework.taskboard.model.Task
import ru.homework.taskboard.model.TaskDraft
import ru.homework.taskboard.repository.TaskRepository

class TaskService(private val tasks: TaskRepository, registry: MeterRegistry) {
    private val created = registry.counter("taskboard.tasks.creations")
    private val viewed = registry.counter("taskboard.tasks.views")
    private val updated = registry.counter("taskboard.tasks.updates")
    private val deleted = registry.counter("taskboard.tasks.deletions")

    fun list(): List<Task> = tasks.findAll()

    fun get(id: Long): Task = (tasks.findById(id) ?: throw TaskNotFoundException())
        .also { viewed.increment() }

    fun create(draft: TaskDraft): Task = tasks.create(draft).also { created.increment() }

    fun update(id: Long, draft: TaskDraft): Task =
        (tasks.update(id, draft) ?: throw TaskNotFoundException()).also { updated.increment() }

    fun delete(id: Long) {
        if (!tasks.delete(id)) {
            throw TaskNotFoundException()
        }
        deleted.increment()
    }
}
