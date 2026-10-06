package com.taskcenter.robustness;

import com.jayway.jsonpath.JsonPath;
import com.taskcenter.model.User;
import com.taskcenter.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class JwtRobustnessTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @Value("${jwt.secret}")
    private String secret;

    private String signedWith(String key, Date expiry, String subject) {
        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis() - 10_000))
                .setExpiration(expiry)
                .signWith(Keys.hmacShaKeyFor(key.getBytes(StandardCharsets.UTF_8)),
                        SignatureAlgorithm.HS256)
                .compact();
    }

    private String createTestUser(String username) throws Exception {
        String body = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + username + "\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.data.token");
    }

    @Test
    void expiredToken_isRejected() throws Exception {
        String expired = signedWith(secret, new Date(System.currentTimeMillis() - 5_000), "elshod");
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + expired))
                .andExpect(status().isForbidden());
    }

    @Test
    void tokenWithWrongKey_isRejected() throws Exception {
        String foreign = signedWith("completely-different-secret-that-is-also-256-bits-long-xyz",
                new Date(System.currentTimeMillis() + 600_000), "elshod");
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + foreign))
                .andExpect(status().isForbidden());
    }

    @Test
    void tokenForDeletedUser_isRejected() throws Exception {
        String jwt = createTestUser("tobe_deleted");
        
        // Delete the user from DB directly
        User user = userRepository.findByName("tobe_deleted").get();
        userRepository.delete(user);

        // Try to access a protected endpoint
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isForbidden());
    }
}
