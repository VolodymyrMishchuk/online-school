
import { client } from './client';

export interface NotificationDto {
    id: string;
    userId?: string;
    toUser?: any;
    recipient?: any;
    title: string;
    message: string;
    type: 'GENERIC' | 'SYSTEM' | 'COURSE_ACCESS_EXTENDED' | 'COURSE_PURCHASED' | 'NEW_USER_REGISTRATION' | 'ADMIN_ANNOUNCEMENT';
    read: boolean;
    createdAt: string;
    buttonUrl?: string;
}

export const getNotifications = async (): Promise<NotificationDto[]> => {
    const response = await client.get('/notifications');
    return response.data;
};

export const markAsRead = async (id: string): Promise<void> => {
    await client.put(`/notifications/${id}/read`);
};

export const markAsUnread = async (id: string): Promise<void> => {
    await client.put(`/notifications/${id}/unread`);
};

export interface BroadcastRequest {
    title: string;
    message: string;
    buttonUrl?: string;
}

export interface TargetedNotificationRequest {
    title: string;
    message: string;
    userIds: string[];
    buttonUrl?: string;
}

export const broadcastNotification = async (data: BroadcastRequest): Promise<void> => {
    await client.post('/notifications/broadcast', data);
};

export const sendTargetedNotification = async (data: TargetedNotificationRequest): Promise<void> => {
    await client.post('/notifications/send-to-users', data);
};

export const deleteNotification = async (id: string): Promise<void> => {
    await client.delete(`/notifications/${id}`);
};

export const markAllAsRead = async (): Promise<void> => {
    await client.put('/notifications/read-all');
};

export const markAllAsUnread = async (): Promise<void> => {
    await client.put('/notifications/unread-all');
};

export const deleteAllNotifications = async (): Promise<void> => {
    await client.delete('/notifications/all');
};

export const getUnreadCount = async (): Promise<number> => {
    const response = await client.get('/notifications/unread-count');
    return response.data;
};
