"use client";

import BrandLogo from "../common/BrandLogo";
import Link from "next/link";
import { useParams, usePathname } from "next/navigation";
import type { CSSProperties, ReactNode } from "react";
import { useEffect, useRef, useState } from "react";

import { getMe } from "../../lib/api/userApi";
import { getMembers, getRoom } from "../../lib/api/roomsApi";
import { getBudgetRequests } from "../../lib/api/budgetRequestApi";
import NotificationBell from "../common/NotificationBell";
import UserProfileMenu from "../common/UserProfileMenu/UserProfileMenu";

type IconName =
    | "dashboard"
    | "budget"
    | "request"
    | "settlement"
    | "members"
    | "invite"
    | "approval"
    | "menu"
    | "close"
    | "settings"
    | "back";

const navigation: {
    label: string;
    path: string;
    icon: IconName;
    authority: string;
}[] = [
        { label: "대시보드", path: "dashboard", icon: "dashboard", authority: "멤버" },
        { label: "예산 변경", path: "budget", icon: "budget", authority: "운영자" },
        { label: "예산 신청", path: "budget-requests", icon: "request", authority: "멤버" },
        { label: "정산하기", path: "settlements", icon: "settlement", authority: "멤버" },
        { label: "멤버", path: "members", icon: "members", authority: "멤버" },
        { label: "초대하기", path: "invite/create", icon: "invite", authority: "방장" },
        { label: "승인 관리", path: "approvals", icon: "approval", authority: "운영자" },
    ];

const workspaceStyle = {
    "--workspace-sidebar-width": "16rem",
} as CSSProperties;

function Icon({
    name,
    className = "",
}: {
    name: IconName;
    className?: string;
}) {
    const svgProps = {
        className: `h-5 w-5 shrink-0 ${className}`,
        viewBox: "0 0 24 24",
        fill: "none",
        stroke: "currentColor",
        strokeWidth: 1.8,
        strokeLinecap: "round" as const,
        strokeLinejoin: "round" as const,
        "aria-hidden": true,
    };

    const content: Record<IconName, ReactNode> = {
        dashboard: (
            <>
                <rect x="3" y="3" width="7" height="7" rx="1" />
                <rect x="14" y="3" width="7" height="7" rx="1" />
                <rect x="3" y="14" width="7" height="7" rx="1" />
                <rect x="14" y="14" width="7" height="7" rx="1" />
            </>
        ),

        budget: <><path d="M7 4v16m-3-3 3 3 3-3M17 20V4m-3 3 3-3 3 3" /></>,

        request: (
            <>
                <rect x="5" y="3" width="14" height="18" rx="2" />
                <path d="M8 8h8M8 12h8M8 16h4" />
            </>
        ),

        settlement: (
            <>
                <path d="M5 4h14v16H5z" />
                <path d="M8 8h8M8 12l2 2 4-4M8 17h5" />
            </>
        ),

        members: (
            <>
                <circle cx="9" cy="8" r="3" />
                <path d="M3.5 20a5.5 5.5 0 0 1 11 0M16 11a3 3 0 1 0-1.8-5.4M16 14a5 5 0 0 1 4.5 3" />
            </>
        ),

        invite: (
            <>
                <path d="M10.5 13.5a4 4 0 0 0 5.7.1l2.1-2.1a4 4 0 0 0-5.7-5.7l-1.2 1.2" />
                <path d="M13.5 10.5a4 4 0 0 0-5.7-.1l-2.1 2.1a4 4 0 1 0 5.7 5.7l1.2-1.2" />
            </>
        ),

        approval: (
            <>
                <circle cx="12" cy="12" r="8.5" />
                <path d="m8.5 12 2.3 2.3 4.8-5" />
            </>
        ),

        menu: <path d="M4 7h16M4 12h16M4 17h16" />,

        close: <path d="m6 6 12 12M18 6 6 18" />,

        settings: (
            <path
                fill="currentColor"
                stroke="none"
                d="M19.43 12.98c.04-.32.07-.65.07-.98s-.02-.66-.07-.98l2.11-1.65a.5.5 0 0 0 .12-.64l-2-3.46a.5.5 0 0 0-.6-.22l-2.49 1a7.1 7.1 0 0 0-1.69-.98l-.38-2.65A.5.5 0 0 0 14 2h-4a.5.5 0 0 0-.5.42l-.38 2.65c-.61.25-1.18.59-1.69.98l-2.49-1a.5.5 0 0 0-.6.22l-2 3.46a.5.5 0 0 0 .12.64l2.11 1.65c-.04.32-.07.65-.07.98s.02.66.07.98l-2.11 1.65a.5.5 0 0 0-.12.64l2 3.46a.5.5 0 0 0 .6.22l2.49-1c.51.4 1.08.73 1.69.98l.38 2.65c.04.24.25.42.5.42h4c.25 0 .46-.18.5-.42l.38-2.65c.61-.25 1.18-.58 1.69-.98l2.49 1a.5.5 0 0 0 .6-.22l2-3.46a.5.5 0 0 0-.12-.64l-2.11-1.65ZM12 15.5A3.5 3.5 0 1 1 12 8a3.5 3.5 0 0 1 0 7.5Z"
            />
        ),

        back: <path d="m15 18-6-6 6-6" />,
    };

    return <svg {...svgProps}>{content[name]}</svg>;
}

function Sidebar({
    onNavigate,
    roomName,
    userName,
    userRole,
    pendingRequestCount,
}: {
    onNavigate?: () => void;
    roomName: string;
    userName: string;
    userRole: string;
    pendingRequestCount: number;
}) {
    const pathname = usePathname();

    const roomId = pathname.match(/^\/rooms\/([^/]+)/)?.[1];
    const roomBasePath = roomId ? `/rooms/${roomId}` : null;

    return (
        <aside className="workspace-sidebar flex h-full flex-col border-r border-zinc-200 bg-white px-3 py-5">
            <Link href="/rooms" onClick={onNavigate} className="mb-8 flex items-center gap-2.5 px-3 text-lg font-extrabold tracking-tight"><BrandLogo markClassName="h-9 w-9" /></Link>
            <Link
                href="/rooms"
                onClick={onNavigate}
                className="mb-5 flex items-center gap-2 px-2 text-sm font-medium text-zinc-500"
            >
                <Icon name="back" className="h-4 w-4" />
                내 모임 목록
            </Link>

            <Link
                href={roomBasePath ?? "/rooms"}
                onClick={onNavigate}
                className="mb-6 flex w-full items-center gap-3 rounded-xl px-2 py-2 text-left hover:bg-zinc-50"
            >
                <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-indigo-600 text-base font-bold text-white">
                    {roomName.charAt(0) || "모"}
                </span>

                <span className="min-w-0 flex-1 truncate text-[15px] font-semibold text-zinc-800">
                    {roomName}
                </span>

                <Icon name="settings" className="h-4 w-4 text-zinc-400" />
            </Link>

            <nav aria-label="업무 메뉴" className="space-y-1">
                {navigation
                    .filter((item) => {
                        if (userRole === "방장") {
                            return true;
                        }

                        if (userRole === "운영자") {
                            return (
                                item.authority === "운영자" ||
                                item.authority === "멤버"
                            );
                        }

                        if (userRole === "멤버") {
                            return item.authority === "멤버";
                        }

                        return false;
                    })
                    .map((item) => {
                        const href = roomBasePath
                            ? `${roomBasePath}/${item.path}`
                            : "/rooms";

                        const active = pathname === href || pathname.startsWith(`${href}/`);

                        return (
                            <Link
                                key={item.path}
                                aria-current={active ? "page" : undefined}
                                href={href}
                                onClick={onNavigate}
                                className={`workspace-nav flex h-11 items-center gap-3 rounded-xl px-3 text-sm font-medium transition-colors ${active
                                        ? "bg-indigo-50 text-indigo-600"
                                        : "text-zinc-600 hover:bg-zinc-50 hover:text-zinc-900"
                                    }`}
                            >
                                <Icon name={item.icon} />

                                <span className="flex-1">{item.label}</span>

                                {item.path === "approvals" &&
                                    pendingRequestCount > 0 && (
                                        <span className="flex h-5 min-w-5 items-center justify-center rounded-full bg-amber-100 px-1.5 text-xs font-semibold text-amber-700">
                                            {pendingRequestCount}
                                        </span>
                                    )}
                            </Link>
                        );

                    })}
            </nav>

            {/* 현재 로그인한 사용자 */}
            <div className="mt-auto border-t border-zinc-100 px-2 pt-4">
                <UserProfileMenu
                    userName={userName}
                    userRole={userRole}
                    menuPosition="top"
                />
            </div>
        </aside>
    );
}

export default function WorkspaceShell({
    children,
}: {
    children: ReactNode;
}) {
    const [isOpen, setIsOpen] = useState(false);
    const pathname = usePathname();
    const drawerRef = useRef<HTMLDivElement>(null);
    useEffect(() => {
        if (!isOpen) return;
        const previous = document.activeElement as HTMLElement | null;
        const overflow = document.body.style.overflow;
        document.body.style.overflow = "hidden";
        drawerRef.current?.querySelector<HTMLButtonElement>("button")?.focus();
        const handleKey = (event: KeyboardEvent) => {
            if (event.key === "Escape") setIsOpen(false);
            if (event.key !== "Tab") return;
            const items = drawerRef.current?.querySelectorAll<HTMLElement>("a[href],button:not([disabled])");
            if (!items?.length) return;
            const first = items[0], last = items[items.length - 1];
            if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus(); }
            else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); }
        };
        document.addEventListener("keydown", handleKey);
        return () => { document.body.style.overflow = overflow; document.removeEventListener("keydown", handleKey); previous?.focus(); };
    }, [isOpen]);

    const { roomId: roomIdParam } = useParams<{
        roomId?: string;
    }>();

    const roomId = roomIdParam ? Number(roomIdParam) : null;

    const [roomName, setRoomName] = useState("모임");

    // 현재 로그인한 사용자 정보
    const [userName, setUserName] = useState("");
    const [userRole, setUserRole] = useState("");
    const [accessCheck, setAccessCheck] = useState<{
        roomId: number;
        failed: boolean;
    } | null>(null);

    const restrictedPage = pathname.match(
        /^\/rooms\/[^/]+\/(budget|approvals|invite\/create)(?:\/|$)/,
    )?.[1];
    const validRoomId = roomId !== null && Number.isSafeInteger(roomId) && roomId > 0;
    const accessChecked = accessCheck?.roomId === roomId;
    const canAccess = userRole === "방장" ||
        (restrictedPage !== "invite/create" && userRole === "운영자");


    // 승인 요청 개수
    const [pendingRequestCount, setPendingRequestCount] = useState(0);

    useEffect(() => {
        if (!roomId || Number.isNaN(roomId)) {
            return;
        }

        let cancelled = false;
        const loadWorkspace = async () => {
            if (cancelled) return;
            setAccessCheck(null);
            try {
                const [room, user, members] = await Promise.all([
                    getRoom(roomId),
                    getMe(),
                    getMembers(roomId),
                ]);

                if (cancelled) return;

                // 모임 이름
                setRoomName(room.name);

                // 현재 로그인한 사용자의 이름
                setUserName(user.name);

                // 현재 로그인한 사용자의 이 모임 권한 찾기
                const currentMember = members.find(
                    (member) => member.userId === user.id,
                );

                if (currentMember) {
                    switch (currentMember.authority) {
                        case "OWNER":
                            setUserRole("방장");
                            break;

                        case "OPERATOR":
                            setUserRole("운영자");
                            break;

                        case "MEMBER":
                            setUserRole("멤버");
                            break;

                        default:
                            setUserRole(currentMember.authority);
                    }
                } else {
                    setUserRole("");
                }
                setAccessCheck({ roomId, failed: false });
            } catch (error) {
                if (cancelled) return;
                setUserRole("");
                setAccessCheck({ roomId, failed: true });
                console.error("모임 정보를 불러오지 못했습니다.", error);
            }
        };

        void Promise.resolve().then(loadWorkspace);
        return () => { cancelled = true; };
    }, [roomId]);

    useEffect(() => {
        if (!roomId || Number.isNaN(roomId)) {
            return;
        }

        const loadPendingRequestCount = async () => {
            try {
                const requests = await getBudgetRequests(roomId);

                const requestCount = requests.filter(
                    (request) => request.status === "REQUEST",
                ).length;

                setPendingRequestCount(requestCount);
            } catch (error) {
                console.error(
                    "승인 요청 개수를 불러오지 못했습니다.",
                    error,
                );
            }
        };

        void loadPendingRequestCount();
    }, [roomId]);

    return (
        <div
            className="workspace-shell min-h-screen text-zinc-900"
            style={workspaceStyle}
        >
            <div className="fixed right-4 top-3 z-30 md:right-10 md:top-8"><NotificationBell /></div>

            {/* PC 사이드바 */}
            <div className="fixed inset-y-0 left-0 z-20 hidden w-[var(--workspace-sidebar-width)] md:block">
                <Sidebar
                    roomName={roomName}
                    userName={userName}
                    userRole={userRole}
                    pendingRequestCount={pendingRequestCount}
                />
            </div>

            {/* 모바일 헤더 */}
            <header className="sticky top-0 z-10 flex h-16 items-center border-b border-zinc-200 bg-white px-4 md:hidden">
                <button
                    type="button"
                    aria-label="메뉴 열기"
                    aria-expanded={isOpen}
                    aria-controls="workspace-drawer"
                    onClick={() => setIsOpen(true)}
                    className="rounded-lg p-2 text-zinc-700 hover:bg-zinc-100"
                >
                    <Icon name="menu" />
                </button>

                <span className="ml-3 mr-12 truncate text-sm font-semibold">
                    {roomName}
                </span>
            </header>

            {/* 모바일 사이드바 */}
            {isOpen && (
                <div className="fixed inset-0 z-30 md:hidden">
                    <button
                        type="button"
                        aria-label="메뉴 닫기"
                        onClick={() => setIsOpen(false)}
                        className="absolute inset-0 bg-zinc-900/30"
                    />

                    <div id="workspace-drawer" ref={drawerRef} role="dialog" aria-modal="true" aria-label="모임 메뉴" className="relative h-full w-[var(--workspace-sidebar-width)] bg-white shadow-xl">
                        <button
                            type="button"
                            aria-label="메뉴 닫기"
                            onClick={() => setIsOpen(false)}
                            className="absolute right-3 top-4 rounded-lg p-2 text-zinc-600 hover:bg-zinc-100"
                        >
                            <Icon name="close" />
                        </button>

                        <Sidebar
                            roomName={roomName}
                            userName={userName}
                            userRole={userRole}
                            pendingRequestCount={pendingRequestCount}
                            onNavigate={() => setIsOpen(false)}
                        />
                    </div>
                </div>
            )}

            <main inert={isOpen || undefined} className="min-h-[calc(100vh-4rem)] px-5 py-6 md:ml-[var(--workspace-sidebar-width)] md:min-h-screen md:px-10 md:py-10 lg:px-14">
                <div className="mb-8 hidden items-center justify-between pr-14 border-b border-zinc-200 pb-5 md:flex"><p className="flex items-center gap-2 text-sm text-zinc-500"><span>{roomName}</span><span aria-hidden="true">/</span><span className="font-semibold text-zinc-800">{navigation.find(item => (pathname.endsWith(`/${item.path}`) || pathname.includes(`/${item.path}/`)))?.label ?? "모임 정보"}</span></p><span className="rounded-full border border-zinc-200 bg-white px-3 py-1 text-xs text-zinc-600">{userRole || "확인 중"}</span></div>
                {/* 일반 페이지는 그대로 표시하고, 제한 페이지는 권한 확인 후에만 표시 */}
                {/* 확인 중·조회 실패·권한 없음 상태에서는 페이지 내부 API도 실행되지 않음 */}
                {!restrictedPage ? children : !validRoomId ? (
                    <p role="alert" className="py-12 text-center text-sm text-red-600">잘못된 모임 주소입니다.</p>
                ) : !accessChecked ? (
                    <p role="status" className="py-12 text-center text-sm text-zinc-500">접근 권한을 확인하고 있습니다.</p>
                ) : accessCheck?.failed ? (
                    <section className="rounded-2xl border border-red-200 bg-white p-8 text-center">
                        <p role="alert" className="text-sm text-red-600">접근 권한을 확인하지 못했습니다. 새로고침 후 다시 시도해 주세요.</p>
                    </section>
                ) : !canAccess ? (
                    <section className="rounded-2xl border border-zinc-200 bg-white p-8 text-center">
                        <h1 className="text-xl font-bold">접근 권한이 없습니다.</h1>
                        <p className="mt-2 text-sm text-zinc-500">이 페이지를 이용할 수 있는 모임 권한이 없습니다.</p>
                        <Link href={`/rooms/${roomId}/dashboard`} className="mt-5 inline-flex rounded-xl bg-indigo-600 px-4 py-2.5 text-sm font-semibold text-white">대시보드로 이동</Link>
                    </section>
                ) : children}
            </main>
        </div>
    );
}
