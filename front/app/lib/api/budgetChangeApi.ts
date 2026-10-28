import { apiFetch } from "./client";

export type BudgetChangeCreateRequest = {
    changedBudget: number;
    reason: string;
};

export type BudgetChangeCreateResponse = {
    id: number;
    requestId: number;
    userId: number;
    changeBudget: number;
    changedBudget: number;
    budgetType: string;
    userName: string;
    processedAt: string;
    reason: string;
};

export type BudgetChange = {
    id: number;
    changeBudget: number;
    changedBudget: number;
    type: string;
    userName: string;
    reason: string;
    processedAt: string;
};

export type BudgetChangeListResponse = {
    changes: BudgetChange[];
};

export type BudgetChangeDetailResponse = {
    id: number;
    roomId: number;
    userId: number;
    requestId: number | null;
    changeBudget: number;
    changedBudget: number;
    budgetType: string;
    processedAt: string;
    userName: string;
    userEmail: string;
    reason: string;
    requestCreatedAt: string | null;
    requestUpdatedAt: string | null;
};

export type BudgetChangeUpdateRequest = {
    changeBudget: number;
    changedBudget: number;
    userName: string;
    userEmail: string;
    reason: string;
};

export type BudgetChangeUpdateResponse = {
    id: number;
    changeBudget: number;
    changedBudget: number;
    userName: string;
    userEmail: string;
    reason: string;
};

export async function createBudgetChange(
    roomId: number,
    requestId: number,
    request: BudgetChangeCreateRequest,
    receipt?: File | null,
) {
    if (receipt) {
        const body = new FormData();
        body.append("file", receipt);
        body.append("request", new Blob([JSON.stringify(request)], { type: "application/json" }));
        return apiFetch<BudgetChangeCreateResponse>(`/rooms/${roomId}/budget/changes/${requestId}/with-receipt`, { method: "POST", body });
    }
    return apiFetch<BudgetChangeCreateResponse>(
        `/rooms/${roomId}/budget/changes/${requestId}`,
        {
            method: "POST",
            body: request,
        },
    );
}

export async function getSettlementChanges(roomId: number) {
    return apiFetch<BudgetChangeListResponse>(
        `/rooms/${roomId}/budget/changes/settlement`,
    );
}

export async function getBudgetChange(
    roomId: number,
    changeId: number,
) {
    return apiFetch<BudgetChangeDetailResponse>(
        `/rooms/${roomId}/budget/changes/${changeId}`,
    );
}

export async function updateBudgetChange(
    roomId: number,
    changeId: number,
    request: BudgetChangeUpdateRequest,
) {
    return apiFetch<BudgetChangeUpdateResponse>(
        `/rooms/${roomId}/budget/changes/${changeId}`,
        {
            method: "PUT",
            body: request,
        },
    );
}