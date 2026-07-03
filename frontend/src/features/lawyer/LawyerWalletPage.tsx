import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { getLawyerWalletBalance, listMyPayoutRequests, requestPayout } from "../../api/billing";
import { Card } from "../../components/Card";
import { Button } from "../../components/Button";
import { Spinner } from "../../components/Spinner";
import { formatInr, formatIst } from "../../utils/format";
import { ApiError } from "../../api/client";

export function LawyerWalletPage() {
  const [error, setError] = useState<string | null>(null);
  const [requesting, setRequesting] = useState(false);
  const balanceQuery = useQuery({ queryKey: ["lawyer-wallet-balance"], queryFn: getLawyerWalletBalance });
  const payoutsQuery = useQuery({ queryKey: ["my-payout-requests"], queryFn: () => listMyPayoutRequests() });

  async function handleRequestPayout() {
    setError(null);
    setRequesting(true);
    try {
      await requestPayout();
      await payoutsQuery.refetch();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to request payout");
    } finally {
      setRequesting(false);
    }
  }

  return (
    <div className="mx-auto max-w-2xl px-4 py-8">
      <h1 className="mb-6 text-xl font-semibold text-gray-900">Earnings & Payouts</h1>

      <Card title="Wallet balance" className="mb-6">
        {balanceQuery.isLoading ? (
          <Spinner />
        ) : (
          <p className="text-2xl font-semibold text-gray-900">{formatInr(balanceQuery.data?.paidSecondsRemaining ?? 0)}</p>
        )}
        <Button className="mt-4" onClick={handleRequestPayout} disabled={requesting}>
          {requesting ? "Requesting..." : "Request payout"}
        </Button>
        {error && <p className="mt-2 text-sm text-red-600">{error}</p>}
      </Card>

      <Card title="Payout requests">
        <div className="space-y-2">
          {payoutsQuery.data?.content.map((p) => (
            <div key={p.id} className="flex items-center justify-between border-b border-gray-100 py-2 text-sm last:border-0">
              <span>{formatInr(p.amountMinorUnits)}</span>
              <span className="text-gray-500">{formatIst(p.createdAt)}</span>
              <span className="rounded-full bg-gray-100 px-2 py-0.5 text-xs">{p.status}</span>
            </div>
          ))}
        </div>
      </Card>
    </div>
  );
}
