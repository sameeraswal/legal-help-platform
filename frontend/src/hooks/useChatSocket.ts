import { useEffect, useRef, useState } from "react";
import { Client, type IMessage } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { API_BASE_URL, getAccessToken } from "../api/client";
import type { ChatEvent } from "../api/types/chat";

/** STOMP-over-SockJS connection for one chat session's event topic. */
export function useChatSocket(sessionId: number | null, onEvent: (event: ChatEvent) => void) {
  const clientRef = useRef<Client | null>(null);
  const [connected, setConnected] = useState(false);
  const onEventRef = useRef(onEvent);
  onEventRef.current = onEvent;

  useEffect(() => {
    if (sessionId == null) {
      return;
    }

    const client = new Client({
      webSocketFactory: () => new SockJS(`${API_BASE_URL}/ws`) as unknown as WebSocket,
      connectHeaders: { Authorization: `Bearer ${getAccessToken() ?? ""}` },
      reconnectDelay: 3000,
      onConnect: () => {
        setConnected(true);
        client.subscribe(`/topic/sessions/${sessionId}`, (message: IMessage) => {
          onEventRef.current(JSON.parse(message.body) as ChatEvent);
        });
      },
      onDisconnect: () => setConnected(false),
      onStompError: () => setConnected(false),
    });

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
      clientRef.current = null;
    };
  }, [sessionId]);

  function sendMessage(content: string) {
    if (sessionId == null || !clientRef.current?.connected) {
      return;
    }
    clientRef.current.publish({
      destination: "/app/chat.send",
      body: JSON.stringify({ sessionId, content }),
    });
  }

  return { connected, sendMessage };
}
