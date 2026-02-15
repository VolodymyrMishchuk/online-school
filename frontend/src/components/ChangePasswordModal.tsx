import { useState } from 'react';
import { changePassword, forgotPassword } from '../api/auth';
import { Eye, EyeOff, Lock, AlertCircle, CheckCircle, Mail, X } from 'lucide-react';

interface ChangePasswordModalProps {
    isOpen: boolean;
    onClose: () => void;
    userEmail: string;
}

export default function ChangePasswordModal({ isOpen, onClose, userEmail }: ChangePasswordModalProps) {
    const [view, setView] = useState<'change' | 'forgot' | 'success'>('change');

    // Change Password State
    const [oldPassword, setOldPassword] = useState('');
    const [newPassword, setNewPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [showOldPass, setShowOldPass] = useState(false);
    const [showNewPass, setShowNewPass] = useState(false);
    const [showConfirmPass, setShowConfirmPass] = useState(false);

    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [successMessage, setSuccessMessage] = useState('');

    if (!isOpen) return null;

    const resetState = () => {
        setOldPassword('');
        setNewPassword('');
        setConfirmPassword('');
        setError('');
        setSuccessMessage('');
        setView('change');
        setLoading(false);
    };

    const handleClose = () => {
        onClose();
        setTimeout(resetState, 300); // Reset state after animation
    };

    const handleChangePassword = async (e: React.FormEvent) => {
        e.preventDefault();
        setError('');

        if (newPassword !== confirmPassword) {
            setError('Паролі не співпадають');
            return;
        }

        if (newPassword.length < 6) {
            setError('Новий пароль повинен містити щонайменше 6 символів');
            return;
        }

        setLoading(true);
        try {
            await changePassword({ oldPassword, newPassword });
            setSuccessMessage('Пароль успішно змінено');
            setView('success');
        } catch (err: any) {
            setError('Не вдалося змінити пароль. Перевірте старий пароль.');
        } finally {
            setLoading(false);
        }
    };

    const handleForgotPassword = async () => {
        setLoading(true);
        setError('');
        try {
            await forgotPassword({ email: userEmail });
            setSuccessMessage(`Посилання для відновлення надіслано на ${userEmail}`);
            setView('success');
        } catch (err: any) {
            setError('Не вдалося надіслати посилання. Спробуйте ще раз.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-white/30 backdrop-blur-md animate-in fade-in duration-200">
            {/* Glass Panel Modal */}
            <div
                className="glass-panel w-full max-w-md flex flex-col overflow-hidden animate-in zoom-in-95 duration-200 relative rounded-lg shadow-xl"
                style={{ background: 'rgba(255, 255, 255, 0.9)', maxHeight: '90vh' }}
            >
                {/* Header Bar - Static */}
                <div className="flex items-center justify-between px-6 py-5 border-b border-gray-100 bg-white shrink-0 z-10 relative shadow-sm">
                    <div className="flex items-center gap-3">
                        <div className="flex items-center justify-center w-10 h-10 rounded-full bg-brand-light/50 text-brand-primary ring-2 ring-white shadow-sm">
                            {view === 'success' ? <CheckCircle className="w-5 h-5" /> : <Lock className="w-5 h-5" />}
                        </div>
                        <div>
                            <h2 className="text-xl font-bold text-brand-dark">
                                {view === 'change' && 'Зміна паролю'}
                                {view === 'forgot' && 'Відновлення паролю'}
                                {view === 'success' && 'Успіх!'}
                            </h2>
                        </div>
                    </div>
                    <button
                        onClick={handleClose}
                        className="p-2 text-gray-400 hover:text-brand-primary hover:bg-gray-100 rounded-lg transition-all"
                    >
                        <X className="w-5 h-5" />
                    </button>
                </div>

                {/* Body - Scrollable */}
                <div className="flex-1 overflow-y-auto px-8 py-6 custom-scrollbar flex flex-col gap-6">
                    {view === 'forgot' && (
                        <p className="text-sm text-gray-500">
                            Це ваша електронна адреса? Ми надішлемо посилання для відновлення туди.
                        </p>
                    )}

                    {view === 'change' && (
                        <form id="change-password-form" onSubmit={handleChangePassword} className="space-y-5">
                            {/* Old Password */}
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-2 ml-1">Старий пароль</label>
                                <div className="relative">
                                    <input
                                        type={showOldPass ? "text" : "password"}
                                        value={oldPassword}
                                        onChange={(e) => setOldPassword(e.target.value)}
                                        className="w-full px-4 py-3 rounded-lg border border-gray-200 bg-white/50 outline-none transition-all focus:border-brand-primary focus:ring-2 focus:ring-brand-light focus:bg-white pr-10"
                                        placeholder="••••••••"
                                        required
                                    />
                                    <button
                                        type="button"
                                        onClick={() => setShowOldPass(!showOldPass)}
                                        className="absolute right-3 top-3 text-gray-400 hover:text-brand-primary transition-colors"
                                    >
                                        {showOldPass ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                                    </button>
                                </div>
                            </div>

                            {/* New Password */}
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-2 ml-1">Новий пароль</label>
                                <div className="relative">
                                    <input
                                        type={showNewPass ? "text" : "password"}
                                        value={newPassword}
                                        onChange={(e) => setNewPassword(e.target.value)}
                                        className="w-full px-4 py-3 rounded-lg border border-gray-200 bg-white/50 outline-none transition-all focus:border-brand-primary focus:ring-2 focus:ring-brand-light focus:bg-white pr-10"
                                        placeholder="••••••••"
                                        required
                                    />
                                    <button
                                        type="button"
                                        onClick={() => setShowNewPass(!showNewPass)}
                                        className="absolute right-3 top-3 text-gray-400 hover:text-brand-primary transition-colors"
                                    >
                                        {showNewPass ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                                    </button>
                                </div>
                            </div>

                            {/* Confirm Password */}
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-2 ml-1">Підтвердіть пароль</label>
                                <div className="relative">
                                    <input
                                        type={showConfirmPass ? "text" : "password"}
                                        value={confirmPassword}
                                        onChange={(e) => setConfirmPassword(e.target.value)}
                                        className="w-full px-4 py-3 rounded-lg border border-gray-200 bg-white/50 outline-none transition-all focus:border-brand-primary focus:ring-2 focus:ring-brand-light focus:bg-white pr-10"
                                        placeholder="••••••••"
                                        required
                                    />
                                    <button
                                        type="button"
                                        onClick={() => setShowConfirmPass(!showConfirmPass)}
                                        className="absolute right-3 top-3 text-gray-400 hover:text-brand-primary transition-colors"
                                    >
                                        {showConfirmPass ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                                    </button>
                                </div>
                            </div>

                            <div className="flex justify-end">
                                <button
                                    type="button"
                                    onClick={() => setView('forgot')}
                                    className="text-sm font-bold text-brand-primary hover:text-brand-secondary transition-colors"
                                >
                                    Забули пароль?
                                </button>
                            </div>

                            {error && (
                                <div className="flex items-center gap-2 p-3 text-sm text-red-600 bg-red-50 rounded-lg border border-red-100">
                                    <AlertCircle className="w-4 h-4 shrink-0" />
                                    {error}
                                </div>
                            )}
                        </form>
                    )}

                    {view === 'forgot' && (
                        <div className="space-y-6">
                            <div className="flex items-center gap-3 p-4 bg-blue-50/50 border border-blue-100 text-blue-800 rounded-lg">
                                <Mail className="w-6 h-6 shrink-0 text-blue-500" />
                                <span className="font-medium text-lg truncate">{userEmail}</span>
                            </div>

                            {error && (
                                <div className="flex items-center gap-2 p-3 text-sm text-red-600 bg-red-50 rounded-lg border border-red-100">
                                    <AlertCircle className="w-4 h-4 shrink-0" />
                                    {error}
                                </div>
                            )}
                        </div>
                    )}

                    {view === 'success' && (
                        <div className="text-center space-y-8">
                            <div className="bg-green-50 text-green-700 p-4 rounded-lg border border-green-100">
                                <p className="font-medium">{successMessage}</p>
                            </div>
                        </div>
                    )}
                </div>

                {/* Footer - Static */}
                <div className="flex gap-4 p-6 border-t border-gray-100 bg-white/50 backdrop-blur-sm shrink-0">
                    {view === 'change' && (
                        <>
                            <button
                                type="button"
                                onClick={handleClose}
                                className="flex-1 py-3 font-bold text-brand-primary bg-white hover:bg-brand-primary hover:text-white rounded-lg transition-colors shadow-sm border border-gray-100"
                            >
                                Скасувати
                            </button>
                            <button
                                type="submit"
                                form="change-password-form"
                                disabled={loading}
                                className="flex-1 py-3 font-bold text-white bg-brand-primary hover:bg-brand-secondary rounded-lg transition-all shadow-lg hover:shadow-xl transform active:scale-95 duration-200 disabled:opacity-70 disabled:transform-none"
                            >
                                {loading ? (
                                    <div className="flex items-center justify-center gap-2">
                                        <div className="animate-spin rounded-full h-4 w-4 border-2 border-white/30 border-t-white"></div>
                                        <span>Збереження...</span>
                                    </div>
                                ) : 'Змінити'}
                            </button>
                        </>
                    )}

                    {view === 'forgot' && (
                        <>
                            <button
                                type="button"
                                onClick={() => setView('change')}
                                className="flex-1 py-3 font-bold text-gray-500 bg-white hover:bg-gray-100 rounded-lg transition-colors shadow-sm"
                            >
                                Скасувати
                            </button>
                            <button
                                type="button"
                                onClick={handleForgotPassword}
                                disabled={loading}
                                className="flex-1 py-3 font-bold text-white bg-brand-primary hover:bg-brand-secondary rounded-lg transition-all shadow-lg hover:shadow-xl transform active:scale-95 duration-200 disabled:opacity-70"
                            >
                                {loading ? 'Надсилання...' : 'Надіслати'}
                            </button>
                        </>
                    )}

                    {view === 'success' && (
                        <button
                            onClick={handleClose}
                            className="w-full py-3 font-bold text-white bg-brand-primary hover:bg-brand-secondary rounded-lg transition-all shadow-lg hover:shadow-xl transform active:scale-95 duration-200"
                        >
                            Закрити
                        </button>
                    )}
                </div>
            </div>
        </div>
    );
}
