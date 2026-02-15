import type { CourseDto } from '../api/courses';
import type { Lesson } from '../api/lessons';
import type { CreateModuleDto } from '../api/modules';
import { X, Layers, BookOpen, CheckCircle, FileText } from 'lucide-react';
import React, { useEffect, useState } from 'react';

interface ModuleModalProps {
    isOpen: boolean;
    onClose: () => void;
    onSubmit: (data: CreateModuleDto) => void;
    unassignedLessons: Lesson[];
    courses: CourseDto[];
    courseId?: string;
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
    const [isSubmitting, setIsSubmitting] = useState(false);

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

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setIsSubmitting(true);
        try {
            await onSubmit({
                name,
                description,
                courseId: selectedCourseId,
                lessonIds: selectedLessonIds,
            });
            onClose();
        } finally {
            setIsSubmitting(false);
        }
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
                            <Layers className="w-5 h-5" />
                        </div>
                        <h2 className="text-xl font-bold text-brand-dark">
                            {initialData ? 'Редагувати модуль' : 'Створити новий модуль'}
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
                    <form id="module-form" onSubmit={handleSubmit} className="space-y-6">
                        {/* Course Selection (Only visible when creating and no pre-selected course) */}
                        {!initialData && !courseId && (
                            <div>
                                <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2 ml-1">
                                    <BookOpen className="w-4 h-4" />
                                    Курс
                                </label>
                                <div className="relative">
                                    <select
                                        value={selectedCourseId}
                                        onChange={(e) => setSelectedCourseId(e.target.value)}
                                        className="w-full px-4 py-3 rounded-lg border border-gray-200 bg-white/50 outline-none transition-all focus:border-brand-primary focus:ring-2 focus:ring-brand-light focus:bg-white appearance-none"
                                    >
                                        <option value="">Без курсу</option>
                                        {courses.map(course => (
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
                                {courses.length === 0 && (
                                    <p className="text-sm text-gray-500 mt-1 ml-1">
                                        Курси відсутні. Модуль буде створено без курсу.
                                    </p>
                                )}
                            </div>
                        )}

                        {/* Module Name */}
                        <div>
                            <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2 ml-1">
                                <Layers className="w-4 h-4" />
                                Назва модуля *
                            </label>
                            <input
                                type="text"
                                value={name}
                                onChange={(e) => setName(e.target.value)}
                                className="w-full px-4 py-3 rounded-lg border border-gray-200 bg-white/50 outline-none transition-all focus:border-brand-primary focus:ring-2 focus:ring-brand-light focus:bg-white"
                                placeholder="Введіть назву модуля"
                                required
                            />
                        </div>

                        {/* Module Description */}
                        <div>
                            <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2 ml-1">
                                <FileText className="w-4 h-4" />
                                Опис *
                            </label>
                            <textarea
                                value={description}
                                onChange={(e) => setDescription(e.target.value)}
                                className="w-full px-4 py-3 rounded-lg border border-gray-200 bg-white/50 outline-none transition-all focus:border-brand-primary focus:ring-2 focus:ring-brand-light focus:bg-white resize-none"
                                placeholder="Введіть опис модуля"
                                rows={3}
                                required
                            />
                        </div>

                        {/* Lesson Selection */}
                        <div>
                            <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2 ml-1">
                                <BookOpen className="w-4 h-4" />
                                Вибрати уроки (Опціонально)
                            </label>
                            <div className="border border-gray-200 rounded-lg p-4 max-h-64 overflow-y-auto bg-white/50 custom-scrollbar">
                                {availableLessons.length === 0 ? (
                                    <div className="p-4 text-center text-gray-500 text-sm italic">
                                        Немає вільних уроків
                                    </div>
                                ) : (
                                    <div className="space-y-2">
                                        {availableLessons.map((lesson) => (
                                            <label
                                                key={lesson.id}
                                                className="flex items-start p-3 hover:bg-white rounded-lg cursor-pointer transition-colors group border border-transparent hover:border-gray-100"
                                            >
                                                <div className="relative flex items-center mt-0.5">
                                                    <input
                                                        type="checkbox"
                                                        checked={selectedLessonIds.includes(lesson.id)}
                                                        onChange={() => toggleLesson(lesson.id)}
                                                        className="peer h-5 w-5 cursor-pointer appearance-none rounded-md border border-gray-300 transition-all checked:border-brand-primary checked:bg-brand-primary group-hover:border-brand-primary"
                                                    />
                                                    <CheckCircle className="absolute pointer-events-none opacity-0 peer-checked:opacity-100 text-white w-3.5 h-3.5 left-[3px] top-[3px]" />
                                                </div>
                                                <div className="ml-3 flex-1">
                                                    <div className="font-bold text-gray-700 group-hover:text-brand-dark transition-colors">
                                                        {lesson.name}
                                                    </div>
                                                    {lesson.description && (
                                                        <div className="text-sm text-gray-500 mt-0.5 line-clamp-1">
                                                            {lesson.description}
                                                        </div>
                                                    )}
                                                </div>
                                            </label>
                                        ))}
                                    </div>
                                )}
                            </div>
                            <div className="text-xs font-medium text-gray-500 text-right pt-2 border-t border-gray-200/50 mt-2">
                                Вибрано: <span className="text-brand-primary font-bold">{selectedLessonIds.length}</span>
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
                        form="module-form"
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
