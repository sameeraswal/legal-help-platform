import { useEffect, useState } from "react";
import { getActiveSession, getSessionMessages, startLlmSession } from "../../api/chat";
import type { ChatMessage, ChatSession } from "../../api/types/chat";
import { ChatWindow } from "../../components/ChatWindow";
import { Button } from "../../components/Button";
import { Spinner } from "../../components/Spinner";

export function LlmChatPage() {
  const [session, setSession] = useState<ChatSession | null | undefined>(undefined);
  const [messages, setMessages] = useState<ChatMessage[]>([]);

  useEffect(() => {
    getActiveSession().then(async (active) => {
      if (active && active.counterpartType === "LLM") {
        setSession(active);
        setMessages(await getSessionMessages(active.id));
      } else {
        setSession(null);
      }
    });
  }, []);

  async function handleStart() {
    const created = await startLlmSession();
    setSession(created);
    setMessages([]);
  }

  return (
    <div className="mx-auto max-w-2xl px-4 py-8">
      <h1 className="mb-6 text-xl font-semibold text-gray-900">AI Legal Assistant</h1>

      {session === undefined && <Spinner />}

      {session === null && (
        <div className="text-center">
          <p className="mb-4 text-sm text-gray-600">Start a chat with our AI legal assistant.</p>
          <Button onClick={handleStart}>Start chat</Button>
        </div>
      )}

      {session && <ChatWindow sessionId={session.id} initialMessages={messages} />}
    </div>
  );
}
