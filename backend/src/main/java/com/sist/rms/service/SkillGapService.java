package com.sist.rms.service;

import com.sist.rms.dto.SkillGapResponse;
import com.sist.rms.entity.*;
import com.sist.rms.repository.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * SKILL-GAP DETECTION
 *
 *   required = the skills listed for the milestone
 *   covered  = every skill owned by the developers allocated to it
 *   missing  = required - covered
 *
 * If 'missing' is not empty the milestone has a SKILL GAP. For each missing
 * skill we then look for any ACTIVE developer in the company who has it:
 *
 *   somebody has it  ->  we suggest them
 *   nobody has it    ->  "Skill Gap: No available developer has this skill."
 *
 * The same class also answers "how well does ONE developer match this
 * milestone?", which the allocation screen and the what-if simulator use.
 */
@Service
public class SkillGapService {

    private final MilestoneRepository milestoneRepository;
    private final MilestoneSkillRepository milestoneSkillRepository;
    private final ResourceAllocationRepository allocationRepository;
    private final DeveloperSkillRepository developerSkillRepository;
    private final DeveloperRepository developerRepository;

    public SkillGapService(MilestoneRepository milestoneRepository,
                           MilestoneSkillRepository milestoneSkillRepository,
                           ResourceAllocationRepository allocationRepository,
                           DeveloperSkillRepository developerSkillRepository,
                           DeveloperRepository developerRepository) {
        this.milestoneRepository = milestoneRepository;
        this.milestoneSkillRepository = milestoneSkillRepository;
        this.allocationRepository = allocationRepository;
        this.developerSkillRepository = developerSkillRepository;
        this.developerRepository = developerRepository;
    }

    // -----------------------------------------------------------------
    //  Milestone level
    // -----------------------------------------------------------------

    /** Only the milestones that actually have a gap. */
    public List<SkillGapResponse> findAllSkillGaps() {
        List<SkillGapResponse> gaps = new ArrayList<>();
        for (Milestone milestone : milestoneRepository.findAllByOrderByStartDateAsc()) {
            SkillGapResponse gap = analyseMilestone(milestone);
            if (!gap.getMissingSkills().isEmpty()) {
                gaps.add(gap);
            }
        }
        return gaps;
    }

    /** Full analysis of one milestone, whether or not it has a gap. */
    public SkillGapResponse analyseMilestone(Milestone milestone) {
        SkillGapResponse response = new SkillGapResponse();
        response.setMilestoneId(milestone.getId());
        response.setMilestoneName(milestone.getName());
        response.setProjectId(milestone.getProject().getId());
        response.setProjectName(milestone.getProject().getName());

        Set<String> required = requiredSkillNames(milestone.getId());
        response.setRequiredSkills(new ArrayList<>(required));

        // union of the skills of everybody allocated to this milestone
        Set<String> covered = new LinkedHashSet<>();
        for (ResourceAllocation allocation : allocationRepository.findByMilestoneId(milestone.getId())) {
            covered.addAll(developerSkillNames(allocation.getDeveloper().getId()));
        }
        // only the skills that are actually relevant to this milestone
        Set<String> coveredRelevant = new LinkedHashSet<>();
        for (String skill : required) {
            if (containsIgnoreCase(covered, skill)) {
                coveredRelevant.add(skill);
            }
        }
        response.setCoveredSkills(new ArrayList<>(coveredRelevant));

        List<String> missing = new ArrayList<>();
        for (String skill : required) {
            if (!containsIgnoreCase(covered, skill)) {
                missing.add(skill);
            }
        }
        response.setMissingSkills(missing);

        if (missing.isEmpty()) {
            response.setMessage("All required skills are covered.");
            return response;
        }

        // for every missing skill: who in the company could do it?
        List<String> suggestions = new ArrayList<>();
        for (String skill : missing) {
            List<String> candidates = activeDevelopersWithSkill(skill);
            if (candidates.isEmpty()) {
                suggestions.add(skill + " - Skill Gap: No available developer has this skill.");
            } else {
                suggestions.add(skill + " - available developers: " + String.join(", ", candidates));
            }
        }
        response.setSuggestions(suggestions);
        response.setMessage("SKILL GAP: " + milestone.getName() + " is missing "
                + String.join(", ", missing) + ".");
        return response;
    }

    // -----------------------------------------------------------------
    //  Developer level (used by allocations and by the simulator)
    // -----------------------------------------------------------------

    /** Required skills of the milestone that this developer does NOT have. */
    public List<String> missingSkillsForDeveloper(Long developerId, Long milestoneId) {
        Set<String> required = requiredSkillNames(milestoneId);
        Set<String> owned = developerSkillNames(developerId);
        List<String> missing = new ArrayList<>();
        for (String skill : required) {
            if (!containsIgnoreCase(owned, skill)) {
                missing.add(skill);
            }
        }
        return missing;
    }

    /**
     * skill match = (required skills the developer has / total required) x 100
     * A milestone with no required skills counts as a 100 % match.
     */
    public int skillMatchPercentage(Long developerId, Long milestoneId) {
        Set<String> required = requiredSkillNames(milestoneId);
        if (required.isEmpty()) {
            return 100;
        }
        Set<String> owned = developerSkillNames(developerId);
        int matched = 0;
        for (String skill : required) {
            if (containsIgnoreCase(owned, skill)) {
                matched++;
            }
        }
        return (int) Math.round((matched * 100.0) / required.size());
    }

    // -----------------------------------------------------------------
    //  Small shared helpers
    // -----------------------------------------------------------------

    public Set<String> requiredSkillNames(Long milestoneId) {
        Set<String> names = new LinkedHashSet<>();
        for (MilestoneSkill milestoneSkill : milestoneSkillRepository.findByMilestoneId(milestoneId)) {
            names.add(milestoneSkill.getSkill().getName());
        }
        return names;
    }

    public Set<String> developerSkillNames(Long developerId) {
        Set<String> names = new LinkedHashSet<>();
        for (DeveloperSkill developerSkill : developerSkillRepository.findByDeveloperId(developerId)) {
            names.add(developerSkill.getSkill().getName());
        }
        return names;
    }

    /** Names of every ACTIVE developer who has the given skill. */
    public List<String> activeDevelopersWithSkill(String skillName) {
        List<String> names = new ArrayList<>();
        for (Developer developer : developerRepository.findByStatus(DeveloperStatus.ACTIVE)) {
            if (containsIgnoreCase(developerSkillNames(developer.getId()), skillName)) {
                names.add(developer.getName());
            }
        }
        return names;
    }

    private boolean containsIgnoreCase(Set<String> set, String value) {
        for (String item : set) {
            if (item.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }
}
