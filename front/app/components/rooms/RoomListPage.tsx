"use client";

import BrandLogo from "../common/BrandLogo";
import Link from "next/link";
import { useEffect, useState } from "react";

import { ApiError } from "../../lib/api/types";
import { getRooms, type Room } from "../../lib/api/roomsApi";
import { getMe } from "../../lib/api/userApi";
import UserProfileMenu from "../common/UserProfileMenu/UserProfileMenu";

function formatBudget(amount: number, currency: string) {
  const formatted = new Intl.NumberFormat("ko-KR").format(amount);
  return currency === "KRW" ? `${formatted}원` : `${currency} ${formatted}`;
}

function RoomCard({ room }: { room: Room }) {
  const availableRate =
      room.totalBudget > 0
          ? Math.max(
              0,
              Math.min(100, (room.availableBudget / room.totalBudget) * 100),
          )
          : 0;

  const usedRate = room.totalBudget > 0 ? 100 - availableRate : 0;

  return (
      <article className="room-card rounded-2xl border border-zinc-200 bg-white p-6 shadow-sm sm:p-7">
        <div className="flex items-start justify-between gap-5">
          <div className="flex min-w-0 items-center gap-4">
          <span className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-indigo-50 text-lg font-bold text-indigo-500">
            {room.name.charAt(0)}
          </span>

            <div className="min-w-0">
              <h2 className="truncate text-lg font-bold text-zinc-900">
                {room.name}
              </h2>

              <p className="mt-1 text-sm text-zinc-500">
                {room.currency} · 현재 잔액{" "}
                {formatBudget(room.totalBudget, room.currency)}
              </p>
            </div>
          </div>

          <div className="shrink-0 text-right">
            <p className="text-sm text-zinc-500">사용 가능</p>

            <p className="mt-1 text-xl font-extrabold text-emerald-600 sm:text-2xl">
              {formatBudget(room.availableBudget, room.currency)}
            </p>
          </div>
        </div>

        <div className="mt-7">
          <div
              className="flex h-3 overflow-hidden rounded-full bg-zinc-100"
              aria-label={`${room.name} 예산 현황`}
          >
          <span
              className="bg-indigo-500"
              style={{ width: `${usedRate}%` }}
          />

            <span
                className="bg-emerald-100"
                style={{ width: `${availableRate}%` }}
            />
          </div>

          <div className="mt-3 flex gap-4 text-sm text-zinc-500">
          <span className="flex items-center gap-1.5">
            <i className="h-2.5 w-2.5 rounded-full bg-indigo-500" />
            선점 중
          </span>

            <span className="flex items-center gap-1.5">
            <i className="h-2.5 w-2.5 rounded-full bg-emerald-300" />
            사용 가능
          </span>
          </div>
        </div>

        <div className="mt-6 flex flex-wrap items-end justify-between gap-5 border-t border-zinc-200 pt-5">
          <dl className="grid min-w-[250px] flex-1 grid-cols-2 gap-5 text-sm">
            <div>
              <dt className="text-zinc-500">현재 잔액</dt>

              <dd className="mt-1 text-base font-extrabold text-zinc-900">
                {formatBudget(room.totalBudget, room.currency)}
              </dd>
            </div>

            <div>
              <dt className="text-zinc-500">선점 비율</dt>

              <dd className="mt-1 text-base font-extrabold text-zinc-900">
                {usedRate.toFixed(1)}%
              </dd>
            </div>
          </dl>

          <div className="flex items-center gap-4">
            <Link
                href={`/rooms/${room.id}`}
                className="text-sm font-semibold text-indigo-600 hover:text-indigo-800"
            >
              모임 상세
            </Link>

            <Link
                href={`/rooms/${room.id}/dashboard`}
                className="rounded-xl bg-indigo-600 px-4 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-indigo-700"
            >
              이 모임 열기
            </Link>
          </div>
        </div>
      </article>
  );
}

function getErrorMessage(error: unknown) {
  if (error instanceof ApiError && error.status === 401) {
    return null;
  }

  return error instanceof ApiError
      ? error.message
      : "모임 목록을 불러오지 못했습니다.";
}

export default function RoomListPage() {
  const [query, setQuery] = useState("");
  const [rooms, setRooms] = useState<Room[]>([]);
  const [userName, setUserName] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  const loadRooms = async () => {
    setLoading(true);
    setError(null);

    try {
      setRooms(await getRooms());
    } catch (caughtError) {
      setError(getErrorMessage(caughtError));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    let cancelled = false;

    getRooms()
        .then((loadedRooms) => {
          if (!cancelled) {
            setRooms(loadedRooms);
          }
        })
        .catch((caughtError: unknown) => {
          if (!cancelled) {
            setError(getErrorMessage(caughtError));
          }
        })
        .finally(() => {
          if (!cancelled) {
            setLoading(false);
          }
        });

    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    const loadUser = async () => {
      try {
        const user = await getMe();
        setUserName(user.name);
      } catch (caughtError) {
        console.error(
            "사용자 정보를 불러오지 못했습니다.",
            caughtError,
        );
      }
    };

    void loadUser();
  }, []);

  const filteredRooms = rooms.filter(room => room.name.toLocaleLowerCase().includes(query.trim().toLocaleLowerCase()));

  return (
      <main className="min-h-screen bg-background text-zinc-900">
        <header className="border-b border-zinc-200 bg-white">
          <div className="mx-auto flex h-20 max-w-6xl items-center justify-between px-5 sm:px-8">
            <Link
                href="/rooms"
                className="flex items-center gap-3"
            >
              <BrandLogo />
            </Link>

            <UserProfileMenu userName={userName} />
          </div>
        </header>

        <div className="mx-auto max-w-5xl px-5 py-12 sm:px-8">
          <div className="flex flex-wrap items-start justify-between gap-5">
            <div>
              <p className="eyebrow mb-3 text-indigo-600">YOUR WORKSPACE</p>
              <h1 className="text-3xl font-bold tracking-tight">
                내 모임
              </h1>

              <p className="mt-2 text-zinc-500">
                {loading
                    ? "참여 중인 모임을 불러오는 중이에요"
                    : `참여 중인 ${rooms.length}개의 모임이 있어요`}
              </p>
            </div>

            <div className="flex gap-2">
              <Link
                  href="/rooms/create"
                  className="rounded-xl bg-indigo-600 px-4 py-2.5 text-sm font-semibold text-white shadow-sm transition-colors hover:bg-indigo-700"
              >
                ＋ 새 모임 만들기
              </Link>
            </div>
          </div>

          {!loading && !error && rooms.length > 0 && <div className="mt-8 flex flex-wrap items-center justify-between gap-3"><label className="relative block w-full sm:max-w-xs"><span className="sr-only">모임 이름 검색</span><input type="search" value={query} onChange={event => setQuery(event.target.value)} placeholder="모임 이름으로 검색" className="h-11 w-full rounded-xl border border-zinc-200 bg-white px-4 text-sm" /></label><p aria-live="polite" className="text-sm text-zinc-500">{filteredRooms.length}개 모임</p></div>}
          <section
              className="mt-6 space-y-5"
              aria-label="참여 중인 모임"
          >
            {loading && (
<div role="status" aria-label="모임 목록을 불러오는 중입니다" className="space-y-5">{[1, 2].map(item => <div key={item} className="rounded-2xl border border-zinc-200 bg-white p-7"><div className="loading-skeleton h-5 w-36" /><div className="loading-skeleton mt-6 h-10 w-52" /><div className="loading-skeleton mt-6 h-3 w-full" /></div>)}</div>
            )}

            {error && (
                <div className="rounded-2xl border border-red-200 bg-red-50 px-6 py-10 text-center">
                  <p className="text-sm font-medium text-red-600">
                    {error}
                  </p>

                  <button
                      type="button"
                      onClick={() => void loadRooms()}
                      className="mt-4 rounded-lg border border-red-200 bg-white px-3 py-2 text-sm font-semibold text-red-600"
                  >
                    다시 시도
                  </button>
                </div>
            )}

            {!loading && !error && rooms.length === 0 && (
                <div className="rounded-2xl border border-dashed border-zinc-300 bg-white px-6 py-16 text-center">
                  <h2 className="font-bold">
                    참여 중인 모임이 없습니다.
                  </h2>

                  <p className="mt-2 text-sm text-zinc-500">
                    초대 링크로 모임에 참여하거나 새 모임을 만들어 시작하세요.
                  </p>
                </div>
            )}

            {!loading &&
                !error &&
                filteredRooms.map((room) => (
                    <RoomCard
                        key={room.id}
                        room={room}
                    />
                ))}
            {!loading && !error && rooms.length > 0 && filteredRooms.length === 0 && <div className="rounded-2xl border border-dashed border-zinc-300 bg-white p-12 text-center"><h2 className="font-bold">검색 결과가 없어요</h2><p className="mt-2 text-sm text-zinc-500">다른 이름으로 검색해 보세요.</p><button onClick={() => setQuery("")} className="mt-4 text-sm font-semibold text-indigo-600">검색 초기화</button></div>}
          </section>
        </div>
      </main>
  );
}
