import { useAuth } from "../../hooks/useAuth";
import { Button } from "../../components/Button";

export function DashboardPage() {
  const { user, logout } = useAuth();
  return (
    <div className="mx-auto mt-16 max-w-2xl px-4">
      <h1 className="text-xl font-semibold text-gray-900">Welcome, {user?.name}</h1>
      <p className="mt-2 text-sm text-gray-600">Role: {user?.role}</p>
      <Button variant="secondary" className="mt-6" onClick={logout}>
        Log out
      </Button>
    </div>
  );
}
