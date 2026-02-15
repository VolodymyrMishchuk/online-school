import { useState, useEffect } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { X, Upload, File, Loader2, BookOpen, Video, Clock, Layout, FileText, Trash2, AlertCircle } from 'lucide-react';
import { createLesson, updateLesson } from '../api/lessons';
import type { CreateLessonDto, Lesson } from '../api/lessons';
import { uploadFile } from '../api/files';
import { getModules } from '../api/modules';

interface LessonModalProps {
    isOpen: boolean;
    onClose: () => void;
    onSuccess: () => void;
    initialData?: Lesson | null;
}

export default function LessonModal({ isOpen, onClose, onSuccess, initialData }: LessonModalProps) {
    const isEditing = !!initialData;
    const [formData, setFormData] = useState<CreateLessonDto>({
        name: '',
        description: '',
        videoUrl: '',
        moduleId: '',
        durationMinutes: undefined,
    });
    const [selectedFiles, setSelectedFiles] = useState<File[]>([]);
    const [errors, setErrors] = useState<Record<string, string>>({});

    // Reset form when modal opens or initialData changes
    useEffect(() => {
        if (isOpen) {
            if (initialData) {
                setFormData({
                    name: initialData.name,
                    description: initialData.description,
                    videoUrl: initialData.videoUrl || '',
                    moduleId: initialData.moduleId,
                    durationMinutes: initialData.durationMinutes,
                });
            } else {
                setFormData({
                    name: '',
                    description: '',
                    videoUrl: '',
                    moduleId: '',
                    durationMinutes: undefined,
                });
            }
            setSelectedFiles([]);
            setErrors({});
        }
    }, [isOpen, initialData]);

    // Fetch modules for select
    const { data: modules, isLoading: modulesLoading } = useQuery({
        queryKey: ['modules'],
        queryFn: getModules,
        enabled: isOpen,
    });

    // Create/Update lesson mutation
    const mutation = useMutation({
        mutationFn: async () => {
            // Validate
            const newErrors: Record<string, string> = {};
            if (!formData.name.trim()) newErrors.name = "Назва обов'язкова";
            if (!formData.description.trim()) newErrors.description = "Опис обов'язковий";

            if (formData.videoUrl && formData.videoUrl.trim()) {
                try {
                    new URL(formData.videoUrl);
                } catch {
                    newErrors.videoUrl = "Невалідний URL";
                }
            }

            if (Object.keys(newErrors).length > 0) {
                setErrors(newErrors);
                throw new Error('Validation failed');
            }

            let lessonId: string;

            if (isEditing && initialData) {
                // Update
                await updateLesson(initialData.id, formData);
                lessonId = initialData.id;
            } else {
                // Create
                const lesson = await createLesson(formData);
                lessonId = lesson.id;
            }

            // Upload files if any
            if (selectedFiles.length > 0) {
                await Promise.all(
                    selectedFiles.map(file => uploadFile(file, 'LESSON', lessonId))
                );
            }
        },
        onSuccess: () => {
            onSuccess();
            onClose();
        },
        onError: (error: any) => {
            if (error.message !== 'Validation failed') {
                alert(`Помилка ${isEditing ? 'оновлення' : 'створення'} уроку: ` + (error.response?.data?.message || error.message));
            }
        },
    });

    const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
        if (e.target.files) {
            setSelectedFiles(prev => [...prev, ...Array.from(e.target.files!)]);
        }
    };

    const removeFile = (index: number) => {
        setSelectedFiles(prev => prev.filter((_, i) => i !== index));
    };

    const formatFileSize = (bytes: number): string => {
        if (bytes < 1024) return bytes + ' B';
        if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
        return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
    };

    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-white/30 backdrop-blur-md animate-in fade-in duration-200">
            {/* Glass Panel Modal */}
            <div
                className="glass-panel w-full max-w-2xl flex flex-col overflow-hidden animate-in zoom-in-95 duration-200 relative rounded-lg shadow-xl"
                style={{ background: 'rgba(255, 255, 255, 0.9)', maxHeight: '90vh' }}
            >
                {/* Header Bar - Static, Lighter (bg-white) */}
                <div className="flex items-center justify-between px-6 py-5 border-b border-gray-100 bg-white shrink-0 z-10 relative shadow-sm">
                    <div className="flex items-center gap-3">
                        <div className="flex items-center justify-center w-10 h-10 rounded-full bg-brand-light/50 text-brand-primary ring-2 ring-white shadow-sm">
                            <BookOpen className="w-5 h-5" />
                        </div>
                        <div>
                            <h2 className="text-xl font-bold text-brand-dark">
                                {isEditing ? 'Редагувати урок' : 'Створити урок'}
                            </h2>
                            <p className="text-xs text-gray-500 font-medium">
                                {isEditing ? 'Зміна існуючого контенту' : 'Додавання навчального матеріалу'}
                            </p>
                        </div>
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
                    {/* Name */}
                    <div>
                        <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2 ml-1">
                            <FileText className="w-4 h-4" />
                            Назва уроку <span className="text-red-500">*</span>
                        </label>
                        <input
                            type="text"
                            value={formData.name}
                            onChange={(e) => {
                                setFormData(prev => ({ ...prev, name: e.target.value }));
                                setErrors(prev => ({ ...prev, name: '' }));
                            }}
                            className={`w-full px-4 py-3 rounded-lg border bg-white/50 outline-none transition-all focus:ring-2 focus:ring-brand-light focus:bg-white ${errors.name
                                ? 'border-red-300 focus:border-red-500'
                                : 'border-gray-200 focus:border-brand-primary'
                                }`}
                            placeholder="Введіть назву уроку..."
                        />
                        {errors.name && (
                            <p className="flex items-center gap-1 text-red-500 text-xs mt-1 ml-1">
                                <AlertCircle className="w-3 h-3" />
                                {errors.name}
                            </p>
                        )}
                    </div>

                    {/* Description */}
                    <div>
                        <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2 ml-1">
                            <BookOpen className="w-4 h-4" />
                            Опис <span className="text-red-500">*</span>
                        </label>
                        <textarea
                            value={formData.description}
                            onChange={(e) => {
                                setFormData(prev => ({ ...prev, description: e.target.value }));
                                setErrors(prev => ({ ...prev, description: '' }));
                            }}
                            rows={4}
                            className={`w-full px-4 py-3 rounded-lg border bg-white/50 outline-none transition-all focus:ring-2 focus:ring-brand-light focus:bg-white resize-none ${errors.description
                                ? 'border-red-300 focus:border-red-500'
                                : 'border-gray-200 focus:border-brand-primary'
                                }`}
                            placeholder="Введіть опис уроку..."
                        />
                        {errors.description && (
                            <p className="flex items-center gap-1 text-red-500 text-xs mt-1 ml-1">
                                <AlertCircle className="w-3 h-3" />
                                {errors.description}
                            </p>
                        )}
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                        {/* Video URL */}
                        <div>
                            <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2 ml-1">
                                <Video className="w-4 h-4" />
                                Посилання на відео
                            </label>
                            <input
                                type="url"
                                value={formData.videoUrl || ''}
                                onChange={(e) => {
                                    setFormData(prev => ({ ...prev, videoUrl: e.target.value }));
                                    setErrors(prev => ({ ...prev, videoUrl: '' }));
                                }}
                                className={`w-full px-4 py-3 rounded-lg border bg-white/50 outline-none transition-all focus:ring-2 focus:ring-brand-light focus:bg-white ${errors.videoUrl
                                    ? 'border-red-300 focus:border-red-500'
                                    : 'border-gray-200 focus:border-brand-primary'
                                    }`}
                                placeholder="https://youtube.com/watch?v=..."
                            />
                            {errors.videoUrl && (
                                <p className="flex items-center gap-1 text-red-500 text-xs mt-1 ml-1">
                                    <AlertCircle className="w-3 h-3" />
                                    {errors.videoUrl}
                                </p>
                            )}
                        </div>

                        {/* Duration */}
                        <div>
                            <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2 ml-1">
                                <Clock className="w-4 h-4" />
                                Тривалість (хв)
                            </label>
                            <input
                                type="number"
                                min="0"
                                value={formData.durationMinutes || ''}
                                onChange={(e) => setFormData(prev => ({
                                    ...prev,
                                    durationMinutes: e.target.value ? parseInt(e.target.value) : undefined
                                }))}
                                className="w-full px-4 py-3 rounded-lg border border-gray-200 bg-white/50 outline-none transition-all focus:border-brand-primary focus:ring-2 focus:ring-brand-light focus:bg-white"
                                placeholder="30"
                            />
                        </div>
                    </div>

                    {/* Module Select - Only visible when creating */}
                    {!isEditing && (
                        <div>
                            <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2 ml-1">
                                <Layout className="w-4 h-4" />
                                Модуль
                            </label>
                            <div className="relative">
                                <select
                                    value={formData.moduleId || ''}
                                    onChange={(e) => {
                                        setFormData(prev => ({ ...prev, moduleId: e.target.value }));
                                        setErrors(prev => ({ ...prev, moduleId: '' }));
                                    }}
                                    className={`w-full px-4 py-3 rounded-lg border bg-white/50 outline-none transition-all focus:ring-2 focus:ring-brand-light focus:bg-white appearance-none ${errors.moduleId
                                        ? 'border-red-300 focus:border-red-500'
                                        : 'border-gray-200 focus:border-brand-primary'
                                        }`}
                                    disabled={modulesLoading}
                                >
                                    <option value="">Виберіть модуль...</option>
                                    {modules?.map(module => (
                                        <option key={module.id} value={module.id}>
                                            {module.name} {module.courseName && `(${module.courseName})`}
                                        </option>
                                    ))}
                                </select>
                                <div className="absolute right-4 top-1/2 -translate-y-1/2 pointer-events-none text-gray-400">
                                    <Layout className="w-4 h-4" />
                                </div>
                            </div>
                            {errors.moduleId && (
                                <p className="flex items-center gap-1 text-red-500 text-xs mt-1 ml-1">
                                    <AlertCircle className="w-3 h-3" />
                                    {errors.moduleId}
                                </p>
                            )}
                        </div>
                    )}

                    {/* Files Upload */}
                    <div>
                        <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2 ml-1">
                            <Upload className="w-4 h-4" />
                            {isEditing ? 'Додати нові файли' : 'Файли'}
                        </label>
                        <div className="border border-dashed border-gray-300 rounded-lg p-8 text-center hover:bg-gray-50/50 hover:border-brand-primary/50 transition-all cursor-pointer group bg-white/30">
                            <label className="cursor-pointer block w-full h-full">
                                <input
                                    type="file"
                                    multiple
                                    onChange={handleFileSelect}
                                    className="hidden"
                                />
                                <div className="flex flex-col items-center gap-3">
                                    <div className="w-12 h-12 rounded-full bg-brand-light/30 flex items-center justify-center group-hover:bg-brand-light/60 transition-colors">
                                        <Upload className="w-6 h-6 text-brand-primary" />
                                    </div>
                                    <div>
                                        <span className="text-sm text-brand-primary font-bold hover:underline">
                                            Натисніть
                                        </span>
                                        <span className="text-sm text-gray-500 font-medium">
                                            {' '}щоб обрати файли
                                        </span>
                                    </div>
                                    <p className="text-xs text-gray-400">
                                        або перетягніть файли сюди
                                    </p>
                                </div>
                            </label>
                        </div>

                        {/* Selected files list */}
                        {selectedFiles.length > 0 && (
                            <div className="mt-4 space-y-2 animate-in slide-in-from-top-2 duration-200">
                                {selectedFiles.map((file, index) => (
                                    <div
                                        key={index}
                                        className="flex items-center gap-3 p-3 bg-white border border-gray-100 rounded-lg shadow-sm"
                                    >
                                        <div className="w-8 h-8 rounded-lg bg-gray-100 flex items-center justify-center shrink-0">
                                            <File className="w-4 h-4 text-gray-500" />
                                        </div>
                                        <div className="flex-1 min-w-0">
                                            <p className="text-sm font-bold text-gray-700 truncate">
                                                {file.name}
                                            </p>
                                            <p className="text-xs text-gray-500">
                                                {formatFileSize(file.size)}
                                            </p>
                                        </div>
                                        <button
                                            onClick={() => removeFile(index)}
                                            className="p-1.5 rounded-lg text-gray-400 hover:text-red-500 hover:bg-red-50 transition-colors"
                                        >
                                            <Trash2 className="w-4 h-4" />
                                        </button>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>
                </div>

                {/* Footer - Static */}
                <div className="flex gap-4 p-6 border-t border-gray-100 bg-white/50 backdrop-blur-sm shrink-0">
                    <button
                        onClick={onClose}
                        disabled={mutation.isPending}
                        className="flex-1 py-3 font-bold text-brand-primary bg-white hover:bg-brand-primary hover:text-white rounded-lg transition-colors shadow-sm border border-gray-100"
                    >
                        Скасувати
                    </button>
                    <button
                        onClick={() => mutation.mutate()}
                        disabled={mutation.isPending}
                        className="flex-1 py-3 font-bold text-white bg-brand-primary hover:bg-brand-secondary rounded-lg transition-all shadow-lg hover:shadow-xl transform active:scale-95 duration-200 disabled:opacity-70 disabled:transform-none flex items-center justify-center gap-2"
                    >
                        {mutation.isPending && (
                            <Loader2 className="w-4 h-4 animate-spin" />
                        )}
                        {mutation.isPending
                            ? (isEditing ? 'Зберігаємо...' : 'Створюємо...')
                            : (isEditing ? 'Зберегти зміни' : 'Створити урок')}
                    </button>
                </div>
            </div>
        </div>
    );
}
