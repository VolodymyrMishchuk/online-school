import React, { useState, useEffect } from 'react';
import { type CreatePersonDto, getRoles } from '../api/users';
import { getCourses, type CourseDto } from '../api/courses';
import { UserPlus, X, User, Mail, Phone, Lock, Calendar, Shield, BookOpen, AlertCircle, CheckCircle } from 'lucide-react';

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

    const resetState = () => {
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
        setError(null);
        setLoading(false);
    };

    const handleClose = () => {
        onClose();
        setTimeout(resetState, 300);
    };

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
            handleClose();
        } catch (err) {
            setError('Не вдалося створити користувача. Перевірте введені дані.');
            setLoading(false);
        }
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-white/30 backdrop-blur-md animate-in fade-in duration-200">
            {/* Glass Panel Modal */}
            <div
                className="glass-panel w-full max-w-2xl flex flex-col overflow-hidden animate-in zoom-in-95 duration-200 relative rounded-lg shadow-xl"
                style={{ background: 'rgba(255, 255, 255, 0.9)', maxHeight: '90vh' }}
            >
                {/* Header Bar - Static, Lighter (bg-white) */}
                <div className="flex items-center justify-between px-6 py-5 border-b border-gray-100 bg-white shrink-0 z-10 relative shadow-sm">
                    <div className="flex items-center gap-3">
                        <div className="flex items-center justify-center w-10 h-10 rounded-full bg-brand-light/50 text-brand-primary ring-2 ring-white shadow-sm">
                            <UserPlus className="w-5 h-5" />
                        </div>
                        <h2 className="text-xl font-bold text-brand-dark">Створити нового користувача</h2>
                    </div>
                    <button
                        onClick={handleClose}
                        className="p-2 text-gray-400 hover:text-brand-primary hover:bg-white/50 rounded-lg transition-all"
                    >
                        <X className="w-5 h-5" />
                    </button>
                </div>

                {/* Body - Scrollable */}
                <div className="flex-1 overflow-y-auto px-8 py-6 custom-scrollbar flex flex-col gap-6">
                    {error && (
                        <div className="flex items-center gap-2 p-3 text-sm text-red-600 bg-red-50 rounded-lg border border-red-100">
                            <AlertCircle className="w-4 h-4 shrink-0" />
                            {error}
                        </div>
                    )}

                    <form id="create-user-form" onSubmit={handleSubmit} className="space-y-6">
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                            {/* First Name */}
                            <div>
                                <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2 ml-1">
                                    <User className="w-4 h-4" />
                                    Ім'я
                                </label>
                                <input
                                    type="text"
                                    name="firstName"
                                    value={formData.firstName}
                                    onChange={handleChange}
                                    required
                                    placeholder="Іван"
                                    className="w-full px-4 py-3 rounded-lg border border-gray-200 bg-white/50 outline-none transition-all focus:border-brand-primary focus:ring-2 focus:ring-brand-light focus:bg-white"
                                />
                            </div>
                            {/* Last Name */}
                            <div>
                                <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2 ml-1">
                                    <User className="w-4 h-4" />
                                    Прізвище
                                </label>
                                <input
                                    type="text"
                                    name="lastName"
                                    value={formData.lastName}
                                    onChange={handleChange}
                                    required
                                    placeholder="Петренко"
                                    className="w-full px-4 py-3 rounded-lg border border-gray-200 bg-white/50 outline-none transition-all focus:border-brand-primary focus:ring-2 focus:ring-brand-light focus:bg-white"
                                />
                            </div>
                        </div>

                        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                            {/* Email */}
                            <div>
                                <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2 ml-1">
                                    <Mail className="w-4 h-4" />
                                    Email
                                </label>
                                <input
                                    type="email"
                                    name="email"
                                    value={formData.email}
                                    onChange={handleChange}
                                    required
                                    placeholder="ivan@example.com"
                                    className="w-full px-4 py-3 rounded-lg border border-gray-200 bg-white/50 outline-none transition-all focus:border-brand-primary focus:ring-2 focus:ring-brand-light focus:bg-white"
                                />
                            </div>
                            {/* Phone */}
                            <div>
                                <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2 ml-1">
                                    <Phone className="w-4 h-4" />
                                    Номер телефону
                                </label>
                                <input
                                    type="tel"
                                    name="phoneNumber"
                                    value={formData.phoneNumber}
                                    onChange={handleChange}
                                    required
                                    placeholder="+380..."
                                    className="w-full px-4 py-3 rounded-lg border border-gray-200 bg-white/50 outline-none transition-all focus:border-brand-primary focus:ring-2 focus:ring-brand-light focus:bg-white"
                                />
                            </div>
                        </div>

                        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                            {/* Password */}
                            <div>
                                <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2 ml-1">
                                    <Lock className="w-4 h-4" />
                                    Пароль
                                </label>
                                <input
                                    type="password"
                                    name="password"
                                    value={formData.password}
                                    onChange={handleChange}
                                    required
                                    placeholder="••••••••"
                                    className="w-full px-4 py-3 rounded-lg border border-gray-200 bg-white/50 outline-none transition-all focus:border-brand-primary focus:ring-2 focus:ring-brand-light focus:bg-white"
                                />
                            </div>
                            {/* Birth Date */}
                            <div>
                                <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2 ml-1">
                                    <Calendar className="w-4 h-4" />
                                    Дата народження
                                </label>
                                <input
                                    type="date"
                                    name="bornedAt"
                                    value={formData.bornedAt}
                                    onChange={handleChange}
                                    className="w-full px-4 py-3 rounded-lg border border-gray-200 bg-white/50 outline-none transition-all focus:border-brand-primary focus:ring-2 focus:ring-brand-light focus:bg-white"
                                />
                            </div>
                        </div>

                        {/* Role */}
                        <div>
                            <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2 ml-1">
                                <Shield className="w-4 h-4" />
                                Роль
                            </label>
                            <div className="relative">
                                <select
                                    name="role"
                                    value={formData.role}
                                    onChange={handleChange}
                                    className="w-full px-4 py-3 rounded-lg border border-gray-200 bg-white/50 outline-none transition-all focus:border-brand-primary focus:ring-2 focus:ring-brand-light focus:bg-white appearance-none"
                                >
                                    {roles.map(role => (
                                        <option key={role} value={role}>{role}</option>
                                    ))}
                                </select>
                                <div className="absolute inset-y-0 right-0 flex items-center px-4 pointer-events-none text-gray-500">
                                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 9l-7 7-7-7" />
                                    </svg>
                                </div>
                            </div>
                        </div>

                        {/* Courses Access */}
                        <div>
                            <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2 ml-1">
                                <BookOpen className="w-4 h-4" />
                                Надати доступ до курсів
                            </label>
                            <div className="border border-gray-200 rounded-lg p-4 max-h-48 overflow-y-auto bg-white/50 custom-scrollbar">
                                {courses.length === 0 ? (
                                    <p className="text-gray-500 text-sm italic text-center py-2">Немає доступних курсів</p>
                                ) : (
                                    <div className="space-y-3">
                                        {courses.map(course => (
                                            <label key={course.id} className="flex items-center p-2 rounded-lg hover:bg-white transition-colors cursor-pointer group">
                                                <div className="relative flex items-center">
                                                    <input
                                                        type="checkbox"
                                                        checked={formData.courseIds?.includes(course.id)}
                                                        onChange={() => handleCourseToggle(course.id)}
                                                        className="peer h-5 w-5 cursor-pointer appearance-none rounded-md border border-gray-300 transition-all checked:border-brand-primary checked:bg-brand-primary group-hover:border-brand-primary"
                                                    />
                                                    <CheckCircle className="absolute pointer-events-none opacity-0 peer-checked:opacity-100 text-white w-3.5 h-3.5 left-[3px] top-[3px]" />
                                                </div>
                                                <span className="ml-3 text-sm text-gray-700 group-hover:text-brand-dark transition-colors">
                                                    {course.name}
                                                </span>
                                            </label>
                                        ))}
                                    </div>
                                )}
                            </div>
                        </div>
                    </form>
                </div>

                {/* Footer - Static */}
                <div className="flex gap-4 p-6 border-t border-gray-100 bg-white/50 backdrop-blur-sm shrink-0">
                    <button
                        type="button"
                        onClick={handleClose}
                        className="flex-1 py-3 font-bold text-brand-primary bg-white hover:bg-brand-primary hover:text-white rounded-lg transition-colors shadow-sm border border-gray-100"
                    >
                        Скасувати
                    </button>
                    <button
                        type="submit"
                        form="create-user-form"
                        disabled={loading}
                        className="flex-1 py-3 font-bold text-white bg-brand-primary hover:bg-brand-secondary rounded-lg transition-all shadow-lg hover:shadow-xl transform active:scale-95 duration-200 disabled:opacity-70 disabled:transform-none"
                    >
                        {loading ? (
                            <div className="flex items-center justify-center gap-2">
                                <div className="animate-spin rounded-full h-4 w-4 border-2 border-white/30 border-t-white"></div>
                                <span>Створення...</span>
                            </div>
                        ) : 'Створити'}
                    </button>
                </div>
            </div>
        </div>
    );
};
