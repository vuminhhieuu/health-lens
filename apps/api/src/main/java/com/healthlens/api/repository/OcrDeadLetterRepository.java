package com.healthlens.api.repository;

import com.healthlens.api.entity.OcrDeadLetter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OcrDeadLetterRepository extends JpaRepository<OcrDeadLetter, UUID> {
}
