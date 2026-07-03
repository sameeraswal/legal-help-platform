import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { getWalletBalance, getWalletTransactions, initiatePurchase, listPlans } from "../../api/billing";
import { useAuth } from "../../hooks/useAuth";
import { Card } from "../../components/Card";
import { Button } from "../../components/Button";
import { Spinner } from "../../components/Spinner";
import { formatDuration, formatInr, formatIst } from "../../utils/format";
import { openRazorpayCheckout } from "../../utils/razorpay";

export function WalletPage() {
  const { user } = useAuth();
  const [purchasing, setPurchasing] = useState<number | null>(null);
  const balanceQuery = useQuery({ queryKey: ["wallet-balance"], queryFn: getWalletBalance });
  const plansQuery = useQuery({ queryKey: ["plans"], queryFn: listPlans });
  const transactionsQuery = useQuery({ queryKey: ["wallet-transactions"], queryFn: () => getWalletTransactions() });

  async function handlePurchase(planId: number) {
    if (!user) return;
    setPurchasing(planId);
    try {
      const purchase = await initiatePurchase(planId);
      await openRazorpayCheckout(purchase, user.name, user.email, () => setPurchasing(null));
    } finally {
      setPurchasing(null);
    }
  }

  return (
    <div className="mx-auto max-w-3xl px-4 py-8">
      <h1 className="mb-6 text-xl font-semibold text-gray-900">Wallet</h1>

      <Card title="Balance" className="mb-6">
        {balanceQuery.isLoading ? (
          <Spinner />
        ) : (
          <p className="text-2xl font-semibold text-gray-900">
            {formatDuration(balanceQuery.data?.totalSecondsAvailable ?? 0)}
            <span className="ml-2 text-sm font-normal text-gray-500">available chat time</span>
          </p>
        )}
      </Card>

      <Card title="Recharge plans" className="mb-6">
        <div className="grid gap-3 sm:grid-cols-3">
          {plansQuery.data?.map((plan) => (
            <div key={plan.id} className="rounded-md border border-gray-200 p-3 text-center">
              <p className="text-sm font-medium text-gray-900">{plan.name}</p>
              <p className="mt-1 text-lg font-semibold text-brand-700">{formatInr(plan.priceMinorUnits)}</p>
              <p className="text-xs text-gray-500">{formatDuration(plan.seconds)}</p>
              <Button className="mt-3 w-full" onClick={() => handlePurchase(plan.id)} disabled={purchasing === plan.id}>
                {purchasing === plan.id ? "Opening checkout..." : "Buy"}
              </Button>
            </div>
          ))}
        </div>
      </Card>

      <Card title="Transaction history">
        <div className="space-y-2">
          {transactionsQuery.data?.content.map((entry) => (
            <div key={entry.id} className="flex items-center justify-between border-b border-gray-100 py-2 text-sm last:border-0">
              <div>
                <p className="font-medium text-gray-900">{entry.entryType}</p>
                <p className="text-xs text-gray-500">{formatIst(entry.createdAt)}</p>
              </div>
              <span className={entry.secondsDelta && entry.secondsDelta < 0 ? "text-red-600" : "text-green-600"}>
                {entry.secondsDelta != null ? formatDuration(Math.abs(entry.secondsDelta)) : formatInr(Math.abs(entry.amountDeltaMinorUnits ?? 0))}
              </span>
            </div>
          ))}
        </div>
      </Card>
    </div>
  );
}
