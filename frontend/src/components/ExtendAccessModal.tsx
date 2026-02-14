
import React, { useState } from 'react';
import { extendAccessForReview } from '../api/courses';
import { X, Upload, CheckCircle, AlertCircle } from 'lucide-react';

interface ExtendAccessModalProps {
    courseId: string;
    isOpen: boolean;
    onClose: () => void;
    onSuccess: () => void;
}

export const ExtendAccessModal: React.FC<ExtendAccessModalProps> = ({
    courseId,
    isOpen,
    onClose,
    onSuccess
}) => {
    const [file, setFile] = useState<File | null>(null);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [error, setError] = useState<string | null>(null);

    if (!isOpen) return null;

    const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        if (e.target.files && e.target.files[0]) {
            setFile(e.target.files[0]);
            setError(null);
        }
    };

    const handleSubmit = async () => {
        if (!file) {
            setError('Будь ласка, завантажте відео-відгук');
            return;
        }

        setIsSubmitting(true);
        setError(null);

        try {
            await extendAccessForReview(courseId, file);
            onSuccess();
            onClose();
        } catch (err) {
            console.error(err);
            setError('Сталася помилка під час відправки. Спробуйте пізніше.');
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm animate-in fade-in duration-200" onClick={onClose}>
            <div
                className="relative w-full max-w-2xl my-8 bg-white/90 backdrop-blur-md rounded-lg shadow-2xl overflow-hidden animate-in zoom-in-95 duration-200 max-h-[90vh] flex flex-col"
                onClick={(e) => e.stopPropagation()}
            >
                {/* Header */}
                <div className="flex items-center justify-between px-8 py-6 border-b border-gray-200/50 bg-white/30 backdrop-blur-sm">
                    <h2 className="text-xl font-bold text-gray-900">
                        Отримай +1 місяць доступу!
                    </h2>
                    <button
                        onClick={onClose}
                        className="p-2 text-gray-500 hover:text-gray-900 hover:bg-gray-100 rounded-lg transition-colors"
                    >
                        <X size={20} />
                    </button>
                </div>

                {/* Content */}
                <div className="p-8 space-y-6 overflow-y-auto">
                    <div className="space-y-4">
                        <div className="-mx-8 -mt-8 mb-6 px-8 py-4 bg-white/50 backdrop-blur-sm border-b border-gray-200/50">
                            <p className="text-gray-700">
                                Вітаю! Ти можеш отримати додатковий місяць доступу, залишивши відгук про передивлений курс.
                            </p>
                        </div>

                        {/* Steps / Instructions with nice UI */}
                        <div className="grid gap-3">
                            <div className="flex items-start gap-3 p-3 rounded-lg bg-white/50 border border-gray-200/50">
                                <div className="p-2 rounded-full bg-brand-primary/10 text-brand-primary mt-0.5 w-8 h-8 flex items-center justify-center">
                                    <span className="font-bold text-sm">1</span>
                                </div>
                                <div>
                                    <h3 className="text-gray-900 font-bold text-sm mb-1">Запиши відео-відгук</h3>
                                    <p className="text-xs text-gray-600">Розкажи про свої враження від курсу, що сподобалось, а що можна покращити.</p>
                                </div>
                            </div>
                            <div className="flex items-start gap-3 p-3 rounded-lg bg-white/50 border border-gray-200/50">
                                <div className="p-2 rounded-full bg-brand-primary/10 text-brand-primary mt-0.5 w-8 h-8 flex items-center justify-center">
                                    <span className="font-bold text-sm">2</span>
                                </div>
                                <div>
                                    <h3 className="text-gray-900 font-bold text-sm mb-1">Завантаж відео</h3>
                                    <p className="text-xs text-gray-600">Завантаж файл з відео у форму нижче.</p>
                                </div>
                            </div>
                            <div className="flex items-start gap-3 p-3 rounded-lg bg-white/50 border border-gray-200/50">
                                <div className="p-2 rounded-full bg-brand-primary/10 text-brand-primary mt-0.5 w-8 h-8 flex items-center justify-center">
                                    <span className="font-bold text-sm">3</span>
                                </div>
                                <div>
                                    <h3 className="text-gray-900 font-bold text-sm mb-1">Отримай доступ</h3>
                                    <p className="text-xs text-gray-600">Доступ буде продовжено автоматично одразу після відправки як подяка за твій час.</p>
                                </div>
                            </div>
                        </div>

                        {/* YouTube example video */}
                        <div className="aspect-video w-full bg-gray-100 rounded-lg overflow-hidden border border-gray-200/50 relative group">
                            <iframe
                                className="w-full h-full"
                                src="https://www.youtube.com/embed/dQw4w9WgXcQ"
                                title="Example Review"
                                frameBorder="0"
                                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                                allowFullScreen
                            ></iframe>
                        </div>
                        <p className="text-xs text-center text-gray-500">Приклад відео-відгуку</p>

                    </div>

                    {/* File Upload Area */}
                    <div className="space-y-2">
                        <label className="block text-sm font-bold text-gray-900">
                            Ваше відео
                        </label>
                        <div className={`
              border-2 border-dashed rounded-lg p-8 
              flex flex-col items-center justify-center text-center cursor-pointer transition-colors
              ${file ? 'border-green-500 bg-green-50' : 'border-gray-300 hover:border-brand-primary hover:bg-white/80'}
            `}>
                            <input
                                type="file"
                                accept="video/*"
                                className="hidden"
                                id="video-upload"
                                onChange={handleFileChange}
                            />
                            <label htmlFor="video-upload" className="cursor-pointer w-full flex flex-col items-center">
                                {file ? (
                                    <>
                                        <CheckCircle className="w-10 h-10 text-green-500 mb-3" />
                                        <p className="text-sm font-medium text-gray-900 break-all max-w-xs">{file.name}</p>
                                        <p className="text-xs text-gray-500 mt-1">Натисніть щоб замінити</p>
                                    </>
                                ) : (
                                    <>
                                        <Upload className="w-10 h-10 text-gray-400 mb-3" />
                                        <p className="text-sm font-medium text-gray-900">Натисніть для завантаження</p>
                                        <p className="text-xs text-gray-500 mt-1">MP4, MOV до 50MB</p>
                                    </>
                                )}
                            </label>
                        </div>
                        {error && (
                            <div className="flex items-center gap-2 text-red-600 text-sm mt-2">
                                <AlertCircle size={14} />
                                <span>{error}</span>
                            </div>
                        )}
                    </div>
                </div>

                {/* Footer */}
                <div className="flex items-center justify-end gap-3 px-8 py-6 border-t border-gray-200/50 bg-white/30 backdrop-blur-sm">
                    <button
                        onClick={onClose}
                        className="text-gray-900 font-medium hover:bg-gray-100 rounded-lg px-4 py-2 transition-colors"
                    >
                        Скасувати
                    </button>
                    <button
                        onClick={handleSubmit}
                        disabled={isSubmitting || !file}
                        className={`
              bg-brand-primary text-white font-bold rounded-lg px-6 py-3 shadow-lg hover:shadow-xl transform active:scale-95 transition-all
              ${isSubmitting || !file ? 'opacity-50 cursor-not-allowed' : 'hover:opacity-90'}
            `}
                    >
                        {isSubmitting ? 'Відправка...' : 'Відправити'}
                    </button>
                </div>
            </div>
        </div>
    );
};
