import { useQuery } from "@tanstack/react-query";
import { listAuditLogs } from "../../api/admin";
import { Card } from "../../components/Card";
import { Spinner } from "../../components/Spinner";
import { formatIst } from "../../utils/format";

export function AdminAuditLogsPage() {
  const { data, isLoading } = useQuery({ queryKey: ["audit-logs"], queryFn: () => listAuditLogs() });

  return (
    <div className="mx-auto max-w-4xl px-4 py-8">
      <h1 className="mb-6 text-xl font-semibold text-gray-900">Audit Log</h1>
      {isLoading && <Spinner />}
      <div className="space-y-2">
        {data?.content.map((entry) => (
          <Card key={entry.id}>
            <div className="flex items-center justify-between text-sm">
              <div>
                <p className="font-medium text-gray-900">
                  {entry.action} — {entry.entity}
                  {entry.entityId ? ` #${entry.entityId}` : ""}
                </p>
                <p className="text-xs text-gray-500">
                  Actor #{entry.actorId ?? "system"} ({entry.actorRole ?? "-"}) · {formatIst(entry.createdAt)}
                </p>
              </div>
            </div>
          </Card>
        ))}
      </div>
    </div>
  );
}
