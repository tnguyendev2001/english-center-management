package com.englishcenter.auth.security;

import com.englishcenter.common.api.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class JsonAuthEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper;

    public JsonAuthEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String message = authException.getMessage();
        if (message == null || message.isBlank() || message.contains("Full authentication is required")) {
            message = "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.";
        }
        objectMapper.writeValue(response.getOutputStream(), ErrorResponse.of(message));
    }
}
