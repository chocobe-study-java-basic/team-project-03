import type { Metadata } from "next";
import type { ReactNode } from "react";
import "./globals.css";
import Feedback from "./components/common/Feedback";

export const metadata: Metadata = {
  title: "Budzet",
  description: "팀 예산 관리 서비스",
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="ko" className="h-full antialiased">
      <body className="min-h-full flex flex-col">{children}<Feedback /></body>
    </html>
  );
}
