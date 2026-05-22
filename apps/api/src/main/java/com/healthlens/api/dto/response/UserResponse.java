package com.healthlens.api.dto.response;

import java.time.LocalDate;
import java.util.UUID;

public record UserResponse(
                UUID id,
                String email,
                String fullName,
                LocalDate birthDate,
                String gender,
                boolean emailVerified,
                Boolean consentGiven,
                String avatarUrl,
                String personalDescription,
                String personalNotes,
                String chronicConditions,
                String currentMedications,
                String allergies) {
}
