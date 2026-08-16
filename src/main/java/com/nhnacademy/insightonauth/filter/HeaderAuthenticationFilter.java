package com.nhnacademy.insightonauth.filter;

import com.nhnacademy.insightonauth.service.UserRoleService;
import com.nhnacademy.insightonauth.service.UserAuthenticationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    private final UserAuthenticationService userAuthenticationService;
    private final UserRoleService userRoleService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String userIdHeader = request.getHeader("X-User-Id");
        // admin만 role 줄거임 그외는 다 null로 들어올거라 null체크
        String rolesHeader = request.getHeader("X-User-Role");

        if (userIdHeader != null) {
            try {
                Long userId = Long.valueOf(userIdHeader);

                List<GrantedAuthority> authorities = new ArrayList<>();
                if (rolesHeader != null && !rolesHeader.isBlank()) {
                    for (String role : rolesHeader.split(",")) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.trim()));
                    }
                }

                Authentication auth = new UsernamePasswordAuthenticationToken(userId, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception e) {
                log.debug("유효하지 않은 인증 헤더 - X-User-Id: {}, X-User-Role: {}", userIdHeader, rolesHeader);
            }
        }

        filterChain.doFilter(request, response);
    }
}
