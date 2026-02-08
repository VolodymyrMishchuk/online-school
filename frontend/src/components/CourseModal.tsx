import type { CourseDto, CreateCourseDto } from '../api/courses';
import type { Module } from '../api/modules';
import { X } from 'lucide-react';
import React, { useEffect, useState } from 'react';

interface CourseModalProps {
    isOpen: boolean;
    onClose: () => void;
    onSubmit: (data: CreateCourseDto) => void;
    modules: Module[];
    initialData?: CourseDto;
    initialModuleIds?: string[]; // IDs of modules assigned to this course (for editing)
}

export const CourseModal: React.FC<CourseModalProps> = ({
    isOpen,
    onClose,
    onSubmit,
    modules,
    initialData,
    initialModuleIds = []
}) => {
    const [name, setName] = useState('');
    const [description, setDescription] = useState('');
    const [selectedModuleIds, setSelectedModuleIds] = useState<string[]>([]);

    useEffect(() => {
        if (initialData) {
            setName(initialData.name);
            setDescription(initialData.description);
            setSelectedModuleIds(initialModuleIds);
        } else {
            setName('');
            setDescription('');
            setSelectedModuleIds([]);
        }
    }, [initialData, isOpen, initialModuleIds]);

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        onSubmit({
            name,
            description,
            moduleIds: selectedModuleIds
        });
        onClose();
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
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
            <div className="bg-white rounded-lg shadow-xl max-w-2xl w-full max-h-[90vh] overflow-hidden flex flex-col">
                {/* Header */}
                <div className="flex justify-between items-center p-6 border-b">
                    <h2 className="text-2xl font-bold text-gray-800">
                        {initialData ? 'Редагувати Курс' : 'Створити Новий Курс'}
                    </h2>
                    <button
                        onClick={onClose}
                        className="text-gray-500 hover:text-gray-700 transition-colors"
                    >
                        <X size={24} />
                    </button>
                </div>

                {/* Form */}
                <form onSubmit={handleSubmit} className="flex flex-col flex-1 overflow-hidden">
                    <div className="p-6 space-y-4 overflow-y-auto flex-1">
                        {/* Course Name */}
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-2">
                                Назва Курсу *
                            </label>
                            <input
                                type="text"
                                value={name}
                                onChange={(e) => setName(e.target.value)}
                                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                                placeholder="Введіть назву курсу"
                                required
                            />
                        </div>

                        {/* Course Description */}
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-2">
                                Опис *
                            </label>
                            <textarea
                                value={description}
                                onChange={(e) => setDescription(e.target.value)}
                                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-none"
                                placeholder="Введіть опис курсу"
                                rows={3}
                                required
                            />
                        </div>

                        {/* Module Selection */}
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-2">
                                Вибрати Модулі (Опціонально)
                            </label>
                            <div className="border border-gray-300 rounded-lg max-h-64 overflow-y-auto">
                                {modules
                                    .filter(module => !module.courseId || (initialData && module.courseId === initialData.id))
                                    .length === 0 ? (
                                    <div className="p-4 text-center text-gray-500">
                                        Доступних модулів не знайдено
                                    </div>
                                ) : (
                                    <div className="divide-y divide-gray-200">
                                        {modules
                                            .filter(module => !module.courseId || (initialData && module.courseId === initialData.id))
                                            .map((module) => (
                                                <label
                                                    key={module.id}
                                                    className="flex items-start p-3 hover:bg-gray-50 cursor-pointer transition-colors"
                                                >
                                                    <input
                                                        type="checkbox"
                                                        checked={selectedModuleIds.includes(module.id)}
                                                        onChange={() => toggleModule(module.id)}
                                                        className="mt-1 mr-3 h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                                                    />
                                                    <div className="flex-1">
                                                        <div className="font-medium text-gray-900">
                                                            {module.name}
                                                        </div>
                                                        {module.description && (
                                                            <div className="text-sm text-gray-500 mt-1">
                                                                {module.description}
                                                            </div>
                                                        )}
                                                        {module.courseName && module.courseName !== (initialData?.name || '') && (
                                                            <div className="text-xs text-orange-500 mt-1">
                                                                Поточний курс: {module.courseName} (буде перепризначено)
                                                            </div>
                                                        )}
                                                    </div>
                                                </label>
                                            ))}
                                    </div>
                                )}
                            </div>
                            <div className="text-sm text-gray-500 mt-2">
                                {selectedModuleIds.length} модулів вибрано
                            </div>
                        </div>
                    </div>

                    {/* Footer */}
                    <div className="flex justify-end gap-3 p-6 border-t bg-gray-50">
                        <button
                            type="button"
                            onClick={onClose}
                            className="px-6 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-100 transition-colors"
                        >
                            Скасувати
                        </button>
                        <button
                            type="submit"
                            className="px-6 py-2 bg-blue-600 hover:bg-blue-700 rounded-lg text-white transition-colors"
                        >
                            {initialData ? 'Оновити Курс' : 'Створити Курс'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};
