"use client";

import { Suspense } from "react";
import { useParams } from "next/navigation";
import CreateInviteButton from "../CreateInviteButton";

export default function CreateInvitePage() {
  return (
    <Suspense fallback={null}>
      <CreateInviteContent />
    </Suspense>
  );
}

function CreateInviteContent() {
  const { roomId } = useParams<{ roomId: string }>();
  const numericRoomId = Number(roomId);

  if (!Number.isSafeInteger(numericRoomId) || numericRoomId <= 0) {
    return (
        <main className="mx-auto w-full max-w-3xl">
          <div className="rounded-2xl border border-red-200 bg-red-50 p-6">
            <h1 className="font-bold text-red-700">
              모임 정보가 없습니다.
            </h1>

            <p className="mt-2 text-sm text-red-600">
              모임 목록에서 모임을 선택한 후 다시 시도해주세요.
            </p>
          </div>
        </main>
    );
  }

  return (
      <main className="w-full max-w-4xl">
        <div className="mb-8">
          <h1 className="text-3xl font-bold tracking-tight text-zinc-900">
            초대하기
          </h1>

          <p className="mt-2 text-base text-zinc-500">
            링크를 공유해 새로운 멤버를 초대하세요
          </p>
        </div>

        <section className="rounded-2xl border border-zinc-200 bg-white p-8 shadow-sm">
          <h2 className="text-lg font-semibold text-zinc-900">
            초대 링크
          </h2>

          <CreateInviteButton roomId={numericRoomId} />
        </section>

        <section className="mt-6 rounded-2xl border border-zinc-200 bg-white p-8 shadow-sm">
          <h2 className="text-lg font-semibold text-zinc-900">
            초대 방법
          </h2>

          <div className="mt-6 space-y-5">
            <div className="flex items-start gap-4">
            <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-indigo-50 text-sm font-semibold text-indigo-600">
              1
            </span>

              <p className="pt-0.5 text-sm text-zinc-600">
                위 초대 링크를 복사하세요.
              </p>
            </div>

            <div className="flex items-start gap-4">
            <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-indigo-50 text-sm font-semibold text-indigo-600">
              2
            </span>

              <p className="pt-0.5 text-sm text-zinc-600">
                카카오톡, 문자 등으로 초대할 멤버에게 공유하세요.
              </p>
            </div>

            <div className="flex items-start gap-4">
            <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-indigo-50 text-sm font-semibold text-indigo-600">
              3
            </span>

              <p className="pt-0.5 text-sm text-zinc-600">
                멤버가 링크를 열고 로그인한 뒤 참여하면 완료돼요.
              </p>
            </div>
          </div>
        </section>
      </main>
  );
}
