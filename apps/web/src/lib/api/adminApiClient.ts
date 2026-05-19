import axios, { AxiosHeaders } from "axios";

import { API_ROUTES } from "@/lib/api/routes";

const apiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";
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

function isUnsafeMethod(method?: string): boolean {
  const normalizedMethod = method?.toUpperCase() ?? "GET";
  return ["POST", "PUT", "PATCH", "DELETE"].includes(normalizedMethod);
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

const adminApiClient = axios.create({
  baseURL: apiBaseUrl,
  withCredentials: true,
  headers: {
    "Content-Type": "application/json",
  },
});

adminApiClient.interceptors.request.use(async (config) => {
  const headers = AxiosHeaders.from(config.headers);
  config.headers = headers;

  if (typeof FormData !== "undefined" && config.data instanceof FormData) {
    headers.delete("Content-Type");
    headers.delete("content-type");
  }

  if (isUnsafeMethod(config.method)) {
    await ensureCsrfToken();
    const xsrfToken = readCookie(XSRF_COOKIE_NAME);
    if (xsrfToken && !headers.has(XSRF_HEADER_NAME)) {
      headers.set(XSRF_HEADER_NAME, xsrfToken);
    }
  }

  return config;
});

adminApiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401 && typeof window !== "undefined") {
      if (window.location.pathname !== "/admin/login") {
        window.location.href = "/admin/login";
      }
    }
    return Promise.reject(error);
  },
);

export { adminApiClient };
