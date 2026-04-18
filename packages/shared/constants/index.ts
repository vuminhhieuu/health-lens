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