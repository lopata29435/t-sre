package ru.homework.taskboard

import com.fasterxml.jackson.databind.JsonNode
import org.assertj.core.api.Assertions.assertThat
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.concurrent.CompletableFuture

@Testcontainers
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["spring.flyway.enabled=true"],
)
class TaskApiIT {
    companion object {
        @Container
        @JvmField
        val postgres = PostgreSQLContainer<Nothing>("postgres:17.11-alpine")

        @DynamicPropertySource
        @JvmStatic
        fun database(properties: DynamicPropertyRegistry) {
            properties.add("DB_URL", postgres::getJdbcUrl)
            properties.add("DB_USER", postgres::getUsername)
            properties.add("DB_PASSWORD", postgres::getPassword)
            properties.add("INSTANCE_ID") { "integration-test" }
        }
    }

    @Autowired
    lateinit var http: TestRestTemplate

    @Autowired
    lateinit var jdbc: JdbcClient

    @Autowired
    lateinit var flyway: Flyway

    @BeforeEach
    fun clearDatabase() {
        jdbc.sql("DELETE FROM tasks").update()
    }

    @Test
    fun `complete CRUD over HTTP`() {
        val created = http.postForEntity(
            "/api/tasks",
            mapOf("title" to "  Проверить бэкапы  ", "description" to "PostgreSQL", "status" to "TODO"),
            JsonNode::class.java,
        )
        assertThat(created.statusCode).isEqualTo(HttpStatus.CREATED)
        val task = requireNotNull(created.body)
        val path = "/api/tasks/${task["id"].asLong()}"
        assertThat(created.headers.location.toString()).isEqualTo(path)
        assertThat(task["title"].asText()).isEqualTo("Проверить бэкапы")
        assertThat(task["createdAt"].asText()).endsWith("Z")

        val fetched = http.getForEntity(path, JsonNode::class.java)
        assertThat(fetched.body).isEqualTo(task)
        assertThat(fetched.headers.getFirst("X-Instance-Id")).isEqualTo("integration-test")
        assertThat(fetched.headers.cacheControl).isEqualTo("no-store")
        assertThat(http.getForObject("/api/tasks", JsonNode::class.java)).hasSize(1)

        val updated = http.exchange(
            path, HttpMethod.PUT,
            HttpEntity(mapOf("title" to "Бэкапы проверены", "description" to "Готово", "status" to "DONE")),
            JsonNode::class.java,
        )
        assertThat(updated.statusCode).isEqualTo(HttpStatus.OK)
        val updatedTask = requireNotNull(updated.body)
        assertThat(updatedTask["title"].asText()).isEqualTo("Бэкапы проверены")
        assertThat(updatedTask["status"].asText()).isEqualTo("DONE")
        assertThat(updatedTask["createdAt"]).isEqualTo(task["createdAt"])

        val deleted = http.exchange(path, HttpMethod.DELETE, HttpEntity.EMPTY, String::class.java)
        assertThat(deleted.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
        assertThat(deleted.body).isNullOrEmpty()
        assertThat(http.getForEntity(path, JsonNode::class.java).statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        assertThat(http.getForObject("/api/tasks", JsonNode::class.java)).isEmpty()
    }

    @Test
    fun `invalid input never reaches database`() {
        val invalidBodies = listOf(
            """{"title":"   ","status":"TODO"}""",
            """{"status":"TODO"}""",
            """{"title":null,"status":"TODO"}""",
            """{"title":"x"}""",
            """{"title":"x","status":"UNKNOWN"}""",
            """{"title":"x","status":0}""",
            """{"title":42,"status":"TODO"}""",
            """{"title":true,"status":"TODO"}""",
            """{"title":"x","description":42,"status":"TODO"}""",
            """{"title":"x","status":null}""",
            """{"title":"x","status":"TODO","unexpected":true}""",
            """{"title":"${"x".repeat(161)}","status":"TODO"}""",
            """{"title":"x","description":"${"x".repeat(4001)}","status":"TODO"}""",
            "null",
            "not json",
        )
        val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
        for (body in invalidBodies) {
            val request = HttpEntity(body, headers)
            val created = http.postForEntity("/api/tasks", request, JsonNode::class.java)
            assertThat(created.statusCode).describedAs(body).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(requireNotNull(created.body).hasNonNull("message")).isTrue()
            val updated = http.exchange("/api/tasks/0", HttpMethod.PUT, request, JsonNode::class.java)
            assertThat(updated.statusCode).describedAs(body).isEqualTo(HttpStatus.BAD_REQUEST)
        }
        assertThat(taskCount()).isZero()
    }

    @Test
    fun `missing tasks and invalid IDs have predictable errors`() {
        assertThat(http.getForEntity("/api/tasks/0", String::class.java).statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        assertThat(http.exchange("/api/tasks/0", HttpMethod.DELETE, HttpEntity.EMPTY, String::class.java).statusCode)
            .isEqualTo(HttpStatus.NOT_FOUND)
        val updated = http.exchange(
            "/api/tasks/0", HttpMethod.PUT,
            HttpEntity(mapOf("title" to "x", "status" to "TODO")), String::class.java,
        )
        assertThat(updated.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        assertThat(http.getForEntity("/api/tasks/not-a-number", String::class.java).statusCode)
            .isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `SQL and HTML are stored as text`() {
        val title = "'; DROP TABLE tasks; -- <script>alert(1)</script>"
        val created = http.postForEntity(
            "/api/tasks",
            mapOf("title" to title, "description" to null, "status" to "IN_PROGRESS"),
            JsonNode::class.java,
        )
        assertThat(created.statusCode).isEqualTo(HttpStatus.CREATED)
        val task = requireNotNull(created.body)
        assertThat(task["title"].asText()).isEqualTo(title)
        assertThat(task["description"].asText()).isEmpty()
        assertThat(taskCount()).isEqualTo(1)
    }

    @Test
    fun `concurrent requests have unique IDs and persist all tasks`() {
        val requests = (1..12).map { index ->
            CompletableFuture.supplyAsync {
                http.postForEntity(
                    "/api/tasks", mapOf("title" to "task $index", "status" to "TODO"), JsonNode::class.java,
                )
            }
        }
        val responses = requests.map { it.join() }
        assertThat(responses).allSatisfy { response ->
            assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        }
        assertThat(responses.map { requireNotNull(it.body)["id"].asLong() }.distinct()).hasSize(12)
        assertThat(taskCount()).isEqualTo(12)
    }

    @Test
    fun `migration can be repeated without data loss`() {
        http.postForEntity("/api/tasks", mapOf("title" to "Сохранить", "status" to "TODO"), JsonNode::class.java)
        assertThat(flyway.migrate().migrationsExecuted).isZero()
        assertThat(taskCount()).isEqualTo(1)
    }

    @Test
    fun `missing schema disables readiness but keeps liveness`() {
        jdbc.sql("ALTER TABLE tasks RENAME TO tasks_unavailable").update()
        try {
            assertThat(http.getForEntity("/health/live", String::class.java).statusCode).isEqualTo(HttpStatus.OK)
            assertThat(http.getForEntity("/health/ready", String::class.java).statusCode)
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
            val response = http.getForEntity("/api/tasks", JsonNode::class.java)
            assertThat(response.statusCode).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
            assertThat(requireNotNull(response.body)["message"].asText()).isEqualTo("База данных временно недоступна")
        } finally {
            jdbc.sql("ALTER TABLE tasks_unavailable RENAME TO tasks").update()
        }
        assertThat(http.getForEntity("/health/ready", String::class.java).statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `serves frontend and health probes`() {
        val page = http.getForEntity("/", String::class.java)
        assertThat(page.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(page.body).contains("Taskboard", "task-form", "/app.js")
        assertThat(http.getForEntity("/app.js", String::class.java).statusCode).isEqualTo(HttpStatus.OK)
        assertThat(http.getForEntity("/styles.css", String::class.java).statusCode).isEqualTo(HttpStatus.OK)
        assertThat(requireNotNull(http.getForObject("/health/live", JsonNode::class.java))["status"].asText())
            .isEqualTo("UP")
        assertThat(http.getForEntity("/health/ready", JsonNode::class.java).statusCode).isEqualTo(HttpStatus.OK)
    }

    private fun taskCount(): Int = jdbc.sql("SELECT count(*) FROM tasks").query(Int::class.javaObjectType).single()
}
