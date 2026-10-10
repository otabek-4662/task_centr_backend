package com.taskcenter.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.*;

class CorsConfigurationTest {

    @Test
    @DisplayName("Default holda (env bo'sh, dev emas): Hech qanday origin ruxsat etilmaydi, shu jumladan render va vercel")
    void default_nonDev_noEnv_allowsNoOrigins() {
        MockEnvironment env = new MockEnvironment();
        SecurityConfig config = new SecurityConfig(null, null, null, null, env, "");
        CorsConfigurationSource source = config.corsConfigurationSource();

        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/workspaces");
        request.addHeader("Origin", "http://localhost:3000");

        CorsConfiguration corsConfig = source.getCorsConfiguration(request);
        assertNotNull(corsConfig);
        assertNull(corsConfig.checkOrigin("http://localhost:3000"));
        assertNull(corsConfig.checkOrigin("https://task-center.onrender.com"));
        assertNull(corsConfig.checkOrigin("https://task-center.vercel.app"));
    }

    @Test
    @DisplayName("Dev profilida: localhost va 127.0.0.1 ruxsat etiladi")
    void devProfile_allowsLocalhost() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("dev");
        SecurityConfig config = new SecurityConfig(null, null, null, null, env, "");
        CorsConfigurationSource source = config.corsConfigurationSource();

        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/workspaces");
        request.addHeader("Origin", "http://localhost:3000");

        CorsConfiguration corsConfig = source.getCorsConfiguration(request);
        assertNotNull(corsConfig);
        assertEquals("http://localhost:3000", corsConfig.checkOrigin("http://localhost:3000"));
        assertEquals("http://127.0.0.1:5173", corsConfig.checkOrigin("http://127.0.0.1:5173"));
        assertNull(corsConfig.checkOrigin("https://evil.com"));
    }

    @Test
    @DisplayName("CORS_ALLOWED_ORIGINS belgilanganda: Faqat ko'rsatilgan domenlar ruxsat etiladi")
    void customOrigins_configuredViaEnv_onlyAllowsSpecifiedDomains() {
        MockEnvironment env = new MockEnvironment();
        String customOrigins = "https://taskcenter.app, https://frontend.company.com";
        SecurityConfig config = new SecurityConfig(null, null, null, null, env, customOrigins);
        CorsConfigurationSource source = config.corsConfigurationSource();

        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/workspaces");
        CorsConfiguration corsConfig = source.getCorsConfiguration(request);
        assertNotNull(corsConfig);

        assertEquals("https://taskcenter.app", corsConfig.checkOrigin("https://taskcenter.app"));
        assertEquals("https://frontend.company.com", corsConfig.checkOrigin("https://frontend.company.com"));
        assertNull(corsConfig.checkOrigin("http://localhost:3000"));
        assertNull(corsConfig.checkOrigin("https://other-render.onrender.com"));
    }
}
