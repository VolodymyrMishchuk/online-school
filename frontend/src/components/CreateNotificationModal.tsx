
import { useState, useEffect } from 'react';
import { X, Users, Search, Globe, Send, Loader2 } from 'lucide-react';
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
            onClose();
            // Reset form
            setTitle('');
            setMessage('');
            setButtonUrl('');
            setTargetType('ALL');
            setSelectedUsers([]);
        } catch (error) {
            console.error('Failed to send notification', error);
        } finally {
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
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm animate-in fade-in duration-200">
            <div className="bg-white rounded-3xl w-full max-w-2xl max-h-[90vh] overflow-hidden shadow-2xl flex flex-col">
                {/* Header */}
                <div className="p-6 border-b border-gray-100 flex items-center justify-between bg-gray-50/50">
                    <div>
                        <h2 className="text-xl font-bold text-gray-900">Створити сповіщення</h2>
                        <p className="text-sm text-gray-500">Надіслати повідомлення користувачам</p>
                    </div>
                    <button onClick={onClose} className="p-2 hover:bg-gray-200/50 rounded-full transition-colors">
                        <X className="w-5 h-5 text-gray-500" />
                    </button>
                </div>

                {/* Body */}
                <div className="flex-1 overflow-y-auto p-6 space-y-6">
                    <div className="space-y-4">
                        {/* Target Selection */}
                        <div className="flex gap-4 p-1 bg-gray-100/80 rounded-xl w-fit">
                            <button
                                type="button"
                                onClick={() => setTargetType('ALL')}
                                className={`flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-all ${targetType === 'ALL'
                                    ? 'bg-white text-gray-900 shadow-sm'
                                    : 'text-gray-500 hover:text-gray-700'
                                    }`}
                            >
                                <Globe className="w-4 h-4" />
                                Всім користувачам
                            </button>
                            <button
                                type="button"
                                onClick={() => setTargetType('SPECIFIC')}
                                className={`flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-all ${targetType === 'SPECIFIC'
                                    ? 'bg-white text-gray-900 shadow-sm'
                                    : 'text-gray-500 hover:text-gray-700'
                                    }`}
                            >
                                <Users className="w-4 h-4" />
                                Вибрати зі списку
                            </button>
                        </div>

                        {/* User Selection UI */}
                        {targetType === 'SPECIFIC' && (
                            <div className="bg-gray-50 rounded-xl p-4 border border-gray-200 space-y-3">
                                <div className="relative">
                                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                                    <input
                                        type="text"
                                        placeholder="Пошук користувачів..."
                                        value={searchQuery}
                                        onChange={(e) => setSearchQuery(e.target.value)}
                                        className="w-full pl-9 pr-4 py-2 bg-white border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-brand-primary/20 focus:border-brand-primary"
                                    />
                                </div>

                                <div className="max-h-48 overflow-y-auto space-y-1 pr-2 custom-scrollbar">
                                    {isLoadingUsers ? (
                                        <div className="text-center py-4 text-gray-500 text-sm">Завантаження...</div>
                                    ) : filteredUsers.length === 0 ? (
                                        <div className="text-center py-4 text-gray-500 text-sm">Користувачів не знайдено</div>
                                    ) : (
                                        filteredUsers.map(user => (
                                            <label
                                                key={user.id}
                                                className="flex items-center justify-between p-2 hover:bg-white rounded-lg cursor-pointer transition-colors group"
                                            >
                                                <div className="flex items-center gap-3">
                                                    <input
                                                        type="checkbox"
                                                        checked={selectedUsers.includes(user.id)}
                                                        onChange={() => toggleUser(user.id)}
                                                        className="w-4 h-4 rounded border-gray-300 text-brand-primary focus:ring-brand-primary"
                                                    />
                                                    <div>
                                                        <div className="text-sm font-medium text-gray-900">
                                                            {user.firstName} {user.lastName}
                                                        </div>
                                                        <div className="text-xs text-gray-500">{user.email}</div>
                                                    </div>
                                                </div>
                                                <div className={`px-2 py-0.5 rounded text-xs font-medium ${user.role === 'ADMIN' ? 'bg-purple-100 text-purple-700' : 'bg-gray-100 text-gray-600'
                                                    }`}>
                                                    {user.role}
                                                </div>
                                            </label>
                                        ))
                                    )}
                                </div>
                                <div className="text-xs text-gray-500 text-right">
                                    Вибрано: {selectedUsers.length}
                                </div>
                            </div>
                        )}

                        <div className="space-y-4">
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Заголовок</label>
                                <input
                                    type="text"
                                    value={title}
                                    onChange={(e) => setTitle(e.target.value)}
                                    placeholder="Введіть заголовок сповіщення"
                                    className="w-full px-4 py-2 bg-gray-50 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-brand-primary/20 focus:border-brand-primary transition-all"
                                />
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Текст повідомлення</label>
                                <textarea
                                    value={message}
                                    onChange={(e) => setMessage(e.target.value)}
                                    rows={4}
                                    placeholder="Введіть текст сповіщення..."
                                    className="w-full px-4 py-2 bg-gray-50 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-brand-primary/20 focus:border-brand-primary transition-all resize-none"
                                />
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Посилання (опційно)</label>
                                <input
                                    type="text"
                                    value={buttonUrl}
                                    onChange={(e) => setButtonUrl(e.target.value)}
                                    placeholder="https://..."
                                    className="w-full px-4 py-2 bg-gray-50 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-brand-primary/20 focus:border-brand-primary transition-all"
                                />
                            </div>
                        </div>
                    </div>
                </div>

                {/* Footer */}
                <div className="p-6 border-t border-gray-100 bg-gray-50/50 flex justify-end gap-3">
                    <button
                        onClick={onClose}
                        className="px-6 py-2.5 rounded-xl text-gray-600 font-medium hover:bg-white hover:shadow-sm border border-transparent hover:border-gray-200 transition-all"
                    >
                        Скасувати
                    </button>
                    <button
                        onClick={handleSubmit}
                        disabled={isSubmitting || !title || !message || (targetType === 'SPECIFIC' && selectedUsers.length === 0)}
                        className="flex items-center gap-2 px-6 py-2.5 bg-brand-primary text-white rounded-xl font-medium hover:bg-brand-secondary disabled:opacity-50 disabled:cursor-not-allowed transition-all shadow-lg shadow-brand-primary/30"
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
