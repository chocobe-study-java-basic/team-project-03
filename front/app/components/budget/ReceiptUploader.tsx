"use client";

import { useEffect, useRef, useState } from "react";
import { apiFetch } from "@/app/lib/api/client";

type Result = { amount: number | null; message: string };
export default function ReceiptUploader({ roomId, requestId, disabled, approvedAmount, onApply, onBusy, onFile, allowOcr }: {
  allowOcr: boolean; roomId: number; requestId: number; disabled: boolean; approvedAmount: number;
  onApply: (amount: string) => void; onBusy: (busy: boolean) => void; onFile: (file: File | null) => void;
}) {
  const [file, setFile] = useState<File | null>(null);
  const [preview, setPreview] = useState("");
  const [consent, setConsent] = useState(false);
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<Result | null>(null);
  const [error, setError] = useState("");
  const [applied, setApplied] = useState(false);
  const busy = useRef(false);
  useEffect(() => () => {
    if (preview) URL.revokeObjectURL(preview);
  }, [preview]);
  async function analyze() {
    if (!file || !consent || !allowOcr || disabled || busy.current) return;
    busy.current = true; setLoading(true); onBusy(true); setError(""); setResult(null); setApplied(false);
    try {
      const body = new FormData(); body.append("file", file);
      setResult(await apiFetch<Result>(`/rooms/${roomId}/budget/requests/${requestId}/receipt/analyze`, { method: "POST", body }));
    } catch (e) { setError(e instanceof Error ? e.message : "영수증을 인식하지 못했어요. 금액을 직접 입력해 주세요."); }
    finally { busy.current = false; setLoading(false); onBusy(false); }
  }
  return <section aria-labelledby="receipt-title" className="mt-5 rounded-2xl border border-indigo-100 bg-indigo-50/40 p-4">
    <div className="flex items-center justify-between gap-3"><h3 id="receipt-title" className="text-sm font-bold text-zinc-900">영수증 첨부 · 금액 인식</h3><span className="rounded-full bg-white px-2 py-1 text-[11px] font-semibold text-indigo-600">선택 사항</span></div>
    <p className="mt-2 text-xs leading-5 text-zinc-500">사진에서 총 결제 금액을 찾아드려요. 분석해도 정산은 자동으로 진행되지 않아요.</p>
    <label className="mt-3 block text-xs font-semibold text-zinc-700" htmlFor="receipt-file">영수증 사진 · JPEG / PNG · 1MB 이하</label>
    <input id="receipt-file" type="file" accept="image/jpeg,image/png" disabled={disabled || loading} className="mt-2 block w-full text-xs text-zinc-600 file:mr-3 file:rounded-lg file:border-0 file:bg-white file:px-3 file:py-2 file:font-semibold file:text-indigo-700 disabled:opacity-50" onChange={e => {
      const next = e.target.files?.[0]; setResult(null); setError(""); setApplied(false); setConsent(false);
      setFile(null); onFile(null); setPreview("");
      if (!next) return;
      if (next.size > 1_000_000 || !["image/jpeg", "image/png"].includes(next.type)) { setError("1MB 이하의 JPEG 또는 PNG 이미지를 선택해 주세요."); e.target.value = ""; return; }
      setFile(next); onFile(next); setPreview(URL.createObjectURL(next));
    }}/>
    {preview && <div className="mt-3 rounded-xl border border-zinc-200 bg-white p-2">{/* eslint-disable-next-line @next/next/no-img-element */}
      <img src={preview} alt="분석할 영수증 미리보기" className="mx-auto max-h-48 rounded-lg object-contain" /></div>}
    {!allowOcr && <p className="mt-3 text-xs text-zinc-500">이 통화는 사진 첨부만 지원해요. 금액은 직접 입력해 주세요.</p>}
    <label className="mt-3 flex items-start gap-2 text-xs leading-5 text-zinc-600"><input type="checkbox" checked={consent} onChange={e => setConsent(e.target.checked)} disabled={disabled || loading} className="mt-1"/>영수증 이미지가 외부 서비스 OCR.space로 전송됨을 확인했습니다. 카드번호 등 개인정보는 먼저 가려주세요.</label>
    <button type="button" onClick={analyze} disabled={!allowOcr || !file || !consent || loading || disabled} className="mt-3 w-full rounded-xl bg-white px-4 py-2.5 text-sm font-semibold text-indigo-700 ring-1 ring-indigo-200 disabled:cursor-not-allowed disabled:opacity-50">{loading ? "영수증을 읽고 있어요…" : "결제 금액 인식하기"}</button>
    <p className="mt-2 text-[11px] text-zinc-500">선택한 사진은 정산 완료 시 증빙으로 저장되어 모임 멤버가 조회할 수 있어요. OCR 없이 직접 금액을 입력해도 첨부돼요.</p>
    {error && <p role="alert" className="mt-3 rounded-lg bg-amber-50 p-3 text-xs leading-5 text-amber-800">{error}</p>}
    {result && <div role="status" className="mt-3 rounded-xl border border-indigo-100 bg-white p-3"><p className="text-xs leading-5 text-zinc-600">{result.message}</p>{result.amount !== null && <><p className="mt-2 text-xl font-extrabold text-zinc-900">{result.amount.toLocaleString("ko-KR")}원</p>{result.amount > approvedAmount && <p className="mt-2 text-xs text-red-600">승인 금액을 초과했어요. 영수증과 승인 내역을 확인해 주세요.</p>}<button type="button" disabled={disabled || result.amount > approvedAmount || applied} onClick={() => { onApply(String(result.amount)); setApplied(true); }} className="mt-3 rounded-lg bg-indigo-600 px-3 py-2 text-xs font-semibold text-white disabled:opacity-50">{applied ? "입력란에 적용했어요" : "확인한 금액 적용"}</button></>}</div>}
  </section>;
}
