/* tslint:disable */
/* eslint-disable */
/**
 * 
 * @export
 * @interface AdminLoginRequest
 */
export interface AdminLoginRequest {
    /**
     * 
     * @type {string}
     * @memberof AdminLoginRequest
     */
    email: string;
    /**
     * 
     * @type {string}
     * @memberof AdminLoginRequest
     */
    password: string;
    /**
     * 
     * @type {string}
     * @memberof AdminLoginRequest
     */
    totpCode?: string;
}
/**
 * 
 * @export
 * @interface AdminReferenceImportConfirmRequest
 */
export interface AdminReferenceImportConfirmRequest {
    /**
     * 
     * @type {string}
     * @memberof AdminReferenceImportConfirmRequest
     */
    importId: string;
}
/**
 * 
 * @export
 * @interface AdminReferenceMetricRequest
 */
export interface AdminReferenceMetricRequest {
    /**
     * 
     * @type {string}
     * @memberof AdminReferenceMetricRequest
     */
    name: string;
    /**
     * 
     * @type {string}
     * @memberof AdminReferenceMetricRequest
     */
    displayNameVi: string;
    /**
     * 
     * @type {string}
     * @memberof AdminReferenceMetricRequest
     */
    unit: string;
    /**
     * 
     * @type {Array<AdminReferenceRangeRequest>}
     * @memberof AdminReferenceMetricRequest
     */
    ranges: Array<AdminReferenceRangeRequest>;
}
/**
 * 
 * @export
 * @interface AdminReferenceRangeRequest
 */
export interface AdminReferenceRangeRequest {
    /**
     * 
     * @type {number}
     * @memberof AdminReferenceRangeRequest
     */
    minValue: number;
    /**
     * 
     * @type {number}
     * @memberof AdminReferenceRangeRequest
     */
    maxValue: number;
    /**
     * 
     * @type {number}
     * @memberof AdminReferenceRangeRequest
     */
    attentionMin: number;
    /**
     * 
     * @type {number}
     * @memberof AdminReferenceRangeRequest
     */
    attentionMax: number;
    /**
     * 
     * @type {string}
     * @memberof AdminReferenceRangeRequest
     */
    gender?: string;
    /**
     * 
     * @type {number}
     * @memberof AdminReferenceRangeRequest
     */
    minAge?: number;
    /**
     * 
     * @type {number}
     * @memberof AdminReferenceRangeRequest
     */
    maxAge?: number;
}
/**
 * 
 * @export
 * @interface AdminRejectChangeSetRequest
 */
export interface AdminRejectChangeSetRequest {
    /**
     * 
     * @type {string}
     * @memberof AdminRejectChangeSetRequest
     */
    reason: string;
}
/**
 * 
 * @export
 * @interface AdminTotpVerifyRequest
 */
export interface AdminTotpVerifyRequest {
    /**
     * 
     * @type {string}
     * @memberof AdminTotpVerifyRequest
     */
    code: string;
}
/**
 * 
 * @export
 * @interface AuditLogEntryDto
 */
export interface AuditLogEntryDto {
    /**
     * 
     * @type {string}
     * @memberof AuditLogEntryDto
     */
    id?: string;
    /**
     * 
     * @type {string}
     * @memberof AuditLogEntryDto
     */
    actorEmail?: string;
    /**
     * 
     * @type {string}
     * @memberof AuditLogEntryDto
     */
    action?: string;
    /**
     * 
     * @type {string}
     * @memberof AuditLogEntryDto
     */
    resourceType?: string;
    /**
     * 
     * @type {string}
     * @memberof AuditLogEntryDto
     */
    resourceId?: string;
    /**
     * 
     * @type {string}
     * @memberof AuditLogEntryDto
     */
    entityLabel?: string;
    /**
     * 
     * @type {string}
     * @memberof AuditLogEntryDto
     */
    detailSummary?: string;
    /**
     * 
     * @type {string}
     * @memberof AuditLogEntryDto
     */
    outcome?: string;
    /**
     * 
     * @type {string}
     * @memberof AuditLogEntryDto
     */
    oldValueJson?: string;
    /**
     * 
     * @type {string}
     * @memberof AuditLogEntryDto
     */
    newValueJson?: string;
    /**
     * 
     * @type {string}
     * @memberof AuditLogEntryDto
     */
    metadataJson?: string;
    /**
     * 
     * @type {string}
     * @memberof AuditLogEntryDto
     */
    correlationId?: string;
    /**
     * 
     * @type {string}
     * @memberof AuditLogEntryDto
     */
    requestId?: string;
    /**
     * 
     * @type {string}
     * @memberof AuditLogEntryDto
     */
    traceId?: string;
    /**
     * 
     * @type {string}
     * @memberof AuditLogEntryDto
     */
    ipAddress?: string;
    /**
     * 
     * @type {string}
     * @memberof AuditLogEntryDto
     */
    createdAt?: string;
}
/**
 * 
 * @export
 * @interface AuditLogPageDto
 */
export interface AuditLogPageDto {
    /**
     * 
     * @type {Array<AuditLogEntryDto>}
     * @memberof AuditLogPageDto
     */
    content?: Array<AuditLogEntryDto>;
    /**
     * 
     * @type {number}
     * @memberof AuditLogPageDto
     */
    totalElements?: number;
    /**
     * 
     * @type {number}
     * @memberof AuditLogPageDto
     */
    page?: number;
    /**
     * 
     * @type {number}
     * @memberof AuditLogPageDto
     */
    limit?: number;
}
/**
 * 
 * @export
 * @interface BoundingBox
 */
export interface BoundingBox {
    /**
     * 
     * @type {number}
     * @memberof BoundingBox
     */
    left?: number;
    /**
     * 
     * @type {number}
     * @memberof BoundingBox
     */
    top?: number;
    /**
     * 
     * @type {number}
     * @memberof BoundingBox
     */
    width?: number;
    /**
     * 
     * @type {number}
     * @memberof BoundingBox
     */
    height?: number;
}
/**
 * 
 * @export
 * @interface ChangePasswordRequest
 */
export interface ChangePasswordRequest {
    /**
     * 
     * @type {string}
     * @memberof ChangePasswordRequest
     */
    currentPassword: string;
    /**
     * 
     * @type {string}
     * @memberof ChangePasswordRequest
     */
    newPassword: string;
}
/**
 * 
 * @export
 * @interface ConfirmRecordRequest
 */
export interface ConfirmRecordRequest {
    /**
     * 
     * @type {string}
     * @memberof ConfirmRecordRequest
     */
    examDate?: string;
    /**
     * 
     * @type {string}
     * @memberof ConfirmRecordRequest
     */
    recordType?: string;
    /**
     * 
     * @type {string}
     * @memberof ConfirmRecordRequest
     */
    hospitalName?: string;
    /**
     * 
     * @type {string}
     * @memberof ConfirmRecordRequest
     */
    diagnosis?: string;
    /**
     * 
     * @type {string}
     * @memberof ConfirmRecordRequest
     */
    analyzerModel?: string;
    /**
     * 
     * @type {string}
     * @memberof ConfirmRecordRequest
     */
    testMethod?: string;
    /**
     * 
     * @type {string}
     * @memberof ConfirmRecordRequest
     */
    labSite?: string;
    /**
     * 
     * @type {boolean}
     * @memberof ConfirmRecordRequest
     */
    keepPartial?: boolean;
    /**
     * 
     * @type {Array<MetricDto>}
     * @memberof ConfirmRecordRequest
     */
    metrics?: Array<MetricDto>;
}
/**
 * 
 * @export
 * @interface ConsentRequest
 */
export interface ConsentRequest {
    /**
     * 
     * @type {string}
     * @memberof ConsentRequest
     */
    version: string;
    /**
     * 
     * @type {boolean}
     * @memberof ConsentRequest
     */
    accepted: boolean;
}
/**
 * 
 * @export
 * @interface ConsentResponse
 */
export interface ConsentResponse {
    /**
     * 
     * @type {boolean}
     * @memberof ConsentResponse
     */
    consentGiven?: boolean;
    /**
     * 
     * @type {string}
     * @memberof ConsentResponse
     */
    consentVersion?: string;
    /**
     * 
     * @type {string}
     * @memberof ConsentResponse
     */
    consentedAt?: string;
}
/**
 * 
 * @export
 * @interface ConsentVersionResponse
 */
export interface ConsentVersionResponse {
    /**
     * 
     * @type {string}
     * @memberof ConsentVersionResponse
     */
    version?: string;
}
/**
 * 
 * @export
 * @interface CreateProfileRequest
 */
export interface CreateProfileRequest {
    /**
     * 
     * @type {string}
     * @memberof CreateProfileRequest
     */
    displayName: string;
    /**
     * 
     * @type {string}
     * @memberof CreateProfileRequest
     */
    birthDate?: string;
    /**
     * 
     * @type {string}
     * @memberof CreateProfileRequest
     */
    gender?: string;
    /**
     * 
     * @type {string}
     * @memberof CreateProfileRequest
     */
    notes?: string;
    /**
     * 
     * @type {string}
     * @memberof CreateProfileRequest
     */
    chronicConditions?: string;
    /**
     * 
     * @type {string}
     * @memberof CreateProfileRequest
     */
    currentMedications?: string;
    /**
     * 
     * @type {string}
     * @memberof CreateProfileRequest
     */
    allergies?: string;
}
/**
 * 
 * @export
 * @interface CreateUploadUrlRequest
 */
export interface CreateUploadUrlRequest {
    /**
     * 
     * @type {string}
     * @memberof CreateUploadUrlRequest
     */
    profileId: string;
    /**
     * 
     * @type {string}
     * @memberof CreateUploadUrlRequest
     */
    fileType: string;
    /**
     * 
     * @type {string}
     * @memberof CreateUploadUrlRequest
     */
    retryRecordId?: string;
}
/**
 * 
 * @export
 * @interface CsrfToken
 */
export interface CsrfToken {
    /**
     * 
     * @type {string}
     * @memberof CsrfToken
     */
    headerName?: string;
    /**
     * 
     * @type {string}
     * @memberof CsrfToken
     */
    token?: string;
    /**
     * 
     * @type {string}
     * @memberof CsrfToken
     */
    parameterName?: string;
}
/**
 * 
 * @export
 * @interface DeleteAccountRequest
 */
export interface DeleteAccountRequest {
    /**
     * 
     * @type {string}
     * @memberof DeleteAccountRequest
     */
    password: string;
}
/**
 * 
 * @export
 * @interface FollowUpReminderRequest
 */
export interface FollowUpReminderRequest {
    /**
     * 
     * @type {string}
     * @memberof FollowUpReminderRequest
     */
    reminderDate: string;
    /**
     * 
     * @type {string}
     * @memberof FollowUpReminderRequest
     */
    reminderType: string;
    /**
     * 
     * @type {string}
     * @memberof FollowUpReminderRequest
     */
    note?: string;
}
/**
 * 
 * @export
 * @interface ForgotPasswordRequest
 */
export interface ForgotPasswordRequest {
    /**
     * 
     * @type {string}
     * @memberof ForgotPasswordRequest
     */
    email: string;
}
/**
 * 
 * @export
 * @interface InviteHealthRecordRequest
 */
export interface InviteHealthRecordRequest {
    /**
     * 
     * @type {string}
     * @memberof InviteHealthRecordRequest
     */
    email: string;
    /**
     * 
     * @type {string}
     * @memberof InviteHealthRecordRequest
     */
    accessLevel?: string;
}
/**
 * 
 * @export
 * @interface InviteProfileMemberRequest
 */
export interface InviteProfileMemberRequest {
    /**
     * 
     * @type {string}
     * @memberof InviteProfileMemberRequest
     */
    email: string;
    /**
     * 
     * @type {string}
     * @memberof InviteProfileMemberRequest
     */
    accessLevel?: string;
}
/**
 * 
 * @export
 * @interface LoginRequest
 */
export interface LoginRequest {
    /**
     * 
     * @type {string}
     * @memberof LoginRequest
     */
    email: string;
    /**
     * 
     * @type {string}
     * @memberof LoginRequest
     */
    password: string;
}
/**
 * 
 * @export
 * @interface MarkNotificationInboxReadRequest
 */
export interface MarkNotificationInboxReadRequest {
    /**
     * 
     * @type {string}
     * @memberof MarkNotificationInboxReadRequest
     */
    itemId: string;
}
/**
 * 
 * @export
 * @interface MetricDto
 */
export interface MetricDto {
    /**
     * 
     * @type {string}
     * @memberof MetricDto
     */
    name?: string;
    /**
     * 
     * @type {string}
     * @memberof MetricDto
     */
    value?: string;
    /**
     * 
     * @type {string}
     * @memberof MetricDto
     */
    unit?: string;
    /**
     * 
     * @type {string}
     * @memberof MetricDto
     */
    rawName?: string;
    /**
     * 
     * @type {string}
     * @memberof MetricDto
     */
    rawValue?: string;
    /**
     * 
     * @type {string}
     * @memberof MetricDto
     */
    rawUnit?: string;
    /**
     * 
     * @type {string}
     * @memberof MetricDto
     */
    normalizedName?: string;
    /**
     * 
     * @type {string}
     * @memberof MetricDto
     */
    normalizedValue?: string;
    /**
     * 
     * @type {string}
     * @memberof MetricDto
     */
    normalizedUnit?: string;
    /**
     * 
     * @type {number}
     * @memberof MetricDto
     */
    confidence?: number;
    /**
     * 
     * @type {string}
     * @memberof MetricDto
     */
    source?: string;
    /**
     * 
     * @type {string}
     * @memberof MetricDto
     */
    confidenceLevel?: string;
    /**
     * 
     * @type {string}
     * @memberof MetricDto
     */
    displayNameVi?: string;
    /**
     * 
     * @type {string}
     * @memberof MetricDto
     */
    status?: string;
    /**
     * 
     * @type {string}
     * @memberof MetricDto
     */
    statusSource?: string;
    /**
     * 
     * @type {string}
     * @memberof MetricDto
     */
    interpretation?: string;
    /**
     * 
     * @type {string}
     * @memberof MetricDto
     */
    interpretationSource?: string;
    /**
     * 
     * @type {boolean}
     * @memberof MetricDto
     */
    critical?: boolean;
    /**
     * 
     * @type {ReferenceRangeDto}
     * @memberof MetricDto
     */
    referenceRange?: ReferenceRangeDto;
    /**
     * 
     * @type {string}
     * @memberof MetricDto
     */
    referenceRangeSource?: string;
    /**
     * 
     * @type {RangeContextDto}
     * @memberof MetricDto
     */
    rangeContext?: RangeContextDto;
    /**
     * 
     * @type {string}
     * @memberof MetricDto
     */
    explanation?: string;
}
/**
 * 
 * @export
 * @interface MetricNameDto
 */
export interface MetricNameDto {
    /**
     * 
     * @type {string}
     * @memberof MetricNameDto
     */
    name?: string;
    /**
     * 
     * @type {string}
     * @memberof MetricNameDto
     */
    displayNameVi?: string;
    /**
     * 
     * @type {string}
     * @memberof MetricNameDto
     */
    unit?: string;
}
/**
 * 
 * @export
 * @interface OcrDiagnostic
 */
export interface OcrDiagnostic {
    /**
     * 
     * @type {string}
     * @memberof OcrDiagnostic
     */
    code?: string;
    /**
     * 
     * @type {string}
     * @memberof OcrDiagnostic
     */
    category?: string;
    /**
     * 
     * @type {string}
     * @memberof OcrDiagnostic
     */
    provider?: string;
    /**
     * 
     * @type {string}
     * @memberof OcrDiagnostic
     */
    message?: string;
    /**
     * 
     * @type {{ [key: string]: string; }}
     * @memberof OcrDiagnostic
     */
    context?: { [key: string]: string; };
}
/**
 * 
 * @export
 * @interface OcrPage
 */
export interface OcrPage {
    /**
     * 
     * @type {number}
     * @memberof OcrPage
     */
    pageNumber?: number;
    /**
     * 
     * @type {string}
     * @memberof OcrPage
     */
    text?: string;
    /**
     * 
     * @type {number}
     * @memberof OcrPage
     */
    confidence?: number;
    /**
     * 
     * @type {BoundingBox}
     * @memberof OcrPage
     */
    boundingBox?: BoundingBox;
}
/**
 * 
 * @export
 * @interface OcrResult
 */
export interface OcrResult {
    /**
     * 
     * @type {string}
     * @memberof OcrResult
     */
    provider?: string;
    /**
     * 
     * @type {string}
     * @memberof OcrResult
     */
    modelVersion?: string;
    /**
     * 
     * @type {string}
     * @memberof OcrResult
     */
    mimeType?: string;
    /**
     * 
     * @type {Array<OcrPage>}
     * @memberof OcrResult
     */
    pages?: Array<OcrPage>;
    /**
     * 
     * @type {Array<OcrSegment>}
     * @memberof OcrResult
     */
    blocks?: Array<OcrSegment>;
    /**
     * 
     * @type {Array<OcrSegment>}
     * @memberof OcrResult
     */
    lines?: Array<OcrSegment>;
    /**
     * 
     * @type {string}
     * @memberof OcrResult
     */
    text?: string;
    /**
     * 
     * @type {number}
     * @memberof OcrResult
     */
    confidence?: number;
    /**
     * 
     * @type {Array<OcrDiagnostic>}
     * @memberof OcrResult
     */
    diagnostics?: Array<OcrDiagnostic>;
    /**
     * 
     * @type {string}
     * @memberof OcrResult
     */
    providerRequestId?: string;
    /**
     * 
     * @type {number}
     * @memberof OcrResult
     */
    latencyMs?: number;
    /**
     * 
     * @type {string}
     * @memberof OcrResult
     */
    retentionMode?: string;
    /**
     * 
     * @type {string}
     * @memberof OcrResult
     */
    language?: string;
    /**
     * 
     * @type {number}
     * @memberof OcrResult
     */
    processingTimeMs?: number;
    /**
     * 
     * @type {string}
     * @memberof OcrResult
     */
    source?: string;
}
/**
 * 
 * @export
 * @interface OcrSegment
 */
export interface OcrSegment {
    /**
     * 
     * @type {number}
     * @memberof OcrSegment
     */
    pageNumber?: number;
    /**
     * 
     * @type {string}
     * @memberof OcrSegment
     */
    text?: string;
    /**
     * 
     * @type {number}
     * @memberof OcrSegment
     */
    confidence?: number;
    /**
     * 
     * @type {BoundingBox}
     * @memberof OcrSegment
     */
    boundingBox?: BoundingBox;
    /**
     * 
     * @type {string}
     * @memberof OcrSegment
     */
    kind?: string;
}
/**
 * 
 * @export
 * @interface OnlineRagCitationEntryDto
 */
export interface OnlineRagCitationEntryDto {
    /**
     * 
     * @type {string}
     * @memberof OnlineRagCitationEntryDto
     */
    id?: string;
    /**
     * 
     * @type {string}
     * @memberof OnlineRagCitationEntryDto
     */
    healthRecordId?: string;
    /**
     * 
     * @type {string}
     * @memberof OnlineRagCitationEntryDto
     */
    metricName?: string;
    /**
     * 
     * @type {string}
     * @memberof OnlineRagCitationEntryDto
     */
    answerHash?: string;
    /**
     * 
     * @type {string}
     * @memberof OnlineRagCitationEntryDto
     */
    sourceSnapshotId?: string;
    /**
     * 
     * @type {string}
     * @memberof OnlineRagCitationEntryDto
     */
    sourceUrl?: string;
    /**
     * 
     * @type {string}
     * @memberof OnlineRagCitationEntryDto
     */
    publisher?: string;
    /**
     * 
     * @type {string}
     * @memberof OnlineRagCitationEntryDto
     */
    retrievedAt?: string;
    /**
     * 
     * @type {string}
     * @memberof OnlineRagCitationEntryDto
     */
    snapshotHash?: string;
    /**
     * 
     * @type {string}
     * @memberof OnlineRagCitationEntryDto
     */
    reviewStatus?: string;
    /**
     * 
     * @type {boolean}
     * @memberof OnlineRagCitationEntryDto
     */
    excluded?: boolean;
    /**
     * 
     * @type {boolean}
     * @memberof OnlineRagCitationEntryDto
     */
    cacheHit?: boolean;
    /**
     * 
     * @type {boolean}
     * @memberof OnlineRagCitationEntryDto
     */
    usableForAi?: boolean;
    /**
     * 
     * @type {boolean}
     * @memberof OnlineRagCitationEntryDto
     */
    reviewRequired?: boolean;
    /**
     * 
     * @type {boolean}
     * @memberof OnlineRagCitationEntryDto
     */
    rejected?: boolean;
    /**
     * 
     * @type {boolean}
     * @memberof OnlineRagCitationEntryDto
     */
    stale?: boolean;
    /**
     * 
     * @type {string}
     * @memberof OnlineRagCitationEntryDto
     */
    retrievalSource?: string;
    /**
     * 
     * @type {string}
     * @memberof OnlineRagCitationEntryDto
     */
    promptVersion?: string;
    /**
     * 
     * @type {string}
     * @memberof OnlineRagCitationEntryDto
     */
    modelVersion?: string;
    /**
     * 
     * @type {string}
     * @memberof OnlineRagCitationEntryDto
     */
    createdAt?: string;
}
/**
 * 
 * @export
 * @interface OnlineRagCitationPageDto
 */
export interface OnlineRagCitationPageDto {
    /**
     * 
     * @type {Array<OnlineRagCitationEntryDto>}
     * @memberof OnlineRagCitationPageDto
     */
    content?: Array<OnlineRagCitationEntryDto>;
    /**
     * 
     * @type {number}
     * @memberof OnlineRagCitationPageDto
     */
    totalElements?: number;
    /**
     * 
     * @type {number}
     * @memberof OnlineRagCitationPageDto
     */
    page?: number;
    /**
     * 
     * @type {number}
     * @memberof OnlineRagCitationPageDto
     */
    limit?: number;
}
/**
 * 
 * @export
 * @interface RangeContextDto
 */
export interface RangeContextDto {
    /**
     * 
     * @type {string}
     * @memberof RangeContextDto
     */
    gender?: string;
    /**
     * 
     * @type {string}
     * @memberof RangeContextDto
     */
    ageRange?: string;
}
/**
 * 
 * @export
 * @interface ReferenceDataSearchResult
 */
export interface ReferenceDataSearchResult {
    /**
     * 
     * @type {string}
     * @memberof ReferenceDataSearchResult
     */
    id?: string;
    /**
     * 
     * @type {string}
     * @memberof ReferenceDataSearchResult
     */
    text?: string;
    /**
     * 
     * @type {{ [key: string]: any; }}
     * @memberof ReferenceDataSearchResult
     */
    metadata?: { [key: string]: any; };
}
/**
 * 
 * @export
 * @interface ReferenceMetricAdminDto
 */
export interface ReferenceMetricAdminDto {
    /**
     * 
     * @type {string}
     * @memberof ReferenceMetricAdminDto
     */
    id?: string;
    /**
     * 
     * @type {string}
     * @memberof ReferenceMetricAdminDto
     */
    name?: string;
    /**
     * 
     * @type {string}
     * @memberof ReferenceMetricAdminDto
     */
    displayNameVi?: string;
    /**
     * 
     * @type {string}
     * @memberof ReferenceMetricAdminDto
     */
    unit?: string;
}
/**
 * 
 * @export
 * @interface ReferenceRangeDto
 */
export interface ReferenceRangeDto {
    /**
     * 
     * @type {number}
     * @memberof ReferenceRangeDto
     */
    min?: number;
    /**
     * 
     * @type {number}
     * @memberof ReferenceRangeDto
     */
    max?: number;
    /**
     * 
     * @type {number}
     * @memberof ReferenceRangeDto
     */
    attentionMin?: number;
    /**
     * 
     * @type {number}
     * @memberof ReferenceRangeDto
     */
    attentionMax?: number;
    /**
     * 
     * @type {string}
     * @memberof ReferenceRangeDto
     */
    unit?: string;
}
/**
 * 
 * @export
 * @interface RegisterRequest
 */
export interface RegisterRequest {
    /**
     * 
     * @type {string}
     * @memberof RegisterRequest
     */
    fullName: string;
    /**
     * 
     * @type {string}
     * @memberof RegisterRequest
     */
    email: string;
    /**
     * 
     * @type {string}
     * @memberof RegisterRequest
     */
    birthDate: string;
    /**
     * 
     * @type {string}
     * @memberof RegisterRequest
     */
    password: string;
}
/**
 * 
 * @export
 * @interface ResetPasswordRequest
 */
export interface ResetPasswordRequest {
    /**
     * 
     * @type {string}
     * @memberof ResetPasswordRequest
     */
    token: string;
    /**
     * 
     * @type {string}
     * @memberof ResetPasswordRequest
     */
    newPassword: string;
}
/**
 * 
 * @export
 * @interface UpdateHealthContextRequest
 */
export interface UpdateHealthContextRequest {
    /**
     * 
     * @type {string}
     * @memberof UpdateHealthContextRequest
     */
    chronicConditions?: string;
    /**
     * 
     * @type {string}
     * @memberof UpdateHealthContextRequest
     */
    currentMedications?: string;
    /**
     * 
     * @type {string}
     * @memberof UpdateHealthContextRequest
     */
    allergies?: string;
}
/**
 * 
 * @export
 * @interface UpdateMetricsRequest
 */
export interface UpdateMetricsRequest {
    /**
     * 
     * @type {Array<MetricDto>}
     * @memberof UpdateMetricsRequest
     */
    metrics: Array<MetricDto>;
}
/**
 * 
 * @export
 * @interface UpdateNotificationPreferencesRequest
 */
export interface UpdateNotificationPreferencesRequest {
    /**
     * 
     * @type {boolean}
     * @memberof UpdateNotificationPreferencesRequest
     */
    shareInvite: boolean;
    /**
     * 
     * @type {boolean}
     * @memberof UpdateNotificationPreferencesRequest
     */
    shareAccepted: boolean;
    /**
     * 
     * @type {boolean}
     * @memberof UpdateNotificationPreferencesRequest
     */
    followUpReminder: boolean;
    /**
     * 
     * @type {boolean}
     * @memberof UpdateNotificationPreferencesRequest
     */
    security?: boolean;
}
/**
 * 
 * @export
 * @interface UpdateProfileRequest
 */
export interface UpdateProfileRequest {
    /**
     * 
     * @type {string}
     * @memberof UpdateProfileRequest
     */
    displayName: string;
    /**
     * 
     * @type {string}
     * @memberof UpdateProfileRequest
     */
    birthDate?: string;
    /**
     * 
     * @type {string}
     * @memberof UpdateProfileRequest
     */
    gender?: string;
    /**
     * 
     * @type {string}
     * @memberof UpdateProfileRequest
     */
    notes?: string;
    /**
     * 
     * @type {string}
     * @memberof UpdateProfileRequest
     */
    chronicConditions?: string;
    /**
     * 
     * @type {string}
     * @memberof UpdateProfileRequest
     */
    currentMedications?: string;
    /**
     * 
     * @type {string}
     * @memberof UpdateProfileRequest
     */
    allergies?: string;
}
/**
 * 
 * @export
 * @interface UpdateReferenceMetricDisplayRequest
 */
export interface UpdateReferenceMetricDisplayRequest {
    /**
     * 
     * @type {string}
     * @memberof UpdateReferenceMetricDisplayRequest
     */
    displayNameVi: string;
}
/**
 * 
 * @export
 * @interface UpdateUserRequest
 */
export interface UpdateUserRequest {
    /**
     * 
     * @type {string}
     * @memberof UpdateUserRequest
     */
    fullName: string;
    /**
     * 
     * @type {string}
     * @memberof UpdateUserRequest
     */
    birthDate?: string;
    /**
     * 
     * @type {string}
     * @memberof UpdateUserRequest
     */
    gender?: string;
    /**
     * 
     * @type {string}
     * @memberof UpdateUserRequest
     */
    personalDescription?: string;
    /**
     * 
     * @type {string}
     * @memberof UpdateUserRequest
     */
    personalNotes?: string;
}
/**
 * 
 * @export
 * @interface UploadAvatarRequest
 */
export interface UploadAvatarRequest {
    /**
     * 
     * @type {Blob}
     * @memberof UploadAvatarRequest
     */
    file: Blob;
}
/**
 * 
 * @export
 * @interface UserAuthTotpVerifyRequest
 */
export interface UserAuthTotpVerifyRequest {
    /**
     * 
     * @type {string}
     * @memberof UserAuthTotpVerifyRequest
     */
    preAuthToken: string;
    /**
     * 
     * @type {string}
     * @memberof UserAuthTotpVerifyRequest
     */
    code: string;
}
/**
 * 
 * @export
 * @interface UserTotpDisableRequest
 */
export interface UserTotpDisableRequest {
    /**
     * 
     * @type {string}
     * @memberof UserTotpDisableRequest
     */
    password: string;
    /**
     * 
     * @type {string}
     * @memberof UserTotpDisableRequest
     */
    code: string;
}
/**
 * 
 * @export
 * @interface UserTotpVerifyRequest
 */
export interface UserTotpVerifyRequest {
    /**
     * 
     * @type {string}
     * @memberof UserTotpVerifyRequest
     */
    code: string;
}
/**
 * 
 * @export
 * @interface VerifyEmailRequest
 */
export interface VerifyEmailRequest {
    /**
     * 
     * @type {string}
     * @memberof VerifyEmailRequest
     */
    token: string;
}
