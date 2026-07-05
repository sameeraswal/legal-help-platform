import type { AuthResponse } from "./types/auth";

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

// Endpoints that must never trigger the refresh-and-retry logic below - refresh itself would
// recurse, and a 401 from login/register means bad credentials, not an expired session.
const AUTH_ENDPOINTS_EXEMPT_FROM_REFRESH = ["/api/auth/login", "/api/auth/register", "/api/auth/refresh"];

export class ApiError extends Error {
  constructor(
    public readonly code: string,
    message: string,
    public readonly traceId: string,
  ) {
    super(message);
  }
}

export function getAccessToken(): string | null {
  return localStorage.getItem("accessToken");
}

export function getRefreshToken(): string | null {
  return localStorage.getItem("refreshToken");
}

export function setTokens(accessToken: string, refreshToken: string): void {
  localStorage.setItem("accessToken", accessToken);
  localStorage.setItem("refreshToken", refreshToken);
}

export function clearTokens(): void {
  localStorage.removeItem("accessToken");
  localStorage.removeItem("refreshToken");
}

function authHeaders(): Record<string, string> {
  const token = getAccessToken();
  return token ? { Authorization: `Bearer ${token}` } : {};
}

async function parseBody(response: Response): Promise<unknown> {
  // Some endpoints (e.g. void controller methods) return 200/204 with an empty body -
  // response.json() throws on an empty string, so only parse when there's content.
  const text = await response.text();
  return text.length > 0 ? JSON.parse(text) : undefined;
}

interface ErrorBody {
  code?: string;
  message?: string;
  traceId?: string;
}

function errorFrom(body: unknown, fallbackMessage: string): ApiError {
  const { code, message, traceId } = (body ?? {}) as ErrorBody;
  return new ApiError(code ?? "UNKNOWN", message ?? fallbackMessage, traceId ?? "unknown");
}

// Coalesces concurrent 401s onto a single refresh call instead of firing one per in-flight
// request (each refresh rotates the refresh token server-side, so racing calls would each try
// to consume it).
let refreshPromise: Promise<void> | null = null;

async function refreshAccessToken(): Promise<void> {
  if (!refreshPromise) {
    refreshPromise = (async () => {
      const refreshToken = getRefreshToken();
      if (!refreshToken) {
        throw new ApiError("UNAUTHENTICATED", "Not logged in", "unknown");
      }
      const response = await fetch(`${API_BASE_URL}/api/auth/refresh`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ refreshToken }),
      });
      const body = await parseBody(response);
      if (!response.ok) {
        throw errorFrom(body, "Session expired");
      }
      const authResponse = body as AuthResponse;
      setTokens(authResponse.accessToken, authResponse.refreshToken);
      localStorage.setItem("user", JSON.stringify(authResponse.user));
    })().finally(() => {
      refreshPromise = null;
    });
  }
  return refreshPromise;
}

/** Clears the session and sends the user back to the login page. Session is unrecoverable. */
function forceLogout(): void {
  clearTokens();
  localStorage.removeItem("user");
  if (window.location.pathname !== "/login") {
    window.location.href = "/login";
  }
}

export async function apiFetch<T>(path: string, options: RequestInit = {}, isRetry = false): Promise<T> {
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...authHeaders(),
    ...(options.headers as Record<string, string> | undefined),
  };

  const response = await fetch(`${API_BASE_URL}${path}`, { ...options, headers });

  // 401 means the access token is missing/invalid/expired (see RestAuthenticationEntryPoint on
  // the backend) - try a silent refresh and retry once. 403 means authenticated but the wrong
  // role (RestAccessDeniedHandler); refreshing the token wouldn't fix that, so don't retry it.
  if (response.status === 401 && !isRetry && !AUTH_ENDPOINTS_EXEMPT_FROM_REFRESH.includes(path)) {
    try {
      await refreshAccessToken();
    } catch {
      forceLogout();
      throw new ApiError("SESSION_EXPIRED", "Your session has expired. Please log in again.", "unknown");
    }
    return apiFetch<T>(path, options, true);
  }

  const body = await parseBody(response);

  if (!response.ok) {
    throw errorFrom(body, "Request failed");
  }

  return body as T;
}

export async function apiFetchBlob(path: string): Promise<Blob> {
  const response = await fetch(`${API_BASE_URL}${path}`, { headers: authHeaders() });
  if (!response.ok) {
    throw new ApiError("DOWNLOAD_FAILED", "Failed to download file", "unknown");
  }
  return response.blob();
}
