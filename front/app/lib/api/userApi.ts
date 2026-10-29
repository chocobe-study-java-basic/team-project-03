import { ApiError } from "./types";
import { apiFetch, redirectToLogin } from "./client";
import { performLogout, performPasswordChange, startAuthentication } from "./tokenManager";

export type User = {
    id: number;
    email: string;
    name: string;
};

export type UserJoinRequest = {
    email: string;
    password: string;
    name: string;
};

export type UserLoginRequest = {
    email: string;
    password: string;
};

export type UserLoginResponse = {
    user: User;
    accessToken: string;
};

export async function signup(request: UserJoinRequest) {
    return apiFetch<User>(
        "/users/join",
        {
            method: "POST",
            body: request,
            redirectOnUnauthorized: false,
            skipTokenRefresh: true,
        },
    );
}

export async function login(request: UserLoginRequest) {
    const response = await apiFetch<UserLoginResponse>(
        "/users/login",
        {
            method: "POST",
            body: request,
            redirectOnUnauthorized: false,
            skipTokenRefresh: true,
        },
    );
    startAuthentication(response.accessToken);
    return response;
}

export async function logout() {
    try {
        await performLogout(
            () => apiFetch<{ accessToken: string }>("/users/refresh", {
                method: "POST",
                redirectOnUnauthorized: false,
                skipTokenRefresh: true,
            }),
            () => apiFetch<void>("/users/logout", {
                method: "DELETE",
                redirectOnUnauthorized: false,
                skipTokenRefresh: true,
            }),
        );
    } catch (error) {
        // 갱신/로그아웃 실패 시에도 자동 갱신을 종료합니다. 서버 성공으로 취급하지 않습니다.
        redirectToLogin();
        throw error;
    }
}

export async function getMe() {
    return apiFetch<User>(
        "/users/me",
    );
}

export type UserProfileUpdateRequest = {
    name: string;
};

export async function updateProfile(request: UserProfileUpdateRequest) {
    return apiFetch<User>("/users/me", {
        method: "PATCH",
        body: request,
    });
}


export type UserPasswordUpdateRequest = {
    currentPassword: string;
    newPassword: string;
};

export async function updatePassword(request: UserPasswordUpdateRequest) {
    await performPasswordChange(
        async () => {
            try {
                return await apiFetch<{ accessToken: string }>("/users/refresh", {
                    method: "POST",
                    redirectOnUnauthorized: false,
                    skipTokenRefresh: true,
                });
            } catch (error) {
                redirectToLogin();
                throw error;
            }
        },
        async () => {
            try {
                await apiFetch<void>("/users/me/password", {
                    method: "PATCH",
                    body: request,
                    redirectOnUnauthorized: false,
                    // performPasswordChange 안에서 갱신을 완료하고 인증 잠금을 유지
                    skipTokenRefresh: true,
                });
            } catch (error) {
                if (error instanceof ApiError && error.status === 401) {
                    // 현재 비밀번호 오류와 인증 만료가 모두 401이므로 로그인 상태 확인
                    await apiFetch<User>("/users/me", { skipTokenRefresh: true });
                }
                throw error;
            }
        },
    );
}
