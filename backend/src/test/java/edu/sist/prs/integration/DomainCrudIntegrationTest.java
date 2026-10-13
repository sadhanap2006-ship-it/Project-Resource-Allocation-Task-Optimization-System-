package edu.sist.prs.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.sist.prs.dto.request.*;
import edu.sist.prs.entity.CostUnit;
import edu.sist.prs.entity.ResourceType;
import edu.sist.prs.entity.UserEntity;
import edu.sist.prs.entity.UserRole;
import edu.sist.prs.repository.AllocationRepository;
import edu.sist.prs.repository.ProjectRepository;
import edu.sist.prs.repository.ResourceRepository;
import edu.sist.prs.repository.SkillRepository;
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
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class DomainCrudIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private AllocationRepository allocationRepository;

    @BeforeEach
    void setUp() {
        projectRepository.findByCode("INT-01").ifPresent(p -> {
            allocationRepository.findByProjectId(p.getId()).forEach(allocationRepository::delete);
            projectRepository.delete(p);
        });
        skillRepository.findByName("Java Core").ifPresent(skillRepository::delete);
        resourceRepository.findAll().stream()
                .filter(r -> "Integration Dev".equals(r.getName()))
                .forEach(r -> {
                    allocationRepository.findByResourceId(r.getId()).forEach(allocationRepository::delete);
                    resourceRepository.delete(r);
                });

        if (!userRepository.existsByEmail("crud_pm@prs.local")) {
            userRepository.save(new UserEntity("CRUD PM", "crud_pm@prs.local", passwordEncoder.encode("secret"), UserRole.PM, null));
        }
    }

    @Test
    @WithMockUser(username = "crud_pm@prs.local", roles = {"PM", "ADMIN"})
    void testEndToEndDomainCrud() throws Exception {
        LocalDate today = LocalDate.now();

        // 1. Create Project
        ProjectRequest projReq = new ProjectRequest(
            "INT-01", "Integration Project", "Testing CRUD flows",
            today, today.plusDays(60), null, new BigDecimal("500000.00")
        );
        MvcResult projRes = mockMvc.perform(post("/api/projects")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(projReq)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.data.code").value("INT-01"))
            .andReturn();

        Number projIdNum = com.jayway.jsonpath.JsonPath.read(projRes.getResponse().getContentAsString(), "$.data.id");
        Long projectId = projIdNum.longValue();

        // 2. Create Milestone inside Project dates
        MilestoneRequest msReq = new MilestoneRequest(
            "Sprint 1", "Core APIs", today, today.plusDays(20), null, 0, new BigDecimal("160.00")
        );
        MvcResult msRes = mockMvc.perform(post("/api/projects/" + projectId + "/milestones")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(msReq)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andReturn();

        Number msIdNum = com.jayway.jsonpath.JsonPath.read(msRes.getResponse().getContentAsString(), "$.data.id");
        Long milestoneId = msIdNum.longValue();

        // 3. Milestone outside Project dates -> 400
        MilestoneRequest invalidMsReq = new MilestoneRequest(
            "Sprint Invalid", "Bad dates", today.minusDays(10), today.plusDays(20), null, 0, new BigDecimal("80.00")
        );
        mockMvc.perform(post("/api/projects/" + projectId + "/milestones")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidMsReq)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("INVALID_DATE_RANGE"));

        // 4. Create Skill
        SkillRequest skillReq = new SkillRequest("Java Core", "Backend");
        MvcResult skillRes = mockMvc.perform(post("/api/skills")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(skillReq)))
            .andExpect(status().isCreated())
            .andReturn();

        Number skillIdNum = com.jayway.jsonpath.JsonPath.read(skillRes.getResponse().getContentAsString(), "$.data.id");
        Long skillId = skillIdNum.longValue();

        // 5. Create Developer Resource
        CreateResourceRequest resReq = new CreateResourceRequest(
            ResourceType.DEVELOPER, "Integration Dev", 100, new BigDecimal("600.00"), CostUnit.HOUR,
            "int_dev@prs.local", "Senior Engineer", null, null, null, null, null, null
        );
        MvcResult resourceRes = mockMvc.perform(post("/api/resources")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resReq)))
            .andExpect(status().isCreated())
            .andReturn();

        Number resIdNum = com.jayway.jsonpath.JsonPath.read(resourceRes.getResponse().getContentAsString(), "$.data.id");
        Long resourceId = resIdNum.longValue();

        // 6. Assign Skill to Resource
        List<ResourceSkillInput> skillsInput = List.of(new ResourceSkillInput(skillId, 3));
        mockMvc.perform(put("/api/resources/" + resourceId + "/skills")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(skillsInput)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].proficiency").value(3));

        // 7. Create Allocation inside Milestone dates
        AllocationRequest allocReq = new AllocationRequest(
            resourceId, milestoneId, today, today.plusDays(10), 60, false
        );
        MvcResult allocRes = mockMvc.perform(post("/api/allocations")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(allocReq)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.allocationPct").value(60))
            .andReturn();

        Number allocIdNum = com.jayway.jsonpath.JsonPath.read(allocRes.getResponse().getContentAsString(), "$.data.id");
        Long allocationId = allocIdNum.longValue();

        // 8. Duplicate Allocation -> 409
        mockMvc.perform(post("/api/allocations")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(allocReq)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.errorCode").value("DUPLICATE_ALLOCATION"));

        // 9. Delete Allocation -> 204
        mockMvc.perform(delete("/api/allocations/" + allocationId)
                .with(csrf()))
            .andExpect(status().isNoContent());
    }
}
