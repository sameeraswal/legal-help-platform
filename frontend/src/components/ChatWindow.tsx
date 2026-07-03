import { useEffect, useRef, useState } from "react";
import { useChatSocket } from "../hooks/useChatSocket";
import type { ChatEvent, ChatMessage } from "../api/types/chat";
import { Button } from "./Button";
import { Input } from "./Input";
import { formatDuration } from "../utils/format";

interface Props {
  sessionId: number;
  initialMessages: ChatMessage[];
  onSessionEnded?: (reason: string) => void;
}

/** A local, client-side rendering of a message currently streaming in from the LLM. */
interface StreamingMessage {
  content: string;
}

export function ChatWindow({ sessionId, initialMessages, onSessionEnded }: Props) {
  const [messages, setMessages] = useState<ChatMessage[]>(initialMessages);
  const [streaming, setStreaming] = useState<StreamingMessage | null>(null);
  const [warning, setWarning] = useState<number | null>(null);
  const [ended, setEnded] = useState(false);
  const [input, setInput] = useState("");
  const bottomRef = useRef<HTMLDivElement>(null);

  const { connected, sendMessage } = useChatSocket(sessionId, (event: ChatEvent) => {
    switch (event.type) {
      case "MESSAGE":
        setMessages((prev) => [...prev, event.payload as ChatMessage]);
        break;
      case "TOKEN_DELTA":
        setStreaming((prev) => ({ content: (prev?.content ?? "") + (event.payload as string) }));
        break;
      case "TOKEN_COMPLETE":
        setStreaming((current) => {
          if (current) {
            setMessages((prev) => [
              ...prev,
              { id: Date.now(), sessionId, sender: "LLM", content: current.content, timestamp: new Date().toISOString() },
            ]);
          }
          return null;
        });
        break;
      case "SESSION_WARNING":
        setWarning((event.payload as { remainingSeconds: number }).remainingSeconds);
        break;
      case "SESSION_ENDED":
        setEnded(true);
        onSessionEnded?.((event.payload as { reason: string }).reason);
        break;
    }
  });

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, streaming]);

  function handleSend() {
    if (!input.trim()) return;
    setMessages((prev) => [
      ...prev,
      { id: Date.now(), sessionId, sender: "CUSTOMER", content: input, timestamp: new Date().toISOString() },
    ]);
    sendMessage(input);
    setInput("");
  }

  return (
    <div className="flex h-[70vh] flex-col rounded-lg border border-gray-200 bg-white">
      {warning !== null && !ended && (
        <div className="bg-amber-50 px-4 py-2 text-xs text-amber-800">
          Only {formatDuration(warning)} of chat time remaining — recharge your wallet to keep chatting.
        </div>
      )}
      {ended && <div className="bg-red-50 px-4 py-2 text-xs text-red-800">This session has ended.</div>}

      <div className="flex-1 space-y-3 overflow-y-auto p-4">
        {messages
          .filter((m) => m.sender !== "SYSTEM")
          .map((m) => (
            <div key={m.id} className={`flex ${m.sender === "CUSTOMER" ? "justify-end" : "justify-start"}`}>
              <div
                className={`max-w-[75%] rounded-lg px-3 py-2 text-sm ${
                  m.sender === "CUSTOMER" ? "bg-brand-600 text-white" : "bg-gray-100 text-gray-900"
                }`}
              >
                {m.content}
              </div>
            </div>
          ))}
        {streaming && (
          <div className="flex justify-start">
            <div className="max-w-[75%] rounded-lg bg-gray-100 px-3 py-2 text-sm text-gray-900">{streaming.content}</div>
          </div>
        )}
        <div ref={bottomRef} />
      </div>

      <div className="flex gap-2 border-t border-gray-200 p-3">
        <Input
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && handleSend()}
          placeholder={connected ? "Type your message..." : "Connecting..."}
          disabled={ended}
        />
        <Button onClick={handleSend} disabled={!connected || ended}>
          Send
        </Button>
      </div>
    </div>
  );
}
