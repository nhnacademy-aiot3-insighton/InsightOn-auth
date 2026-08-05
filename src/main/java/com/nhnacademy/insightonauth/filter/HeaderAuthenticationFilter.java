package com.nhnacademy.insightonauth.filter;

import com.nhnacademy.insightonauth.entity.User;
import com.nhnacademy.insightonauth.entity.UserRole;
import com.nhnacademy.insightonauth.service.UserRoleService;
import com.nhnacademy.insightonauth.service.UserService;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@Transactional
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    private final UserService userService;
    private final UserRoleService userRoleService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String userIdHeader = request.getHeader("X-User-Id");

        if (userIdHeader != null) {
            Long userId = Long.valueOf(userIdHeader);

            User user = userService.findById(userId);
            List<UserRole> userRoles = userRoleService.findByUser(user);
            List<GrantedAuthority> authorities = userRoles.stream()
                    .map(userRole -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + userRole.getRole().name()))
                    .toList();

            Authentication auth = new UsernamePasswordAuthenticationToken(userId, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
}
