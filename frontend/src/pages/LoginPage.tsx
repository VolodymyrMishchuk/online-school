import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { login } from '../api/auth';
import { Heart, Eye, EyeOff } from 'lucide-react';
import { useTranslation } from 'react-i18next';

export default function LoginPage() {
    const { t, i18n } = useTranslation();
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [error, setError] = useState('');
    const navigate = useNavigate();

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            const response = await login({ email, password });
            const { accessToken, userId, role, firstName, lastName } = response;
            localStorage.clear(); // Clear previous session
            localStorage.setItem('token', accessToken);
            localStorage.setItem('userId', userId);
            localStorage.setItem('userRole', role);
            localStorage.setItem('user', JSON.stringify({ userId, role, firstName, lastName, email }));
            navigate('/dashboard');
        } catch (err: any) {
            const message = err.response?.data?.message;
            if (message && (message.toLowerCase().includes('google') || message.toLowerCase().includes('apple'))) {
                setError(message);
            } else {
                setError(t('auth.login.invalidCredentials', 'Невірний email або пароль'));
            }
        }
    };

    return (
        <div className="h-full flex-grow flex flex-col items-center justify-center py-12 bg-brand-light/30 px-4 sm:px-6 lg:px-8 font-sans">
            <div className="bg-white p-10 rounded-3xl shadow-xl w-full max-w-md border border-brand-light">
                <div className="text-center mb-8">
                    <Link to="/" className="inline-block bg-brand-light p-3 rounded-full mb-4">
                        <Heart className="w-8 h-8 text-brand-secondary fill-brand-secondary" />
                    </Link>
                    <h2 className="text-3xl font-bold text-brand-dark">
                        {t('auth.login.title', 'З поверненням!')}
                    </h2>
                    <p className="mt-2 text-gray-500">{t('auth.login.subtitle', 'Будь ласка, увійдіть у свій акаунт')}</p>
                </div>

                <form className="space-y-6" onSubmit={handleSubmit}>
                    <div className="space-y-4">
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1 ml-1">{t('auth.login.emailLabel', 'Email')}</label>
                            <input
                                type="email"
                                required
                                className="block w-full px-5 py-3 rounded-xl border border-gray-200 text-gray-900 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-brand-primary/50 focus:border-brand-primary transition-all bg-gray-50 focus:bg-white"
                                placeholder={t('auth.login.emailPlaceholder', 'you@example.com')}
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                            />
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1 ml-1">{t('auth.login.passwordLabel', 'Пароль')}</label>
                            <div className="relative">
                                <input
                                    type={showPassword ? "text" : "password"}
                                    required
                                    className="block w-full px-5 py-3 rounded-xl border border-gray-200 text-gray-900 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-brand-primary/50 focus:border-brand-primary transition-all bg-gray-50 focus:bg-white pr-12"
                                    placeholder={t('auth.login.passwordPlaceholder', '••••••••')}
                                    value={password}
                                    onChange={(e) => setPassword(e.target.value)}
                                />
                                <button
                                    type="button"
                                    onClick={() => setShowPassword(!showPassword)}
                                    className="absolute right-3 top-3.5 text-gray-400 hover:text-gray-600 transition-colors"
                                >
                                    {showPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                                </button>
                            </div>
                        </div>
                        <div className="flex justify-end">
                            <Link to="/forgot-password" className="text-sm font-medium text-brand-primary hover:text-brand-secondary">
                                {t('auth.login.forgotPassword', 'Забули пароль?')}
                            </Link>
                        </div>
                    </div>

                    {error && <div className="p-3 rounded-xl bg-red-50 text-red-500 text-sm text-center font-medium">{error}</div>}

                    <div>
                        <button
                            type="submit"
                            className="w-full py-3.5 px-4 border border-transparent rounded-full shadow-lg text-white bg-brand-primary hover:bg-brand-secondary focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-brand-primary font-bold text-lg transition-all transform hover:-translate-y-0.5"
                        >
                            {t('auth.login.submit', 'Увійти')}
                        </button>
                    </div>
                </form>

                <div className="mt-6">
                    <div className="relative">
                        <div className="absolute inset-0 flex items-center">
                            <div className="w-full border-t border-gray-200"></div>
                        </div>
                        <div className="relative flex justify-center text-sm">
                            <span className="px-2 bg-white text-gray-500">
                                Або
                            </span>
                        </div>
                    </div>
                    
                    <div className="mt-6">
                        <a
                            href="http://localhost:8080/oauth2/authorization/google"
                            className="w-full flex justify-center items-center gap-3 py-3 px-4 border border-gray-200 rounded-full shadow-sm bg-white text-sm font-medium text-gray-700 hover:bg-gray-50 transition-colors"
                            onClick={() => {
                                document.cookie = `frontend_lang=${i18n.language}; path=/; max-age=300`;
                            }}
                        >
                            <svg className="h-5 w-5" viewBox="0 0 24 24">
                                <path
                                    d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
                                    fill="#4285F4"
                                />
                                <path
                                    d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
                                    fill="#34A853"
                                />
                                <path
                                    d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"
                                    fill="#FBBC05"
                                />
                                <path
                                    d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
                                    fill="#EA4335"
                                />
                                <path d="M1 1h22v22H1z" fill="none" />
                            </svg>
                            Увійти через Google
                        </a>
                    </div>
                </div>

                <div className="mt-6 text-center text-sm text-gray-500">
                    {t('auth.login.noAccount', 'Немає акаунту?')} <Link to="/register" className="font-medium text-brand-primary hover:text-brand-secondary">{t('auth.login.signUp', 'Зареєструватися')}</Link>
                </div>
            </div>
        </div>
    );
}
