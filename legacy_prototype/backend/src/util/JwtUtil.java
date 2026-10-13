package util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * JwtUtil - Pure Java SE HMAC-SHA256 JWT Token Utility
 * 
 * Provides standard RFC 7519 JWT token generation, signature validation,
 * claims extraction, and token expiration verification using built-in standard library.
 */
public class JwtUtil {

    private static final String SECRET_KEY = "SIST_CSE_AI_CAPSTONE_PROJECT_RESOURCE_ALLOCATION_JWT_SUPER_SECRET_KEY_2026";
    private static final String HMAC_ALGO = "HmacSHA256";
    private static final long ACCESS_TOKEN_VALIDITY_MS = 24 * 60 * 60 * 1000L; // 24 Hours
    private static final long REFRESH_TOKEN_VALIDITY_MS = 7 * 24 * 60 * 60 * 1000L; // 7 Days

    public static String generateToken(int userId, String email, String role, Integer developerId) {
        long now = System.currentTimeMillis();
        long exp = now + ACCESS_TOKEN_VALIDITY_MS;

        String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payloadJson = String.format(
            "{\"userId\":%d,\"email\":\"%s\",\"role\":\"%s\",\"developerId\":%s,\"iat\":%d,\"exp\":%d}",
            userId,
            escapeJson(email),
            escapeJson(role),
            developerId != null ? developerId : "null",
            now,
            exp
        );

        String b64Header = base64UrlEncode(headerJson.getBytes(StandardCharsets.UTF_8));
        String b64Payload = base64UrlEncode(payloadJson.getBytes(StandardCharsets.UTF_8));
        String signature = sign(b64Header + "." + b64Payload, SECRET_KEY);

        return b64Header + "." + b64Payload + "." + signature;
    }

    public static String generateRefreshToken(int userId, String email) {
        long now = System.currentTimeMillis();
        long exp = now + REFRESH_TOKEN_VALIDITY_MS;

        String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payloadJson = String.format(
            "{\"userId\":%d,\"email\":\"%s\",\"tokenType\":\"refresh\",\"iat\":%d,\"exp\":%d}",
            userId,
            escapeJson(email),
            now,
            exp
        );

        String b64Header = base64UrlEncode(headerJson.getBytes(StandardCharsets.UTF_8));
        String b64Payload = base64UrlEncode(payloadJson.getBytes(StandardCharsets.UTF_8));
        String signature = sign(b64Header + "." + b64Payload, SECRET_KEY);

        return b64Header + "." + b64Payload + "." + signature;
    }

    public static Map<String, Object> validateAndExtractClaims(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }

        if (token.startsWith("Bearer ")) {
            token = token.substring(7).trim();
        }

        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return null;
        }

        String b64Header = parts[0];
        String b64Payload = parts[1];
        String signature = parts[2];

        // 1. Verify signature
        String expectedSignature = sign(b64Header + "." + b64Payload, SECRET_KEY);
        if (!expectedSignature.equals(signature)) {
            return null;
        }

        // 2. Decode payload
        try {
            byte[] payloadBytes = base64UrlDecode(b64Payload);
            String payloadJson = new String(payloadBytes, StandardCharsets.UTF_8);
            Map<String, Object> claims = JsonUtil.parseObject(payloadJson);

            // 3. Verify expiry
            Object expObj = claims.get("exp");
            if (expObj != null) {
                long exp = expObj instanceof Number ? ((Number) expObj).longValue() : Long.parseLong(expObj.toString());
                if (System.currentTimeMillis() > exp) {
                    return null; // Expired token
                }
            }
            return claims;
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isAuthorizedRole(String token, String... allowedRoles) {
        Map<String, Object> claims = validateAndExtractClaims(token);
        if (claims == null) return false;
        String role = (String) claims.get("role");
        if (role == null) return false;

        for (String r : allowedRoles) {
            if (role.equalsIgnoreCase(r)) return true;
        }
        return false;
    }

    private static String sign(String data, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGO);
            mac.init(secretKeySpec);
            byte[] signedBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return base64UrlEncode(signedBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("HMAC signing failed", e);
        }
    }

    private static String base64UrlEncode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static byte[] base64UrlDecode(String str) {
        return Base64.getUrlDecoder().decode(str);
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
