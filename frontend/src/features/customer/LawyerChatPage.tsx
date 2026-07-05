import { useEffect, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { endSession, getActiveSession, getSessionMessages, listOnlineLawyers, startLawyerSession } from "../../api/chat";
import { ApiError } from "../../api/client";
import type { ChatMessage, ChatSession } from "../../api/types/chat";
import { ChatWindow } from "../../components/ChatWindow";
import { Button } from "../../components/Button";
import { Card } from "../../components/Card";
import { Spinner } from "../../components/Spinner";

export function LawyerChatPage() {
  const [session, setSession] = useState<ChatSession | null | undefined>(undefined);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  // A customer can only have one active chat session at a time (LLM or lawyer). If they already
  // have an active LLM session, starting a lawyer session will be rejected by the backend - track
  // it here so we can explain that instead of showing a lawyer list that's guaranteed to fail.
  const [blockingSession, setBlockingSession] = useState<ChatSession | null>(null);
  const [error, setError] = useState<string | null>(null);
  const { data: onlineLawyers } = useQuery({
    queryKey: ["online-lawyers"],
    queryFn: listOnlineLawyers,
    refetchInterval: 5000,
    enabled: session === null && blockingSession === null,
  });

  useEffect(() => {
    getActiveSession().then(async (active) => {
      if (active && active.counterpartType === "LAWYER") {
        setSession(active);
        setMessages(await getSessionMessages(active.id));
      } else {
        setSession(null);
        setBlockingSession(active);
      }
    });
  }, []);

  async function handleSelectLawyer(lawyerId: number) {
    setError(null);
    try {
      const created = await startLawyerSession(lawyerId);
      setSession(created);
      setMessages([]);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to start chat with lawyer");
    }
  }

  async function handleEndBlockingSession() {
    if (!blockingSession) {
      return;
    }
    setError(null);
    try {
      await endSession(blockingSession.id);
      setBlockingSession(null);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to end existing session");
    }
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

  if (blockingSession) {
    return (
      <div className="mx-auto max-w-2xl px-4 py-8">
        <h1 className="mb-6 text-xl font-semibold text-gray-900">Chat with an online lawyer</h1>
        {error && <p className="mb-4 text-sm text-red-600">{error}</p>}
        <Card>
          <p className="mb-4 text-sm text-gray-600">
            You already have an active chat session. You can only have one active chat at a time - finish or end it
            before starting a new chat with a lawyer.
          </p>
          <div className="flex gap-3">
            <Link to="/chat/llm">
              <Button variant="secondary">Go to your active chat</Button>
            </Link>
            <Button variant="primary" onClick={handleEndBlockingSession}>
              End that session
            </Button>
          </div>
        </Card>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-2xl px-4 py-8">
      <h1 className="mb-6 text-xl font-semibold text-gray-900">Chat with an online lawyer</h1>
      {error && <p className="mb-4 text-sm text-red-600">{error}</p>}
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
