import { useState } from 'react';
import { BookOpen, Edit2, Trash2, ChevronDown, ChevronUp, ShoppingCart, CheckCircle } from 'lucide-react';
import type { CourseDto } from '../api/courses';
import type { Module } from '../api/modules';
import type { Lesson } from '../api/lessons';
import ModuleExpandableItem from './ModuleExpandableItem';

// Helper function for Ukrainian pluralization
const pluralize = (count: number, one: string, few: string, many: string): string => {
    const lastDigit = count % 10;
    const lastTwoDigits = count % 100;

    if (lastTwoDigits >= 11 && lastTwoDigits <= 14) {
        return many;
    }
    if (lastDigit === 1) {
        return one;
    }
    if (lastDigit >= 2 && lastDigit <= 4) {
        return few;
    }
    return many;
};

interface CourseExpandableCardProps {
    course: CourseDto;
    modules: Module[]; // All modules, we will filter for this course
    onEdit: (course: CourseDto) => void;
    onDelete: (course: CourseDto) => void;
    onEditLesson?: (lesson: Lesson) => void;
    onDeleteLesson?: (lesson: Lesson) => void;
    onEnroll?: (courseId: string) => void;
    isCatalogMode?: boolean;
}

function CourseExpandableCard({
    course,
    modules,
    onEdit,
    onDelete,
    onEditLesson,
    onDeleteLesson,
    onEnroll,
    isCatalogMode = false
}: CourseExpandableCardProps) {
    const [isExpanded, setIsExpanded] = useState(false);

    // Filter modules for this course
    const courseModules = modules.filter(m => m.courseId === course.id);

    // Calculate access status
    const getAccessDisplay = () => {
        if (isCatalogMode) return null; // Don't show access info in catalog mode
        if (!course.isEnrolled || !course.enrolledAt) return null;

        if (course.enrollmentStatus === 'BLOCKED') {
            return {
                text: 'Доступ завершено',
                className: 'bg-red-50 border-red-200 text-red-700',
                iconColor: 'text-red-600'
            };
        }

        if (!course.accessDuration) {
            return {
                text: new Date(course.enrolledAt).toLocaleDateString('uk-UA', {
                    day: 'numeric',
                    month: 'long',
                    year: 'numeric'
                }),
                className: 'bg-green-50 border-green-200 text-green-700',
                iconColor: 'text-green-600'
            };
        }

        const enrolledDate = new Date(course.enrolledAt);
        const expirationDate = new Date(enrolledDate.getTime() + course.accessDuration * 24 * 60 * 60 * 1000);
        const now = new Date();
        const diffMs = expirationDate.getTime() - now.getTime();

        if (diffMs <= 0) {
            return {
                text: 'Доступ завершено',
                className: 'bg-red-50 border-red-200 text-red-700',
                iconColor: 'text-red-600'
            };
        }

        const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));
        let timeText = '';

        if (diffDays >= 2) {
            const months = Math.floor(diffDays / 30);
            const days = diffDays % 30;
            if (months > 0) {
                timeText = `${months} міс. ${days} дн.`;
            } else {
                timeText = `${days} ${pluralize(days, 'день', 'дні', 'днів')}`;
            }
        } else {
            const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
            const diffMinutes = Math.floor((diffMs % (1000 * 60 * 60)) / (1000 * 60));
            timeText = `${diffHours} год ${diffMinutes} хв`;
        }

        return {
            text: `Залишилось: ${timeText}`,
            className: diffDays < 2 ? 'bg-orange-50 border-orange-200 text-orange-700' : 'bg-green-50 border-green-200 text-green-700',
            iconColor: diffDays < 2 ? 'text-orange-600' : 'text-green-600'
        };
    };

    const accessDisplay = getAccessDisplay();

    return (
        <div className={`bg-white rounded-xl shadow-sm border border-gray-100 transition-all duration-300 ${isExpanded ? 'shadow-md ring-2 ring-blue-50' : 'hover:shadow-md'}`}>
            <div
                className="p-6 cursor-pointer"
                onClick={() => setIsExpanded(!isExpanded)}
            >
                <div className="flex justify-between items-start">
                    <div className="flex gap-4 flex-1">
                        <div className="p-3 bg-blue-50 rounded-xl h-fit">
                            <BookOpen className="text-blue-600" size={24} />
                        </div>
                        <div className="flex-1">
                            <div className="flex items-center gap-3 mb-2">
                                <h3 className="text-xl font-bold text-gray-800">
                                    {course.name}
                                </h3>
                                {course.isEnrolled && accessDisplay && !isCatalogMode && (
                                    <div className={`flex items-center gap-1.5 px-3 py-1 border rounded-full ${accessDisplay.className}`}>
                                        <CheckCircle className={accessDisplay.iconColor} size={14} />
                                        <span className="text-xs font-semibold">Мій курс</span>
                                        <span className={`text-xs ${accessDisplay.iconColor}`}>•</span>
                                        <span className="text-xs font-medium">
                                            {accessDisplay.text}
                                        </span>
                                    </div>
                                )}
                            </div>

                            <p className="text-gray-600 mb-4 line-clamp-2">
                                {course.description}
                            </p>

                            <div className="flex items-center gap-6 text-sm text-gray-500">
                                <div className="flex items-center gap-2">
                                    <BookOpen size={16} />
                                    <span className="font-medium">
                                        {courseModules.length} {pluralize(courseModules.length, 'модуль', 'модулі', 'модулів')}
                                    </span>
                                </div>
                                {course.isEnrolled && (() => {
                                    const lessonCount = courseModules.reduce((sum, m) => sum + (m.lessonsNumber || 0), 0);
                                    const totalMinutes = courseModules.reduce((sum, m) => sum + (m.durationMinutes || 0), 0);
                                    const hours = Math.floor(totalMinutes / 60);
                                    const minutes = totalMinutes % 60;
                                    const durationText = hours > 0
                                        ? `${hours} год ${minutes > 0 ? minutes + ' хв' : ''}`
                                        : `${minutes} хв`;

                                    return (
                                        <>
                                            <div className="flex items-center gap-2">
                                                <BookOpen size={16} />
                                                <span className="font-medium">
                                                    {lessonCount} {pluralize(lessonCount, 'урок', 'уроки', 'уроків')}
                                                </span>
                                            </div>
                                            {totalMinutes > 0 && (
                                                <div className="flex items-center gap-2">
                                                    <span className="font-medium">{durationText}</span>
                                                </div>
                                            )}
                                        </>
                                    );
                                })()}
                            </div>


                        </div>
                    </div>

                    <div className="flex items-center gap-4">
                        {/* Price Display */}
                        {isCatalogMode && (
                            course.price ? (
                                <div className="flex flex-col items-end">
                                    {(course.discountAmount && course.discountAmount > 0) || (course.discountPercentage && course.discountPercentage > 0) ? (
                                        <>
                                            <div className="flex items-center gap-1.5 text-xs">
                                                <span className="text-gray-400 line-through decoration-gray-400">{course.price}€</span>
                                                <span className="bg-red-100 text-red-600 px-1.5 py-0.5 rounded text-[10px] font-bold">
                                                    {course.discountAmount ? `-${course.discountAmount}€` : `-${course.discountPercentage}%`}
                                                </span>
                                            </div>
                                            <div className="text-lg font-bold text-gray-900 leading-none">
                                                {(course.discountAmount
                                                    ? (course.price - course.discountAmount)
                                                    : (course.price * (1 - (course.discountPercentage || 0) / 100))
                                                ).toFixed(2)}€
                                            </div>
                                        </>
                                    ) : (
                                        <div className="text-lg font-bold text-gray-900">
                                            {course.price}€
                                        </div>
                                    )}
                                </div>
                            ) : (
                                course.price === 0 && <span className="text-green-600 font-bold">Безкоштовно</span>
                            )
                        )}

                        <div className="flex items-center gap-3">
                            <div className="flex gap-1" onClick={(e) => e.stopPropagation()}>
                                {(!course.isEnrolled || isCatalogMode) && onEnroll && (
                                    <button
                                        onClick={() => onEnroll(course.id)}
                                        className="px-4 py-2 bg-brand-primary text-white rounded-lg hover:bg-brand-primary/90 transition-colors font-medium flex items-center gap-2"
                                        title="Придбати курс"
                                    >
                                        <ShoppingCart size={16} />
                                        <span>Придбати</span>
                                    </button>
                                )}
                                {(!course.isEnrolled || isCatalogMode) && (
                                    <>
                                        <button
                                            onClick={() => onEdit(course)}
                                            className="p-2 text-gray-400 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                                            title="Редагувати курс"
                                        >
                                            <Edit2 size={18} />
                                        </button>
                                        <button
                                            onClick={() => onDelete(course)}
                                            className="p-2 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                                            title="Видалити курс"
                                        >
                                            <Trash2 size={18} />
                                        </button>
                                    </>
                                )}
                            </div>
                            {isExpanded ? <ChevronUp className="text-gray-400" /> : <ChevronDown className="text-gray-400" />}
                        </div>
                    </div>
                </div>
            </div>

            {/* Expanded Content: Modules List */}
            {isExpanded && (
                <div className="border-t border-gray-100 bg-gray-50/50 p-6">
                    <h4 className="text-sm font-semibold text-gray-900 mb-4 uppercase tracking-wide">
                        Зміст Курсу
                    </h4>

                    {courseModules.length > 0 ? (
                        <div className="flex flex-col gap-3">
                            {courseModules.map(module => (
                                <ModuleExpandableItem
                                    key={module.id}
                                    module={module}
                                    onEditLesson={onEditLesson}
                                    onDeleteLesson={onDeleteLesson ? (_lessonId: string) => {
                                        // We need to find the lesson by ID to call the parent handler
                                        // For now, just ignore this since we don't have the full lesson object
                                        // This is a limitation we can fix later if needed
                                    } : undefined}
                                />
                            ))}
                        </div>
                    ) : (
                        <div className="text-center py-8 bg-white rounded-xl border border-dashed border-gray-200 text-gray-500">
                            <p>У цьому курсі поки немає модулів.</p>
                        </div>
                    )}
                </div>
            )}
        </div>
    );
}

export default CourseExpandableCard;
