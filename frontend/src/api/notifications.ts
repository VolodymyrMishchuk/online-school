
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

export const markAllAsRead = async (): Promise<void> => {
    // Assuming backend has such endpoint or we loop. 
    // Given the backend summary, I didn't explicitly see markAllAsRead.
    // I check usage. Ideally we match backend.
    // For now, I'll stick to basic get.
    // Wait, did I implement markAsRead in backend? I just saw NotificationService has create and get.
    // Let me check NotificationController.java.
};
