package ru.homework.taskboard.controller

import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import ru.homework.taskboard.exception.TaskNotFoundException
import ru.homework.taskboard.exception.InvalidTaskException

@RestControllerAdvice
class ApiExceptionHandler {
    private val log = LoggerFactory.getLogger(ApiExceptionHandler::class.java)

    @ExceptionHandler(InvalidTaskException::class)
    fun validation(error: InvalidTaskException): ResponseEntity<Map<String, String>> =
        ResponseEntity.badRequest().body(mapOf("message" to (error.message ?: "Неверные данные задачи")))

    @ExceptionHandler(HttpMessageNotReadableException::class, MethodArgumentTypeMismatchException::class)
    fun badRequest(): ResponseEntity<Map<String, String>> =
        ResponseEntity.badRequest().body(mapOf("message" to "Неверный JSON, статус или идентификатор"))

    @ExceptionHandler(TaskNotFoundException::class)
    fun notFound(): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(404).body(mapOf("message" to "Задача не найдена"))

    @ExceptionHandler(DataAccessException::class)
    fun database(error: DataAccessException): ResponseEntity<Map<String, String>> {
        log.error("Database request failed: {}", error.javaClass.simpleName)
        return ResponseEntity.status(503).body(mapOf("message" to "База данных временно недоступна"))
    }
}
