export type HealthRecordHubStatus = "normal" | "warning" | "critical";

export type HealthRecordHubCard = {
  id: string;
  displayName: string;
  relationship: string;
  notes?: string;
  updatedAt?: string;
  latestStatus?: HealthRecordHubStatus;
  lastRecordAt?: string;
  isSharedProfile: boolean;
  href: string;
};

export type SharedHealthRecord = {
  recordId: string;
  profileId: string;
  profileDisplayName: string;
  recordType?: string | null;
  examDate?: string | null;
  hospitalName?: string | null;
  status?: string | null;
  overallStatus?: string | null;
  lastUpdated?: string | null;
  sharedAt?: string | null;
};

type RecordState = {
  recordStatus?: string | null;
  overallStatus?: string | null;
};

type SharedHealthRecordProfileGroup = {
  profileId: string;
  profileDisplayName: string;
  totalRecords: number;
  latestStatus?: HealthRecordHubStatus;
  latestActivityAt?: string;
};

function normalizeStatus(status?: string | null) {
  return status?.trim().toLowerCase();
}

function parseTimestamp(value?: string | Date | null): number | null {
  if (!value) return null;
  const timestamp = value instanceof Date ? value.getTime() : Date.parse(value);
  return Number.isFinite(timestamp) ? timestamp : null;
}

function latestValidTimestamp(
  ...values: Array<string | Date | null | undefined>
): { value?: string; timestamp: number | null } {
  return values.reduce<{ value?: string; timestamp: number | null }>(
    (latest, value) => {
      const timestamp = parseTimestamp(value);
      if (timestamp === null) return latest;
      if (latest.timestamp !== null && timestamp <= latest.timestamp) return latest;
      const timestampValue = value instanceof Date ? value.toISOString() : String(value);
      return { value: timestampValue, timestamp };
    },
    { value: undefined, timestamp: null }
  );
}

export function latestValidDateValue(...values: Array<string | Date | null | undefined>): string | undefined {
  return latestValidTimestamp(...values).value;
}

export function mapRecordStateToCardStatus({
  recordStatus,
  overallStatus,
}: RecordState): HealthRecordHubStatus | undefined {
  const normalizedRecordStatus = normalizeStatus(recordStatus);
  const normalizedOverallStatus = normalizeStatus(overallStatus);

  if (normalizedRecordStatus) {
    if (normalizedRecordStatus === "done" || normalizedRecordStatus === "completed") {
      if (normalizedOverallStatus === "abnormal") return "critical";
      if (normalizedOverallStatus === "attention" || normalizedOverallStatus === "warning") return "warning";
      if (normalizedOverallStatus === "normal") return "normal";
      return undefined;
    }

    if (
      normalizedRecordStatus === "processing" ||
      normalizedRecordStatus === "pending" ||
      normalizedRecordStatus === "review_required" ||
      normalizedRecordStatus === "unverified" ||
      normalizedRecordStatus === "ocr_failed" ||
      normalizedRecordStatus === "failed" ||
      normalizedRecordStatus === "error"
    ) {
      return undefined;
    }
  }

  if (normalizedOverallStatus === "normal") return "normal";
  if (normalizedOverallStatus === "attention" || normalizedOverallStatus === "warning") return "warning";
  if (normalizedOverallStatus === "abnormal") return "critical";
  return undefined;
}

export function buildSharedRecordProfileGroups(records: SharedHealthRecord[]): HealthRecordHubCard[] {
  const groupedByProfile = records.reduce<Record<string, SharedHealthRecordProfileGroup>>((acc, record) => {
    const activity = latestValidTimestamp(record.lastUpdated, record.sharedAt, record.examDate);
    const latestStatus = mapRecordStateToCardStatus({
      recordStatus: record.status,
      overallStatus: record.overallStatus,
    });
    const existingGroup = acc[record.profileId];

    if (!existingGroup) {
      acc[record.profileId] = {
        profileId: record.profileId,
        profileDisplayName: record.profileDisplayName,
        totalRecords: 1,
        latestStatus,
        latestActivityAt: activity.value,
      };
      return acc;
    }

    const existingTimestamp = parseTimestamp(existingGroup.latestActivityAt);
    const shouldUseRecord =
      activity.timestamp !== null &&
      (existingTimestamp === null || activity.timestamp > existingTimestamp);

    acc[record.profileId] = {
      ...existingGroup,
      totalRecords: existingGroup.totalRecords + 1,
      latestStatus: shouldUseRecord ? latestStatus : existingGroup.latestStatus,
      latestActivityAt: shouldUseRecord ? activity.value : existingGroup.latestActivityAt,
    };
    return acc;
  }, {});

  return Object.values(groupedByProfile).map((group) => ({
    id: group.profileId,
    displayName: group.profileDisplayName,
    notes:
      group.totalRecords > 1
        ? `${group.totalRecords} kết quả khám đã được chia sẻ`
        : "1 kết quả khám đã được chia sẻ",
    updatedAt: group.latestActivityAt,
    latestStatus: group.latestStatus,
    lastRecordAt: group.latestActivityAt,
    relationship: "Kết quả được chia sẻ",
    isSharedProfile: true,
    href: `/profiles/${group.profileId}/history?displayName=${encodeURIComponent(group.profileDisplayName)}`,
  }));
}

export function sortHubCardsByLatestActivity(cards: HealthRecordHubCard[]): HealthRecordHubCard[] {
  return [...cards].sort((left, right) => {
    const leftTimestamp = latestValidTimestamp(left.updatedAt, left.lastRecordAt).timestamp;
    const rightTimestamp = latestValidTimestamp(right.updatedAt, right.lastRecordAt).timestamp;

    if (leftTimestamp !== null && rightTimestamp !== null && leftTimestamp !== rightTimestamp) {
      return rightTimestamp - leftTimestamp;
    }
    if (leftTimestamp !== null && rightTimestamp === null) return -1;
    if (leftTimestamp === null && rightTimestamp !== null) return 1;

    const relationshipOrder = left.relationship.localeCompare(right.relationship, "vi");
    if (relationshipOrder !== 0) return relationshipOrder;
    const nameOrder = left.displayName.localeCompare(right.displayName, "vi");
    if (nameOrder !== 0) return nameOrder;
    return left.id.localeCompare(right.id);
  });
}
