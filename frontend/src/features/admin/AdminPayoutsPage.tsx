import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { decidePayout, listPendingPayouts } from "../../api/billing";
import { Card } from "../../components/Card";
import { Button } from "../../components/Button";
import { Input } from "../../components/Input";
import { Spinner } from "../../components/Spinner";
import { formatInr, formatIst } from "../../utils/format";

export function AdminPayoutsPage() {
  const queryClient = useQueryClient();
  const { data, isLoading } = useQuery({ queryKey: ["pending-payouts"], queryFn: () => listPendingPayouts() });
  const [bankRefs, setBankRefs] = useState<Record<number, string>>({});

  const decisionMutation = useMutation({
    mutationFn: ({ id, approve, bankReference }: { id: number; approve: boolean; bankReference?: string }) =>
      decidePayout(id, approve, bankReference),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["pending-payouts"] }),
  });

  return (
    <div className="mx-auto max-w-3xl px-4 py-8">
      <h1 className="mb-6 text-xl font-semibold text-gray-900">Payout Requests</h1>
      {isLoading && <Spinner />}
      {data && data.content.length === 0 && (
        <Card>
          <p className="text-sm text-gray-600">No pending payout requests.</p>
        </Card>
      )}
      <div className="space-y-3">
        {data?.content.map((request) => (
          <Card key={request.id}>
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-900">Lawyer #{request.lawyerId}</p>
                <p className="text-xs text-gray-500">
                  {formatInr(request.amountMinorUnits)} · requested {formatIst(request.createdAt)}
                </p>
              </div>
            </div>
            <div className="mt-3 flex items-center gap-2">
              <Input
                placeholder="Bank transfer reference"
                value={bankRefs[request.id] ?? ""}
                onChange={(e) => setBankRefs({ ...bankRefs, [request.id]: e.target.value })}
                className="max-w-xs"
              />
              <Button
                onClick={() => decisionMutation.mutate({ id: request.id, approve: true, bankReference: bankRefs[request.id] })}
                disabled={!bankRefs[request.id] || decisionMutation.isPending}
              >
                Approve & pay
              </Button>
              <Button variant="secondary" onClick={() => decisionMutation.mutate({ id: request.id, approve: false })}>
                Reject
              </Button>
            </div>
          </Card>
        ))}
      </div>
    </div>
  );
}
