package com.taskcenter.config;

import com.taskcenter.security.JwtAuthenticationFilter;
import com.taskcenter.security.RateLimitFilter;
import com.taskcenter.security.CustomAuthenticationEntryPoint;
import com.taskcenter.security.CustomAccessDeniedHandler;
import com.taskcenter.security.TelegramWebAppAuthFilter;
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
    private final TelegramWebAppAuthFilter telegramWebAppAuthFilter;
    private final RateLimitFilter rateLimitFilter;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final org.springframework.core.env.Environment environment;
    private final String customAllowedOrigins;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            TelegramWebAppAuthFilter telegramWebAppAuthFilter,
            RateLimitFilter rateLimitFilter,
            CustomAuthenticationEntryPoint customAuthenticationEntryPoint,
            CustomAccessDeniedHandler customAccessDeniedHandler,
            org.springframework.core.env.Environment environment,
            @Value("${app.cors.allowed-origins:${CORS_ALLOWED_ORIGINS:https://task-center-frontend.onrender.com}}") String customAllowedOrigins) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.telegramWebAppAuthFilter = telegramWebAppAuthFilter;
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

        boolean isDev = environment != null && Arrays.asList(environment.getActiveProfiles()).contains("dev");

        // 1. Env / property dagi ruxsat etilgan originlar (vergul bilan ajratilgan)
        if (customAllowedOrigins != null && !customAllowedOrigins.isBlank()) {
            for (String origin : customAllowedOrigins.split(",")) {
                String trimmed = origin.trim();
                if (!trimmed.isEmpty()) {
                    if (trimmed.endsWith("/")) {
                        trimmed = trimmed.substring(0, trimmed.length() - 1);
                    }
                    if (!origins.contains(trimmed)) {
                        origins.add(trimmed);
                    }
                }
            }
        }

        // Dev profilida agar origin ko'rsatilmagan bo'lsa, http://localhost:5173 default qilinadi
        if (origins.isEmpty() && isDev) {
            origins.add("http://localhost:5173");
        }

        if (!origins.isEmpty()) {
            config.setAllowedOriginPatterns(origins);
        } else {
            config.setAllowedOriginPatterns(Collections.emptyList());
        }

        // 2. Metodlar: GET, POST, PUT, PATCH, DELETE, OPTIONS
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // 2. Headerlar: Authorization, Content-Type, X-Telegram-Init-Data
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Telegram-Init-Data"));
        config.setExposedHeaders(List.of("Retry-After", "Authorization"));

        // 3. allowCredentials faqat kerak bo'lsa yoq; "*" va credentials birga ishlatilmasin
        boolean hasWildcard = origins.stream().anyMatch("*"::equals);
        if (!origins.isEmpty() && !hasWildcard) {
            config.setAllowCredentials(true);
        } else {
            config.setAllowCredentials(false);
        }

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
        List<String> activeProfiles = environment != null ? Arrays.asList(environment.getActiveProfiles()) : Collections.emptyList();
        boolean isProd = activeProfiles.contains("prod");

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
            .authorizeHttpRequests(authz -> {
                // Public endpoints
                authz
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/v1/auth/**", "/api/auth/**", "/api/webhooks/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/config/public", "/favicon.ico", "/api/invitations/*").permitAll()
                    .requestMatchers("/favicon.ico", "/ws/**").permitAll();

                // Swagger UI va OpenAPI hujjatlari faqat dev / non-prod profilida ochiq bo'ladi
                if (!isProd) {
                    authz.requestMatchers(
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/v3/api-docs",
                        "/swagger-resources/**",
                        "/webjars/**"
                    ).permitAll();
                }

                authz
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
                    .anyRequest().authenticated();
            });

        http.addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(telegramWebAppAuthFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
