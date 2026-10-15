package com.taskcenter.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "prod"})
class SwaggerSecurityProfileTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Prod profilida swagger-ui va v3/api-docs anonim foydalanuvchilar uchun yopiq (401 Unauthorized yoki 404 Not Found)")
    void testSwaggerDisabledInProdProfile() throws Exception {
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
