import axios, { AxiosHeaders } from "axios";

const apiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

const adminApiClient = axios.create({
  baseURL: apiBaseUrl,
  withCredentials: true,
  headers: {
    "Content-Type": "application/json",
  },
});

adminApiClient.interceptors.request.use((config) => {
  const headers = AxiosHeaders.from(config.headers);
  config.headers = headers;

  if (typeof window !== "undefined") {
    const token = sessionStorage.getItem("admin_access_token");
    if (token && !headers.has("Authorization") && !headers.has("authorization")) {
      headers.set("Authorization", `Bearer ${token}`);
    }
  }

  return config;
});

adminApiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401 && typeof window !== "undefined") {
      sessionStorage.removeItem("admin_access_token");
      if (window.location.pathname !== "/admin/login") {
        window.location.href = "/admin/login";
      }
    }
    return Promise.reject(error);
  },
);

export { adminApiClient };
