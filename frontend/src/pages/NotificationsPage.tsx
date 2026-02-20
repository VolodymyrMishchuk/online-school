import { useEffect, useState } from 'react';
import { createPortal } from 'react-dom';
import { useOutletContext } from 'react-router-dom';
import apiClient from '../api/client';
import { getNotifications, markAsRead, markAsUnread, deleteNotification, markAllAsRead, markAllAsUnread, deleteAllNotifications } from '../api/notifications';
import type { NotificationDto } from '../api/notifications';
import { Bell, CheckCircle, Info, ShoppingCart, UserPlus, Megaphone, Download, ExternalLink, Plus, Trash2, Search } from 'lucide-react';
import { format } from 'date-fns';
import { uk } from 'date-fns/locale';
import { CreateNotificationModal } from '../components/CreateNotificationModal';

export const NotificationsPage: React.FC = () => {
    const [notifications, setNotifications] = useState<NotificationDto[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
    const [searchQuery, setSearchQuery] = useState('');
    const [isDeleteAllModalOpen, setIsDeleteAllModalOpen] = useState(false);

    // Get the refresh function from DashboardLayout context
    const { refreshUnreadCount } = useOutletContext<{ refreshUnreadCount: () => void }>() || { refreshUnreadCount: () => { } };

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
            setNotifications(prev => prev.map(n => n.id === id ? { ...n, read: true, viewAgain: false } as any : n));
            refreshUnreadCount();
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
            refreshUnreadCount();
        } catch (error) {
            console.error('Failed to delete notification', error);
        }
    };

    const handleDownloadClick = async (e: React.MouseEvent, url: string) => {
        e.preventDefault();
        e.stopPropagation();

        try {
            const response = await apiClient.get(url, { responseType: 'blob' });

            const blob = new Blob([response.data]);
            const downloadUrl = window.URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = downloadUrl;

            const contentDisposition = response.headers['content-disposition'];
            let filename = 'video-review.mp4';
            if (contentDisposition) {
                const filenameMatch = contentDisposition.match(/filename="?([^"]+)"?/);
                if (filenameMatch && filenameMatch.length === 2) {
                    filename = filenameMatch[1];
                }
            }

            link.download = filename;
            document.body.appendChild(link);
            link.click();
            link.remove();
            window.URL.revokeObjectURL(downloadUrl);
        } catch (error) {
            console.error('Download failed', error);
            alert('Помилка при завантаженні відео. Спробуйте пізніше.');
        }
    };

    const handleNotificationClick = (notification: NotificationDto) => {
        if (!notification.read) {
            handleMarkAsRead(notification.id, { stopPropagation: () => { } } as React.MouseEvent);
        } else {
            handleMarkAsUnread(notification.id, { stopPropagation: () => { } } as React.MouseEvent);
        }
    };

    const handleMarkAsUnread = async (id: string, e: React.MouseEvent) => {
        e.stopPropagation();
        try {
            await markAsUnread(id);
            setNotifications(prev => prev.map(n => n.id === id ? { ...n, read: false, viewAgain: true } as any : n));
            refreshUnreadCount();
        } catch (error) {
            console.error('Failed to mark as unread', error);
        }
    };

    const handleMarkAllAsUnread = async () => {
        try {
            await markAllAsUnread();
            setNotifications(prev => prev.map(n => ({ ...n, read: false, viewAgain: n.read ? true : (n as any).viewAgain } as any)));
            refreshUnreadCount();
        } catch (error) {
            console.error('Failed to mark all as unread', error);
        }
    };

    const handleMarkAllAsRead = async () => {
        try {
            await markAllAsRead();
            setNotifications(prev => prev.map(n => ({ ...n, read: true, viewAgain: false } as any)));
            refreshUnreadCount();
        } catch (error) {
            console.error('Failed to mark all as read', error);
        }
    };

    const handleDeleteAll = async () => {
        try {
            await deleteAllNotifications();
            setNotifications([]);
            setIsDeleteAllModalOpen(false);
            refreshUnreadCount();
        } catch (error) {
            console.error('Failed to delete all notifications', error);
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

            <div className="flex flex-wrap gap-4 mb-8 bg-white p-3 rounded-lg border border-gray-100 shadow-sm items-center">
                <div className="relative min-w-[250px] flex-1">
                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                    <input
                        type="text"
                        placeholder="Пошук..."
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                        className="w-full pl-9 pr-4 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-brand-light focus:border-brand-primary transition-all shadow-sm"
                    />
                </div>

                <div className="flex flex-wrap items-center gap-2 w-full md:w-auto ml-auto">
                    <button
                        onClick={handleMarkAllAsUnread}
                        className="flex-1 md:flex-none px-4 py-2 text-sm font-bold text-brand-primary bg-brand-light/20 border border-brand-primary/20 rounded-lg hover:bg-brand-primary hover:text-white transition-colors whitespace-nowrap"
                    >
                        Вибрати все
                    </button>
                    <button
                        onClick={handleMarkAllAsRead}
                        className="flex-1 md:flex-none px-4 py-2 text-sm font-medium text-gray-600 bg-white border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors whitespace-nowrap"
                    >
                        Прочитати все
                    </button>
                    <button
                        onClick={() => setIsDeleteAllModalOpen(true)}
                        className="flex-1 md:flex-none px-4 py-2 text-sm font-medium text-red-500 bg-red-50 border border-red-100 rounded-lg hover:bg-red-500 hover:text-white transition-colors whitespace-nowrap"
                    >
                        Видалити все
                    </button>
                </div>
            </div>

            <div className="space-y-2">
                {notifications.filter(n =>
                    n.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
                    n.message.toLowerCase().includes(searchQuery.toLowerCase())
                ).length === 0 ? (
                    <div className="text-center py-20 bg-gray-50 rounded-lg border border-dashed border-gray-200">
                        <div className="w-16 h-16 bg-white rounded-full flex items-center justify-center mx-auto mb-4 shadow-sm">
                            <Bell className="w-8 h-8 text-gray-300" />
                        </div>
                        <h3 className="text-xl font-medium text-gray-900 mb-2">Немає нових сповіщень</h3>
                        <p className="text-gray-400">Тут будуть показані всі важливі події</p>
                    </div>
                ) : (
                    notifications.filter(n =>
                        n.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
                        n.message.toLowerCase().includes(searchQuery.toLowerCase())
                    ).map(notification => (
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
                                            {notification.type === 'COURSE_ACCESS_EXTENDED' || notification.type === 'SYSTEM' ? (
                                                <button
                                                    onClick={(e) => handleDownloadClick(e, notification.buttonUrl!)}
                                                    className="inline-flex items-center gap-2 px-4 py-2 bg-white border border-gray-200 rounded-lg text-sm font-medium text-gray-700 hover:bg-gray-50 hover:border-gray-300 transition-all shadow-sm hover:shadow"
                                                >
                                                    <Download className="w-4 h-4 text-brand-primary" />
                                                    Завантажити відео
                                                </button>
                                            ) : (
                                                <a
                                                    href={notification.buttonUrl}
                                                    target="_blank"
                                                    rel="noopener noreferrer"
                                                    onClick={(e) => e.stopPropagation()}
                                                    className="inline-flex items-center gap-2 px-4 py-2 bg-white border border-gray-200 rounded-lg text-sm font-medium text-gray-700 hover:bg-gray-50 hover:border-gray-300 transition-all shadow-sm hover:shadow"
                                                >
                                                    <ExternalLink className="w-4 h-4 text-brand-primary" />
                                                    Перейти
                                                </a>
                                            )}
                                        </div>
                                    )}

                                    <div className="mt-3 flex items-center gap-2 text-sm text-gray-400">
                                        <span>
                                            {format(new Date(notification.createdAt), "d MMMM yyyy HH:mm", { locale: uk })}
                                        </span>
                                        {(notification as any).viewAgain ? (
                                            <span className="flex items-center gap-1 text-brand-primary font-medium">
                                                • Переглянути знову
                                            </span>
                                        ) : notification.read ? (
                                            <span className="flex items-center gap-1 text-gray-300">
                                                • Прочитано
                                            </span>
                                        ) : null}
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

            {/* Modal for deleting all notifications */}
            {isDeleteAllModalOpen && createPortal(
                <div
                    className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/20 backdrop-blur-sm animate-in fade-in duration-200"
                    onClick={(e) => { e.stopPropagation(); setIsDeleteAllModalOpen(false); }}
                >
                    <div
                        className="bg-white/90 backdrop-blur-md rounded-2xl shadow-xl w-full max-w-sm overflow-hidden animate-in zoom-in-95 duration-200 border border-gray-100"
                        onClick={(e) => e.stopPropagation()}
                    >
                        <div className="p-6">
                            <div className="flex items-center gap-3 mb-2">
                                <div className="p-2 bg-red-100 rounded-lg">
                                    <Trash2 className="w-6 h-6 text-red-600" />
                                </div>
                                <h3 className="text-xl font-bold text-gray-900">Видалення</h3>
                            </div>
                            <p className="text-gray-600 mt-2 mb-6 text-sm">
                                Ви впевнені, що хочете видалити <strong>ВСІ</strong> сповіщення? Цю дію неможливо скасувати.
                            </p>
                            <div className="flex gap-3">
                                <button
                                    onClick={() => setIsDeleteAllModalOpen(false)}
                                    className="flex-1 px-4 py-2.5 rounded-xl font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 transition-colors"
                                >
                                    Скасувати
                                </button>
                                <button
                                    onClick={handleDeleteAll}
                                    className="flex-1 px-4 py-2.5 rounded-xl font-bold text-white bg-red-500 hover:bg-red-600 shadow-md shadow-red-500/20 transition-all active:scale-95"
                                >
                                    Видалити всі
                                </button>
                            </div>
                        </div>
                    </div>
                </div>,
                document.body
            )}
        </div>
    );
};
