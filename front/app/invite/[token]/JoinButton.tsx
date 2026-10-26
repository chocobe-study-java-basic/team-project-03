"use client";
import Link from "next/link";
import { useState } from "react";
import { useRouter } from "next/navigation";
import { joinRoom } from "@/app/lib/api/inviteApi";
import { ApiError } from "@/app/lib/api/types";

export default function JoinButton({ token }: { token: string }) {
  const router = useRouter();
  const [joining, setJoining] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [alreadyJoined, setAlreadyJoined] = useState(false);
  async function join() {
    setJoining(true);
    setError(null);
    try {
      const room = await joinRoom(token);
      router.replace("/rooms/" + room.roomId + "/dashboard");
    } catch (error) {
      setAlreadyJoined(error instanceof ApiError && error.status === 409);
      setError(
        error instanceof Error
          ? error.message
          : "모임에 참여하지 못했어요. 다시 시도해 주세요.",
      );
    } finally {
      setJoining(false);
    }
  }
  return (
    <div className="mt-6">
      {error && (
        <p
          role="alert"
          className="mb-4 rounded-xl bg-amber-50 p-4 text-sm text-amber-800"
        >
          {error}
        </p>
      )}
      {alreadyJoined ? (
        <Link
          href="/rooms"
          className="block rounded-xl bg-indigo-600 py-3 text-center text-sm font-semibold text-white"
        >
          참여 중인 모임 보기
        </Link>
      ) : (
        <button
          type="button"
          onClick={join}
          disabled={joining}
          className="w-full rounded-xl bg-indigo-600 py-3 text-sm font-semibold text-white hover:bg-indigo-700 disabled:opacity-50"
        >
          {joining ? "모임에 참여하는 중…" : "초대 수락하고 참여하기"}
        </button>
      )}
    </div>
  );
}
