import { useState } from 'react';
import { Link } from 'react-router-dom';
import { forgotPassword } from '../api/auth';
import { Heart, ArrowLeft, Mail } from 'lucide-react';
import { useTranslation } from 'react-i18next';

export default function ForgotPasswordPage() {
    const { t } = useTranslation();
    const [email, setEmail] = useState('');
    const [submitted, setSubmitted] = useState(false);
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true);
        setError('');
        try {
            await forgotPassword({ email });
            setSubmitted(true);
        } catch (err: any) {
            // If the server returns 200 even for non-existent emails (recommended), this catch might not be hit for that.
            setError(t('auth.forgotPassword.error', 'Щось пішло не так. Спробуйте ще раз пізніше.'));
        } finally {
            setLoading(false);
        }
    };

    if (submitted) {
        return (
            <div className="h-full flex-grow flex flex-col items-center justify-center py-12 bg-brand-light/30 px-4 sm:px-6 lg:px-8 font-sans">
                <div className="bg-white p-10 rounded-3xl shadow-xl w-full max-w-md border border-brand-light text-center">
                    <div className="inline-block bg-green-100 p-4 rounded-full mb-6">
                        <Mail className="w-8 h-8 text-green-600" />
                    </div>
                    <h2 className="text-2xl font-bold text-brand-dark mb-4">{t('auth.forgotPassword.checkEmailTitle', 'Перевірте пошту')}</h2>
                    <p className="text-gray-500 mb-8">
                        {t('auth.forgotPassword.checkEmailDescStart', 'Ми надіслали інструкції для відновлення пароля на')} <strong>{email}</strong>{t('auth.forgotPassword.checkEmailDescEnd', '.')}
                    </p>
                    <Link to="/login" className="text-brand-primary font-semibold hover:text-brand-secondary">
                        {t('auth.forgotPassword.returnToSignIn', 'Повернутися до входу')}
                    </Link>
                </div>
            </div>
        );
    }

    return (
        <div className="h-full flex-grow flex flex-col items-center justify-center py-12 bg-brand-light/30 px-4 sm:px-6 lg:px-8 font-sans">
            <div className="bg-white p-10 rounded-3xl shadow-xl w-full max-w-md border border-brand-light">
                <div className="text-center mb-8">
                    <Link to="/" className="inline-block bg-brand-light p-3 rounded-full mb-4">
                        <Heart className="w-8 h-8 text-brand-secondary fill-brand-secondary" />
                    </Link>
                    <h2 className="text-3xl font-bold text-brand-dark">
                        {t('auth.forgotPassword.title', 'Відновлення пароля')}
                    </h2>
                    <p className="mt-2 text-gray-500">{t('auth.forgotPassword.subtitle', 'Введіть ваш email, щоб отримати посилання для відновлення')}</p>
                </div>

                <form className="space-y-6" onSubmit={handleSubmit}>
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1 ml-1">{t('auth.login.emailLabel', 'Email')}</label>
                        <input
                            type="email"
                            required
                            className="block w-full px-5 py-3 rounded-xl border border-gray-200 text-gray-900 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-brand-primary/50 focus:border-brand-primary transition-all bg-gray-50 focus:bg-white"
                            placeholder="you@example.com"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                        />
                    </div>

                    {error && <div className="p-3 rounded-xl bg-red-50 text-red-500 text-sm text-center font-medium">{error}</div>}

                    <button
                        type="submit"
                        disabled={loading}
                        className="w-full py-3.5 px-4 border border-transparent rounded-full shadow-lg text-white bg-brand-primary hover:bg-brand-secondary focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-brand-primary font-bold text-lg transition-all transform hover:-translate-y-0.5 disabled:opacity-70 disabled:cursor-not-allowed"
                    >
                        {loading ? t('auth.forgotPassword.sending', 'Надсилаємо...') : t('auth.forgotPassword.submit', 'Надіслати')}
                    </button>
                </form>

                <div className="mt-6 text-center text-sm">
                    <Link to="/login" className="font-medium text-gray-500 hover:text-brand-dark flex items-center justify-center gap-2">
                        <ArrowLeft className="w-4 h-4" /> {t('auth.forgotPassword.backToSignIn', 'Назад до входу')}
                    </Link>
                </div>
            </div>
        </div>
    );
}
