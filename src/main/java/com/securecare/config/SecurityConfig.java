package com.securecare.config;

import com.securecare.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

@Configuration
public class SecurityConfig {
    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter) throws Exception {
        return http.csrf(csrf -> csrf.disable()).cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a.requestMatchers(HttpMethod.OPTIONS,"/**").permitAll()
                        .requestMatchers("/", "/index.html", "/app.js", "/styles.css", "/favicon.ico").permitAll()
                        .requestMatchers("/api/health", "/api/auth/login", "/api/auth/register",
                            "/api/auth/forgot-password", "/api/auth/reset-password").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request,response,error) -> {
                            response.setStatus(401); response.setContentType("application/json");
                            String path=request.getRequestURI().replace("\\","\\\\").replace("\"","\\\"");
                            response.getWriter().write("{\"code\":\"UNAUTHENTICATED\",\"message\":\"A valid Bearer token is required\",\"timestamp\":\""+java.time.Instant.now()+"\",\"path\":\""+path+"\"}");
                        })
                        .accessDeniedHandler((request,response,error) -> {
                            response.setStatus(403); response.setContentType("application/json");
                            String path=request.getRequestURI().replace("\\","\\\\").replace("\"","\\\"");
                            response.getWriter().write("{\"code\":\"FORBIDDEN\",\"message\":\"You do not have permission to perform this operation\",\"timestamp\":\""+java.time.Instant.now()+"\",\"path\":\""+path+"\"}");
                        }))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .httpBasic(b -> b.disable()).formLogin(f -> f.disable()).build();
    }
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }
    @Bean CorsConfigurationSource corsConfigurationSource(org.springframework.core.env.Environment env) {
        CorsConfiguration c=new CorsConfiguration(); c.setAllowedOrigins(List.of(env.getProperty("app.cors.allowed-origin","http://localhost:5500")));
        c.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        c.setAllowedHeaders(List.of("Authorization","Content-Type")); c.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource s=new UrlBasedCorsConfigurationSource(); s.registerCorsConfiguration("/**",c); return s;
    }
}
