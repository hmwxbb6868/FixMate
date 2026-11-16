package com.fixmate.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixmate.common.api.ApiResponse;
import com.fixmate.common.api.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 强制写请求携带合法格式的 X-Request-Id 与 Idempotency-Key。
 * 契约要求 8–64 位字母、数字、下划线或连字符；缺失或格式错误一律 400。
 */
@Component
public class RequestHeaderValidationFilter extends OncePerRequestFilter {

    private static final Pattern HEADER_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{8,64}$");
    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "DELETE", "PATCH");

    private final ObjectMapper objectMapper;

    public RequestHeaderValidationFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!request.getRequestURI().startsWith("/api/")) {
            chain.doFilter(request, response);
            return;
        }
        boolean write = WRITE_METHODS.contains(request.getMethod());
        String requestId = request.getHeader("X-Request-Id");
        boolean validRequestId = requestId != null && HEADER_PATTERN.matcher(requestId).matches();

        com.fixmate.common.trace.TraceContext.set(
                validRequestId ? requestId : java.util.UUID.randomUUID().toString().replace("-", ""));
        try {
            if (write && !validRequestId) {
                writeError(response, "X-Request-Id 缺失或格式不正确");
                return;
            }
            if (write) {
                String idempotencyKey = request.getHeader("Idempotency-Key");
                if (idempotencyKey == null || !HEADER_PATTERN.matcher(idempotencyKey).matches()) {
                    writeError(response, "Idempotency-Key 缺失或格式不正确");
                    return;
                }
            }
            chain.doFilter(request, response);
        } finally {
            com.fixmate.common.trace.TraceContext.clear();
        }
    }

    private void writeError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(
                ApiResponse.error(ErrorCode.Codes.VALIDATION_ERROR, message)));
    }
}
