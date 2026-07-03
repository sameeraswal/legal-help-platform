import { useState, type FormEvent } from "react";
import { useNavigate, Link } from "react-router-dom";
import { login } from "../../api/auth";
import { ApiError } from "../../api/client";
import { useAuth } from "../../hooks/useAuth";
import { Button } from "../../components/Button";
import { Input } from "../../components/Input";

export function LoginPage() {
  const { applyAuthResponse } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      const response = await login({ email, password });
      applyAuthResponse(response);
      navigate("/");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Login failed");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="mx-auto mt-16 w-full max-w-sm px-4">
      <h1 className="mb-6 text-xl font-semibold text-gray-900">Log in</h1>
      <form onSubmit={handleSubmit} className="space-y-4">
        <Input type="email" placeholder="Email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        <Input
          type="password"
          placeholder="Password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
        />
        {error && <p className="text-sm text-red-600">{error}</p>}
        <Button type="submit" disabled={submitting} className="w-full">
          {submitting ? "Logging in..." : "Log in"}
        </Button>
      </form>
      <p className="mt-4 text-sm text-gray-600">
        No account? <Link className="text-brand-600" to="/register">Register</Link>
      </p>
    </div>
  );
}
