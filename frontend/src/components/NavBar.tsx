import { NavLink } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";

const linkClass = ({ isActive }: { isActive: boolean }) =>
  `rounded-md px-3 py-2 text-sm font-medium ${isActive ? "bg-brand-50 text-brand-700" : "text-gray-600 hover:text-brand-600"}`;

export function NavBar() {
  const { user, logout } = useAuth();
  if (!user) {
    return null;
  }

  return (
    <header className="border-b border-gray-200 bg-white">
      <div className="mx-auto flex max-w-5xl items-center justify-between px-4 py-3">
        <span className="text-sm font-semibold text-brand-700">Legal Help Platform</span>
        <nav className="flex items-center gap-1">
          {user.role === "CUSTOMER" && (
            <>
              <NavLink to="/" className={linkClass} end>
                My Cases
              </NavLink>
              <NavLink to="/categories" className={linkClass}>
                New Case
              </NavLink>
              <NavLink to="/chat/llm" className={linkClass}>
                AI Chat
              </NavLink>
              <NavLink to="/chat/lawyer" className={linkClass}>
                Lawyer Chat
              </NavLink>
              <NavLink to="/wallet" className={linkClass}>
                Wallet
              </NavLink>
            </>
          )}
          {user.role === "LAWYER" && (
            <>
              <NavLink to="/lawyer" className={linkClass} end>
                Dashboard
              </NavLink>
              <NavLink to="/lawyer/wallet" className={linkClass}>
                Wallet
              </NavLink>
            </>
          )}
          {user.role === "ADMIN" && (
            <>
              <NavLink to="/admin" className={linkClass} end>
                Categories
              </NavLink>
              <NavLink to="/admin/plans" className={linkClass}>
                Plans
              </NavLink>
              <NavLink to="/admin/config" className={linkClass}>
                Config
              </NavLink>
              <NavLink to="/admin/transactions" className={linkClass}>
                Transactions
              </NavLink>
              <NavLink to="/admin/lawyer-rates" className={linkClass}>
                Rates
              </NavLink>
              <NavLink to="/admin/payouts" className={linkClass}>
                Payouts
              </NavLink>
              <NavLink to="/admin/users" className={linkClass}>
                Users
              </NavLink>
              <NavLink to="/admin/audit-logs" className={linkClass}>
                Audit Log
              </NavLink>
            </>
          )}
          <span className="ml-3 text-sm text-gray-500">{user.name}</span>
          <button onClick={logout} className="ml-2 text-sm text-gray-500 hover:text-red-600">
            Log out
          </button>
        </nav>
      </div>
    </header>
  );
}
