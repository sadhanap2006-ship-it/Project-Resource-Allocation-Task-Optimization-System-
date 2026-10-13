package edu.sist.prs.service;

import edu.sist.prs.dto.request.CreateResourceRequest;
import edu.sist.prs.dto.request.ResourceSkillInput;
import edu.sist.prs.dto.request.UpdateResourceRequest;
import edu.sist.prs.dto.response.ResourceProjectAllocationDto;
import edu.sist.prs.dto.response.ResourceResponse;
import edu.sist.prs.dto.response.ResourceSkillDto;
import edu.sist.prs.entity.*;
import edu.sist.prs.exception.BadRequestException;
import edu.sist.prs.exception.ConflictException;
import edu.sist.prs.exception.ResourceNotFoundException;
import edu.sist.prs.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final DeveloperProfileRepository developerProfileRepository;
    private final HardwareAssetRepository hardwareAssetRepository;
    private final CloudResourceRepository cloudResourceRepository;
    private final ResourceSkillRepository resourceSkillRepository;
    private final SkillRepository skillRepository;

    public ResourceService(
            ResourceRepository resourceRepository,
            DeveloperProfileRepository developerProfileRepository,
            HardwareAssetRepository hardwareAssetRepository,
            CloudResourceRepository cloudResourceRepository,
            ResourceSkillRepository resourceSkillRepository,
            SkillRepository skillRepository) {
        this.resourceRepository = resourceRepository;
        this.developerProfileRepository = developerProfileRepository;
        this.hardwareAssetRepository = hardwareAssetRepository;
        this.cloudResourceRepository = cloudResourceRepository;
        this.resourceSkillRepository = resourceSkillRepository;
        this.skillRepository = skillRepository;
    }

    @Transactional(readOnly = true)
    public List<ResourceResponse> getAllResources(ResourceType type, ResourceStatus status, String query) {
        List<ResourceEntity> list;
        if (type != null && status != null) {
            list = resourceRepository.findByTypeAndStatus(type, status);
        } else if (type != null) {
            list = resourceRepository.findByType(type);
        } else if (status != null) {
            list = resourceRepository.findByStatus(status);
        } else {
            list = resourceRepository.findAll();
        }

        if (query != null && !query.trim().isEmpty()) {
            String qLower = query.toLowerCase();
            list = list.stream()
                .filter(r -> r.getName().toLowerCase().contains(qLower))
                .collect(Collectors.toList());
        }

        return list.stream().map(this::toResourceResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ResourceResponse getResourceById(Long id) {
        ResourceEntity resource = resourceRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Resource", id));
        return toResourceResponse(resource);
    }

    @Transactional
    public ResourceResponse createResource(CreateResourceRequest request) {
        ResourceEntity resource = new ResourceEntity(
            request.type(),
            request.name(),
            ResourceStatus.ACTIVE,
            request.capacityPct(),
            request.costRate(),
            request.costUnit()
        );
        ResourceEntity saved = resourceRepository.save(resource);

        switch (request.type()) {
            case DEVELOPER -> {
                if (request.email() == null || request.email().isBlank()) {
                    throw new BadRequestException("VALIDATION_FAILED", "Email is required for DEVELOPER resource");
                }
                if (developerProfileRepository.findByEmail(request.email()).isPresent()) {
                    throw new ConflictException("DUPLICATE_EMAIL", "Developer profile with email '" + request.email() + "' already exists");
                }
                DeveloperProfileEntity profile = new DeveloperProfileEntity(saved, request.email(), request.jobTitle());
                developerProfileRepository.save(profile);
            }
            case HARDWARE -> {
                if (request.assetTag() == null || request.assetTag().isBlank()) {
                    throw new BadRequestException("VALIDATION_FAILED", "Asset tag is required for HARDWARE resource");
                }
                if (hardwareAssetRepository.findByAssetTag(request.assetTag()).isPresent()) {
                    throw new ConflictException("DUPLICATE_ASSET_TAG", "Asset tag '" + request.assetTag() + "' already exists");
                }
                HardwareAssetEntity asset = new HardwareAssetEntity(saved, request.assetTag(), request.labLocation());
                hardwareAssetRepository.save(asset);
            }
            case CLOUD -> {
                if (request.provider() == null || request.unitName() == null || request.dailyQuotaUnits() == null || request.monthlyCap() == null) {
                    throw new BadRequestException("VALIDATION_FAILED", "Cloud details (provider, unitName, dailyQuotaUnits, monthlyCap) are required for CLOUD resource");
                }
                CloudResourceEntity cloud = new CloudResourceEntity(saved, request.provider(), request.unitName(), request.dailyQuotaUnits(), request.monthlyCap());
                cloudResourceRepository.save(cloud);
            }
        }

        return toResourceResponse(saved);
    }

    @Transactional
    public ResourceResponse updateResource(Long id, UpdateResourceRequest request) {
        ResourceEntity resource = resourceRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Resource", id));

        resource.setName(request.name());
        resource.setStatus(request.status());
        resource.setCapacityPct(request.capacityPct());
        resource.setCostRate(request.costRate());
        resource.setCostUnit(request.costUnit());

        switch (resource.getType()) {
            case DEVELOPER -> {
                DeveloperProfileEntity profile = developerProfileRepository.findById(id)
                    .orElseGet(() -> new DeveloperProfileEntity(resource, request.email(), request.jobTitle()));
                if (request.email() != null && !request.email().equalsIgnoreCase(profile.getEmail())
                        && developerProfileRepository.findByEmail(request.email()).isPresent()) {
                    throw new ConflictException("DUPLICATE_EMAIL", "Developer profile with email '" + request.email() + "' already exists");
                }
                if (request.email() != null) profile.setEmail(request.email());
                if (request.jobTitle() != null) profile.setJobTitle(request.jobTitle());
                developerProfileRepository.save(profile);
            }
            case HARDWARE -> {
                HardwareAssetEntity asset = hardwareAssetRepository.findById(id)
                    .orElseGet(() -> new HardwareAssetEntity(resource, request.assetTag(), request.labLocation()));
                if (request.assetTag() != null && !request.assetTag().equalsIgnoreCase(asset.getAssetTag())
                        && hardwareAssetRepository.findByAssetTag(request.assetTag()).isPresent()) {
                    throw new ConflictException("DUPLICATE_ASSET_TAG", "Asset tag '" + request.assetTag() + "' already exists");
                }
                if (request.assetTag() != null) asset.setAssetTag(request.assetTag());
                if (request.labLocation() != null) asset.setLabLocation(request.labLocation());
                hardwareAssetRepository.save(asset);
            }
            case CLOUD -> {
                CloudResourceEntity cloud = cloudResourceRepository.findById(id)
                    .orElseGet(() -> new CloudResourceEntity(resource, request.provider(), request.unitName(), request.dailyQuotaUnits(), request.monthlyCap()));
                if (request.provider() != null) cloud.setProvider(request.provider());
                if (request.unitName() != null) cloud.setUnitName(request.unitName());
                if (request.dailyQuotaUnits() != null) cloud.setDailyQuotaUnits(request.dailyQuotaUnits());
                if (request.monthlyCap() != null) cloud.setMonthlyCap(request.monthlyCap());
                cloudResourceRepository.save(cloud);
            }
        }

        return toResourceResponse(resource);
    }

    @Transactional
    public void deleteResource(Long id) {
        ResourceEntity resource = resourceRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Resource", id));
        // Section 20 Clarification 1: status is only ACTIVE or INACTIVE. DELETE sets INACTIVE.
        resource.setStatus(ResourceStatus.INACTIVE);
    }

    @Transactional
    public List<ResourceSkillDto> updateResourceSkills(Long resourceId, List<ResourceSkillInput> skillsInput) {
        ResourceEntity resource = resourceRepository.findById(resourceId)
            .orElseThrow(() -> new ResourceNotFoundException("Resource", resourceId));

        if (resource.getType() != ResourceType.DEVELOPER) {
            throw new BadRequestException("VALIDATION_FAILED", "Skills can only be assigned to DEVELOPER resources");
        }

        resourceSkillRepository.deleteByIdResourceId(resourceId);

        List<ResourceSkillEntity> entities = new ArrayList<>();
        for (ResourceSkillInput input : skillsInput) {
            SkillEntity skill = skillRepository.findById(input.skillId())
                .orElseThrow(() -> new ResourceNotFoundException("Skill", input.skillId()));
            ResourceSkillEntity rse = new ResourceSkillEntity(resource, skill, input.proficiency());
            entities.add(rse);
        }

        resourceSkillRepository.saveAll(entities);

        return entities.stream()
            .map(rse -> new ResourceSkillDto(
                rse.getSkill().getId(),
                rse.getSkill().getName(),
                rse.getSkill().getCategory(),
                rse.getProficiency()))
            .collect(Collectors.toList());
    }

    public ResourceResponse toResourceResponse(ResourceEntity resource) {
        String email = null;
        String jobTitle = null;
        List<ResourceSkillDto> skills = List.of();

        String assetTag = null;
        String labLocation = null;

        String provider = null;
        String unitName = null;
        java.math.BigDecimal dailyQuotaUnits = null;
        java.math.BigDecimal monthlyCap = null;

        if (resource.getType() == ResourceType.DEVELOPER) {
            var profile = developerProfileRepository.findById(resource.getId()).orElse(null);
            if (profile != null) {
                email = profile.getEmail();
                jobTitle = profile.getJobTitle();
            }
            skills = resourceSkillRepository.findByIdResourceId(resource.getId()).stream()
                .map(rse -> new ResourceSkillDto(
                    rse.getSkill().getId(),
                    rse.getSkill().getName(),
                    rse.getSkill().getCategory(),
                    rse.getProficiency()))
                .collect(Collectors.toList());
        } else if (resource.getType() == ResourceType.HARDWARE) {
            var asset = hardwareAssetRepository.findById(resource.getId()).orElse(null);
            if (asset != null) {
                assetTag = asset.getAssetTag();
                labLocation = asset.getLabLocation();
            }
        } else if (resource.getType() == ResourceType.CLOUD) {
            var cloud = cloudResourceRepository.findById(resource.getId()).orElse(null);
            if (cloud != null) {
                provider = cloud.getProvider();
                unitName = cloud.getUnitName();
                dailyQuotaUnits = cloud.getDailyQuotaUnits();
                monthlyCap = cloud.getMonthlyCap();
            }
        }

        return new ResourceResponse(
            resource.getId(),
            resource.getType(),
            resource.getName(),
            resource.getStatus(),
            resource.getCapacityPct(),
            resource.getCostRate(),
            resource.getCostUnit(),
            email,
            jobTitle,
            skills,
            assetTag,
            labLocation,
            provider,
            unitName,
            dailyQuotaUnits,
            monthlyCap,
            0, // Default placeholders, populated by Engine in Phase 4
            0,
            resource.getCapacityPct(),
            List.of()
        );
    }
}
