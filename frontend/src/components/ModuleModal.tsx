import type { CourseDto } from '../api/courses';
import type { Lesson } from '../api/lessons';
import type { CreateModuleDto } from '../api/modules';
import { X } from 'lucide-react';
import React, { useEffect, useState } from 'react';

interface ModuleModalProps {
    isOpen: boolean;
    onClose: () => void;
    onSubmit: (data: CreateModuleDto) => void;
    unassignedLessons: Lesson[];
    courses: CourseDto[]; // Added courses prop
    courseId?: string; // Made optional
    initialData?: {
        id: string;
        name: string;
        description: string;
        lessons: Lesson[];
    };
}

export const ModuleModal: React.FC<ModuleModalProps> = ({
    isOpen,
    onClose,
    onSubmit,
    unassignedLessons,
    courses,
    courseId,
    initialData,
}) => {
    const [name, setName] = useState('');
    const [description, setDescription] = useState('');
    const [selectedCourseId, setSelectedCourseId] = useState(courseId || (courses.length > 0 ? courses[0].id : ''));
    const [selectedLessonIds, setSelectedLessonIds] = useState<string[]>([]);

    // Initialize form with existing data when editing or when props change
    useEffect(() => {
        if (initialData) {
            setName(initialData.name);
            setDescription(initialData.description);
            setSelectedLessonIds(initialData.lessons.map(l => l.id));
            if (courseId) {
                setSelectedCourseId(courseId);
            }
        } else {
            setName('');
            setDescription('');
            setSelectedLessonIds([]);
            // If courseId prop is provided, use it, otherwise use the first available course or current selection
            if (courseId) {
                setSelectedCourseId(courseId);
            } else if (courses.length > 0 && !selectedCourseId) {
                setSelectedCourseId(courses[0].id);
            }
        }
    }, [initialData, isOpen, courseId, courses]);

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        onSubmit({
            name,
            description,
            courseId: selectedCourseId || undefined,
            lessonIds: selectedLessonIds,
        });
        onClose();
    };

    const toggleLesson = (lessonId: string) => {
        setSelectedLessonIds(prev =>
            prev.includes(lessonId)
                ? prev.filter(id => id !== lessonId)
                : [...prev, lessonId]
        );
    };

    // Combine unassigned lessons with currently assigned lessons (for editing)
    const availableLessons = initialData
        ? [...unassignedLessons, ...initialData.lessons]
        : unassignedLessons;

    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
            <div className="bg-white rounded-lg shadow-xl max-w-2xl w-full max-h-[90vh] overflow-hidden flex flex-col">
                {/* Header */}
                <div className="flex justify-between items-center p-6 border-b">
                    <h2 className="text-2xl font-bold text-gray-800">
                        {initialData ? 'Редагувати Модуль' : 'Створити Новий Модуль'}
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
                        {/* Course Selection (Optional) */}
                        {/* Course Selection (Optional) - Only visible when creating */}
                        {!initialData && (
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-2">
                                    Курс
                                </label>
                                <select
                                    value={selectedCourseId}
                                    onChange={(e) => setSelectedCourseId(e.target.value)}
                                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                                >
                                    <option value="">Без Курсу</option>
                                    {courses.map(course => (
                                        <option key={course.id} value={course.id}>
                                            {course.name}
                                        </option>
                                    ))}
                                </select>
                                {courses.length === 0 && (
                                    <p className="text-sm text-gray-500 mt-1">
                                        Курси відсутні. Модуль буде створено без курсу.
                                    </p>
                                )}
                            </div>
                        )}

                        {/* Module Name */}
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-2">
                                Назва Модуля *
                            </label>
                            <input
                                type="text"
                                value={name}
                                onChange={(e) => setName(e.target.value)}
                                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                                placeholder="Введіть назву модуля"
                                required
                            />
                        </div>

                        {/* Module Description */}
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-2">
                                Опис *
                            </label>
                            <textarea
                                value={description}
                                onChange={(e) => setDescription(e.target.value)}
                                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-none"
                                placeholder="Введіть опис модуля"
                                rows={3}
                                required
                            />
                        </div>

                        {/* Lesson Selection */}
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-2">
                                Вибрати Уроки (Опціонально)
                            </label>
                            <div className="border border-gray-300 rounded-lg max-h-64 overflow-y-auto">
                                {availableLessons.length === 0 ? (
                                    <div className="p-4 text-center text-gray-500">
                                        Немає вільних уроків
                                    </div>
                                ) : (
                                    <div className="divide-y divide-gray-200">
                                        {availableLessons.map((lesson) => (
                                            <label
                                                key={lesson.id}
                                                className="flex items-start p-3 hover:bg-gray-50 cursor-pointer transition-colors"
                                            >
                                                <input
                                                    type="checkbox"
                                                    checked={selectedLessonIds.includes(lesson.id)}
                                                    onChange={() => toggleLesson(lesson.id)}
                                                    className="mt-1 mr-3 h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                                                />
                                                <div className="flex-1">
                                                    <div className="font-medium text-gray-900">
                                                        {lesson.name}
                                                    </div>
                                                    {lesson.description && (
                                                        <div className="text-sm text-gray-500 mt-1">
                                                            {lesson.description}
                                                        </div>
                                                    )}
                                                </div>
                                            </label>
                                        ))}
                                    </div>
                                )}
                            </div>
                            <div className="text-sm text-gray-500 mt-2">
                                {selectedLessonIds.length} урок(ів) вибрано
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
                            {initialData ? 'Оновити Модуль' : 'Створити Модуль'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};
