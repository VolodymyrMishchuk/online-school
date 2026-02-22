import { User, Mail, Lock, Phone, Calendar, Save, AlertCircle, CheckCircle } from 'lucide-react';
import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import ChangePasswordModal from '../components/ChangePasswordModal';
import { getPerson, updatePerson } from '../api/persons';
import type { PersonDto } from '../api/persons';

export default function SettingsPage() {
    const userId = localStorage.getItem('userId');
    const [, setProfile] = useState<PersonDto | null>(null);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [message, setMessage] = useState<{ type: 'success' | 'error', text: string } | null>(null);
    const navigate = useNavigate();

    // Form State
    const [firstName, setFirstName] = useState('');
    const [lastName, setLastName] = useState('');
    const [email, setEmail] = useState('');
    const [phoneNumber, setPhoneNumber] = useState('');
    const [bornedAt, setBornedAt] = useState('');

    const [isChangePasswordModalOpen, setIsChangePasswordModalOpen] = useState(false);

    useEffect(() => {
        if (userId) {
            fetchProfile(userId);
        }
    }, [userId]);

    const fetchProfile = async (id: string) => {
        try {
            const data = await getPerson(id);
            setProfile(data);
            setFirstName(data.firstName || '');
            setLastName(data.lastName || '');
            setEmail(data.email || '');
            setPhoneNumber(data.phoneNumber || '');
            // Format date for input type="date" (YYYY-MM-DD)
            if (data.bornedAt) {
                const date = new Date(data.bornedAt);
                setBornedAt(date.toISOString().split('T')[0]);
            } else {
                setBornedAt('');
            }
        } catch (error) {
            console.error('Failed to fetch profile:', error);
            setMessage({ type: 'error', text: 'Failed to load profile data' });
        } finally {
            setLoading(false);
        }
    };

    const handleSave = async () => {
        if (!userId) return;
        setSaving(true);
        setMessage(null);

        try {
            await updatePerson(userId, {
                firstName,
                lastName,
                phoneNumber,
                bornedAt: bornedAt ? new Date(bornedAt).toISOString() : undefined,
                email // sending email back just in case, though usually ignored if not changed
            });
            setMessage({ type: 'success', text: 'Profile updated successfully' });

            // Update local storage user info if name changed (optional, but good for consistency)
            const userStr = localStorage.getItem('user');
            if (userStr) {
                const user = JSON.parse(userStr);
                user.firstName = firstName;
                user.lastName = lastName;
                user.email = email;
                localStorage.setItem('user', JSON.stringify(user));
            }

        } catch (error) {
            console.error('Failed to update profile:', error);
            setMessage({ type: 'error', text: 'Failed to update profile' });
        } finally {
            setSaving(false);
        }
    };

    if (loading) {
        return (
            <div className="container mx-auto px-6 py-12 flex items-center justify-center min-h-[400px]">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-brand-primary"></div>
            </div>
        );
    }

    return (
        <div className="container mx-auto px-6 py-12">
            <div className="flex items-center gap-3 mb-8">
                <h1 className="text-3xl font-bold text-brand-dark">Налаштування</h1>
            </div>

            <div className="max-w-2xl">
                <div className="glass-panel rounded-lg overflow-hidden">
                    <div className="px-8 py-6 border-b border-gray-200/50 bg-white/30 backdrop-blur-sm">
                        <h2 className="text-xl font-bold text-brand-dark">Мій профіль</h2>
                    </div>

                    <div className="p-8">
                        {message && (
                            <div className={`mb-6 p-4 rounded-lg flex items-center gap-3 ${message.type === 'success' ? 'bg-green-50 text-green-700' : 'bg-red-50 text-red-700'
                                }`}>
                                {message.type === 'success' ? (
                                    <CheckCircle className="w-5 h-5 shrink-0" />
                                ) : (
                                    <AlertCircle className="w-5 h-5 shrink-0" />
                                )}
                                <p className="font-medium">{message.text}</p>
                            </div>
                        )}

                        <div className="space-y-6">
                            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                                <div>
                                    <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2">
                                        <User className="w-4 h-4" />
                                        Ім'я
                                    </label>
                                    <input
                                        type="text"
                                        value={firstName}
                                        onChange={(e) => setFirstName(e.target.value)}
                                        placeholder="Ваше ім'я"
                                        className="w-full px-4 py-3 rounded-lg border border-gray-200 focus:border-brand-primary focus:ring-2 focus:ring-brand-light outline-none transition-all bg-white/50 focus:bg-white"
                                    />
                                </div>
                                <div>
                                    <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2">
                                        <User className="w-4 h-4" />
                                        Прізвище
                                    </label>
                                    <input
                                        type="text"
                                        value={lastName}
                                        onChange={(e) => setLastName(e.target.value)}
                                        placeholder="Ваше прізвище"
                                        className="w-full px-4 py-3 rounded-lg border border-gray-200 focus:border-brand-primary focus:ring-2 focus:ring-brand-light outline-none transition-all bg-white/50 focus:bg-white"
                                    />
                                </div>
                            </div>

                            <div>
                                <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2">
                                    <Mail className="w-4 h-4" />
                                    Email
                                </label>
                                <input
                                    type="email"
                                    value={email}
                                    disabled
                                    className="w-full px-4 py-3 rounded-lg border border-gray-200 bg-gray-100/50 text-gray-500 cursor-not-allowed outline-none"
                                />
                                <p className="mt-1 text-xs text-gray-400">Email cannot be changed directly.</p>
                            </div>

                            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                                <div>
                                    <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2">
                                        <Phone className="w-4 h-4" />
                                        Номер телефону
                                    </label>
                                    <input
                                        type="tel"
                                        value={phoneNumber}
                                        onChange={(e) => setPhoneNumber(e.target.value)}
                                        placeholder="+380..."
                                        className="w-full px-4 py-3 rounded-lg border border-gray-200 focus:border-brand-primary focus:ring-2 focus:ring-brand-light outline-none transition-all bg-white/50 focus:bg-white"
                                    />
                                </div>
                                <div>
                                    <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2">
                                        <Calendar className="w-4 h-4" />
                                        Дата народження
                                    </label>
                                    <input
                                        type="date"
                                        value={bornedAt}
                                        onChange={(e) => setBornedAt(e.target.value)}
                                        className="w-full px-4 py-3 rounded-lg border border-gray-200 focus:border-brand-primary focus:ring-2 focus:ring-brand-light outline-none transition-all bg-white/50 focus:bg-white"
                                    />
                                </div>
                            </div>

                            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                                <div>
                                    <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2">
                                        <Lock className="w-4 h-4" />
                                        Пароль
                                    </label>
                                    <button
                                        onClick={() => setIsChangePasswordModalOpen(true)}
                                        className="w-full px-4 py-3 text-sm rounded-lg bg-white text-brand-primary font-bold hover:bg-brand-primary hover:text-white transition-colors shadow-sm"
                                    >
                                        Змінити пароль
                                    </button>
                                </div>
                            </div>
                        </div>

                        <div className="mt-8 pt-6 border-t border-gray-100 grid grid-cols-2 gap-4">
                            <button
                                onClick={() => navigate('/dashboard/my-courses')}
                                disabled={saving}
                                className="flex items-center justify-center gap-2 px-6 py-3 bg-white text-gray-700 font-bold rounded-lg border border-gray-200 hover:bg-gray-50 transition-colors disabled:opacity-70 disabled:cursor-not-allowed shadow-sm hover:shadow-md transform active:scale-95 duration-200"
                            >
                                Скасувати
                            </button>
                            <button
                                onClick={handleSave}
                                disabled={saving}
                                className="flex items-center justify-center gap-2 px-6 py-3 bg-brand-primary text-white font-bold rounded-lg hover:bg-brand-secondary transition-colors disabled:opacity-70 disabled:cursor-not-allowed shadow-lg hover:shadow-xl transform active:scale-95 duration-200"
                            >
                                {saving ? (
                                    <>
                                        <div className="animate-spin rounded-full h-4 w-4 border-2 border-white/30 border-t-white"></div>
                                        Збереження...
                                    </>
                                ) : (
                                    <>
                                        <Save className="w-5 h-5" />
                                        Зберегти зміни
                                    </>
                                )}
                            </button>
                        </div>
                    </div>
                </div>
            </div>

            <ChangePasswordModal
                isOpen={isChangePasswordModalOpen}
                onClose={() => setIsChangePasswordModalOpen(false)}
                userEmail={email}
            />
        </div>
    );
}
