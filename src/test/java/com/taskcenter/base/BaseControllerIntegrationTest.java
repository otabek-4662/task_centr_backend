package com.taskcenter.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.taskcenter.model.User;
import com.taskcenter.repository.UserRepository;
import com.taskcenter.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * RealWorld (Conduit) uslubidagi markaziy bazaviy integratsion test klassi.
 *
 * Afzalliklari:
 * 1. Barcha controller testlarida takrorlanuvchi MockMvc, ObjectMapper, TokenProvider konfiguratsiyalarini DRY qiladi.
 * 2. Har bir testda /api/auth/login ga HTTP so'rov qilish o'rniga JwtTokenProvider orqali bevosita token yasaydi (50x tezroq).
 * 3. JSON payloadlarni qo'lda string formatida birlashtirish o'rniga ObjectMapper bilan fluent ishlaydi.
 * 4. GET, POST, PUT, DELETE so'rovlari va Response parsing uchun qulay helper metodlarni taqdim etadi.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class BaseControllerIntegrationTest {

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JwtTokenProvider jwtTokenProvider;

    @Autowired
    protected UserRepository userRepository;

    @BeforeEach
    void setUpBase() {
        // Test boshlanishidan oldin umumiy sozlashlar (zarur bo'lsa)
    }

    /**
     * Berilgan foydalanuvchi nomi uchun to'g'ridan-to'g'ri JWT token qaytaradi.
     * Agar foydalanuvchi bazada bo'lmasa, test uchun avtomatik yaratiladi.
     */
    protected String tokenFor(String username) {
        User user = userRepository.findByName(username).orElseGet(() -> {
            User newUser = User.builder()
                    .name(username)
                    .fullName(username + " Test")
                    .email(username.toLowerCase() + "@taskcenter.test")
                    .password("$2a$10$dummyHashedPasswordForTestEnvironmentOnly123456")
                    .role(User.Role.USER)
                    .build();
            return userRepository.save(newUser);
        });
        return jwtTokenProvider.generateTokenFromUser(user);
    }

    /**
     * "Bearer <token>" sarlavhasini qaytaradi.
     */
    protected String bearer(String username) {
        return "Bearer " + tokenFor(username);
    }

    /**
     * Obyektni JSON string ga aylantiradi.
     */
    protected String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException("Obyektni JSON ga aylantirishda xatolik", e);
        }
    }

    /**
     * GET so'rovini avtorizatsiya va JSON header lari bilan yuborish.
     */
    protected ResultActions performGet(String url, String bearerToken) throws Exception {
        var req = get(url).contentType(MediaType.APPLICATION_JSON);
        if (bearerToken != null && !bearerToken.isBlank()) {
            req.header("Authorization", bearerToken);
        }
        return mvc.perform(req);
    }

    /**
     * Ochiq (unauthenticated) GET so'rovi.
     */
    protected ResultActions performGet(String url) throws Exception {
        return performGet(url, null);
    }

    /**
     * POST so'rovini payload va token bilan yuborish.
     */
    protected ResultActions performPost(String url, Object body, String bearerToken) throws Exception {
        var req = post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body instanceof String ? (String) body : toJson(body));
        if (bearerToken != null && !bearerToken.isBlank()) {
            req.header("Authorization", bearerToken);
        }
        return mvc.perform(req);
    }

    /**
     * PUT so'rovini payload va token bilan yuborish.
     */
    protected ResultActions performPut(String url, Object body, String bearerToken) throws Exception {
        var req = put(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body instanceof String ? (String) body : toJson(body));
        if (bearerToken != null && !bearerToken.isBlank()) {
            req.header("Authorization", bearerToken);
        }
        return mvc.perform(req);
    }

    /**
     * DELETE so'rovini yuborish.
     */
    protected ResultActions performDelete(String url, String bearerToken) throws Exception {
        var req = delete(url);
        if (bearerToken != null && !bearerToken.isBlank()) {
            req.header("Authorization", bearerToken);
        }
        return mvc.perform(req);
    }

    /**
     * MvcResult javobidan $.data.id qiymatini ajratib oladi.
     */
    protected String extractDataId(MvcResult result) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.id");
    }

    /**
     * Unikal test nomini generatsiya qiladi.
     */
    protected String uniqueString(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
