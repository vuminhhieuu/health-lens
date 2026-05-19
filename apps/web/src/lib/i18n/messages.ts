export const messageCatalog = {
  auth: {
    accountLocked: (minutes: number) =>
      `Tài khoản bị khóa tạm thời. Vui lòng thử lại sau ${minutes} phút.`,
    emailOrPasswordInvalid: "Email hoặc mật khẩu không đúng.",
    loginFailed: "Đăng nhập thất bại. Vui lòng thử lại.",
    loginUnavailable: "Tài khoản chưa thể đăng nhập ở thời điểm hiện tại.",
    networkError: "Không thể kết nối đến máy chủ. Vui lòng thử lại.",
    pendingDeletion:
      "Tài khoản đang chờ xóa. Trong thời gian hệ thống xử lý yêu cầu xóa dữ liệu, bạn không thể đăng nhập.",
    rateLimited: (minutes: number) =>
      `Bạn đã gửi yêu cầu quá nhanh. Vui lòng thử lại sau ${minutes} phút.`,
    registerSuccess:
      "Tài khoản đã tạo. Vui lòng kiểm tra email của bạn để xác thực.",
    registerSuccessWithInvite:
      "Tài khoản đã tạo. Vui lòng xác thực email, sau đó đăng nhập để tiếp tục lời mời đang chờ.",
    registerFailed: "Đăng ký thất bại. Vui lòng thử lại.",
    registerEmailExists: "Email này đã được đăng ký.",
    registerValidationFailed: "Thông tin đăng ký chưa hợp lệ. Vui lòng kiểm tra lại.",
    forgotPasswordSent:
      "Đã gửi hướng dẫn khôi phục mật khẩu đến email của bạn.",
    forgotPasswordRateLimited: (minutes: number) =>
      `Bạn đã gửi yêu cầu quá nhiều. Vui lòng thử lại sau ${minutes} phút.`,
    genericRetry: "Đã có lỗi xảy ra. Vui lòng thử lại sau.",
    resetTokenInvalid: "Token không hợp lệ hoặc đã hết hạn.",
    resetPasswordSuccess: "Mật khẩu của bạn đã được cập nhật thành công.",
  },
  deletion: {
    requestCreated: "Đã gửi yêu cầu xóa tài khoản.",
    requestCancelled: "Đã hủy yêu cầu xóa tài khoản.",
    cancelTokenInvalid:
      "Liên kết hủy yêu cầu không hợp lệ hoặc đã hết hiệu lực.",
    cancelUnavailable: "Yêu cầu xóa này không thể hủy được.",
    genericError: "Có lỗi xảy ra. Vui lòng thử lại.",
  },
  sharing: {
    invitationProcessingFailed:
      "Không thể xử lý lời mời. Vui lòng thử lại.",
    invitationEmailMismatch: "Email đăng nhập không khớp với lời mời.",
    invitationInvalid:
      "Liên kết mời không hợp lệ hoặc không còn dùng được.",
    invitationRateLimited:
      "Bạn đã thử xử lý lời mời quá nhiều lần. Vui lòng thử lại sau ít phút.",
    loginInvalidOrServerError:
      "Phiên đăng nhập không hợp lệ hoặc hệ thống đang lỗi. Vui lòng thử lại sau.",
    systemError: "Hệ thống đang gặp sự cố. Vui lòng thử lại sau.",
    networkError: "Không thể kết nối. Vui lòng kiểm tra mạng và thử lại.",
  },
  upload: {
    ocrRateLimited: (minutes: number) =>
      `Bạn đã gửi yêu cầu xử lý OCR quá nhanh. Vui lòng thử lại sau ${minutes} phút.`,
  },
} as const;

export type ApiErrorPayload = {
  detail?: string;
  title?: string;
  type?: string;
  errorCode?: string;
  retryAfterSeconds?: number;
  errors?: Array<{
    field?: string;
    message?: string;
  }>;
  properties?: {
    errorCode?: string;
    retryAfterSeconds?: number;
  };
};

type ApiErrorLike = {
  response?: {
    status?: number;
    data?: ApiErrorPayload;
  };
};

export function getApiErrorCode(error: unknown): string | undefined {
  const payload = getApiErrorPayload(error);
  return payload?.errorCode ?? payload?.properties?.errorCode;
}

export function getApiErrorPayload(error: unknown): ApiErrorPayload | undefined {
  if (!error || typeof error !== "object" || !("response" in error)) {
    return undefined;
  }

  return (error as ApiErrorLike).response?.data;
}

export function getApiErrorStatus(error: unknown): number | undefined {
  if (!error || typeof error !== "object" || !("response" in error)) {
    return undefined;
  }

  return (error as ApiErrorLike).response?.status;
}

export function retryAfterMinutes(payload: ApiErrorPayload | undefined, fallbackSeconds: number) {
  const retryAfter =
    payload?.retryAfterSeconds ?? payload?.properties?.retryAfterSeconds ?? fallbackSeconds;
  const seconds = typeof retryAfter === "number" && Number.isFinite(retryAfter) && retryAfter > 0
    ? retryAfter
    : fallbackSeconds;
  return Math.max(1, Math.ceil(seconds / 60));
}

export function authErrorMessage(error: unknown): string {
  const code = getApiErrorCode(error);
  const status = getApiErrorStatus(error);
  const payload = getApiErrorPayload(error);
  const type = payload?.type ?? "";

  if (code === "ACCOUNT_LOCKED" || status === 429) {
    return messageCatalog.auth.accountLocked(retryAfterMinutes(payload, 900));
  }

  if (code === "ACCOUNT_PENDING_DELETION" || type.endsWith("/account-pending-deletion")) {
    return messageCatalog.auth.pendingDeletion;
  }

  if (code === "INVALID_CREDENTIALS" || status === 401) {
    return messageCatalog.auth.emailOrPasswordInvalid;
  }

  if (status === 403 || status === 423) {
    return messageCatalog.auth.loginUnavailable;
  }

  return status ? messageCatalog.auth.loginFailed : messageCatalog.auth.networkError;
}

export function invitationErrorMessage(error: unknown): string {
  const status = getApiErrorStatus(error);

  if (status === 429) {
    return messageCatalog.sharing.invitationRateLimited;
  }

  if (status === 403) {
    return messageCatalog.sharing.invitationEmailMismatch;
  }

  if (status === 404) {
    return messageCatalog.sharing.invitationInvalid;
  }

  if (status !== undefined && status >= 500) {
    return messageCatalog.sharing.systemError;
  }

  return status
    ? messageCatalog.sharing.invitationProcessingFailed
    : messageCatalog.sharing.networkError;
}

export function registerErrorMessage(error: unknown): string {
  const code = getApiErrorCode(error);
  const payload = getApiErrorPayload(error);

  if (code === "RATE_LIMITED" || getApiErrorStatus(error) === 429) {
    return messageCatalog.auth.rateLimited(retryAfterMinutes(payload, 3600));
  }

  if (code === "EMAIL_ALREADY_EXISTS") {
    return messageCatalog.auth.registerEmailExists;
  }

  if (code === "VALIDATION_ERROR") {
    const firstFieldMessage = payload?.errors?.find((item) => item.message)?.message;
    return firstFieldMessage ?? messageCatalog.auth.registerValidationFailed;
  }

  return getApiErrorStatus(error)
    ? messageCatalog.auth.registerFailed
    : messageCatalog.auth.networkError;
}
