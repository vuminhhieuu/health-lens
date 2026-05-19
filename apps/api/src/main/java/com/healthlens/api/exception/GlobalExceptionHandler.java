package com.healthlens.api.exception;

import com.healthlens.api.security.LoginRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private final LoginRateLimiter rateLimiter;

    public GlobalExceptionHandler(LoginRateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    /**
     * AC #6: 429 Too Many Requests with Retry-After header
     */
    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ProblemDetail> handleAccountLocked(AccountLockedException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
        problem.setType(URI.create("https://healthlens.vn/errors/account-locked"));
        problem.setTitle("Tài khoản bị khóa tạm thời");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("retryAfterSeconds", ex.getRetryAfterSeconds());
        applyErrorCode(problem, ApiErrorCode.ACCOUNT_LOCKED);
        
        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
                .body(problem);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ProblemDetail> handleRateLimitExceeded(
            RateLimitExceededException ex,
            HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
        problem.setType(URI.create("https://healthlens.vn/errors/rate-limited"));
        problem.setTitle("Quá nhiều yêu cầu");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("retryAfterSeconds", ex.getRetryAfterSeconds());
        applyErrorCode(problem, ApiErrorCode.RATE_LIMITED);

        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
                .body(problem);
    }

    @ExceptionHandler(AccountPendingDeletionException.class)
    public ProblemDetail handleAccountPendingDeletion(
            AccountPendingDeletionException ex,
            HttpServletRequest request) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                ex.getMessage()
        );

        problem.setTitle("Tài khoản đang chờ xóa");
        problem.setInstance(URI.create(request.getRequestURI()));
        applyErrorCode(problem, ApiErrorCode.ACCOUNT_PENDING_DELETION);

        return problem;
    }

    /**
     * AC #5: 401 Unauthorized with generic message (no email leak)
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                ex.getMessage() == null ? "Email hoặc mật khẩu không đúng." : ex.getMessage());
        problem.setType(URI.create("https://healthlens.vn/errors/unauthorized"));
        problem.setTitle("Không được xác thực");
        problem.setInstance(URI.create(request.getRequestURI()));
        applyErrorCode(problem, ApiErrorCode.INVALID_CREDENTIALS);
        return problem;
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ProblemDetail handleEmailExists(EmailAlreadyExistsException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Email này đã được đăng ký");
        problem.setType(URI.create("https://healthlens.vn/errors/email-already-exists"));
        problem.setTitle("Email đã tồn tại");
        problem.setInstance(URI.create(request.getRequestURI()));
        applyErrorCode(problem, ApiErrorCode.EMAIL_ALREADY_EXISTS);
        return problem;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
        if (isPendingProfileInvitationDuplicate(ex)) {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                    HttpStatus.CONFLICT, "Đã có lời mời đang chờ cho email này.");
            problem.setType(URI.create("https://healthlens.vn/errors/invitation-pending-exists"));
            problem.setTitle("Lời mời đang chờ");
            problem.setInstance(URI.create(request.getRequestURI()));
            applyErrorCode(problem, ApiErrorCode.BUSINESS_ERROR);
            return problem;
        }
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Email này đã được đăng ký");
        problem.setType(URI.create("https://healthlens.vn/errors/email-already-exists"));
        problem.setTitle("Email đã tồn tại");
        problem.setInstance(URI.create(request.getRequestURI()));
        applyErrorCode(problem, ApiErrorCode.EMAIL_ALREADY_EXISTS);
        return problem;
    }

    /**
     * Prevent DB errors from being masked as 401 via /error forward.
     * Always return 500 with a safe, user-friendly message.
     */
    @ExceptionHandler(DataAccessException.class)
    public ProblemDetail handleDataAccessException(DataAccessException ex, HttpServletRequest request) {
        log.error("Database error at {}: {}", request.getRequestURI(),
                ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage(), ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Lỗi hệ thống dữ liệu. Vui lòng thử lại sau."
        );
        problem.setType(URI.create("https://healthlens.vn/errors/database-error"));
        problem.setTitle("Lỗi hệ thống dữ liệu");
        problem.setInstance(URI.create(request.getRequestURI()));
        applyErrorCode(problem, ApiErrorCode.DATABASE_ERROR);
        return problem;
    }

    private static boolean isPendingProfileInvitationDuplicate(DataIntegrityViolationException ex) {
        String msg = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();
        return msg != null && msg.contains("uq_profile_invitations_pending_profile_email");
    }

    @ExceptionHandler(WeakPasswordException.class)
    public ProblemDetail handleWeakPassword(WeakPasswordException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create("https://healthlens.vn/errors/validation-error"));
        problem.setTitle("Dữ liệu không hợp lệ");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("errors", List.of(Map.of("field", "password", "message", ex.getMessage())));
        applyErrorCode(problem, ApiErrorCode.VALIDATION_ERROR);
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<Map<String, String>> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toFieldError)
                .toList();

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Dữ liệu đầu vào không hợp lệ");
        problem.setType(URI.create("https://healthlens.vn/errors/validation-error"));
        problem.setTitle("Dữ liệu không hợp lệ");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("errors", errors);
        applyErrorCode(problem, ApiErrorCode.VALIDATION_ERROR);
        return problem;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        List<Map<String, String>> errors = ex.getConstraintViolations()
                .stream()
                .map(violation -> Map.of(
                        "field", violation.getPropertyPath().toString(),
                        "message", violation.getMessage()
                ))
                .toList();

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Dữ liệu đầu vào không hợp lệ");
        problem.setType(URI.create("https://healthlens.vn/errors/validation-error"));
        problem.setTitle("Dữ liệu không hợp lệ");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("errors", errors);
        applyErrorCode(problem, ApiErrorCode.VALIDATION_ERROR);
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create("https://healthlens.vn/errors/validation-error"));
        problem.setTitle("Dữ liệu không hợp lệ");
        problem.setInstance(URI.create(request.getRequestURI()));
        applyErrorCode(problem, deletionCancelCode(ex));
        return problem;
    }

    @ExceptionHandler(DeletionCancellationTokenException.class)
    public ProblemDetail handleDeletionCancellationToken(
            DeletionCancellationTokenException ex,
            HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        problem.setType(URI.create("https://healthlens.vn/errors/deletion-cancel-token-invalid"));
        problem.setTitle("Liên kết hủy xóa không hợp lệ");
        problem.setInstance(URI.create(request.getRequestURI()));
        applyErrorCode(problem, ApiErrorCode.DELETION_CANCEL_TOKEN_INVALID);
        return problem;
    }

    @ExceptionHandler(DeletionCancellationConflictException.class)
    public ProblemDetail handleDeletionCancellationConflict(
            DeletionCancellationConflictException ex,
            HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://healthlens.vn/errors/deletion-cancel-conflict"));
        problem.setTitle("Yêu cầu xóa không còn có thể hủy");
        problem.setInstance(URI.create(request.getRequestURI()));
        applyErrorCode(problem, ApiErrorCode.INVALID_STATE);
        return problem;
    }

    @ExceptionHandler(DeletionCancellationForbiddenException.class)
    public ProblemDetail handleDeletionCancellationForbidden(
            DeletionCancellationForbiddenException ex,
            HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setType(URI.create("https://healthlens.vn/errors/deletion-cancel-forbidden"));
        problem.setTitle("Không thể hủy yêu cầu xóa");
        problem.setInstance(URI.create(request.getRequestURI()));
        applyErrorCode(problem, ApiErrorCode.FORBIDDEN);
        return problem;
    }

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusinessException(BusinessException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create("https://healthlens.vn/errors/business-error"));
        problem.setTitle("Lỗi xử lý nghiệp vụ");
        problem.setInstance(URI.create(request.getRequestURI()));
        applyErrorCode(problem, ApiErrorCode.BUSINESS_ERROR);
        return problem;
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://healthlens.vn/errors/invalid-state"));
        problem.setTitle("Trạng thái không hợp lệ");
        problem.setInstance(URI.create(request.getRequestURI()));
        applyErrorCode(problem, ApiErrorCode.INVALID_STATE);
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        String detail = "Nội dung yêu cầu không hợp lệ hoặc thiếu dữ liệu";
        log.warn("Invalid request body at {}: {}", request.getRequestURI(), ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setType(URI.create("https://healthlens.vn/errors/bad-request"));
        problem.setTitle("Yêu cầu không hợp lệ");
        problem.setInstance(URI.create(request.getRequestURI()));
        applyErrorCode(problem, ApiErrorCode.BAD_REQUEST);
        return problem;
    }

    private Map<String, String> toFieldError(FieldError error) {
        return Map.of(
                "field", error.getField(),
                "message", error.getDefaultMessage() == null ? "Giá trị không hợp lệ" : error.getDefaultMessage()
        );
    }

    @ExceptionHandler(ConsentRequiredException.class)
    public ProblemDetail handleConsentRequired(ConsentRequiredException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setType(URI.create("https://healthlens.vn/errors/consent-required"));
        problem.setTitle("Cần xác nhận đồng thuận");
        problem.setInstance(URI.create(request.getRequestURI()));
        applyErrorCode(problem, ApiErrorCode.CONSENT_REQUIRED);
        return problem;
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleUserNotFound(UserNotFoundException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://healthlens.vn/errors/user-not-found"));
        problem.setTitle("Không tìm thấy người dùng");
        problem.setInstance(URI.create(request.getRequestURI()));
        applyErrorCode(problem, ApiErrorCode.NOT_FOUND);
        return problem;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://healthlens.vn/errors/not-found"));
        problem.setTitle("Không tìm thấy dữ liệu");
        problem.setInstance(URI.create(request.getRequestURI()));
        applyErrorCode(problem, ApiErrorCode.NOT_FOUND);
        return problem;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setType(URI.create("https://healthlens.vn/errors/forbidden"));
        problem.setTitle("Không có quyền truy cập hồ sơ này");
        problem.setInstance(URI.create(request.getRequestURI()));
        applyErrorCode(problem, ApiErrorCode.FORBIDDEN);
        return problem;
    }

    @ExceptionHandler(ProfileAccessRevokedException.class)
    public ProblemDetail handleProfileAccessRevoked(ProfileAccessRevokedException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setType(URI.create("https://healthlens.vn/errors/profile-access-revoked"));
        problem.setTitle("Quyền truy cập hồ sơ đã bị thu hồi");
        problem.setInstance(URI.create(request.getRequestURI()));
        applyErrorCode(problem, ApiErrorCode.PROFILE_ACCESS_REVOKED);
        return problem;
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        log.warn("Upload too large at {}: {}", request.getRequestURI(), ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "Tệp vượt quá giới hạn tải lên của máy chủ. Vui lòng chọn tệp nhỏ hơn (tối đa 10MB)."
        );
        problem.setType(URI.create("https://healthlens.vn/errors/payload-too-large"));
        problem.setTitle("Tệp quá lớn");
        problem.setInstance(URI.create(request.getRequestURI()));
        applyErrorCode(problem, ApiErrorCode.VALIDATION_ERROR);
        return problem;
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ProblemDetail handleMissingServletRequestPart(
            MissingServletRequestPartException ex,
            HttpServletRequest request) {
        log.warn("Missing multipart part at {}: {}", request.getRequestURI(), ex.getRequestPartName());
        String detail = "Thiếu phần tải lên \"%s\". Gửi multipart/form-data với trường \"file\" chứa tệp CSV/JSON."
                .formatted(ex.getRequestPartName() != null ? ex.getRequestPartName() : "file");
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setType(URI.create("https://healthlens.vn/errors/missing-part"));
        problem.setTitle("Thiếu tệp tải lên");
        problem.setInstance(URI.create(request.getRequestURI()));
        applyErrorCode(problem, ApiErrorCode.BAD_REQUEST);
        return problem;
    }

    @ExceptionHandler(MultipartException.class)
    public ProblemDetail handleMultipartException(MultipartException ex, HttpServletRequest request) {
        log.warn("Multipart error at {}: {}", request.getRequestURI(), ex.getMessage());
        String detail = ex.getMessage() != null && !ex.getMessage().isBlank()
                ? ex.getMessage()
                : "Không đọc được dữ liệu tải lên. Kiểm tra gửi đúng multipart (trường \"file\").";
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setType(URI.create("https://healthlens.vn/errors/multipart"));
        problem.setTitle("Lỗi tải lên");
        problem.setInstance(URI.create(request.getRequestURI()));
        applyErrorCode(problem, ApiErrorCode.BAD_REQUEST);
        return problem;
    }

    private static void applyErrorCode(ProblemDetail problem, ApiErrorCode errorCode) {
        problem.setProperty("errorCode", errorCode.value());
    }

    private static ApiErrorCode deletionCancelCode(IllegalArgumentException ex) {
        String message = ex.getMessage();
        if (message != null && message.contains("Liên kết hủy yêu cầu")) {
            return ApiErrorCode.DELETION_CANCEL_TOKEN_INVALID;
        }
        return ApiErrorCode.VALIDATION_ERROR;
    }
}
