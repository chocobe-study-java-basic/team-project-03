"use client";


import { notify } from "@/app/components/common/Feedback";
import { useCallback, useEffect, useMemo, useState } from "react";
import { useParams } from "next/navigation";

import ConfirmDialog from "@/app/components/common/ConfigmDialog/ConfirmDialog";
import { getBudget } from "@/app/lib/api/budgetApi";
import {
    approveBudgetRequest,
    getBudgetRequests,
    rejectBudgetRequest,
    type BudgetRequest,
} from "@/app/lib/api/budgetRequestApi";

export default function ApprovalsPage() {
    const { roomId: roomIdParam } = useParams<{ roomId: string }>();
    const roomId = Number(roomIdParam);

    const [requests, setRequests] = useState<BudgetRequest[]>([]);
    const [availableBudget, setAvailableBudget] = useState<number | null>(null);

    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const [approveTarget, setApproveTarget] = useState<BudgetRequest | null>(null);
    const [processingId, setProcessingId] = useState<number | null>(null);

    const [rejectTarget, setRejectTarget] =
        useState<BudgetRequest | null>(null);

    const [rejectReason, setRejectReason] = useState("");
    const [rejecting, setRejecting] = useState(false);

    const loadApprovalData = useCallback(async () => {
        if (!roomId || Number.isNaN(roomId)) {
            setError("모임 정보를 찾을 수 없습니다.");
            setLoading(false);
            return;
        }

        try {
            setLoading(true);
            setError(null);

            const [requestData, budgetData] = await Promise.all([
                getBudgetRequests(roomId),
                getBudget(roomId),
            ]);

            setRequests(requestData);
            setAvailableBudget(budgetData.availableBudget);
        } catch (error) {
            console.error(
                "승인 관리 정보를 불러오지 못했습니다.",
                error,
            );

            setError(
                error instanceof Error
                    ? error.message
                    : "승인 관리 정보를 불러오지 못했습니다.",
            );
        } finally {
            setLoading(false);
        }
    }, [roomId]);

    useEffect(() => {
        let isSubscribed = true;

        // microtask로 스케줄링하여 Effect synchronous setState 경고 방지
        Promise.resolve().then(() => {
            if (isSubscribed) {
                void loadApprovalData();
            }
        });

        return () => {
            isSubscribed = false;
        };
    }, [loadApprovalData]);

    /**
     * REQUEST 상태만 승인 대기
     */
    const pendingRequests = useMemo(
        () =>
            requests.filter(
                (request) => request.status === "REQUEST",
            ),
        [requests],
    );

    /**
     * REQUEST가 아닌 요청은 처리 완료
     */
    const processedRequests = useMemo(
        () =>
            requests.filter(
                (request) => request.status !== "REQUEST",
            ),
        [requests],
    );

    const formatAmount = (amount: number) =>
        `${amount.toLocaleString("ko-KR")}원`;

    /**
     * 승인
     */
    const handleApprove = async (requestId: number) => {
        if (processingId !== null) return;
        try {
            setProcessingId(requestId);

            await approveBudgetRequest(roomId, requestId);
            setApproveTarget(null);

            notify("예산 신청이 승인되었습니다.");

            await loadApprovalData();
        } catch (error) {
            console.error(
                "예산 신청 승인에 실패했습니다.",
                error,
            );

            notify(
                error instanceof Error
                    ? error.message
                    : "예산 신청 승인에 실패했습니다.",
            );
        } finally {
            setProcessingId(null);
        }
    };

    /**
     * 반려 모달 열기
     */
    const handleOpenReject = (request: BudgetRequest) => {
        setRejectTarget(request);
        setRejectReason("");
    };

    /**
     * 반려 모달 닫기
     */
    const handleCloseReject = () => {
        if (rejecting) {
            return;
        }

        setRejectTarget(null);
        setRejectReason("");
    };

    /**
     * 반려
     */
    const handleReject = async () => {
        if (!rejectTarget) {
            return;
        }

        const reason = rejectReason.trim();

        if (!reason) {
            notify("반려 사유를 입력해주세요.");
            return;
        }

        if (reason.length > 20) {
            notify("반려 사유는 20자 이하로 입력해주세요.");
            return;
        }

        try {
            setRejecting(true);

            await rejectBudgetRequest(
                roomId,
                rejectTarget.id,
                reason,
            );

            notify("예산 신청이 반려되었습니다.");

            setRejectTarget(null);
            setRejectReason("");

            await loadApprovalData();
        } catch (error) {
            console.error(
                "예산 신청 반려에 실패했습니다.",
                error,
            );

            notify(
                error instanceof Error
                    ? error.message
                    : "예산 신청 반려에 실패했습니다.",
            );
        } finally {
            setRejecting(false);
        }
    };

    if (!roomId || Number.isNaN(roomId)) {
        return (
            <section className="mx-auto max-w-5xl">
                <p className="text-sm text-zinc-500">
                    모임 정보가 없습니다.
                </p>
            </section>
        );
    }

    return (
        <>
            <section className="mx-auto max-w-5xl">
                <div className="mb-7">
                    <h1 className="text-2xl font-bold text-zinc-900">
                        승인 관리
                    </h1>

                    <p className="mt-2 text-sm text-zinc-500">
                        멤버들의 예산 신청을 검토하고 승인 또는 반려하세요
                    </p>
                </div>

                {/* 현재 사용 가능한 예산 */}
                <div className="mb-7 flex items-center justify-between rounded-2xl border border-indigo-200 bg-indigo-50 px-6 py-5">
                    <div>
                        <p className="text-sm font-semibold text-indigo-700">
                            현재 사용 가능한 예산
                        </p>

                        <p className="mt-1 text-xs text-indigo-400">
                            이 금액 내에서 승인할 수 있어요
                        </p>
                    </div>

                    <p className="text-2xl font-bold text-indigo-600">
                        {availableBudget === null
                            ? "-"
                            : formatAmount(availableBudget)}
                    </p>
                </div>

                {/* 로딩 */}
                {loading && (
                    <div className="rounded-2xl border border-zinc-200 bg-white p-8 text-center text-sm text-zinc-500">
                        승인 관리 정보를 불러오는 중입니다.
                    </div>
                )}

                {/* 에러 */}
                {!loading && error && (
                    <div className="rounded-2xl border border-red-200 bg-red-50 p-8 text-center text-sm text-red-600">
                        <p>{error}</p>

                        <button
                            type="button"
                            onClick={() => void loadApprovalData()}
                            className="mt-4 rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-700"
                        >
                            다시 불러오기
                        </button>
                    </div>
                )}

                {/* 데이터 */}
                {!loading && !error && (
                    <>
                        {/* 승인 대기 */}
                        <div>
                            <div className="mb-3 flex items-center gap-1">
                                <h2 className="font-semibold text-zinc-900">
                                    대기 중
                                </h2>

                                <span className="text-sm font-semibold text-amber-600">
                                    ({pendingRequests.length}건)
                                </span>
                            </div>

                            {pendingRequests.length === 0 ? (
                                <div className="rounded-2xl border border-zinc-200 bg-white px-6 py-10 text-center text-sm text-zinc-400">
                                    대기 중인 예산 신청이 없습니다.
                                </div>
                            ) : (
                                <div className="space-y-3">
                                    {pendingRequests.map((request) => (
                                        <ApprovalCard
                                            key={request.id}
                                            request={request}
                                            formatAmount={formatAmount}
                                            processing={
                                                processingId === request.id
                                            }
                                            onApprove={async requestId => { setApproveTarget(requests.find(request => request.id === requestId) ?? null); }}
                                            onReject={handleOpenReject}
                                        />
                                    ))}
                                </div>
                            )}
                        </div>

                        {/* 처리된 요청 */}
                        <div className="mt-10">
                            <h2 className="mb-3 font-semibold text-zinc-900">
                                처리된 요청
                            </h2>

                            {processedRequests.length === 0 ? (
                                <div className="rounded-2xl border border-zinc-200 bg-white px-6 py-10 text-center text-sm text-zinc-400">
                                    처리된 요청이 없습니다.
                                </div>
                            ) : (
                                <div className="overflow-hidden rounded-2xl border border-zinc-200 bg-white">
                                    <div className="grid grid-cols-[1fr_1fr] gap-2 sm:grid-cols-[1fr_2fr_1fr_100px] bg-zinc-50 px-5 py-3 text-xs font-medium text-zinc-500">
                                        <span>신청자</span>
                                        <span>사유</span>
                                        <span>금액</span>
                                        <span>결과</span>
                                    </div>

                                    <div className="divide-y divide-zinc-100">
                                        {processedRequests.map((request) => (
                                            <div
                                                key={request.id}
                                                className="grid grid-cols-[1fr_2fr_1fr_100px] items-center px-5 py-4"
                                            >
                                                <span className="text-sm font-medium text-zinc-800">
                                                    {request.userName ??
                                                        `User #${request.userId}`}
                                                </span>

                                                <span className="truncate text-sm text-zinc-500">
                                                    {request.reason}
                                                </span>

                                                <span className="text-sm font-semibold text-zinc-900">
                                                    {formatAmount(
                                                        request.requestedAmount,
                                                    )}
                                                </span>

                                                <StatusBadge
                                                    status={request.status}
                                                />
                                            </div>
                                        ))}
                                    </div>
                                </div>
                            )}
                        </div>
                    </>
                )}
            </section>

            <ConfirmDialog open={approveTarget !== null} title="이 신청을 승인할까요?" description={approveTarget ? `${approveTarget.userName ?? "신청자"}님의 ${formatAmount(approveTarget.requestedAmount)} 신청을 승인하면 해당 금액이 가용 예산에서 선점됩니다.` : ""} confirmLabel="예산 승인" loading={processingId !== null} onCancel={() => setApproveTarget(null)} onConfirm={() => { if (approveTarget) void handleApprove(approveTarget.id); }} />
            {/* 반려 모달 */}
            {rejectTarget && (
                <RejectModal
                    request={rejectTarget}
                    rejectReason={rejectReason}
                    rejecting={rejecting}
                    onChangeReason={setRejectReason}
                    onClose={handleCloseReject}
                    onSubmit={handleReject}
                />
            )}
        </>
    );
}

function ApprovalCard({
    request,
    formatAmount,
    processing,
    onApprove,
    onReject,
}: {
    request: BudgetRequest;
    formatAmount: (amount: number) => string;
    processing: boolean;
    onApprove: (requestId: number) => Promise<void>;
    onReject: (request: BudgetRequest) => void;
}) {
    const date = new Date(
        request.createdAt,
    ).toLocaleDateString("ko-KR");

    return (
        <div className="rounded-2xl border border-zinc-200 bg-white p-5">
            <div className="flex items-start justify-between">
                <div className="flex items-center gap-3">
                    <div className="flex h-10 w-10 items-center justify-center rounded-full bg-indigo-50 text-sm font-semibold text-indigo-600">
                        #{request.userId}
                    </div>

                    <div>
                        <p className="font-semibold text-zinc-900">
                            {request.userName ??
                                `User #${request.userId}`}
                        </p>

                        <p className="mt-1 text-xs text-zinc-400">
                            {date}
                        </p>
                    </div>
                </div>

                <div className="text-right">
                    <p className="text-xl font-bold text-zinc-900">
                        {formatAmount(request.requestedAmount)}
                    </p>

                    <span className="mt-1 inline-flex rounded-full border border-amber-200 bg-amber-50 px-2 py-0.5 text-xs font-medium text-amber-600">
                        검토 중
                    </span>
                </div>
            </div>

            <div className="mt-5 rounded-xl bg-zinc-50 px-4 py-3">
                <p className="text-xs text-zinc-400">
                    신청 사유
                </p>

                <p className="mt-1 text-sm text-zinc-700">
                    {request.reason}
                </p>
            </div>


            <div className="mt-4 grid grid-cols-2 gap-2">
                <button
                    type="button"
                    disabled={processing}
                    onClick={() => void onApprove(request.id)}
                    className="rounded-xl bg-indigo-600 py-2.5 text-sm font-semibold text-white transition hover:bg-indigo-700 disabled:cursor-not-allowed disabled:opacity-50"
                >
                    {processing ? "처리 중..." : "승인"}
                </button>

                <button
                    type="button"
                    disabled={processing}
                    onClick={() => onReject(request)}
                    className="rounded-xl border border-red-200 bg-red-50 py-2.5 text-sm font-semibold text-red-500 transition hover:bg-red-100 disabled:cursor-not-allowed disabled:opacity-50"
                >
                    반려
                </button>
            </div>

        </div>
    );
}

function RejectModal({
    request,
    rejectReason,
    rejecting,
    onChangeReason,
    onClose,
    onSubmit,
}: {
    request: BudgetRequest;
    rejectReason: string;
    rejecting: boolean;
    onChangeReason: (value: string) => void;
    onClose: () => void;
    onSubmit: () => Promise<void>;
}) {
    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4">
            <div className="w-full max-w-md rounded-2xl bg-white p-6 shadow-xl">
                <h2 className="text-lg font-bold text-zinc-900">
                    예산 신청 반려
                </h2>

                <p className="mt-2 text-sm text-zinc-500">
                    {request.userName ??
                        `User #${request.userId}`}{" "}
                    님의 예산 신청을 반려합니다.
                </p>

                <div className="mt-5">
                    <div className="mb-2 flex items-center justify-between">
                        <label
                            htmlFor="rejectReason"
                            className="text-sm font-medium text-zinc-700"
                        >
                            반려 사유
                        </label>

                        <span className="text-xs text-zinc-400">
                            {rejectReason.length}/20
                        </span>
                    </div>

                    <textarea
                        id="rejectReason"
                        maxLength={20}
                        value={rejectReason}
                        onChange={(event) =>
                            onChangeReason(event.target.value)
                        }
                        placeholder="반려 사유를 입력해주세요."
                        rows={4}
                        autoFocus
                        className="w-full resize-none rounded-xl border border-zinc-200 px-4 py-3 text-sm outline-none transition focus:ring-2 focus:ring-red-100"
                    />
                </div>

                <div className="mt-5 grid grid-cols-2 gap-2">
                    <button
                        type="button"
                        disabled={rejecting}
                        onClick={onClose}
                        className="rounded-xl border border-zinc-200 bg-white py-2.5 text-sm font-semibold text-zinc-600 hover:bg-zinc-50 disabled:cursor-not-allowed disabled:opacity-50"
                    >
                        취소
                    </button>

                    <button
                        type="button"
                        disabled={rejecting}
                        onClick={() => void onSubmit()}
                        className="rounded-xl bg-red-500 py-2.5 text-sm font-semibold text-white hover:bg-red-600 disabled:cursor-not-allowed disabled:opacity-50"
                    >
                        {rejecting ? "반려 처리 중..." : "반려하기"}
                    </button>
                </div>
            </div>
        </div>
    );
}

function StatusBadge({ status }: { status: string }) {
    const statusMap: Record<
        string,
        {
            label: string;
            className: string;
        }
    > = {
        APPROVE: {
            label: "승인됨",
            className: "bg-emerald-50 text-emerald-600",
        },
        REJECT: {
            label: "반려됨",
            className: "bg-red-50 text-red-500",
        },
        SETTLEMENT: {
            label: "정산",
            className: "bg-zinc-100 text-zinc-600",
        },
    };

    const current = statusMap[status] ?? {
        label: status,
        className: "bg-zinc-100 text-zinc-600",
    };

    return (
        <span
            className={`inline-flex w-fit rounded-full px-2.5 py-1 text-xs font-medium ${current.className}`}
        >
            {current.label}
        </span>
    );
}
