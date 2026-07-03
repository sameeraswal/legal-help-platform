import { Navigate, Route, Routes } from "react-router-dom";
import { LoginPage } from "./features/customer/LoginPage";
import { RegisterPage } from "./features/customer/RegisterPage";
import { MyCasesPage } from "./features/customer/MyCasesPage";
import { CategoriesPage } from "./features/customer/CategoriesPage";
import { CaseIntakeWizard } from "./features/customer/CaseIntakeWizard";
import { CaseDetailPage } from "./features/customer/CaseDetailPage";
import { LlmChatPage } from "./features/customer/LlmChatPage";
import { LawyerChatPage } from "./features/customer/LawyerChatPage";
import { WalletPage } from "./features/customer/WalletPage";
import { LawyerDashboardPage } from "./features/lawyer/LawyerDashboardPage";
import { LawyerWalletPage } from "./features/lawyer/LawyerWalletPage";
import { AdminCategoriesPage } from "./features/admin/AdminCategoriesPage";
import { AdminPlansPage } from "./features/admin/AdminPlansPage";
import { AdminConfigPage } from "./features/admin/AdminConfigPage";
import { AdminTransactionsPage } from "./features/admin/AdminTransactionsPage";
import { AdminLawyerRatesPage } from "./features/admin/AdminLawyerRatesPage";
import { AdminPayoutsPage } from "./features/admin/AdminPayoutsPage";
import { AdminUsersPage } from "./features/admin/AdminUsersPage";
import { AdminAuditLogsPage } from "./features/admin/AdminAuditLogsPage";
import { ProtectedRoute } from "./components/ProtectedRoute";
import { NavBar } from "./components/NavBar";

export function App() {
  return (
    <div className="min-h-screen bg-gray-50">
      <NavBar />
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />

        <Route path="/" element={<ProtectedRoute roles={["CUSTOMER"]}><MyCasesPage /></ProtectedRoute>} />
        <Route path="/categories" element={<ProtectedRoute roles={["CUSTOMER"]}><CategoriesPage /></ProtectedRoute>} />
        <Route path="/cases/:caseId/intake" element={<ProtectedRoute roles={["CUSTOMER"]}><CaseIntakeWizard /></ProtectedRoute>} />
        <Route path="/cases/:caseId" element={<ProtectedRoute roles={["CUSTOMER"]}><CaseDetailPage /></ProtectedRoute>} />
        <Route path="/chat/llm" element={<ProtectedRoute roles={["CUSTOMER"]}><LlmChatPage /></ProtectedRoute>} />
        <Route path="/chat/lawyer" element={<ProtectedRoute roles={["CUSTOMER"]}><LawyerChatPage /></ProtectedRoute>} />
        <Route path="/wallet" element={<ProtectedRoute roles={["CUSTOMER"]}><WalletPage /></ProtectedRoute>} />

        <Route path="/lawyer" element={<ProtectedRoute roles={["LAWYER"]}><LawyerDashboardPage /></ProtectedRoute>} />
        <Route path="/lawyer/wallet" element={<ProtectedRoute roles={["LAWYER"]}><LawyerWalletPage /></ProtectedRoute>} />

        <Route path="/admin" element={<ProtectedRoute roles={["ADMIN"]}><AdminCategoriesPage /></ProtectedRoute>} />
        <Route path="/admin/plans" element={<ProtectedRoute roles={["ADMIN"]}><AdminPlansPage /></ProtectedRoute>} />
        <Route path="/admin/config" element={<ProtectedRoute roles={["ADMIN"]}><AdminConfigPage /></ProtectedRoute>} />
        <Route path="/admin/transactions" element={<ProtectedRoute roles={["ADMIN"]}><AdminTransactionsPage /></ProtectedRoute>} />
        <Route path="/admin/lawyer-rates" element={<ProtectedRoute roles={["ADMIN"]}><AdminLawyerRatesPage /></ProtectedRoute>} />
        <Route path="/admin/payouts" element={<ProtectedRoute roles={["ADMIN"]}><AdminPayoutsPage /></ProtectedRoute>} />
        <Route path="/admin/users" element={<ProtectedRoute roles={["ADMIN"]}><AdminUsersPage /></ProtectedRoute>} />
        <Route path="/admin/audit-logs" element={<ProtectedRoute roles={["ADMIN"]}><AdminAuditLogsPage /></ProtectedRoute>} />

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </div>
  );
}
