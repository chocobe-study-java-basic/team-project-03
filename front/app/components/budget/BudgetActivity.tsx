"use client";

import { useEffect, useId, useRef, useState } from "react";
import type { BudgetHistoryItem } from "@/app/lib/api/budgetApi";

type View = "ledger" | "chart" | "calendar";
const kinds = {
  INCREASE: "예산 증액",
  DECREASE: "예산 감액",
  SETTLEMENT: "사용 정산",
};
const colors = {
  INCREASE: "bg-emerald-500",
  DECREASE: "bg-amber-400",
  SETTLEMENT: "bg-indigo-500",
};
function dayKey(date: Date) {
  return (
    date.getFullYear() +
    "-" +
    String(date.getMonth() + 1).padStart(2, "0") +
    "-" +
    String(date.getDate()).padStart(2, "0")
  );
}
function monthKey(date: Date) {
  return dayKey(date).slice(0, 7);
}
function amount(item: BudgetHistoryItem) {
  return item.type === "SETTLEMENT" ? item.changedBudget : item.changeBudget;
}
function money(value: number, currency: string) {
  return (
    new Intl.NumberFormat("ko-KR").format(value) +
    (currency === "KRW" ? "원" : " " + currency)
  );
}

function TransactionTypeFilter({ value, onChange, counts }: {
  value: string;
  onChange: (value: string) => void;
  counts: Record<string, number>;
}) {
  const [open, setOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const triggerRef = useRef<HTMLButtonElement>(null);
  const optionRefs = useRef<(HTMLButtonElement | null)[]>([]);
  const menuId = useId();
  const options = [{ value: "ALL", label: "모든 유형" }, ...Object.entries(kinds).map(([value, label]) => ({ value, label }))];
  const selected = options.find(option => option.value === value) ?? options[0];
  useEffect(() => {
    if (!open) return;
    optionRefs.current[Math.max(0, ["ALL", "INCREASE", "DECREASE", "SETTLEMENT"].indexOf(value))]?.focus();
    const outside = (event: PointerEvent) => {
      if (!containerRef.current?.contains(event.target as Node)) setOpen(false);
    };
    document.addEventListener("pointerdown", outside);
    return () => document.removeEventListener("pointerdown", outside);
  }, [open, value]);
  const dot = (type: string) => type === "ALL" ? "bg-zinc-400" : colors[type as keyof typeof colors];
  return <div ref={containerRef} className="relative" onBlur={event => {
    if (!event.currentTarget.contains(event.relatedTarget as Node | null)) setOpen(false);
  }}>
    <button ref={triggerRef} type="button" aria-label={"거래 유형: " + selected.label} aria-haspopup="menu" aria-expanded={open} aria-controls={open ? menuId : undefined} onClick={() => setOpen(current => !current)} onKeyDown={event => {
      if (event.key === "ArrowDown" || event.key === "ArrowUp") { event.preventDefault(); setOpen(true); }
      if (event.key === "Escape") setOpen(false);
    }} className={"flex h-11 w-full min-w-36 items-center justify-between gap-3 rounded-xl border bg-white px-3.5 text-sm font-medium transition " + (open ? "border-indigo-300 ring-2 ring-indigo-100" : "border-zinc-200 text-zinc-700 hover:border-zinc-300 hover:bg-zinc-50")}>
      <span className="flex items-center gap-2 whitespace-nowrap"><span aria-hidden="true" className={"h-2 w-2 shrink-0 rounded-full " + dot(value)} />{selected.label}</span>
      <svg aria-hidden="true" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.5" className={"h-4 w-4 shrink-0 text-zinc-400 transition-transform " + (open ? "rotate-180" : "")}><path d="m5 7.5 5 5 5-5" /></svg>
    </button>
    {open && <div id={menuId} role="menu" aria-label="거래 유형 선택" className="absolute right-0 top-full z-30 mt-2 w-56 max-w-[calc(100vw-3rem)] rounded-2xl border border-zinc-200 bg-white p-1.5 shadow-[0_12px_32px_rgba(23,32,51,0.12)]">
      <p className="px-3 pb-2 pt-2 text-[11px] font-medium text-zinc-400">거래 유형으로 보기</p>
      {options.map((option, index) => <button key={option.value} ref={element => { optionRefs.current[index] = element; }} type="button" role="menuitemradio" aria-checked={value === option.value} tabIndex={-1} onClick={() => { onChange(option.value); setOpen(false); triggerRef.current?.focus(); }} onKeyDown={event => {
        if (event.key === "Escape") { event.preventDefault(); event.stopPropagation(); setOpen(false); triggerRef.current?.focus(); return; }
        let next = index;
        if (event.key === "ArrowDown") next = (index + 1) % options.length;
        else if (event.key === "ArrowUp") next = (index - 1 + options.length) % options.length;
        else if (event.key === "Home") next = 0;
        else if (event.key === "End") next = options.length - 1;
        else return;
        event.preventDefault(); optionRefs.current[next]?.focus();
      }} className={"flex min-h-11 w-full items-center gap-2.5 rounded-xl px-3 py-2.5 text-left text-sm transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-indigo-300 " + (value === option.value ? "bg-indigo-50 font-semibold text-indigo-700" : "text-zinc-600 hover:bg-zinc-50")}>
        <span aria-hidden="true" className={"h-2 w-2 shrink-0 rounded-full " + dot(option.value)} /><span className="flex-1">{option.label}</span><span className="text-xs tabular-nums text-zinc-400">{counts[option.value] ?? 0}</span><span aria-hidden="true" className="flex h-4 w-4 items-center justify-center text-indigo-600">{value === option.value ? "✓" : ""}</span>
      </button>)}
    </div>}
  </div>;
}

export default function BudgetActivity({
  history,
  currency,
}: {
  history: BudgetHistoryItem[];
  currency: string;
}) {
  const [view, setView] = useState<View>("ledger");
  const [month, setMonth] = useState(() => monthKey(new Date()));
  const [kind, setKind] = useState("ALL");
  const [query, setQuery] = useState("");
  const [selectedDay, setSelectedDay] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [year, monthNumber] = month.split("-").map(Number);
  const entries = history
    .filter((item) => Number.isFinite(new Date(item.processedAt).getTime()))
    .slice()
    .sort(
      (a, b) =>
        new Date(b.processedAt).getTime() - new Date(a.processedAt).getTime() ||
        b.id - a.id,
    );
  const monthly = entries.filter(
    (item) => monthKey(new Date(item.processedAt)) === month,
  );
  const summary = (type: keyof typeof kinds) =>
    monthly
      .filter((item) => item.type === type)
      .reduce((sum, item) => sum + amount(item), 0);
  const filtered = monthly.filter(
    (item) =>
      (kind === "ALL" || item.type === kind) &&
      (item.reason + " " + item.userName)
        .toLocaleLowerCase()
        .includes(query.trim().toLocaleLowerCase()),
  );
  const pages = Math.max(1, Math.ceil(filtered.length / 10));
  const currentPage = Math.min(page, pages - 1);
  const dayEntries = monthly.filter(
    (item) =>
      selectedDay !== null && dayKey(new Date(item.processedAt)) === selectedDay,
  );
  const months = Array.from({ length: 6 }, (_, i) => {
    const date = new Date(year, monthNumber - 6 + i, 1);
    return { key: monthKey(date), label: date.getMonth() + 1 + "월" };
  });
  const bars = months.map((m) => ({
    ...m,
    totals: (Object.keys(kinds) as (keyof typeof kinds)[]).map((type) => ({
      type,
      value: entries
        .filter(
          (item) =>
            monthKey(new Date(item.processedAt)) === m.key &&
            item.type === type,
        )
        .reduce((sum, item) => sum + amount(item), 0),
    })),
  }));
  const maxValue = Math.max(
    1,
    ...bars.flatMap((bar) => bar.totals.map((total) => total.value)),
  );
  function moveMonth(delta: number) {
    setMonth(monthKey(new Date(year, monthNumber - 1 + delta, 1)));
    setSelectedDay(null);
    setPage(0);
  }
  function ledgerRow(item: BudgetHistoryItem) {
    const positive = item.type === "INCREASE";
    return (
      <li
        key={item.id}
        className="grid grid-cols-[minmax(0,1fr)_auto] items-center gap-3 rounded-xl border-b border-zinc-100 px-2 py-4 transition hover:bg-zinc-50 last:border-0 sm:px-3"
      >
        <div className="flex min-w-0 flex-1 items-center gap-3">
          <span
            aria-hidden="true"
            className={
              "flex h-10 w-10 shrink-0 items-center justify-center rounded-xl " +
              (positive
                ? "bg-emerald-50 text-emerald-700"
                : item.type === "DECREASE" ? "bg-amber-50 text-amber-700" : "bg-indigo-50 text-indigo-600")
            }
          >
            {positive ? "↙" : "↗"}
          </span>
          <div className="min-w-0">
            <p className="truncate text-sm font-semibold text-zinc-800">
              {item.reason || kinds[item.type]}
            </p>
            <p className="mt-1 text-xs text-zinc-500">
              {new Date(item.processedAt).toLocaleDateString("ko-KR")} ·{" "}
              {item.userName}
            </p>
          </div>
        </div>
        <p
          className={
            "ml-auto max-w-[42vw] break-words text-right text-sm font-bold tabular-nums sm:max-w-none " +
            (positive ? "text-emerald-700" : "text-zinc-900")
          }
        >
          {positive ? "+" : "−"}
          {money(amount(item), currency)}
        </p>
      </li>
    );
  }
  return (
    <section
      className="mt-7 rounded-2xl border border-zinc-200 bg-white p-5 shadow-sm sm:p-7"
      aria-labelledby="activity-title"
    >
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <p className="eyebrow text-indigo-600">BUDGET ACTIVITY</p>
          <h2 id="activity-title" className="mt-2 text-xl font-bold">
            우리 모임의 예산 기록
          </h2>
          <p className="mt-2 text-sm text-zinc-500">
            돈의 흐름을 장부, 그래프, 달력으로 확인해요.
          </p>
        </div>
        <div className="flex items-center gap-1 rounded-xl border border-zinc-200 p-1">
          <button
            type="button"
            onClick={() => moveMonth(-1)}
            aria-label="이전 달"
            className="h-9 w-9 rounded-lg hover:bg-zinc-100"
          >
            ‹
          </button>
          <label>
            <span className="sr-only">기록 조회 월</span>
            <input
              type="month"
              value={month}
              onChange={(event) => {
                if (/^\d{4}-\d{2}$/.test(event.target.value)) {
                  setMonth(event.target.value);
                  setSelectedDay(null);
                  setPage(0);
                }
              }}
              className="min-w-0 max-w-40 border-0 bg-transparent px-1 text-sm font-semibold"
            />
          </label>
          <button
            type="button"
            onClick={() => moveMonth(1)}
            aria-label="다음 달"
            className="h-9 w-9 rounded-lg hover:bg-zinc-100"
          >
            ›
          </button>
        </div>
      </div>
      <div className="mt-6 grid grid-cols-1 gap-3 sm:grid-cols-3">
        {(Object.keys(kinds) as (keyof typeof kinds)[]).map((type) => (
          <div key={type} className="rounded-xl bg-zinc-50 p-4">
            <p className="flex items-center gap-2 text-xs text-zinc-500">
              <span className={"h-2 w-2 rounded-full " + colors[type]} />
              {kinds[type]}
            </p>
            <p className="mt-2 break-all text-lg font-bold tabular-nums">
              {money(summary(type), currency)}
            </p>
          </div>
        ))}
      </div>
      <div
        role="group"
        aria-label="예산 기록 보기 방식"
        className="mt-6 inline-flex max-w-full gap-1 rounded-xl bg-zinc-100 p-1"
      >
        {[
          { id: "ledger", label: "거래 장부" },
          { id: "chart", label: "월별 그래프" },
          { id: "calendar", label: "캘린더" },
        ].map((tab) => (
          <button
            key={tab.id}
            type="button"
            aria-pressed={view === tab.id}
            onClick={() => setView(tab.id as View)}
            className={
              "min-h-10 rounded-lg px-3 py-2 text-xs font-semibold transition sm:px-5 sm:text-sm " +
              (view === tab.id
                ? "bg-white text-indigo-700 shadow-sm"
                : "text-zinc-500 hover:text-zinc-900")
            }
          >
            {tab.label}
          </button>
        ))}
      </div>
      {view === "ledger" && (
        <div className="mt-5">
          <div className="grid grid-cols-[minmax(0,1fr)_auto] items-start gap-3">
            <label className="relative block">
              <span className="sr-only">장부 검색</span>
              <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" className="pointer-events-none absolute left-3.5 top-3.5 h-4 w-4 text-zinc-400"><circle cx="10.5" cy="10.5" r="6.5" /><path d="m16 16 4 4" /></svg>
              <input type="search" value={query} onChange={(event) => { setQuery(event.target.value); setPage(0); }} placeholder="사유 또는 이름으로 검색" className="h-11 w-full rounded-xl border border-zinc-200 bg-white pl-10 pr-3 text-sm outline-none transition focus:border-indigo-400 focus:ring-2 focus:ring-indigo-100" />
            </label>
            <TransactionTypeFilter value={kind} onChange={value => { setKind(value); setPage(0); }} counts={Object.fromEntries(["ALL", ...Object.keys(kinds)].map(type => [type, monthly.filter(item => (type === "ALL" || item.type === type) && (item.reason + " " + item.userName).toLocaleLowerCase().includes(query.trim().toLocaleLowerCase())).length]))} />
          </div>
          <div className="mt-5 flex items-center justify-between gap-3 border-b border-zinc-200 pb-3">
            <p aria-live="polite" className="text-sm font-semibold text-zinc-700">{kind === "ALL" ? "전체 내역" : kinds[kind as keyof typeof kinds]}<span className="ml-2 text-indigo-600">{filtered.length}건</span></p>
            {(kind !== "ALL" || query) && <button type="button" onClick={() => { setKind("ALL"); setQuery(""); setPage(0); }} className="rounded-lg px-2 py-1 text-xs font-medium text-zinc-500 hover:bg-zinc-100 hover:text-zinc-900">필터 초기화 ↺</button>}
          </div>
          {filtered.length ? (
            <ul>
              {filtered
                .slice(currentPage * 10, (currentPage + 1) * 10)
                .map(ledgerRow)}
            </ul>
          ) : (
            <p className="my-4 rounded-xl border border-dashed border-zinc-200 bg-zinc-50/50 px-4 py-12 text-center text-sm leading-7 text-zinc-500">
              이 달에 조건과 일치하는 기록이 없어요.
            </p>
          )}
          {pages > 1 && (
            <div className="mt-4 flex items-center justify-end gap-4 text-sm">
              <button
                type="button"
                disabled={currentPage === 0}
                onClick={() => setPage(currentPage - 1)}
                className="rounded-lg border border-zinc-200 px-3 py-2 disabled:opacity-40"
              >
                이전
              </button>
              <span>
                {currentPage + 1} / {pages}
              </span>
              <button
                type="button"
                disabled={currentPage === pages - 1}
                onClick={() => setPage(currentPage + 1)}
                className="rounded-lg border border-zinc-200 px-3 py-2 disabled:opacity-40"
              >
                다음
              </button>
            </div>
          )}
        </div>
      )}
      {view === "chart" && (
        <div className="mt-6">
          <h3 className="text-sm font-semibold">
            선택한 달까지 최근 6개월의 자금 변동
          </h3>
          <p className="mt-1 text-xs text-zinc-500">
            막대 높이는 금액을 비교합니다. 각 막대를 누르면 해당 월 장부를 볼 수
            있어요.
          </p>
          <div
            className="mt-6 grid h-52 grid-cols-6 gap-2 border-b border-zinc-200"
            aria-label="최근 6개월 증액·감액·정산 금액"
          >
            {bars.map((bar) => (
              <div
                key={bar.key}
                className="flex h-full min-w-0 flex-col justify-end"
              >
                <div className="flex h-44 items-end justify-center gap-1">
                  {bar.totals.map((total) => (
                    <button
                      type="button"
                      key={total.type}
                      aria-label={
                        bar.key +
                        " " +
                        kinds[total.type] +
                        " " +
                        money(total.value, currency)
                      }
                      title={
                        kinds[total.type] + " " + money(total.value, currency)
                      }
                      onClick={() => {
                        setMonth(bar.key);
                        setView("ledger");
                        setKind(total.type);
                        setPage(0);
                      }}
                      className={
                        "w-full max-w-5 rounded-t-md transition hover:opacity-70 " +
                        colors[total.type]
                      }
                      style={{
                        height:
                          total.value > 0
                            ? Math.max(3, (total.value / maxValue) * 100) + "%"
                            : "3px",
                        opacity: total.value ? 1 : 0.2,
                      }}
                    />
                  ))}
                </div>
                <span className="py-2 text-center text-xs text-zinc-500">
                  {bar.label}
                </span>
              </div>
            ))}
          </div>
          <div className="mt-4 flex flex-wrap gap-4">
            {Object.entries(kinds).map(([key, label]) => (
              <span
                key={key}
                className="flex items-center gap-2 text-xs text-zinc-500"
              >
                <span
                  className={
                    "h-2 w-2 rounded-full " + colors[key as keyof typeof colors]
                  }
                />
                {label}
              </span>
            ))}
          </div>
        </div>
      )}
      {view === "calendar" && (
        <div className="mt-6">
          <p className="mb-4 text-xs leading-6 text-zinc-500">
            예산이 변경된 날짜에 자동 표시돼요. 날짜를 누르면 그날의 내역을 볼
            수 있어요.
          </p>
          <div className="grid grid-cols-7 text-center">
            {["일", "월", "화", "수", "목", "금", "토"].map((day, i) => (
              <div
                key={day}
                className={
                  "pb-3 text-xs font-medium " +
                  (i === 0 ? "text-red-500" : "text-zinc-500")
                }
              >
                {day}
              </div>
            ))}
            {Array.from(
              { length: new Date(year, monthNumber - 1, 1).getDay() },
              (_, i) => (
                <div key={"empty" + i} />
              ),
            )}
            {Array.from(
              { length: new Date(year, monthNumber, 0).getDate() },
              (_, i) => {
                const date = new Date(year, monthNumber - 1, i + 1);
                const key = dayKey(date);
                const items = monthly.filter(
                  (item) => dayKey(new Date(item.processedAt)) === key,
                );
                const today = key === dayKey(new Date());
                return (
                  <button
                    type="button"
                    key={key}
                    aria-pressed={selectedDay === key}
                    aria-label={
                      date.toLocaleDateString("ko-KR") +
                      ", 기록 " +
                      items.length +
                      "건"
                    }
                    onClick={() =>
                      setSelectedDay(selectedDay === key ? null : key)
                    }
                    className={
                      "flex min-h-16 flex-col items-center justify-center gap-1.5 border-t border-zinc-100 py-2 transition sm:min-h-20 " +
                      (selectedDay === key
                        ? "rounded-xl bg-indigo-50 text-indigo-700 ring-1 ring-inset ring-indigo-200"
                        : "hover:bg-zinc-50")
                    }
                  >
                    <span
                      className={
                        "flex h-7 w-7 items-center justify-center rounded-full text-sm " +
                        (today ? "bg-indigo-600 font-bold text-white" : "")
                      }
                    >
                      {i + 1}
                    </span>
                    <span className="flex min-h-2 gap-1" aria-hidden="true">
                      {(Object.keys(kinds) as (keyof typeof kinds)[])
                        .filter((type) =>
                          items.some((item) => item.type === type),
                        )
                        .map((type) => (
                          <span
                            key={type}
                            className={
                              "h-1.5 w-1.5 rounded-full " + colors[type]
                            }
                          />
                        ))}
                    </span>
                  </button>
                );
              },
            )}
          </div>
          {selectedDay && <>
          <div className="mt-5 flex items-center justify-between border-t border-zinc-200 pt-5">
            <h3 className="text-sm font-semibold">
              {selectedDay + " 내역"}
            </h3>
            {selectedDay && (
              <button
                type="button"
                onClick={() => setSelectedDay(null)}
                className="text-xs font-semibold text-indigo-600"
              >
                선택 해제
              </button>
            )}
          </div>
          {dayEntries.length ? (
            <ul>{dayEntries.slice(0, 10).map(ledgerRow)}</ul>
          ) : (
            <p className="py-8 text-center text-sm text-zinc-500">
              기록된 예산 변동이 없어요.
            </p>
          )}
          {dayEntries.length > 10 && (
            <button
              type="button"
              onClick={() => {
                setView("ledger");
                setKind("ALL");
                setQuery("");
                setPage(0);
              }}
              className="mt-4 text-sm font-semibold text-indigo-600"
            >
              이 달의 전체 내역 보기 →
            </button>
          )}
          </>}
        </div>
      )}
      <p className="mt-6 border-t border-zinc-100 pt-4 text-xs leading-6 text-zinc-500">
        등록된 예산 변동 기준으로 표시합니다. 수정된 내역은 현재 금액으로
        반영됩니다. 캘린더에는 처리 완료된 내역이 표시돼요.
      </p>
    </section>
  );
}
