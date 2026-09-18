package com.sist.rms.service;

import com.sist.rms.dto.SkillRequest;
import com.sist.rms.dto.SkillResponse;
import com.sist.rms.entity.Skill;
import com.sist.rms.exception.ResourceNotFoundException;
import com.sist.rms.exception.ValidationException;
import com.sist.rms.repository.SkillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/** Manages the master list of skills. */
@Service
public class SkillService {

    private final SkillRepository skillRepository;

    public SkillService(SkillRepository skillRepository) {
        this.skillRepository = skillRepository;
    }

    public List<SkillResponse> findAll() {
        List<SkillResponse> responses = new ArrayList<>();
        for (Skill skill : skillRepository.findAllByOrderByNameAsc()) {
            responses.add(toResponse(skill));
        }
        return responses;
    }

    public SkillResponse findById(Long id) {
        return toResponse(getSkillOrThrow(id));
    }

    @Transactional
    public SkillResponse create(SkillRequest request) {
        String name = request.getName() == null ? "" : request.getName().trim();
        if (name.isEmpty()) {
            throw new ValidationException("Skill name is required");
        }
        if (skillRepository.existsByNameIgnoreCase(name)) {
            throw new ValidationException("A skill named '" + name + "' already exists");
        }
        Skill skill = new Skill();
        skill.setName(name);
        skill.setCategory(request.getCategory());
        return toResponse(skillRepository.save(skill));
    }

    @Transactional
    public void delete(Long id) {
        skillRepository.delete(getSkillOrThrow(id));
    }

    /**
     * Looks a skill up by name and creates it when it is new, so the user can
     * simply type "AWS" on the developer screen without adding it first.
     */
    @Transactional
    public Skill findOrCreateByName(String name) {
        String clean = name == null ? "" : name.trim();
        if (clean.isEmpty()) {
            throw new ValidationException("Skill name is required");
        }
        return skillRepository.findByNameIgnoreCase(clean).orElseGet(() -> {
            Skill skill = new Skill();
            skill.setName(clean);
            return skillRepository.save(skill);
        });
    }

    public Skill getSkillOrThrow(Long id) {
        return skillRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Skill", id));
    }

    private SkillResponse toResponse(Skill skill) {
        return new SkillResponse(skill.getId(), skill.getName(), skill.getCategory());
    }
}
