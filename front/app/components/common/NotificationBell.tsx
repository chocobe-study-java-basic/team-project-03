"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { getNotifications, getUnreadNotificationCount, readNotification, type Notification } from "@/app/lib/api/notificationApi";
import { isLoggingOut } from "@/app/lib/api/tokenManager";

const labels = { BUDGET_REQUESTED: "예산 신청", BUDGET_APPROVED: "승인 완료", BUDGET_REJECTED: "반려", MEMBER_JOINED: "새 멤버" };
function dateLabel(value: string) {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "" : date.toLocaleString("ko-KR", { month: "short", day: "numeric", hour: "2-digit", minute: "2-digit" });
}

export default function NotificationBell() {
  const router = useRouter();
  const [open, setOpen] = useState(false);
  const [count, setCount] = useState(0);
  const [items, setItems] = useState<Notification[]>([]);
  const [nextPage, setNextPage] = useState<number | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [countError, setCountError] = useState(false);
  const [busyId, setBusyId] = useState<number | null>(null);
  const container = useRef<HTMLDivElement>(null);
  const button = useRef<HTMLButtonElement>(null);
  const generation = useRef(0);
  const actionBusy = useRef(false);
  const pageBusy = useRef(false);

  const refreshCount = useCallback(async (signal?: AbortSignal) => {
    if (isLoggingOut()) return;
    try {
      const result = await getUnreadNotificationCount(signal);
      if (!signal?.aborted) { setCount(result.count); setCountError(false); }
    } catch { if (!signal?.aborted) setCountError(true); }
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    let pending = false;
    const poll = async () => {
      if (document.visibilityState !== "visible" || pending || isLoggingOut()) return;
      pending = true;
      try { await refreshCount(controller.signal); } finally { pending = false; }
    };
    void poll();
    const timer = window.setInterval(() => { void poll(); }, 30_000);
    document.addEventListener("visibilitychange", poll);
    return () => { controller.abort(); window.clearInterval(timer); document.removeEventListener("visibilitychange", poll); };
  }, [refreshCount]);

  useEffect(() => {
    if (!open) return;
    const controller = new AbortController();
    const current = ++generation.current;
    let pending = false;
    const reload = async () => {
      if (pending || document.visibilityState !== "visible" || isLoggingOut()) return;
      pending = true;
      try {
        const result = await getNotifications(0, controller.signal);
        if (generation.current === current && !controller.signal.aborted) {
          setItems(result.notifications); setNextPage(result.nextPage); setError("");
        }
      } catch (e) {
        if (!controller.signal.aborted && generation.current === current) setError(e instanceof Error ? e.message : "알림을 불러오지 못했어요.");
      } finally {
        pending = false;
        if (!controller.signal.aborted && generation.current === current) setLoading(false);
      }
    };
    const outside = (e: PointerEvent) => { if (!container.current?.contains(e.target as Node)) setOpen(false); };
    const escape = (e: KeyboardEvent) => { if (e.key === "Escape") { setOpen(false); button.current?.focus(); } };
    void reload();
    void refreshCount(controller.signal);
    const timer = window.setInterval(() => { void reload(); }, 30_000);
    document.addEventListener("pointerdown", outside);
    document.addEventListener("keydown", escape);
    return () => { generation.current = current + 1; controller.abort(); window.clearInterval(timer); document.removeEventListener("pointerdown", outside); document.removeEventListener("keydown", escape); };
  }, [open, refreshCount]);

  async function more() {
    if (nextPage === null || pageBusy.current) return;
    const current = generation.current;
    pageBusy.current = true; setLoading(true);
    try {
      const result = await getNotifications(nextPage);
      if (current === generation.current) {
        setItems(previous => { const ids = new Set(previous.map(item => item.id)); return [...previous, ...result.notifications.filter(item => !ids.has(item.id))]; });
        setNextPage(result.nextPage); setError("");
      }
    } catch (e) { if (current === generation.current) setError(e instanceof Error ? e.message : "알림을 불러오지 못했어요."); }
    finally { pageBusy.current = false; if (current === generation.current) setLoading(false); }
  }

  async function select(item: Notification) {
    if (actionBusy.current) return;
    const current = generation.current;
    actionBusy.current = true; setBusyId(item.id);
    try {
      if (!item.readAt) await readNotification(item.id);
      if (current !== generation.current) return;
      await refreshCount();
      setOpen(false);
      router.push(`/rooms/${item.roomId}/${item.type === "MEMBER_JOINED" ? "members" : "budget-requests"}`);
    } catch (e) { if (current === generation.current) setError(e instanceof Error ? e.message : "알림을 처리하지 못했어요."); }
    finally { actionBusy.current = false; setBusyId(null); }
  }

  return <div className="relative" ref={container}>
    <button ref={button} type="button" aria-label={`알림${countError ? " 조회 실패" : `, 안 읽은 알림 ${count}개`}`} aria-expanded={open} aria-haspopup="dialog" onClick={() => {
      if (!open) { setLoading(true); setError(""); setItems([]); setNextPage(null); }
      setOpen(value => !value);
    }} className="relative flex h-10 w-10 items-center justify-center rounded-xl border border-zinc-200 bg-white text-zinc-600 transition hover:bg-indigo-50 hover:text-indigo-600 focus-visible:outline-2 focus-visible:outline-indigo-500">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true" className="h-5 w-5"><path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4"/></svg>
      {count > 0 && <span className="absolute -right-1.5 -top-1.5 min-w-5 rounded-full bg-indigo-600 px-1 text-[10px] font-bold leading-5 text-white">{count > 99 ? "99+" : count}</span>}
      {countError && <span className="absolute right-0 top-0 h-2 w-2 rounded-full bg-amber-500"/>}
    </button>
    {open && <section role="dialog" aria-label="내 알림" className="absolute right-0 top-12 z-50 w-[min(360px,calc(100vw-2rem))] overflow-hidden rounded-2xl border border-zinc-200 bg-white shadow-xl">
      <div className="flex items-center justify-between border-b border-zinc-100 px-4 py-4"><div><h2 className="text-base font-bold text-zinc-900">알림</h2><p className="mt-1 text-xs text-zinc-500">{countError ? "개수를 불러오지 못했어요" : `안 읽은 알림 ${count}개`}</p></div><button type="button" aria-label="알림 닫기" className="rounded-lg px-3 py-2 text-sm text-zinc-500 hover:bg-zinc-100" onClick={() => { setOpen(false); button.current?.focus(); }}>닫기</button></div>
      {error && <p role="alert" className="mx-3 mt-3 rounded-xl bg-amber-50 p-3 text-xs leading-5 text-amber-800">{error}</p>}
      <div className="max-h-[min(60vh,480px)] overflow-y-auto overscroll-contain">
        {loading && items.length === 0 ? <p role="status" className="px-4 py-10 text-center text-sm text-zinc-500">알림을 불러오는 중…</p> : items.length === 0 && !error ? <div className="px-5 py-10 text-center"><p className="font-semibold text-zinc-700">아직 도착한 알림이 없어요</p><p className="mt-2 text-xs leading-5 text-zinc-500">예산 신청 결과와 모임 소식을 여기서 확인할 수 있어요.</p></div> : <ul>{items.map(item => <li key={item.id} className="border-b border-zinc-100 last:border-0"><button type="button" disabled={busyId !== null} onClick={() => { void select(item); }} className={`w-full px-4 py-4 text-left transition hover:bg-zinc-50 disabled:opacity-60 ${item.readAt ? "bg-white" : "bg-indigo-50/50"}`}><div className="flex items-center justify-between gap-2"><span className="truncate text-xs font-semibold text-indigo-600">{item.roomName} · {labels[item.type]}</span>{!item.readAt && <span className="h-1.5 w-1.5 shrink-0 rounded-full bg-indigo-500" aria-label="안 읽음"/>}</div><p className="mt-2 break-words text-sm leading-6 text-zinc-800">{item.message}</p><time className="mt-2 block text-[11px] text-zinc-400" dateTime={item.createdAt}>{busyId === item.id ? "처리 중…" : dateLabel(item.createdAt)}</time></button></li>)}</ul>}
        {nextPage !== null && <button type="button" disabled={loading} onClick={() => { void more(); }} className="w-full px-4 py-3 text-xs font-semibold text-indigo-600 disabled:opacity-50">{loading ? "불러오는 중…" : "이전 알림 더 보기"}</button>}
      </div>
      <p className="border-t border-zinc-100 px-4 py-3 text-[11px] text-zinc-400">알림을 누르면 읽음 처리 후 관련 화면으로 이동해요.</p>
    </section>}
  </div>;
}
