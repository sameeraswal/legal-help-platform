import { useEffect, useState } from "react";
import { getAppConfig, updateGeneralConfig, updatePaymentGatewayConfig } from "../../api/admin";
import { Card } from "../../components/Card";
import { Button } from "../../components/Button";
import { Input } from "../../components/Input";
import { Spinner } from "../../components/Spinner";

export function AdminConfigPage() {
  const [loading, setLoading] = useState(true);
  const [freeMinutes, setFreeMinutes] = useState(30);
  const [payoutThreshold, setPayoutThreshold] = useState(0);
  const [pgConfigured, setPgConfigured] = useState(false);
  const [keyId, setKeyId] = useState("");
  const [keySecret, setKeySecret] = useState("");
  const [webhookSecret, setWebhookSecret] = useState("");
  const [savedMessage, setSavedMessage] = useState<string | null>(null);

  useEffect(() => {
    getAppConfig().then((config) => {
      setFreeMinutes(config.freeMinutes);
      setPayoutThreshold(config.payoutThresholdMinorUnits);
      setPgConfigured(config.pgConfigured);
      setLoading(false);
    });
  }, []);

  async function saveGeneral() {
    await updateGeneralConfig(freeMinutes, payoutThreshold);
    setSavedMessage("General settings saved.");
  }

  async function savePg() {
    await updatePaymentGatewayConfig(keyId, keySecret, webhookSecret);
    setPgConfigured(true);
    setKeyId("");
    setKeySecret("");
    setWebhookSecret("");
    setSavedMessage("Payment gateway credentials saved.");
  }

  if (loading) {
    return (
      <div className="mx-auto max-w-2xl px-4 py-8">
        <Spinner />
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-2xl px-4 py-8">
      <h1 className="mb-6 text-xl font-semibold text-gray-900">Platform Configuration</h1>
      {savedMessage && <p className="mb-4 text-sm text-green-600">{savedMessage}</p>}

      <Card title="General" className="mb-6">
        <div className="grid gap-3 sm:grid-cols-2">
          <label className="text-sm text-gray-700">
            Free chat minutes for new customers
            <Input type="number" value={freeMinutes} onChange={(e) => setFreeMinutes(Number(e.target.value))} className="mt-1" />
          </label>
          <label className="text-sm text-gray-700">
            Lawyer payout threshold (paise)
            <Input
              type="number"
              value={payoutThreshold}
              onChange={(e) => setPayoutThreshold(Number(e.target.value))}
              className="mt-1"
            />
          </label>
        </div>
        <Button className="mt-3" onClick={saveGeneral}>
          Save
        </Button>
      </Card>

      <Card title="Payment gateway (Razorpay)">
        <p className="mb-3 text-xs text-gray-500">
          {pgConfigured ? "Credentials are configured (encrypted at rest)." : "Not yet configured — using environment defaults."}
        </p>
        <div className="grid gap-3">
          <Input placeholder="Key ID" value={keyId} onChange={(e) => setKeyId(e.target.value)} />
          <Input placeholder="Key Secret" type="password" value={keySecret} onChange={(e) => setKeySecret(e.target.value)} />
          <Input
            placeholder="Webhook Secret"
            type="password"
            value={webhookSecret}
            onChange={(e) => setWebhookSecret(e.target.value)}
          />
        </div>
        <Button className="mt-3" onClick={savePg} disabled={!keyId || !keySecret || !webhookSecret}>
          Save credentials
        </Button>
      </Card>
    </div>
  );
}
