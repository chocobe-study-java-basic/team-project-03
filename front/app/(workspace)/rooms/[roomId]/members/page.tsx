"use client";


import MemberRoleFilter from "@/app/components/rooms/MemberRoleFilter";
import DefaultAvatar from "@/app/components/common/DefaultAvatar";
import { notify } from "@/app/components/common/Feedback";
import Link from "next/link";
import ConfirmDialog from "@/app/components/common/ConfigmDialog/ConfirmDialog";
import { getMe } from "@/app/lib/api/userApi";
import { useEffect, useState } from "react";
import { useParams } from "next/navigation";

import {
    changeMemberAuthority,
    delegateOwner,
    getMembers,
    kickMember,
    leaveRoom,
    type Member,
} from "@/app/lib/api/memberApi";

type ActionType =
    | "PROMOTE"
    | "DEMOTE"
    | "KICK"
    | "DELEGATE_OWNER"
    | "LEAVE";

type ConfirmState = {
    type: ActionType;
    member?: Member;
} | null;

export default function MembersPage() {
    const { roomId } = useParams<{ roomId: string }>();

    const [currentUserId, setCurrentUserId] = useState<number | null>(null);
    const [query, setQuery] = useState("");
    const [roleFilter, setRoleFilter] = useState("ALL");
    const [members, setMembers] = useState<Member[] | null>(null);
    const [error, setError] = useState<string | null>(null);

    const [openMenuUserId, setOpenMenuUserId] = useState<number | null>(null);
    const [confirmState, setConfirmState] = useState<ConfirmState>(null);
    const [actionLoading, setActionLoading] = useState(false);

    const roomIdNumber = Number(roomId);

    useEffect(() => {
        if (!roomId || Number.isNaN(roomIdNumber)) {
            return;
        }

        const loadMembers = async () => {
            try {
                const [data, user] = await Promise.all([getMembers(roomIdNumber), getMe()]);
                setCurrentUserId(user.id);

                setMembers(data);
                setError(null);
            } catch (error) {
                console.error(error);
                setError("멤버 정보를 불러오지 못했습니다.");
            }
        };

        void loadMembers();
    }, [roomId, roomIdNumber]);

    const currentMember = members?.find(member => member.userId === currentUserId);
    const isOwner = currentMember?.authority === "OWNER";
    const canManage = isOwner || currentMember?.authority === "OPERATOR";
    const matchingMembers = members?.filter(member => member.name.toLocaleLowerCase().includes(query.trim().toLocaleLowerCase())) ?? [];
    const visibleMembers = matchingMembers.filter(member => roleFilter === "ALL" || member.authority === roleFilter);
    const roleCounts = Object.fromEntries(["ALL", "OWNER", "OPERATOR", "MEMBER"].map(role => [role, matchingMembers.filter(member => role === "ALL" || member.authority === role).length]));

    const getAuthorityLabel = (authority: Member["authority"]) => {
        switch (authority) {
            case "OWNER":
                return "방장";
            case "OPERATOR":
                return "운영자";
            case "MEMBER":
                return "멤버";
        }
    };

    const ownerCount =
        members?.filter(
            (member) => member.authority === "OWNER",
        ).length ?? 0;

    const operatorCount =
        members?.filter(
            (member) => member.authority === "OPERATOR",
        ).length ?? 0;

    const normalMemberCount =
        members?.filter(
            (member) => member.authority === "MEMBER",
        ).length ?? 0;

    const openConfirm = (
        type: ActionType,
        member?: Member,
    ) => {
        setOpenMenuUserId(null);

        setConfirmState({
            type,
            member,
        });
    };

    const closeConfirm = () => {
        if (actionLoading) {
            return;
        }

        setConfirmState(null);
    };

    const handleAction = async () => {
        if (!confirmState) {
            return;
        }

        if (!roomId || Number.isNaN(roomIdNumber)) {
            return;
        }

        const { type, member } = confirmState;

        try {
            setActionLoading(true);

            switch (type) {
                case "PROMOTE":
                    if (!member) {
                        return;
                    }

                    await changeMemberAuthority(
                        roomIdNumber,
                        member.userId,
                        "OPERATOR",
                    );
                    break;

                case "DEMOTE":
                    if (!member) {
                        return;
                    }

                    await changeMemberAuthority(
                        roomIdNumber,
                        member.userId,
                        "MEMBER",
                    );
                    break;

                case "KICK":
                    if (!member) {
                        return;
                    }

                    await kickMember(
                        roomIdNumber,
                        member.userId,
                    );
                    break;

                case "DELEGATE_OWNER":
                    if (!member) {
                        return;
                    }

                    await delegateOwner(
                        roomIdNumber,
                        member.userId,
                    );
                    break;

                case "LEAVE":
                    await leaveRoom(roomIdNumber);

                    window.location.replace("/rooms");
                    return;
            }

            const updatedMembers = await getMembers(roomIdNumber);

            setMembers(updatedMembers);
            setConfirmState(null);
            setOpenMenuUserId(null);
            setError(null);
        } catch (error) {
            console.error(error);

            notify(
                error instanceof Error
                    ? error.message
                    : "요청 처리에 실패했습니다.",
            );
        } finally {
            setActionLoading(false);
        }
    };

    if (!roomId || Number.isNaN(roomIdNumber)) {
        return (
            <section className="mx-auto max-w-5xl">
                <p className="text-sm text-zinc-500">
                    모임 정보가 없습니다.
                </p>
            </section>
        );
    }

    return (
        <section className="mx-auto max-w-5xl">
            {/* 헤더 */}
            <div className="mb-7 flex items-start justify-between">
                <div>
                    <h1 className="text-2xl font-bold text-zinc-900">
                        멤버
                    </h1>

                    <p className="mt-2 text-sm text-zinc-500">
                        총 {members?.length ?? 0}명이 함께하고 있어요
                    </p>
                </div>

                {isOwner && <Link href={`/rooms/${roomId}/invite/create`} className="rounded-xl bg-indigo-600 px-4 py-2.5 text-sm font-semibold text-white hover:bg-indigo-700">+ 멤버 초대</Link>}
            </div>

            {/* 로딩 */}
            {members === null && !error && (
                <div className="rounded-2xl border border-zinc-200 bg-white p-8 text-center text-sm text-zinc-500">
                    멤버 정보를 불러오는 중입니다.
                </div>
            )}

            {/* 에러 */}
            {members === null && error && (
                <div className="rounded-2xl border border-red-200 bg-red-50 p-8 text-center text-sm text-red-600">
                    {error}
                </div>
            )}

            {/* 멤버 목록 */}
            {members !== null && !error && (
                <>
                    <div className="mb-5 flex flex-wrap gap-3"><label className="min-w-0 flex-1"><span className="sr-only">멤버 이름 검색</span><input type="search" value={query} onChange={event => setQuery(event.target.value)} placeholder="멤버 이름으로 검색" className="w-full rounded-xl border border-zinc-200 bg-white px-4 py-3 text-sm" /></label><MemberRoleFilter value={roleFilter} onChange={setRoleFilter} counts={roleCounts} /></div>
                    <p aria-live="polite" className="mb-3 text-xs text-zinc-500">{visibleMembers.length}명의 멤버</p>
                    {visibleMembers.length === 0 && <div className="rounded-2xl border border-dashed border-zinc-300 bg-white p-10 text-center text-sm text-zinc-500">조건에 맞는 멤버가 없어요. 검색어나 권한 필터를 바꿔 보세요.</div>}
                    <div className="space-y-3">
                        {visibleMembers.map((member) => (
                            <div
                                key={member.userId}
                                className="relative flex items-center rounded-2xl border border-zinc-200 bg-white px-6 py-5"
                            >
                                {/* 프로필 */}
                                <DefaultAvatar className="h-11 w-11" />

                                {/* 이름 / 권한 */}
                                <div className="ml-4 min-w-0 flex-1">
                                    <div className="flex items-center gap-2">
                                        <p className="font-semibold text-zinc-900">
                                            {member.name}
                                        </p>

                                        <span
                                            className={`rounded-full px-2.5 py-1 text-xs font-medium ${
                                                member.authority ===
                                                "OWNER"
                                                    ? "bg-indigo-50 text-indigo-600"
                                                    : member.authority ===
                                                    "OPERATOR"
                                                        ? "bg-blue-50 text-blue-600"
                                                        : "bg-zinc-100 text-zinc-500"
                                            }`}
                                        >
                                            {getAuthorityLabel(
                                                member.authority,
                                            )}
                                        </span>
                                    </div>

                                    <p className="mt-1 text-sm text-zinc-400">
                                        {member.userId === currentUserId ? "내 계정" : "함께하는 멤버"}
                                    </p>
                                </div>

                                {/* 더보기 */}
                                {canManage && member.authority !== "OWNER" && member.userId !== currentUserId && <div className="relative">
                                    <button
                                        type="button"
                                        aria-label={`${member.name} 멤버 메뉴`}
                                        onClick={() =>
                                            setOpenMenuUserId(
                                                (current) =>
                                                    current ===
                                                    member.userId
                                                        ? null
                                                        : member.userId,
                                            )
                                        }
                                        className="flex h-9 w-9 items-center justify-center rounded-full text-xl text-zinc-400 transition hover:bg-zinc-100 hover:text-zinc-700"
                                    >
                                        ⋮
                                    </button>

                                    {openMenuUserId ===
                                        member.userId && (
                                            <MemberActionMenu
                                                member={member}
                                                isOwner={isOwner}
                                                onPromote={() =>
                                                    openConfirm(
                                                        "PROMOTE",
                                                        member,
                                                    )
                                                }
                                                onDemote={() =>
                                                    openConfirm(
                                                        "DEMOTE",
                                                        member,
                                                    )
                                                }
                                                onKick={() =>
                                                    openConfirm(
                                                        "KICK",
                                                        member,
                                                    )
                                                }
                                                onDelegateOwner={() =>
                                                    openConfirm(
                                                        "DELEGATE_OWNER",
                                                        member,
                                                    )
                                                }
                                            />
                                        )}
                                </div>}
                            </div>
                        ))}
                    </div>

                    {/* 통계 */}
                    <div className="mt-6 grid grid-cols-3 gap-4">
                        <StatCard
                            value={members.length}
                            label="전체 멤버"
                        />

                        <StatCard
                            value={ownerCount + operatorCount}
                            label="관리자"
                            highlight
                        />

                        <StatCard
                            value={normalMemberCount}
                            label="일반 멤버"
                        />
                    </div>

                    {/* 모임 나가기 */}
                    {!isOwner && <div className="mt-8 border-t border-zinc-200 pt-6">
                        <button
                            type="button"
                            onClick={() =>
                                openConfirm("LEAVE")
                            }
                            className="w-full rounded-xl border border-red-200 bg-white py-3.5 text-sm font-semibold text-red-500 transition hover:bg-red-50"
                        >
                            모임 나가기
                        </button>
                    </div>}
                </>
            )}

            {/* 확인 모달 */}
            {confirmState && (
                <ConfirmModal
                    state={confirmState}
                    loading={actionLoading}
                    onCancel={closeConfirm}
                    onConfirm={handleAction}
                />
            )}
        </section>
    );
}

function MemberActionMenu({
                              member,
                              isOwner,
                              onPromote,
                              onDemote,
                              onKick,
                              onDelegateOwner,
                          }: {
    member: Member;
    isOwner: boolean;
    onPromote: () => void;
    onDemote: () => void;
    onKick: () => void;
    onDelegateOwner: () => void;
}) {
    return (
        <div className="absolute right-0 top-11 z-20 w-44 overflow-hidden rounded-xl border border-zinc-200 bg-white py-1 shadow-lg">
            {isOwner && member.authority === "MEMBER" && (
                <button
                    type="button"
                    onClick={onPromote}
                    className="block w-full px-4 py-3 text-left text-sm text-zinc-700 transition hover:bg-zinc-50"
                >
                    관리자 지정
                </button>
            )}

            {isOwner && member.authority === "OPERATOR" && (
                <button
                    type="button"
                    onClick={onDemote}
                    className="block w-full px-4 py-3 text-left text-sm text-zinc-700 transition hover:bg-zinc-50"
                >
                    관리자 해제
                </button>
            )}

            {member.authority !== "OWNER" && (
                <button
                    type="button"
                    onClick={onKick}
                    className="block w-full px-4 py-3 text-left text-sm text-red-500 transition hover:bg-red-50"
                >
                    강퇴하기
                </button>
            )}

            {isOwner && member.authority !== "OWNER" && (
                <button
                    type="button"
                    onClick={onDelegateOwner}
                    className="block w-full px-4 py-3 text-left text-sm text-zinc-700 transition hover:bg-zinc-50"
                >
                    방장 위임
                </button>
            )}

            {member.authority === "OWNER" && (
                <div className="px-4 py-3 text-sm text-zinc-400">
                    현재 방장
                </div>
            )}
        </div>
    );
}

function ConfirmModal({
                          state,
                          loading,
                          onCancel,
                          onConfirm,
                      }: {
    state: ConfirmState;
    loading: boolean;
    onCancel: () => void;
    onConfirm: () => void;
}) {
    const memberName = state?.member?.name;

    let title = "";
    let description = "";
    let confirmText = "";
    let danger = false;

    switch (state?.type) {
        case "PROMOTE":
            title = "관리자로 지정할까요?";
            description = `${memberName}님을 관리자로 지정합니다.`;
            confirmText = "관리자 지정";
            break;

        case "DEMOTE":
            title = "관리자 권한을 해제할까요?";
            description = `${memberName}님의 관리자 권한을 해제합니다.`;
            confirmText = "권한 해제";
            break;

        case "KICK":
            title = "멤버를 강퇴할까요?";
            description = `${memberName}님을 모임에서 내보냅니다.`;
            confirmText = "강퇴하기";
            danger = true;
            break;

        case "DELEGATE_OWNER":
            title = "방장을 위임할까요?";
            description = `${memberName}님에게 방장 권한을 넘깁니다.`;
            confirmText = "방장 위임";
            danger = true;
            break;

        case "LEAVE":
            title = "모임을 나갈까요?";
            description =
                "모임을 나가면 다시 초대를 받아야 참여할 수 있습니다.";
            confirmText = "모임 나가기";
            danger = true;
            break;
    }

    return <ConfirmDialog open={Boolean(state)} title={title} description={description} confirmLabel={confirmText} variant={danger ? "danger" : "default"} loading={loading} onCancel={onCancel} onConfirm={onConfirm} />;
}

function StatCard({
                      value,
                      label,
                      highlight = false,
                  }: {
    value: number;
    label: string;
    highlight?: boolean;
}) {
    return (
        <div className="rounded-2xl border border-zinc-200 bg-white px-6 py-5 text-center">
            <p
                className={`text-2xl font-bold ${
                    highlight
                        ? "text-indigo-600"
                        : "text-zinc-900"
                }`}
            >
                {value}
            </p>

            <p className="mt-1 text-sm text-zinc-500">
                {label}
            </p>
        </div>
    );
}