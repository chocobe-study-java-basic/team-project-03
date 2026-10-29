"use client";

import { ApiError } from "./types";

const EXPIRES_AT_KEY = "budzet.accessTokenExpiresAt";
const STOPPED_KEY = "budzet.authStopped";
const AUTH_LOCK = "budzet.auth";
const REFRESH_MARGIN_MS = 1000 * 60 * 5 // 5분

let refreshPromise: Promise<void> | null = null;
let stopped = false;
let loggingOut = false;
let passwordChangePromise: Promise<void> | null = null;

export function isLoggingOut() {
  return loggingOut;
}

function isStopped() {
  return stopped || localStorage.getItem(STOPPED_KEY) === "true";
}

function saveExpiration(accessToken: string) {
  const payload = accessToken.split(".")[1];
  if (!payload) throw new ApiError("토큰 만료 정보를 확인할 수 없습니다.", 0);

  // 쿠키가 아닌 로그인/refresh 응답의 JWT에서 갱신 판단용 exp만 읽음
  const base64 = payload.replace(/-/g, "+").replace(/_/g, "/");
  const { exp } = JSON.parse(atob(base64.padEnd(Math.ceil(base64.length / 4) * 4, "="))) as { exp?: number };
  if (typeof exp !== "number" || !Number.isFinite(exp) || exp * 1000 <= Date.now()) {
    throw new ApiError("토큰 만료 정보가 올바르지 않습니다.", 0);
  }
  localStorage.setItem(EXPIRES_AT_KEY, String(exp * 1000));
}

export function startAuthentication(accessToken: string) {
  saveExpiration(accessToken);
  localStorage.removeItem(STOPPED_KEY);
  stopped = false;
}

export function stopAuthentication() {
  stopped = true;
  try {
    localStorage.setItem(STOPPED_KEY, "true");
    localStorage.removeItem(EXPIRES_AT_KEY);
  } catch {
    // 현재 탭에서는 stopped로 후속 갱신을 차단
  }
}

async function withAuthLock<T>(action: () => Promise<T>): Promise<T> {
  // 지원 브라우저에서는 RT를 공유하는 다른 탭과도 직렬화
  if (typeof navigator !== "undefined" && navigator.locks) {
    return navigator.locks.request(AUTH_LOCK, action);
  }
  return action();
}

type RefreshRequest = () => Promise<{ accessToken: string }>;

// 호출자는 인증 잠금을 잡은 상태여야 함
async function refreshIfNeeded(refresh: RefreshRequest) {
  if (isStopped()) throw new ApiError("로그인 후 이용해주세요.", 401);
  // 다른 탭이 이미 갱신했을 수 있으므로 잠금 안에서 다시 읽음
  const expiresAt = Number(localStorage.getItem(EXPIRES_AT_KEY));
  if (Number.isFinite(expiresAt) && expiresAt - Date.now() > REFRESH_MARGIN_MS) return;

  try {
    const response = await refresh();
    if (isStopped()) throw new ApiError("인증이 종료되었습니다.", 401);
    saveExpiration(response.accessToken);
  } catch (error) {
    stopAuthentication();
    throw error;
  }
}

export async function ensureFreshAccessToken(refresh: RefreshRequest) {
  if (typeof window === "undefined") return;
  // 비밀번호 변경 중에는 갱신을 기다리고, 실패했다면 기존 인증을 계속 사용
  await passwordChangePromise?.catch(() => undefined);
  if (loggingOut || isStopped()) throw new ApiError("로그인 후 이용해주세요.", 401);

  if (!refreshPromise) {
    refreshPromise = withAuthLock(() => refreshIfNeeded(refresh)).finally(() => {
      refreshPromise = null;
    });
  }
  await refreshPromise;
  // 기다리던 중 로그아웃이 시작됐다면 원래 API도 보내지 않음
  if (loggingOut || isStopped()) throw new ApiError("인증이 종료되었습니다.", 401);
}

let logoutPromise: Promise<void> | null = null;

export function performLogout(refresh: RefreshRequest, request: () => Promise<void>) {
  if (logoutPromise) return logoutPromise;
  loggingOut = true;
  logoutPromise = (async () => {
    await passwordChangePromise?.catch(() => undefined);
    // 먼저 시작된 토큰 갱신이 있다면 끝날 때까지 기다림
    await refreshPromise;
    await withAuthLock(async () => {
      // 다른 탭의 갱신 작업이 중간에 실행되지 않도록 함
      await refreshIfNeeded(refresh);
      stopAuthentication();
      await request();
    });
  })().finally(() => {
    loggingOut = false;
    logoutPromise = null;
  });
  return logoutPromise;
}


export function performPasswordChange(refresh: RefreshRequest, request: () => Promise<void>) {
  if (passwordChangePromise) return passwordChangePromise;
  if (loggingOut) return Promise.reject(new ApiError("로그아웃 중입니다.", 401));

  passwordChangePromise = (async () => {
    await refreshPromise;
    await withAuthLock(async () => {
      await refreshIfNeeded(refresh);
      // 검증 실패 시에는 인증 상태를 유지하고 성공했을 때만 갱신을 종료
      await request();
      stopAuthentication();
    });
  })().finally(() => {
    passwordChangePromise = null;
  });
  return passwordChangePromise;
}
