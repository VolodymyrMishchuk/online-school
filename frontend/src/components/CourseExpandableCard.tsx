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
    onDeleteLesson?: (lessonId: string) => void;
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

    // Calculate if access is locked (expired or blocked)
    const isLocked = () => {
        if (!course.isEnrolled || isCatalogMode) return false;
        if (course.enrollmentStatus === 'BLOCKED') return true;
        if (course.accessDuration && course.enrolledAt) {
            const enrolledDate = new Date(course.enrolledAt);
            const expirationDate = new Date(enrolledDate.getTime() + course.accessDuration * 24 * 60 * 60 * 1000);
            return new Date() > expirationDate;
        }
        return false;
    };

    const locked = isLocked();

    return (
        <div className={`glass-strong rounded-3xl overflow-hidden transition-all duration-300 w-full mb-6 ${!locked && isExpanded ? 'ring-2 ring-brand-primary/20' : ''} ${locked ? 'opacity-90 grayscale-[0.5]' : 'hover:translate-y-[-2px] hover:shadow-lg'}`}>
            <div
                className="p-6 md:p-8 cursor-pointer"
                onClick={() => setIsExpanded(!isExpanded)}
            >
                <div className="flex flex-col md:flex-row md:items-center gap-6">
                    {/* Icon Bubble */}
                    <div className="icon-bubble shrink-0 w-16 h-16 rounded-2xl">
                        <BookOpen className="w-8 h-8" />
                    </div>

                    {/* Content */}
                    <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-3">
                            <h2 className="text-2xl font-bold text-gray-900 leading-tight">
                                {course.name}
                            </h2>
                            {course.isEnrolled && accessDisplay && !isCatalogMode && (
                                <div className={`flex items-center gap-1.5 px-3 py-1 border rounded-full text-xs font-semibold ${accessDisplay.className}`}>
                                    <CheckCircle className={`w-3 h-3 ${accessDisplay.iconColor}`} />
                                    <span>{accessDisplay.text}</span>
                                </div>
                            )}
                        </div>

                        <p className="text-gray-500 mt-2 line-clamp-2 leading-relaxed">
                            {course.description}
                        </p>

                        <div className="flex items-center gap-6 mt-4 text-sm text-gray-500 flex-wrap">
                            <span className="flex items-center gap-2 bg-white/50 px-3 py-1.5 rounded-lg border border-white/60">
                                <BookOpen className="w-4 h-4 text-brand-secondary" />
                                {courseModules.length} {pluralize(courseModules.length, 'модуль', 'модулі', 'модулів')}
                            </span>

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
                                        <span className="flex items-center gap-2 bg-white/50 px-3 py-1.5 rounded-lg border border-white/60">
                                            <BookOpen className="w-4 h-4 text-brand-secondary" />
                                            {lessonCount} {pluralize(lessonCount, 'урок', 'уроки', 'уроків')}
                                        </span>
                                        {totalMinutes > 0 && (
                                            <span className="flex items-center gap-2 bg-white/50 px-3 py-1.5 rounded-lg border border-white/60">
                                                <div className="w-4 h-4 flex items-center justify-center">
                                                    <div className="w-3 h-3 rounded-full border-2 border-brand-secondary" />
                                                </div>
                                                {durationText}
                                            </span>
                                        )}
                                    </>
                                );
                            })()}
                        </div>
                    </div>

                    {/* Actions & Price */}
                    <div className="flex flex-col md:items-end gap-5 shrink-0 pt-4 md:pt-0 border-t md:border-t-0 border-gray-100 md:pl-6 md:border-l border-white/50">
                        {/* Price Display */}
                        {isCatalogMode && course.price !== undefined && (
                            <div className="text-right">
                                {(course.discountAmount && course.discountAmount > 0) || (course.discountPercentage && course.discountPercentage > 0) ? (
                                    <>
                                        <div className="flex items-center justify-end">
                                            <span className="text-sm text-gray-400 line-through mr-2 font-medium">
                                                {course.price}€
                                            </span>
                                            <span className="badge-discount">
                                                {course.discountAmount ? `-${course.discountAmount}€` : `-${course.discountPercentage}%`}
                                            </span>
                                        </div>
                                        <div className="text-3xl font-bold text-gray-900 mt-1">
                                            {(course.discountAmount
                                                ? (course.price - course.discountAmount)
                                                : (course.price * (1 - (course.discountPercentage || 0) / 100))
                                            ).toFixed(2)}€
                                        </div>
                                    </>
                                ) : (
                                    <div className="text-3xl font-bold text-gray-900">
                                        {course.price === 0 ? 'Безкоштовно' : `${course.price}€`}
                                    </div>
                                )}
                            </div>
                        )}

                        <div className="flex items-center gap-3 w-full md:w-auto">
                            {(!course.isEnrolled || isCatalogMode) && onEnroll && (
                                <button
                                    onClick={(e) => {
                                        e.stopPropagation();
                                        onEnroll(course.id);
                                    }}
                                    className="flex-1 md:flex-none flex items-center justify-center gap-2 px-8 py-3 rounded-xl bg-brand-primary text-white font-bold hover:opacity-90 transition-all shadow-lg shadow-brand-primary/25"
                                >
                                    <ShoppingCart className="w-5 h-5" />
                                    Придбати
                                </button>
                            )}

                            {(!course.isEnrolled || isCatalogMode) && (
                                <div className="flex items-center gap-2" onClick={(e) => e.stopPropagation()}>
                                    <button
                                        onClick={() => onEdit(course)}
                                        className="p-3 rounded-xl hover:bg-white/50 transition-colors text-gray-500 hover:text-brand-primary border border-transparent hover:border-brand-primary/20"
                                        title="Редагувати"
                                    >
                                        <Edit2 className="w-5 h-5" />
                                    </button>
                                    <button
                                        onClick={() => onDelete(course)}
                                        className="p-3 rounded-xl hover:bg-red-50/80 transition-colors text-gray-500 hover:text-red-500 border border-transparent hover:border-red-200"
                                        title="Видалити"
                                    >
                                        <Trash2 className="w-5 h-5" />
                                    </button>
                                </div>
                            )}

                            <button className="p-3 rounded-xl hover:bg-white/50 transition-colors text-gray-400">
                                {isExpanded ? <ChevronUp className="w-6 h-6" /> : <ChevronDown className="w-6 h-6" />}
                            </button>
                        </div>
                    </div>
                </div>
            </div>

            {/* Expanded Content: Modules List */}
            {isExpanded && (
                <div className="border-t border-white/60 bg-white/30 backdrop-blur-sm p-6 md:p-8">
                    <h4 className="text-sm font-bold text-gray-800 mb-6 uppercase tracking-wider flex items-center gap-2 opacity-70">
                        <BookOpen className="w-4 h-4" />
                        Зміст Курсу
                    </h4>

                    {courseModules.length > 0 ? (
                        <div className="flex flex-col gap-4">
                            {courseModules.map(module => (
                                <ModuleExpandableItem
                                    key={module.id}
                                    module={module}
                                    isLocked={locked}
                                    onEditLesson={onEditLesson}
                                    onDeleteLesson={onDeleteLesson}
                                />
                            ))}
                        </div>
                    ) : (
                        <div className="text-center py-12 bg-white/50 rounded-2xl border border-dashed border-gray-200 text-gray-500">
                            <p>У цьому курсі поки немає модулів.</p>
                        </div>
                    )}
                </div>
            )}
        </div>
    );
}

export default CourseExpandableCard;
