package edu.sist.prs.service;

import edu.sist.prs.entity.SettingEntity;
import edu.sist.prs.repository.SettingRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SettingsService {

    private final SettingRepository settingRepository;
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public SettingsService(SettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    @PostConstruct
    public void refreshCache() {
        cache.clear();
        for (SettingEntity s : settingRepository.findAll()) {
            cache.put(s.getSettingKey(), s.getSettingValue());
        }
    }

    public String get(String key, String defaultValue) {
        return cache.getOrDefault(key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        String val = cache.get(key);
        if (val == null) return defaultValue;
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public double getDouble(String key, double defaultValue) {
        String val = cache.get(key);
        if (val == null) return defaultValue;
        try {
            return Double.parseDouble(val.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
