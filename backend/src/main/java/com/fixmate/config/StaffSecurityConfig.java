package com.fixmate.config;

import com.fixmate.security.*;
import com.fixmate.common.api.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class StaffSecurityConfig {
    @Bean public StaffAuthenticationFilter staffAuthenticationFilter(StaffJwtService jwt,StaffAccessService access,ObjectMapper json){return new StaffAuthenticationFilter(jwt,access,json);}
    @Bean public PasswordEncoder staffPasswordEncoder(){return new StaffPasswordEncoder();}
    @Bean public FilterRegistrationBean<StaffAuthenticationFilter> noStaffServletFilter(StaffAuthenticationFilter filter){
        var bean=new FilterRegistrationBean<>(filter);bean.setEnabled(false);return bean;
    }
    @Bean @Order(1)
    public SecurityFilterChain staffChain(HttpSecurity http,StaffAuthenticationFilter filter,RequestHeaderValidationFilter headers,ObjectMapper json)throws Exception {
        http.securityMatcher("/api/v1/admin/**").csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a->a.requestMatchers("/api/v1/admin/auth/login","/api/v1/admin/auth/refresh","/api/v1/admin/auth/logout").permitAll().anyRequest().hasAuthority("STAFF"))
            .exceptionHandling(e->e.authenticationEntryPoint((r,s,x)->{s.setStatus(401);s.setContentType("application/json;charset=UTF-8");s.getWriter().write(json.writeValueAsString(ApiResponse.error("UNAUTHORIZED","账号或凭据无效")));})
                .accessDeniedHandler((r,s,x)->{s.setStatus(403);s.setContentType("application/json;charset=UTF-8");s.getWriter().write(json.writeValueAsString(ApiResponse.error("FORBIDDEN","无权访问该资源")));}))
            .addFilterBefore(headers,UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(filter,UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
