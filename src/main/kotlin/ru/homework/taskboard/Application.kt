package ru.homework.taskboard

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.core.env.Profiles

@SpringBootApplication
class Application

fun main(args: Array<String>) {
    val context = runApplication<Application>(*args)
    if (context.environment.acceptsProfiles(Profiles.of("migrate"))) {
        context.close()
    }
}
