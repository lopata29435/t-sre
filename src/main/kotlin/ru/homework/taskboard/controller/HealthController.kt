package ru.homework.taskboard.controller

import org.springframework.dao.DataAccessException
import org.springframework.http.ResponseEntity
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class HealthController(private val jdbc: JdbcClient) {
    @GetMapping("/health/live")
    fun live(): Map<String, String> = mapOf("status" to "UP")

    @GetMapping("/health/ready")
    fun ready(): ResponseEntity<Map<String, String>> {
        return try {
            jdbc.sql("SELECT id FROM tasks LIMIT 1").query(Long::class.javaObjectType).list()
            ResponseEntity.ok(mapOf("status" to "UP"))
        } catch (error: DataAccessException) {
            ResponseEntity.status(503).body(mapOf("status" to "DOWN"))
        }
    }
}
