import { useState } from 'react';
import { BookOpen, Edit2, Trash2, ChevronDown, ChevronUp, ShoppingCart } from 'lucide-react';
import type { CourseDto } from '../api/courses';
import type { Module } from '../api/modules';
import type { Lesson } from '../api/lessons';
import ModuleExpandableItem from './ModuleExpandableItem';
import { ExtendAccessModal } from './ExtendAccessModal';

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
    const [isExtendModalOpen, setIsExtendModalOpen] = useState(false);

    // Filter modules for this course
    const courseModules = modules.filter(m => m.courseId === course.id);
    const isLocked = !course.isEnrolled && !isCatalogMode; // Locked if not enrolled, but in catalog mode we usually show structure

    const lessonCount = courseModules.reduce((sum, m) => sum + (m.lessonsNumber || 0), 0);
    const totalMinutes = courseModules.reduce((sum, m) => sum + (m.durationMinutes || 0), 0);



    return (
        <div className="bg-white rounded-lg shadow-sm border border-gray-100 hover:shadow-md transition-shadow">
            <div
                className="p-4 cursor-pointer"
                onClick={() => setIsExpanded(!isExpanded)}
            >
                <div className="flex items-start gap-4">
                    {/* Icon */}
                    <div className="w-12 h-12 rounded-lg bg-red-50 flex items-center justify-center shrink-0">
                        <BookOpen className="w-6 h-6 text-brand-primary" />
                    </div>

                    {/* Main Content */}
                    <div className="flex-1 min-w-0">
                        <div className="flex items-start justify-between gap-4">
                            <div>
                                <h3 className="text-lg font-bold text-gray-900 leading-tight mb-1">
                                    {course.name}
                                </h3>
                                {/* Enrolled Badge or Status */}
                                {course.isEnrolled && !isCatalogMode && (
                                    <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-green-100 text-green-800">
                                        Ви записані
                                    </span>
                                )}
                                {course.description && (
                                    <p className="text-sm text-gray-500 mt-2 pr-4">
                                        {course.description}
                                    </p>
                                )}
                            </div>
                        </div>

                        {/* Metrics Line */}
                        <div className="mt-3 text-sm font-medium text-gray-500">
                            {(() => {
                                const parts = [];

                                // Helper for pluralization
                                const getNoun = (number: number, one: string, two: string, five: string) => {
                                    let n = Math.abs(number);
                                    n %= 100;
                                    if (n >= 5 && n <= 20) {
                                        return five;
                                    }
                                    n %= 10;
                                    if (n === 1) {
                                        return one;
                                    }
                                    if (n >= 2 && n <= 4) {
                                        return two;
                                    }
                                    return five;
                                };

                                // Modules
                                const modulesCount = courseModules.length;
                                if (modulesCount > 0) {
                                    parts.push(`${modulesCount} ${getNoun(modulesCount, 'модуль', 'модулі', 'модулів')}`);
                                }

                                // Lessons
                                if (lessonCount > 0) {
                                    parts.push(`${lessonCount} ${getNoun(lessonCount, 'урок', 'уроки', 'уроків')}`);
                                }

                                // Files - REMOVED per request

                                // Duration
                                if (totalMinutes > 0) {
                                    const h = Math.floor(totalMinutes / 60);
                                    const m = totalMinutes % 60;
                                    if (h > 0) {
                                        parts.push(`${h} год${m > 0 ? ` ${m} хв` : ''}`);
                                    } else {
                                        parts.push(`${m} хв`);
                                    }
                                }

                                return parts.join(' | ');
                            })()}
                        </div>
                    </div>

                    {/* Actions & Chevron */}
                    <div className="flex flex-col items-end gap-2 shrink-0 self-start mt-1">
                        <div className="flex items-center gap-4 text-gray-400">
                            {/* Price for Catalog */}
                            {(!course.isEnrolled || isCatalogMode) && (
                                <div className="text-right mr-2" onClick={(e) => e.stopPropagation()}>
                                    {(course.discountAmount && course.discountAmount > 0) || (course.discountPercentage && course.discountPercentage > 0) ? (
                                        <div className="flex flex-col items-end">
                                            <div className="flex items-center gap-2">
                                                <span className="text-xs text-gray-400 line-through">
                                                    {course.price || 0}€
                                                </span>
                                                <span className="text-xs font-bold text-red-500 bg-red-50 px-1.5 rounded">
                                                    {course.discountAmount ? `-${course.discountAmount}€` : `-${course.discountPercentage}%`}
                                                </span>
                                            </div>
                                            <div className="text-lg font-bold text-gray-900">
                                                {(course.discountAmount
                                                    ? ((course.price || 0) - course.discountAmount)
                                                    : ((course.price || 0) * (1 - (course.discountPercentage || 0) / 100))
                                                ).toFixed(2)}€
                                            </div>
                                        </div>
                                    ) : (
                                        <div className="text-lg font-bold text-gray-900">
                                            {!course.price || course.price === 0 ? 'Безкоштовно' : `${course.price}€`}
                                        </div>
                                    )}
                                </div>
                            )}

                            {/* Management Actions */}
                            {(!course.isEnrolled || isCatalogMode) && (
                                <div className="flex items-center gap-1" onClick={(e) => e.stopPropagation()}>
                                    <button
                                        onClick={() => onEdit(course)}
                                        className="p-2 hover:bg-gray-100 rounded-lg text-gray-500 hover:text-brand-primary transition-colors"
                                        title="Редагувати"
                                    >
                                        <Edit2 className="w-4 h-4" />
                                    </button>
                                    <button
                                        onClick={() => onDelete(course)}
                                        className="p-2 hover:bg-red-50 rounded-lg text-gray-500 hover:text-red-50 transition-colors"
                                        title="Видалити"
                                    >
                                        <Trash2 className="w-4 h-4" />
                                    </button>
                                </div>
                            )}

                            {isExpanded ? <ChevronUp className="w-5 h-5" /> : <ChevronDown className="w-5 h-5" />}
                        </div>

                        {/* Buy Button - Below */}
                        {onEnroll && (!course.isEnrolled || isCatalogMode) && (
                            <button
                                onClick={(e) => {
                                    e.stopPropagation();
                                    onEnroll(course.id);
                                }}
                                className="flex items-center gap-2 px-4 py-2 rounded-lg bg-brand-primary text-white hover:bg-brand-primary/90 transition-colors shadow-sm w-full justify-center"
                                title="Придбати курс"
                            >
                                <ShoppingCart className="w-4 h-4" />
                                <span className="font-medium">Придбати курс</span>
                            </button>
                        )}
                    </div>
                </div>
            </div>

            {/* Expanded Content */}
            <div className={`grid transition-all duration-500 ease-in-out ${isExpanded ? 'grid-rows-[1fr] opacity-100' : 'grid-rows-[0fr] opacity-0'}`}>
                <div className="overflow-hidden px-4 pb-4">
                    {/* Horizontal divider included in padding above implicitly or added here */}
                    {isExpanded && <div className="border-t border-gray-100 mb-4"></div>}

                    {/* Modules List */}
                    <div className="mt-4 pt-2">
                        <h4 className="text-sm font-semibold text-gray-900 mb-4 uppercase tracking-wide flex items-center gap-2">
                            <BookOpen className="w-4 h-4 text-brand-primary" />
                            Зміст Курсу ({courseModules.length})
                        </h4>

                        {courseModules.length > 0 ? (
                            <div className="flex flex-col gap-2">
                                {courseModules.map(module => (
                                    <ModuleExpandableItem
                                        key={module.id}
                                        module={module}
                                        isLocked={isLocked}
                                        onEditLesson={onEditLesson}
                                        onDeleteLesson={onDeleteLesson}
                                    />
                                ))}
                            </div>
                        ) : (
                            <div className="text-center py-6 text-gray-400 bg-gray-50/50 rounded-lg border border-dashed border-gray-100">
                                <BookOpen size={24} className="mx-auto mb-2 opacity-30" />
                                <p className="text-xs font-medium">Модулі ще не додано</p>
                            </div>
                        )}
                    </div>
                </div>
            </div>

            <ExtendAccessModal
                courseId={course.id}
                isOpen={isExtendModalOpen}
                onClose={() => setIsExtendModalOpen(false)}
                onSuccess={() => {
                    setIsExtendModalOpen(false);
                    // Optionally reload page or trigger refresh
                    window.location.reload();
                }}
            />
        </div>
    );
}

export default CourseExpandableCard;
