import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { login } from '../api/auth';
import { Heart, Eye, EyeOff } from 'lucide-react';
import { useTranslation } from 'react-i18next';

export default function LoginPage() {
    const { t } = useTranslation();
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
        } catch (err) {
            setError(t('auth.login.invalidCredentials', 'Невірний email або пароль'));
        }
    };

    return (
        <div className="min-h-screen flex flex-col items-center justify-center bg-brand-light/30 px-4 sm:px-6 lg:px-8 font-sans">
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

                <div className="mt-6 text-center text-sm text-gray-500">
                    {t('auth.login.noAccount', 'Немає акаунту?')} <Link to="/register" className="font-medium text-brand-primary hover:text-brand-secondary">{t('auth.login.signUp', 'Зареєструватися')}</Link>
                </div>
            </div>
        </div>
    );
}
