package ru.homework.taskboard.repository

import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import ru.homework.taskboard.model.Task
import ru.homework.taskboard.model.TaskDraft
import ru.homework.taskboard.model.TaskStatus

@Repository
class JdbcTaskRepository(private val jdbc: JdbcClient) : TaskRepository {
    private val taskMapper = RowMapper { row, _ ->
        Task(
            id = row.getLong("id"),
            title = row.getString("title"),
            description = row.getString("description"),
            status = TaskStatus.valueOf(row.getString("status")),
            createdAt = row.getTimestamp("created_at").toInstant(),
            updatedAt = row.getTimestamp("updated_at").toInstant(),
        )
    }

    override fun findAll(): List<Task> = jdbc.sql(
        """
        SELECT id, title, description, status, created_at, updated_at
        FROM tasks
        ORDER BY id DESC
        """.trimIndent()
    )
        .query(taskMapper)
        .list()

    override fun findById(id: Long): Task? = jdbc.sql(
        """
        SELECT id, title, description, status, created_at, updated_at
        FROM tasks
        WHERE id = :id
        """.trimIndent()
    )
        .param("id", id)
        .query(taskMapper)
        .optional()
        .orElse(null)

    override fun create(draft: TaskDraft): Task = jdbc.sql(
        """
        INSERT INTO tasks (title, description, status)
        VALUES (:title, :description, :status)
        RETURNING id, title, description, status, created_at, updated_at
        """.trimIndent()
    )
        .param("title", draft.title)
        .param("description", draft.description)
        .param("status", draft.status.name)
        .query(taskMapper)
        .single()

    override fun update(id: Long, draft: TaskDraft): Task? = jdbc.sql(
        """
        UPDATE tasks
        SET title = :title,
            description = :description,
            status = :status,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = :id
        RETURNING id, title, description, status, created_at, updated_at
        """.trimIndent()
    )
        .param("id", id)
        .param("title", draft.title)
        .param("description", draft.description)
        .param("status", draft.status.name)
        .query(taskMapper)
        .optional()
        .orElse(null)

    override fun delete(id: Long): Boolean {
        val deletedRows = jdbc.sql("DELETE FROM tasks WHERE id = :id")
            .param("id", id)
            .update()
        return deletedRows == 1
    }
}
