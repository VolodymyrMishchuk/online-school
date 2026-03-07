import { useQuery, useMutation } from '@tanstack/react-query';
import { getCourses } from '../api/courses';
import { enrollInCourse } from '../api/enrollments';
import { BookOpen, Clock, ArrowRight } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useState } from 'react';
import { ConfirmModal } from '../components/ConfirmModal';

export default function CatalogPage() {
    const { t } = useTranslation();
    const { data: courses, isLoading } = useQuery({ queryKey: ['courses'], queryFn: () => getCourses() });
    const navigate = useNavigate();

    // Alert Modal State
    const [isAlertOpen, setIsAlertOpen] = useState(false);
    const [alertMessage, setAlertMessage] = useState('');
    const [alertTitle, setAlertTitle] = useState('');
    const [alertType, setAlertType] = useState<'info' | 'warning' | 'danger'>('warning');

    const showAlert = (message: string, type: 'info' | 'warning' | 'danger' = 'warning', title = t('common.notification', 'Сповіщення')) => {
        setAlertMessage(message);
        setAlertType(type);
        setAlertTitle(title);
        setIsAlertOpen(true);
    };

    // Get user from local storage
    const userStr = localStorage.getItem('user');
    const user = userStr ? JSON.parse(userStr) : null;

    const enrollMutation = useMutation({
        mutationFn: (courseId: string) => {
            if (!user) throw new Error("User not logged in");
            return enrollInCourse(user.userId, courseId);
        },
        onSuccess: () => {
            showAlert(t('catalog.enrollSuccess', 'Ви успішно записалися!'), 'info', t('common.success', 'Успіх'));
        },
        onError: (error: any) => {
            showAlert(t('catalog.enrollError', 'Помилка при записі: ') + (error.response?.data?.message || t('common.unknownError', 'Невідома помилка')));
        }
    });

    const handleEnroll = (courseId: string) => {
        if (!user) {
            navigate('/login');
            return;
        }
        enrollMutation.mutate(courseId);
    };

    if (isLoading) return <div className="min-h-screen flex items-center justify-center text-brand-primary font-medium">{t('common.loading', 'Завантаження...')}</div>;

    return (
        <div className="min-h-screen bg-white font-sans">
            <div className="bg-brand-light/30 py-20">
                <div className="container mx-auto px-6 text-center">
                    <h1 className="text-4xl md:text-5xl font-bold text-brand-dark mb-4">{t('catalog.title', 'Досліджуйте наші курси')}</h1>
                    <p className="text-gray-600 max-w-2xl mx-auto text-lg">{t('catalog.subtitle', 'Знайдіть ідеальний курс, щоб почати свою нову подорож.')}</p>
                </div>
            </div>

            <div className="container mx-auto px-6 -mt-10 pb-20">
                <div className="grid gap-8 md:grid-cols-2 lg:grid-cols-3">
                    {courses?.map((course) => (
                        <div key={course.id} className="bg-white rounded-3xl p-6 shadow-sm border border-gray-100 hover:shadow-xl hover:border-brand-light transition-all duration-300 flex flex-col h-full">
                            <div className="flex justify-between items-start mb-6">
                                <div className={`px-3 py-1 rounded-full text-xs font-bold uppercase tracking-wide ${course.status === 'PUBLISHED' ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-600'}`}>
                                    {course.status}
                                </div>
                                {/* Placeholder for difficulty or category icon */}
                            </div>

                            <h3 className="text-2xl font-bold text-brand-dark mb-3 line-clamp-2">{course.name}</h3>
                            <p className="text-gray-500 mb-6 line-clamp-3 leading-relaxed flex-grow">{course.description}</p>

                            <div className="pt-6 border-t border-gray-50 mt-auto">
                                <div className="flex items-center justify-between text-sm text-gray-500 mb-6">
                                    <div className="flex items-center gap-2">
                                        <BookOpen className="w-4 h-4 text-brand-secondary" />
                                        <span>{t('catalog.modulesCount', '{{count}} Модулів', { count: course.modulesNumber || 0 })}</span>
                                    </div>
                                    <div className="flex items-center gap-2">
                                        <Clock className="w-4 h-4 text-brand-secondary" />
                                        <span>{t('catalog.flexible', 'Гнучкий графік')}</span>
                                    </div>
                                </div>
                                <button
                                    onClick={() => handleEnroll(course.id)}
                                    className="w-full py-3.5 px-4 rounded-full bg-brand-dark text-white font-bold hover:bg-brand-primary transition-all flex items-center justify-center gap-2 group"
                                >
                                    <span>{t('catalog.enrollBtn', 'Записатися зараз')}</span>
                                    <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
                                </button>
                            </div>
                        </div>
                    ))}
                </div>
            </div>

            <ConfirmModal
                isOpen={isAlertOpen}
                onClose={() => setIsAlertOpen(false)}
                onConfirm={() => setIsAlertOpen(false)}
                title={alertTitle}
                message={alertMessage}
                isAlert={true}
                type={alertType}
            />
        </div>
    );
}
