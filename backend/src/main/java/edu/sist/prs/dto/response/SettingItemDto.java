package edu.sist.prs.dto.response;

public record SettingItemDto(
    String key,
    String value,
    String description
) {}
