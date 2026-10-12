package com.taskcenter.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CorsConfigurationTest {

    @Test
    @DisplayName("Default holda (env bo'sh, dev emas): Hech qanday origin ruxsat etilmaydi")
    void default_nonDev_noEnv_allowsNoOrigins() {
        MockEnvironment env = new MockEnvironment();
        SecurityConfig config = new SecurityConfig(null, null, null, null, null, env, "");
        CorsConfigurationSource source = config.corsConfigurationSource();

        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/workspaces");
        request.addHeader("Origin", "http://localhost:5173");

        CorsConfiguration corsConfig = source.getCorsConfiguration(request);
        assertNotNull(corsConfig);
        assertNull(corsConfig.checkOrigin("http://localhost:5173"));
        assertNull(corsConfig.checkOrigin("https://task-center.onrender.com"));
        assertNull(corsConfig.checkOrigin("https://task-center.vercel.app"));
        assertFalse(corsConfig.getAllowCredentials());
    }

    @Test
    @DisplayName("Dev profilida origin ko'rsatilmaganda: http://localhost:5173 default qilib olinadi")
    void devProfile_allowsLocalhost5173ByDefault() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("dev");
        SecurityConfig config = new SecurityConfig(null, null, null, null, null, env, "");
        CorsConfigurationSource source = config.corsConfigurationSource();

        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/workspaces");
        request.addHeader("Origin", "http://localhost:5173");

        CorsConfiguration corsConfig = source.getCorsConfiguration(request);
        assertNotNull(corsConfig);
        assertEquals("http://localhost:5173", corsConfig.checkOrigin("http://localhost:5173"));
        assertNull(corsConfig.checkOrigin("http://localhost:3000"));
        assertNull(corsConfig.checkOrigin("https://evil.com"));
        assertTrue(corsConfig.getAllowCredentials());
    }

    @Test
    @DisplayName("CORS_ALLOWED_ORIGINS / app.cors.allowed-origins belgilanganda: Faqat ko'rsatilgan domenlar ruxsat etiladi")
    void customOrigins_configuredViaEnv_onlyAllowsSpecifiedDomains() {
        MockEnvironment env = new MockEnvironment();
        String customOrigins = "https://taskcenter.app, https://frontend.company.com";
        SecurityConfig config = new SecurityConfig(null, null, null, null, null, env, customOrigins);
        CorsConfigurationSource source = config.corsConfigurationSource();

        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/workspaces");
        CorsConfiguration corsConfig = source.getCorsConfiguration(request);
        assertNotNull(corsConfig);

        assertEquals("https://taskcenter.app", corsConfig.checkOrigin("https://taskcenter.app"));
        assertEquals("https://frontend.company.com", corsConfig.checkOrigin("https://frontend.company.com"));
        assertNull(corsConfig.checkOrigin("http://localhost:5173"));
        assertNull(corsConfig.checkOrigin("https://other-render.onrender.com"));
        assertTrue(corsConfig.getAllowCredentials());
    }

    @Test
    @DisplayName("CORS metodlari, headerlari va allowCredentials spetsifikatsiyaga to'liq mos")
    void corsConfiguration_methods_headers_credentials() {
        MockEnvironment env = new MockEnvironment();
        SecurityConfig config = new SecurityConfig(null, null, null, null, null, env, "http://localhost:5173");
        CorsConfigurationSource source = config.corsConfigurationSource();

        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/workspaces");
        CorsConfiguration corsConfig = source.getCorsConfiguration(request);
        assertNotNull(corsConfig);

        // Metodlar: GET, POST, PUT, PATCH, DELETE, OPTIONS
        assertEquals(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"), corsConfig.getAllowedMethods());

        // Headerlar: Authorization, Content-Type, X-Telegram-Init-Data
        assertEquals(List.of("Authorization", "Content-Type", "X-Telegram-Init-Data"), corsConfig.getAllowedHeaders());

        // allowCredentials: true (aniq origin bo'lgani uchun)
        assertTrue(corsConfig.getAllowCredentials());
    }

    @Test
    @DisplayName("Wildcard '*' bo'lsa, allowCredentials false bo'lishi shart (* va credentials birga ishlatilmasin)")
    void wildcard_disablesCredentials() {
        MockEnvironment env = new MockEnvironment();
        SecurityConfig config = new SecurityConfig(null, null, null, null, null, env, "*");
        CorsConfigurationSource source = config.corsConfigurationSource();

        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/workspaces");
        CorsConfiguration corsConfig = source.getCorsConfiguration(request);
        assertNotNull(corsConfig);

        assertFalse(corsConfig.getAllowCredentials());
    }

    @Test
    @DisplayName("Render frontend domeni (https://task-center-frontend.onrender.com) CORS da ruxsat etiladi")
    void renderFrontendDomain_isAllowed() {
        MockEnvironment env = new MockEnvironment();
        SecurityConfig config = new SecurityConfig(null, null, null, null, null, env, "https://task-center-frontend.onrender.com/");
        CorsConfigurationSource source = config.corsConfigurationSource();

        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/workspaces");
        CorsConfiguration corsConfig = source.getCorsConfiguration(request);
        assertNotNull(corsConfig);
        assertEquals("https://task-center-frontend.onrender.com", corsConfig.checkOrigin("https://task-center-frontend.onrender.com"));
        assertTrue(corsConfig.getAllowCredentials());
    }
}
