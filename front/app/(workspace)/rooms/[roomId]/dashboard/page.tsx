"use client";

import Link from "next/link";
import BudgetActivity from "@/app/components/budget/BudgetActivity";
import { useParams } from "next/navigation";
import { useCallback, useEffect, useState } from "react";

import {
  getBudget,
  getBudgetHistory,
  type Budget,
  type BudgetHistoryItem,
} from "../../../../lib/api/budgetApi";

export default function DashboardPage() {
  const { roomId: roomIdParam } = useParams<{ roomId: string }>();
  return <DashboardContent roomId={Number(roomIdParam)} />;
}

function DashboardContent({ roomId }: { roomId: number }) {
  const [budget, setBudget] = useState<Budget | null>(null);
  const [history, setHistory] = useState<BudgetHistoryItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const roomBasePath = `/rooms/${roomId}`;

  const loadDashboard = useCallback(async () => {
    if (!roomId || Number.isNaN(roomId)) {
      setError("모임 정보를 찾을 수 없습니다.");
      setLoading(false);
      return;
    }

    try {
      setLoading(true);
      setError(null);
      const [budgetData, historyData] = await Promise.all([
        getBudget(roomId),
        getBudgetHistory(roomId),
      ]);
      setBudget(budgetData);
      setHistory(historyData.history);
    } catch (caughtError) {
      console.error("대시보드 조회 실패:", caughtError);
      setError(
        caughtError instanceof Error
          ? caughtError.message
          : "대시보드 정보를 불러오지 못했습니다.",
      );
    } finally {
      setLoading(false);
    }
  }, [roomId]);

  useEffect(() => {
    void Promise.resolve().then(loadDashboard);
  }, [loadDashboard]);

  if (loading) return <DashboardLoadingState />;

  if (error || !budget) {
    return (
      <section className="mx-auto w-full max-w-5xl">
        <div className="rounded-2xl border border-red-200 bg-white p-8 text-center shadow-sm">
          <p className="text-sm text-red-500">
            {error ?? "예산 정보를 불러오지 못했습니다."}
          </p>
          <button
            type="button"
            onClick={() => void loadDashboard()}
            className="mt-4 rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-700"
          >
            다시 불러오기
          </button>
        </div>
      </section>
    );
  }

  return (
    <section aria-labelledby="dashboard-title" className="mx-auto w-full max-w-5xl">
      <header className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 id="dashboard-title" className="text-2xl font-bold tracking-tight text-zinc-900">
            예산 한눈에 보기
          </h1>
          <p className="mt-1 text-sm text-zinc-500">{budget.roomName}의 예산 현황</p>
        </div>
        <Link href={`${roomBasePath}/budget-requests`} className="rounded-xl bg-indigo-600 px-4 py-2.5 text-sm font-semibold text-white hover:bg-indigo-700">+ 예산 신청</Link>
      </header>

      <article className="mt-8 rounded-2xl border border-zinc-200 bg-white p-6 shadow-sm sm:p-8">
        <div className="grid gap-7 sm:grid-cols-3 sm:gap-6">
          <SummaryItem label="현재 잔액" amount={formatMoney(budget.totalBudget, budget.currency)} tone="default" />
          <SummaryItem
            label="사용 가능"
            amount={formatMoney(budget.availableBudget, budget.currency)}
            description={`${getAvailablePercent(budget.availableBudget, budget.totalBudget)}%`}
            tone="available"
          />
          <SummaryItem
            label="예약 중"
            amount={formatMoney(budget.reserveBudget, budget.currency)}
            description="승인된 미정산 요청"
            tone="reserved"
          />
        </div>
        <div className="mt-7 budget-track" role="img" aria-label={`가용 ${getAvailablePercent(budget.availableBudget, budget.totalBudget)}%, 나머지는 선점 중`}><span className="bg-emerald-500" style={{ width: `${Math.max(0, Math.min(100, Number(getAvailablePercent(budget.availableBudget, budget.totalBudget))))}%` }} /><span className="flex-1 bg-indigo-300" /></div>
        <p className="mt-6 border-t border-zinc-200 pt-6 text-sm font-medium text-zinc-800">
          현재 잔액 {formatMoney(budget.totalBudget, budget.currency)}
          <span className="mx-2 text-zinc-400">→</span>
          <span className="text-indigo-500">사용 가능 {formatMoney(budget.availableBudget, budget.currency)}</span>
          <span className="mx-2 text-zinc-400">+</span>
          <span className="text-indigo-400">예약 중 {formatMoney(budget.reserveBudget, budget.currency)}</span>
        </p>
      </article>

      <div className="mt-6 grid gap-3 sm:grid-cols-2"><Link href={`${roomBasePath}/budget-requests`} className="room-card flex items-center justify-between rounded-2xl border border-zinc-200 bg-white p-5"><div><p className="font-semibold">필요한 예산 신청하기</p><p className="mt-1 text-sm text-zinc-500">사용 목적과 금액을 남겨 주세요.</p></div><span aria-hidden="true" className="text-indigo-600">↗</span></Link><Link href={`${roomBasePath}/settlements`} className="room-card flex items-center justify-between rounded-2xl border border-zinc-200 bg-white p-5"><div><p className="font-semibold">사용한 금액 정산하기</p><p className="mt-1 text-sm text-zinc-500">승인된 신청의 실제 지출을 기록해요.</p></div><span aria-hidden="true" className="text-indigo-600">↗</span></Link></div>
      <BudgetActivity history={history} currency={budget.currency} />
    </section>
  );
}

function SummaryItem({
  label,
  amount,
  description,
  tone,
}: {
  label: string;
  amount: string;
  description?: string;
  tone: "default" | "available" | "reserved";
}) {
  const amountColor =
    tone === "available"
      ? "text-emerald-600"
      : tone === "reserved"
        ? "text-indigo-400"
        : "text-zinc-950";

  return (
    <div>
      <p className="text-sm font-medium text-zinc-500">{label}</p>
      <p className={`mt-1 break-words text-2xl font-extrabold tracking-tight lg:text-3xl ${amountColor}`}>
        {amount}
      </p>
      {description && <p className="mt-1 text-sm text-zinc-500">{description}</p>}
    </div>
  );
}

function formatMoney(amount: number, currency: Budget["currency"]) {
  const symbols = { KRW: "₩", USD: "$", JPY: "¥" };
  return `${symbols[currency]}${amount.toLocaleString("ko-KR")}`;
}

function getAvailablePercent(available: number, total: number) {
  if (total === 0) return "0.0";
  return ((available / total) * 100).toFixed(1);
}


function DashboardLoadingState() {
  return (
    <section className="mx-auto w-full max-w-5xl">
      <div className="rounded-2xl bg-white p-8 text-center shadow-sm">
        <p className="text-sm text-zinc-500">대시보드를 불러오는 중...</p>
      </div>
    </section>
  );
}
