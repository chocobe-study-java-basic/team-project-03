"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { getRoom, type Room } from "../../../../lib/api/roomsApi";
import { ApiError } from "../../../../lib/api/types";
import {
  getBudgetHistory,
  updateBudget,
  type BudgetHistoryItem,
  type BudgetType,
} from "../../../../lib/api/budgetApi";

function formatBudget(amount: number, currency: string) {
  const formatted = new Intl.NumberFormat("ko-KR").format(amount);
  return currency === "KRW" ? `${formatted}원` : `${currency} ${formatted}`;
}

export default function BudgetPage() {
  const { roomId: value } = useParams<{ roomId?: string }>();
  const roomId = typeof value === "string" && /^\d+$/.test(value) ? Number(value) : 0;
  if (!Number.isSafeInteger(roomId) || roomId <= 0) {
    return <section><h1 className="text-xl font-bold">잘못된 모임 주소입니다.</h1><Link href="/rooms" className="text-indigo-600">내 모임으로 돌아가기</Link></section>;
  }
  return <BudgetContent key={roomId} roomId={roomId} />;
}

function BudgetContent({ roomId }: { roomId: number }) {
  const [room, setRoom] = useState<Room | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [redirectingToLogin, setRedirectingToLogin] = useState(false);
  const [budgetInput, setBudgetInput] = useState("");
  const [budgetType, setBudgetType] = useState<BudgetType>("INCREASE");
  const [budgetReason, setBudgetReason] = useState("");
  const [budgetError, setBudgetError] = useState<string | null>(null);
  const [savingBudget, setSavingBudget] = useState(false);
  const [budgetHistory, setBudgetHistory] = useState<BudgetHistoryItem[]>([]);
  const [loadingBudgetHistory, setLoadingBudgetHistory] = useState(true);
  const [budgetHistoryError, setBudgetHistoryError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const loadData = useCallback(async (isCancelled: () => boolean = () => false) => {
    setLoading(true);
    setLoadingBudgetHistory(true);
    setError(null);
    setBudgetHistoryError(null);
    const [roomResult, historyResult] = await Promise.allSettled([
      getRoom(roomId),
      getBudgetHistory(roomId),
    ]);
    if (isCancelled()) return;
    for (const result of [roomResult, historyResult]) {
      if (result.status === "rejected" && result.reason instanceof ApiError && result.reason.status === 401) {
        setRedirectingToLogin(true);
      }
    }
    if (roomResult.status === "fulfilled") {
      setRoom(roomResult.value);
    } else {
      setError(roomResult.reason instanceof Error ? roomResult.reason.message : "예산 정보를 불러오지 못했습니다.");
    }
    if (historyResult.status === "fulfilled") {
      setBudgetHistory(historyResult.value.history);
    } else {
      setBudgetHistoryError(historyResult.reason instanceof Error ? historyResult.reason.message : "예산 변경 내역을 불러오지 못했습니다.");
    }
    setLoading(false);
    setLoadingBudgetHistory(false);
  }, [roomId]);

  useEffect(() => {
    let cancelled = false;
    void Promise.resolve().then(() => {
      if (!cancelled) return loadData(() => cancelled);
    });
    return () => { cancelled = true; };
  }, [loadData]);

  if (loading || redirectingToLogin) {
    return <p className="py-12 text-center text-sm text-zinc-500">{redirectingToLogin ? "로그인 페이지로 이동하고 있습니다." : "예산 정보를 불러오는 중입니다."}</p>;
  }
  if (error || !room) {
    return <section className="rounded-2xl border border-red-200 bg-white p-8 text-center">
      {successMessage && <p role="status" className="mb-3 text-emerald-700">{successMessage}</p>}
      <p role="alert" className="text-red-600">{error ?? "예산 정보를 불러오지 못했습니다."}</p>
      <button type="button" onClick={() => void loadData()} className="mt-4 rounded-lg border px-4 py-2">다시 불러오기</button>
    </section>;
  }

  const usedBudget = Math.max(0, room.totalBudget - room.availableBudget);
  const usedRate = room.totalBudget > 0 ? Math.max(0, Math.min(100, (usedBudget / room.totalBudget) * 100)) : 0;
  const availableRate = 100 - usedRate;

  const filteredBudgetHistory = budgetHistory.filter(
      (history) =>
          history.type === "INCREASE" ||
          history.type === "DECREASE",
  );

  const cancelEditingBudget = () => {
    if (savingBudget) return;
    setBudgetInput("");
    setBudgetType("INCREASE");
    setBudgetReason("");
    setBudgetError(null);
  };

  const handleBudgetInputChange = (
      event: React.ChangeEvent<HTMLInputElement>,
  ) => {
    const value = event.target.value.replace(/[^\d]/g, "");

    if (!value) {
      setBudgetInput("");
      return;
    }

    setBudgetInput(Number(value).toLocaleString("ko-KR"));

    if (budgetError) {
      setBudgetError(null);
    }
  };

  const submitBudget = async (
      event: React.FormEvent<HTMLFormElement>,
  ) => {
    event.preventDefault();
    if (savingBudget) return;

    const amount = Number(budgetInput.replace(/,/g, ""));
    const trimmedReason = budgetReason.trim();

    if (!Number.isSafeInteger(amount) || amount <= 0) {
      setBudgetError("변경할 예산 금액을 입력해 주세요.");
      return;
    }

    if (
        budgetType === "DECREASE" &&
        amount > room.availableBudget
    ) {
      setBudgetError(
          `예산 감소 금액은 현재 사용 가능한 예산 ${formatBudget(
              room.availableBudget,
              room.currency,
          )}을 초과할 수 없습니다.`,
      );
      return;
    }

    if (trimmedReason.length > 20) {
      setBudgetError("변동 사유는 20자 이내로 입력해 주세요.");
      return;
    }

    setSuccessMessage(null);
    setSavingBudget(true);
    setBudgetError(null);

    try {
      await updateBudget(room.id, {
        totalBudget: amount,
        budgetType,
        reason: trimmedReason || undefined,
      });

      setSuccessMessage("예산이 저장되었습니다.");
      setBudgetInput("");
      setBudgetReason("");
      setBudgetType("INCREASE");
      await loadData();
    } catch (caughtError) {
      if (
          caughtError instanceof ApiError &&
          caughtError.status === 401
      ) {
        setRedirectingToLogin(true);
        return;
      }

      if (
          caughtError instanceof ApiError &&
          caughtError.status === 403
      ) {
        setBudgetError("방장과 운영자만 예산을 수정할 수 있습니다.");
        return;
      }

      setBudgetError(
          caughtError instanceof ApiError
              ? caughtError.message
              : "예산을 수정하지 못했습니다. 잠시 후 다시 시도해 주세요.",
      );
    } finally {
      setSavingBudget(false);
    }
  };

  return (
    <section className="mx-auto w-full max-w-5xl">
      <header><h1 className="text-2xl font-bold tracking-tight">예산 변경</h1><p className="mt-1 text-sm text-zinc-500">{room.name}의 예산을 변경하고 내역을 확인하세요.</p></header>
      {successMessage && <p role="status" className="mt-5 rounded-xl bg-emerald-50 p-4 text-sm text-emerald-700">{successMessage}</p>}
        <section className="mt-10 rounded-2xl border border-zinc-200 bg-white p-6 shadow-sm sm:p-8" aria-labelledby="budget-title">
          <div className="mt-1 flex items-center gap-3">
            <h2 id="budget-title" className="text-xl font-bold">
              {room.name}의 예산
            </h2>

          </div>
          <dl className="mt-8 grid gap-6 sm:grid-cols-3"><div><dt className="text-sm text-zinc-500">현재 잔액</dt><dd className="mt-2 text-2xl font-extrabold tracking-tight">{formatBudget(room.totalBudget, room.currency)}</dd></div><div><dt className="text-sm text-zinc-500">선점 금액</dt><dd className="mt-2 text-2xl font-extrabold tracking-tight text-indigo-600">{formatBudget(usedBudget, room.currency)}</dd></div><div><dt className="text-sm text-zinc-500">사용 가능</dt><dd className="mt-2 text-2xl font-extrabold tracking-tight text-emerald-600">{formatBudget(room.availableBudget, room.currency)}</dd></div></dl>
          <div className="mt-9"><div className="flex h-3 overflow-hidden rounded-full bg-zinc-100" aria-label={`${room.name} 예산 사용 현황`}><span className="bg-indigo-500" style={{ width: `${usedRate}%` }} /><span className="bg-emerald-100" style={{ width: `${availableRate}%` }} /></div><div className="mt-3 flex flex-wrap gap-x-4 gap-y-2 text-sm text-zinc-500"><span className="flex items-center gap-1.5"><i className="h-2.5 w-2.5 rounded-full bg-indigo-500" />선점 금액</span><span className="flex items-center gap-1.5"><i className="h-2.5 w-2.5 rounded-full bg-emerald-300" />사용 가능</span></div></div>
        </section>
            <section className="mt-5 rounded-2xl border border-indigo-200 bg-white p-6 shadow-sm sm:p-8">
              <div className="flex items-start justify-between gap-4"><div><p className="text-sm font-medium text-indigo-600">예산 수정</p><h3 className="mt-1 text-xl font-bold">모임 예산 변경</h3><p className="mt-2 text-sm text-zinc-500">현재 현재 잔액은{" "}<span className="font-semibold text-zinc-700">{formatBudget(room.totalBudget, room.currency)}</span>입니다.</p></div></div><form onSubmit={(event) => void submitBudget(event)} className="mt-7 space-y-5">
                {/* 변경 유형 */}<div><label className="block text-sm font-semibold text-zinc-800">예산 변경 유형</label><div className="mt-2 grid grid-cols-2 gap-2"><button type="button" onClick={() => {setBudgetType("INCREASE");setBudgetError(null);}} disabled={savingBudget} className={`rounded-xl border px-4 py-3 text-sm font-semibold transition-colors ${budgetType === "INCREASE" ? "border-emerald-300 bg-emerald-50 text-emerald-700" : "border-zinc-200 bg-white text-zinc-600 hover:bg-zinc-50"}`}><span className="mr-1.5">＋</span>예산 증가</button><button type="button" onClick={() => {setBudgetType("DECREASE");setBudgetError(null);}} disabled={savingBudget} className={`rounded-xl border px-4 py-3 text-sm font-semibold transition-colors ${budgetType === "DECREASE" ? "border-rose-300 bg-rose-50 text-rose-700" : "border-zinc-200 bg-white text-zinc-600 hover:bg-zinc-50"}`}><span className="mr-1.5">－</span>예산 감소</button></div></div>
                {/* 변경 금액 */}<div><label htmlFor="budget-input" className="block text-sm font-semibold text-zinc-800">변경 금액</label><div className="relative mt-2"><input id="budget-input" type="text" inputMode="numeric" value={budgetInput} onChange={handleBudgetInputChange} disabled={savingBudget} placeholder="예: 100,000" className="h-12 w-full rounded-xl border border-indigo-200 bg-indigo-50/40 px-4 pr-16 text-lg font-bold text-zinc-900 outline-none transition focus:ring-2 focus:ring-indigo-100"/><span className="pointer-events-none absolute inset-y-0 right-4 flex items-center text-sm font-semibold text-zinc-500">{room.currency}</span></div><div className="mt-2 flex flex-wrap items-center gap-x-2 gap-y-1 text-xs text-zinc-500"><span>현재{" "}<span className="font-semibold text-zinc-700">{formatBudget(room.totalBudget, room.currency)}</span></span><span className="text-zinc-400">에서</span><span className={`font-semibold ${budgetType === "INCREASE" ? "text-emerald-600" : "text-rose-600"}`}>{budgetInput ? `${budgetType === "INCREASE" ? "+" : "-"}${formatBudget(Number(budgetInput.replace(/,/g, "")), room.currency,)}` : "-"}</span><span className="text-zinc-400">→</span><span className="font-semibold text-zinc-900">{budgetInput ? formatBudget(budgetType === "INCREASE" ? room.totalBudget + Number(budgetInput.replace(/,/g, "")) : room.totalBudget - Number(budgetInput.replace(/,/g, "")), room.currency,) : "-"}</span></div></div>
                {/* 변동 사유 */}<div><label htmlFor="budget-reason" className="block text-sm font-semibold text-zinc-800">변동 사유<span className="ml-1 text-xs font-normal text-zinc-400">(선택 · 최대 20자)</span></label><input id="budget-reason" type="text" value={budgetReason} onChange={(event) => {setBudgetReason(event.target.value);if (budgetError) {setBudgetError(null);}}} disabled={savingBudget} maxLength={20} placeholder="예: 추가 회비 반영" className="mt-2 h-11 w-full rounded-xl border border-indigo-200 bg-indigo-50/40 px-3 text-sm outline-none transition focus:ring-2 focus:ring-indigo-100"/><div className="mt-1 flex justify-end"><span className="text-xs text-zinc-400">{budgetReason.length}/20</span></div></div>
                {/* 에러 */}{budgetError && (<div role="alert" className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm font-medium text-red-600">{budgetError}</div>)}
                {/* 버튼 */}<div className="flex justify-end gap-2 pt-1"><button type="button" onClick={cancelEditingBudget} disabled={savingBudget} className="rounded-xl border border-zinc-200 bg-white px-4 py-2.5 text-sm font-semibold text-zinc-600 transition-colors hover:bg-zinc-50 disabled:cursor-not-allowed disabled:opacity-50">취소</button><button type="submit" disabled={savingBudget || !budgetInput} className="rounded-xl bg-indigo-600 px-4 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-indigo-700 disabled:cursor-not-allowed">{savingBudget ? "저장 중..." : "예산 저장"}</button></div></form>
            </section>
            <section className="mt-5 rounded-2xl border border-zinc-200 bg-white p-6 shadow-sm sm:p-8">
              <div className="flex items-center justify-between gap-4">
                <div>
                  <p className="text-sm font-medium text-indigo-600">
                    예산 변경 내역
                  </p>
                  <h3 className="mt-1 text-xl font-bold">
                    예산 변경 기록
                  </h3>
                </div>
              </div>

              {loadingBudgetHistory ? (
                  <div className="mt-6 rounded-xl bg-zinc-50 px-4 py-10 text-center text-sm text-zinc-500">
                    예산 변경 내역을 불러오는 중입니다.
                  </div>
              ) : budgetHistoryError ? (
                  <div className="mt-6 rounded-xl border border-red-200 bg-red-50 px-4 py-4">
                    <p className="text-sm font-medium text-red-600">
                      {budgetHistoryError}
                    </p>
                  </div>
              ) : filteredBudgetHistory.length === 0 ? (
                  <div className="mt-6 rounded-xl bg-zinc-50 px-4 py-10 text-center">
                    <p className="text-sm text-zinc-500">
                      예산 변경 내역이 없습니다.
                    </p>
                  </div>
              ) : (
                  <div className="mt-6 overflow-hidden rounded-xl border border-zinc-200">
                    <div className="hidden grid-cols-[120px_minmax(0,1fr)_100px_120px] border-b border-zinc-200 bg-zinc-50 px-5 py-3 text-xs font-semibold text-zinc-500 sm:grid">
                      <span>변경일</span>
                      <span>변경 사유</span>
                      <span>변경자</span>
                      <span className="text-right">증가/감소 금액</span>
                    </div>

                    <div className="divide-y divide-zinc-100">
                      {filteredBudgetHistory.map((history) => (
                          <div
                              key={history.id}
                              className="grid gap-3 px-5 py-4 sm:grid-cols-[120px_minmax(0,1fr)_100px_120px] sm:items-center"
                          >
                            <div className="text-sm text-zinc-500">
                              {new Date(history.processedAt).toLocaleDateString(
                                  "ko-KR",
                              )}
                            </div>

                            <div className="min-w-0">
                              <p className="truncate text-sm font-medium text-zinc-800">
                                {history.reason || "사유 없음"}
                              </p>
                            </div>

                            <div className="text-sm text-zinc-600">
                              {history.userName}
                            </div>

                            <div className="text-right">
                        <span
                            className={`text-sm font-bold ${
                                history.type === "INCREASE"
                                    ? "text-emerald-600"
                                    : "text-rose-600"
                            }`}
                        >
                          {history.type === "INCREASE" ? "+" : "-"}
                          {formatBudget(
                              history.changeBudget,
                              room.currency,
                          )}
                        </span>
                            </div>
                          </div>
                      ))}
                    </div>
                  </div>
              )}
            </section>
      {budgetHistoryError && <button type="button" onClick={() => void loadData()} disabled={savingBudget} className="mt-3 rounded-lg border px-4 py-2 text-sm">내역 다시 불러오기</button>}
    </section>
  );
}
