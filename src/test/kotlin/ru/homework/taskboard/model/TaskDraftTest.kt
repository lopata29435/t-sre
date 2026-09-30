package ru.homework.taskboard.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import ru.homework.taskboard.exception.InvalidTaskException

class TaskDraftTest {
    @Test
    fun `trims text before checking its length`() {
        val draft = TaskDraft("  ${"x".repeat(160)}  ", "  ${"y".repeat(4000)}  ", TaskStatus.TODO)

        assertThat(draft.title).isEqualTo("x".repeat(160))
        assertThat(draft.description).isEqualTo("y".repeat(4000))
    }

    @Test
    fun `rejects blank title`() {
        val error = assertThrows<InvalidTaskException> { TaskDraft(" \t\n ", "", TaskStatus.TODO) }

        assertThat(error).hasMessage("Укажите название")
    }

    @Test
    fun `rejects long title`() {
        val error = assertThrows<InvalidTaskException> { TaskDraft("x".repeat(161), "", TaskStatus.TODO) }

        assertThat(error).hasMessage("Название — не более 160 символов")
    }

    @Test
    fun `rejects long description`() {
        val error = assertThrows<InvalidTaskException> { TaskDraft("Задача", "x".repeat(4001), TaskStatus.TODO) }

        assertThat(error).hasMessage("Описание — не более 4000 символов")
    }
}
