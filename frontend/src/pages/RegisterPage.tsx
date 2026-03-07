import { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import PhoneInput, { isValidPhoneNumber } from 'react-phone-number-input';
import 'react-phone-number-input/style.css';
import { register } from '../api/auth';
import { Heart } from 'lucide-react';
import { useTranslation } from 'react-i18next';

export default function RegisterPage() {
    const [formData, setFormData] = useState({
        firstName: '',
        lastName: '',
        email: '',
        phoneNumber: '',
        birthDate: '',
        password: ''
    });
    const [formErrors, setFormErrors] = useState<Record<string, string>>({});
    const navigate = useNavigate();
    const [defaultCountry, setDefaultCountry] = useState<any>('UA');
    const { t, i18n } = useTranslation();

    useEffect(() => {
        fetch('https://ipapi.co/json/')
            .then(res => res.json())
            .then(data => {
                if (data && data.country_code) {
                    setDefaultCountry(data.country_code);
                }
            })
            .catch(err => console.error('Error fetching country:', err));
    }, []);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setFormErrors({});

        if (formData.phoneNumber && !isValidPhoneNumber(formData.phoneNumber)) {
            setFormErrors(prev => ({ ...prev, phoneNumber: t('auth.register.invalidPhone', 'Невірний номер телефону') }));
            return;
        }

        try {
            // Format date to ISO OffsetDateTime
            const bornedAt = new Date(formData.birthDate).toISOString();

            const response = await register({
                firstName: formData.firstName,
                lastName: formData.lastName,
                email: formData.email,
                phoneNumber: formData.phoneNumber,
                bornedAt: bornedAt,
                password: formData.password,
                language: i18n.language
            });

            // Зберігаємо токен та дані користувача
            localStorage.setItem('token', response.accessToken);
            localStorage.setItem('userId', response.userId);
            localStorage.setItem('userRole', response.role);

            // Перенаправляємо на dashboard
            navigate('/dashboard');
        } catch (err: any) {
            const message = err.response?.data?.message || err.message;
            setFormErrors({ general: message });
        }
    };

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        setFormData({ ...formData, [e.target.name]: e.target.value });
    };

    return (
        <div className="min-h-screen flex flex-col items-center justify-center bg-brand-light/30 px-4 sm:px-6 lg:px-8 font-sans py-10">
            <div className="bg-white p-10 rounded-3xl shadow-xl w-full max-w-lg border border-brand-light">
                <div className="text-center mb-8">
                    <Link to="/" className="inline-block bg-brand-light p-3 rounded-full mb-4">
                        <Heart className="w-8 h-8 text-brand-secondary fill-brand-secondary" />
                    </Link>
                    <h2 className="text-3xl font-bold text-brand-dark">
                        {t('auth.register.title', 'Приєднуйтесь до нас')}
                    </h2>
                    <p className="mt-2 text-gray-500">{t('auth.register.subtitle', 'Створіть акаунт, щоб почати навчання')}</p>
                </div>

                <form className="space-y-5" onSubmit={handleSubmit}>
                    <div className="grid grid-cols-2 gap-4">
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1 ml-1">{t('auth.register.firstName', 'Імʼя')}</label>
                            <input
                                type="text"
                                name="firstName"
                                required
                                className="block w-full px-5 py-3 rounded-xl border border-gray-200 text-gray-900 focus:outline-none focus:ring-2 focus:ring-brand-primary/50 focus:border-brand-primary transition-all bg-gray-50 focus:bg-white"
                                placeholder={t('auth.register.firstNamePlaceholder', 'John')}
                                value={formData.firstName}
                                onChange={handleChange}
                            />
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1 ml-1">{t('auth.register.lastName', 'Прізвище')}</label>
                            <input
                                type="text"
                                name="lastName"
                                required
                                className="block w-full px-5 py-3 rounded-xl border border-gray-200 text-gray-900 focus:outline-none focus:ring-2 focus:ring-brand-primary/50 focus:border-brand-primary transition-all bg-gray-50 focus:bg-white"
                                placeholder={t('auth.register.lastNamePlaceholder', 'Doe')}
                                value={formData.lastName}
                                onChange={handleChange}
                            />
                        </div>
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1 ml-1">{t('auth.register.email', 'Email')}</label>
                        <input
                            type="email"
                            name="email"
                            required
                            className="block w-full px-5 py-3 rounded-xl border border-gray-200 text-gray-900 focus:outline-none focus:ring-2 focus:ring-brand-primary/50 focus:border-brand-primary transition-all bg-gray-50 focus:bg-white"
                            placeholder="you@example.com"
                            value={formData.email}
                            onChange={handleChange}
                        />
                    </div>

                    <div className="grid grid-cols-2 gap-4">
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1 ml-1">{t('auth.register.phone', 'Номер телефону')}</label>
                            <style>{`
                                .phone-input-override .PhoneInputInput {
                                    border: none;
                                    outline: none;
                                    background: transparent;
                                    width: 100%;
                                    color: inherit;
                                    height: 100%;
                                }
                                .phone-input-override .PhoneInputCountry {
                                    margin-right: 0.5rem;
                                }
                                .phone-input-override .PhoneInputCountrySelect {
                                    outline: none;
                                }
                            `}</style>
                            <div className="block w-full px-5 py-3 rounded-xl border border-gray-200 text-gray-900 focus-within:outline-none focus-within:ring-2 focus-within:ring-brand-primary/50 focus-within:border-brand-primary transition-all bg-gray-50 focus-within:bg-white">
                                <PhoneInput
                                    international
                                    defaultCountry={defaultCountry}
                                    value={formData.phoneNumber}
                                    onChange={(val) => setFormData({ ...formData, phoneNumber: val || '' })}
                                    placeholder="+1234567890"
                                    className="phone-input-override w-full"
                                />
                            </div>
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1 ml-1">{t('auth.register.birthDate', 'Дата народження')}</label>
                            <input
                                type="date"
                                name="birthDate"
                                required
                                className="block w-full px-5 py-3 rounded-xl border border-gray-200 text-gray-900 focus:outline-none focus:ring-2 focus:ring-brand-primary/50 focus:border-brand-primary transition-all bg-gray-50 focus:bg-white"
                                value={formData.birthDate}
                                onChange={handleChange}
                            />
                        </div>
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1 ml-1">{t('auth.register.password', 'Пароль')}</label>
                        <input
                            type="password"
                            name="password"
                            required
                            className="block w-full px-5 py-3 rounded-xl border border-gray-200 text-gray-900 focus:outline-none focus:ring-2 focus:ring-brand-primary/50 focus:border-brand-primary transition-all bg-gray-50 focus:bg-white"
                            placeholder="••••••••"
                            value={formData.password}
                            onChange={handleChange}
                        />
                    </div>

                    {formErrors.general && <div className="p-3 rounded-xl bg-red-50 text-red-500 text-sm text-center font-medium">{formErrors.general}</div>}

                    <div>
                        <button
                            type="submit"
                            className="w-full py-3.5 px-4 border border-transparent rounded-full shadow-lg text-white bg-brand-primary hover:bg-brand-secondary focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-brand-primary font-bold text-lg transition-all transform hover:-translate-y-0.5"
                        >
                            {t('auth.register.submit', 'Зареєструватися')}
                        </button>
                    </div>
                </form>

                <div className="mt-6 text-center text-sm text-gray-500">
                    {t('auth.register.hasAccount', 'Вже маєте акаунт?')} <Link to="/login" className="font-medium text-brand-primary hover:text-brand-secondary">{t('auth.register.signIn', 'Увійти')}</Link>
                </div>
            </div>
        </div>
    );
}
