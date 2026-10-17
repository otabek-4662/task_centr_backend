package com.taskcenter.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SwaggerSecurityProfileTest {

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @ActiveProfiles({"test", "prod"})
    class WhenSwaggerEnabledByDefault {

        @Autowired
        private MockMvc mockMvc;

        @Test
        @DisplayName("Prod profilida default holatda swagger-ui va v3/api-docs ochiq bo'ladi")
        void testSwaggerEnabledInProdProfileByDefault() throws Exception {
            mockMvc.perform(get("/swagger-ui.html"))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        org.junit.jupiter.api.Assertions.assertTrue(status == 200 || status == 302,
                                "Kutilgan status 200 yoki 302, lekin " + status + " qaytdi");
                    });

            mockMvc.perform(get("/swagger-ui/index.html"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    @ActiveProfiles({"test", "prod"})
    @TestPropertySource(properties = {
            "springdoc.swagger-ui.enabled=false",
            "springdoc.api-docs.enabled=false"
    })
    class WhenSwaggerExplicitlyDisabled {

        @Autowired
        private MockMvc mockMvc;

        @Test
        @DisplayName("SWAGGER_ENABLED=false bo'lganda swagger-ui va v3/api-docs yopiq bo'ladi (401 yoki 404)")
        void testSwaggerDisabledWhenConfigured() throws Exception {
            mockMvc.perform(get("/v3/api-docs"))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        org.junit.jupiter.api.Assertions.assertTrue(status == 401 || status == 404,
                                "Kutilgan status 401 yoki 404, lekin " + status + " qaytdi");
                    });

            mockMvc.perform(get("/swagger-ui/index.html"))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        org.junit.jupiter.api.Assertions.assertTrue(status == 401 || status == 404,
                                "Kutilgan status 401 yoki 404, lekin " + status + " qaytdi");
                    });
        }
    }
}
