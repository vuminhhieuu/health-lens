import axios, { AxiosHeaders } from "axios";

import { refreshSessionOnce } from "@/lib/auth/refreshSession";
import { useAuthStore } from "@/stores/authStore";

import { readSessionStorage } from "@/lib/browser/sessionStorage";
import { PENDING_PROFILE_INVITATION_TOKEN_KEY } from "@/lib/sharing/profileInvitationStorage";

import { API_ROUTES } from "./routes";

const apiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

const apiClient = axios.create({
  baseURL: apiBaseUrl,
  withCredentials: true, // Send HttpOnly cookies
  headers: {
    "Content-Type": "application/json",
  },
});

const XSRF_COOKIE_NAME = "XSRF-TOKEN";
const XSRF_HEADER_NAME = "X-XSRF-TOKEN";
let csrfBootstrapPromise: Promise<void> | null = null;

function readCookie(name: string): string | null {
  if (typeof document === "undefined") {
    return null;
  }

  const encodedName = `${encodeURIComponent(name)}=`;
  const cookie = document.cookie
    .split(";")
    .map((value) => value.trim())
    .find((value) => value.startsWith(encodedName));

  return cookie ? decodeURIComponent(cookie.slice(encodedName.length)) : null;
}

function redirectToLoginPreservingInvitationContext() {
  if (typeof window === "undefined") {
    return;
  }

  const pathname = window.location.pathname;
  if (pathname === "/invitations/accept") {
    const tokenFromQuery = new URLSearchParams(window.location.search).get("token");
    const token = tokenFromQuery ?? readSessionStorage(PENDING_PROFILE_INVITATION_TOKEN_KEY);
    if (token) {
      const returnUrl = `/invitations/accept?token=${encodeURIComponent(token)}`;
      window.location.href = `/login?returnUrl=${encodeURIComponent(returnUrl)}`;
      return;
    }
  }

  window.location.href = "/login";
}

function isUnsafeAuthCookieEndpoint(url?: string, method?: string): boolean {
  const normalizedMethod = method?.toUpperCase() ?? "GET";
  if (!["POST", "PUT", "PATCH", "DELETE"].includes(normalizedMethod)) {
    return false;
  }

  return Boolean(
    url?.includes(API_ROUTES.AUTH.LOGIN) ||
      url?.includes(API_ROUTES.AUTH.TOTP_VERIFY) ||
      url?.includes(API_ROUTES.AUTH.REFRESH) ||
      url?.includes(API_ROUTES.AUTH.LOGOUT),
  );
}

async function ensureCsrfToken(): Promise<void> {
  if (typeof window === "undefined" || readCookie(XSRF_COOKIE_NAME)) {
    return;
  }

  csrfBootstrapPromise ??= axios
    .get(API_ROUTES.AUTH.CSRF, {
      baseURL: apiBaseUrl,
      withCredentials: true,
      headers: { "Content-Type": "application/json" },
    })
    .then(() => undefined)
    .finally(() => {
      csrfBootstrapPromise = null;
    });

  await csrfBootstrapPromise;
}

// Request interceptor: attach Authorization header
apiClient.interceptors.request.use(async (config) => {
  const needsXsrfHeader = isUnsafeAuthCookieEndpoint(config.url, config.method);
  if (needsXsrfHeader) {
    await ensureCsrfToken();
  }

  const { accessToken } = useAuthStore.getState();
  const headers = AxiosHeaders.from(config.headers);
  config.headers = headers;

  if (typeof FormData !== "undefined" && config.data instanceof FormData) {
    headers.delete("Content-Type");
    headers.delete("content-type");
  }

  const isCancelDeletion =
    config.url?.includes("/deletion-requests/cancel");

  if (isCancelDeletion) {
    headers.delete("Authorization");
    headers.delete("authorization");
    return config;
  }
  if (accessToken && !headers.has("Authorization") && !headers.has("authorization")) {
    headers.set("Authorization", `Bearer ${accessToken}`);
  }
  const xsrfToken = needsXsrfHeader ? readCookie(XSRF_COOKIE_NAME) : null;
  if (xsrfToken && !headers.has(XSRF_HEADER_NAME)) {
    headers.set(XSRF_HEADER_NAME, xsrfToken);
  }
  return config;
});

// Response interceptor: auto-refresh on 401
let isRefreshing = false;
let failedQueue: Array<{
  resolve: (value: unknown) => void;
  reject: (reason: unknown) => void;
}> = [];

const processQueue = (error: unknown) => {
  failedQueue.forEach((promise) => {
    if (error) {
      promise.reject(error);
    } else {
      promise.resolve(undefined);
    }
  });
  failedQueue = [];
};

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    const errorCode: string | undefined =
      error?.response?.data?.errorCode ??
      error?.response?.data?.properties?.errorCode;
    const problemType: string | undefined = error?.response?.data?.type;

    // Only attempt refresh for 401 that is NOT from auth endpoints
    const isCancelDeletion =
      originalRequest.url?.startsWith(API_ROUTES.USERS.CANCEL_DELETION);
    if (
      error.response?.status === 401 &&
      !originalRequest._retry &&
      !originalRequest.url?.includes(API_ROUTES.AUTH.LOGIN) &&
      !originalRequest.url?.includes(API_ROUTES.AUTH.REFRESH) &&
      !originalRequest.url?.includes("/admin/auth/") &&
      !isCancelDeletion
    ) {
      if (isRefreshing) {
        // Queue subsequent 401s while refresh is in progress
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        }).then(() => apiClient(originalRequest));
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        const refreshed = await refreshSessionOnce();

        if (refreshed) {
          processQueue(null);
          return apiClient(originalRequest);
        }

        processQueue(new Error("Refresh token invalid"));
        useAuthStore.getState().clearAuth();
        if (typeof window !== "undefined") {
          redirectToLoginPreservingInvitationContext();
        }
        return Promise.reject(new Error("Refresh token invalid"));
      } catch (refreshError) {
        processQueue(refreshError);
        useAuthStore.getState().clearAuth();
        if (typeof window !== "undefined") {
          redirectToLoginPreservingInvitationContext();
        }
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    // Story 6.3: if a shared member gets revoked while online, the next API call
    // should fail with 403 and the client should return to dashboard.
    if (
      error.response?.status === 403 &&
      typeof window !== "undefined" &&
      window.location.pathname !== "/dashboard"
    ) {
      const url: string = String(originalRequest?.url ?? "");
      const isSharedProfileCall =
        url.includes("/health-records") || url.includes("/shared-profiles");
      const isRevokedSharedProfileAccess =
        errorCode === "PROFILE_ACCESS_REVOKED" ||
        problemType === "https://healthlens.vn/errors/profile-access-revoked";

      if (isSharedProfileCall && isRevokedSharedProfileAccess) {
        window.location.href = "/dashboard";
      }
    }

    return Promise.reject(error);
  },
);

export { apiClient, apiBaseUrl };
