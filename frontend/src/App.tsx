import { Navigate, Route, Routes } from "react-router-dom";
import { LoginPage } from "./features/customer/LoginPage";
import { RegisterPage } from "./features/customer/RegisterPage";
import { DashboardPage } from "./features/customer/DashboardPage";
import { ProtectedRoute } from "./components/ProtectedRoute";

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route
        path="/"
        element={
          <ProtectedRoute>
            <DashboardPage />
          </ProtectedRoute>
        }
      />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
