import { useQuery } from '@tanstack/react-query';
import { getCourses } from '../api/courses';
import { BookOpen, Clock, ArrowRight } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useState } from 'react';
import { PaymentModal } from '../components/modals/PaymentModal';

export default function CatalogPage() {
    const { t } = useTranslation();
    const { data: courses, isLoading } = useQuery({ queryKey: ['courses'], queryFn: () => getCourses() });
    const navigate = useNavigate();

    const [selectedCourse, setSelectedCourse] = useState<any>(null);

    const handleEnroll = (courseId: string) => {
        const course = courses?.find(c => c.id === courseId);
        if (course) {
            setSelectedCourse(course);
        }
    };

    const handlePaymentSuccess = () => {
        setSelectedCourse(null);
        navigate('/dashboard/my-courses');
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
                            <div className="flex justify-end items-start mb-6">
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


            {selectedCourse && (
                <PaymentModal
                    isOpen={!!selectedCourse}
                    onClose={() => setSelectedCourse(null)}
                    courseId={selectedCourse.id}
                    courseName={selectedCourse.name}
                    price={selectedCourse.price || 0}
                    onSuccess={handlePaymentSuccess}
                />
            )}
        </div>
    );
}
