/**
 * HealthLens Shared Constants
 * 
 * Centralized constants for the entire application.
 */

// Constants
export * from './api';
export * from './error-codes';
export * from './status';

// Config (from parent directory)
export * from '../config';
export * from './consent';

export const GENDER_OPTIONS = ['male', 'female', 'other'] as const;
export type Gender = typeof GENDER_OPTIONS[number];

export const UPLOAD_MAX_SIZE_BYTES = 20 * 1024 * 1024;
export const ALLOWED_FILE_TYPES = ['application/pdf', 'image/jpeg', 'image/png'] as const;