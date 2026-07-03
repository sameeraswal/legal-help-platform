import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { listTransactionsAdmin, refundPayment } from "../../api/billing";
import type { PaymentStatus } from "../../api/types/billing";
import { Card } from "../../components/Card";
import { Button } from "../../components/Button";
import { Spinner } from "../../components/Spinner";
import { formatInr, formatIst } from "../../utils/format";

const STATUSES: (PaymentStatus | "ALL")[] = ["ALL", "PENDING", "SUCCESS", "FAILED", "REFUNDED"];

export function AdminTransactionsPage() {
  const [status, setStatus] = useState<PaymentStatus | "ALL">("ALL");
  const queryClient = useQueryClient();
  const { data, isLoading } = useQuery({
    queryKey: ["admin-transactions", status],
    queryFn: () => listTransactionsAdmin(status === "ALL" ? undefined : status),
  });

  const refundMutation = useMutation({
    mutationFn: (paymentId: number) => refundPayment(paymentId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["admin-transactions"] }),
  });

  return (
    <div className="mx-auto max-w-4xl px-4 py-8">
      <h1 className="mb-6 text-xl font-semibold text-gray-900">Transactions</h1>

      <div className="mb-4 flex gap-2">
        {STATUSES.map((s) => (
          <button
            key={s}
            onClick={() => setStatus(s)}
            className={`rounded-md px-3 py-1 text-xs font-medium ${
              status === s ? "bg-brand-600 text-white" : "bg-gray-100 text-gray-600"
            }`}
          >
            {s}
          </button>
        ))}
      </div>

      {isLoading && <Spinner />}

      <div className="space-y-2">
        {data?.content.map((payment) => (
          <Card key={payment.id}>
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-900">{formatInr(payment.amountMinorUnits)}</p>
                <p className="text-xs text-gray-500">
                  Order {payment.orderId} · {formatIst(payment.createdAt)}
                </p>
              </div>
              <div className="flex items-center gap-3">
                <span className="rounded-full bg-gray-100 px-2 py-0.5 text-xs">{payment.status}</span>
                {payment.status === "SUCCESS" && (
                  <Button
                    variant="secondary"
                    onClick={() => refundMutation.mutate(payment.id)}
                    disabled={refundMutation.isPending}
                  >
                    Refund
                  </Button>
                )}
              </div>
            </div>
          </Card>
        ))}
      </div>
    </div>
  );
}
