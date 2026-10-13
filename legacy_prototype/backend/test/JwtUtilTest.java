package test;

import util.JwtUtil;

import java.util.Map;

public class JwtUtilTest {

    public static void runTests() {
        System.out.println("Running JwtUtilTest...");
        int passed = 0;
        int total = 4;

        // Test 1: Token Generation and Claim Extraction
        String token = JwtUtil.generateToken(2, "arun@company.com", "EMPLOYEE", 1);
        if (token == null || !token.contains(".")) {
            throw new AssertionError("JWT token generation failed");
        }
        Map<String, Object> claims = JwtUtil.validateAndExtractClaims(token);
        if (claims == null || !"arun@company.com".equals(claims.get("email"))) {
            throw new AssertionError("JWT claims extraction failed");
        }
        passed++;

        // Test 2: Role Authorization
        boolean isEmp = JwtUtil.isAuthorizedRole(token, "EMPLOYEE", "ADMIN");
        boolean isManager = JwtUtil.isAuthorizedRole(token, "MANAGER");
        if (!isEmp || isManager) {
            throw new AssertionError("Role authorization check failed");
        }
        passed++;

        // Test 3: Tampered Signature Detection
        String tamperedToken = token.substring(0, token.lastIndexOf('.') + 1) + "invalidSignature123";
        Map<String, Object> tamperedClaims = JwtUtil.validateAndExtractClaims(tamperedToken);
        if (tamperedClaims != null) {
            throw new AssertionError("Tampered token should be rejected");
        }
        passed++;

        // Test 4: Refresh Token
        String refreshToken = JwtUtil.generateRefreshToken(2, "arun@company.com");
        Map<String, Object> refreshClaims = JwtUtil.validateAndExtractClaims(refreshToken);
        if (refreshClaims == null || !"refresh".equals(refreshClaims.get("tokenType"))) {
            throw new AssertionError("Refresh token validation failed");
        }
        passed++;

        System.out.println("  [PASS] JwtUtilTest (" + passed + "/" + total + " passed)");
    }
}
