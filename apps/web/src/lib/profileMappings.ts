export type Profile = {
  id: string;
  displayName: string;
  birthDate?: string;
  gender?: string;
  notes?: string;
  chronicConditions?: string;
  currentMedications?: string;
  allergies?: string;
  isDefault: boolean;
  createdAt: string;
  updatedAt: string;
  lastRecordAt?: string;
  latestStatus?: string;
};

export type IncomingInvitation = {
  id: string;
  profileId: string;
  profileDisplayName: string;
  inviterName: string;
  expiresAt: string;
  createdAt: string;
  accessLevel: SharedAccessLevel;
  acceptPath: string;
};

export type SharedAccessLevel = "view" | "edit" | "unknown";

export type SharedProfile = {
  profileId: string;
  displayName: string;
  accessLevel: SharedAccessLevel;
  latestStatus?: string;
  lastUpdated?: string;
  lastRecordAt?: string;
  birthDate?: string;
  gender?: string;
  notes?: string;
  chronicConditions?: string;
  currentMedications?: string;
  allergies?: string;
};

export type HistoryItem = {
  id: string;
  status?: string | null;
  recordStatus?: string | null;
  verificationStatus?: string | null;
  examDate: string | null;
  testType: string;
  overallStatus: "normal" | "attention" | "abnormal" | string;
  abnormalCount: number;
  hospitalName: string | null;
  sourceType: string | null;
  createdAt: string;
  canDelete?: boolean;
};

export type HistoryPagination = {
  page: number;
  limit: number;
  total: number;
  totalPages: number;
};

export type HistoryPageResponse = {
  data: HistoryItem[];
  pagination: HistoryPagination;
};

const DEFAULT_PAGINATION: HistoryPagination = {
  page: 0,
  limit: 20,
  total: 0,
  totalPages: 1,
};

function isRecord(value: unknown): value is Record<string, unknown> {
  return value !== null && typeof value === "object" && !Array.isArray(value);
}

function asString(value: unknown): string | undefined {
  if (typeof value !== "string") {
    return undefined;
  }

  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : undefined;
}

function asNullableString(value: unknown): string | null {
  if (typeof value !== "string") {
    return null;
  }

  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : null;
}

function asOptionalString(value: unknown): string | undefined {
  return typeof value === "string" ? value.trim() : undefined;
}

function asBoolean(value: unknown): boolean | undefined {
  return typeof value === "boolean" ? value : undefined;
}

function asNumber(value: unknown): number | undefined {
  if (typeof value === "number" && Number.isFinite(value)) {
    return value;
  }

  if (typeof value === "string" && value.trim().length > 0) {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : undefined;
  }

  return undefined;
}

function normalizeAccessLevel(value: unknown): SharedAccessLevel {
  if (typeof value !== "string") {
    return "unknown";
  }

  const normalized = value.trim().toLowerCase();
  if (normalized === "edit" || normalized === "view") {
    return normalized;
  }

  return "unknown";
}

export function mapProfilesResponse(value: unknown): Profile[] {
  if (!Array.isArray(value)) {
    return [];
  }

  return value.flatMap((item) => {
    if (!isRecord(item)) {
      return [];
    }

    const id = asString(item.id);
    const displayName = asString(item.displayName);
    const isDefault = asBoolean(item.isDefault);
    const createdAt = asString(item.createdAt);
    const updatedAt = asString(item.updatedAt);

    if (!id || !displayName || typeof isDefault !== "boolean" || !createdAt || !updatedAt) {
      return [];
    }

    return [
      {
        id,
        displayName,
        isDefault,
        createdAt,
        updatedAt,
        birthDate: asOptionalString(item.birthDate),
        gender: asOptionalString(item.gender),
        notes: asOptionalString(item.notes),
        chronicConditions: asOptionalString(item.chronicConditions),
        currentMedications: asOptionalString(item.currentMedications),
        allergies: asOptionalString(item.allergies),
        lastRecordAt: asOptionalString(item.lastRecordAt),
        latestStatus: asOptionalString(item.latestStatus),
      },
    ];
  });
}

export function mapIncomingInvitationsResponse(value: unknown): IncomingInvitation[] {
  if (!Array.isArray(value)) {
    return [];
  }

  return value.flatMap((item) => {
    if (!isRecord(item)) {
      return [];
    }

    const id = asString(item.id);
    const profileId = asString(item.profileId);
    const profileDisplayName = asString(item.profileDisplayName);
    const inviterName = asString(item.inviterName);
    const expiresAt = asString(item.expiresAt);
    const createdAt = asString(item.createdAt);
    const acceptPath = asString(item.acceptPath);

    if (!id || !profileId || !profileDisplayName || !inviterName || !expiresAt || !createdAt || !acceptPath) {
      return [];
    }

    return [
      {
        id,
        profileId,
        profileDisplayName,
        inviterName,
        expiresAt,
        createdAt,
        acceptPath,
        accessLevel: normalizeAccessLevel(item.accessLevel),
      },
    ];
  });
}

export function mapSharedProfilesResponse(value: unknown): SharedProfile[] {
  if (!Array.isArray(value)) {
    return [];
  }

  return value.flatMap((item) => {
    if (!isRecord(item)) {
      return [];
    }

    const profileId = asString(item.profileId);
    const displayName = asString(item.displayName);

    if (!profileId || !displayName) {
      return [];
    }

    return [
      {
        profileId,
        displayName,
        accessLevel: normalizeAccessLevel(item.accessLevel),
        latestStatus: asOptionalString(item.latestStatus),
        lastUpdated: asOptionalString(item.lastUpdated),
        lastRecordAt: asOptionalString(item.lastRecordAt),
        birthDate: asOptionalString(item.birthDate),
        gender: asOptionalString(item.gender),
        notes: asOptionalString(item.notes),
        chronicConditions: asOptionalString(item.chronicConditions),
        currentMedications: asOptionalString(item.currentMedications),
        allergies: asOptionalString(item.allergies),
      },
    ];
  });
}

export function mapHistoryPageResponse(value: unknown): HistoryPageResponse {
  if (!isRecord(value)) {
    return { data: [], pagination: DEFAULT_PAGINATION };
  }

  return {
    data: mapHistoryItems(value.data),
    pagination: mapHistoryPagination(value.pagination),
  };
}

function mapHistoryItems(value: unknown): HistoryItem[] {
  if (!Array.isArray(value)) {
    return [];
  }

  return value.flatMap((item) => {
    if (!isRecord(item)) {
      return [];
    }

    const id = asString(item.id);
    if (!id) {
      return [];
    }

    return [
      {
        id,
        status: asNullableString(item.status),
        recordStatus: asNullableString(item.recordStatus),
        verificationStatus: asNullableString(item.verificationStatus),
        examDate: asNullableString(item.examDate),
        testType: asOptionalString(item.testType) ?? "",
        overallStatus: asOptionalString(item.overallStatus) ?? "unverified",
        abnormalCount: asNumber(item.abnormalCount) ?? 0,
        hospitalName: asNullableString(item.hospitalName),
        sourceType: asNullableString(item.sourceType),
        createdAt: asOptionalString(item.createdAt) ?? "",
        canDelete: asBoolean(item.canDelete),
      },
    ];
  });
}

function mapHistoryPagination(value: unknown): HistoryPagination {
  if (!isRecord(value)) {
    return DEFAULT_PAGINATION;
  }

  return {
    page: Math.max(0, Math.trunc(asNumber(value.page) ?? DEFAULT_PAGINATION.page)),
    limit: Math.max(1, Math.trunc(asNumber(value.limit) ?? DEFAULT_PAGINATION.limit)),
    total: Math.max(0, Math.trunc(asNumber(value.total) ?? DEFAULT_PAGINATION.total)),
    totalPages: Math.max(
      1,
      Math.trunc(asNumber(value.totalPages) ?? DEFAULT_PAGINATION.totalPages),
    ),
  };
}

export function resolveHistoryStatus(item: HistoryItem): string {
  const recordStatus = (
    item.status ??
    item.recordStatus ??
    item.verificationStatus
  )?.trim().toLowerCase();

  if (recordStatus === "done") {
    if (item.overallStatus === "abnormal" || item.overallStatus === "attention") {
      return item.overallStatus;
    }
    return "normal";
  }

  if (
    recordStatus === "review_required" ||
    recordStatus === "processing" ||
    recordStatus === "pending"
  ) {
    return "unverified";
  }

  if (
    recordStatus === "ocr_failed" ||
    recordStatus === "failed" ||
    recordStatus === "error"
  ) {
    return "error";
  }

  if (!item.examDate && item.sourceType === "ocr_partial") {
    return "unverified";
  }

  return item.overallStatus === "abnormal" || item.overallStatus === "attention"
    ? item.overallStatus
    : "unverified";
}
