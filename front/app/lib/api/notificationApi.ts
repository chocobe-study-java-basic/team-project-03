import { apiFetch } from "./client";

export type Notification = {
  id: number;
  roomId: number;
  roomName: string;
  type: "BUDGET_REQUESTED" | "BUDGET_APPROVED" | "BUDGET_REJECTED" | "MEMBER_JOINED";
  message: string;
  targetId: number;
  createdAt: string;
  readAt: string | null;
};
export type NotificationPage = { notifications: Notification[]; nextPage: number | null };
export function getNotifications(page = 0, signal?: AbortSignal) {
  return apiFetch<NotificationPage>(`/notifications?page=${page}&size=20`, { signal });
}
export function getUnreadNotificationCount(signal?: AbortSignal) {
  return apiFetch<{ count: number }>("/notifications/unread-count", { signal });
}
export function readNotification(id: number) {
  return apiFetch<void>(`/notifications/${id}/read`, { method: "PATCH" });
}
