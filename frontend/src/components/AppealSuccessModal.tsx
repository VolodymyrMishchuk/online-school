import React from 'react';
import { CheckCircle, X } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

interface AppealSuccessModalProps {
    isOpen: boolean;
    onClose: () => void;
}

export const AppealSuccessModal: React.FC<AppealSuccessModalProps> = ({ isOpen, onClose }) => {
    const navigate = useNavigate();

    if (!isOpen) return null;

    const handleAcknowledge = () => {
        onClose();
        navigate('/dashboard/all-courses');
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-white/30 backdrop-blur-md animate-in fade-in duration-200">
            {/* Glass Panel Modal */}
            <div
                className="glass-panel w-full max-w-md flex flex-col overflow-hidden animate-in zoom-in-95 duration-200 relative rounded-lg shadow-xl"
                style={{ background: 'rgba(255, 255, 255, 0.9)', maxHeight: '90vh' }}
            >
                {/* Header Bar */}
                <div className="flex items-center justify-between px-6 py-5 border-b border-gray-100 bg-white shrink-0 z-10 relative shadow-sm">
                    <div className="flex items-center gap-3">
                        <div className="flex items-center justify-center w-10 h-10 rounded-full bg-brand-light/50 text-brand-primary ring-2 ring-white shadow-sm">
                            <CheckCircle className="w-5 h-5 text-green-500" />
                        </div>
                        <h2 className="text-xl font-bold text-brand-dark">Успіх!</h2>
                    </div>
                    <button
                        onClick={handleAcknowledge}
                        className="p-2 text-gray-400 hover:text-brand-primary hover:bg-white/50 rounded-lg transition-all"
                    >
                        <X className="w-5 h-5" />
                    </button>
                </div>

                {/* Body */}
                <div className="flex-1 px-8 py-8 flex flex-col items-center justify-center text-center gap-4">
                    <div className="w-16 h-16 bg-green-50 rounded-full flex items-center justify-center mb-2">
                        <CheckCircle className="w-8 h-8 text-green-500" />
                    </div>
                    <p className="text-gray-700 font-medium text-lg">
                        Ваше звернення успішно відправлено!
                    </p>
                    <p className="text-sm text-gray-500">
                        Ми зв'яжемося з Вами найближчим часом за вказаними контактами.
                    </p>
                </div>

                {/* Footer */}
                <div className="flex gap-4 p-6 border-t border-gray-100 bg-white/50 backdrop-blur-sm shrink-0">
                    <button
                        onClick={handleAcknowledge}
                        className="w-full py-3 font-bold text-white bg-brand-primary hover:bg-brand-secondary rounded-lg transition-all shadow-lg hover:shadow-xl transform active:scale-95 duration-200"
                    >
                        Зрозуміло
                    </button>
                </div>
            </div>
        </div>
    );
};
