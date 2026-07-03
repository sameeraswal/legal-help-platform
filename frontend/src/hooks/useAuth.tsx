import { createContext, useContext, useMemo, useState, type ReactNode } from "react";
import { clearTokens, setTokens } from "../api/client";
import type { AuthResponse, UserProfile } from "../api/types/auth";

interface AuthContextValue {
  user: UserProfile | null;
  applyAuthResponse: (response: AuthResponse) => void;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

function loadStoredUser(): UserProfile | null {
  const raw = localStorage.getItem("user");
  return raw ? (JSON.parse(raw) as UserProfile) : null;
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserProfile | null>(loadStoredUser);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      applyAuthResponse: (response: AuthResponse) => {
        setTokens(response.accessToken, response.refreshToken);
        localStorage.setItem("user", JSON.stringify(response.user));
        setUser(response.user);
      },
      logout: () => {
        clearTokens();
        localStorage.removeItem("user");
        setUser(null);
      },
    }),
    [user],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return ctx;
}
