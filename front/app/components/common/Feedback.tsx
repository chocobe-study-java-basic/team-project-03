"use client";

import { useEffect, useRef, useState } from "react";

const feedbackEvent = "budzet:feedback";
export function notify(message: string) {
  if (typeof window !== "undefined")
    window.dispatchEvent(new CustomEvent(feedbackEvent, { detail: message }));
}

export default function Feedback() {
  const [messages, setMessages] = useState<{ id: number; text: string }[]>([]);
  const nextId = useRef(0);
  const timers = useRef(new Set<ReturnType<typeof setTimeout>>());
  useEffect(() => {
    const pending = timers.current;
    const receive = (event: Event) => {
      const text = (event as CustomEvent<string>).detail;
      const id = ++nextId.current;
      setMessages((current) => [...current.slice(-2), { id, text }]);
      const timer = setTimeout(() => {
        setMessages((current) =>
          current.filter((message) => message.id !== id),
        );
        pending.delete(timer);
      }, 6500);
      pending.add(timer);
    };
    window.addEventListener(feedbackEvent, receive);
    return () => {
      window.removeEventListener(feedbackEvent, receive);
      pending.forEach(clearTimeout);
      pending.clear();
    };
  }, []);
  return (
    <div className="feedback-stack" aria-live="polite" aria-atomic="false">
      {messages.map((message) => (
        <div key={message.id} className="feedback-message">
          <span aria-hidden="true" className="feedback-dot" />
          <p className="flex-1 text-sm leading-6">{message.text}</p>
          <button
            type="button"
            aria-label="알림 닫기"
            onClick={() =>
              setMessages((current) =>
                current.filter((item) => item.id !== message.id),
              )
            }
            className="rounded-lg px-2 py-1 text-zinc-500"
          >
            ×
          </button>
        </div>
      ))}
    </div>
  );
}
