package edu.sist.prs.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.sist.prs.dto.request.ProjectRequest;
import edu.sist.prs.entity.UserEntity;
import edu.sist.prs.entity.UserRole;
import edu.sist.prs.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class SecurityAndRbacIntegrationTest {

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
        if (!userRepository.existsByEmail("dev_rbac@prs.local")) {
            userRepository.save(new UserEntity("Dev RBAC", "dev_rbac@prs.local", passwordEncoder.encode("secret"), UserRole.DEVELOPER, null));
        }
        if (!userRepository.existsByEmail("pm_rbac@prs.local")) {
            userRepository.save(new UserEntity("PM RBAC", "pm_rbac@prs.local", passwordEncoder.encode("secret"), UserRole.PM, null));
        }
        if (!userRepository.existsByEmail("admin_rbac@prs.local")) {
            userRepository.save(new UserEntity("Admin RBAC", "admin_rbac@prs.local", passwordEncoder.encode("secret"), UserRole.ADMIN, null));
        }
    }

    @Test
    void testUnauthenticatedAccess_Returns401() throws Exception {
        mockMvc.perform(get("/api/projects"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value("ERROR"))
            .andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"));
    }

    @Test
    @WithMockUser(username = "dev_rbac@prs.local", roles = {"DEVELOPER"})
    void testDeveloperCannotCreateProject_Returns403() throws Exception {
        ProjectRequest request = new ProjectRequest(
            "TEST-DEV", "Dev Proj", "Desc", LocalDate.now(), LocalDate.now().plusDays(30), null, new BigDecimal("100000.00")
        );

        mockMvc.perform(post("/api/projects")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value("ERROR"))
            .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    @WithMockUser(username = "pm_rbac@prs.local", roles = {"PM"})
    void testPmCannotAccessAdmin_Returns403() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value("ERROR"))
            .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    @WithMockUser(username = "admin_rbac@prs.local", roles = {"ADMIN"})
    void testAdminCanAccessAdminUsers_Returns200() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    @WithMockUser(username = "pm_rbac@prs.local", roles = {"PM"})
    void testPmCanAccessProjects_Returns200() throws Exception {
        mockMvc.perform(get("/api/projects"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUCCESS"));
    }
}
