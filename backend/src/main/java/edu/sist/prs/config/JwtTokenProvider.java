package edu.sist.prs.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Standard RFC 7519 JSON Web Token (JWT) generator and validator using HMAC-SHA256.
 * Completely standalone using Java Standard Cryptography and Jackson.
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${security.jwt.secret:c3VwZXJfc2VjcmV0X2p3dF9rZXlfZm9yX3Byc19hcHBsaWNhdGlvbl8yMDI2}")
    private String jwtSecret;

    @Value("${security.jwt.expiration-ms:86400000}") // 24 hours default
    private long jwtExpirationMs;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String generateToken(String email, String role, Long userId, String name, Long resourceId) {
        long now = System.currentTimeMillis();
        long exp = now + jwtExpirationMs;

        Map<String, Object> header = new HashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> payload = new HashMap<>();
        payload.put("sub", email);
        payload.put("role", role);
        payload.put("userId", userId);
        payload.put("name", name);
        if (resourceId != null) {
            payload.put("resourceId", resourceId);
        }
        payload.put("iat", now / 1000);
        payload.put("exp", exp / 1000);

        try {
            String encodedHeader = base64UrlEncode(objectMapper.writeValueAsString(header).getBytes(StandardCharsets.UTF_8));
            String encodedPayload = base64UrlEncode(objectMapper.writeValueAsString(payload).getBytes(StandardCharsets.UTF_8));
            String content = encodedHeader + "." + encodedPayload;
            String signature = sign(content, jwtSecret);
            return content + "." + signature;
        } catch (Exception e) {
            log.error("Failed to generate JWT token for {}", email, e);
            throw new RuntimeException("Failed to generate JWT token", e);
        }
    }

    public boolean validateToken(String token) {
        try {
            if (token == null || token.isBlank()) {
                return false;
            }
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return false;
            }
            String content = parts[0] + "." + parts[1];
            String expectedSignature = sign(content, jwtSecret);
            if (!MessageDigest.isEqual(parts[2].getBytes(StandardCharsets.UTF_8), expectedSignature.getBytes(StandardCharsets.UTF_8))) {
                log.warn("Invalid JWT signature");
                return false;
            }

            // Check expiration
            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            Map<?, ?> payload = objectMapper.readValue(payloadBytes, Map.class);
            Number exp = (Number) payload.get("exp");
            if (exp != null && (exp.longValue() * 1000) < System.currentTimeMillis()) {
                log.warn("JWT token has expired");
                return false;
            }
            return true;
        } catch (Exception e) {
            log.warn("JWT validation error: {}", e.getMessage());
            return false;
        }
    }

    public String getEmailFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            Map<?, ?> payload = objectMapper.readValue(payloadBytes, Map.class);
            return (String) payload.get("sub");
        } catch (Exception e) {
            return null;
        }
    }

    private String sign(String data, String secret) throws Exception {
        Mac hmacSha256 = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        hmacSha256.init(secretKey);
        byte[] signatureBytes = hmacSha256.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return base64UrlEncode(signatureBytes);
    }

    private String base64UrlEncode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
