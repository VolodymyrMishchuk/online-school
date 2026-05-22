import type { CourseDto, CreateCourseDto } from '../api/courses';
import type { Module } from '../api/modules';
import { X, BookOpen, Layers, CheckCircle, Euro, Percent, Clock, Tag, ArrowRight, FileText, Settings2 } from 'lucide-react';
import React, { useEffect, useState } from 'react';
import { API_URL } from '../api/client';
import { useTranslation } from 'react-i18next';

interface CourseModalProps {
    isOpen: boolean;
    onClose: () => void;
    onSubmit: (data: CreateCourseDto, file?: File) => Promise<void> | void;
    modules: Module[];
    courses: CourseDto[]; // All available courses for next course selection
    initialData?: CourseDto;
    initialModuleIds?: string[]; // IDs of modules assigned to this course (for editing)
}

export const CourseModal: React.FC<CourseModalProps> = ({
    isOpen,
    onClose,
    onSubmit,
    modules,
    courses,
    initialData,
    initialModuleIds = []
}) => {
    const { t } = useTranslation();
    const [name, setName] = useState('');
    const [description, setDescription] = useState('');
    const [price, setPrice] = useState<number | undefined>(undefined);
    const [discountAmount, setDiscountAmount] = useState<number | undefined>(undefined);
    const [discountPercentage, setDiscountPercentage] = useState<number | undefined>(undefined);
    const [accessDuration, setAccessDuration] = useState<number | undefined>(undefined);
    const [promotionalDiscountPercentage, setPromotionalDiscountPercentage] = useState<number | undefined>(undefined);
    const [promotionalDiscountAmount, setPromotionalDiscountAmount] = useState<number | undefined>(undefined);
    const [renewalDiscountPercentage, setRenewalDiscountPercentage] = useState<number | undefined>(undefined);
    const [renewalDiscountAmount, setRenewalDiscountAmount] = useState<number | undefined>(undefined);
    const [extendForReviewEnabled, setExtendForReviewEnabled] = useState(true);
    const [renewalEnabled, setRenewalEnabled] = useState(true);
    const [nextCourseDiscountEnabled, setNextCourseDiscountEnabled] = useState(true);
    const [nextCourseId, setNextCourseId] = useState<string | undefined>(undefined);
    const [selectedModuleIds, setSelectedModuleIds] = useState<string[]>([]);
    const [file, setFile] = useState<File | undefined>(undefined);
    const [previewUrl, setPreviewUrl] = useState<string | undefined>(undefined);
    const [deleteCover, setDeleteCover] = useState(false);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        if (initialData) {
            // ... existing setters ...
            setName(initialData.name);
            setDescription(initialData.description);
            setPrice(initialData.price);
            setDiscountAmount(initialData.discountAmount);
            setDiscountPercentage(initialData.discountPercentage);
            setAccessDuration(initialData.accessDuration);
            setPromotionalDiscountPercentage(initialData.promotionalDiscountPercentage);
            setPromotionalDiscountAmount(initialData.promotionalDiscountAmount);
            setRenewalDiscountPercentage(initialData.renewalDiscountPercentage);
            setRenewalDiscountAmount(initialData.renewalDiscountAmount);
            setExtendForReviewEnabled(initialData.extendForReviewEnabled !== false);
            setRenewalEnabled(initialData.renewalEnabled !== false);
            setNextCourseDiscountEnabled(initialData.nextCourseDiscountEnabled !== false);
            setNextCourseId(initialData.nextCourseId);
            setSelectedModuleIds(initialModuleIds);
            setPreviewUrl(initialData.coverImageUrl ? `${API_URL}${initialData.coverImageUrl}` : undefined);
            setDeleteCover(false);
        } else {
            // ... existing resets ...
            setName('');
            setDescription('');
            setPrice(undefined);
            setDiscountAmount(undefined);
            setDiscountPercentage(undefined);
            setAccessDuration(undefined);
            setPromotionalDiscountPercentage(undefined);
            setPromotionalDiscountAmount(undefined);
            setRenewalDiscountPercentage(undefined);
            setRenewalDiscountAmount(undefined);
            setExtendForReviewEnabled(true);
            setRenewalEnabled(true);
            setNextCourseDiscountEnabled(true);
            setNextCourseId(undefined);
            setSelectedModuleIds([]);
            setFile(undefined);
            setPreviewUrl(undefined);
            setDeleteCover(false);
        }
    }, [initialData, isOpen, initialModuleIds]);

    const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const selectedFile = e.target.files?.[0];
        if (selectedFile) {
            setFile(selectedFile);
            setPreviewUrl(URL.createObjectURL(selectedFile));
            setDeleteCover(false);
        }
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError(null);

        const currentPrice = price || 0;
        
        if (discountAmount && discountAmount > currentPrice) {
            setError(t('courseModal.errorDiscountAmount', 'Знижка не може бути більшою за ціну курсу'));
            return;
        }
        if (renewalDiscountAmount && renewalDiscountAmount > currentPrice) {
            setError(t('courseModal.errorRenewalDiscount', 'Знижка на продовження не може бути більшою за ціну курсу'));
            return;
        }
        
        if (promotionalDiscountAmount && nextCourseId) {
            const nextCourse = courses.find(c => c.id === nextCourseId);
            if (nextCourse && nextCourse.price !== undefined && promotionalDiscountAmount > nextCourse.price) {
                 setError(t('courseModal.errorPromotionalDiscount', 'Акційна знижка не може бути більшою за ціну наступного курсу'));
                 return;
            }
        }

        if ((discountPercentage && discountPercentage > 100) || 
            (renewalDiscountPercentage && renewalDiscountPercentage > 100) || 
            (promotionalDiscountPercentage && promotionalDiscountPercentage > 100)) {
            setError(t('courseModal.errorPercentage', 'Відсоток знижки не може перевищувати 100%'));
            return;
        }

        setIsSubmitting(true);
        try {
            await onSubmit({
                name,
                description,
                price,
                discountAmount,
                discountPercentage,
                accessDuration,
                promotionalDiscountPercentage,
                promotionalDiscountAmount,
                renewalDiscountPercentage,
                renewalDiscountAmount,
                extendForReviewEnabled,
                renewalEnabled,
                nextCourseDiscountEnabled,
                nextCourseId,
                moduleIds: selectedModuleIds,
                deleteCoverImage: deleteCover // Pass the flag
            } as any, file); // Cast to any or intersection to avoid TS error
            onClose();
        } finally {
            setIsSubmitting(false);
        }
    };

    const toggleModule = (moduleId: string) => {
        setSelectedModuleIds(prev =>
            prev.includes(moduleId)
                ? prev.filter(id => id !== moduleId)
                : [...prev, moduleId]
        );
    };

    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-white/30 backdrop-blur-md animate-in fade-in duration-200">
            {/* Glass Panel Modal */}
            <div
                className="glass-panel w-full max-w-2xl flex flex-col overflow-hidden animate-in zoom-in-95 duration-200 relative rounded-lg shadow-xl"
                style={{ background: 'rgba(255, 255, 255, 0.9)', maxHeight: '90vh' }}
            >
                {/* Header Bar - Static */}
                <div className="flex items-center justify-between px-6 py-5 border-b border-gray-100 bg-white shrink-0 z-10 relative shadow-sm">
                    <div className="flex items-center gap-3">
                        <div className="flex items-center justify-center w-10 h-10 rounded-full bg-brand-light/50 text-brand-primary ring-2 ring-white shadow-sm">
                            <BookOpen className="w-5 h-5" />
                        </div>
                        <h2 className="text-xl font-bold text-brand-dark">
                            {initialData ? t('courseModal.editCourse', 'Редагувати курс') : t('courseModal.createNewCourse', 'Створити новий курс')}
                        </h2>
                    </div>
                    <button
                        onClick={onClose}
                        className="p-2 text-gray-400 hover:text-brand-primary hover:bg-gray-100 rounded-lg transition-all"
                    >
                        <X className="w-5 h-5" />
                    </button>
                </div>

                {/* Body - Scrollable */}
                <div className="flex-1 overflow-y-auto px-8 py-6 custom-scrollbar flex flex-col gap-6">
                    <form id="course-form" onSubmit={handleSubmit} className="space-y-6">
                        {/* Cover Image */}
                        <div>
                            <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2 ml-1">
                                <FileText className="w-4 h-4" />
                                {t('courseModal.courseCover', 'Обкладинка курсу')}
                            </label>
                            <div className="flex flex-col gap-3 w-full">
                                <div className="flex items-start gap-4">
                                    {previewUrl && (
                                        <div className="w-32 h-20 rounded-lg overflow-hidden border border-gray-200 shrink-0 relative group">
                                            <img src={previewUrl} alt={t('courseModal.coverPreviewAlt', 'Превʼю обкладинки')} className="w-full h-full object-cover" />
                                        </div>
                                    )}
                                    <div className="flex flex-col gap-2">
                                        <input
                                            type="file"
                                            accept="image/*"
                                            onChange={handleFileChange}
                                            className="hidden"
                                            id="cover-upload"
                                        />
                                        <label
                                            htmlFor="cover-upload"
                                            className="px-4 py-2 bg-brand-light/50 text-brand-primary hover:bg-brand-light/80 font-semibold rounded-lg cursor-pointer transition-colors text-sm text-center inline-block"
                                        >
                                            {t('courseModal.chooseFileBtn', 'Обрати файл')}
                                        </label>

                                        {previewUrl && (
                                            <button
                                                type="button"
                                                onClick={() => {
                                                    setFile(undefined);
                                                    setPreviewUrl(undefined);
                                                    setDeleteCover(true);
                                                    // Reset file input value to allow selecting same file again
                                                    const fileInput = document.getElementById('cover-upload') as HTMLInputElement;
                                                    if (fileInput) fileInput.value = '';
                                                }}
                                                className="px-4 py-2 bg-white border border-red-500 text-red-500 hover:bg-red-50 font-bold rounded-lg transition-all shadow-sm hover:shadow-md transform active:scale-95 duration-200 text-sm"
                                            >
                                                {t('courseModal.deleteCoverBtn', 'Видалити обкладинку')}
                                            </button>
                                        )}
                                    </div>
                                </div>
                                {!previewUrl && <span className="text-sm text-gray-500">{t('courseModal.noFileChosen', 'Файл не обрано')}</span>}
                            </div>
                        </div>

                        {/* Course Name */}
                        <div>
                            <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2 ml-1">
                                <BookOpen className="w-4 h-4" />
                                {t('courseModal.courseName', 'Назва курсу *')}
                            </label>
                            <input
                                type="text"
                                value={name}
                                onChange={(e) => setName(e.target.value)}
                                className="w-full px-4 py-3 rounded-lg border border-gray-200 bg-white/50 outline-none transition-all focus:border-brand-primary focus:ring-2 focus:ring-brand-light focus:bg-white"
                                placeholder={t('courseModal.courseNamePlaceholder', 'Введіть назву курсу')}
                                required
                            />
                        </div>

                        {/* Course Description */}
                        <div>
                            <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2 ml-1">
                                <FileText className="w-4 h-4" />
                                {t('courseModal.description', 'Опис *')}
                            </label>
                            <textarea
                                value={description}
                                onChange={(e) => setDescription(e.target.value)}
                                className="w-full px-4 py-3 rounded-lg border border-gray-200 bg-white/50 outline-none transition-all focus:border-brand-primary focus:ring-2 focus:ring-brand-light focus:bg-white resize-none"
                                placeholder={t('courseModal.descriptionPlaceholder', 'Введіть опис курсу')}
                                rows={3}
                                required
                            />
                        </div>

                        {/* Price and Discounts */}
                        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                            {/* Price */}
                            <div>
                                <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2 ml-1">
                                    <Euro className="w-4 h-4" />
                                    {t('courseModal.price', 'Ціна (€)')}
                                </label>
                                <div className="relative">
                                    <input
                                        type="number"
                                        min="0"
                                        step="0.01"
                                        value={price || ''}
                                        onChange={(e) => setPrice(e.target.value ? parseFloat(e.target.value) : undefined)}
                                        className="w-full px-4 py-3 rounded-lg border border-gray-200 bg-white/50 outline-none transition-all focus:border-brand-primary focus:ring-2 focus:ring-brand-light focus:bg-white [appearance:textfield] [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none"
                                        placeholder="0.00"
                                    />
                                    <span className="absolute right-4 top-3.5 text-gray-400 font-medium">€</span>
                                </div>
                                {price && (discountAmount || discountPercentage) ? (
                                    <div className="mt-2 text-sm font-medium bg-green-50 text-green-700 px-3 py-1.5 rounded-lg border border-green-100 inline-block w-full text-center">
                                        {t('courseModal.newPrice', 'Нова ціна:')} {(discountAmount
                                            ? (price - discountAmount)
                                            : (price * (1 - (discountPercentage || 0) / 100))
                                        ).toFixed(2)}€
                                    </div>
                                ) : null}
                            </div>

                            {/* Discount Fixed */}
                            <div>
                                <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2 ml-1">
                                    <Tag className="w-4 h-4" />
                                    {t('courseModal.discountAmount', 'Знижка (Сума)')}
                                </label>
                                <div className="relative">
                                    <input
                                        type="number"
                                        min="0"
                                        step="0.01"
                                        value={discountAmount || ''}
                                        onChange={(e) => {
                                            const val = e.target.value ? parseFloat(e.target.value) : undefined;
                                            setDiscountAmount(val);
                                            if (val && val > 0) setDiscountPercentage(undefined);
                                        }}
                                        className="w-full px-4 py-3 rounded-lg border border-gray-200 bg-white/50 outline-none transition-all focus:border-brand-primary focus:ring-2 focus:ring-brand-light focus:bg-white [appearance:textfield] [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none"
                                        placeholder="0.00"
                                    />
                                    <span className="absolute right-4 top-3.5 text-gray-400 font-medium">€</span>
                                </div>
                            </div>

                            {/* Discount Percentage */}
                            <div>
                                <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2 ml-1">
                                    <Percent className="w-4 h-4" />
                                    {t('courseModal.discountPercentage', 'Знижка (%)')}
                                </label>
                                <div className="relative">
                                    <input
                                        type="number"
                                        min="0"
                                        max="100"
                                        value={discountPercentage || ''}
                                        onChange={(e) => {
                                            const val = e.target.value ? parseInt(e.target.value) : undefined;
                                            setDiscountPercentage(val);
                                            if (val && val > 0) setDiscountAmount(undefined);
                                        }}
                                        className="w-full px-4 py-3 rounded-lg border border-gray-200 bg-white/50 outline-none transition-all focus:border-brand-primary focus:ring-2 focus:ring-brand-light focus:bg-white [appearance:textfield] [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none"
                                        placeholder="0"
                                    />
                                    <span className="absolute right-4 top-3.5 text-gray-400 font-medium">%</span>
                                </div>
                            </div>
                        </div>

                        {/* Access Duration */}
                        <div>
                            <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2 ml-1">
                                <Clock className="w-4 h-4" />
                                {t('courseModal.accessDuration', 'Тривалість доступу (дні)')}
                            </label>
                            <input
                                type="number"
                                min="1"
                                value={accessDuration || ''}
                                onChange={(e) => setAccessDuration(e.target.value ? parseInt(e.target.value) : undefined)}
                                className="w-full px-4 py-3 rounded-lg border border-gray-200 bg-white/50 outline-none transition-all focus:border-brand-primary focus:ring-2 focus:ring-brand-light focus:bg-white"
                                placeholder={t('courseModal.accessDurationPlaceholder', 'Наприклад: 30')}
                            />
                            <p className="text-xs text-gray-500 mt-1 ml-1">
                                {t('courseModal.unlimitedAccessHint', 'Залиште порожнім для необмеженого доступу')}
                            </p>
                        </div>

                        {/* Blocked Course Feature Flags */}
                        <div className="border border-brand-primary/20 rounded-xl p-5 bg-brand-primary/3 space-y-3">
                            {/* Section header */}
                            <p className="text-sm font-semibold text-gray-700 flex items-center gap-2">
                                <Settings2 className="w-4 h-4 text-brand-primary" />
                                {t('courseModal.blockedCourseOptions', 'Опції для картки з закінченим доступом')}
                            </p>

                            {/* 1. Extend for review — simple toggle only */}
                            <div className={`rounded-lg border transition-all ${extendForReviewEnabled ? 'border-brand-primary/40 bg-white shadow-sm' : 'border-gray-200 bg-gray-100/50 opacity-60'}`}>
                                <label className="flex items-center gap-3 p-3 cursor-pointer">
                                    <div className="relative flex items-center shrink-0">
                                        <input type="checkbox" checked={extendForReviewEnabled}
                                            onChange={(e) => setExtendForReviewEnabled(e.target.checked)}
                                            className="peer h-5 w-5 cursor-pointer appearance-none rounded-md border border-gray-300 transition-all checked:border-brand-primary checked:bg-brand-primary" />
                                        <CheckCircle className="absolute pointer-events-none opacity-0 peer-checked:opacity-100 text-white w-3.5 h-3.5 left-[3px] top-[3px]" />
                                    </div>
                                    <div>
                                        <span className="text-sm font-medium text-gray-700">{t('courseModal.extendForReviewEnabled', 'Отримати доступ за відеовідгук')}</span>
                                        <p className="text-xs text-gray-400">{t('courseModal.extendForReviewHint', 'Кнопка продовження доступу через відеовідгук')}</p>
                                    </div>
                                </label>
                            </div>

                            {/* 2. Renewal — toggle + renewal discount fields */}
                            <div className={`rounded-lg border transition-all ${renewalEnabled ? 'border-brand-primary/40 bg-white shadow-sm' : 'border-gray-200 bg-gray-100/50 opacity-60'}`}>
                                <label className="flex items-center gap-3 p-3 cursor-pointer">
                                    <div className="relative flex items-center shrink-0">
                                        <input type="checkbox" checked={renewalEnabled}
                                            onChange={(e) => setRenewalEnabled(e.target.checked)}
                                            className="peer h-5 w-5 cursor-pointer appearance-none rounded-md border border-gray-300 transition-all checked:border-brand-primary checked:bg-brand-primary" />
                                        <CheckCircle className="absolute pointer-events-none opacity-0 peer-checked:opacity-100 text-white w-3.5 h-3.5 left-[3px] top-[3px]" />
                                    </div>
                                    <div>
                                        <span className="text-sm font-medium text-gray-700">{t('courseModal.renewalEnabled', 'Продовжити курс зі знижкою')}</span>
                                        <p className="text-xs text-gray-400">{t('courseModal.renewalHint', 'Кнопка повторного придбання з актуальною знижкою')}</p>
                                    </div>
                                </label>
                                {/* Renewal discount fields */}
                                <div className="px-3 pb-3 border-t border-gray-100 pt-2">
                                    <p className="text-xs font-medium text-gray-500 mb-2 flex items-center gap-1">
                                        <Tag className="w-3 h-3" />
                                        {t('courseModal.renewalDiscount', 'Знижка на продовження доступу')}
                                    </p>
                                    <div className="grid grid-cols-2 gap-2">
                                        <div className="relative">
                                            <input type="number" min="0" max="100"
                                                value={renewalDiscountPercentage || ''}
                                                onChange={(e) => { const val = e.target.value ? parseInt(e.target.value) : undefined; setRenewalDiscountPercentage(val); if (val && val > 0) setRenewalDiscountAmount(undefined); }}
                                                className="w-full px-3 py-2 text-sm rounded-lg border border-gray-200 bg-gray-50 outline-none transition-all focus:border-brand-primary focus:ring-1 focus:ring-brand-light focus:bg-white [appearance:textfield] [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none"
                                                placeholder="%" />
                                            <span className="absolute right-3 top-2 text-gray-400 text-sm">%</span>
                                        </div>
                                        <div className="relative">
                                            <input type="number" min="0" step="0.01"
                                                value={renewalDiscountAmount || ''}
                                                onChange={(e) => { const val = e.target.value ? parseFloat(e.target.value) : undefined; setRenewalDiscountAmount(val); if (val && val > 0) setRenewalDiscountPercentage(undefined); }}
                                                className="w-full px-3 py-2 text-sm rounded-lg border border-gray-200 bg-gray-50 outline-none transition-all focus:border-brand-primary focus:ring-1 focus:ring-brand-light focus:bg-white [appearance:textfield] [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none"
                                                placeholder="€" />
                                            <span className="absolute right-3 top-2 text-gray-400 text-sm">€</span>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            {/* 3. Next course discount — toggle + promotional discount + next course selector */}
                            <div className={`rounded-lg border transition-all ${nextCourseDiscountEnabled ? 'border-brand-primary/40 bg-white shadow-sm' : 'border-gray-200 bg-gray-100/50 opacity-60'}`}>
                                <label className="flex items-center gap-3 p-3 cursor-pointer">
                                    <div className="relative flex items-center shrink-0">
                                        <input type="checkbox" checked={nextCourseDiscountEnabled}
                                            onChange={(e) => setNextCourseDiscountEnabled(e.target.checked)}
                                            className="peer h-5 w-5 cursor-pointer appearance-none rounded-md border border-gray-300 transition-all checked:border-brand-primary checked:bg-brand-primary" />
                                        <CheckCircle className="absolute pointer-events-none opacity-0 peer-checked:opacity-100 text-white w-3.5 h-3.5 left-[3px] top-[3px]" />
                                    </div>
                                    <div>
                                        <span className="text-sm font-medium text-gray-700">{t('courseModal.nextCourseDiscountEnabled', 'Отримати знижку на наступний курс')}</span>
                                        <p className="text-xs text-gray-400">{t('courseModal.nextCourseDiscountHint', 'Кнопка переходу до наступного рекомендованого курсу')}</p>
                                    </div>
                                </label>
                                <div className="px-3 pb-3 border-t border-gray-100 pt-2 space-y-3">
                                    <div>
                                        <p className="text-xs font-medium text-gray-500 mb-2 flex items-center gap-1">
                                            <Percent className="w-3 h-3" />
                                            {t('courseModal.promotionalDiscount', 'Акційна знижка на наступний курс')}
                                        </p>
                                        <div className="grid grid-cols-2 gap-2">
                                            <div className="relative">
                                                <input type="number" min="0" max="100"
                                                    value={promotionalDiscountPercentage || ''}
                                                    onChange={(e) => { const val = e.target.value ? parseInt(e.target.value) : undefined; setPromotionalDiscountPercentage(val); if (val && val > 0) setPromotionalDiscountAmount(undefined); }}
                                                    className="w-full px-3 py-2 text-sm rounded-lg border border-gray-200 bg-gray-50 outline-none transition-all focus:border-brand-primary focus:ring-1 focus:ring-brand-light focus:bg-white [appearance:textfield] [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none"
                                                    placeholder="%" />
                                                <span className="absolute right-3 top-2 text-gray-400 text-sm">%</span>
                                            </div>
                                            <div className="relative">
                                                <input type="number" min="0" step="0.01"
                                                    value={promotionalDiscountAmount || ''}
                                                    onChange={(e) => { const val = e.target.value ? parseFloat(e.target.value) : undefined; setPromotionalDiscountAmount(val); if (val && val > 0) setPromotionalDiscountPercentage(undefined); }}
                                                    className="w-full px-3 py-2 text-sm rounded-lg border border-gray-200 bg-gray-50 outline-none transition-all focus:border-brand-primary focus:ring-1 focus:ring-brand-light focus:bg-white [appearance:textfield] [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none"
                                                    placeholder="€" />
                                                <span className="absolute right-3 top-2 text-gray-400 text-sm">€</span>
                                            </div>
                                        </div>
                                    </div>
                                    <div>
                                        <p className="text-xs font-medium text-gray-500 mb-2 flex items-center gap-1">
                                            <ArrowRight className="w-3 h-3" />
                                            {t('courseModal.nextCourse', 'Наступний рекомендований курс')}
                                        </p>
                                        <div className="relative">
                                            <select value={nextCourseId || ''} onChange={(e) => setNextCourseId(e.target.value || undefined)}
                                                className="w-full px-3 py-2 text-sm rounded-lg border border-gray-200 bg-gray-50 outline-none transition-all focus:border-brand-primary focus:ring-1 focus:ring-brand-light focus:bg-white appearance-none">
                                                <option value="">{t('courseModal.notSpecified', 'Не вказано')}</option>
                                                {courses.filter(c => c.id !== initialData?.id).map(c => (
                                                    <option key={c.id} value={c.id}>{c.name}</option>
                                                ))}
                                            </select>
                                            <div className="absolute inset-y-0 right-0 flex items-center px-3 pointer-events-none text-gray-400">
                                                <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 9l-7 7-7-7" />
                                                </svg>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>

                        {/* Module Selection */}
                        <div>
                            <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2 ml-1">
                                <Layers className="w-4 h-4" />
                                {t('courseModal.selectModules', 'Вибрати модулі (Опціонально)')}
                            </label>
                            <div className="border border-gray-200 rounded-lg p-4 max-h-64 overflow-y-auto bg-white/50 custom-scrollbar">
                                {modules
                                    .filter(module => !module.courseId || (initialData && module.courseId === initialData.id))
                                    .length === 0 ? (
                                    <div className="p-4 text-center text-gray-500 text-sm italic">
                                        {t('courseModal.noModulesFound', 'Доступних модулів не знайдено')}
                                    </div>
                                ) : (
                                    <div className="space-y-2">
                                        {modules
                                            .filter(module => !module.courseId || (initialData && module.courseId === initialData.id))
                                            .map((module) => (
                                                <label
                                                    key={module.id}
                                                    className="flex items-start p-3 hover:bg-white rounded-lg cursor-pointer transition-colors group border border-transparent hover:border-gray-100"
                                                >
                                                    <div className="relative flex items-center mt-0.5">
                                                        <input
                                                            type="checkbox"
                                                            checked={selectedModuleIds.includes(module.id)}
                                                            onChange={() => toggleModule(module.id)}
                                                            className="peer h-5 w-5 cursor-pointer appearance-none rounded-md border border-gray-300 transition-all checked:border-brand-primary checked:bg-brand-primary group-hover:border-brand-primary"
                                                        />
                                                        <CheckCircle className="absolute pointer-events-none opacity-0 peer-checked:opacity-100 text-white w-3.5 h-3.5 left-[3px] top-[3px]" />
                                                    </div>
                                                    <div className="ml-3 flex-1">
                                                        <div className="font-bold text-gray-700 group-hover:text-brand-dark transition-colors">
                                                            {module.name}
                                                        </div>
                                                        {module.description && (
                                                            <div className="text-sm text-gray-500 mt-0.5 line-clamp-1">
                                                                {module.description}
                                                            </div>
                                                        )}
                                                        {module.courseName && module.courseName !== (initialData?.name || '') && (
                                                            <div className="text-xs text-orange-500 mt-1 font-medium bg-orange-50 inline-block px-2 py-0.5 rounded border border-orange-100">
                                                                {t('courseModal.currentCourse', 'Поточний курс:')} {module.courseName} {t('courseModal.willBeChanged', '(буде змінено)')}
                                                            </div>
                                                        )}
                                                    </div>
                                                </label>
                                            ))}
                                    </div>
                                )}
                            </div>
                            <div className="text-xs font-medium text-gray-500 text-right pt-2 border-t border-gray-200/50 mt-2">
                                {t('courseModal.selectedModulesCount', 'Вибрано:')} <span className="text-brand-primary font-bold">{selectedModuleIds.length}</span>
                            </div>
                        </div>
                    </form>
                </div>

                {error && (
                    <div className="px-6 pb-2 bg-white/50 backdrop-blur-sm">
                        <div className="p-3 bg-red-50 text-red-500 text-sm rounded-lg border border-red-100 flex items-start gap-2">
                            <svg className="w-5 h-5 shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                            </svg>
                            <span>{error}</span>
                        </div>
                    </div>
                )}
                {/* Footer - Static */}
                <div className="flex gap-4 p-6 border-t border-gray-100 bg-white/50 backdrop-blur-sm shrink-0">
                    <button
                        type="button"
                        onClick={onClose}
                        className="flex-1 py-3 font-bold text-brand-primary bg-white hover:bg-brand-primary hover:text-white rounded-lg transition-colors shadow-sm border border-gray-100"
                    >
                        {t('common.cancelBtn', 'Скасувати')}
                    </button>
                    <button
                        type="submit"
                        form="course-form"
                        disabled={isSubmitting}
                        className="flex-1 py-3 font-bold text-white bg-brand-primary hover:bg-brand-secondary rounded-lg transition-all shadow-lg hover:shadow-xl transform active:scale-95 duration-200 disabled:opacity-70 disabled:transform-none flex items-center justify-center gap-2"
                    >
                        {isSubmitting ? (
                            <>
                                <div className="animate-spin rounded-full h-4 w-4 border-2 border-white/30 border-t-white"></div>
                                <span>{t('common.saving', 'Збереження...')}</span>
                            </>
                        ) : (
                            initialData ? t('common.update', 'Оновити') : t('common.create', 'Створити')
                        )}
                    </button>
                </div>
            </div>
        </div>
    );
};
