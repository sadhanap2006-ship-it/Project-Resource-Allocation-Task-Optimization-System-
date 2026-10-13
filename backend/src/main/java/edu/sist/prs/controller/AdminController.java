package edu.sist.prs.controller;

import edu.sist.prs.dto.request.CreateUserRequest;
import edu.sist.prs.dto.request.UpdatePasswordRequest;
import edu.sist.prs.dto.request.UpdateUserRequest;
import edu.sist.prs.dto.response.ApiResponse;
import edu.sist.prs.dto.response.SettingItemDto;
import edu.sist.prs.dto.response.UserResponse;
import edu.sist.prs.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        List<UserResponse> users = adminService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success(users, "Users retrieved"));
    }

    @PostMapping("/users")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserResponse user = adminService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(user, "User created successfully"));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        UserResponse user = adminService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.success(user, "User updated successfully"));
    }

    @PutMapping("/users/{id}/password")
    public ResponseEntity<ApiResponse<Void>> updatePassword(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePasswordRequest request) {
        adminService.updatePassword(id, request);
        return ResponseEntity.ok(ApiResponse.success(null, "Password reset successfully"));
    }

    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<List<SettingItemDto>>> getSettings() {
        List<SettingItemDto> settings = adminService.getAllSettings();
        return ResponseEntity.ok(ApiResponse.success(settings, "Settings retrieved"));
    }

    @PutMapping("/settings")
    public ResponseEntity<ApiResponse<List<SettingItemDto>>> updateSettings(@RequestBody Map<String, String> newSettings) {
        List<SettingItemDto> updated = adminService.updateSettings(newSettings);
        return ResponseEntity.ok(ApiResponse.success(updated, "Settings updated successfully"));
    }
}
