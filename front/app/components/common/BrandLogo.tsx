export function BrandMark({ className = "h-10 w-10" }: { className?: string }) {
  return (
    <svg viewBox="0 0 56 56" fill="none" aria-hidden="true" focusable="false" className={`shrink-0 ${className}`}>
      <rect width="56" height="56" rx="14" fill="#4F46E5" />
      <path d="M15 12h13c7 0 11 3.4 11 8.4 0 3.1-1.4 5.4-4.1 6.8 3.2 1.3 5.1 3.8 5.1 7.1C40 40.3 35.6 44 28.4 44H15a2 2 0 0 1-2-2v-4a2 2 0 0 1 2-2h12c2.8 0 4.3-1.2 4.3-3.3s-1.5-3.2-4.3-3.2H15V12Z" fill="white" />
      <rect x="11" y="20" width="18" height="15" rx="2" fill="#4F46E5" />
      <rect x="11" y="25" width="14" height="6" rx="1.5" fill="white" />
    </svg>
  );
}

export default function BrandLogo({ className = "", markClassName = "h-10 w-10" }: { className?: string; markClassName?: string }) {
  return <span className={`inline-flex items-center gap-2.5 whitespace-nowrap ${className}`}>
    <BrandMark className={markClassName} />
    <span className="text-[25px] font-extrabold leading-none tracking-[-0.06em]">Budzet</span>
  </span>;
}
