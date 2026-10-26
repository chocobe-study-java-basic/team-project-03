"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import ConfirmDialog from "../common/ConfigmDialog/ConfirmDialog";
import { deleteRoom, getRoom, updateRoom, type Room } from "../../lib/api/roomsApi";
import { ApiError } from "../../lib/api/types";

function formatBudget(amount: number, currency: string) {
  const formatted = new Intl.NumberFormat("ko-KR").format(amount);
  return currency === "KRW" ? `${formatted}원` : `${currency} ${formatted}`;
}

function formatCreatedAt(createdAt: string) {
  const date = new Date(createdAt);

  return Number.isNaN(date.getTime())
    ? createdAt
    : new Intl.DateTimeFormat("ko-KR", {
        year: "numeric",
        month: "long",
        day: "numeric",
      }).format(date);
}

function getRoomId(value: string | string[] | undefined) {
  if (typeof value !== "string" || !/^\d+$/.test(value)) {
    return null;
  }

  const roomId = Number(value);
  return Number.isSafeInteger(roomId) && roomId > 0 ? roomId : null;
}

export default function RoomDetailPage() {
  const { roomId: roomIdParam } = useParams<{ roomId?: string | string[] }>();
  const router = useRouter();
  const roomId = useMemo(() => getRoomId(roomIdParam), [roomIdParam]);
  const [room, setRoom] = useState<Room | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(roomId !== null);
  const [redirectingToLogin, setRedirectingToLogin] = useState(false);
  const [isEditingName, setIsEditingName] = useState(false);
  const [nameInput, setNameInput] = useState("");
  const [updateError, setUpdateError] = useState<string | null>(null);
  const [savingName, setSavingName] = useState(false);
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false);
  const [deleteConfirmation, setDeleteConfirmation] = useState("");
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [deleting, setDeleting] = useState(false);


  const loadRoom = async () => {
    if (roomId === null) return;

    setLoading(true);
    setError(null);
    setRedirectingToLogin(false);

    try {
      setRoom(await getRoom(roomId));
    } catch (caughtError) {
      if (caughtError instanceof ApiError && caughtError.status === 401) {
        setRedirectingToLogin(true);
        return;
      }

      setError(
        caughtError instanceof ApiError
          ? caughtError.message
          : "모임 정보를 불러오지 못했습니다.",
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (roomId === null) {
      return;
    }

    let cancelled = false;

    getRoom(roomId)
      .then((loadedRoom) => {
        if (!cancelled) {
          setRoom(loadedRoom);
        }
      })
      .catch((caughtError: unknown) => {
        if (cancelled) {
          return;
        }

        if (caughtError instanceof ApiError && caughtError.status === 401) {
          setRedirectingToLogin(true);
          return;
        }

        setError(
          caughtError instanceof ApiError
            ? caughtError.message
            : "모임 정보를 불러오지 못했습니다.",
        );
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [roomId]);

  if (roomId === null) {
    return (
      <main className="min-h-screen bg-[#f8f8fb] px-5 py-12 text-zinc-900 sm:px-8 sm:py-20">
        <section className="mx-auto max-w-xl rounded-2xl border border-zinc-200 bg-white px-6 py-14 text-center shadow-sm">
          <h1 className="text-xl font-bold">잘못된 모임 주소입니다.</h1>
          <p className="mt-2 text-sm text-zinc-500">모임 목록에서 다시 선택해 주세요.</p>
          <Link href="/rooms" className="mt-6 inline-flex rounded-xl bg-indigo-600 px-4 py-2.5 text-sm font-semibold text-white hover:bg-indigo-700">내 모임으로 돌아가기</Link>
        </section>
      </main>
    );
  }

  if (loading || redirectingToLogin) {
    return (
      <main className="min-h-screen bg-[#f8f8fb] px-5 py-12 text-zinc-900 sm:px-8 sm:py-20">
        <p className="text-center text-sm text-zinc-500">{redirectingToLogin ? "로그인 페이지로 이동하고 있습니다." : "모임 정보를 불러오는 중입니다."}</p>
      </main>
    );
  }

  if (error || !room) {
    return (
      <main className="min-h-screen bg-[#f8f8fb] px-5 py-12 text-zinc-900 sm:px-8 sm:py-20">
        <section className="mx-auto max-w-xl rounded-2xl border border-red-200 bg-red-50 px-6 py-14 text-center">
          <h1 className="text-lg font-bold text-red-700">모임 정보를 불러오지 못했습니다.</h1>
          <p className="mt-2 text-sm text-red-600">{error ?? "잠시 후 다시 시도해 주세요."}</p>
          <div className="mt-6 flex justify-center gap-2"><button type="button" onClick={() => void loadRoom()} className="rounded-xl border border-red-200 bg-white px-4 py-2.5 text-sm font-semibold text-red-600">다시 시도</button><Link href="/rooms" className="rounded-xl border border-zinc-200 bg-white px-4 py-2.5 text-sm font-semibold text-zinc-700">내 모임으로</Link></div>
        </section>
      </main>
    );
  }

  const usedBudget = Math.max(0, room.totalBudget - room.availableBudget);
  const usedRate = room.totalBudget > 0
    ? Math.max(0, Math.min(100, (usedBudget / room.totalBudget) * 100))
    : 0;
  const availableRate = 100 - usedRate;
  const updatedName = nameInput.trim();
  const nameError = !updatedName
    ? "모임 이름을 입력해 주세요."
    : updatedName.length > 20
      ? "모임 이름은 20자 이하여야 합니다."
      : null;

  const startEditingName = () => {
    setNameInput(room.name);
    setUpdateError(null);
    setIsEditingName(true);
  };

  const cancelEditingName = () => {
    if (savingName) return;

    setNameInput("");
    setUpdateError(null);
    setIsEditingName(false);
  };

  const submitName = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (nameError || updatedName === room.name) {
      setUpdateError(nameError);
      return;
    }

    setSavingName(true);
    setUpdateError(null);

    try {
      const updatedRoom = await updateRoom(room.id, { name: updatedName });
      setRoom(updatedRoom);
      setIsEditingName(false);
      setNameInput("");
    } catch (caughtError) {
      if (caughtError instanceof ApiError && caughtError.status === 401) {
        setRedirectingToLogin(true);
        return;
      }

      if (caughtError instanceof ApiError && caughtError.status === 403) {
        setUpdateError("방장만 모임 이름을 수정할 수 있습니다.");
        return;
      }

      setUpdateError(
        caughtError instanceof ApiError
          ? caughtError.message
          : "모임 이름을 수정하지 못했습니다. 잠시 후 다시 시도해 주세요.",
      );
    } finally {
      setSavingName(false);
    }
  };
  const openDeleteDialog = () => {
    setDeleteConfirmation("");
    setDeleteError(null);
    setIsDeleteDialogOpen(true);
  };


  const closeDeleteDialog = () => {
    if (deleting) return;

    setIsDeleteDialogOpen(false);
    setDeleteConfirmation("");
    setDeleteError(null);
  };

  const submitDelete = async () => {
    if (deleteConfirmation !== room.name || deleting) return;

    setDeleting(true);
    setDeleteError(null);

    try {
      await deleteRoom(room.id);
      router.replace("/rooms");
    } catch (caughtError) {
      if (caughtError instanceof ApiError && caughtError.status === 401) {
        setRedirectingToLogin(true);
        return;
      }

      if (caughtError instanceof ApiError && caughtError.status === 403) {
        setDeleteError("방장만 모임을 삭제할 수 있습니다.");
        return;
      }

      setDeleteError(
        caughtError instanceof ApiError
          ? caughtError.message
          : "모임을 삭제하지 못했습니다. 잠시 후 다시 시도해 주세요.",
      );
    } finally {
      setDeleting(false);
    }
  };

  return (
    <main className="min-h-screen bg-[#f8f8fb] px-5 py-12 text-zinc-900 sm:px-8 sm:py-20">
      <div className="mx-auto w-full max-w-3xl">
        <Link href="/rooms" className="inline-flex items-center gap-1 text-sm font-medium text-zinc-500 transition-colors hover:text-zinc-900"><span aria-hidden>‹</span> 내 모임으로</Link>

        <header className="mt-8 flex flex-wrap items-start justify-between gap-5">
          <div className="flex min-w-0 flex-1 items-start gap-4"><span className="mt-1 flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl bg-indigo-50 text-xl font-bold text-indigo-500">{room.name.charAt(0)}</span><div className="min-w-0 flex-1"><p className="text-sm font-medium text-zinc-500">{room.currency} · {formatCreatedAt(room.createdAt)} 생성</p>{isEditingName ? <form onSubmit={(event) => void submitName(event)} className="mt-2 max-w-md"><label htmlFor="room-name" className="sr-only">모임 이름</label><div className="flex flex-wrap gap-2"><input id="room-name" value={nameInput} onChange={(event) => { setNameInput(event.target.value); if (updateError) setUpdateError(null); }} disabled={savingName} maxLength={20} className="h-11 min-w-0 flex-1 rounded-xl border border-zinc-300 px-3 text-lg font-bold outline-none focus:ring-2 focus:ring-indigo-100 disabled:bg-zinc-50" autoFocus /><button type="button" onClick={cancelEditingName} disabled={savingName} className="rounded-xl border border-zinc-200 bg-white px-3 py-2 text-sm font-semibold text-zinc-600 disabled:cursor-not-allowed">취소</button><button type="submit" disabled={savingName || Boolean(nameError) || updatedName === room.name} className="rounded-xl bg-indigo-600 px-3 py-2 text-sm font-semibold text-white hover:bg-indigo-700 disabled:cursor-not-allowed">{savingName ? "저장 중" : "저장"}</button></div>{(updateError || nameError) && <p role="alert" className="mt-2 text-sm font-medium text-red-600">{updateError ?? nameError}</p>}</form> : <h1 className="mt-1 truncate text-3xl font-bold tracking-tight">{room.name}</h1>}</div></div>
          <div className="flex flex-wrap gap-2">{!isEditingName && <><button type="button" onClick={startEditingName} className="rounded-xl border border-indigo-200 bg-white px-4 py-2.5 text-sm font-semibold text-indigo-600 transition-colors hover:bg-indigo-50">모임 이름 수정</button><button type="button" onClick={openDeleteDialog} className="rounded-xl border border-red-200 bg-white px-4 py-2.5 text-sm font-semibold text-red-600 transition-colors hover:bg-red-50">모임 삭제</button></>}<Link href={`/rooms/${room.id}/dashboard`} className="rounded-xl bg-indigo-600 px-4 py-2.5 text-sm font-semibold text-white shadow-sm transition-colors hover:bg-indigo-700">이 모임 열기</Link></div>
        </header>

        <section className="mt-10 rounded-2xl border border-zinc-200 bg-white p-6 shadow-sm sm:p-8" aria-labelledby="budget-title">
          <div className="mt-1 flex items-center gap-3">
            <h2 id="budget-title" className="text-xl font-bold">
              {room.name}의 예산
            </h2>

          </div>
          <dl className="mt-8 grid gap-6 sm:grid-cols-3"><div><dt className="text-sm text-zinc-500">현재 잔액</dt><dd className="mt-2 text-2xl font-extrabold tracking-tight">{formatBudget(room.totalBudget, room.currency)}</dd></div><div><dt className="text-sm text-zinc-500">선점 금액</dt><dd className="mt-2 text-2xl font-extrabold tracking-tight text-indigo-600">{formatBudget(usedBudget, room.currency)}</dd></div><div><dt className="text-sm text-zinc-500">사용 가능</dt><dd className="mt-2 text-2xl font-extrabold tracking-tight text-emerald-600">{formatBudget(room.availableBudget, room.currency)}</dd></div></dl>
          <div className="mt-9"><div className="flex h-3 overflow-hidden rounded-full bg-zinc-100" aria-label={`${room.name} 예산 사용 현황`}><span className="bg-indigo-500" style={{ width: `${usedRate}%` }} /><span className="bg-emerald-100" style={{ width: `${availableRate}%` }} /></div><div className="mt-3 flex flex-wrap gap-x-4 gap-y-2 text-sm text-zinc-500"><span className="flex items-center gap-1.5"><i className="h-2.5 w-2.5 rounded-full bg-indigo-500" />선점 금액</span><span className="flex items-center gap-1.5"><i className="h-2.5 w-2.5 rounded-full bg-emerald-300" />사용 가능</span></div></div>
        </section>
      </div>

      <ConfirmDialog
        open={isDeleteDialogOpen}
        title="모임을 삭제할까요?"
        description="모임의 멤버, 예산 신청, 정산 내역, 초대 링크가 모두 영구 삭제됩니다. 계속하려면 아래에 모임 이름을 정확히 입력해 주세요."
        confirmLabel="모임 삭제"
        variant="danger"
        loading={deleting}
        confirmDisabled={deleteConfirmation !== room.name}
        onCancel={closeDeleteDialog}
        onConfirm={() => void submitDelete()}
      >
        <label htmlFor="delete-room-confirmation" className="block text-sm font-semibold text-zinc-800">
          확인을 위해 <span className="text-red-600">{room.name}</span>을 입력해 주세요.
        </label>
        <input
          id="delete-room-confirmation"
          value={deleteConfirmation}
          onChange={(event) => {
            setDeleteConfirmation(event.target.value);
            if (deleteError) setDeleteError(null);
          }}
          disabled={deleting}
          autoComplete="off"
          className="mt-2 h-11 w-full rounded-xl border border-zinc-300 px-3 text-sm outline-none focus:ring-2 focus:ring-red-100 disabled:bg-zinc-50"
        />
        {deleteError && <p role="alert" className="mt-2 text-sm font-medium text-red-600">{deleteError}</p>}
      </ConfirmDialog>
    </main>
  );
}
