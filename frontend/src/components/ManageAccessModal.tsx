import React, { useState, useEffect } from 'react';
import { addCourseAccess, removeCourseAccess } from '../api/users';
import type { PersonWithEnrollments } from '../api/users';
import { getCourses } from '../api/courses';
import type { CourseDto } from '../api/courses';
import { X, Search, BookOpen, Layers, CheckCircle, AlertCircle, Loader2, Trash2, PlusCircle, Shield, AlertTriangle, Clock } from 'lucide-react';

interface ManageAccessModalProps {
    isOpen: boolean;
    onClose: () => void;
    user: PersonWithEnrollments | null;
    onRefresh: () => void;
    isReadonlyForFakeAdmin?: boolean;
    onShowRestriction?: () => void;
}

export const ManageAccessModal: React.FC<ManageAccessModalProps> = ({ isOpen, onClose, user, onRefresh, isReadonlyForFakeAdmin, onShowRestriction }) => {
    const [allCourses, setAllCourses] = useState<CourseDto[]>([]);
    const [loading, setLoading] = useState(false);
    const [actionLoading, setActionLoading] = useState<string | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [searchTerm, setSearchTerm] = useState('');
    const [confirmRevokeCourse, setConfirmRevokeCourse] = useState<CourseDto | null>(null);

    useEffect(() => {
        if (isOpen) {
            fetchCourses();
            setSearchTerm('');
            setConfirmRevokeCourse(null);
        }
    }, [isOpen]);

    const fetchCourses = async () => {
        setLoading(true);
        try {
            const data = await getCourses();
            setAllCourses(data);
        } catch (err) {
            console.error(err);
            setError('Не вдалося завантажити курси.');
        } finally {
            setLoading(false);
        }
    };

    const handleToggleAccess = async (course: CourseDto, isEnrolled: boolean) => {
        if (!user) return;

        if (isReadonlyForFakeAdmin) {
            if (onShowRestriction) onShowRestriction();
            return;
        }

        if (isEnrolled) {
            setConfirmRevokeCourse(course);
            return;
        }

        // Grants access immediately
        await executeAccessChange(course, false);
    };

    const handleConfirmRevoke = async () => {
        if (confirmRevokeCourse) {
            await executeAccessChange(confirmRevokeCourse, true);
            setConfirmRevokeCourse(null);
        }
    };

    const executeAccessChange = async (course: CourseDto, isRevoking: boolean) => {
        if (!user) return;

        setActionLoading(course.id);
        setError(null);
        try {
            if (isRevoking) {
                await removeCourseAccess(user.id, course.id);
            } else {
                await addCourseAccess(user.id, course.id);
            }
            onRefresh();
        } catch (err) {
            setError(`Не вдалося ${isRevoking ? 'забрати' : 'додати'} доступ.`);
        } finally {
            setActionLoading(null);
        }
    };

    if (!isOpen || !user) return null;

    const enrolledCourseIds = new Set(user.enrollments.map(e => e.courseId));

    const filteredCourses = allCourses.filter(course =>
        course.name.toLowerCase().includes(searchTerm.toLowerCase())
    );

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-white/30 backdrop-blur-md animate-in fade-in duration-200">
            {/* Glass Panel Modal */}
            <div
                className="glass-panel w-full max-w-2xl flex flex-col overflow-hidden animate-in zoom-in-95 duration-200 relative rounded-lg shadow-xl"
                style={{ background: 'rgba(255, 255, 255, 0.95)', height: '80vh', maxHeight: '800px' }}
            >
                {/* Header Bar */}
                <div className="flex items-center justify-between px-6 py-5 border-b border-gray-100 bg-white shrink-0 z-10 relative shadow-sm">
                    <div className="flex items-center gap-3">
                        <div className="flex items-center justify-center w-10 h-10 rounded-full bg-brand-light/50 text-brand-primary ring-2 ring-white shadow-sm">
                            <Shield className="w-5 h-5" />
                        </div>
                        <div>
                            <h2 className="text-xl font-bold text-brand-dark">Управління доступом</h2>
                            <p className="text-xs text-gray-500 font-medium">
                                {user.firstName} {user.lastName}
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

                {/* Sub-header / Search */}
                <div className="px-6 py-4 bg-gray-50/50 border-b border-gray-100 shrink-0">
                    <div className="relative">
                        <input
                            type="text"
                            placeholder="Пошук курсу..."
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                            className="w-full pl-10 pr-4 py-2 rounded-lg border border-gray-200 bg-white outline-none focus:border-brand-primary focus:ring-2 focus:ring-brand-light transition-all text-sm"
                        />
                        <Search className="absolute left-3 top-2.5 w-4 h-4 text-gray-400" />
                    </div>
                </div>

                {/* Body - List */}
                <div className="flex-1 overflow-y-auto custom-scrollbar bg-white/30 p-4">
                    {error && (
                        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm flex items-center gap-2 mb-4 animate-in fade-in slide-in-from-top-2">
                            <AlertCircle className="w-4 h-4" />
                            {error}
                        </div>
                    )}

                    {loading ? (
                        <div className="flex flex-col items-center justify-center h-full text-gray-400 gap-3">
                            <Loader2 className="w-8 h-8 animate-spin text-brand-primary" />
                            <p className="text-sm">Завантаження курсів...</p>
                        </div>
                    ) : filteredCourses.length === 0 ? (
                        <div className="flex flex-col items-center justify-center h-full text-gray-400 gap-2">
                            <BookOpen className="w-10 h-10 opacity-20" />
                            <p className="text-sm">Курсів не знайдено</p>
                        </div>
                    ) : (
                        <div className="space-y-3">
                            {filteredCourses.map((course) => {
                                const isEnrolled = enrolledCourseIds.has(course.id);
                                const isLoading = actionLoading === course.id;

                                return (
                                    <div
                                        key={course.id}
                                        className={`flex items-center justify-between p-4 rounded-xl border transition-all duration-200 ${isEnrolled
                                            ? 'bg-green-50/50 border-green-100 hover:shadow-sm'
                                            : 'bg-white border-gray-100 hover:border-brand-light hover:shadow-sm'
                                            }`}
                                    >
                                        <div className="flex items-center gap-4">
                                            <div className={`flex items-center justify-center w-10 h-10 rounded-lg ${isEnrolled ? 'bg-green-100 text-green-600' : 'bg-gray-100 text-gray-500'
                                                }`}>
                                                <BookOpen className="w-5 h-5" />
                                            </div>
                                            <div>
                                                <h3 className="font-semibold text-gray-900">{course.name}</h3>
                                                <div className="flex items-center gap-3 mt-1 text-xs text-gray-500">
                                                    <span className="flex items-center gap-1">
                                                        <Layers className="w-3 h-3" />
                                                        {(() => {
                                                            const count = course.modulesNumber || 0;
                                                            const lastDigit = count % 10;
                                                            const lastTwoDigits = count % 100;
                                                            if (lastTwoDigits >= 11 && lastTwoDigits <= 14) return `${count} модулів`;
                                                            if (lastDigit === 1) return `${count} модуль`;
                                                            if (lastDigit >= 2 && lastDigit <= 4) return `${count} модулі`;
                                                            return `${count} модулів`;
                                                        })()}
                                                    </span>
                                                    <span className="flex items-center gap-1">
                                                        <BookOpen className="w-3 h-3" />
                                                        {(() => {
                                                            const count = course.lessonsCount || 0;
                                                            const lastDigit = count % 10;
                                                            const lastTwoDigits = count % 100;
                                                            if (lastTwoDigits >= 11 && lastTwoDigits <= 14) return `${count} уроків`;
                                                            if (lastDigit === 1) return `${count} урок`;
                                                            if (lastDigit >= 2 && lastDigit <= 4) return `${count} уроки`;
                                                            return `${count} уроків`;
                                                        })()}
                                                    </span>
                                                    <span className="flex items-center gap-1">
                                                        <Clock className="w-3 h-3" />
                                                        {(() => {
                                                            const minutes = course.durationMinutes || 0;
                                                            const hours = Math.floor(minutes / 60);
                                                            const mins = minutes % 60;

                                                            let timeString = "";
                                                            if (hours > 0) {
                                                                timeString += `${hours} год `;
                                                            }
                                                            timeString += `${mins} хв`;
                                                            return timeString.trim();
                                                        })()}
                                                    </span>

                                                    {isEnrolled && (
                                                        <span className="flex items-center gap-1 font-medium text-green-600 bg-green-100/50 px-2 py-0.5 rounded-full ml-1">
                                                            <CheckCircle className="w-3 h-3" />
                                                            Доступ відкрито
                                                        </span>
                                                    )}
                                                </div>
                                            </div>
                                        </div>

                                        <button
                                            onClick={() => handleToggleAccess(course, isEnrolled)}
                                            disabled={isLoading}
                                            className={`flex items-center justify-center w-10 h-10 rounded-lg transition-all duration-200 ${isLoading
                                                ? 'bg-gray-100 text-gray-400 cursor-not-allowed'
                                                : isEnrolled
                                                    ? 'bg-white border border-red-100 text-red-500 hover:bg-red-50 hover:border-red-200 hover:shadow-sm'
                                                    : 'bg-brand-primary text-white hover:bg-brand-secondary hover:shadow-md hover:scale-105 active:scale-95'
                                                }`}
                                            title={isEnrolled ? "Забрати доступ" : "Надати доступ"}
                                        >
                                            {isLoading ? (
                                                <Loader2 className="w-5 h-5 animate-spin" />
                                            ) : isEnrolled ? (
                                                <Trash2 className="w-5 h-5" />
                                            ) : (
                                                <PlusCircle className="w-5 h-5" />
                                            )}
                                        </button>
                                    </div>
                                );
                            })}
                        </div>
                    )}
                </div>

                {/* Footer - Static */}
                <div className="flex justify-end p-6 border-t border-gray-100 bg-white/50 backdrop-blur-sm shrink-0">
                    <button
                        type="button"
                        onClick={onClose}
                        className="px-6 py-2.5 font-bold text-gray-700 bg-white hover:bg-gray-50 border border-gray-200 rounded-lg transition-all shadow-sm hover:shadow"
                    >
                        Закрити
                    </button>
                </div>
            </div>

            {/* Custom Confirmation Modal Overlay */}
            {confirmRevokeCourse && (
                <div className="absolute inset-0 z-[60] flex items-center justify-center bg-black/40 backdrop-blur-sm animate-in fade-in duration-200">
                    <div className="bg-white rounded-2xl shadow-2xl p-6 w-full max-w-sm mx-4 animate-in zoom-in-95 duration-200 border border-gray-100">
                        <div className="flex flex-col items-center text-center">
                            <div className="w-12 h-12 bg-red-100 text-red-600 rounded-full flex items-center justify-center mb-4">
                                <AlertTriangle className="w-6 h-6" />
                            </div>
                            <h3 className="text-lg font-bold text-gray-900 mb-2">Забрати доступ?</h3>
                            <p className="text-gray-500 text-sm mb-6">
                                Ви впевнені, що хочете забрати доступ до курсу <span className="font-semibold text-gray-900">"{confirmRevokeCourse.name}"</span>?
                                Користувач втратить можливість переглядати матеріали цього курсу.
                            </p>
                            <div className="flex gap-3 w-full">
                                <button
                                    onClick={() => setConfirmRevokeCourse(null)}
                                    className="flex-1 px-4 py-2.5 bg-gray-100 hover:bg-gray-200 text-gray-700 rounded-xl font-medium transition-colors"
                                >
                                    Скасувати
                                </button>
                                <button
                                    onClick={handleConfirmRevoke}
                                    className="flex-1 px-4 py-2.5 bg-red-600 hover:bg-red-700 text-white rounded-xl font-medium transition-colors shadow-lg shadow-red-200"
                                >
                                    Забрати доступ
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};
