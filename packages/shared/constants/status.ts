/**
 * Status Enums for HealthLens
 * 
 * Standardized status values across the application.
 */

/**
 * User account status
 */
export const UserStatus = {
  /** Account is active and can be used */
  ACTIVE: 'ACTIVE',
  /** Account is temporarily disabled */
  INACTIVE: 'INACTIVE',
  /** Account is permanently banned */
  BANNED: 'BANNED',
  /** Account created but email not verified */
  PENDING_VERIFICATION: 'PENDING_VERIFICATION',
  /** Account is soft-deleted */
  DELETED: 'DELETED',
} as const;

export type UserStatus = typeof UserStatus[keyof typeof UserStatus];

/**
 * User profile status
 */
export const ProfileStatus = {
  /** Profile is active and accessible */
  ACTIVE: 'ACTIVE',
  /** Profile is inactive (hidden but not deleted) */
  INACTIVE: 'INACTIVE',
  /** Profile is being created or updated */
  PENDING: 'PENDING',
} as const;

export type ProfileStatus = typeof ProfileStatus[keyof typeof ProfileStatus];

/**
 * Health record processing status
 */
export const HealthRecordStatus = {
  /** Record is a draft, not yet submitted */
  DRAFT: 'DRAFT',
  /** Record is being processed (OCR, AI analysis) */
  PROCESSING: 'PROCESSING',
  /** Record processing completed successfully */
  COMPLETED: 'COMPLETED',
  /** Record processing failed */
  FAILED: 'FAILED',
  /** Record is archived */
  ARCHIVED: 'ARCHIVED',
} as const;

export type HealthRecordStatus = typeof HealthRecordStatus[keyof typeof HealthRecordStatus];

/**
 * AI analysis status
 */
export const AnalysisStatus = {
  /** Analysis not started */
  PENDING: 'PENDING',
  /** Analysis is in progress */
  IN_PROGRESS: 'IN_PROGRESS',
  /** Analysis completed successfully */
  COMPLETED: 'COMPLETED',
  /** Analysis failed */
  FAILED: 'FAILED',
  /** Analysis result expired */
  EXPIRED: 'EXPIRED',
} as const;

export type AnalysisStatus = typeof AnalysisStatus[keyof typeof AnalysisStatus];

/**
 * Document type
 */
export const DocumentType = {
  /** Medical prescription */
  PRESCRIPTION: 'PRESCRIPTION',
  /** Medical test results */
  LAB_RESULT: 'LAB_RESULT',
  /** Medical imaging (X-ray, MRI, etc.) */
  MEDICAL_IMAGE: 'MEDICAL_IMAGE',
  /** Health insurance card */
  INSURANCE_CARD: 'INSURANCE_CARD',
  /** ID card or passport */
  ID_CARD: 'ID_CARD',
  /** Vaccination record */
  VACCINATION: 'VACCINATION',
  /** Other medical documents */
  OTHER: 'OTHER',
} as const;

export type DocumentType = typeof DocumentType[keyof typeof DocumentType];

/**
 * Document processing status
 */
export const DocumentStatus = {
  /** Document is uploaded but not processed */
  UPLOADED: 'UPLOADED',
  /** Document is being processed */
  PROCESSING: 'PROCESSING',
  /** Document processed successfully */
  PROCESSED: 'PROCESSED',
  /** Document processing failed */
  FAILED: 'FAILED',
} as const;

export type DocumentStatus = typeof DocumentStatus[keyof typeof DocumentStatus];

/**
 * Subscription/Pricing tier
 */
export const SubscriptionTier = {
  /** Free tier with limited features */
  FREE: 'FREE',
  /** Basic paid tier */
  BASIC: 'BASIC',
  /** Premium tier with all features */
  PREMIUM: 'PREMIUM',
  /** Enterprise tier */
  ENTERPRISE: 'ENTERPRISE',
} as const;

export type SubscriptionTier = typeof SubscriptionTier[keyof typeof SubscriptionTier];

/**
 * Gender options
 */
export const Gender = {
  MALE: 'MALE',
  FEMALE: 'FEMALE',
  OTHER: 'OTHER',
  PREFER_NOT_TO_SAY: 'PREFER_NOT_TO_SAY',
} as const;

export type Gender = typeof Gender[keyof typeof Gender];

/**
 * Blood type
 */
export const BloodType = {
  A_POSITIVE: 'A_POSITIVE',
  A_NEGATIVE: 'A_NEGATIVE',
  B_POSITIVE: 'B_POSITIVE',
  B_NEGATIVE: 'B_NEGATIVE',
  AB_POSITIVE: 'AB_POSITIVE',
  AB_NEGATIVE: 'AB_NEGATIVE',
  O_POSITIVE: 'O_POSITIVE',
  O_NEGATIVE: 'O_NEGATIVE',
  UNKNOWN: 'UNKNOWN',
} as const;

export type BloodType = typeof BloodType[keyof typeof BloodType];

/**
 * Relationship type (for family profiles)
 */
export const RelationshipType = {
  /** Self */
  SELF: 'SELF',
  /** Spouse/Partner */
  SPOUSE: 'SPOUSE',
  /** Child */
  CHILD: 'CHILD',
  /** Parent */
  PARENT: 'PARENT',
  /** Sibling */
  SIBLING: 'SIBLING',
  /** Grandparent */
  GRANDPARENT: 'GRANDPARENT',
  /** Other family member */
  OTHER: 'OTHER',
} as const;

export type RelationshipType = typeof RelationshipType[keyof typeof RelationshipType];

/**
 * All status exports
 */
export const Status = {
  UserStatus,
  ProfileStatus,
  HealthRecordStatus,
  AnalysisStatus,
  DocumentType,
  DocumentStatus,
  SubscriptionTier,
  Gender,
  BloodType,
  RelationshipType,
} as const;

export type Status = typeof Status;
