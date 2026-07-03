import { useEffect, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { getActiveSession, getSessionMessages, listOnlineLawyers, startLawyerSession } from "../../api/chat";
import type { ChatMessage, ChatSession } from "../../api/types/chat";
import { ChatWindow } from "../../components/ChatWindow";
import { Card } from "../../components/Card";
import { Spinner } from "../../components/Spinner";

export function LawyerChatPage() {
  const [session, setSession] = useState<ChatSession | null | undefined>(undefined);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const { data: onlineLawyers } = useQuery({
    queryKey: ["online-lawyers"],
    queryFn: listOnlineLawyers,
    refetchInterval: 5000,
    enabled: session === null,
  });

  useEffect(() => {
    getActiveSession().then(async (active) => {
      if (active && active.counterpartType === "LAWYER") {
        setSession(active);
        setMessages(await getSessionMessages(active.id));
      } else {
        setSession(null);
      }
    });
  }, []);

  async function handleSelectLawyer(lawyerId: number) {
    const created = await startLawyerSession(lawyerId);
    setSession(created);
    setMessages([]);
  }

  if (session === undefined) {
    return (
      <div className="mx-auto max-w-2xl px-4 py-8">
        <Spinner />
      </div>
    );
  }

  if (session) {
    return (
      <div className="mx-auto max-w-2xl px-4 py-8">
        <h1 className="mb-6 text-xl font-semibold text-gray-900">Chat with your lawyer</h1>
        <ChatWindow sessionId={session.id} initialMessages={messages} />
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-2xl px-4 py-8">
      <h1 className="mb-6 text-xl font-semibold text-gray-900">Chat with an online lawyer</h1>
      {onlineLawyers && onlineLawyers.length === 0 && (
        <Card>
          <p className="text-sm text-gray-600">No lawyers are online right now. Please check back shortly.</p>
        </Card>
      )}
      <div className="space-y-2">
        {onlineLawyers?.map((lawyer) => (
          <button key={lawyer.lawyerId} onClick={() => handleSelectLawyer(lawyer.lawyerId)} className="w-full text-left">
            <Card className="hover:border-brand-300">
              <div className="flex items-center gap-2">
                <span className="h-2 w-2 rounded-full bg-green-500" />
                <span className="text-sm font-medium text-gray-900">{lawyer.name}</span>
              </div>
            </Card>
          </button>
        ))}
      </div>
    </div>
  );
}
