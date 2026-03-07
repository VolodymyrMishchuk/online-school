import React from 'react';
import { createPortal } from 'react-dom';
import { AlertTriangle, X } from 'lucide-react';
import { useTranslation } from 'react-i18next';

interface ConfirmModalProps {
    isOpen: boolean;
    onClose: () => void;
    onConfirm: () => void;
    title: string;
    message: string;
    warningMessage?: string;
    confirmText?: string;
    cancelText?: string;
    type?: 'danger' | 'warning' | 'info';
    isLoading?: boolean;
    isAlert?: boolean;
}

export const ConfirmModal: React.FC<ConfirmModalProps> = ({
    isOpen,
    onClose,
    onConfirm,
    title,
    message,
    warningMessage,
    confirmText,
    cancelText,
    type = 'danger',
    isLoading = false,
    isAlert = false
}) => {
    const { t } = useTranslation();
    if (!isOpen) return null;

    const getIcon = () => {
        switch (type) {
            case 'danger':
            case 'warning':
                return <AlertTriangle className={`w-6 h-6 ${type === 'danger' ? 'text-red-500' : 'text-amber-500'}`} />;
            default:
                return <AlertTriangle className="w-6 h-6 text-brand-primary" />;
        }
    };

    const getConfirmBtnClass = () => {
        switch (type) {
            case 'danger':
                return 'bg-red-600 hover:bg-red-700 text-white';
            case 'warning':
                return 'bg-amber-500 hover:bg-amber-600 text-white';
            default:
                return 'bg-brand-primary hover:bg-brand-secondary text-white';
        }
    };

    return createPortal(
        <div className="fixed inset-0 z-[10000] flex items-center justify-center p-4 bg-black/20 backdrop-blur-sm animate-in fade-in duration-200">
            <div className="glass-panel w-full max-w-md bg-white rounded-2xl shadow-2xl overflow-hidden animate-in zoom-in-95 duration-200 border border-gray-100 flex flex-col">
                {/* Header */}
                <div className="flex items-center justify-between px-6 py-4 border-b border-gray-100">
                    <div className="flex items-center gap-3">
                        <div className={`p-2 rounded-xl ${type === 'danger' ? 'bg-red-50' : type === 'warning' ? 'bg-amber-50' : 'bg-brand-light'} shrink-0`}>
                            {getIcon()}
                        </div>
                        <h3 className="text-lg font-bold text-gray-900">{title}</h3>
                    </div>
                    <button
                        onClick={onClose}
                        className="p-2 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-lg transition-colors"
                    >
                        <X className="w-5 h-5" />
                    </button>
                </div>

                {/* Body */}
                <div className="px-6 py-8 text-center flex flex-col gap-4">
                    <p className="text-gray-600 leading-relaxed">
                        {message}
                    </p>
                    {warningMessage && (
                        <p className="text-red-600 font-medium bg-red-50 p-4 rounded-xl border border-red-100 text-sm">
                            {warningMessage}
                        </p>
                    )}
                </div>

                {/* Footer */}
                <div className="flex gap-3 px-6 py-4 bg-gray-50/50 border-t border-gray-100">
                    {!isAlert && (
                        <button
                            onClick={onClose}
                            disabled={isLoading}
                            className="flex-1 py-3 px-4 text-sm font-bold text-gray-700 bg-white border border-gray-200 rounded-xl hover:bg-gray-50 transition-colors shadow-sm disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                            {cancelText || t('common.cancelBtn', 'Скасувати')}
                        </button>
                    )}
                    <button
                        onClick={() => {
                            onConfirm();
                            // Don't close immediately if loading state is meant to be shown, let caller handle closing
                            if (!isLoading) {
                                onClose();
                            }
                        }}
                        disabled={isLoading}
                        className={`flex-1 flex items-center justify-center gap-2 py-3 px-4 text-sm font-bold rounded-xl transition-all shadow-lg hover:shadow-xl transform active:scale-95 duration-200 disabled:opacity-70 disabled:transform-none disabled:cursor-not-allowed ${getConfirmBtnClass()}`}
                    >
                        {isLoading && (
                            <svg className="animate-spin -ml-1 mr-2 h-4 w-4 text-current" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                            </svg>
                        )}
                        {confirmText || (isAlert ? t('common.okBtn', 'OK') : t('common.deleteBtn', 'Видалити'))}
                    </button>
                </div>
            </div>
        </div>,
        document.body
    );
};
