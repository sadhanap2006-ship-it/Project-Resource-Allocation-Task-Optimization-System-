package edu.sist.prs.service;

import edu.sist.prs.dto.request.SkillRequest;
import edu.sist.prs.dto.response.SkillResponse;
import edu.sist.prs.entity.SkillEntity;
import edu.sist.prs.exception.ConflictException;
import edu.sist.prs.repository.SkillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SkillService {

    private final SkillRepository skillRepository;

    public SkillService(SkillRepository skillRepository) {
        this.skillRepository = skillRepository;
    }

    @Transactional(readOnly = true)
    public List<SkillResponse> getAllSkills() {
        return skillRepository.findAll().stream()
            .map(s -> new SkillResponse(s.getId(), s.getName(), s.getCategory()))
            .collect(Collectors.toList());
    }

    @Transactional
    public SkillResponse createSkill(SkillRequest request) {
        if (skillRepository.existsByName(request.name())) {
            throw new ConflictException("DUPLICATE_SKILL", "Skill '" + request.name() + "' already exists");
        }
        SkillEntity skill = new SkillEntity(request.name(), request.category());
        SkillEntity saved = skillRepository.save(skill);
        return new SkillResponse(saved.getId(), saved.getName(), saved.getCategory());
    }
}
