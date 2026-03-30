package com.mishchuk.onlineschool.security;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

@TestConfiguration
@EnableMethodSecurity
public class TestSecurityConfig {

    @Bean
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // --- /auth ---
                        .requestMatchers("/auth/change-password").authenticated()
                        .requestMatchers("/auth/**").permitAll()
                        // --- /appeals ---
                        .requestMatchers(HttpMethod.POST, "/appeals/public").permitAll()
                        // --- /lessons ---
                        .requestMatchers(HttpMethod.GET, "/lessons", "/lessons/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/lessons").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/lessons/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/lessons/**").authenticated()
                        // --- /courses ---
                        .requestMatchers(HttpMethod.GET, "/courses", "/courses/**").permitAll()
                        // --- /modules ---
                        .requestMatchers(HttpMethod.GET, "/modules", "/modules/**").permitAll()
                        // --- /files ---
                        .requestMatchers("/files/my-files").authenticated()
                        .requestMatchers("/uploads/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/files/**").authenticated()
                        .requestMatchers("/files/**").authenticated()
                        // --- решта ---
                        .anyRequest().authenticated()
                )
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )
                .sessionManagement(sess -> sess
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        return http.build();
    }
}
