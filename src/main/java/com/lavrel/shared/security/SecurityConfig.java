package com.lavrel.shared.security;

import com.lavrel.user.application.port.in.AuthenticateUserUseCase;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
                                                   JsonAccessDeniedHandler accessDeniedHandler)
            throws Exception {

        http
                // Stateless token API: no cookies, no sessions, so CSRF protection does not apply.
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/error").permitAll()
                        // Platforms redirect the browser here without our token; the signed state identifies the user.
                        .requestMatchers("/api/platforms/*/callback", "/api/steam/callback").permitAll()
                        // Public catalog: search, filter options, game pages and achievement lists, no login needed.
                        .requestMatchers(HttpMethod.GET, "/api/games", "/api/games/*", "/api/games/*/achievements",
                                "/api/search", "/api/tags", "/api/creators").permitAll()
                        // Reachable before the e-mail is verified: the own profile (to show "check your inbox"),
                        // deleting the account (someone who signed up with another person's e-mail can undo it),
                        // and the dev-only verification shortcut (absent unless app.dev.verify-email-uids is set).
                        .requestMatchers(HttpMethod.GET, "/api/currentuser").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/currentuser").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/dev/verify-my-email").authenticated()
                        // Role from the users table; only granted together with a verified e-mail.
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().hasAuthority(FirebaseAuthenticationFilter.EMAIL_VERIFIED))
                .exceptionHandling(e -> e.authenticationEntryPoint(entryPoint).accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(new FirebaseAuthenticationFilter(authenticateUser, entryPoint),
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
     * Passwords never reach this backend: e-mail + password sign-in happens in Firebase, and we only see the
     * resulting token. Declaring this bean stops Spring Boot from creating its default user and logging a
     * generated password at startup.
     */
    @Bean
    public UserDetailsService noPasswordUsers() {
        return username -> {
            throw new UsernameNotFoundException("Password login is not supported");
        };
    }
}
