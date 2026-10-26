import Link from "next/link";
import type { ReactNode } from "react";

export default function AuthFrame({ children }: { children: ReactNode }) {
  return (
    <main className="auth-layout">
      <section className="auth-story" aria-label="Budzet 소개">
        <Link
          href="/login"
          className="flex items-center gap-3 text-xl font-extrabold tracking-tight"
        >
          <span className="brand-mark">B</span>Budzet
          <span className="ml-1 text-xs font-medium text-slate-400">
            TEAM BUDGET
          </span>
        </Link>
        <div className="auth-story-content">
          <span className="eyebrow text-indigo-300">
            함께 쓰는 예산, 함께 보는 흐름
          </span>
          <h1 className="mt-6 text-4xl font-bold leading-[1.4] tracking-tight lg:text-5xl">
            우리 팀의 예산을
            <br />더 명확하게.
          </h1>
          <p className="mt-5 max-w-sm text-base leading-8 text-slate-300">
            신청부터 승인, 정산까지.
            <br />
            흩어진 예산 관리를 한곳에서 이어가세요.
          </p>
          <div className="auth-flow" aria-label="예산 관리 흐름">
            {["예산 신청", "관리자 승인", "사용 후 정산"].map((step, index) => (
              <div key={step}>
                <span className="auth-step-number">0{index + 1}</span>
                <span>{step}</span>
                {index < 2 && (
                  <span aria-hidden="true" className="ml-auto text-slate-500">
                    ↓
                  </span>
                )}
              </div>
            ))}
          </div>
        </div>
        <p className="text-xs text-slate-400">
          작은 모임부터 팀 프로젝트까지, Budzet
        </p>
      </section>
      <section className="auth-form-area">
        <Link
          href="/login"
          className="mb-10 flex items-center gap-2 text-xl font-extrabold md:hidden"
        >
          <span className="brand-mark">B</span>Budzet
        </Link>
        <div className="w-full max-w-md">{children}</div>
        <p className="mt-8 text-center text-xs leading-6 text-zinc-500">
          팀의 예산을 관리하는 가장 명확한 방법.
        </p>
      </section>
    </main>
  );
}
