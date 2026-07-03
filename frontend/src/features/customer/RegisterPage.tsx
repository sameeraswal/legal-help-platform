import { useState, type FormEvent } from "react";
import { useNavigate, Link } from "react-router-dom";
import { register } from "../../api/auth";
import { ApiError } from "../../api/client";
import { useAuth } from "../../hooks/useAuth";
import { Button } from "../../components/Button";
import { Input } from "../../components/Input";

export function RegisterPage() {
  const { applyAuthResponse } = useAuth();
  const navigate = useNavigate();
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [role, setRole] = useState<"CUSTOMER" | "LAWYER">("CUSTOMER");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      const response = await register({ name, email, password, role });
      applyAuthResponse(response);
      navigate("/");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Registration failed");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="mx-auto mt-16 w-full max-w-sm px-4">
      <h1 className="mb-6 text-xl font-semibold text-gray-900">Create an account</h1>
      <form onSubmit={handleSubmit} className="space-y-4">
        <Input placeholder="Full name" value={name} onChange={(e) => setName(e.target.value)} required />
        <Input type="email" placeholder="Email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        <Input
          type="password"
          placeholder="Password (min 8 characters)"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          minLength={8}
          required
        />
        <select
          className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm"
          value={role}
          onChange={(e) => setRole(e.target.value as "CUSTOMER" | "LAWYER")}
        >
          <option value="CUSTOMER">I need legal help</option>
          <option value="LAWYER">I am a lawyer</option>
        </select>
        {error && <p className="text-sm text-red-600">{error}</p>}
        <Button type="submit" disabled={submitting} className="w-full">
          {submitting ? "Creating account..." : "Create account"}
        </Button>
      </form>
      <p className="mt-4 text-sm text-gray-600">
        Already have an account? <Link className="text-brand-600" to="/login">Log in</Link>
      </p>
    </div>
  );
}
