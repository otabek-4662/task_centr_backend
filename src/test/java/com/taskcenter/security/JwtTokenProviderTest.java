package com.taskcenter.security;

import com.taskcenter.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private static final String SECRET =
            "test-secret-key-that-is-at-least-256-bits-long-for-hs256-testing-only";

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "jwtSecret", SECRET);
        ReflectionTestUtils.setField(provider, "jwtExpirationMs", 604800000);
    }

    private Authentication authentication(String name) {
        User user = User.builder()
                .id("abc123abc123abc123abc123")
                .name(name)
                .password("encoded")
                .role(User.Role.USER)
                .build();
        return new UsernamePasswordAuthenticationToken(
                user, null, List.of(() -> "ROLE_USER"));
    }

    @Test
    void generateToken_returnsValidTokenWithSubject() {
        String token = provider.generateToken(authentication("elshod"));

        assertThat(token).isNotBlank();
        assertThat(provider.validateToken(token)).isTrue();
        assertThat(provider.getUserNameFromJWT(token)).isEqualTo("elshod");
    }

    @Test
    void validateToken_rejectsExpiredToken() {
        ReflectionTestUtils.setField(provider, "jwtExpirationMs", -1000);

        String token = provider.generateToken(authentication("elshod"));

        assertThat(provider.validateToken(token)).isFalse();
    }

    @Test
    void validateToken_rejectsTamperedSignature() {
        String token = provider.generateToken(authentication("elshod"));
        // Tamper a middle char: the last base64 char carries only 2 significant
        // bits, so flipping it can decode to identical bytes and stay valid.
        int idx = token.length() / 2;
        char original = token.charAt(idx);
        char replacement = original == 'a' ? 'Z' : 'a';
        String tampered = token.substring(0, idx) + replacement + token.substring(idx + 1);

        assertThat(provider.validateToken(tampered)).isFalse();
    }

    @Test
    void validateToken_rejectsTokenSignedWithDifferentSecret() {
        JwtTokenProvider other = new JwtTokenProvider();
        ReflectionTestUtils.setField(other, "jwtSecret",
                "completely-different-secret-that-is-also-256-bits-long-xyz");
        ReflectionTestUtils.setField(other, "jwtExpirationMs", 604800000);

        String foreign = other.generateToken(authentication("elshod"));

        assertThat(provider.validateToken(foreign)).isFalse();
    }

    @Test
    void validateToken_rejectsGarbage() {
        assertThat(provider.validateToken("not.a.jwt")).isFalse();
        assertThat(provider.validateToken("")).isFalse();
        assertThat(provider.validateToken(null)).isFalse();
    }
}
