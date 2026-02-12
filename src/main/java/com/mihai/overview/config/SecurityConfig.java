package com.mihai.overview.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mihai.overview.exception.ExceptionResponses;
import com.mihai.overview.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@EnableMethodSecurity
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserRepository userRepository;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // Use ONE mapper instance for consistent JSON output in these handlers
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SecurityConfig(UserRepository userRepository, JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.userRepository = userRepository;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    UserDetailsService userDetailsService() {
        return username -> userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, ex) -> {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("WWW-Authenticate", "");

            ExceptionResponses body = new ExceptionResponses(
                    HttpStatus.UNAUTHORIZED.value(),
                    "Unauthorized access",
                    System.currentTimeMillis()
            );

            response.getWriter().write(objectMapper.writeValueAsString(body));
        };
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, ex) -> {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            String message;
            if ("/api/auth/register".equals(request.getRequestURI())) {
                message = "You cannot register a new user while you are logged in. Log out and register your own account.";
            } else {
                message = "Access denied.";
            }

            ExceptionResponses body = new ExceptionResponses(
                    HttpStatus.FORBIDDEN.value(),
                    message,
                    System.currentTimeMillis()
            );

            response.getWriter().write(objectMapper.writeValueAsString(body));
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http.csrf(csrf -> csrf.disable());

        http.sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.exceptionHandling(exceptionHandling ->
                exceptionHandling
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler())
        );

        http.authorizeHttpRequests(auth -> auth
                // REGISTER — only when NOT authenticated (anonymous-only)
                .requestMatchers("/api/auth/register")
                .access(new WebExpressionAuthorizationManager("isAnonymous()"))

                // LOGIN — open
                .requestMatchers("/api/auth/login").permitAll()

                // DOCS / SWAGGER — open
                .requestMatchers(
                        "/docs", "/docs/**",
                        "/swagger-ui/**", "/v3/api-docs/**",
                        "/swagger-resources/**", "/webjars/**"
                ).permitAll()

                        // ✅ allow QA/TM/Admin for review config under /api/admin/review-config/**
                        .requestMatchers("/api/admin/review-config/**")
                        .hasAnyRole("ADMIN", "QUALITY_ANALYST", "TEAM_MANAGER")

                        // ✅ all other admin endpoints remain ADMIN-only
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // Hard block: nobody can delete themselves
                        .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/users").denyAll()

                        .anyRequest().authenticated()

        );

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
