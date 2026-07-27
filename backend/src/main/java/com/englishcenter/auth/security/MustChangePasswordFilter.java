package com.englishcenter.auth.security;

import com.englishcenter.common.api.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class MustChangePasswordFilter extends OncePerRequestFilter {
    private static final Set<String> ALLOWED_PATHS = Set.of(
            "/api/auth/me",
            "/api/auth/change-password",
            "/api/auth/login"
    );

    private final ObjectMapper objectMapper;

    public MustChangePasswordFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof AccountPrincipal principal
                && principal.mustChangePassword()) {
            String path = request.getRequestURI();
            if (!ALLOWED_PATHS.contains(path)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                objectMapper.writeValue(
                        response.getOutputStream(),
                        ErrorResponse.of("Bạn phải đổi mật khẩu trước khi tiếp tục sử dụng hệ thống.")
                );
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
