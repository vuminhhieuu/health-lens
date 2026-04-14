import axios from "axios";

import { useAuthStore } from "@/stores/authStore";

import { API_ROUTES } from "./routes";

const apiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

const apiClient = axios.create({
  baseURL: apiBaseUrl,
  withCredentials: true, // Send HttpOnly cookies
  headers: {
    "Content-Type": "application/json",
  },
});

// Request interceptor: attach Authorization header
apiClient.interceptors.request.use((config) => {
  const { accessToken } = useAuthStore.getState();
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
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

    // Only attempt refresh for 401 that is NOT from auth endpoints
    if (
      error.response?.status === 401 &&
      !originalRequest._retry &&
      !originalRequest.url?.includes(API_ROUTES.AUTH.LOGIN) &&
      !originalRequest.url?.includes(API_ROUTES.AUTH.REFRESH)
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
        const refreshResponse = await apiClient.post(API_ROUTES.AUTH.REFRESH);
        const newAccessToken = refreshResponse.data?.data?.accessToken;

        if (newAccessToken) {
          const { user } = useAuthStore.getState();
          if (user) {
            useAuthStore.getState().setAuth(user, newAccessToken);
          }
          processQueue(null);
          return apiClient(originalRequest);
        }

        processQueue(new Error("Refresh token invalid"));
        useAuthStore.getState().clearAuth();
        if (typeof window !== "undefined") {
          window.location.href = "/login";
        }
        return Promise.reject(new Error("Refresh token invalid"));
      } catch (refreshError) {
        processQueue(refreshError);
        useAuthStore.getState().clearAuth();
        if (typeof window !== "undefined") {
          window.location.href = "/login";
        }
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  },
);

export { apiClient, apiBaseUrl };
