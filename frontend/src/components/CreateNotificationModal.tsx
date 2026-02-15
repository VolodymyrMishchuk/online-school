import { useState, useEffect } from 'react';
import { X, Users, Search, Globe, Send, Loader2, Megaphone, Link as LinkIcon, Check, Bell } from 'lucide-react';
import { getAllPersons } from '../api/persons';
import type { PersonDto } from '../api/persons';
import { broadcastNotification, sendTargetedNotification } from '../api/notifications';

interface CreateNotificationModalProps {
    isOpen: boolean;
    onClose: () => void;
    onSuccess: () => void;
}

type TargetType = 'ALL' | 'SPECIFIC';

export const CreateNotificationModal: React.FC<CreateNotificationModalProps> = ({ isOpen, onClose, onSuccess }) => {
    const [title, setTitle] = useState('');
    const [message, setMessage] = useState('');
    const [buttonUrl, setButtonUrl] = useState('');
    const [targetType, setTargetType] = useState<TargetType>('ALL');
    const [selectedUsers, setSelectedUsers] = useState<string[]>([]);
    const [users, setUsers] = useState<PersonDto[]>([]);
    const [searchQuery, setSearchQuery] = useState('');
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [isLoadingUsers, setIsLoadingUsers] = useState(false);

    useEffect(() => {
        if (isOpen && targetType === 'SPECIFIC' && users.length === 0) {
            loadUsers();
        }
    }, [isOpen, targetType]);

    const loadUsers = async () => {
        setIsLoadingUsers(true);
        try {
            const data = await getAllPersons();
            setUsers(data);
        } catch (error) {
            console.error('Failed to load users', error);
        } finally {
            setIsLoadingUsers(false);
        }
    };

    const resetState = () => {
        setTitle('');
        setMessage('');
        setButtonUrl('');
        setTargetType('ALL');
        setSelectedUsers([]);
        setIsSubmitting(false);
    };

    const handleClose = () => {
        onClose();
        setTimeout(resetState, 300);
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!title.trim() || !message.trim()) return;
        if (targetType === 'SPECIFIC' && selectedUsers.length === 0) return;

        setIsSubmitting(true);
        try {
            if (targetType === 'ALL') {
                await broadcastNotification({ title, message, buttonUrl: buttonUrl || undefined });
            } else {
                await sendTargetedNotification({ title, message, userIds: selectedUsers, buttonUrl: buttonUrl || undefined });
            }
            onSuccess();
            handleClose();
        } catch (error) {
            console.error('Failed to send notification', error);
            setIsSubmitting(false);
        }
    };

    const toggleUser = (userId: string) => {
        setSelectedUsers(prev =>
            prev.includes(userId)
                ? prev.filter(id => id !== userId)
                : [...prev, userId]
        );
    };

    const filteredUsers = users.filter(user =>
        (user.firstName?.toLowerCase().includes(searchQuery.toLowerCase()) || '') ||
        (user.lastName?.toLowerCase().includes(searchQuery.toLowerCase()) || '') ||
        user.email.toLowerCase().includes(searchQuery.toLowerCase())
    );

    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-white/30 backdrop-blur-md animate-in fade-in duration-200">
            {/* Glass Panel Modal */}
            <div
                className="glass-panel w-full max-w-2xl flex flex-col overflow-hidden animate-in zoom-in-95 duration-200 relative rounded-lg shadow-xl"
                style={{ background: 'rgba(255, 255, 255, 0.9)', maxHeight: '90vh' }}
            >
                {/* Header Bar - Static, Lighter (bg-white) */}
                <div className="flex items-center justify-between px-6 py-5 border-b border-gray-100 bg-white shrink-0 z-10 relative shadow-sm">
                    <div className="flex items-center gap-3">
                        <div className="flex items-center justify-center w-10 h-10 rounded-full bg-brand-light/50 text-brand-primary ring-2 ring-white shadow-sm">
                            <Megaphone className="w-5 h-5" />
                        </div>
                        <div>
                            <h2 className="text-xl font-bold text-brand-dark">Створити сповіщення</h2>
                            <p className="text-xs text-gray-500 font-medium">Для користувачів</p>
                        </div>
                    </div>
                    <button
                        onClick={handleClose}
                        className="p-2 text-gray-400 hover:text-brand-primary hover:bg-gray-100 rounded-lg transition-all"
                    >
                        <X className="w-5 h-5" />
                    </button>
                </div>

                {/* Body - Scrollable */}
                <div className="flex-1 overflow-y-auto px-8 py-6 custom-scrollbar flex flex-col gap-6">
                    {/* Target Selection */}
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2 ml-1">Отримувачі</label>
                        <div className="flex gap-2 p-1 bg-white/50 border border-gray-100 rounded-lg w-fit">
                            <button
                                type="button"
                                onClick={() => setTargetType('ALL')}
                                className={`flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-bold transition-all ${targetType === 'ALL'
                                    ? 'bg-brand-primary text-white shadow-md'
                                    : 'text-gray-500 hover:bg-white/80 hover:text-brand-primary'
                                    }`}
                            >
                                <Globe className="w-4 h-4" />
                                Всім користувачам
                            </button>
                            <button
                                type="button"
                                onClick={() => setTargetType('SPECIFIC')}
                                className={`flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-bold transition-all ${targetType === 'SPECIFIC'
                                    ? 'bg-brand-primary text-white shadow-md'
                                    : 'text-gray-500 hover:bg-white/80 hover:text-brand-primary'
                                    }`}
                            >
                                <Users className="w-4 h-4" />
                                Вибрати зі списку
                            </button>
                        </div>
                    </div>

                    {/* User Selection UI */}
                    {targetType === 'SPECIFIC' && (
                        <div className="bg-white/40 rounded-lg p-4 border border-gray-200 space-y-3 animate-in fade-in slide-in-from-top-2 duration-300">
                            <div className="relative">
                                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                                <input
                                    type="text"
                                    placeholder="Пошук користувачів..."
                                    value={searchQuery}
                                    onChange={(e) => setSearchQuery(e.target.value)}
                                    className="w-full pl-9 pr-4 py-2.5 bg-white/70 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-brand-light focus:border-brand-primary transition-all"
                                />
                            </div>

                            <div className="max-h-48 overflow-y-auto space-y-1 pr-2 custom-scrollbar">
                                {isLoadingUsers ? (
                                    <div className="text-center py-6 text-gray-500 text-sm flex items-center justify-center gap-2">
                                        <Loader2 className="w-4 h-4 animate-spin" />
                                        Завантаження...
                                    </div>
                                ) : filteredUsers.length === 0 ? (
                                    <div className="text-center py-6 text-gray-500 text-sm">Користувачів не знайдено</div>
                                ) : (
                                    filteredUsers.map(user => (
                                        <label
                                            key={user.id}
                                            className="flex items-center justify-between p-2.5 hover:bg-white rounded-lg cursor-pointer transition-colors group border border-transparent hover:border-gray-100"
                                        >
                                            <div className="flex items-center gap-3">
                                                <div className="relative flex items-center">
                                                    <input
                                                        type="checkbox"
                                                        checked={selectedUsers.includes(user.id)}
                                                        onChange={() => toggleUser(user.id)}
                                                        className="peer h-4 w-4 cursor-pointer appearance-none rounded border border-gray-300 transition-all checked:border-brand-primary checked:bg-brand-primary group-hover:border-brand-primary"
                                                    />
                                                    <Check className="absolute pointer-events-none opacity-0 peer-checked:opacity-100 text-white w-3 h-3 left-[2px] top-[2px]" />
                                                </div>
                                                <div>
                                                    <div className="text-sm font-bold text-gray-700 group-hover:text-brand-dark transition-colors">
                                                        {user.firstName} {user.lastName}
                                                    </div>
                                                    <div className="text-xs text-gray-500">{user.email}</div>
                                                </div>
                                            </div>
                                            <div className={`px-2 py-0.5 rounded text-[10px] font-bold uppercase tracking-wider ${user.role === 'ADMIN'
                                                ? 'bg-purple-100 text-purple-700 border border-purple-200'
                                                : 'bg-gray-100 text-gray-500 border border-gray-200'
                                                }`}>
                                                {user.role}
                                            </div>
                                        </label>
                                    ))
                                )}
                            </div>
                            <div className="text-xs font-medium text-gray-500 text-right pt-2 border-t border-gray-200/50">
                                Вибрано: <span className="text-brand-primary font-bold">{selectedUsers.length}</span>
                            </div>
                        </div>
                    )}

                    <form id="notification-form" onSubmit={handleSubmit} className="space-y-5">
                        {/* Title */}
                        <div>
                            <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2 ml-1">
                                <Megaphone className="w-4 h-4" />
                                Заголовок
                            </label>
                            <input
                                type="text"
                                value={title}
                                onChange={(e) => setTitle(e.target.value)}
                                placeholder="Введіть заголовок сповіщення"
                                className="w-full px-4 py-3 rounded-lg border border-gray-200 bg-white/50 outline-none transition-all focus:border-brand-primary focus:ring-2 focus:ring-brand-light focus:bg-white"
                            />
                        </div>

                        {/* Message */}
                        <div>
                            <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2 ml-1">
                                <Bell className="w-4 h-4" />
                                Текст повідомлення
                            </label>
                            <textarea
                                value={message}
                                onChange={(e) => setMessage(e.target.value)}
                                rows={4}
                                placeholder="Введіть текст сповіщення..."
                                className="w-full px-4 py-3 rounded-lg border border-gray-200 bg-white/50 outline-none transition-all focus:border-brand-primary focus:ring-2 focus:ring-brand-light focus:bg-white resize-none"
                            />
                        </div>

                        {/* Button URL */}
                        <div>
                            <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2 ml-1">
                                <LinkIcon className="w-4 h-4" />
                                Посилання (опційно)
                            </label>
                            <input
                                type="text"
                                value={buttonUrl}
                                onChange={(e) => setButtonUrl(e.target.value)}
                                placeholder="https://..."
                                className="w-full px-4 py-3 rounded-lg border border-gray-200 bg-white/50 outline-none transition-all focus:border-brand-primary focus:ring-2 focus:ring-brand-light focus:bg-white"
                            />
                        </div>
                    </form>
                </div>

                {/* Footer - Static */}
                <div className="flex gap-4 p-6 border-t border-gray-100 bg-white/50 backdrop-blur-sm shrink-0">
                    <button
                        type="button"
                        onClick={handleClose}
                        className="flex-1 py-3 font-bold text-brand-primary bg-white hover:bg-brand-primary hover:text-white rounded-lg transition-colors shadow-sm border border-gray-100"
                    >
                        Скасувати
                    </button>
                    <button
                        type="submit"
                        form="notification-form"
                        disabled={isSubmitting || !title || !message || (targetType === 'SPECIFIC' && selectedUsers.length === 0)}
                        className="flex-1 py-3 font-bold text-white bg-brand-primary hover:bg-brand-secondary rounded-lg transition-all shadow-lg hover:shadow-xl transform active:scale-95 duration-200 disabled:opacity-70 disabled:transform-none flex items-center justify-center gap-2"
                    >
                        {isSubmitting ? (
                            <>
                                <Loader2 className="w-4 h-4 animate-spin" />
                                Відправка...
                            </>
                        ) : (
                            <>
                                <Send className="w-4 h-4" />
                                Надіслати
                            </>
                        )}
                    </button>
                </div>
            </div>
        </div>
    );
};
