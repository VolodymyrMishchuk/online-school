import type { CourseDto, CreateCourseDto } from '../api/courses';
import type { Module } from '../api/modules';
import { X, BookOpen, Layers, CheckCircle, Euro, Percent, Clock, Tag, ArrowRight, FileText } from 'lucide-react';
import React, { useEffect, useState } from 'react';

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
    const [name, setName] = useState('');
    const [description, setDescription] = useState('');
    const [price, setPrice] = useState<number | undefined>(undefined);
    const [discountAmount, setDiscountAmount] = useState<number | undefined>(undefined);
    const [discountPercentage, setDiscountPercentage] = useState<number | undefined>(undefined);
    const [accessDuration, setAccessDuration] = useState<number | undefined>(undefined);
    const [promotionalDiscountPercentage, setPromotionalDiscountPercentage] = useState<number | undefined>(undefined);
    const [promotionalDiscountAmount, setPromotionalDiscountAmount] = useState<number | undefined>(undefined);
    const [nextCourseId, setNextCourseId] = useState<string | undefined>(undefined);
    const [selectedModuleIds, setSelectedModuleIds] = useState<string[]>([]);
    const [file, setFile] = useState<File | undefined>(undefined);
    const [previewUrl, setPreviewUrl] = useState<string | undefined>(undefined);
    const [deleteCover, setDeleteCover] = useState(false);
    const [isSubmitting, setIsSubmitting] = useState(false);

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
            setNextCourseId(initialData.nextCourseId);
            setSelectedModuleIds(initialModuleIds);
            setPreviewUrl(initialData.coverImageUrl);
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
                            {initialData ? 'Редагувати курс' : 'Створити новий курс'}
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
                                Обкладинка курсу
                            </label>
                            <div className="flex flex-col gap-3 w-full">
                                <div className="flex items-start gap-4">
                                    {previewUrl && (
                                        <div className="w-32 h-20 rounded-lg overflow-hidden border border-gray-200 shrink-0 relative group">
                                            <img src={previewUrl} alt="Cover preview" className="w-full h-full object-cover" />
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
                                            Обрати файл
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
                                                Видалити обкладинку
                                            </button>
                                        )}
                                    </div>
                                </div>
                                {!previewUrl && <span className="text-sm text-gray-500">No file chosen</span>}
                            </div>
                        </div>

                        {/* Course Name */}
                        <div>
                            <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2 ml-1">
                                <BookOpen className="w-4 h-4" />
                                Назва курсу *
                            </label>
                            <input
                                type="text"
                                value={name}
                                onChange={(e) => setName(e.target.value)}
                                className="w-full px-4 py-3 rounded-lg border border-gray-200 bg-white/50 outline-none transition-all focus:border-brand-primary focus:ring-2 focus:ring-brand-light focus:bg-white"
                                placeholder="Введіть назву курсу"
                                required
                            />
                        </div>

                        {/* Course Description */}
                        <div>
                            <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2 ml-1">
                                <FileText className="w-4 h-4" />
                                Опис *
                            </label>
                            <textarea
                                value={description}
                                onChange={(e) => setDescription(e.target.value)}
                                className="w-full px-4 py-3 rounded-lg border border-gray-200 bg-white/50 outline-none transition-all focus:border-brand-primary focus:ring-2 focus:ring-brand-light focus:bg-white resize-none"
                                placeholder="Введіть опис курсу"
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
                                    Ціна (€)
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
                                        Нова ціна: {(discountAmount
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
                                    Знижка (Сума)
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
                                    Знижка (%)
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
                                Тривалість доступу (дні)
                            </label>
                            <input
                                type="number"
                                min="1"
                                value={accessDuration || ''}
                                onChange={(e) => setAccessDuration(e.target.value ? parseInt(e.target.value) : undefined)}
                                className="w-full px-4 py-3 rounded-lg border border-gray-200 bg-white/50 outline-none transition-all focus:border-brand-primary focus:ring-2 focus:ring-brand-light focus:bg-white"
                                placeholder="Наприклад: 30"
                            />
                            <p className="text-xs text-gray-500 mt-1 ml-1">
                                Залиште порожнім для необмеженого доступу
                            </p>
                        </div>

                        {/* Promotional Discount */}
                        <div>
                            <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2 ml-1">
                                <Percent className="w-4 h-4" />
                                Акційна знижка на наступний курс (% або фіксована сума)
                            </label>
                            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                                {/* Promotional Discount Percentage */}
                                <div>
                                    <div className="relative">
                                        <input
                                            type="number"
                                            min="0"
                                            max="100"
                                            value={promotionalDiscountPercentage || ''}
                                            onChange={(e) => {
                                                const val = e.target.value ? parseInt(e.target.value) : undefined;
                                                setPromotionalDiscountPercentage(val);
                                                if (val && val > 0) setPromotionalDiscountAmount(undefined);
                                            }}
                                            className="w-full px-4 py-3 rounded-lg border border-gray-200 bg-white/50 outline-none transition-all focus:border-brand-primary focus:ring-2 focus:ring-brand-light focus:bg-white [appearance:textfield] [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none"
                                            placeholder="Наприклад: 15"
                                        />
                                        <span className="absolute right-4 top-3.5 text-gray-400 font-medium">%</span>
                                    </div>
                                    <p className="text-xs text-gray-500 mt-1 ml-1">
                                        Знижка у відсотках
                                    </p>
                                </div>

                                {/* Promotional Discount Amount */}
                                <div>
                                    <div className="relative">
                                        <input
                                            type="number"
                                            min="0"
                                            step="0.01"
                                            value={promotionalDiscountAmount || ''}
                                            onChange={(e) => {
                                                const val = e.target.value ? parseFloat(e.target.value) : undefined;
                                                setPromotionalDiscountAmount(val);
                                                if (val && val > 0) setPromotionalDiscountPercentage(undefined);
                                            }}
                                            className="w-full px-4 py-3 rounded-lg border border-gray-200 bg-white/50 outline-none transition-all focus:border-brand-primary focus:ring-2 focus:ring-brand-light focus:bg-white [appearance:textfield] [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none"
                                            placeholder="Наприклад: 10.00"
                                        />
                                        <span className="absolute right-4 top-3.5 text-gray-400 font-medium">€</span>
                                    </div>
                                    <p className="text-xs text-gray-500 mt-1 ml-1">
                                        Фіксована знижка
                                    </p>
                                </div>
                            </div>
                        </div>

                        {/* Next Course Recommendation */}
                        <div>
                            <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2 ml-1">
                                <ArrowRight className="w-4 h-4" />
                                Наступний рекомендований курс
                            </label>
                            <div className="relative">
                                <select
                                    value={nextCourseId || ''}
                                    onChange={(e) => setNextCourseId(e.target.value || undefined)}
                                    className="w-full px-4 py-3 rounded-lg border border-gray-200 bg-white/50 outline-none transition-all focus:border-brand-primary focus:ring-2 focus:ring-brand-light focus:bg-white appearance-none"
                                >
                                    <option value="">Не вказано</option>
                                    {courses
                                        .filter(course => course.id !== initialData?.id)
                                        .map(course => (
                                            <option key={course.id} value={course.id}>
                                                {course.name}
                                            </option>
                                        ))}
                                </select>
                                <div className="absolute inset-y-0 right-0 flex items-center px-4 pointer-events-none text-gray-500">
                                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 9l-7 7-7-7" />
                                    </svg>
                                </div>
                            </div>
                            <p className="text-xs text-gray-500 mt-1 ml-1">
                                Рекомендований наступний курс після завершення цього
                            </p>
                        </div>

                        {/* Module Selection */}
                        <div>
                            <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2 ml-1">
                                <Layers className="w-4 h-4" />
                                Вибрати модулі (Опціонально)
                            </label>
                            <div className="border border-gray-200 rounded-lg p-4 max-h-64 overflow-y-auto bg-white/50 custom-scrollbar">
                                {modules
                                    .filter(module => !module.courseId || (initialData && module.courseId === initialData.id))
                                    .length === 0 ? (
                                    <div className="p-4 text-center text-gray-500 text-sm italic">
                                        Доступних модулів не знайдено
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
                                                                Поточний курс: {module.courseName} (буде змінено)
                                                            </div>
                                                        )}
                                                    </div>
                                                </label>
                                            ))}
                                    </div>
                                )}
                            </div>
                            <div className="text-xs font-medium text-gray-500 text-right pt-2 border-t border-gray-200/50 mt-2">
                                Вибрано: <span className="text-brand-primary font-bold">{selectedModuleIds.length}</span>
                            </div>
                        </div>
                    </form>
                </div>

                {/* Footer - Static */}
                <div className="flex gap-4 p-6 border-t border-gray-100 bg-white/50 backdrop-blur-sm shrink-0">
                    <button
                        type="button"
                        onClick={onClose}
                        className="flex-1 py-3 font-bold text-brand-primary bg-white hover:bg-brand-primary hover:text-white rounded-lg transition-colors shadow-sm border border-gray-100"
                    >
                        Скасувати
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
                                <span>Збереження...</span>
                            </>
                        ) : (
                            initialData ? 'Оновити' : 'Створити'
                        )}
                    </button>
                </div>
            </div>
        </div>
    );
};
