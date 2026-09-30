package ru.homework.taskboard.config

import com.fasterxml.jackson.databind.cfg.CoercionAction
import com.fasterxml.jackson.databind.cfg.CoercionInputShape
import com.fasterxml.jackson.databind.type.LogicalType
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.homework.taskboard.repository.TaskRepository
import ru.homework.taskboard.service.TaskService

@Configuration(proxyBeanMethods = false)
class ApplicationConfig {
    @Bean
    fun taskService(repository: TaskRepository): TaskService = TaskService(repository)

    @Bean
    fun strictTextFields(): Jackson2ObjectMapperBuilderCustomizer = Jackson2ObjectMapperBuilderCustomizer { builder ->
        builder.postConfigurer { mapper ->
            mapper.coercionConfigFor(LogicalType.Textual)
                .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail)
        }
    }
}
