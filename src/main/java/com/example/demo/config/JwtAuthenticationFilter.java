package com.example.demo.config;

import com.example.demo.respository.UserRepository;
import com.example.demo.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
@Component
public class JwtAuthenticationFilter
        extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserDetailsService userDetailsService,
            UserRepository userRepository
    ) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.userRepository = userRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return "/loginWithJwt".equals(path)
                || "/refreshToken".equals(path)
                || ("POST".equals(request.getMethod()) && "/logout".equals(path))
                || path.startsWith("/api/v1/auth/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader =
                request.getHeader("Authorization");

        if (authHeader == null
                || !authHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7).trim();

        try {
            if (SecurityContextHolder.getContext()
                    .getAuthentication() == null) {
                Claims claims = jwtService.extractAllClaims(token);
                if (!"access".equals(claims.get("type", String.class))) {
                    reject(response, "Access token required");
                    return;
                }
                String userName = claims.getSubject();
                if (userName == null || userName.isBlank() || claims.getExpiration() == null) {
                    reject(response, "Invalid access token");
                    return;
                }
                UserDetails userDetails = userDetailsService.loadUserByUsername(userName);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities()
                        );
                authentication.setDetails(
                        new WebAuthenticationDetailsSource()
                                .buildDetails(request)
                );

                SecurityContextHolder.getContext()
                        .setAuthentication(authentication);
            }
        } catch (ExpiredJwtException e) {
            reject(response, "Access token expired; log in again or refresh your token");
            return;
        } catch (JwtException | IllegalArgumentException e) {
            reject(response, "Invalid access token");
            return;
        } catch (UsernameNotFoundException e) {
            reject(response, "Token user not found; log in again");
            return;
        }



        filterChain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response, String message) throws IOException {
        SecurityContextHolder.clearContext();
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setHeader("WWW-Authenticate", "Bearer");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}
