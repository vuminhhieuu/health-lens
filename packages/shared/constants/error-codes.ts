/**
 * Error Codes for HealthLens
 * 
 * Standardized error codes following RFC 7807 Problem Details format.
 * Single source of truth for error handling across frontend and backend.
 */

export const ErrorCode = {
  // =========================================
  // Authentication Errors (1xxx)
  // =========================================
  AUTH: {
    UNAUTHORIZED: 'AUTH_001',
    INVALID_CREDENTIALS: 'AUTH_002',
    TOKEN_EXPIRED: 'AUTH_003',
    TOKEN_INVALID: 'AUTH_004',
    TOKEN_MISSING: 'AUTH_005',
    REFRESH_TOKEN_EXPIRED: 'AUTH_006',
    REFRESH_TOKEN_INVALID: 'AUTH_007',
    ACCOUNT_LOCKED: 'AUTH_008',
    ACCOUNT_DISABLED: 'AUTH_009',
    EMAIL_NOT_VERIFIED: 'AUTH_010',
    PASSWORD_WEAK: 'AUTH_011',
    PASSWORD_MISMATCH: 'AUTH_012',
    OLD_PASSWORD_INCORRECT: 'AUTH_013',
  },

  // =========================================
  // Validation Errors (2xxx)
  // =========================================
  VALIDATION: {
    VALIDATION_ERROR: 'VAL_001',
    INVALID_INPUT: 'VAL_002',
    INVALID_EMAIL: 'VAL_003',
    INVALID_PASSWORD: 'VAL_004',
    FIELD_REQUIRED: 'VAL_005',
    FIELD_TOO_SHORT: 'VAL_006',
    FIELD_TOO_LONG: 'VAL_007',
    INVALID_FORMAT: 'VAL_008',
    INVALID_FILE_TYPE: 'VAL_009',
    FILE_TOO_LARGE: 'VAL_010',
    INVALID_DATE_RANGE: 'VAL_011',
  },

  // =========================================
  // Resource Errors (3xxx)
  // =========================================
  RESOURCE: {
    NOT_FOUND: 'RES_001',
    ALREADY_EXISTS: 'RES_002',
    RESOURCE_CONFLICT: 'RES_003',
    RESOURCE_DELETED: 'RES_004',
    RESOURCE_LOCKED: 'RES_005',
    PROFILE_NOT_FOUND: 'RES_010',
    HEALTH_RECORD_NOT_FOUND: 'RES_020',
    DOCUMENT_NOT_FOUND: 'RES_030',
    USER_NOT_FOUND: 'RES_040',
  },

  // =========================================
  // Business Logic Errors (4xxx)
  // =========================================
  BUSINESS: {
    MAX_PROFILES_REACHED: 'BIZ_001',
    MAX_HEALTH_RECORDS_REACHED: 'BIZ_002',
    DUPLICATE_ENTRY: 'BIZ_003',
    OPERATION_NOT_ALLOWED: 'BIZ_004',
    SUBSCRIPTION_REQUIRED: 'BIZ_005',
    QUOTA_EXCEEDED: 'BIZ_006',
    FEATURE_NOT_ENABLED: 'BIZ_007',
    TRIAL_EXPIRED: 'BIZ_008',
  },

  // =========================================
  // External Service Errors (5xxx)
  // =========================================
  EXTERNAL: {
    INTERNAL_ERROR: 'EXT_001',
    SERVICE_UNAVAILABLE: 'EXT_002',
    SERVICE_TIMEOUT: 'EXT_003',
    RATE_LIMIT_EXCEEDED: 'EXT_004',
    
    // AI Service errors
    AI_SERVICE_ERROR: 'EXT_010',
    AI_SERVICE_UNAVAILABLE: 'EXT_011',
    AI_SERVICE_TIMEOUT: 'EXT_012',
    AI_INVALID_RESPONSE: 'EXT_013',
    EMBEDDING_SERVICE_ERROR: 'EXT_020',
    
    // OCR Service errors
    OCR_SERVICE_ERROR: 'EXT_030',
    OCR_SERVICE_UNAVAILABLE: 'EXT_031',
    OCR_SERVICE_TIMEOUT: 'EXT_032',
    OCR_INVALID_IMAGE: 'EXT_033',
    OCR_NO_TEXT_FOUND: 'EXT_034',
    
    // Database errors
    DATABASE_ERROR: 'EXT_040',
    DATABASE_CONNECTION_FAILED: 'EXT_041',
    DATABASE_TIMEOUT: 'EXT_042',
    
    // Storage errors
    STORAGE_ERROR: 'EXT_050',
    STORAGE_UPLOAD_FAILED: 'EXT_051',
    STORAGE_DOWNLOAD_FAILED: 'EXT_052',
    STORAGE_DELETE_FAILED: 'EXT_053',
  },

  // =========================================
  // System Errors (9xxx)
  // =========================================
  SYSTEM: {
    UNKNOWN_ERROR: 'SYS_001',
    MAINTENANCE_MODE: 'SYS_002',
    CONFIGURATION_ERROR: 'SYS_003',
    INITIALIZATION_ERROR: 'SYS_004',
  },
} as const;

export type ErrorCodeCategory = keyof typeof ErrorCode;
export type ErrorCodeValue = typeof ErrorCode[ErrorCodeCategory][keyof typeof ErrorCode[ErrorCodeCategory]];

/**
 * Error message templates
 */
export const ErrorMessage = {
  [ErrorCode.AUTH.UNAUTHORIZED]: 'Bạn cần đăng nhập để thực hiện thao tác này.',
  [ErrorCode.AUTH.INVALID_CREDENTIALS]: 'Email hoặc mật khẩu không chính xác.',
  [ErrorCode.AUTH.TOKEN_EXPIRED]: 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.',
  [ErrorCode.AUTH.TOKEN_INVALID]: 'Token không hợp lệ.',
  [ErrorCode.AUTH.EMAIL_NOT_VERIFIED]: 'Vui lòng xác thực email trước khi tiếp tục.',
  [ErrorCode.AUTH.PASSWORD_WEAK]: 'Mật khẩu phải có ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường và số.',
  
  [ErrorCode.VALIDATION.VALIDATION_ERROR]: 'Dữ liệu không hợp lệ.',
  [ErrorCode.VALIDATION.FIELD_REQUIRED]: 'Trường này là bắt buộc.',
  [ErrorCode.VALIDATION.INVALID_EMAIL]: 'Email không hợp lệ.',
  [ErrorCode.VALIDATION.FILE_TOO_LARGE]: 'Kích thước file vượt quá giới hạn cho phép.',
  
  [ErrorCode.RESOURCE.NOT_FOUND]: 'Không tìm thấy tài nguyên yêu cầu.',
  [ErrorCode.RESOURCE.ALREADY_EXISTS]: 'Tài nguyên đã tồn tại.',
  
  [ErrorCode.EXTERNAL.SERVICE_UNAVAILABLE]: 'Dịch vụ tạm thời không khả dụng. Vui lòng thử lại sau.',
  [ErrorCode.EXTERNAL.SERVICE_TIMEOUT]: 'Yêu cầu hết thời gian chờ. Vui lòng thử lại.',
  [ErrorCode.EXTERNAL.RATE_LIMIT_EXCEEDED]: 'Bạn đã gửi quá nhiều yêu cầu. Vui lòng chờ một chút.',
  
  [ErrorCode.SYSTEM.UNKNOWN_ERROR]: 'Đã xảy ra lỗi không xác định. Vui lòng thử lại sau.',
} as const;

/**
 * HTTP Status Code mapping
 */
export const ErrorHttpStatus: Record<ErrorCodeValue, number> = {
  // Auth errors
  [ErrorCode.AUTH.UNAUTHORIZED]: 401,
  [ErrorCode.AUTH.INVALID_CREDENTIALS]: 401,
  [ErrorCode.AUTH.TOKEN_EXPIRED]: 401,
  [ErrorCode.AUTH.TOKEN_INVALID]: 401,
  [ErrorCode.AUTH.TOKEN_MISSING]: 401,
  [ErrorCode.AUTH.REFRESH_TOKEN_EXPIRED]: 401,
  [ErrorCode.AUTH.REFRESH_TOKEN_INVALID]: 401,
  [ErrorCode.AUTH.ACCOUNT_LOCKED]: 423,
  [ErrorCode.AUTH.ACCOUNT_DISABLED]: 403,
  [ErrorCode.AUTH.EMAIL_NOT_VERIFIED]: 403,
  [ErrorCode.AUTH.PASSWORD_WEAK]: 400,
  [ErrorCode.AUTH.PASSWORD_MISMATCH]: 400,
  [ErrorCode.AUTH.OLD_PASSWORD_INCORRECT]: 400,

  // Validation errors
  [ErrorCode.VALIDATION.VALIDATION_ERROR]: 400,
  [ErrorCode.VALIDATION.INVALID_INPUT]: 400,
  [ErrorCode.VALIDATION.INVALID_EMAIL]: 400,
  [ErrorCode.VALIDATION.INVALID_PASSWORD]: 400,
  [ErrorCode.VALIDATION.FIELD_REQUIRED]: 400,
  [ErrorCode.VALIDATION.FIELD_TOO_SHORT]: 400,
  [ErrorCode.VALIDATION.FIELD_TOO_LONG]: 400,
  [ErrorCode.VALIDATION.INVALID_FORMAT]: 400,
  [ErrorCode.VALIDATION.INVALID_FILE_TYPE]: 400,
  [ErrorCode.VALIDATION.FILE_TOO_LARGE]: 413,
  [ErrorCode.VALIDATION.INVALID_DATE_RANGE]: 400,

  // Resource errors
  [ErrorCode.RESOURCE.NOT_FOUND]: 404,
  [ErrorCode.RESOURCE.ALREADY_EXISTS]: 409,
  [ErrorCode.RESOURCE.RESOURCE_CONFLICT]: 409,
  [ErrorCode.RESOURCE.RESOURCE_DELETED]: 410,
  [ErrorCode.RESOURCE.RESOURCE_LOCKED]: 423,
  [ErrorCode.RESOURCE.PROFILE_NOT_FOUND]: 404,
  [ErrorCode.RESOURCE.HEALTH_RECORD_NOT_FOUND]: 404,
  [ErrorCode.RESOURCE.DOCUMENT_NOT_FOUND]: 404,
  [ErrorCode.RESOURCE.USER_NOT_FOUND]: 404,

  // Business errors
  [ErrorCode.BUSINESS.MAX_PROFILES_REACHED]: 400,
  [ErrorCode.BUSINESS.MAX_HEALTH_RECORDS_REACHED]: 400,
  [ErrorCode.BUSINESS.DUPLICATE_ENTRY]: 409,
  [ErrorCode.BUSINESS.OPERATION_NOT_ALLOWED]: 403,
  [ErrorCode.BUSINESS.SUBSCRIPTION_REQUIRED]: 402,
  [ErrorCode.BUSINESS.QUOTA_EXCEEDED]: 429,
  [ErrorCode.BUSINESS.FEATURE_NOT_ENABLED]: 403,
  [ErrorCode.BUSINESS.TRIAL_EXPIRED]: 402,

  // External errors
  [ErrorCode.EXTERNAL.INTERNAL_ERROR]: 500,
  [ErrorCode.EXTERNAL.SERVICE_UNAVAILABLE]: 503,
  [ErrorCode.EXTERNAL.SERVICE_TIMEOUT]: 504,
  [ErrorCode.EXTERNAL.RATE_LIMIT_EXCEEDED]: 429,
  [ErrorCode.EXTERNAL.AI_SERVICE_ERROR]: 502,
  [ErrorCode.EXTERNAL.AI_SERVICE_UNAVAILABLE]: 503,
  [ErrorCode.EXTERNAL.AI_SERVICE_TIMEOUT]: 504,
  [ErrorCode.EXTERNAL.AI_INVALID_RESPONSE]: 502,
  [ErrorCode.EXTERNAL.EMBEDDING_SERVICE_ERROR]: 502,
  [ErrorCode.EXTERNAL.OCR_SERVICE_ERROR]: 502,
  [ErrorCode.EXTERNAL.OCR_SERVICE_UNAVAILABLE]: 503,
  [ErrorCode.EXTERNAL.OCR_SERVICE_TIMEOUT]: 504,
  [ErrorCode.EXTERNAL.OCR_INVALID_IMAGE]: 400,
  [ErrorCode.EXTERNAL.OCR_NO_TEXT_FOUND]: 422,
  [ErrorCode.EXTERNAL.DATABASE_ERROR]: 500,
  [ErrorCode.EXTERNAL.DATABASE_CONNECTION_FAILED]: 503,
  [ErrorCode.EXTERNAL.DATABASE_TIMEOUT]: 504,
  [ErrorCode.EXTERNAL.STORAGE_ERROR]: 500,
  [ErrorCode.EXTERNAL.STORAGE_UPLOAD_FAILED]: 500,
  [ErrorCode.EXTERNAL.STORAGE_DOWNLOAD_FAILED]: 500,
  [ErrorCode.EXTERNAL.STORAGE_DELETE_FAILED]: 500,

  // System errors
  [ErrorCode.SYSTEM.UNKNOWN_ERROR]: 500,
  [ErrorCode.SYSTEM.MAINTENANCE_MODE]: 503,
  [ErrorCode.SYSTEM.CONFIGURATION_ERROR]: 500,
  [ErrorCode.SYSTEM.INITIALIZATION_ERROR]: 500,
} as const;
