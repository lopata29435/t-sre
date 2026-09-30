package ru.homework.taskboard.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import ru.homework.taskboard.dto.TaskRequest
import ru.homework.taskboard.model.Task
import ru.homework.taskboard.service.TaskService
import java.net.URI

@RestController
@RequestMapping("/api/tasks")
class TaskController(private val tasks: TaskService) {
    @GetMapping
    fun list(): List<Task> = tasks.list()

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long): Task = tasks.get(id)

    @PostMapping
    fun create(@RequestBody request: TaskRequest): ResponseEntity<Task> {
        val task = tasks.create(request.toDraft())
        return ResponseEntity.created(URI.create("/api/tasks/${task.id}")).body(task)
    }

    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @RequestBody request: TaskRequest): Task =
        tasks.update(id, request.toDraft())

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) {
        tasks.delete(id)
    }
}
