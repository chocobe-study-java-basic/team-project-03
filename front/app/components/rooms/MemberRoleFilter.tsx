"use client";

import { useEffect, useId, useRef, useState } from "react";

const options = [
  { value: "ALL", label: "모든 권한", dot: "bg-zinc-400" },
  { value: "OWNER", label: "방장", dot: "bg-indigo-500" },
  { value: "OPERATOR", label: "운영자", dot: "bg-emerald-500" },
  { value: "MEMBER", label: "멤버", dot: "bg-slate-400" },
];

export default function MemberRoleFilter({ value, onChange, counts }: {
  value: string;
  onChange: (value: string) => void;
  counts: Record<string, number>;
}) {
  const [open, setOpen] = useState(false);
  const root = useRef<HTMLDivElement>(null);
  const trigger = useRef<HTMLButtonElement>(null);
  const items = useRef<(HTMLButtonElement | null)[]>([]);
  const menuId = useId();
  const selected = options.find(option => option.value === value) ?? options[0];

  useEffect(() => {
    if (!open) return;
    items.current[Math.max(0, options.findIndex(option => option.value === value))]?.focus();
    const outside = (event: PointerEvent) => {
      if (!root.current?.contains(event.target as Node)) setOpen(false);
    };
    document.addEventListener("pointerdown", outside);
    return () => document.removeEventListener("pointerdown", outside);
  }, [open, value]);

  return <div ref={root} className="relative" onBlur={event => {
    if (!event.currentTarget.contains(event.relatedTarget as Node | null)) setOpen(false);
  }}>
    <button ref={trigger} type="button" aria-label={`멤버 권한: ${selected.label}`} aria-haspopup="menu" aria-expanded={open} aria-controls={open ? menuId : undefined}
      onClick={() => setOpen(current => !current)} onKeyDown={event => {
        if (event.key === "ArrowDown" || event.key === "ArrowUp") { event.preventDefault(); setOpen(true); }
        if (event.key === "Escape") setOpen(false);
      }} className={`flex h-12 min-w-[152px] items-center justify-between gap-4 rounded-xl border bg-white px-4 text-sm font-medium transition ${open ? "border-indigo-300 text-zinc-800 ring-2 ring-indigo-100" : "border-zinc-200 text-zinc-700 hover:border-zinc-300 hover:bg-zinc-50"}`}>
      <span className="flex items-center gap-2 whitespace-nowrap"><span aria-hidden="true" className={`h-2 w-2 shrink-0 rounded-full ${selected.dot}`}/>{selected.label}</span>
      <svg aria-hidden="true" viewBox="0 0 20 20" fill="none" stroke="currentColor" strokeWidth="1.5" className={`h-4 w-4 shrink-0 text-zinc-400 transition-transform ${open ? "rotate-180" : ""}`}><path d="m5 7.5 5 5 5-5"/></svg>
    </button>
    {open && <div id={menuId} role="menu" aria-label="멤버 권한 선택" className="absolute right-0 top-full z-30 mt-2 w-56 max-w-[calc(100vw-3rem)] rounded-2xl border border-zinc-200 bg-white p-1.5 shadow-[0_12px_32px_rgba(23,32,51,0.12)]">
      <p className="px-3 pb-2 pt-2 text-[11px] font-medium text-zinc-400">권한으로 보기</p>
      {options.map((option, index) => <button key={option.value} ref={element => { items.current[index] = element; }} type="button" role="menuitemradio" aria-checked={value === option.value} tabIndex={-1}
        onClick={() => { onChange(option.value); setOpen(false); trigger.current?.focus(); }} onKeyDown={event => {
          if (event.key === "Escape") { event.preventDefault(); event.stopPropagation(); setOpen(false); trigger.current?.focus(); return; }
          let next = index;
          if (event.key === "ArrowDown") next = (index + 1) % options.length;
          else if (event.key === "ArrowUp") next = (index - 1 + options.length) % options.length;
          else if (event.key === "Home") next = 0;
          else if (event.key === "End") next = options.length - 1;
          else return;
          event.preventDefault(); items.current[next]?.focus();
        }} className={`flex min-h-11 w-full items-center gap-2.5 rounded-xl px-3 py-2.5 text-left text-sm transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-indigo-300 ${value === option.value ? "bg-indigo-50 font-semibold text-indigo-700" : "text-zinc-600 hover:bg-zinc-50"}`}>
        <span aria-hidden="true" className={`h-2 w-2 shrink-0 rounded-full ${option.dot}`}/><span className="flex-1">{option.label}</span><span className="text-xs tabular-nums text-zinc-400">{counts[option.value] ?? 0}</span><span aria-hidden="true" className="flex h-4 w-4 items-center justify-center text-indigo-600">{value === option.value ? "✓" : ""}</span>
      </button>)}
    </div>}
  </div>;
}
