/**
 * Environment Configuration for HealthLens
 * 
 * Centralized environment variable access with type safety.
 * Provides fallback values for development.
 */

/**
 * API Configuration
 */
export const API_CONFIG = {
  /** Backend API base URL */
  BASE_URL: process.env.NEXT_PUBLIC_API_BASE_URL ?? 'http://localhost:8080',
  
  /** API version prefix */
  VERSION: 'v1',
  
  /** Full API URL */
  URL: (() => {
    const base = process.env.NEXT_PUBLIC_API_BASE_URL ?? 'http://localhost:8080';
    return `${base}/api/v1`;
  })(),
} as const;

/**
 * OCR Service Configuration
 */
export const OCR_CONFIG = {
  /** OCR service base URL */
  BASE_URL: process.env.EXPO_PUBLIC_OCR_SERVICE_URL ?? 
            process.env.NEXT_PUBLIC_OCR_SERVICE_URL ?? 
            'http://localhost:8001',
  
  /** Default OCR timeout in ms */
  TIMEOUT: 60_000,
  
  /** Supported image formats */
  SUPPORTED_FORMATS: ['image/jpeg', 'image/png', 'image/webp', 'image/heic'] as const,
  
  /** SINGLE SOURCE: keep in sync with UPLOAD_MAX_SIZE_BYTES and services/ocr-service/app.py. */
  MAX_FILE_SIZE: 20 * 1024 * 1024,
} as const;

/**
 * AI/ML Configuration
 */
export const AI_CONFIG = {
  /** Default AI model */
  DEFAULT_MODEL: 'qwen-2.5-72b-versatile',
  
  /** Embedding model */
  EMBEDDING_MODEL: 'text-embedding-3-small',
  
  /** Default temperature for AI responses */
  DEFAULT_TEMPERATURE: 0.7,
  
  /** Maximum tokens for AI responses */
  MAX_TOKENS: 500,
} as const;

/**
 * App Configuration
 */
export const APP_CONFIG = {
  /** App name */
  NAME: 'HealthLens',
  
  /** App version */
  VERSION: process.env.NEXT_PUBLIC_APP_VERSION ?? '1.0.0',
  
  /** Environment */
  ENVIRONMENT: (process.env.NEXT_PUBLIC_APP_ENV ?? process.env.NODE_ENV ?? 'development') as 'development' | 'staging' | 'production',
  
  /** Is development mode */
  IS_DEV: (process.env.NEXT_PUBLIC_APP_ENV ?? process.env.NODE_ENV) === 'development',
  
  /** Is production mode */
  IS_PROD: (process.env.NEXT_PUBLIC_APP_ENV ?? process.env.NODE_ENV) === 'production',
  
  /** Is staging mode */
  IS_STAGING: (process.env.NEXT_PUBLIC_APP_ENV ?? process.env.NODE_ENV) === 'staging',
} as const;

/**
 * Feature Flags
 */
export const FEATURE_FLAGS = {
  /** Enable AI analysis feature */
  ENABLE_AI_ANALYSIS: process.env.NEXT_PUBLIC_FLAG_AI_ANALYSIS !== 'false',
  
  /** Enable OCR feature */
  ENABLE_OCR: process.env.NEXT_PUBLIC_FLAG_OCR !== 'false',
  
  /** Enable dark mode */
  ENABLE_DARK_MODE: process.env.NEXT_PUBLIC_FLAG_DARK_MODE !== 'false',
  
  /** Enable notifications */
  ENABLE_NOTIFICATIONS: process.env.NEXT_PUBLIC_FLAG_NOTIFICATIONS !== 'false',
} as const;

/**
 * Pagination defaults
 */
export const PAGINATION = {
  DEFAULT_PAGE: 0,
  DEFAULT_SIZE: 20,
  MAX_SIZE: 100,
} as const;

/**
 * Validation limits
 */
export const VALIDATION_LIMITS = {
  /** Maximum profile name length */
  PROFILE_NAME_MAX: 100,
  
  /** Maximum health record title length */
  HEALTH_RECORD_TITLE_MAX: 200,
  
  /** Maximum notes length */
  NOTES_MAX: 5000,
  
  /** Password minimum length */
  PASSWORD_MIN: 8,
  
  /** Password maximum length */
  PASSWORD_MAX: 128,
} as const;

/**
 * Storage Configuration
 */
export const STORAGE_CONFIG = {
  /** MinIO/S3 bucket name */
  BUCKET: process.env.NEXT_PUBLIC_STORAGE_BUCKET ?? 'healthlens',
  
  /** Storage endpoint */
  ENDPOINT: process.env.NEXT_PUBLIC_STORAGE_ENDPOINT ?? 'localhost:9000',
  
  /** Use SSL for storage */
  USE_SSL: process.env.NODE_ENV === 'production',
} as const;

/**
 * Export all config
 */
export const ENV = {
  API: API_CONFIG,
  OCR: OCR_CONFIG,
  AI: AI_CONFIG,
  APP: APP_CONFIG,
  FEATURES: FEATURE_FLAGS,
  PAGINATION,
  VALIDATION: VALIDATION_LIMITS,
  STORAGE: STORAGE_CONFIG,
} as const;

export type ENV = typeof ENV;
