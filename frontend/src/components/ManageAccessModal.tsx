import React, { useState, useEffect } from 'react';
import { addCourseAccess, removeCourseAccess } from '../api/users';
import type { PersonWithEnrollments } from '../api/users';
import { getCourses } from '../api/courses';
import type { CourseDto } from '../api/courses';
import * as Icons from 'lucide-react';

interface ManageAccessModalProps {
    isOpen: boolean;
    onClose: () => void;
    user: PersonWithEnrollments | null;
    onRefresh: () => void;
}

export const ManageAccessModal: React.FC<ManageAccessModalProps> = ({ isOpen, onClose, user, onRefresh }) => {
    const [allCourses, setAllCourses] = useState<CourseDto[]>([]);
    const [loading, setLoading] = useState(false);
    const [actionLoading, setActionLoading] = useState<string | null>(null); // CourseId being processed
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        if (isOpen) {
            fetchCourses();
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

        if (isEnrolled) {
            if (!window.confirm(`Ви впевнені, що хочете забрати доступ до курсу "${course.name}"?`)) return;
        }

        setActionLoading(course.id);
        setError(null);
        try {
            if (isEnrolled) {
                await removeCourseAccess(user.id, course.id);
            } else {
                await addCourseAccess(user.id, course.id);
            }
            onRefresh(); // Parent should refresh user data
        } catch (err) {
            setError(`Не вдалося ${isEnrolled ? 'забрати' : 'додати'} доступ.`);
        } finally {
            setActionLoading(null);
        }
    };

    if (!isOpen || !user) return null;

    const enrolledCourseIds = new Set(user.enrollments.map(e => e.courseId));

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50">
            <div className="bg-white dark:bg-gray-800 rounded-lg shadow-lg w-full max-w-2xl p-6 max-h-[80vh] flex flex-col">
                <div className="flex justify-between items-center mb-4 shrink-0">
                    <div>
                        <h2 className="text-xl font-bold text-gray-900 dark:text-white">Управління доступом</h2>
                        <p className="text-sm text-gray-500 dark:text-gray-400">{user.firstName} {user.lastName}</p>
                    </div>
                    <button onClick={onClose} className="text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200">
                        <Icons.X size={24} />
                    </button>
                </div>

                {error && (
                    <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4 shrink-0">
                        {error}
                    </div>
                )}

                <div className="flex-1 overflow-y-auto min-h-0 border rounded-md border-gray-200 dark:border-gray-700">
                    {loading ? (
                        <div className="p-8 text-center text-gray-500">Завантаження курсів...</div>
                    ) : allCourses.length === 0 ? (
                        <div className="p-8 text-center text-gray-500">Курсів не знайдено.</div>
                    ) : (
                        <table className="min-w-full divide-y divide-gray-200 dark:divide-gray-700">
                            <thead className="bg-gray-50 dark:bg-gray-900 sticky top-0">
                                <tr>
                                    <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">Курс</th>
                                    <th scope="col" className="px-6 py-3 text-center text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider w-24">Статус</th>
                                    <th scope="col" className="px-6 py-3 text-right text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider w-20">Дія</th>
                                </tr>
                            </thead>
                            <tbody className="bg-white dark:bg-gray-800 divide-y divide-gray-200 dark:divide-gray-700">
                                {allCourses.map((course) => {
                                    const isEnrolled = enrolledCourseIds.has(course.id);
                                    const isLoading = actionLoading === course.id;

                                    return (
                                        <tr key={course.id} className={isEnrolled ? 'bg-blue-50/30' : ''}>
                                            <td className="px-6 py-4 whitespace-nowrap">
                                                <div className="text-sm font-medium text-gray-900 dark:text-white">{course.name}</div>
                                                <div className="text-xs text-gray-500 dark:text-gray-400">{course.modulesNumber} модулів</div>
                                            </td>
                                            <td className="px-6 py-4 whitespace-nowrap text-center">
                                                {isEnrolled ? (
                                                    <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800 border border-green-200">
                                                        Активний
                                                    </span>
                                                ) : (
                                                    <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-800 border border-gray-200">
                                                        Доступний
                                                    </span>
                                                )}
                                            </td>
                                            <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                                                <button
                                                    onClick={() => handleToggleAccess(course, isEnrolled)}
                                                    disabled={isLoading}
                                                    className={`p-2 rounded-full transition-colors ${isEnrolled
                                                            ? 'text-red-600 hover:bg-red-50 hover:text-red-900'
                                                            : 'text-green-600 hover:bg-green-50 hover:text-green-900'
                                                        } disabled:opacity-50 disabled:cursor-not-allowed`}
                                                    title={isEnrolled ? "Забрати доступ" : "Надати доступ"}
                                                >
                                                    {isLoading ? (
                                                        <Icons.Loader2 size={20} className="animate-spin" />
                                                    ) : isEnrolled ? (
                                                        <Icons.Trash2 size={20} />
                                                    ) : (
                                                        <Icons.PlusCircle size={20} />
                                                    )}
                                                </button>
                                            </td>
                                        </tr>
                                    );
                                })}
                            </tbody>
                        </table>
                    )}
                </div>

                <div className="flex justify-end mt-4 shrink-0">
                    <button
                        type="button"
                        onClick={onClose}
                        className="px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 hover:bg-gray-50 dark:text-gray-300 dark:hover:bg-gray-700"
                    >
                        Закрити
                    </button>
                </div>
            </div>
        </div>
    );
};
