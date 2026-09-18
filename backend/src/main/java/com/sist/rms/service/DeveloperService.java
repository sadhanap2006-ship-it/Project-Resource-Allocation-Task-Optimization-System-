package com.sist.rms.service;

import com.sist.rms.dto.DeveloperRequest;
import com.sist.rms.dto.DeveloperResponse;
import com.sist.rms.dto.DeveloperSkillRequest;
import com.sist.rms.entity.*;
import com.sist.rms.exception.ResourceNotFoundException;
import com.sist.rms.exception.ValidationException;
import com.sist.rms.repository.DeveloperRepository;
import com.sist.rms.repository.DeveloperSkillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Create, read, update and delete developers, and manage their skills. */
@Service
public class DeveloperService {

    private final DeveloperRepository developerRepository;
    private final DeveloperSkillRepository developerSkillRepository;
    private final SkillService skillService;

    public DeveloperService(DeveloperRepository developerRepository,
                            DeveloperSkillRepository developerSkillRepository,
                            SkillService skillService) {
        this.developerRepository = developerRepository;
        this.developerSkillRepository = developerSkillRepository;
        this.skillService = skillService;
    }

    public List<DeveloperResponse> findAll() {
        List<DeveloperResponse> responses = new ArrayList<>();
        for (Developer developer : developerRepository.findAllByOrderByNameAsc()) {
            responses.add(toResponse(developer));
        }
        return responses;
    }

    public DeveloperResponse findById(Long id) {
        return toResponse(getDeveloperOrThrow(id));
    }

    @Transactional
    public DeveloperResponse create(DeveloperRequest request) {
        validate(request);
        if (developerRepository.existsByEmailIgnoreCase(request.getEmail().trim())) {
            throw new ValidationException("A developer with this email already exists");
        }

        Developer developer = new Developer();
        applyRequest(developer, request);
        developer = developerRepository.save(developer);

        for (String skillName : request.getSkills()) {
            addSkillInternal(developer, skillName, ProficiencyLevel.INTERMEDIATE);
        }
        return toResponse(developer);
    }

    @Transactional
    public DeveloperResponse update(Long id, DeveloperRequest request) {
        validate(request);
        Developer developer = getDeveloperOrThrow(id);

        developerRepository.findByEmailIgnoreCase(request.getEmail().trim())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new ValidationException("A developer with this email already exists");
                });

        applyRequest(developer, request);
        developer = developerRepository.save(developer);

        // when the request carries a skill list, it replaces the old one
        if (request.getSkills() != null && !request.getSkills().isEmpty()) {
            developerSkillRepository.deleteAll(developerSkillRepository.findByDeveloperId(id));
            for (String skillName : request.getSkills()) {
                addSkillInternal(developer, skillName, ProficiencyLevel.INTERMEDIATE);
            }
        }
        return toResponse(developer);
    }

    @Transactional
    public void delete(Long id) {
        developerRepository.delete(getDeveloperOrThrow(id));
    }

    // -----------------------------------------------------------------
    //  Skills of one developer
    // -----------------------------------------------------------------

    @Transactional
    public DeveloperResponse addSkill(Long developerId, DeveloperSkillRequest request) {
        Developer developer = getDeveloperOrThrow(developerId);
        ProficiencyLevel level = parseProficiency(request.getProficiencyLevel());
        addSkillInternal(developer, request.getSkillName(), level);
        return toResponse(developer);
    }

    @Transactional
    public DeveloperResponse removeSkill(Long developerId, Long skillId) {
        Developer developer = getDeveloperOrThrow(developerId);
        DeveloperSkill link = developerSkillRepository
                .findByDeveloperIdAndSkillId(developerId, skillId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "This developer does not have the skill with id " + skillId));
        developerSkillRepository.delete(link);
        return toResponse(developer);
    }

    private void addSkillInternal(Developer developer, String skillName, ProficiencyLevel level) {
        Skill skill = skillService.findOrCreateByName(skillName);
        if (developerSkillRepository.findByDeveloperIdAndSkillId(developer.getId(), skill.getId()).isPresent()) {
            return;     // already there - adding it twice is not an error
        }
        DeveloperSkill link = new DeveloperSkill();
        link.setDeveloper(developer);
        link.setSkill(skill);
        link.setProficiencyLevel(level);
        developerSkillRepository.save(link);
    }

    // -----------------------------------------------------------------
    //  Helpers
    // -----------------------------------------------------------------

    public Developer getDeveloperOrThrow(Long id) {
        return developerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Developer", id));
    }

    public DeveloperResponse toResponse(Developer developer) {
        DeveloperResponse response = new DeveloperResponse();
        response.setId(developer.getId());
        response.setName(developer.getName());
        response.setEmail(developer.getEmail());
        response.setRole(developer.getRole());
        response.setHourlyCost(developer.getHourlyCost());
        response.setAvailabilityPercentage(developer.getAvailabilityPercentage());
        response.setStatus(developer.getStatus().name());

        List<String> skills = new ArrayList<>();
        for (DeveloperSkill link : developerSkillRepository.findByDeveloperId(developer.getId())) {
            skills.add(link.getSkill().getName());
        }
        response.setSkills(skills);
        return response;
    }

    private void applyRequest(Developer developer, DeveloperRequest request) {
        developer.setName(request.getName().trim());
        developer.setEmail(request.getEmail().trim());
        developer.setRole(request.getRole().trim());
        developer.setHourlyCost(request.getHourlyCost());
        developer.setAvailabilityPercentage(request.getAvailabilityPercentage());
        developer.setStatus(parseStatus(request.getStatus()));
    }

    /** Extra checks on top of the annotations, with clear messages. */
    private void validate(DeveloperRequest request) {
        List<String> errors = new ArrayList<>();
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            errors.add("Name is required");
        }
        if (request.getHourlyCost() != null && request.getHourlyCost().compareTo(BigDecimal.ZERO) < 0) {
            errors.add("Hourly cost cannot be negative");
        }
        if (request.getAvailabilityPercentage() != null
                && (request.getAvailabilityPercentage() < 0 || request.getAvailabilityPercentage() > 100)) {
            errors.add("Availability percentage must be between 0 and 100");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    private DeveloperStatus parseStatus(String value) {
        if (value == null || value.trim().isEmpty()) {
            return DeveloperStatus.ACTIVE;
        }
        try {
            return DeveloperStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("Status must be one of ACTIVE, INACTIVE or ON_LEAVE");
        }
    }

    private ProficiencyLevel parseProficiency(String value) {
        if (value == null || value.trim().isEmpty()) {
            return ProficiencyLevel.INTERMEDIATE;
        }
        try {
            return ProficiencyLevel.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("Proficiency level must be BEGINNER, INTERMEDIATE or EXPERT");
        }
    }
}
