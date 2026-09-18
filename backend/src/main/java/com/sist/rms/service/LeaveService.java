package com.sist.rms.service;

import com.sist.rms.dto.LeaveRequest;
import com.sist.rms.dto.LeaveResponse;
import com.sist.rms.entity.Developer;
import com.sist.rms.entity.DeveloperLeave;
import com.sist.rms.entity.LeaveStatus;
import com.sist.rms.exception.ResourceNotFoundException;
import com.sist.rms.exception.ValidationException;
import com.sist.rms.repository.DeveloperLeaveRepository;
import com.sist.rms.repository.DeveloperRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Planned leave. During leave a developer's effective availability is 0 %,
 * and ConflictService turns any overlapping allocation into a LEAVE CONFLICT.
 */
@Service
public class LeaveService {

    private final DeveloperLeaveRepository leaveRepository;
    private final DeveloperRepository developerRepository;

    public LeaveService(DeveloperLeaveRepository leaveRepository,
                        DeveloperRepository developerRepository) {
        this.leaveRepository = leaveRepository;
        this.developerRepository = developerRepository;
    }

    public List<LeaveResponse> findAll() {
        List<LeaveResponse> responses = new ArrayList<>();
        for (DeveloperLeave leave : leaveRepository.findAllByOrderByStartDateAsc()) {
            responses.add(toResponse(leave));
        }
        return responses;
    }

    public List<LeaveResponse> findByDeveloper(Long developerId) {
        if (!developerRepository.existsById(developerId)) {
            throw new ResourceNotFoundException("Developer", developerId);
        }
        List<LeaveResponse> responses = new ArrayList<>();
        for (DeveloperLeave leave : leaveRepository.findByDeveloperId(developerId)) {
            responses.add(toResponse(leave));
        }
        return responses;
    }

    @Transactional
    public LeaveResponse create(LeaveRequest request) {
        Developer developer = developerRepository.findById(request.getDeveloperId())
                .orElseThrow(() -> new ResourceNotFoundException("Developer", request.getDeveloperId()));

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new ValidationException("Leave end date cannot be before leave start date");
        }

        DeveloperLeave leave = new DeveloperLeave();
        leave.setDeveloper(developer);
        leave.setStartDate(request.getStartDate());
        leave.setEndDate(request.getEndDate());
        leave.setReason(request.getReason());
        leave.setStatus(parseStatus(request.getStatus()));
        return toResponse(leaveRepository.save(leave));
    }

    @Transactional
    public void delete(Long id) {
        DeveloperLeave leave = leaveRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave", id));
        leaveRepository.delete(leave);
    }

    public LeaveResponse toResponse(DeveloperLeave leave) {
        LeaveResponse response = new LeaveResponse();
        response.setId(leave.getId());
        response.setDeveloperId(leave.getDeveloper().getId());
        response.setDeveloperName(leave.getDeveloper().getName());
        response.setStartDate(leave.getStartDate());
        response.setEndDate(leave.getEndDate());
        response.setReason(leave.getReason());
        response.setStatus(leave.getStatus().name());
        return response;
    }

    private LeaveStatus parseStatus(String value) {
        if (value == null || value.trim().isEmpty()) {
            return LeaveStatus.APPROVED;
        }
        try {
            return LeaveStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("Leave status must be PLANNED, APPROVED or CANCELLED");
        }
    }
}
