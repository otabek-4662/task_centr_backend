package com.taskcenter.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AppWebAccessTest {

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("GET /: brauzerdan kirganda /app/ ga muvaffaqiyatli redirect qiladi (302)")
    void rootRedirectsToApp() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/app/"));
    }

    @Test
    @DisplayName("GET /app va /app/: Mini App veb sahifasini ochadi (200 OK)")
    void appEndpointAccessibleWithoutAuth() throws Exception {
        mvc.perform(get("/app/index.html"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Security-Policy", org.hamcrest.Matchers.containsString("frame-ancestors 'self'")));
    }
}
