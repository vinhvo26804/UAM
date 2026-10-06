import type { JwtClaims } from "../types/index.ts";

const ACCESS_TOKEN_KEY = "uam_access_token";
const REFRESH_TOKEN_KEY = "uam_refresh_token";
const USER_ROLES_KEY = "uam_user_roles";
const USER_PROFILE_KEY = "uam_user_profile";

export interface StoredUserProfile {
  id?: number;
  username: string;
  email?: string;
  roles: string[];
}

function notifyStorageChange(): void {
  if (typeof window !== "undefined" && typeof window.dispatchEvent === "function") {
    try {
      window.dispatchEvent(new Event("storage"));
    } catch {
      // Safe fallback for testing or non-browser environments
    }
  }
}

export const tokenStorage = {
  getAccessToken(): string | null {
    if (typeof window === "undefined" || !globalThis.localStorage) return null;
    return globalThis.localStorage.getItem(ACCESS_TOKEN_KEY);
  },

  setAccessToken(token: string): void {
    if (typeof window === "undefined" || !globalThis.localStorage) return;
    globalThis.localStorage.setItem(ACCESS_TOKEN_KEY, token);
  },

  getRefreshToken(): string | null {
    if (typeof window === "undefined" || !globalThis.localStorage) return null;
    return globalThis.localStorage.getItem(REFRESH_TOKEN_KEY);
  },

  setRefreshToken(token: string): void {
    if (typeof window === "undefined" || !globalThis.localStorage) return;
    globalThis.localStorage.setItem(REFRESH_TOKEN_KEY, token);
  },

  setTokens(accessToken: string, refreshToken: string): void {
    if (typeof window === "undefined" || !globalThis.localStorage) return;
    globalThis.localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
    globalThis.localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
  },

  setUserProfile(profile: StoredUserProfile): void {
    if (typeof window === "undefined" || !globalThis.localStorage) return;
    globalThis.localStorage.setItem(USER_PROFILE_KEY, JSON.stringify(profile));
    if (Array.isArray(profile.roles)) {
      globalThis.localStorage.setItem(USER_ROLES_KEY, JSON.stringify(profile.roles));
    }
    notifyStorageChange();
  },

  getUserProfile(): StoredUserProfile | null {
    if (typeof window === "undefined" || !globalThis.localStorage) return null;
    const raw = globalThis.localStorage.getItem(USER_PROFILE_KEY);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as StoredUserProfile;
    } catch {
      return null;
    }
  },

  getUserRoles(): string[] {
    if (typeof window === "undefined" || !globalThis.localStorage) return [];
    const raw = globalThis.localStorage.getItem(USER_ROLES_KEY);
    if (!raw) return [];
    try {
      const parsed = JSON.parse(raw);
      return Array.isArray(parsed) ? parsed : [];
    } catch {
      return [];
    }
  },

  clearTokens(): void {
    if (typeof window === "undefined" || !globalThis.localStorage) return;
    globalThis.localStorage.removeItem(ACCESS_TOKEN_KEY);
    globalThis.localStorage.removeItem(REFRESH_TOKEN_KEY);
    globalThis.localStorage.removeItem(USER_ROLES_KEY);
    globalThis.localStorage.removeItem(USER_PROFILE_KEY);
    notifyStorageChange();
  },

  decodeToken(token: string | null): JwtClaims | null {
    if (!token) return null;
    try {
      const parts = token.split(".");
      if (parts.length !== 3) return null;
      const base64Url = parts[1];
      const base64 = base64Url.replace(/-/g, "+").replace(/_/g, "/");
      const jsonPayload = decodeURIComponent(
        atob(base64)
          .split("")
          .map((c) => "%" + ("00" + c.charCodeAt(0).toString(16)).slice(-2))
          .join("")
      );
      return JSON.parse(jsonPayload) as JwtClaims;
    } catch {
      return null;
    }
  },

  getCurrentUser(): { username: string; roles: string[] } | null {
    const token = this.getAccessToken();
    const claims = this.decodeToken(token);
    if (!claims || !claims.sub) return null;

    // Check expiration
    if (claims.exp && claims.exp * 1000 < Date.now()) {
      return null;
    }

    let roles: string[] = [];
    if (Array.isArray(claims.roles) && claims.roles.length > 0) {
      roles = claims.roles;
    } else if (typeof claims.roles === "string") {
      roles = [claims.roles];
    } else if (claims.role && typeof claims.role === "string") {
      roles = [claims.role];
    }

    // Fallback: If JWT does not embed roles (like current backend Spring Boot implementation),
    // read roles synced from GET /me endpoint
    if (roles.length === 0) {
      roles = this.getUserRoles();
    }

    return {
      username: claims.sub,
      roles,
    };
  },

  isAdmin(): boolean {
    const user = this.getCurrentUser();
    if (!user) return false;
    return user.roles.some((r) => r === "ROLE_ADMIN" || r === "ADMIN");
  },
};
