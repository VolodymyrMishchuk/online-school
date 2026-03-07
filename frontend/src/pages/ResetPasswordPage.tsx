import { useState, useEffect } from 'react';
import { Link, useSearchParams, useNavigate } from 'react-router-dom';
import { resetPassword } from '../api/auth';
import { Heart, Eye, EyeOff, CheckCircle } from 'lucide-react';
import { useTranslation } from 'react-i18next';

export default function ResetPasswordPage() {
    const { t } = useTranslation();
    const [searchParams] = useSearchParams();
    const token = searchParams.get('token');
    const navigate = useNavigate();

    const [newPassword, setNewPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [showNewPassword, setShowNewPassword] = useState(false);
    const [showConfirmPassword, setShowConfirmPassword] = useState(false);
    const [submitted, setSubmitted] = useState(false);
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);

    // Redirect if no token
    useEffect(() => {
        if (!token) {
            navigate('/login');
        }
    }, [token, navigate]);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError('');

        if (newPassword !== confirmPassword) {
            setError(t('auth.resetPassword.errorMismatch', 'Паролі не співпадають'));
            return;
        }

        if (newPassword.length < 6) {
            setError(t('auth.resetPassword.errorLength', 'Пароль повинен містити мінімум 6 символів'));
            return;
        }

        setLoading(true);
        try {
            if (token) {
                await resetPassword({ token, newPassword });
                setSubmitted(true);
                // Optional: Auto redirect after few seconds
                setTimeout(() => navigate('/login'), 3000);
            }
        } catch (err: any) {
            setError(t('auth.resetPassword.errorInvalid', 'Недійсне або прострочене посилання'));
        } finally {
            setLoading(false);
        }
    };

    if (submitted) {
        return (
            <div className="min-h-screen flex flex-col items-center justify-center bg-brand-light/30 px-4 sm:px-6 lg:px-8 font-sans">
                <div className="bg-white p-10 rounded-3xl shadow-xl w-full max-w-md border border-brand-light text-center">
                    <div className="inline-block bg-green-100 p-4 rounded-full mb-6">
                        <CheckCircle className="w-8 h-8 text-green-600" />
                    </div>
                    <h2 className="text-2xl font-bold text-brand-dark mb-4">{t('auth.resetPassword.successTitle', 'Пароль змінено!')}</h2>
                    <p className="text-gray-500 mb-8">
                        {t('auth.resetPassword.successDesc', 'Ваш пароль успішно оновлено. Тепер ви можете увійти з новим паролем.')}
                    </p>
                    <Link to="/login" className="inline-block w-full py-3.5 px-4 rounded-full bg-brand-primary text-white font-bold hover:bg-brand-secondary transition-all">
                        {t('auth.resetPassword.signInNow', 'Увійти зараз')}
                    </Link>
                </div>
            </div>
        );
    }

    return (
        <div className="min-h-screen flex flex-col items-center justify-center bg-brand-light/30 px-4 sm:px-6 lg:px-8 font-sans">
            <div className="bg-white p-10 rounded-3xl shadow-xl w-full max-w-md border border-brand-light">
                <div className="text-center mb-8">
                    <Link to="/" className="inline-block bg-brand-light p-3 rounded-full mb-4">
                        <Heart className="w-8 h-8 text-brand-secondary fill-brand-secondary" />
                    </Link>
                    <h2 className="text-3xl font-bold text-brand-dark">
                        {t('auth.resetPassword.title', 'Новий пароль')}
                    </h2>
                    <p className="mt-2 text-gray-500">{t('auth.resetPassword.subtitle', 'Встановіть новий пароль для свого акаунта')}</p>
                </div>

                <form className="space-y-6" onSubmit={handleSubmit}>
                    <div className="space-y-4">
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1 ml-1">{t('auth.resetPassword.newPassword', 'Новий пароль')}</label>
                            <div className="relative">
                                <input
                                    type={showNewPassword ? "text" : "password"}
                                    required
                                    className="block w-full px-5 py-3 rounded-xl border border-gray-200 text-gray-900 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-brand-primary/50 focus:border-brand-primary transition-all bg-gray-50 focus:bg-white pr-12"
                                    placeholder={t('auth.login.passwordPlaceholder', '••••••••')}
                                    value={newPassword}
                                    onChange={(e) => setNewPassword(e.target.value)}
                                />
                                <button
                                    type="button"
                                    onClick={() => setShowNewPassword(!showNewPassword)}
                                    className="absolute right-3 top-3.5 text-gray-400 hover:text-gray-600 transition-colors"
                                >
                                    {showNewPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                                </button>
                            </div>
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1 ml-1">{t('auth.resetPassword.confirmPassword', 'Підтвердження пароля')}</label>
                            <div className="relative">
                                <input
                                    type={showConfirmPassword ? "text" : "password"}
                                    required
                                    className="block w-full px-5 py-3 rounded-xl border border-gray-200 text-gray-900 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-brand-primary/50 focus:border-brand-primary transition-all bg-gray-50 focus:bg-white pr-12"
                                    placeholder={t('auth.login.passwordPlaceholder', '••••••••')}
                                    value={confirmPassword}
                                    onChange={(e) => setConfirmPassword(e.target.value)}
                                />
                                <button
                                    type="button"
                                    onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                                    className="absolute right-3 top-3.5 text-gray-400 hover:text-gray-600 transition-colors"
                                >
                                    {showConfirmPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                                </button>
                            </div>
                        </div>
                    </div>

                    {error && <div className="p-3 rounded-xl bg-red-50 text-red-500 text-sm text-center font-medium">{error}</div>}

                    <button
                        type="submit"
                        disabled={loading}
                        className="w-full py-3.5 px-4 border border-transparent rounded-full shadow-lg text-white bg-brand-primary hover:bg-brand-secondary focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-brand-primary font-bold text-lg transition-all transform hover:-translate-y-0.5 disabled:opacity-70 disabled:cursor-not-allowed"
                    >
                        {loading ? t('auth.resetPassword.reseting', 'Зберігаємо...') : t('auth.resetPassword.submit', 'Оновити пароль')}
                    </button>
                </form>
            </div>
        </div>
    );
}
