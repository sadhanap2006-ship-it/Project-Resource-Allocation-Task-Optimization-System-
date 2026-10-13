package edu.sist.prs.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.sist.prs.dto.request.LoginRequest;
import edu.sist.prs.entity.UserEntity;
import edu.sist.prs.entity.UserRole;
import edu.sist.prs.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        if (!userRepository.existsByEmail("testpm@prs.local")) {
            UserEntity user = new UserEntity(
                "Test PM",
                "testpm@prs.local",
                passwordEncoder.encode("password123"),
                UserRole.PM,
                null
            );
            userRepository.save(user);
        }
    }

    @Test
    void testLoginSuccess_AndSubsequentMeCall() throws Exception {
        LoginRequest request = new LoginRequest("testpm@prs.local", "password123");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.data.email").value("testpm@prs.local"))
            .andExpect(jsonPath("$.data.role").value("PM"))
            .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession();

        // Second request with session must be authenticated (Section 11)
        mockMvc.perform(get("/api/auth/me").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.data.email").value("testpm@prs.local"));
    }

    @Test
    void testLoginInvalidPassword_Returns401() throws Exception {
        LoginRequest request = new LoginRequest("testpm@prs.local", "wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value("ERROR"))
            .andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"));
    }

    @Test
    void testGetMeUnauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value("ERROR"))
            .andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"));
    }
}
