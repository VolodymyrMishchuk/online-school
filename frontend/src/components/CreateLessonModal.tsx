import { useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { X, Upload, File, Loader2 } from 'lucide-react';
import { createLesson } from '../api/lessons';
import type { CreateLessonDto } from '../api/lessons';
import { uploadFile } from '../api/files';
import { getModules } from '../api/modules';

interface CreateLessonModalProps {
    isOpen: boolean;
    onClose: () => void;
    onSuccess: () => void;
}

export default function CreateLessonModal({ isOpen, onClose, onSuccess }: CreateLessonModalProps) {
    const [formData, setFormData] = useState<CreateLessonDto>({
        name: '',
        description: '',
        videoUrl: '',
        moduleId: '',
        durationMinutes: undefined,
    });
    const [selectedFiles, setSelectedFiles] = useState<File[]>([]);
    const [errors, setErrors] = useState<Record<string, string>>({});

    // Fetch modules for select
    const { data: modules, isLoading: modulesLoading } = useQuery({
        queryKey: ['modules'],
        queryFn: () => getModules(),
        enabled: isOpen,
    });

    // Create lesson mutation
    const createMutation = useMutation({
        mutationFn: async () => {
            // Validate
            const newErrors: Record<string, string> = {};
            if (!formData.name.trim()) newErrors.name = "Назва обов'язкова";
            if (!formData.description.trim()) newErrors.description = "Опис обов'язковий";
            // moduleId is now optional

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

            // 1. Create lesson
            const lesson = await createLesson(formData);

            // 2. Upload files if any
            if (selectedFiles.length > 0) {
                await Promise.all(
                    selectedFiles.map(file => uploadFile(file, 'LESSON', lesson.id))
                );
            }

            return lesson;
        },
        onSuccess: () => {
            onSuccess();
            handleClose();
        },
        onError: (error: any) => {
            if (error.message !== 'Validation failed') {
                alert('Помилка створення уроку: ' + (error.response?.data?.message || error.message));
            }
        },
    });

    const handleClose = () => {
        setFormData({
            name: '',
            description: '',
            videoUrl: '',
            moduleId: '',
            durationMinutes: undefined,
        });
        setSelectedFiles([]);
        setErrors({});
        onClose();
    };

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
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
            {/* Backdrop */}
            <div
                className="absolute inset-0 bg-black/50 backdrop-blur-sm"
                onClick={handleClose}
            />

            {/* Modal */}
            <div className="relative bg-white rounded-3xl shadow-2xl w-full max-w-2xl max-h-[90vh] overflow-y-auto">
                {/* Header */}
                <div className="sticky top-0 bg-white border-b border-gray-100 px-6 py-4 flex items-center justify-between rounded-t-3xl">
                    <h2 className="text-2xl font-bold text-brand-dark">Створити урок</h2>
                    <button
                        onClick={handleClose}
                        className="p-2 rounded-xl hover:bg-gray-100 transition-colors"
                    >
                        <X className="w-5 h-5" />
                    </button>
                </div>

                {/* Form */}
                <div className="p-6 space-y-6">
                    {/* Name */}
                    <div>
                        <label className="block text-sm font-semibold text-brand-dark mb-2">
                            Назва уроку <span className="text-red-500">*</span>
                        </label>
                        <input
                            type="text"
                            value={formData.name}
                            onChange={(e) => {
                                setFormData(prev => ({ ...prev, name: e.target.value }));
                                setErrors(prev => ({ ...prev, name: '' }));
                            }}
                            className={`w-full px-4 py-3 rounded-2xl border ${errors.name ? 'border-red-500' : 'border-gray-200'
                                } focus:outline-none focus:ring-2 focus:ring-brand-primary/20`}
                            placeholder="Введіть назву уроку..."
                        />
                        {errors.name && <p className="text-red-500 text-sm mt-1">{errors.name}</p>}
                    </div>

                    {/* Description */}
                    <div>
                        <label className="block text-sm font-semibold text-brand-dark mb-2">
                            Опис <span className="text-red-500">*</span>
                        </label>
                        <textarea
                            value={formData.description}
                            onChange={(e) => {
                                setFormData(prev => ({ ...prev, description: e.target.value }));
                                setErrors(prev => ({ ...prev, description: '' }));
                            }}
                            rows={4}
                            className={`w-full px-4 py-3 rounded-2xl border ${errors.description ? 'border-red-500' : 'border-gray-200'
                                } focus:outline-none focus:ring-2 focus:ring-brand-primary/20`}
                            placeholder="Введіть опис уроку..."
                        />
                        {errors.description && <p className="text-red-500 text-sm mt-1">{errors.description}</p>}
                    </div>

                    {/* Video URL */}
                    <div>
                        <label className="block text-sm font-semibold text-brand-dark mb-2">
                            Посилання на відео
                        </label>
                        <input
                            type="url"
                            value={formData.videoUrl || ''}
                            onChange={(e) => {
                                setFormData(prev => ({ ...prev, videoUrl: e.target.value }));
                                setErrors(prev => ({ ...prev, videoUrl: '' }));
                            }}
                            className={`w-full px-4 py-3 rounded-2xl border ${errors.videoUrl ? 'border-red-500' : 'border-gray-200'
                                } focus:outline-none focus:ring-2 focus:ring-brand-primary/20`}
                            placeholder="https://youtube.com/watch?v=..."
                        />
                        {errors.videoUrl && <p className="text-red-500 text-sm mt-1">{errors.videoUrl}</p>}
                    </div>

                    {/* Module Select */}
                    <div>
                        <label className="block text-sm font-semibold text-brand-dark mb-2">
                            Модуль
                        </label>
                        <select
                            value={formData.moduleId}
                            onChange={(e) => {
                                setFormData(prev => ({ ...prev, moduleId: e.target.value }));
                                setErrors(prev => ({ ...prev, moduleId: '' }));
                            }}
                            className={`w-full px-4 py-3 rounded-2xl border ${errors.moduleId ? 'border-red-500' : 'border-gray-200'
                                } focus:outline-none focus:ring-2 focus:ring-brand-primary/20 bg-white`}
                            disabled={modulesLoading}
                        >
                            <option value="">Виберіть модуль...</option>
                            {modules?.map(module => (
                                <option key={module.id} value={module.id}>
                                    {module.name} {module.courseName && `(${module.courseName})`}
                                </option>
                            ))}
                        </select>
                        {errors.moduleId && <p className="text-red-500 text-sm mt-1">{errors.moduleId}</p>}
                    </div>

                    {/* Duration */}
                    <div>
                        <label className="block text-sm font-semibold text-brand-dark mb-2">
                            Тривалість (хвилини)
                        </label>
                        <input
                            type="number"
                            min="0"
                            value={formData.durationMinutes || ''}
                            onChange={(e) => setFormData(prev => ({
                                ...prev,
                                durationMinutes: e.target.value ? parseInt(e.target.value) : undefined
                            }))}
                            className="w-full px-4 py-3 rounded-2xl border border-gray-200 focus:outline-none focus:ring-2 focus:ring-brand-primary/20"
                            placeholder="30"
                        />
                    </div>

                    {/* Files */}
                    <div>
                        <label className="block text-sm font-semibold text-brand-dark mb-2">
                            Файли
                        </label>
                        <div className="border-2 border-dashed border-gray-200 rounded-2xl p-6 text-center">
                            <label className="cursor-pointer">
                                <input
                                    type="file"
                                    multiple
                                    onChange={handleFileSelect}
                                    className="hidden"
                                />
                                <div className="flex flex-col items-center gap-2">
                                    <Upload className="w-8 h-8 text-gray-400" />
                                    <span className="text-sm text-gray-600 font-medium">
                                        Натисніть, щоб обрати файли
                                    </span>
                                    <span className="text-xs text-gray-400">
                                        або перетягніть файли сюди
                                    </span>
                                </div>
                            </label>
                        </div>

                        {/* Selected files list */}
                        {selectedFiles.length > 0 && (
                            <div className="mt-4 space-y-2">
                                {selectedFiles.map((file, index) => (
                                    <div
                                        key={index}
                                        className="flex items-center gap-3 p-3 bg-gray-50 rounded-xl"
                                    >
                                        <File className="w-5 h-5 text-brand-primary flex-shrink-0" />
                                        <div className="flex-1 min-w-0">
                                            <p className="text-sm font-medium text-brand-dark truncate">
                                                {file.name}
                                            </p>
                                            <p className="text-xs text-gray-500">
                                                {formatFileSize(file.size)}
                                            </p>
                                        </div>
                                        <button
                                            onClick={() => removeFile(index)}
                                            className="p-1 rounded-lg hover:bg-gray-200 transition-colors"
                                        >
                                            <X className="w-4 h-4 text-gray-500" />
                                        </button>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>
                </div>

                {/* Footer */}
                <div className="sticky bottom-0 bg-white border-t border-gray-100 px-6 py-4 flex items-center justify-end gap-3 rounded-b-3xl">
                    <button
                        onClick={handleClose}
                        disabled={createMutation.isPending}
                        className="px-6 py-3 rounded-2xl border border-gray-200 text-gray-700 font-semibold hover:bg-gray-50 transition-colors disabled:opacity-50"
                    >
                        Скасувати
                    </button>
                    <button
                        onClick={() => createMutation.mutate()}
                        disabled={createMutation.isPending}
                        className="px-6 py-3 rounded-2xl bg-brand-primary text-white font-semibold hover:bg-brand-primary/90 transition-colors disabled:opacity-50 flex items-center gap-2"
                    >
                        {createMutation.isPending && (
                            <Loader2 className="w-4 h-4 animate-spin" />
                        )}
                        {createMutation.isPending ? 'Створюємо...' : 'Створити урок'}
                    </button>
                </div>
            </div>
        </div>
    );
}
