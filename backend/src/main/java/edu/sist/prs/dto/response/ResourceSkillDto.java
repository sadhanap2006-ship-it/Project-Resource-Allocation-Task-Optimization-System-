package edu.sist.prs.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ResourceSkillDto(
    Long skillId,
    String skillName,
    String category,
    Integer proficiency
) {
    @JsonProperty("id")
    public Long id() {
        return skillId;
    }

    @JsonProperty("name")
    public String name() {
        return skillName;
    }

    @JsonProperty("proficiencyLevel")
    public Integer proficiencyLevel() {
        return proficiency;
    }

    @JsonProperty("proficiencyText")
    public String proficiencyText() {
        if (proficiency == null) return "INTERMEDIATE";
        return switch (proficiency) {
            case 1 -> "BEGINNER";
            case 2 -> "INTERMEDIATE";
            case 3 -> "EXPERT";
            default -> "LEVEL " + proficiency;
        };
    }
}
