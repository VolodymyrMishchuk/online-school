import React, { useState, useEffect } from 'react';
import { createPortal } from 'react-dom';
import { X, CreditCard, Lock, CheckCircle2, AlertCircle, Loader2 } from 'lucide-react';
import { login, register, logout as apiLogout } from '../../api/auth';
import { processPayment } from '../../api/payments';
import { getMyEnrollments } from '../../api/enrollments';
import { useNavigate } from 'react-router-dom';

interface PaymentModalProps {
    isOpen: boolean;
    onClose: () => void;
    courseId: string;
    courseName: string;
    price: number;
    currency?: string;
    onSuccess?: () => void;
}

export function PaymentModal({ isOpen, onClose, courseId, courseName, price, currency = '€', onSuccess }: PaymentModalProps) {
    
    const navigate = useNavigate();
    // Auth state
    const [isAuthenticated, setIsAuthenticated] = useState(!!localStorage.getItem('token'));
    const [authMode, setAuthMode] = useState<'login' | 'register'>('login');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [firstName, setFirstName] = useState('');
    const [lastName, setLastName] = useState('');
    
    // Payment state
    const [paymentSystem, setPaymentSystem] = useState<'STRIPE_CARD' | 'STRIPE_PAYPAL'>('STRIPE_CARD');
    const [country] = useState('UA');
    const [isProcessing, setIsProcessing] = useState(false);
    const [error, setError] = useState('');
    const [isSuccess, setIsSuccess] = useState(false);
    const [isAlreadyEnrolled, setIsAlreadyEnrolled] = useState(false);
    const [isCheckingEnrollment, setIsCheckingEnrollment] = useState(false);

    const checkEnrollment = async () => {
        setIsCheckingEnrollment(true);
        try {
            const userId = localStorage.getItem('userId');
            if (userId) {
                const enrollments = await getMyEnrollments(userId);
                const activeEnrollment = enrollments.find(
                    e => e.courseId === courseId && (e.status === 'ACTIVE' || e.status === 'PENDING')
                );
                if (activeEnrollment) {
                    setIsAlreadyEnrolled(true);
                }
            }
        } catch (err) {
            console.error("Failed to check enrollment status", err);
        } finally {
            setIsCheckingEnrollment(false);
        }
    };

    useEffect(() => {
        if (isOpen) {
            const token = localStorage.getItem('token');
            setIsAuthenticated(!!token);
            setIsSuccess(false);
            setError('');
            setIsAlreadyEnrolled(false);
            
            if (token) {
                checkEnrollment();
            }
        }
    }, [isOpen]);

    if (!isOpen) return null;

    const handleAuth = async (e: React.FormEvent) => {
        e.preventDefault();
        setError('');
        setIsProcessing(true);
        try {
            if (authMode === 'login') {
                const response = await login({ email, password });
                localStorage.setItem('token', response.accessToken);
                localStorage.setItem('userId', response.userId);
                localStorage.setItem('userRole', response.role);
                localStorage.setItem('user', JSON.stringify({ 
                    userId: response.userId, 
                    role: response.role, 
                    firstName: response.firstName, 
                    lastName: response.lastName, 
                    email 
                }));
            } else {
                await register({ firstName, lastName, email, password, phoneNumber: '+380000000000', bornedAt: '2000-01-01T00:00:00Z' });
                // Automatically login after register
                const response = await login({ email, password });
                localStorage.setItem('token', response.accessToken);
                localStorage.setItem('userId', response.userId);
                localStorage.setItem('userRole', response.role);
                localStorage.setItem('user', JSON.stringify({ 
                    userId: response.userId, 
                    role: response.role, 
                    firstName: response.firstName, 
                    lastName: response.lastName, 
                    email 
                }));
            }
            setIsAuthenticated(true);
            await checkEnrollment();
        } catch (err: any) {
            setError(err.response?.data?.message || 'Помилка авторизації');
        } finally {
            setIsProcessing(false);
        }
    };

    const handlePayment = async () => {
        
        setError('');
        setIsProcessing(true);
        try {
            await processPayment({
                courseId,
                paymentSystem,
                country
            });
            setIsSuccess(true);
            if (onSuccess) onSuccess();
        } catch (err: any) {
            setError(err.response?.data?.message || 'Сталася помилка при оплаті');
        } finally {
            setIsProcessing(false);
        }
    };

    return createPortal(
        <div className="fixed inset-0 z-[9999] flex items-center justify-center bg-white/40 backdrop-blur-md p-4">
            <div className={`bg-white rounded-3xl w-full ${isAuthenticated ? 'max-w-md' : 'max-w-4xl'} shadow-2xl overflow-hidden animate-in fade-in zoom-in duration-300`}>
                {/* Header */}
                <div className="relative bg-brand-light/30 px-6 py-4 border-b border-gray-100 flex items-center justify-between">
                    <h3 className="font-bold text-gray-900 text-lg flex items-center gap-2">
                        <CreditCard className="w-5 h-5 text-brand-primary" />
                        Оформлення покупки
                    </h3>
                    <button 
                        onClick={onClose}
                        className="p-2 -mr-2 text-gray-400 hover:text-gray-600 hover:bg-white rounded-full transition-colors focus:outline-none z-10"
                    >
                        <X className="w-5 h-5" />
                    </button>
                </div>

                {/* Content */}
                {isSuccess ? (
                    <div className="p-10">
                        <div className="text-center py-8">
                            <div className="w-16 h-16 bg-emerald-100 rounded-full flex items-center justify-center mx-auto mb-4">
                                <CheckCircle2 className="w-8 h-8 text-emerald-500" />
                            </div>
                            <h3 className="text-xl md:text-2xl font-bold text-gray-900 mb-2">Оплата успішна!</h3>
                            <p className="text-gray-500 mb-6 max-w-sm mx-auto">Дякуємо за покупку. Чек та інструкції було відправлено на вашу електронну пошту.</p>
                            <button
                                onClick={onClose}
                                className="w-full max-w-xs py-3 bg-brand-primary text-white rounded-xl font-bold hover:bg-brand-secondary transition-colors"
                            >
                                Закрити
                            </button>
                        </div>
                    </div>
                ) : (
                    <div className="flex flex-col md:flex-row relative">
                        {/* Left Pane - Auth */}
                        {!isAuthenticated && (
                            <div className="w-full md:w-1/2 p-6 md:p-8 border-b md:border-b-0 md:border-r border-gray-100 relative bg-gray-50/30">
                            <div className="space-y-6">
                                <div className="text-center">
                                    <h4 className="text-lg font-bold text-gray-900 mb-1">Реєстрація / Вхід</h4>
                                    <p className="text-sm text-gray-500">Щоб придбати курс, потрібно авторизуватися</p>
                                </div>

                                <div className="flex bg-gray-100 p-1 rounded-xl">
                                    <button
                                        onClick={() => setAuthMode('login')}
                                        className={`flex-1 py-2 text-sm font-medium rounded-lg transition-colors ${authMode === 'login' ? 'bg-white text-gray-900 shadow-sm' : 'text-gray-500 hover:text-gray-700'}`}
                                    >
                                        Вхід
                                    </button>
                                    <button
                                        onClick={() => setAuthMode('register')}
                                        className={`flex-1 py-2 text-sm font-medium rounded-lg transition-colors ${authMode === 'register' ? 'bg-white text-gray-900 shadow-sm' : 'text-gray-500 hover:text-gray-700'}`}
                                    >
                                        Реєстрація
                                    </button>
                                </div>

                                <form onSubmit={handleAuth} className="space-y-4">
                                    {authMode === 'register' && (
                                        <div className="flex gap-4">
                                            <div className="flex-1">
                                                <label className="block text-xs font-medium text-gray-700 mb-1">Ім'я</label>
                                                <input type="text" required value={firstName} onChange={e => setFirstName(e.target.value)} className="w-full px-4 py-2 bg-white border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-brand-primary/50" />
                                            </div>
                                            <div className="flex-1">
                                                <label className="block text-xs font-medium text-gray-700 mb-1">Прізвище</label>
                                                <input type="text" required value={lastName} onChange={e => setLastName(e.target.value)} className="w-full px-4 py-2 bg-white border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-brand-primary/50" />
                                            </div>
                                        </div>
                                    )}
                                    <div>
                                        <label className="block text-xs font-medium text-gray-700 mb-1">Email</label>
                                        <input type="email" required value={email} onChange={e => setEmail(e.target.value)} className="w-full px-4 py-2 bg-white border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-brand-primary/50" />
                                    </div>
                                    <div>
                                        <label className="block text-xs font-medium text-gray-700 mb-1">Пароль</label>
                                        <input type="password" required value={password} onChange={e => setPassword(e.target.value)} className="w-full px-4 py-2 bg-white border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-brand-primary/50" />
                                    </div>

                                    {error && !isAuthenticated && (
                                        <div className="p-3 bg-red-50 text-red-500 text-xs rounded-xl flex items-start gap-2">
                                            <AlertCircle className="w-4 h-4 shrink-0" />
                                            <span>{error}</span>
                                        </div>
                                    )}

                                    <button
                                        type="submit"
                                        disabled={isProcessing}
                                        className="w-full py-3 bg-brand-primary text-white rounded-xl font-bold hover:bg-brand-secondary transition-colors disabled:opacity-70 flex justify-center items-center gap-2"
                                    >
                                        {isProcessing && <Loader2 className="w-4 h-4 animate-spin" />}
                                        {authMode === 'login' ? 'Увійти' : 'Зареєструватися'}
                                    </button>
                                </form>

                                <div className="mt-4">
                                    <div className="relative mb-4">
                                        <div className="absolute inset-0 flex items-center">
                                            <div className="w-full border-t border-gray-200"></div>
                                        </div>
                                        <div className="relative flex justify-center text-sm">
                                            <span className="px-2 bg-gray-50/30 text-gray-500">
                                                Або
                                            </span>
                                        </div>
                                    </div>
                                    <button
                                        type="button"
                                        className="w-full flex justify-center items-center gap-3 py-3 px-4 border border-gray-200 rounded-xl shadow-sm bg-white text-sm font-medium text-gray-700 hover:bg-gray-50 transition-colors"
                                        onClick={(e) => {
                                            e.preventDefault();
                                            document.cookie = `frontend_lang=uk; path=/; max-age=300`;
                                            
                                            const width = 500;
                                            const height = 600;
                                            const left = window.screen.width / 2 - width / 2;
                                            const top = window.screen.height / 2 - height / 2;
                                            const popup = window.open('http://localhost:8080/oauth2/authorization/google', 'Google Login', `width=${width},height=${height},top=${top},left=${left},popup=yes,menubar=no,toolbar=no,location=no,status=no`);
                                            
                                            const handleMessage = (event: MessageEvent) => {
                                                if (event.origin !== window.location.origin) return;
                                                if (event.data?.type === 'OAUTH_SUCCESS') {
                                                    const { payload } = event.data;
                                                    localStorage.setItem('token', payload.token);
                                                    localStorage.setItem('userId', payload.userId);
                                                    localStorage.setItem('userRole', payload.role);
                                                    localStorage.setItem('user', JSON.stringify(payload));
                                                    
                                                    setIsAuthenticated(true);
                                                    window.removeEventListener('message', handleMessage);
                                                }
                                            };
                                            
                                            window.addEventListener('message', handleMessage);
                                        }}
                                    >
                                        <svg className="h-5 w-5" viewBox="0 0 24 24">
                                            <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" fill="#4285F4" />
                                            <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853" />
                                            <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="#FBBC05" />
                                            <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335" />
                                            <path d="M1 1h22v22H1z" fill="none" />
                                        </svg>
                                        Продовжити з Google
                                    </button>
                                </div>
                            </div>
                        </div>
                        )}

                        {/* Right Pane - Payment */}
                        <div className={`w-full ${!isAuthenticated ? 'md:w-1/2' : ''} p-6 md:p-8 relative flex flex-col`}>
                            {!isAuthenticated && !isAlreadyEnrolled && (
                                <div className="absolute inset-0 z-10 bg-white/60 backdrop-blur-[2px] flex flex-col items-center justify-center">
                                    <div className="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mb-4 shadow-sm animate-in zoom-in">
                                        <Lock className="w-8 h-8 text-gray-400" />
                                    </div>
                                    <h3 className="text-xl font-bold text-gray-900 mb-1">Оплата недоступна</h3>
                                    <p className="text-sm text-gray-600 text-center px-4">Спочатку увійдіть або зареєструйтесь,<br/>щоб здійснити покупку</p>
                                </div>
                            )}

                            {isCheckingEnrollment ? (
                                <div className="flex flex-col items-center justify-center h-full flex-1 min-h-[300px]">
                                    <Loader2 className="w-8 h-8 animate-spin text-brand-primary mb-4" />
                                    <p className="text-gray-500 text-sm">Перевірка доступу...</p>
                                </div>
                            ) : isAlreadyEnrolled ? (
                                <div className="flex flex-col items-center justify-center h-full flex-1 text-center py-4 min-h-[300px]">
                                    <div className="w-16 h-16 bg-blue-100 rounded-full flex items-center justify-center mb-4">
                                        <CheckCircle2 className="w-8 h-8 text-blue-500" />
                                    </div>
                                    <h3 className="text-xl font-bold text-gray-900 mb-2">Курс вже придбано</h3>
                                    <p className="text-sm text-gray-600 mb-8">Ви вже маєте доступ до цього курсу. Оплата не потрібна.</p>
                                    
                                    <div className="flex flex-col w-full gap-3 mt-auto">
                                        <button
                                            onClick={() => {
                                                onClose();
                                                navigate('/dashboard/my-courses');
                                            }}
                                            className="w-full py-3 bg-brand-primary text-white rounded-xl font-bold hover:bg-brand-secondary transition-colors"
                                        >
                                            До курсу
                                        </button>
                                        <button
                                            onClick={onClose}
                                            className="w-full py-3 bg-gray-100 text-gray-700 rounded-xl font-bold hover:bg-gray-200 transition-colors"
                                        >
                                            Скасувати
                                        </button>
                                    </div>
                                </div>
                            ) : (
                                <div className="space-y-6 flex-1 flex flex-col">
                                    {/* Order Summary */}
                                    <div className="bg-gray-50 rounded-xl p-4 border border-gray-100">
                                        <h4 className="text-xs font-bold text-gray-500 uppercase tracking-wider mb-3">Ваше замовлення</h4>
                                        <div className="flex justify-between items-start mb-2">
                                            <span className="text-gray-900 font-medium pr-4">{courseName}</span>
                                            <span className="text-gray-900 font-bold whitespace-nowrap">{price.toFixed(2)} {currency}</span>
                                        </div>
                                        <div className="pt-3 border-t border-gray-200 mt-3 flex justify-between items-center">
                                            <span className="text-gray-500 text-sm">До сплати</span>
                                            <span className="text-brand-primary font-black text-xl">{price.toFixed(2)} {currency}</span>
                                        </div>
                                    </div>

                                    {/* Payment Method */}
                                    <div>
                                        <h4 className="text-xs font-bold text-gray-500 uppercase tracking-wider mb-3">Метод оплати</h4>
                                        <div className="space-y-3">
                                            <label className={`flex items-center gap-3 p-4 border rounded-xl cursor-pointer transition-colors ${paymentSystem === 'STRIPE_CARD' ? 'border-brand-primary bg-brand-light/10 shadow-sm' : 'border-gray-200 hover:border-brand-primary/50'}`}>
                                                <input type="radio" name="paymentMethod" checked={paymentSystem === 'STRIPE_CARD'} onChange={() => setPaymentSystem('STRIPE_CARD')} className="w-4 h-4 text-brand-primary focus:ring-brand-primary" />
                                                <div className="flex-1 flex justify-between items-center">
                                                    <span className="font-medium text-gray-900">Банківська картка</span>
                                                    <div className="flex gap-1">
                                                        <div className="w-8 h-5 bg-gray-200 rounded shrink-0 flex items-center justify-center text-[8px] font-bold text-gray-500">VISA</div>
                                                        <div className="w-8 h-5 bg-gray-200 rounded shrink-0 flex items-center justify-center text-[8px] font-bold text-gray-500">MC</div>
                                                    </div>
                                                </div>
                                            </label>
                                            <label className={`flex items-center gap-3 p-4 border rounded-xl cursor-pointer transition-colors ${paymentSystem === 'STRIPE_PAYPAL' ? 'border-brand-primary bg-brand-light/10 shadow-sm' : 'border-gray-200 hover:border-brand-primary/50'}`}>
                                                <input type="radio" name="paymentMethod" checked={paymentSystem === 'STRIPE_PAYPAL'} onChange={() => setPaymentSystem('STRIPE_PAYPAL')} className="w-4 h-4 text-brand-primary focus:ring-brand-primary" />
                                                <div className="flex-1 flex justify-between items-center">
                                                    <span className="font-medium text-gray-900">PayPal</span>
                                                    <div className="w-12 h-5 bg-blue-100 rounded shrink-0 flex items-center justify-center text-[10px] font-bold text-blue-800 italic">PayPal</div>
                                                </div>
                                            </label>
                                        </div>
                                    </div>

                                    {error && isAuthenticated && (
                                        <div className="p-3 bg-red-50 text-red-500 text-xs rounded-xl flex items-start gap-2">
                                            <AlertCircle className="w-4 h-4 shrink-0" />
                                            <span>{error}</span>
                                        </div>
                                    )}

                                    <button
                                        onClick={handlePayment}
                                        disabled={isProcessing || !isAuthenticated}
                                        className="w-full py-3.5 bg-brand-primary text-white rounded-full shadow-lg font-bold hover:bg-brand-secondary transition-all disabled:opacity-70 flex justify-center items-center gap-2 mt-auto"
                                    >
                                        {isProcessing ? <Loader2 className="w-5 h-5 animate-spin" /> : <Lock className="w-4 h-4" />}
                                        Оплатити {price.toFixed(2)} {currency}
                                    </button>
                                    <p className="text-center text-xs text-gray-400 mt-3 flex items-center justify-center gap-1">
                                        <Lock className="w-3 h-3" /> Безпечна оплата гарантована
                                    </p>
                                </div>
                            )}
                        </div>
                    </div>
                )}
            </div>
        </div>
    , document.body);
}
