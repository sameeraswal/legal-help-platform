import type { ReactNode } from "react";
import { Navigate } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";
import type { Role } from "../api/types/auth";

const HOME_ROUTE: Record<Role, string> = {
  CUSTOMER: "/",
  LAWYER: "/lawyer",
  ADMIN: "/admin",
};

export function ProtectedRoute({ roles, children }: { roles?: Role[]; children: ReactNode }) {
  const { user } = useAuth();
  if (!user) {
    return <Navigate to="/login" replace />;
  }
  if (roles && !roles.includes(user.role)) {
    return <Navigate to={HOME_ROUTE[user.role]} replace />;
  }
  return <>{children}</>;
}
