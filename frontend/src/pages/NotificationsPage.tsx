import { useEffect, useState } from 'react';
import { getNotifications, markAsRead, deleteNotification } from '../api/notifications';
import type { NotificationDto } from '../api/notifications';
import { Bell, CheckCircle, Info, ShoppingCart, UserPlus, Megaphone, Download, ExternalLink, Plus, Trash2 } from 'lucide-react';
import { format } from 'date-fns';
import { uk } from 'date-fns/locale';
import { CreateNotificationModal } from '../components/CreateNotificationModal';

export const NotificationsPage: React.FC = () => {
    const [notifications, setNotifications] = useState<NotificationDto[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);

    const userStr = localStorage.getItem('user');
    const user = userStr ? JSON.parse(userStr) : null;
    const isAdmin = user?.role === 'ADMIN';

    useEffect(() => {
        loadNotifications();
    }, []);

    const loadNotifications = async () => {
        try {
            const data = await getNotifications();
            // Sort by date desc
            const sorted = data.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
            setNotifications(sorted);
        } catch (error) {
            console.error('Failed to load notifications', error);
        } finally {
            setIsLoading(false);
        }
    };

    const handleMarkAsRead = async (id: string, e: React.MouseEvent) => {
        e.stopPropagation();
        try {
            await markAsRead(id);
            setNotifications(prev => prev.map(n => n.id === id ? { ...n, read: true } : n));
        } catch (error) {
            console.error('Failed to mark as read', error);
        }
    };

    const handleDelete = async (id: string, e: React.MouseEvent) => {
        e.stopPropagation();
        // Removed confirmation as requested

        try {
            await deleteNotification(id);
            setNotifications(prev => prev.filter(n => n.id !== id));
        } catch (error) {
            console.error('Failed to delete notification', error);
        }
    };

    const handleNotificationClick = (notification: NotificationDto) => {
        if (!notification.read) {
            handleMarkAsRead(notification.id, { stopPropagation: () => { } } as React.MouseEvent);
        }
    };

    const getIcon = (type: string) => {
        switch (type) {
            case 'COURSE_ACCESS_EXTENDED': return <CheckCircle className="w-8 h-8 text-green-400" />;
            case 'COURSE_PURCHASED': return <ShoppingCart className="w-8 h-8 text-brand-primary" />;
            case 'NEW_USER_REGISTRATION': return <UserPlus className="w-8 h-8 text-blue-400" />;
            case 'ADMIN_ANNOUNCEMENT': return <Megaphone className="w-8 h-8 text-orange-400" />;
            case 'SYSTEM': return <Info className="w-8 h-8 text-gray-400" />;
            case 'GENERIC': return <Info className="w-8 h-8 text-brand-secondary" />;
            default: return <Info className="w-8 h-8 text-brand-secondary" />;
        }
    };

    const getBackground = (type: string, read: boolean) => {
        if (read) return 'bg-white border-gray-100 opacity-70';

        // Unread styles - Vova Standard (Clean, light backgrounds with colored borders/accents)
        switch (type) {
            case 'COURSE_ACCESS_EXTENDED': return 'bg-green-50 border-green-200 shadow-sm';
            case 'COURSE_PURCHASED': return 'bg-brand-light/20 border-brand-primary/20 shadow-sm';
            case 'NEW_USER_REGISTRATION': return 'bg-blue-50 border-blue-200 shadow-sm';
            case 'ADMIN_ANNOUNCEMENT': return 'bg-orange-50 border-orange-200 shadow-sm';
            case 'SYSTEM': return 'bg-gray-50 border-gray-200 shadow-sm';
            default: return 'bg-white border-brand-primary/10 shadow-md shadow-brand-primary/5';
        }
    };

    if (isLoading) {
        return (
            <div className="flex items-center justify-center min-h-[400px]">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-brand-primary"></div>
            </div>
        );
    }

    return (
        <div className="container mx-auto px-6 py-12">
            <div className="flex items-center justify-between mb-8">
                <div>
                    <h1 className="text-3xl font-bold text-gray-900 mb-2">Сповіщення</h1>
                </div>
                <div className="flex gap-3">
                    {isAdmin && (
                        <button
                            onClick={() => setIsCreateModalOpen(true)}
                            className="flex items-center gap-2 px-4 py-2 text-gray-900 font-medium hover:bg-gray-100 rounded-lg transition-colors"
                        >
                            <Plus className="w-5 h-5" />
                            <span>Надіслати сповіщення</span>
                        </button>
                    )}
                </div>
            </div>

            <div className="space-y-2">
                {notifications.length === 0 ? (
                    <div className="text-center py-20 bg-gray-50 rounded-lg border border-dashed border-gray-200">
                        <div className="w-16 h-16 bg-white rounded-full flex items-center justify-center mx-auto mb-4 shadow-sm">
                            <Bell className="w-8 h-8 text-gray-300" />
                        </div>
                        <h3 className="text-xl font-medium text-gray-900 mb-2">Немає нових сповіщень</h3>
                        <p className="text-gray-400">Тут будуть показані всі важливі події</p>
                    </div>
                ) : (
                    notifications.map(notification => (
                        <div
                            key={notification.id}
                            onClick={() => handleNotificationClick(notification)}
                            className={`
                                relative p-5 rounded-lg border transition-all duration-300 cursor-pointer group overflow-hidden
                                ${getBackground(notification.type, notification.read)}
                                hover:shadow-lg
                            `}
                        >
                            <div className="flex gap-5 relative z-10 transition-all duration-300 group-hover:pr-[90px]">
                                <div className="shrink-0 mt-1">
                                    <div className={`w-14 h-14 rounded-lg flex items-center justify-center bg-white shadow-sm transition-opacity duration-300 ${notification.read ? 'opacity-50' : ''}`}>
                                        {getIcon(notification.type)}
                                    </div>
                                </div>
                                <div className="flex-1 min-w-0">
                                    <div className="flex items-start justify-between gap-4">
                                        <div className="space-y-1">
                                            <h4 className={`text-lg font-bold transition-colors duration-300 ${notification.read ? 'text-gray-600' : 'text-gray-900'}`}>
                                                {notification.title || 'Сповіщення'}
                                            </h4>
                                            <p className={`text-base transition-colors duration-300 ${notification.read ? 'text-gray-500' : 'text-gray-700'} leading-relaxed`}>
                                                {notification.message}
                                            </p>
                                        </div>

                                        <div className="flex items-center gap-2 shrink-0">
                                            {!notification.read && (
                                                <button
                                                    onClick={(e) => handleMarkAsRead(notification.id, e)}
                                                    className="p-2 hover:bg-black/5 rounded-full transition-colors group/btn"
                                                    title="Позначити, як прочитане"
                                                >
                                                    <div className="w-2.5 h-2.5 rounded-full bg-brand-primary group-hover/btn:bg-brand-secondary transition-colors" />
                                                </button>
                                            )}
                                        </div>
                                    </div>

                                    {notification.buttonUrl && (
                                        <div className="mt-4">
                                            <a
                                                href={notification.buttonUrl}
                                                target="_blank"
                                                rel="noopener noreferrer"
                                                onClick={(e) => e.stopPropagation()}
                                                className="inline-flex items-center gap-2 px-4 py-2 bg-white border border-gray-200 rounded-lg text-sm font-medium text-gray-700 hover:bg-gray-50 hover:border-gray-300 transition-all shadow-sm hover:shadow"
                                            >
                                                {notification.type === 'COURSE_ACCESS_EXTENDED' || notification.type === 'SYSTEM' ? (
                                                    <>
                                                        <Download className="w-4 h-4 text-brand-primary" />
                                                        Завантажити відео
                                                    </>
                                                ) : (
                                                    <>
                                                        <ExternalLink className="w-4 h-4 text-brand-primary" />
                                                        Перейти
                                                    </>
                                                )}
                                            </a>
                                        </div>
                                    )}

                                    <div className="mt-3 flex items-center gap-2 text-sm text-gray-400">
                                        <span>
                                            {format(new Date(notification.createdAt), "d MMMM yyyy HH:mm", { locale: uk })}
                                        </span>
                                        {notification.read && (
                                            <span className="flex items-center gap-1 text-gray-300">
                                                • Прочитано
                                            </span>
                                        )}
                                    </div>
                                </div>
                            </div>

                            {/* Hover Delete Action */}
                            <div
                                onClick={(e) => handleDelete(notification.id, e)}
                                className="absolute right-0 top-0 bottom-0 w-[100px] bg-red-50/90 backdrop-blur-[2px] border-l border-red-100 flex items-center justify-center translate-x-full group-hover:translate-x-0 transition-transform duration-300 z-20 hover:bg-red-500 group/delete"
                            >
                                <Trash2 className="w-6 h-6 text-red-500 transition-colors group-hover/delete:text-white" />
                            </div>
                        </div>
                    ))
                )}
            </div>

            <CreateNotificationModal
                isOpen={isCreateModalOpen}
                onClose={() => setIsCreateModalOpen(false)}
                onSuccess={() => {
                    loadNotifications();
                }}
            />
        </div>
    );
};
