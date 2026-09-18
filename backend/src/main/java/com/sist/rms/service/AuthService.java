package com.sist.rms.service;

import com.sist.rms.dto.LoginRequest;
import com.sist.rms.dto.LoginResponse;
import com.sist.rms.entity.User;
import com.sist.rms.exception.ValidationException;
import com.sist.rms.repository.UserRepository;
import org.springframework.stereotype.Service;

/**
 * A very small login used only to decide WHICH DASHBOARD to open.
 *
 * Note for the viva: the demo passwords are stored as plain text in the
 * users table. A real system would store a BCrypt hash and use Spring
 * Security; that was left out on purpose to keep the project inside the
 * agreed technology stack and easy to explain.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername().trim())
                .orElseThrow(() -> new ValidationException("Invalid username or password"));

        if (!user.getPassword().equals(request.getPassword())) {
            throw new ValidationException("Invalid username or password");
        }

        LoginResponse response = new LoginResponse();
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setRole(user.getRole().name());
        if (user.getDeveloper() != null) {
            response.setDeveloperId(user.getDeveloper().getId());
            response.setDeveloperName(user.getDeveloper().getName());
        }
        return response;
    }
}
