import React, { useState, useEffect } from 'react';
import { type CreatePersonDto, getRoles } from '../api/users';
import { getCourses, type CourseDto } from '../api/courses';

interface CreateUserModalProps {
    isOpen: boolean;
    onClose: () => void;
    onSubmit: (data: CreatePersonDto) => Promise<void>;
}

export const CreateUserModal: React.FC<CreateUserModalProps> = ({ isOpen, onClose, onSubmit }) => {
    const [formData, setFormData] = useState<CreatePersonDto>({
        firstName: '',
        lastName: '',
        email: '',
        password: '',
        phoneNumber: '',
        role: 'USER',
        bornedAt: '',
        courseIds: []
    });
    const [loading, setLoading] = useState(false);
    const [roles, setRoles] = useState<string[]>([]);
    const [courses, setCourses] = useState<CourseDto[]>([]);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const fetchData = async () => {
            try {
                const [rolesData, coursesData] = await Promise.all([
                    getRoles(),
                    getCourses()
                ]);
                setRoles(rolesData);
                setCourses(coursesData);
            } catch (error) {
                console.error('Failed to fetch data', error);
                // Fallback to default roles if fetch fails
                setRoles(['USER', 'ADMIN']);
            }
        };
        if (isOpen) {
            fetchData();
        }
    }, [isOpen]);

    if (!isOpen) return null;

    const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
    };

    const handleCourseToggle = (courseId: string) => {
        setFormData(prev => {
            const currentIds = prev.courseIds || [];
            if (currentIds.includes(courseId)) {
                return { ...prev, courseIds: currentIds.filter(id => id !== courseId) };
            } else {
                return { ...prev, courseIds: [...currentIds, courseId] };
            }
        });
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true);
        setError(null);
        try {
            // Format date to ISO string if present
            const submitData = { ...formData };
            if (submitData.bornedAt) {
                submitData.bornedAt = new Date(submitData.bornedAt).toISOString();
            } else {
                delete submitData.bornedAt;
            }

            await onSubmit(submitData);
            onClose();
            // Reset form
            setFormData({
                firstName: '',
                lastName: '',
                email: '',
                password: '',
                phoneNumber: '',
                role: 'USER',
                bornedAt: '',
                courseIds: []
            });
        } catch (err) {
            setError('Не вдалося створити користувача. Перевірте введені дані.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50">
            <div className="bg-white rounded-lg shadow-lg w-full max-w-2xl p-6 max-h-[90vh] overflow-y-auto">
                <h2 className="text-2xl font-bold mb-4 text-gray-900">Створити нового користувача</h2>

                {error && (
                    <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4">
                        {error}
                    </div>
                )}

                <form onSubmit={handleSubmit} className="space-y-4">
                    <div className="grid grid-cols-2 gap-4">
                        <div>
                            <label className="block text-sm font-medium text-gray-700">Ім'я</label>
                            <input
                                type="text"
                                name="firstName"
                                value={formData.firstName}
                                onChange={handleChange}
                                required
                                className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 p-2 border"
                            />
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-gray-700">Прізвище</label>
                            <input
                                type="text"
                                name="lastName"
                                value={formData.lastName}
                                onChange={handleChange}
                                required
                                className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 p-2 border"
                            />
                        </div>
                    </div>

                    <div className="grid grid-cols-2 gap-4">
                        <div>
                            <label className="block text-sm font-medium text-gray-700">Email</label>
                            <input
                                type="email"
                                name="email"
                                value={formData.email}
                                onChange={handleChange}
                                required
                                className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 p-2 border"
                            />
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-gray-700">Номер телефону</label>
                            <input
                                type="tel"
                                name="phoneNumber"
                                value={formData.phoneNumber}
                                onChange={handleChange}
                                required
                                className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 p-2 border"
                            />
                        </div>
                    </div>

                    <div className="grid grid-cols-2 gap-4">
                        <div>
                            <label className="block text-sm font-medium text-gray-700">Пароль</label>
                            <input
                                type="password"
                                name="password"
                                value={formData.password}
                                onChange={handleChange}
                                required
                                className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 p-2 border"
                            />
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-gray-700">Дата народження</label>
                            <input
                                type="date"
                                name="bornedAt"
                                value={formData.bornedAt}
                                onChange={handleChange}
                                className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 p-2 border"
                            />
                        </div>
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-gray-700">Роль</label>
                        <select
                            name="role"
                            value={formData.role}
                            onChange={handleChange}
                            className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 p-2 border"
                        >
                            {roles.map(role => (
                                <option key={role} value={role}>{role}</option>
                            ))}
                        </select>
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">Надати доступ до курсів</label>
                        <div className="border rounded-md p-4 max-h-48 overflow-y-auto bg-gray-50">
                            {courses.length === 0 ? (
                                <p className="text-gray-500 text-sm">Немає доступних курсів</p>
                            ) : (
                                <div className="space-y-2">
                                    {courses.map(course => (
                                        <div key={course.id} className="flex items-center">
                                            <input
                                                id={`course-${course.id}`}
                                                type="checkbox"
                                                checked={formData.courseIds?.includes(course.id)}
                                                onChange={() => handleCourseToggle(course.id)}
                                                className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                                            />
                                            <label htmlFor={`course-${course.id}`} className="ml-2 block text-sm text-gray-900">
                                                {course.name}
                                            </label>
                                        </div>
                                    ))}
                                </div>
                            )}
                        </div>
                    </div>

                    <div className="flex justify-end space-x-3 mt-6">
                        <button
                            type="button"
                            onClick={onClose}
                            className="px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 hover:bg-gray-50"
                        >
                            Скасувати
                        </button>
                        <button
                            type="submit"
                            disabled={loading}
                            className="px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50"
                        >
                            {loading ? 'Створення...' : 'Створити'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};
