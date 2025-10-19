package com.tictac.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT auth filter:
 * - If no Authorization: Bearer header -> continue anonymously (public endpoints work).
 * - If token present -> try to parse; on any failure -> continue anonymously.
 * - If parsed -> set an Authentication with username as principal.
 *
 * IMPORTANT: JwtUtil must expose:
 *   io.jsonwebtoken.Claims parse(String token)
 * and SecurityConfig must register this filter before UsernamePasswordAuthenticationFilter.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwt;

    public JwtAuthFilter(JwtUtil jwt) {
        this.jwt = jwt;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        // 1) No token? Continue anonymously (so /api/auth/** can work without JWT)
        final String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        // 2) Try to parse token; on any error, continue anonymously
        final String token = header.substring(7);
        try {
            var claims = jwt.parse(token);           // <-- JwtUtil.parse(String) must return io.jsonwebtoken.Claims
            String username = claims.getSubject();

            var auth = new UsernamePasswordAuthenticationToken(
                    // minimal principal; add authorities if you have roles
                    new org.springframework.security.core.userdetails.User(username, "", List.of()),
                    null,
                    List.of()
            );
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);

        } catch (Exception ignored) {
            // invalid/expired token -> leave unauthenticated and continue
        }

        // 3) Always continue the filter chain
        chain.doFilter(request, response);
    }
}
