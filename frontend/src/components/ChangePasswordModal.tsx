import { useState } from 'react';
import { changePassword, forgotPassword } from '../api/auth';
import { Eye, EyeOff, Lock, AlertCircle, CheckCircle, Mail } from 'lucide-react';

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
            setError('Passwords do not match');
            return;
        }

        if (newPassword.length < 6) {
            setError('New password must be at least 6 characters');
            return;
        }

        setLoading(true);
        try {
            await changePassword({ oldPassword, newPassword });
            setSuccessMessage('Password changed successfully');
            setView('success');
        } catch (err: any) {
            setError('Failed to change password. Please check your old password.');
        } finally {
            setLoading(false);
        }
    };

    const handleForgotPassword = async () => {
        setLoading(true);
        setError('');
        try {
            await forgotPassword({ email: userEmail });
            setSuccessMessage(`Reset link sent to ${userEmail}`);
            setView('success');
        } catch (err: any) {
            setError('Failed to send reset link. Please try again.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm animate-in fade-in duration-200">
            <div className="bg-white rounded-3xl shadow-2xl w-full max-w-md overflow-hidden animate-in zoom-in-95 duration-200">
                <div className="p-6 sm:p-8">
                    {/* Header */}
                    <div className="text-center mb-8">
                        <div className="inline-flex items-center justify-center w-12 h-12 rounded-full bg-brand-light mb-4 text-brand-primary">
                            {view === 'success' ? <CheckCircle className="w-6 h-6" /> : <Lock className="w-6 h-6" />}
                        </div>
                        <h2 className="text-2xl font-bold text-gray-900">
                            {view === 'change' && 'Change Password'}
                            {view === 'forgot' && 'Reset Password'}
                            {view === 'success' && 'Success!'}
                        </h2>
                        {view === 'forgot' && (
                            <p className="mt-2 text-sm text-gray-500">
                                Is this your email address? We'll send a reset link there.
                            </p>
                        )}
                    </div>

                    {/* Content */}
                    {view === 'change' && (
                        <form onSubmit={handleChangePassword} className="space-y-4">
                            {/* Old Password */}
                            <div>
                                <label className="block text-xs font-medium text-gray-700 mb-1 ml-1 uppercase tracking-wide">Old Password</label>
                                <div className="relative">
                                    <input
                                        type={showOldPass ? "text" : "password"}
                                        value={oldPassword}
                                        onChange={(e) => setOldPassword(e.target.value)}
                                        className="block w-full px-4 py-3 rounded-xl border border-gray-200 focus:border-brand-primary focus:ring-2 focus:ring-brand-light outline-none transition-all pr-10"
                                        placeholder="••••••••"
                                        required
                                    />
                                    <button
                                        type="button"
                                        onClick={() => setShowOldPass(!showOldPass)}
                                        className="absolute right-3 top-3 text-gray-400 hover:text-gray-600"
                                    >
                                        {showOldPass ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                                    </button>
                                </div>
                            </div>

                            {/* New Password */}
                            <div>
                                <label className="block text-xs font-medium text-gray-700 mb-1 ml-1 uppercase tracking-wide">New Password</label>
                                <div className="relative">
                                    <input
                                        type={showNewPass ? "text" : "password"}
                                        value={newPassword}
                                        onChange={(e) => setNewPassword(e.target.value)}
                                        className="block w-full px-4 py-3 rounded-xl border border-gray-200 focus:border-brand-primary focus:ring-2 focus:ring-brand-light outline-none transition-all pr-10"
                                        placeholder="••••••••"
                                        required
                                    />
                                    <button
                                        type="button"
                                        onClick={() => setShowNewPass(!showNewPass)}
                                        className="absolute right-3 top-3 text-gray-400 hover:text-gray-600"
                                    >
                                        {showNewPass ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                                    </button>
                                </div>
                            </div>

                            {/* Confirm Password */}
                            <div>
                                <label className="block text-xs font-medium text-gray-700 mb-1 ml-1 uppercase tracking-wide">Confirm New Password</label>
                                <div className="relative">
                                    <input
                                        type={showConfirmPass ? "text" : "password"}
                                        value={confirmPassword}
                                        onChange={(e) => setConfirmPassword(e.target.value)}
                                        className="block w-full px-4 py-3 rounded-xl border border-gray-200 focus:border-brand-primary focus:ring-2 focus:ring-brand-light outline-none transition-all pr-10"
                                        placeholder="••••••••"
                                        required
                                    />
                                    <button
                                        type="button"
                                        onClick={() => setShowConfirmPass(!showConfirmPass)}
                                        className="absolute right-3 top-3 text-gray-400 hover:text-gray-600"
                                    >
                                        {showConfirmPass ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                                    </button>
                                </div>
                            </div>

                            <div className="flex justify-end">
                                <button
                                    type="button"
                                    onClick={() => setView('forgot')}
                                    className="text-sm font-medium text-brand-primary hover:text-brand-secondary transition-colors"
                                >
                                    Forgot Password?
                                </button>
                            </div>

                            {error && (
                                <div className="flex items-center gap-2 p-3 text-sm text-red-600 bg-red-50 rounded-xl">
                                    <AlertCircle className="w-4 h-4 shrink-0" />
                                    {error}
                                </div>
                            )}

                            <div className="flex gap-3 pt-2">
                                <button
                                    type="button"
                                    onClick={handleClose}
                                    className="flex-1 py-3 font-medium text-gray-700 bg-gray-50 hover:bg-gray-100 rounded-xl transition-colors"
                                >
                                    Cancel
                                </button>
                                <button
                                    type="submit"
                                    disabled={loading}
                                    className="flex-1 py-3 font-bold text-white bg-brand-primary hover:bg-brand-secondary rounded-xl transition-colors disabled:opacity-70"
                                >
                                    {loading ? 'Saving...' : 'Change Password'}
                                </button>
                            </div>
                        </form>
                    )}

                    {view === 'forgot' && (
                        <div className="space-y-6">
                            <div className="flex items-center gap-3 p-4 bg-blue-50 text-blue-800 rounded-xl">
                                <Mail className="w-6 h-6 shrink-0" />
                                <span className="font-medium text-lg truncate">{userEmail}</span>
                            </div>

                            {error && (
                                <div className="flex items-center gap-2 p-3 text-sm text-red-600 bg-red-50 rounded-xl">
                                    <AlertCircle className="w-4 h-4 shrink-0" />
                                    {error}
                                </div>
                            )}

                            <div className="flex gap-3">
                                <button
                                    type="button"
                                    onClick={() => setView('change')}
                                    className="flex-1 py-3 font-medium text-red-600 bg-red-50 hover:bg-red-100 rounded-xl transition-colors"
                                >
                                    Not my email
                                </button>
                                <button
                                    type="button"
                                    onClick={handleForgotPassword}
                                    disabled={loading}
                                    className="flex-1 py-3 font-bold text-white bg-brand-primary hover:bg-brand-secondary rounded-xl transition-colors disabled:opacity-70"
                                >
                                    {loading ? 'Sending...' : 'Yes, send link'}
                                </button>
                            </div>
                        </div>
                    )}

                    {view === 'success' && (
                        <div className="text-center space-y-6">
                            <p className="text-gray-600">{successMessage}</p>
                            <button
                                onClick={handleClose}
                                className="w-full py-3 font-bold text-white bg-brand-primary hover:bg-brand-secondary rounded-xl transition-colors"
                            >
                                Close
                            </button>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}
