import React from 'react';
import { createPortal } from 'react-dom';
import { ShieldAlert } from 'lucide-react';

interface FakeAdminRestrictionModalProps {
    isOpen: boolean;
    onClose: () => void;
}

export const FakeAdminRestrictionModal: React.FC<FakeAdminRestrictionModalProps> = ({
    isOpen,
    onClose
}) => {
    if (!isOpen) return null;

    return createPortal(
        <div className="fixed inset-0 bg-white/40 backdrop-blur-sm flex items-center justify-center z-[10000] p-4">
            <div className="glass-panel rounded-2xl shadow-xl max-w-md w-full animate-in zoom-in-95 duration-200 overflow-hidden flex flex-col bg-white border border-gray-100">
                {/* Header */}
                <div className="flex justify-between items-center p-4 border-b border-gray-100 bg-white shrink-0 shadow-sm">
                    <div className="flex items-center gap-2 text-red-500">
                        <ShieldAlert className="w-5 h-5" />
                        <h3 className="text-xl font-bold text-brand-dark">Обмеження доступу</h3>
                    </div>
                </div>

                {/* Body */}
                <div className="p-6 text-gray-700 flex-1 overflow-y-auto custom-scrollbar">
                    <p className="text-center text-lg">
                        Вибачте, але демо-адміністратор може редагувати та видаляти лише ті об'єкти, які створив сам.
                    </p>
                </div>

                {/* Footer */}
                <div className="p-4 border-t border-gray-100 bg-white/50 backdrop-blur-sm shrink-0 flex justify-center">
                    <button
                        onClick={onClose}
                        className="px-8 py-3 bg-brand-primary text-white font-bold rounded-lg hover:bg-brand-secondary transition-colors shadow-lg hover:shadow-xl transform active:scale-95 duration-200"
                    >
                        Зрозумів
                    </button>
                </div>
            </div>
        </div>,
        document.body
    );
};
