package com.yubiniperez.biblioteca.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.time.LocalDateTime;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtService jwtService;

    public SecurityConfig(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Públicos
                        .requestMatchers("/api/v1/auth/**", "/error").permitAll()

                        // Libros: lectura para cualquier usuario autenticado, escritura solo ADMIN
                        .requestMatchers(HttpMethod.GET, "/api/v1/libros", "/api/v1/libros/*").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/libros").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/libros/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/libros/*").hasRole("ADMIN")

                        // Préstamos
                        .requestMatchers(HttpMethod.POST, "/api/v1/prestamos").hasAnyRole("BIBLIOTECARIO", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/prestamos/*/devolucion").hasAnyRole("BIBLIOTECARIO", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/prestamos/mis-prestamos").hasRole("LECTOR")
                        .requestMatchers(HttpMethod.GET, "/api/v1/prestamos/atrasados").hasAnyRole("BIBLIOTECARIO", "ADMIN")

                        // Todo lo demás requiere token
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) ->
                                escribirError(res, 401, "Unauthorized", "Token ausente, inválido o expirado"))
                        .accessDeniedHandler((req, res, e) ->
                                escribirError(res, 403, "Forbidden", "No tienes permisos para realizar esta acción")))
                .addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // Respuesta de error con el mismo formato JSON que el GlobalExceptionHandler
    private void escribirError(HttpServletResponse response, int status, String error, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(String.format(
                "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"message\":\"%s\"}",
                LocalDateTime.now(), status, error, message));
    }
}