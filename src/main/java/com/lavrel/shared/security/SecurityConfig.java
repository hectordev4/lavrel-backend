package com.lavrel.shared.security;

import com.lavrel.user.application.port.in.AuthenticateUserUseCase;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   AuthenticateUserUseCase authenticateUser,
                                                   JsonAuthenticationEntryPoint entryPoint,
                                                   @Value("${app.admin.firebase-uids:}") List<String> adminUids)
            throws Exception {
        Set<String> admins = adminUids.stream()
                .map(String::trim)
                .filter(uid -> !uid.isEmpty())
                .collect(Collectors.toSet());

        http
                // Stateless token API: no cookies, no sessions, so CSRF protection does not apply.
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/error").permitAll()
                        // Steam sends the browser here without a token; the signed "state" identifies the user.
                        .requestMatchers("/api/steam/callback").permitAll()
                        // Only the Firebase UIDs listed in app.admin.firebase-uids (none by default).
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e.authenticationEntryPoint(entryPoint))
                .addFilterBefore(new FirebaseAuthenticationFilter(authenticateUser, entryPoint, admins),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** Allowed browser origins for the frontend; override with app.cors.allowed-origins. */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
            List<String> allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * There are no username/password users. Declaring this bean stops Spring Boot from creating its
     * default user and logging a generated password at startup.
     */
    @Bean
    public UserDetailsService noPasswordUsers() {
        return username -> {
            throw new UsernameNotFoundException("Password login is not supported");
        };
    }
}
