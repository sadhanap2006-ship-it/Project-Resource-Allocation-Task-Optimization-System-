package com.sist.rms.controller;

import com.sist.rms.dto.ConflictResponse;
import com.sist.rms.dto.SkillGapResponse;
import com.sist.rms.dto.UtilizationResponse;
import com.sist.rms.service.ConflictService;
import com.sist.rms.service.SkillGapService;
import com.sist.rms.service.UtilizationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** REST API for conflicts, skill gaps and the utilization table. */
@RestController
@RequestMapping("/api")
public class ConflictController {

    private final ConflictService conflictService;
    private final SkillGapService skillGapService;
    private final UtilizationService utilizationService;

    public ConflictController(ConflictService conflictService,
                              SkillGapService skillGapService,
                              UtilizationService utilizationService) {
        this.conflictService = conflictService;
        this.skillGapService = skillGapService;
        this.utilizationService = utilizationService;
    }

    /** GET /api/conflicts  (optionally ?developerId=3) */
    @GetMapping("/conflicts")
    public List<ConflictResponse> getConflicts(@RequestParam(required = false) Long developerId) {
        return developerId == null
                ? conflictService.findAllConflicts()
                : conflictService.findConflictsForDeveloper(developerId);
    }

    @GetMapping("/skill-gaps")
    public List<SkillGapResponse> getSkillGaps() {
        return skillGapService.findAllSkillGaps();
    }

    @GetMapping("/utilization")
    public List<UtilizationResponse> getUtilization() {
        return utilizationService.getUtilization();
    }
}
