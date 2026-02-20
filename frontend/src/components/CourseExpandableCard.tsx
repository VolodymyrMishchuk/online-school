import { useState } from 'react';
import { createPortal } from 'react-dom';
import { BookOpen, Edit2, Trash2, ChevronDown, ChevronUp, ShoppingCart, Clock, X } from 'lucide-react';
import type { CourseDto } from '../api/courses';
import type { Module } from '../api/modules';
import type { Lesson } from '../api/lessons';
import { removeCourseAccess } from '../api/users';
import ModuleExpandableItem from './ModuleExpandableItem';
import { ExtendAccessModal } from './ExtendAccessModal';
import { useNavigate } from 'react-router-dom';

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
    const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
    const [isDeleting, setIsDeleting] = useState(false);
    const navigate = useNavigate();

    // Filter modules for this course
    const courseModules = modules.filter(m => m.courseId === course.id);
    const isLocked = (!course.isEnrolled && !isCatalogMode) || (course.enrollmentStatus === 'BLOCKED' && !isCatalogMode);

    const lessonCount = courseModules.reduce((sum, m) => sum + (m.lessonsNumber || 0), 0);
    const totalMinutes = courseModules.reduce((sum, m) => sum + (m.durationMinutes || 0), 0);



    const hasImage = !!course.coverImageUrl;

    const handleDeleteCourse = (e?: React.MouseEvent) => {
        if (e) e.stopPropagation();
        setIsDeleteModalOpen(true);
    };

    const confirmDeleteCourse = async () => {
        setIsDeleting(true);
        const userStr = localStorage.getItem('user');
        const user = userStr ? JSON.parse(userStr) : null;
        const userId = user?.userId || localStorage.getItem('userId') || '';

        if (!userId) {
            setIsDeleting(false);
            return;
        }

        try {
            await removeCourseAccess(userId, course.id);
            window.location.reload();
        } catch (error) {
            console.error('Failed to remove course', error);
            alert('Помилка при видаленні курсу. Спробуйте пізніше.');
            setIsDeleting(false);
        }
    };

    const handleExtendWithPayment = () => {
        // Placeholder for future logic
        alert('Ця функція перебуває у стадії розробки.');
    };

    const handleGetDiscount = () => {
        if (course.nextCourseId) {
            navigate(`/dashboard/catalog/${course.nextCourseId}`);
        }
    };

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

    let expirationText = '';
    let isExpirationLessThanWeek = false;

    if (course.expiresAt) {
        const diffMs = new Date(course.expiresAt).getTime() - new Date().getTime();
        const diffMins = Math.floor(diffMs / (1000 * 60));

        if (diffMs <= 0) {
            expirationText = 'Доступ завершено';
            isExpirationLessThanWeek = true;
        } else if (diffMins < 60) {
            expirationText = `Курс доступний ще ${diffMins} ${getNoun(diffMins, 'хвилина', 'хвилини', 'хвилин')}`;
            isExpirationLessThanWeek = true;
        } else if (diffMins < 48 * 60) {
            const h = Math.floor(diffMins / 60);
            expirationText = `Курс доступний ще ${h} ${getNoun(h, 'година', 'години', 'годин')}`;
            isExpirationLessThanWeek = true;
        } else {
            const totalDays = Math.floor(diffMins / (60 * 24));
            isExpirationLessThanWeek = totalDays < 7;
            const months = Math.floor(totalDays / 30);
            const days = totalDays % 30;
            const parts = [];

            if (months > 0 && totalDays > 31) {
                parts.push(`${months} ${getNoun(months, 'місяць', 'місяці', 'місяців')}`);
            }

            const remainingDays = totalDays > 31 ? days : totalDays;
            if (remainingDays > 0) {
                parts.push(`${remainingDays} ${getNoun(remainingDays, 'день', 'дні', 'днів')}`);
            }
            expirationText = parts.length > 0 ? `Курс доступний ще ${parts.join(' ')}` : 'Курс доступний ще менше дня';
        }
    }

    return (
        <div
            className={`relative rounded-2xl transition-all duration-300 overflow-hidden group ${hasImage ? 'border-transparent text-white shadow-[0_0_15px_rgba(238,155,148,0.5)] hover:shadow-[0_0_25px_rgba(238,155,148,0.8)]' : 'bg-white border-gray-100 shadow-sm hover:shadow-md text-gray-900'} border`}
        >
            {/* Background Image Layer */}
            {hasImage && (
                <div
                    className="absolute inset-0 z-0 pointer-events-none transition-colors duration-300"
                    style={{ backgroundColor: course.averageColor || '#111827' }}
                >
                    <div className="relative w-full">
                        <img
                            src={course.coverImageUrl}
                            alt=""
                            className="w-full object-cover object-top"
                            style={{
                                minWidth: '100%',
                                height: 'auto',
                                maxHeight: isExpanded ? 'none' : '100%', // Allow full height, but controlled by container
                            }}
                        />
                        {/* Bottom fade gradient for image */}
                        <div
                            className="absolute bottom-0 left-0 right-0 h-[100px]"
                            style={{
                                background: `linear-gradient(to top, ${course.averageColor || '#111827'}, transparent)`
                            }}
                        />
                    </div>

                    {/* Top Gradient for text readability */}
                    <div className="absolute top-0 left-0 right-0 h-[150px] bg-gradient-to-b from-white/30 to-transparent" />
                </div>
            )}

            <div className={`relative z-10 ${hasImage ? '' : ''}`}>
                <div
                    className="p-4 cursor-pointer"
                    onClick={() => setIsExpanded(!isExpanded)}
                >
                    <div className="flex items-start gap-4">
                        {/* Icon */}
                        <div className={`w-12 h-12 rounded-lg flex items-center justify-center shrink-0 ${hasImage ? 'bg-white/20 backdrop-blur-md text-white border border-white/20' : 'bg-red-50 text-brand-primary'}`}>
                            <BookOpen className="w-6 h-6" />
                        </div>

                        {/* Main Content */}
                        <div className="flex-1 min-w-0">
                            <div className="flex items-start justify-between gap-4">
                                <div>
                                    <h3 className={`text-lg font-bold leading-tight mb-1 ${hasImage ? 'text-white drop-shadow-lg' : 'text-gray-900'}`}>
                                        {course.name}
                                    </h3>
                                    {/* Enrolled Badge or Status */}
                                    {course.isEnrolled && !isCatalogMode && course.enrollmentStatus === 'BLOCKED' && (
                                        <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${hasImage ? 'bg-red-500/20 text-red-100 border border-red-500/30 backdrop-blur-sm' : 'bg-red-100 text-red-800'}`}>
                                            Доступ завершено
                                        </span>
                                    )}
                                    {course.expiresAt && !isCatalogMode && course.enrollmentStatus !== 'BLOCKED' && (
                                        <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium gap-1 ${isExpirationLessThanWeek
                                            ? (hasImage ? 'bg-yellow-500/20 text-yellow-100 border border-yellow-500/30 backdrop-blur-sm' : 'bg-yellow-100 text-yellow-800')
                                            : (hasImage ? 'bg-green-500/20 text-green-100 border border-green-500/30 backdrop-blur-sm' : 'bg-green-100 text-green-800')
                                            }`}>
                                            <Clock className="w-3 h-3" />
                                            {expirationText}
                                        </span>
                                    )}
                                    {course.description && (
                                        <p className={`text-sm mt-2 pr-4 ${hasImage ? 'text-white drop-shadow-md' : 'text-gray-500'}`}>
                                            {course.description}
                                        </p>
                                    )}
                                </div>
                            </div>

                            {/* Metrics Line */}
                            <div className={`mt-3 text-sm font-medium ${hasImage ? 'text-gray-100 drop-shadow-md' : 'text-gray-500'}`}>
                                {(() => {
                                    const parts = [];

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
                            <div className={`flex items-center gap-4 ${hasImage ? 'text-gray-300' : 'text-gray-400'}`}>
                                {/* Price for Catalog */}
                                {(!course.isEnrolled || isCatalogMode) && (
                                    <div className="text-right mr-2" onClick={(e) => e.stopPropagation()}>
                                        {(course.discountAmount && course.discountAmount > 0) || (course.discountPercentage && course.discountPercentage > 0) ? (
                                            <div className="flex flex-col items-end">
                                                <div className="flex items-center gap-2">
                                                    <span className={`text-xs line-through ${hasImage ? 'text-gray-400' : 'text-gray-400'}`}>
                                                        {course.price || 0}€
                                                    </span>
                                                    <span className="text-xs font-bold text-red-500 bg-red-50 px-1.5 rounded">
                                                        {course.discountAmount ? `-${course.discountAmount}€` : `-${course.discountPercentage}%`}
                                                    </span>
                                                </div>
                                                <div className={`text-lg font-bold ${hasImage ? 'text-white drop-shadow-md' : 'text-gray-900'}`}>
                                                    {(course.discountAmount
                                                        ? ((course.price || 0) - course.discountAmount)
                                                        : ((course.price || 0) * (1 - (course.discountPercentage || 0) / 100))
                                                    ).toFixed(2)}€
                                                </div>
                                            </div>
                                        ) : (
                                            <div className={`text-lg font-bold ${hasImage ? 'text-white drop-shadow-md' : 'text-gray-900'}`}>
                                                {!course.price || course.price === 0 ? 'Безкоштовно' : `${course.price}€`}
                                            </div>
                                        )}
                                    </div>
                                )}

                                {/* Management Actions */}
                                {isCatalogMode && (
                                    <div className="flex items-center gap-1" onClick={(e) => e.stopPropagation()}>
                                        <button
                                            onClick={() => onEdit(course)}
                                            className={`p-2 rounded-lg transition-colors ${hasImage ? 'hover:bg-white/20 text-gray-300 hover:text-white' : 'hover:bg-gray-100 text-gray-500 hover:text-brand-primary'}`}
                                            title="Редагувати"
                                        >
                                            <Edit2 className="w-4 h-4" />
                                        </button>
                                        <button
                                            onClick={() => onDelete(course)}
                                            className={`p-2 rounded-lg transition-colors ${hasImage ? 'hover:bg-red-500/20 text-gray-300 hover:text-red-400' : 'hover:bg-red-50 text-gray-500 hover:text-red-50'}`}
                                            title="Видалити"
                                        >
                                            <Trash2 className="w-4 h-4" />
                                        </button>
                                    </div>
                                )}
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

                            {/* Blocked Course Actions */}
                            {!isCatalogMode && course.isEnrolled && course.enrollmentStatus === 'BLOCKED' && (
                                <div className="flex flex-col gap-2 w-full mt-4" onClick={(e) => e.stopPropagation()}>
                                    <button
                                        onClick={() => setIsExtendModalOpen(true)}
                                        className="w-full text-center px-4 py-2 text-sm font-medium rounded-lg text-white bg-brand-primary hover:bg-brand-primary/90 transition-colors shadow-sm"
                                    >
                                        Отримати доступ за відеовідгук
                                    </button>
                                    <button
                                        onClick={handleExtendWithPayment}
                                        className="w-full text-center px-4 py-2 text-sm font-medium rounded-lg text-brand-dark bg-white border border-gray-200 hover:bg-gray-50 transition-colors shadow-sm"
                                    >
                                        Продовжити курс зі знижкою
                                    </button>
                                    {course.nextCourseId && (
                                        <button
                                            onClick={handleGetDiscount}
                                            className="w-full text-center px-4 py-2 text-sm font-medium rounded-lg text-white bg-green-600 hover:bg-green-700 transition-colors shadow-sm"
                                        >
                                            Отримати знижку{course.promotionalDiscountPercentage ? ` -${course.promotionalDiscountPercentage}%` : ''}{course.promotionalDiscountAmount ? ` -${course.promotionalDiscountAmount}€` : ''} на {course.nextCourseName || 'наступний курс'}
                                        </button>
                                    )}
                                    <button
                                        onClick={handleDeleteCourse}
                                        className="w-full text-center px-4 py-2 text-sm font-medium rounded-lg text-red-600 bg-red-50 hover:bg-red-100 transition-colors shadow-sm"
                                    >
                                        Видалити курс
                                    </button>
                                </div>
                            )}
                        </div>
                    </div>

                    {/* Chevron perfectly centered at the bottom of the unexpanded view */}
                    <div className="flex justify-center w-full mt-2" onClick={(e) => {
                        e.stopPropagation();
                        setIsExpanded(!isExpanded);
                    }}>
                        <div className={`p-1 flex items-center justify-center rounded-full hover:bg-black/5 transition-colors cursor-pointer ${hasImage ? 'text-gray-300 hover:bg-white/10' : 'text-gray-400'}`}>
                            {isExpanded ? <ChevronUp className="w-6 h-6" /> : <ChevronDown className="w-6 h-6" />}
                        </div>
                    </div>
                </div>

                {/* Expanded Content */}
                <div className={`grid transition-all duration-500 ease-in-out ${isExpanded ? 'grid-rows-[1fr] opacity-100' : 'grid-rows-[0fr] opacity-0'}`}>
                    <div className="overflow-hidden px-4 pb-4">
                        {/* Horizontal divider */}
                        {isExpanded && <div className={`border-t mb-4 ${hasImage ? 'border-white/20' : 'border-gray-100'}`}></div>}

                        {/* Modules List */}
                        <div className="mt-4 pt-2">
                            <h4 className={`text-sm font-semibold mb-4 uppercase tracking-wide flex items-center gap-2 ${hasImage ? 'text-white/90 drop-shadow-sm' : 'text-gray-900'}`}>
                                <BookOpen className={`w-4 h-4 ${hasImage ? 'text-white' : 'text-brand-primary'}`} />
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
                                            isTransparent={hasImage}
                                        />
                                    ))}
                                </div>
                            ) : (
                                <div className={`text-center py-6 rounded-lg border border-dashed ${hasImage ? 'border-white/20 bg-white/10 text-gray-300' : 'border-gray-100 bg-gray-50/50 text-gray-400'}`}>
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
                        window.location.reload();
                    }}
                />
            </div>

            {/* Custom Delete Confirmation Modal using Portal if possible, otherwise just break out */}
            {isDeleteModalOpen && createPortal(
                <div
                    className="fixed inset-0 z-[9999] flex items-center justify-center p-4 bg-white/50 backdrop-blur-sm animate-in fade-in duration-200"
                    onClick={(e) => { e.stopPropagation(); setIsDeleteModalOpen(false); }}
                >
                    <div
                        className="relative w-full max-w-lg bg-white/90 backdrop-blur-md rounded-lg shadow-2xl overflow-hidden animate-in zoom-in-95 duration-200 max-h-[90vh] flex flex-col"
                        onClick={(e) => e.stopPropagation()}
                    >
                        {/* Header */}
                        <div className="flex items-center justify-between px-8 py-6 border-b border-gray-200/50 bg-white/30 backdrop-blur-sm">
                            <h2 className="text-xl font-bold text-gray-900">
                                Підтвердження видалення
                            </h2>
                            <button
                                onClick={() => setIsDeleteModalOpen(false)}
                                className="p-2 text-gray-500 hover:text-gray-900 hover:bg-gray-100 rounded-lg transition-colors"
                            >
                                <X size={20} />
                            </button>
                        </div>

                        {/* Content */}
                        <div className="p-8 space-y-6 overflow-y-auto">
                            <p className="text-gray-700 text-lg">
                                Ви дійсно бажаєте видалити курс "<span className="font-bold">{course.name}</span>" зі свого кабінету?
                            </p>
                            <p className="text-gray-500 text-sm">
                                Ця дія є незворотною. Після видалення ви втратите доступ до матеріалів курсу.
                            </p>
                        </div>

                        {/* Footer */}
                        <div className="flex items-center justify-end gap-3 px-8 py-6 border-t border-gray-200/50 bg-white/30 backdrop-blur-sm">
                            <button
                                onClick={() => setIsDeleteModalOpen(false)}
                                disabled={isDeleting}
                                className="text-gray-900 font-medium hover:bg-gray-100 rounded-lg px-4 py-2 transition-colors disabled:opacity-50"
                            >
                                Скасувати
                            </button>
                            <button
                                onClick={confirmDeleteCourse}
                                disabled={isDeleting}
                                className={`
                                    bg-red-600 text-white font-bold rounded-lg px-6 py-3 shadow-lg hover:shadow-xl transform active:scale-95 transition-all flex items-center justify-center min-w-[140px]
                                    ${isDeleting ? 'opacity-50 cursor-not-allowed' : 'hover:bg-red-700 hover:shadow-red-500/25'}
                                `}
                            >
                                {isDeleting ? (
                                    <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin"></div>
                                ) : (
                                    'Видалити курс'
                                )}
                            </button>
                        </div>
                    </div>
                </div>,
                document.body
            )}
        </div >
    );
}

export default CourseExpandableCard;
