package com.fixmate.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixmate.common.api.ApiResponse;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class StaffAuthenticationFilter extends OncePerRequestFilter {
    private final StaffJwtService jwt;private final StaffAccessService access;private final ObjectMapper json;
    public StaffAuthenticationFilter(StaffJwtService jwt,StaffAccessService access,ObjectMapper json){this.jwt=jwt;this.access=access;this.json=json;}
    @Override protected boolean shouldNotFilter(HttpServletRequest r){return !r.getRequestURI().startsWith("/api/v1/admin/");}
    @Override protected void doFilterInternal(HttpServletRequest r,HttpServletResponse s,FilterChain chain)throws ServletException,IOException {
        String header=r.getHeader("Authorization");
        if(header!=null&&header.startsWith("Bearer ")) {
            try {
                var p=jwt.parse(header.substring(7).trim(),"ACCESS");var user=access.active(p);
                String path=r.getRequestURI();
                if(Boolean.TRUE.equals(user.getPasswordChangeRequired())&&!path.equals("/api/v1/admin/auth/password/change")
                    &&!path.equals("/api/v1/admin/auth/logout")&&!path.equals("/api/v1/admin/auth/refresh")) {
                    s.setStatus(403);s.setContentType("application/json;charset=UTF-8");
                    s.getWriter().write(json.writeValueAsString(ApiResponse.error("FORBIDDEN","请先修改初始密码")));return;
                }
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(p,null,
                    List.of(new SimpleGrantedAuthority("STAFF"),new SimpleGrantedAuthority("ROLE_"+user.getRole()))));
            } catch(io.jsonwebtoken.JwtException|com.fixmate.exception.BusinessException ex) {SecurityContextHolder.clearContext();}
        }
        chain.doFilter(r,s);
    }
}
