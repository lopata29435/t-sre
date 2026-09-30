package ru.homework.taskboard.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class RequestLogFilter(@Value("\${app.instance-id}") private val instanceId: String) : OncePerRequestFilter() {
    private val log = LoggerFactory.getLogger(RequestLogFilter::class.java)

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        val startedAt = System.nanoTime()
        val path = request.requestURI.replace('\r', '_').replace('\n', '_')
        response.setHeader("X-Instance-Id", instanceId)
        if (path.startsWith("/api/") || path.startsWith("/health/")) {
            response.setHeader("Cache-Control", "no-store")
        }
        try {
            chain.doFilter(request, response)
        } finally {
            val durationMs = (System.nanoTime() - startedAt) / 1_000_000
            log.info(
                "instance={} method={} path={} status={} duration_ms={}",
                instanceId, request.method, path, response.status, durationMs,
            )
        }
    }
}
