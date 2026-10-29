"use client";

import Link from "next/link";
import PasswordChangeForm from "./PasswordChangeForm";
import { useEffect, useRef, useState, type FormEvent } from "react";
import BrandLogo from "../components/common/BrandLogo";
import UserProfileMenu from "../components/common/UserProfileMenu/UserProfileMenu";
import { getMe, updateProfile, type User } from "../lib/api/userApi";
import { ApiError } from "../lib/api/types";

export default function AccountPage() {
    const [user, setUser] = useState<User | null>(null);
    const [name, setName] = useState("");
    const [loading, setLoading] = useState(true);
    const [loadError, setLoadError] = useState("");
    const [loadAttempt, setLoadAttempt] = useState(0);
    const [saving, setSaving] = useState(false);
    const submitting = useRef(false);
    const nameInput = useRef<HTMLInputElement>(null);
    const [nameError, setNameError] = useState("");
    const [saveError, setSaveError] = useState("");
    const [success, setSuccess] = useState("");

    useEffect(() => {
        let cancelled = false;
        getMe()
            .then((loadedUser) => {
                if (cancelled) return;
                setUser(loadedUser);
                setName(loadedUser.name);
            })
            .catch((error: unknown) => {
                if (cancelled || (error instanceof ApiError && error.status === 401)) return;
                setLoadError(error instanceof Error ? error.message : "내 정보를 불러오지 못했습니다.");
            })
            .finally(() => {
                if (!cancelled) setLoading(false);
            });
        return () => { cancelled = true; };
    }, [loadAttempt]);

    const handleSave = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();
        if (!user || submitting.current) return;
        setNameError("");
        setSaveError("");
        setSuccess("");
        if (!name.trim()) {
            setNameError("이름을 입력해주세요.");
            nameInput.current?.focus();
            return;
        }
        if (name === user.name) return;

        submitting.current = true;
        setSaving(true);
        try {
            const updatedUser = await updateProfile({ name });
            setUser(updatedUser);
            setName(updatedUser.name);
            setSuccess("이름이 수정되었습니다.");
        } catch (error) {
            if (!(error instanceof ApiError && error.status === 401)) {
                setSaveError(error instanceof Error ? error.message : "이름을 수정하지 못했습니다. 다시 시도해주세요.");
            }
        } finally {
            submitting.current = false;
            setSaving(false);
        }
    };

    return (
        <main className="min-h-screen bg-background text-zinc-900">
            <header className="border-b border-zinc-200 bg-white">
                <div className="mx-auto flex min-h-20 max-w-6xl items-center justify-between gap-4 px-5 py-4 sm:px-8">
                    <Link href="/rooms" aria-label="Budzet 내 모임 목록"><BrandLogo /></Link>
                    {user && <div className="min-w-0 max-w-[50%]"><UserProfileMenu userName={user.name} /></div>}
                </div>
            </header>
            <div className="mx-auto max-w-2xl px-5 py-10 sm:px-8 sm:py-12">
                <Link href="/rooms" className="text-sm font-medium text-zinc-500 hover:text-indigo-600">← 내 모임 목록</Link>
                <h1 className="mt-6 text-3xl font-bold tracking-tight">내 정보</h1>
                <p className="mt-2 text-sm text-zinc-500">계정 정보를 확인하고 이름과 비밀번호를 변경할 수 있어요.</p>

                {loading && <p role="status" className="mt-8 rounded-2xl border border-zinc-200 bg-white p-8 text-sm text-zinc-500">내 정보를 불러오는 중이에요.</p>}
                {loadError && (
                    <div className="mt-8 rounded-2xl border border-red-200 bg-red-50 p-6">
                        <p role="alert" className="text-sm text-red-600">{loadError}</p>
                        <button type="button" onClick={() => {
                            setLoadError("");
                            setLoading(true);
                            setLoadAttempt((attempt) => attempt + 1);
                        }} className="mt-4 rounded-lg border border-red-200 bg-white px-3 py-2 text-sm font-semibold text-red-600">다시 시도</button>
                    </div>
                )}
                {user && (
                    <section aria-labelledby="profile-heading" className="mt-8 rounded-2xl border border-zinc-200 bg-white p-6 shadow-sm sm:p-8">
                        <h2 id="profile-heading" className="text-lg font-bold">기본 정보</h2>
                        <dl className="mt-6 border-b border-zinc-100 pb-6">
                            <dt className="text-sm font-medium text-zinc-700">이메일</dt>
                            <dd className="mt-2 break-all text-sm text-zinc-600">{user.email}</dd>
                        </dl>
                        <form onSubmit={handleSave} noValidate className="mt-6" aria-busy={saving}>
                            <label htmlFor="name" className="block text-sm font-medium text-zinc-700">이름</label>
                            <input ref={nameInput} id="name" name="name" autoComplete="name" required
                                value={name} disabled={saving} aria-invalid={!!nameError}
                                aria-describedby={nameError ? "name-error" : undefined}
                                onChange={(event) => {
                                    setName(event.target.value);
                                    setNameError("");
                                    setSaveError("");
                                    setSuccess("");
                                }}
                                className="mt-2 h-12 w-full rounded-xl border border-zinc-300 px-4 text-sm outline-none transition focus:border-indigo-600 focus:ring-2 focus:ring-indigo-100 disabled:bg-zinc-50"
                            />
                            {nameError && <p id="name-error" role="alert" className="mt-2 text-sm text-red-600">{nameError}</p>}
                            {saveError && <p role="alert" className="mt-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-600">{saveError}</p>}
                            {success && <p role="status" className="mt-4 rounded-lg bg-emerald-50 px-3 py-2 text-sm text-emerald-700">{success}</p>}
                            <div className="mt-6 flex justify-end">
                                <button type="submit" disabled={saving || name === user.name}
                                    className="w-full rounded-xl bg-indigo-600 px-5 py-3 text-sm font-semibold text-white transition hover:bg-indigo-700 disabled:opacity-50 sm:w-auto">
                                    {saving ? "저장 중..." : "이름 저장"}
                                </button>
                            </div>
                        </form>
                    </section>
                )}
                {user && <PasswordChangeForm />}
            </div>
        </main>
    );
}
