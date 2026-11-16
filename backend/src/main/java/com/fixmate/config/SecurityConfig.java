package com.fixmate.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixmate.common.api.ApiResponse;
import com.fixmate.common.api.ErrorCode;
import com.fixmate.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final com.fixmate.security.RequestHeaderValidationFilter requestHeaderValidationFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          com.fixmate.security.RequestHeaderValidationFilter requestHeaderValidationFilter,
                          ObjectMapper objectMapper) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.requestHeaderValidationFilter = requestHeaderValidationFilter;
        this.objectMapper = objectMapper;
    }

    @Bean
    public org.springframework.boot.web.servlet.FilterRegistrationBean<JwtAuthenticationFilter> noCustomerServletFilter(JwtAuthenticationFilter filter) {
        var bean=new org.springframework.boot.web.servlet.FilterRegistrationBean<>(filter);bean.setEnabled(false);return bean;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/user/auth/wx-login",
                                "/api/v1/user/auth/refresh",
                                "/api/v1/user/auth/logout",
                                "/actuator/health",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/doc.html",
                                "/webjars/**")
                        .permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((request, response, ex) -> writeError(response, 401, "未登录或登录已失效"))
                        .accessDeniedHandler((request, response, ex) -> writeError(response, 403, "无权访问该资源")))
                .addFilterBefore(requestHeaderValidationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private void writeError(HttpServletResponse response, int status, String message) throws java.io.IOException {
        String code = status == 401 ? ErrorCode.Codes.UNAUTHORIZED : ErrorCode.Codes.FORBIDDEN;
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error(code, message)));
    }
}
