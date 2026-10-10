package com.taskcenter.config;

import com.taskcenter.security.JwtAuthenticationFilter;
import com.taskcenter.security.RateLimitFilter;
import com.taskcenter.security.CustomAuthenticationEntryPoint;
import com.taskcenter.security.CustomAccessDeniedHandler;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.beans.factory.annotation.Value;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitFilter rateLimitFilter;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final org.springframework.core.env.Environment environment;
    private final String customAllowedOrigins;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            RateLimitFilter rateLimitFilter,
            CustomAuthenticationEntryPoint customAuthenticationEntryPoint,
            CustomAccessDeniedHandler customAccessDeniedHandler,
            org.springframework.core.env.Environment environment,
            @Value("${CORS_ALLOWED_ORIGINS:${app.cors.allowed-origins:}}") String customAllowedOrigins) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.rateLimitFilter = rateLimitFilter;
        this.customAuthenticationEntryPoint = customAuthenticationEntryPoint;
        this.customAccessDeniedHandler = customAccessDeniedHandler;
        this.environment = environment;
        this.customAllowedOrigins = customAllowedOrigins;
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
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = new ArrayList<>();

        // localhost faqat dev profilida
        if (environment != null && Arrays.asList(environment.getActiveProfiles()).contains("dev")) {
            origins.add("http://localhost:*");
            origins.add("http://127.0.0.1:*");
        }

        // faqat CORS_ALLOWED_ORIGINS env'dagi aniq originlar
        if (customAllowedOrigins != null && !customAllowedOrigins.isBlank()) {
            for (String origin : customAllowedOrigins.split(",")) {
                String trimmed = origin.trim();
                if (!trimmed.isEmpty() && !origins.contains(trimmed)) {
                    origins.add(trimmed);
                }
            }
        }

        if (!origins.isEmpty()) {
            config.setAllowedOriginPatterns(origins);
        } else {
            config.setAllowedOriginPatterns(Collections.emptyList());
        }

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Retry-After", "Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L); // Preflight keshini 1 soatga saqlash (OPTIONS so'rovlarini kamaytiradi)
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    @org.springframework.core.annotation.Order(1)
    public SecurityFilterChain appFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/app", "/app/**")
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                    "default-src 'self'; " +
                    "script-src 'self' https://telegram.org https://cdn.jsdelivr.net; " +
                    "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
                    "font-src 'self' https://fonts.gstatic.com data:; " +
                    "img-src 'self' data:; " +
                    "connect-src 'self'; " +
                    "frame-ancestors https://web.telegram.org https://*.telegram.org https://telegram.org"
                ))
                .referrerPolicy(referrer -> referrer.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                .frameOptions(frame -> frame.disable())
            )
            .authorizeHttpRequests(authz -> authz
                .anyRequest().permitAll()
            );
        return http.build();
    }

    @Bean
    @org.springframework.core.annotation.Order(2)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers
                .frameOptions(frame -> frame.deny())
                .referrerPolicy(referrer -> referrer.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(customAuthenticationEntryPoint)
                .accessDeniedHandler(customAccessDeniedHandler)
            )
            .authorizeHttpRequests(authz -> authz
                // Public endpoints
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/**", "/api/auth/**", "/api/webhooks/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/config/public", "/favicon.ico", "/api/invitations/*").permitAll()
                .requestMatchers(
                    "/favicon.ico",
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/v3/api-docs",
                    "/swagger-resources/**",
                    "/webjars/**",
                    "/ws/**"
                ).permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/actuator/metrics/**", "/actuator/prometheus").hasRole("ADMIN")
                // Admin only endpoints (workspaceId parametri berilmaganda faqat admin ko'ra oladi)
                .requestMatchers(request -> "GET".equalsIgnoreCase(request.getMethod())
                        && ("/api/users".equals(request.getRequestURI()) || "/api/users".equals(request.getServletPath()))
                        && (request.getParameter("workspaceId") == null || request.getParameter("workspaceId").isBlank())).hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/users").hasAnyRole("USER", "ADMIN")
                // Joriy foydalanuvchi o'z Telegram ulanishini uzishi mumkin (admin qoidasidan OLDIN turishi shart)
                .requestMatchers(HttpMethod.DELETE, "/api/users/me/telegram").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/users/me/telegram").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasRole("ADMIN")
                // Workspace owner can do anything on their workspaces
                .requestMatchers("/api/workspaces/**").hasAnyRole("USER", "ADMIN")
                // Board, tasks, labels - requires authentication
                .anyRequest().authenticated()
            );

        http.addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
