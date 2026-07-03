import type { PurchaseInitiateResponse } from "../api/types/billing";

declare global {
  interface Window {
    Razorpay?: new (options: Record<string, unknown>) => { open: () => void };
  }
}

const SCRIPT_URL = "https://checkout.razorpay.com/v1/checkout.js";

function loadScript(): Promise<void> {
  if (window.Razorpay) {
    return Promise.resolve();
  }
  return new Promise((resolve, reject) => {
    const script = document.createElement("script");
    script.src = SCRIPT_URL;
    script.onload = () => resolve();
    script.onerror = () => reject(new Error("Failed to load Razorpay checkout script"));
    document.body.appendChild(script);
  });
}

/** Opens the Razorpay checkout modal. The wallet is credited server-side once the webhook confirms payment. */
export async function openRazorpayCheckout(
  purchase: PurchaseInitiateResponse,
  customerName: string,
  customerEmail: string,
  onDismiss: () => void,
): Promise<void> {
  await loadScript();
  if (!window.Razorpay) {
    throw new Error("Razorpay checkout is unavailable");
  }
  const checkout = new window.Razorpay({
    key: purchase.pgKeyId,
    order_id: purchase.pgOrderId,
    amount: purchase.amountMinorUnits,
    currency: purchase.currency,
    name: "Legal Help Platform",
    prefill: { name: customerName, email: customerEmail },
    modal: { ondismiss: onDismiss },
  });
  checkout.open();
}
