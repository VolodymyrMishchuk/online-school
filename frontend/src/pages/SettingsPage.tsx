import { Settings as SettingsIcon, User, Mail, Lock } from 'lucide-react';
import { useState } from 'react';

export default function SettingsPage() {
    const userStr = localStorage.getItem('user');
    const user = userStr ? JSON.parse(userStr) : null;
    const [email] = useState(user?.email || '');

    return (
        <div className="container mx-auto px-6 py-12">
            <div className="flex items-center gap-3 mb-8">
                <SettingsIcon className="w-8 h-8 text-brand-primary" />
                <h1 className="text-3xl font-bold text-brand-dark">Налаштування</h1>
            </div>

            <div className="max-w-2xl">
                <div className="bg-white rounded-3xl shadow-sm border border-gray-100 p-8">
                    <h2 className="text-xl font-bold text-brand-dark mb-6">Профіль користувача</h2>

                    <div className="space-y-6">
                        <div>
                            <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2">
                                <User className="w-4 h-4" />
                                Ім'я
                            </label>
                            <input
                                type="text"
                                placeholder="Ваше ім'я"
                                className="w-full px-4 py-3 rounded-xl border border-gray-200 focus:border-brand-primary focus:ring-2 focus:ring-brand-light outline-none transition-all"
                                disabled
                            />
                        </div>

                        <div>
                            <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2">
                                <Mail className="w-4 h-4" />
                                Email
                            </label>
                            <input
                                type="email"
                                value={email}
                                className="w-full px-4 py-3 rounded-xl border border-gray-200 bg-gray-50 outline-none"
                                disabled
                            />
                        </div>

                        <div>
                            <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2">
                                <Lock className="w-4 h-4" />
                                Пароль
                            </label>
                            <button className="px-4 py-2 text-sm rounded-xl bg-gray-50 text-brand-primary font-medium hover:bg-brand-light transition-colors">
                                Змінити пароль
                            </button>
                        </div>
                    </div>

                    <div className="mt-8 pt-6 border-t border-gray-100">
                        <p className="text-sm text-gray-500">Функціонал редагування профілю в розробці</p>
                    </div>
                </div>
            </div>
        </div>
    );
}
