/** A neutral default profile shared by member lists and profile menus. */
export default function DefaultAvatar({ className = "h-10 w-10" }: { className?: string }) {
  return (
    <svg viewBox="0 0 48 48" aria-hidden="true" focusable="false" className={`shrink-0 rounded-full ${className}`}>
      <rect width="48" height="48" rx="24" fill="#F0F2F5" />
      <circle cx="24" cy="17" r="8" fill="#B3BDCA" />
      <path d="M7 48v-3c0-10.2 7.6-17 17-17s17 6.8 17 17v3H7Z" fill="#B3BDCA" />
    </svg>
  );
}
