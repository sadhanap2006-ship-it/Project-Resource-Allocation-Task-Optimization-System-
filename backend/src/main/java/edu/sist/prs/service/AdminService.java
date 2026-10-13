package edu.sist.prs.service;

import edu.sist.prs.dto.request.CreateUserRequest;
import edu.sist.prs.dto.request.UpdatePasswordRequest;
import edu.sist.prs.dto.request.UpdateUserRequest;
import edu.sist.prs.dto.response.SettingItemDto;
import edu.sist.prs.dto.response.UserResponse;
import edu.sist.prs.entity.ResourceEntity;
import edu.sist.prs.entity.ResourceType;
import edu.sist.prs.entity.SettingEntity;
import edu.sist.prs.entity.UserEntity;
import edu.sist.prs.entity.UserRole;
import edu.sist.prs.exception.BadRequestException;
import edu.sist.prs.exception.ConflictException;
import edu.sist.prs.exception.ResourceNotFoundException;
import edu.sist.prs.repository.ResourceRepository;
import edu.sist.prs.repository.SettingRepository;
import edu.sist.prs.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final ResourceRepository resourceRepository;
    private final SettingRepository settingRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminService(
            UserRepository userRepository,
            ResourceRepository resourceRepository,
            SettingRepository settingRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.resourceRepository = resourceRepository;
        this.settingRepository = settingRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
            .map(this::toUserResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("DUPLICATE_EMAIL", "User with email '" + request.email() + "' already exists");
        }

        validateResourceRoleLink(request.role(), request.resourceId());

        UserEntity user = new UserEntity(
            request.name(),
            request.email(),
            passwordEncoder.encode(request.password()),
            request.role(),
            request.resourceId()
        );

        UserEntity saved = userRepository.save(user);
        return toUserResponse(saved);
    }

    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        UserEntity user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));

        if (!user.getEmail().equalsIgnoreCase(request.email()) && userRepository.existsByEmail(request.email())) {
            throw new ConflictException("DUPLICATE_EMAIL", "User with email '" + request.email() + "' already exists");
        }

        validateResourceRoleLink(request.role(), request.resourceId());

        user.setName(request.name());
        user.setEmail(request.email());
        user.setRole(request.role());
        user.setResourceId(request.resourceId());
        user.setActive(request.active());

        return toUserResponse(user);
    }

    @Transactional
    public void updatePassword(Long id, UpdatePasswordRequest request) {
        UserEntity user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));
        user.setPasswordHash(passwordEncoder.encode(request.password()));
    }

    @Transactional(readOnly = true)
    public List<SettingItemDto> getAllSettings() {
        return settingRepository.findAll().stream()
            .map(s -> new SettingItemDto(s.getSettingKey(), s.getSettingValue(), s.getDescription()))
            .collect(Collectors.toList());
    }

    @Transactional
    public List<SettingItemDto> updateSettings(Map<String, String> newSettings) {
        Map<String, SettingEntity> currentMap = settingRepository.findAll().stream()
            .collect(Collectors.toMap(SettingEntity::getSettingKey, s -> s));

        // Create projected map to validate ranking weights
        Map<String, String> projected = currentMap.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getSettingValue()));
        projected.putAll(newSettings);

        // Section 8: The three ranking weights must sum to 1.0 (ADMIN update rejected otherwise, 400)
        try {
            double wSkill = Double.parseDouble(projected.getOrDefault("ranking.weight.skill", "0.5"));
            double wFit = Double.parseDouble(projected.getOrDefault("ranking.weight.fit", "0.3"));
            double wCost = Double.parseDouble(projected.getOrDefault("ranking.weight.cost", "0.2"));

            if (Math.abs((wSkill + wFit + wCost) - 1.0) > 0.0001) {
                throw new BadRequestException("INVALID_SETTINGS", "Ranking weights (skill, fit, cost) must sum to 1.0");
            }
        } catch (NumberFormatException e) {
            throw new BadRequestException("INVALID_SETTINGS", "Ranking weights must be numeric decimal values");
        }

        for (Map.Entry<String, String> entry : newSettings.entrySet()) {
            SettingEntity entity = currentMap.get(entry.getKey());
            if (entity != null) {
                entity.setSettingValue(entry.getValue());
            } else {
                entity = new SettingEntity(entry.getKey(), entry.getValue(), null);
                currentMap.put(entry.getKey(), entity);
            }
            settingRepository.save(entity);
        }

        return getAllSettings();
    }

    private void validateResourceRoleLink(UserRole role, Long resourceId) {
        if (role == UserRole.DEVELOPER) {
            if (resourceId != null) {
                ResourceEntity resource = resourceRepository.findById(resourceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Resource", resourceId));
                if (resource.getType() != ResourceType.DEVELOPER) {
                    throw new BadRequestException("VALIDATION_FAILED", "Developer users may only be linked to DEVELOPER resources");
                }
            }
        } else {
            if (resourceId != null) {
                throw new BadRequestException("VALIDATION_FAILED", "Only DEVELOPER users may have an associated resource_id");
            }
        }
    }

    private UserResponse toUserResponse(UserEntity user) {
        return new UserResponse(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getRole(),
            user.getResourceId(),
            user.getActive(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }
}
