package ru.homework.taskboard.model

import ru.homework.taskboard.exception.InvalidTaskException

class TaskDraft(title: String, description: String, val status: TaskStatus) {
    val title = title.trim()
    val description = description.trim()

    init {
        if (this.title.isBlank()) {
            throw InvalidTaskException("Укажите название")
        }
        if (this.title.length > 160) {
            throw InvalidTaskException("Название — не более 160 символов")
        }
        if (this.description.length > 4000) {
            throw InvalidTaskException("Описание — не более 4000 символов")
        }
    }
}
