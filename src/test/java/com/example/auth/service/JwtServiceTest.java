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
        String token = jwtService.generateToken(1L, "test@example.com");
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void parseToken_shouldReturnCorrectClaims() {
        String token = jwtService.generateToken(1L, "test@example.com");
        Claims claims = jwtService.parseToken(token);

        assertEquals("1", claims.getSubject());
        assertEquals("test@example.com", claims.get("email", String.class));
    }

    @Test
    void getUserIdFromToken_shouldReturnCorrectUserId() {
        String token = jwtService.generateToken(1L, "test@example.com");
        Long userId = jwtService.getUserIdFromToken(token);
        assertEquals(1L, userId);
    }

    @Test
    void getEmailFromToken_shouldReturnCorrectEmail() {
        String token = jwtService.generateToken(1L, "test@example.com");
        String email = jwtService.getEmailFromToken(token);
        assertEquals("test@example.com", email);
    }

    @Test
    void validateToken_shouldReturnTrue_whenValid() {
        String token = jwtService.generateToken(1L, "test@example.com");
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
        String token = expiredService.generateToken(1L, "test@example.com");
        assertFalse(expiredService.validateToken(token));
    }

    @Test
    void parseToken_shouldThrow_whenInvalid() {
        assertThrows(JwtException.class, () -> jwtService.parseToken("invalid.token.string"));
    }
}
