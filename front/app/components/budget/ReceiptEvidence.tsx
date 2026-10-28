"use client";
import { useEffect, useState } from "react";
import { apiFetch } from "@/app/lib/api/client";

type Evidence = { contentType: string; base64: string };
export default function ReceiptEvidence({ roomId, changeId }: { roomId: number; changeId: number }) {
  const [receipt, setReceipt] = useState<Evidence | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  useEffect(() => {
    let active = true;
    apiFetch<Evidence | null>(`/rooms/${roomId}/budget/changes/${changeId}/receipt`)
      .then(value => { if (active) setReceipt(value); })
      .catch(e => { if (active) setError(e instanceof Error ? e.message : "증빙을 불러오지 못했습니다."); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [roomId, changeId]);
  return <section className="mt-5 rounded-2xl border border-zinc-200 bg-white p-5" aria-label="영수증 증빙">
    <h2 className="text-base font-bold text-zinc-900">영수증 증빙</h2>
    <p className="mt-1 text-xs text-zinc-500">정산 시 첨부한 원본입니다. 모임 멤버만 확인할 수 있어요.</p>
    {loading ? <p role="status" className="mt-4 text-sm text-zinc-500">증빙을 불러오는 중…</p> : error ? <p role="alert" className="mt-4 text-sm text-red-600">{error}</p> : receipt ? <div className="mt-4 rounded-xl bg-zinc-50 p-3">{/* eslint-disable-next-line @next/next/no-img-element */}
      <img src={`data:${receipt.contentType};base64,${receipt.base64}`} alt="저장된 정산 영수증 증빙" className="mx-auto h-auto max-h-[700px] max-w-full object-contain"/></div> : <p className="mt-4 text-sm text-zinc-500">첨부된 영수증이 없습니다.</p>}
  </section>;
}
