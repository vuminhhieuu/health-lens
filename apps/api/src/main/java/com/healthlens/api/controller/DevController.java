package com.healthlens.api.controller;

import com.healthlens.api.constants.ApiRoutes;
import com.healthlens.api.entity.User;
import com.healthlens.api.repository.UserRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Development-only endpoints for testing.
 * Only active when running with 'docker' or 'dev' profile.
 * NOT available in production.
 */
@RestController
@RequestMapping(ApiRoutes.DEV_BASE)
@Profile({"docker", "dev"})
public class DevController {

    private final UserRepository userRepository;

    public DevController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Manually verify a user's email for development/testing.
     * Usage: POST /api/v1/dev/verify-email/user@example.com
     */
    @PostMapping("/verify-email/{email}")
    public ResponseEntity<Map<String, String>> verifyEmail(@PathVariable String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElse(null);

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        user.setEmailVerified(true);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "message", "Email verified successfully for: " + email,
                "userId", user.getId().toString()
        ));
    }
}
