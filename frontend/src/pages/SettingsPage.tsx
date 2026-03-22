import { User, Mail, Lock, Phone, Calendar, Save, AlertCircle, CheckCircle } from 'lucide-react';
import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import PhoneInput, { isValidPhoneNumber } from 'react-phone-number-input';
import 'react-phone-number-input/style.css';
import ChangePasswordModal from '../components/ChangePasswordModal';
import { getPerson, updatePerson } from '../api/persons';
import type { PersonDto } from '../api/persons';
import { useTranslation } from 'react-i18next';
import { useCountryCode } from '../hooks/useCountryCode';

export default function SettingsPage() {
    const userId = localStorage.getItem('userId');
    const [, setProfile] = useState<PersonDto | null>(null);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [message, setMessage] = useState<{ type: 'success' | 'error', text: string } | null>(null);
    const navigate = useNavigate();
    const { t, i18n } = useTranslation();

    const LANGUAGES = [
        { code: 'uk', label: t('languages.ukrainian', 'Українська') },
        { code: 'en', label: t('languages.english', 'English') },
        { code: 'de', label: t('languages.german', 'Deutsch') },
    ];
    const [firstName, setFirstName] = useState('');
    const [lastName, setLastName] = useState('');
    const [email, setEmail] = useState('');
    const [phoneNumber, setPhoneNumber] = useState('');
    const [bornedAt, setBornedAt] = useState('');
    const [language, setLanguage] = useState('');

    const [isChangePasswordModalOpen, setIsChangePasswordModalOpen] = useState(false);
    const defaultCountry = useCountryCode('UA');

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
            setLanguage(data.language || 'uk');

            if (data.language && data.language !== i18n.language) {
                i18n.changeLanguage(data.language);
            }

            // Format date for input type="date" (YYYY-MM-DD)
            if (data.bornedAt) {
                const date = new Date(data.bornedAt);
                setBornedAt(date.toISOString().split('T')[0]);
            } else {
                setBornedAt('');
            }
        } catch (error) {
            console.error('Failed to fetch profile:', error);
            setMessage({ type: 'error', text: t('settings.loadError', 'Failed to load profile data') });
        } finally {
            setLoading(false);
        }
    };

    const handleSave = async () => {
        if (!userId) return;

        if (phoneNumber && !isValidPhoneNumber(phoneNumber)) {
            setMessage({ type: 'error', text: t('settings.invalidPhoneError', 'Invalid phone number') });
            return;
        }

        setSaving(true);
        setMessage(null);

        try {
            await updatePerson(userId, {
                firstName,
                lastName,
                phoneNumber,
                bornedAt: bornedAt ? new Date(bornedAt).toISOString() : undefined,
                email, // sending email back just in case, though usually ignored if not changed
                language
            });

            if (language !== i18n.language) {
                i18n.changeLanguage(language);
            }

            setMessage({ type: 'success', text: t('settings.successMessage', 'Settings saved successfully') });

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
            setMessage({ type: 'error', text: t('settings.updateError', 'Failed to update profile') });
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
                <h1 className="text-3xl font-bold text-brand-dark">{t('settings.title')}</h1>
            </div>

            <div className="max-w-2xl">
                <div className="glass-panel rounded-lg overflow-hidden">
                    <div className="px-8 py-6 border-b border-gray-200/50 bg-white/30 backdrop-blur-sm">
                        <h2 className="text-xl font-bold text-brand-dark">{t('settings.myProfile', 'My Profile')}</h2>
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
                                        {t('settings.firstNameLabel', 'First Name')}
                                    </label>
                                    <input
                                        type="text"
                                        value={firstName}
                                        onChange={(e) => setFirstName(e.target.value)}
                                        placeholder={t('settings.firstNamePlaceholder', 'Your first name')}
                                        className="w-full px-4 py-3 rounded-lg border border-gray-200 focus:border-brand-primary focus:ring-2 focus:ring-brand-light outline-none transition-all bg-white/50 focus:bg-white"
                                    />
                                </div>
                                <div>
                                    <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2">
                                        <User className="w-4 h-4" />
                                        {t('settings.lastNameLabel', 'Last Name')}
                                    </label>
                                    <input
                                        type="text"
                                        value={lastName}
                                        onChange={(e) => setLastName(e.target.value)}
                                        placeholder={t('settings.lastNamePlaceholder', 'Your last name')}
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
                                <p className="mt-1 text-xs text-gray-400">{t('settings.emailHelpText', 'Email cannot be changed directly.')}</p>
                            </div>

                            <div>
                                <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2">
                                    {t('settings.language', 'Мова інтерфейсу')}
                                </label>
                                <div className="relative">
                                    <select
                                        value={language}
                                        onChange={(e) => setLanguage(e.target.value)}
                                        className="w-full px-4 py-3 rounded-lg border border-gray-200 focus:border-brand-primary focus:ring-2 focus:ring-brand-light outline-none transition-all bg-white/50 focus:bg-white appearance-none cursor-pointer"
                                    >
                                        {LANGUAGES.map((lang) => (
                                            <option key={lang.code} value={lang.code}>
                                                {lang.label}
                                            </option>
                                        ))}
                                    </select>
                                    <div className="absolute inset-y-0 right-0 flex items-center px-4 pointer-events-none text-gray-500">
                                        ▼
                                    </div>
                                </div>
                            </div>

                            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                                <div>
                                    <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2">
                                        <Phone className="w-4 h-4" />
                                        {t('settings.phoneLabel', 'Phone Number')}
                                    </label>
                                    <style>{`
                                        .phone-input-override .PhoneInputInput {
                                            border: none;
                                            outline: none;
                                            background: transparent;
                                            width: 100%;
                                            color: inherit;
                                        }
                                        .phone-input-override .PhoneInputCountry {
                                            margin-right: 0.5rem;
                                        }
                                        .phone-input-override .PhoneInputCountrySelect {
                                            outline: none;
                                        }
                                    `}</style>
                                    <PhoneInput
                                        international
                                        defaultCountry={defaultCountry}
                                        value={phoneNumber}
                                        onChange={(val) => setPhoneNumber(val || '')}
                                        placeholder="+380..."
                                        className="w-full px-4 py-3 rounded-lg border border-gray-200 focus-within:border-brand-primary focus-within:ring-2 focus-within:ring-brand-light outline-none transition-all bg-white/50 focus-within:bg-white phone-input-override"
                                    />
                                </div>
                                <div>
                                    <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2">
                                        <Calendar className="w-4 h-4" />
                                        {t('settings.birthDateLabel', 'Birth Date')}
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
                                        {t('settings.passwordLabel', 'Password')}
                                    </label>
                                    <button
                                        onClick={() => setIsChangePasswordModalOpen(true)}
                                        className="w-full px-4 py-3 text-sm rounded-lg bg-white text-brand-primary font-bold hover:bg-brand-primary hover:text-white transition-colors shadow-sm"
                                    >
                                        {t('settings.changePassword', 'Change Password')}
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
                                {t('settings.cancel', 'Cancel')}
                            </button>
                            <button
                                onClick={handleSave}
                                disabled={saving}
                                className="flex items-center justify-center gap-2 px-6 py-3 bg-brand-primary text-white font-bold rounded-lg hover:bg-brand-secondary transition-colors disabled:opacity-70 disabled:cursor-not-allowed shadow-lg hover:shadow-xl transform active:scale-95 duration-200"
                            >
                                {saving ? (
                                    <>
                                        <div className="animate-spin rounded-full h-4 w-4 border-2 border-white/30 border-t-white"></div>
                                        {t('settings.saving', 'Saving...')}
                                    </>
                                ) : (
                                    <>
                                        <Save className="w-5 h-5" />
                                        {t('settings.save')}
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
