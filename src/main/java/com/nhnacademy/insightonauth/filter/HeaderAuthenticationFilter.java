package com.nhnacademy.insightonauth.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HeaderAuthenticationFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String userIdHeader = request.getHeader("X-USER-ID");
        String rolesHeader = request.getHeader("X-User-Roles");

        if (userIdHeader != null) {
            Long userId = Long.valueOf(userIdHeader);

            List<GrantedAuthority> authorities = new ArrayList<>();
            if (rolesHeader != null && !rolesHeader.isBlank()) {
                for (String role : rolesHeader.split(",")) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role.trim()));
                }
            }

            Authentication auth = new UsernamePasswordAuthenticationToken(
                    userId, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
}
