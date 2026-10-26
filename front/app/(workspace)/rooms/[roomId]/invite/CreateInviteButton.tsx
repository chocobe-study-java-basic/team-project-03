"use client";

import { useEffect, useState } from "react";
import { createInvite } from "@/app/lib/api/inviteApi";

export default function CreateInviteButton({ roomId }: { roomId: number }) {
  const [invite, setInvite] = useState<{
    url: string;
    expireAt: string;
  } | null>(null);
  const [loading, setLoading] = useState(false);
  const [copied, setCopied] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [now, setNow] = useState(0);
  useEffect(() => {
    if (!invite) return;
    const timer = window.setInterval(() => setNow(Date.now()), 1000);
    return () => window.clearInterval(timer);
  }, [invite]);
  const remaining = invite
    ? Math.max(
        0,
        Math.ceil((new Date(invite.expireAt).getTime() - now) / 60000),
      )
    : 0;
  const expired = invite !== null && remaining === 0;
  async function create() {
    if (loading) return;
    setLoading(true);
    setError(null);
    setCopied(false);
    try {
      const response = await createInvite(roomId);
      setNow(Date.now());
      setInvite({
        url: window.location.origin + "/invite/" + response.code,
        expireAt: response.expireAt,
      });
    } catch (error) {
      setError(
        error instanceof Error
          ? error.message
          : "초대 링크를 만들지 못했어요. 다시 시도해 주세요.",
      );
    } finally {
      setLoading(false);
    }
  }
  async function copy() {
    if (!invite || expired) return;
    setError(null);
    try {
      await navigator.clipboard.writeText(invite.url);
      setCopied(true);
    } catch {
      setError(
        "자동 복사를 사용할 수 없어요. 아래 링크를 선택해 직접 복사해 주세요.",
      );
    }
  }
  return (
    <div className="mt-6">
      {!invite ? (
        <div className="rounded-2xl border border-dashed border-indigo-200 bg-indigo-50/50 p-7 text-center">
          <div
            className="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-white text-xl text-indigo-600"
            aria-hidden="true"
          >
            ↗
          </div>
          <h3 className="mt-4 font-bold">함께할 멤버를 초대해 보세요</h3>
          <p className="mt-2 text-sm leading-6 text-zinc-500">
            링크를 받은 사람은 로그인 후<br />이 모임에 멤버로 참여할 수 있어요.
          </p>
        </div>
      ) : (
        <div className="rounded-2xl border border-zinc-200 bg-zinc-50 p-5">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <label htmlFor="invite-link" className="text-sm font-semibold">
              공유할 초대 링크
            </label>
            <span
              className={
                expired
                  ? "text-xs font-semibold text-red-600"
                  : "text-xs font-semibold text-emerald-700"
              }
            >
              {expired ? "만료된 링크" : "사용 가능 · " + remaining + "분 남음"}
            </span>
          </div>
          <input
            id="invite-link"
            readOnly
            value={invite.url}
            onFocus={(event) => event.currentTarget.select()}
            className="mt-3 w-full rounded-xl border border-zinc-200 bg-white px-3 py-3 text-sm text-zinc-600"
          />
          <p className="mt-2 text-xs text-zinc-500">
            만료: {new Date(invite.expireAt).toLocaleString("ko-KR")}
          </p>
          <button
            type="button"
            onClick={copy}
            disabled={expired}
            className="mt-4 w-full rounded-xl bg-indigo-600 px-4 py-3 text-sm font-semibold text-white hover:bg-indigo-700 disabled:opacity-40"
          >
            {expired
              ? "링크가 만료됐어요"
              : copied
                ? "✓ 링크를 복사했어요"
                : "초대 링크 복사"}
          </button>
          <p
            role="status"
            className="mt-2 min-h-5 text-center text-xs text-emerald-700"
          >
            {copied && !expired ? "초대할 멤버에게 링크를 공유해 주세요." : ""}
          </p>
        </div>
      )}
      {error && (
        <p
          role="alert"
          className="mt-4 rounded-xl bg-red-50 px-4 py-3 text-sm text-red-600"
        >
          {error}
        </p>
      )}
      <button
        type="button"
        onClick={create}
        disabled={loading}
        className={
          "mt-5 w-full rounded-xl px-4 py-3 text-sm font-semibold transition disabled:opacity-50 " +
          (invite
            ? "border border-zinc-200 bg-white text-zinc-700 hover:bg-zinc-50"
            : "bg-indigo-600 text-white hover:bg-indigo-700")
        }
      >
        {loading
          ? "링크를 만드는 중…"
          : invite
            ? "새 초대 링크 만들기"
            : "초대 링크 만들기"}
      </button>
      <p className="mt-3 text-center text-xs leading-6 text-zinc-500">
        {invite
          ? "새 링크를 만들어도 기존 링크는 만료 시각까지 유지돼요."
          : "초대 링크는 생성 후 1시간 동안 사용할 수 있어요."}
      </p>
    </div>
  );
}
