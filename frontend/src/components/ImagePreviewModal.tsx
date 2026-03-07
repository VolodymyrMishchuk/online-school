import { X } from 'lucide-react';
import { useTranslation } from 'react-i18next';

interface ImagePreviewModalProps {
    isOpen: boolean;
    imageUrl: string;
    onClose: () => void;
}

export default function ImagePreviewModal({ isOpen, imageUrl, onClose }: ImagePreviewModalProps) {
    const { t } = useTranslation();
    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm p-4 animate-fadeIn" onClick={onClose}>
            <button
                onClick={onClose}
                className="absolute top-4 right-4 p-2 text-white/70 hover:text-white bg-black/20 hover:bg-black/40 rounded-full transition-colors"
                aria-label={t('common.closeBtn', 'Закрити')}
            >
                <X className="w-8 h-8" />
            </button>
            <div className="relative max-w-5xl max-h-[90vh] w-full flex items-center justify-center p-2" onClick={e => e.stopPropagation()}>
                <img
                    src={imageUrl}
                    alt={t('imagePreview.alt', 'Попередній перегляд')}
                    className="max-w-full max-h-[85vh] object-contain rounded-lg shadow-2xl"
                />
            </div>
        </div>
    );
}
