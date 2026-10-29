"use client";

import { useRef, useState, type FormEvent } from "react";
import { updatePassword } from "../lib/api/userApi";

type Field = "currentPassword" | "newPassword" | "confirmPassword";
const fields: { name: Field; label: string; autoComplete: string }[] = [
    { name: "currentPassword", label: "현재 비밀번호", autoComplete: "current-password" },
    { name: "newPassword", label: "새 비밀번호", autoComplete: "new-password" },
    { name: "confirmPassword", label: "새 비밀번호 확인", autoComplete: "new-password" },
];

export default function PasswordChangeForm() {
    const [values, setValues] = useState<Record<Field, string>>({ currentPassword: "", newPassword: "", confirmPassword: "" });
    const [errors, setErrors] = useState<Partial<Record<Field, string>>>({});
    const [error, setError] = useState("");
    const [saving, setSaving] = useState(false);
    const [showPassword, setShowPassword] = useState(false);
    const submitting = useRef(false);
    const inputs = useRef<Partial<Record<Field, HTMLInputElement | null>>>({});

    const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();
        if (submitting.current) return;
        const nextErrors: Partial<Record<Field, string>> = {};
        if (!values.currentPassword.trim()) nextErrors.currentPassword = "현재 비밀번호를 입력해주세요.";
        if (!values.newPassword.trim()) nextErrors.newPassword = "새 비밀번호를 입력해주세요.";
        else if (values.newPassword.length < 8 || values.newPassword.length > 20) nextErrors.newPassword = "비밀번호는 8자 이상 20자 이하로 입력해주세요.";
        else if (values.newPassword === values.currentPassword) nextErrors.newPassword = "새 비밀번호는 현재 비밀번호와 달라야 합니다.";
        if (!values.confirmPassword) nextErrors.confirmPassword = "새 비밀번호를 다시 입력해주세요.";
        else if (values.confirmPassword !== values.newPassword) nextErrors.confirmPassword = "새 비밀번호가 일치하지 않습니다.";
        setErrors(nextErrors);
        setError("");
        const firstInvalid = fields.find((field) => nextErrors[field.name]);
        if (firstInvalid) {
            inputs.current[firstInvalid.name]?.focus();
            return;
        }

        submitting.current = true;
        setSaving(true);
        try {
            await updatePassword({ currentPassword: values.currentPassword, newPassword: values.newPassword });
            setValues({ currentPassword: "", newPassword: "", confirmPassword: "" });
            window.location.replace("/login?passwordChanged=1");
        } catch (caughtError) {
            setError(caughtError instanceof Error ? caughtError.message : "비밀번호를 변경하지 못했습니다. 다시 시도해주세요.");
            submitting.current = false;
            setSaving(false);
        }
    };

    return (
        <section aria-labelledby="password-heading" className="mt-6 rounded-2xl border border-zinc-200 bg-white p-6 shadow-sm sm:p-8">
            <h2 id="password-heading" className="text-lg font-bold">비밀번호 변경</h2>
            <p id="password-help" className="mt-2 text-sm text-zinc-500">새 비밀번호는 8~20자로 입력해주세요. 변경 후에는 다시 로그인해야 합니다.</p>
            <form onSubmit={handleSubmit} noValidate aria-busy={saving} className="mt-6 space-y-5">
                {fields.map(({ name, label, autoComplete }) => (
                    <div key={name}>
                        <label htmlFor={name} className="block text-sm font-medium text-zinc-700">{label}</label>
                        <input id={name} name={name} type={showPassword ? "text" : "password"}
                            ref={(input) => { inputs.current[name] = input; }}
                            autoComplete={autoComplete} required disabled={saving} value={values[name]}
                            aria-invalid={!!errors[name]}
                            aria-describedby={errors[name] ? `${name}-error` : name === "newPassword" ? "password-help" : undefined}
                            onChange={(event) => {
                                setValues((previous) => ({ ...previous, [name]: event.target.value }));
                                setErrors({});
                                setError("");
                            }}
                            className="mt-2 h-12 w-full rounded-xl border border-zinc-300 px-4 text-sm outline-none transition focus:border-indigo-600 focus:ring-2 focus:ring-indigo-100 disabled:bg-zinc-50"
                        />
                        {errors[name] && <p id={`${name}-error`} role="alert" className="mt-2 text-sm text-red-600">{errors[name]}</p>}
                    </div>
                ))}
                <label className="flex w-fit items-center gap-2 text-sm text-zinc-600">
                    <input type="checkbox" checked={showPassword} disabled={saving} onChange={(event) => setShowPassword(event.target.checked)} className="h-4 w-4 accent-indigo-600" />
                    비밀번호 표시
                </label>
                {error && <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-600">{error}</p>}
                <div className="flex justify-end">
                    <button type="submit" disabled={saving} className="w-full rounded-xl bg-indigo-600 px-5 py-3 text-sm font-semibold text-white transition hover:bg-indigo-700 disabled:opacity-50 sm:w-auto">
                        {saving ? "변경 중..." : "비밀번호 변경"}
                    </button>
                </div>
            </form>
        </section>
    );
}
