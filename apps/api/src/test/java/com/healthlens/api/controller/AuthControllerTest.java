package com.healthlens.api.controller;

import com.healthlens.api.config.SecurityConfig;
import com.healthlens.api.dto.request.RegisterRequest;
import com.healthlens.api.exception.EmailAlreadyExistsException;
import com.healthlens.api.exception.GlobalExceptionHandler;
import com.healthlens.api.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    @DisplayName("POST /api/v1/auth/register -> 201 khi dang ky thanh cong")
    void register_success() throws Exception {
        when(authService.register(any(RegisterRequest.class))).thenReturn(UUID.randomUUID());

        RegisterRequest request = new RegisterRequest("Nguyen Van A", "user@example.com", java.time.LocalDate.of(1999, 1, 1), "StrongPass1");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.message").exists())
                .andExpect(jsonPath("$.meta.timestamp").exists());
    }

    @Test
    @DisplayName("POST /api/v1/auth/register -> 409 khi email da ton tai")
    void register_duplicateEmail() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new EmailAlreadyExistsException("Email nay da duoc dang ky"));

        RegisterRequest request = new RegisterRequest("Nguyen Van A", "existing@example.com", java.time.LocalDate.of(1999, 1, 1), "StrongPass1");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://healthlens.vn/errors/email-already-exists"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register -> 400 va co field-level details khi password khong dat")
    void register_weakPasswordValidation() throws Exception {
        RegisterRequest request = new RegisterRequest("Nguyen Van A", "user@example.com", java.time.LocalDate.of(1999, 1, 1), "weak");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://healthlens.vn/errors/validation-error"))
                .andExpect(jsonPath("$.errors[0].field").value("password"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register -> 409 khi DB unique constraint bi vi pham")
    void register_dbIntegrityViolation() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new DataIntegrityViolationException("users_uk_email"));

        RegisterRequest request = new RegisterRequest("Nguyen Van A", "existing@example.com", java.time.LocalDate.of(1999, 1, 1), "StrongPass1");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://healthlens.vn/errors/email-already-exists"))
                .andExpect(jsonPath("$.status").value(409));
    }

    private String toJson(RegisterRequest request) {
        return "{\"fullName\":\"" + request.fullName() + "\",\"email\":\"" + request.email() + "\",\"birthDate\":\"" + request.birthDate() + "\",\"password\":\"" + request.password() + "\"}";
    }
}
