import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { approveLawyer, listUsersByRole, setUserStatus } from "../../api/admin";
import type { Role } from "../../api/types/auth";
import { Card } from "../../components/Card";
import { Button } from "../../components/Button";
import { Spinner } from "../../components/Spinner";
import { formatIst } from "../../utils/format";

const ROLES: Role[] = ["CUSTOMER", "LAWYER", "ADMIN"];

export function AdminUsersPage() {
  const [role, setRole] = useState<Role>("LAWYER");
  const queryClient = useQueryClient();
  const { data, isLoading } = useQuery({ queryKey: ["admin-users", role], queryFn: () => listUsersByRole(role) });

  const approveMutation = useMutation({
    mutationFn: (userId: number) => approveLawyer(userId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["admin-users"] }),
  });

  const statusMutation = useMutation({
    mutationFn: ({ userId, status }: { userId: number; status: "ACTIVE" | "SUSPENDED" }) => setUserStatus(userId, status),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["admin-users"] }),
  });

  return (
    <div className="mx-auto max-w-3xl px-4 py-8">
      <h1 className="mb-6 text-xl font-semibold text-gray-900">Users</h1>

      <div className="mb-4 flex gap-2">
        {ROLES.map((r) => (
          <button
            key={r}
            onClick={() => setRole(r)}
            className={`rounded-md px-3 py-1 text-xs font-medium ${role === r ? "bg-brand-600 text-white" : "bg-gray-100 text-gray-600"}`}
          >
            {r}
          </button>
        ))}
      </div>

      {isLoading && <Spinner />}

      <div className="space-y-2">
        {data?.content.map((user) => (
          <Card key={user.id}>
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-900">{user.name}</p>
                <p className="text-xs text-gray-500">
                  {user.email} · joined {formatIst(user.createdAt)}
                </p>
              </div>
              <div className="flex items-center gap-2">
                <span className={`text-xs ${user.status === "ACTIVE" ? "text-green-600" : "text-red-600"}`}>{user.status}</span>
                {user.role === "LAWYER" && !user.lawyerApproved && (
                  <Button onClick={() => approveMutation.mutate(user.id)} disabled={approveMutation.isPending}>
                    Approve
                  </Button>
                )}
                <Button
                  variant="secondary"
                  onClick={() =>
                    statusMutation.mutate({ userId: user.id, status: user.status === "ACTIVE" ? "SUSPENDED" : "ACTIVE" })
                  }
                >
                  {user.status === "ACTIVE" ? "Suspend" : "Reactivate"}
                </Button>
              </div>
            </div>
          </Card>
        ))}
      </div>
    </div>
  );
}
