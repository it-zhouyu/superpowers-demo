package com.example.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        String testSecret = "dGVzdC1zZWNyZXQta2V5LWZvci1qdW5pdC10ZXN0aW5nLW9ubHk=";
        long expiration = 3600000;
        jwtService = new JwtService(testSecret, expiration);
    }

    @Test
    void generateToken_shouldReturnValidToken() {
        String token = jwtService.generateToken(1L, "13800138000");
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void parseToken_shouldReturnCorrectClaims() {
        String token = jwtService.generateToken(1L, "13800138000");
        Claims claims = jwtService.parseToken(token);

        assertEquals("1", claims.getSubject());
        assertEquals("13800138000", claims.get("phone", String.class));
    }

    @Test
    void getUserIdFromToken_shouldReturnCorrectUserId() {
        String token = jwtService.generateToken(1L, "13800138000");
        Long userId = jwtService.getUserIdFromToken(token);
        assertEquals(1L, userId);
    }

    @Test
    void getPhoneFromToken_shouldReturnCorrectPhone() {
        String token = jwtService.generateToken(1L, "13800138000");
        String phone = jwtService.getPhoneFromToken(token);
        assertEquals("13800138000", phone);
    }

    @Test
    void validateToken_shouldReturnTrue_whenValid() {
        String token = jwtService.generateToken(1L, "13800138000");
        assertTrue(jwtService.validateToken(token));
    }

    @Test
    void validateToken_shouldReturnFalse_whenInvalid() {
        assertFalse(jwtService.validateToken("invalid.token.string"));
    }

    @Test
    void validateToken_shouldReturnFalse_whenEmpty() {
        assertFalse(jwtService.validateToken(""));
    }

    @Test
    void validateToken_shouldReturnFalse_whenNull() {
        assertFalse(jwtService.validateToken(null));
    }

    @Test
    void validateToken_shouldReturnFalse_whenExpired() {
        String testSecret = "dGVzdC1zZWNyZXQta2V5LWZvci1qdW5pdC10ZXN0aW5nLW9ubHk=";
        JwtService expiredService = new JwtService(testSecret, 0);
        String token = expiredService.generateToken(1L, "13800138000");
        assertFalse(expiredService.validateToken(token));
    }

    @Test
    void parseToken_shouldThrow_whenInvalid() {
        assertThrows(JwtException.class, () -> jwtService.parseToken("invalid.token.string"));
    }
}
