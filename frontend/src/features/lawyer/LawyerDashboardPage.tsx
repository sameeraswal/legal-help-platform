import { useEffect, useState } from "react";
import {
  getLawyerActiveSession,
  getLawyerSessionMessages,
  goOffline,
  goOnline,
  listMyLawyerSessions,
} from "../../api/chat";
import type { ChatMessage, ChatSession } from "../../api/types/chat";
import { ChatWindow } from "../../components/ChatWindow";
import { Button } from "../../components/Button";
import { Card } from "../../components/Card";
import { Spinner } from "../../components/Spinner";
import { formatIst } from "../../utils/format";

export function LawyerDashboardPage() {
  const [online, setOnline] = useState(false);
  const [session, setSession] = useState<ChatSession | null | undefined>(undefined);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [history, setHistory] = useState<ChatSession[]>([]);

  useEffect(() => {
    getLawyerActiveSession().then(async (active) => {
      setSession(active);
      if (active) {
        setMessages(await getLawyerSessionMessages(active.id));
      }
    });
    listMyLawyerSessions().then(setHistory);
  }, []);

  async function toggleOnline() {
    if (online) {
      await goOffline();
    } else {
      await goOnline();
    }
    setOnline(!online);
  }

  return (
    <div className="mx-auto max-w-3xl px-4 py-8">
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-xl font-semibold text-gray-900">Lawyer Dashboard</h1>
        <Button variant={online ? "secondary" : "primary"} onClick={toggleOnline}>
          {online ? "Go offline" : "Go online"}
        </Button>
      </div>

      {session === undefined && <Spinner />}

      {session && (
        <div className="mb-6">
          <h2 className="mb-2 text-sm font-semibold text-gray-900">Active session</h2>
          <ChatWindow sessionId={session.id} initialMessages={messages} onSessionEnded={() => setSession(null)} />
        </div>
      )}

      {session === null && (
        <Card className="mb-6">
          <p className="text-sm text-gray-600">
            {online ? "You're online. Waiting for a customer to start a chat." : "Go online to start receiving customer chats."}
          </p>
        </Card>
      )}

      <Card title="Past sessions">
        <div className="space-y-2">
          {history.map((s) => (
            <div key={s.id} className="flex items-center justify-between border-b border-gray-100 py-2 text-sm last:border-0">
              <span>Session #{s.id}</span>
              <span className="text-gray-500">{formatIst(s.startedAt)}</span>
              <span className="rounded-full bg-gray-100 px-2 py-0.5 text-xs">{s.status}</span>
            </div>
          ))}
        </div>
      </Card>
    </div>
  );
}
