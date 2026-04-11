import { useQuery } from '@tanstack/react-query';
import { getCourses } from '../../api/courses';
import { getModules } from '../../api/modules';
import CourseExpandableCard from '../CourseExpandableCard';
import { motion } from 'framer-motion';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';

export default function CoursesSection() {
    const { t } = useTranslation();
    const navigate = useNavigate();

    const { data: courses, isLoading: coursesLoading } = useQuery({
        queryKey: ['landingCourses'],
        queryFn: () => getCourses()
    });

    const { data: modules } = useQuery({
        queryKey: ['landingModules'],
        queryFn: () => getModules()
    });

    // Only show published courses on landing page
    const publishedCourses = courses?.filter(c => c.status === 'PUBLISHED') || [];

    const handleEnroll = (courseId: string) => {
        // Store intent
        localStorage.setItem('pendingEnrollment', courseId);
        
        const token = localStorage.getItem('token');
        if (token) {
            navigate('/dashboard/my-courses');
        } else {
            navigate('/login?redirect=/dashboard/my-courses');
        }
    };

    return (
        <section id="courses" className="py-24 lg:py-32 relative text-stone-800 bg-[#FFF9F8]">
            <div className="absolute top-20 left-10 w-64 h-64 bg-brand-primary/5 rounded-full blur-3xl" />
            
            <div className="max-w-5xl mx-auto px-6 relative z-10">
                <motion.div
                    initial={{ opacity: 0, y: 20 }}
                    whileInView={{ opacity: 1, y: 0 }}
                    viewport={{ once: true }}
                    transition={{ duration: 0.6 }}
                    className="text-center mb-16"
                >
                    <span className="inline-block text-brand-primary font-semibold text-sm tracking-wider uppercase mb-3">
                        {t('landing.coursesSubtitle', 'Наші курси')}
                    </span>
                    <h2 className="font-sans text-3xl sm:text-4xl lg:text-5xl font-bold">
                        {t('landing.coursesTitle', 'Доступні курси')}
                    </h2>
                    <p className="mt-4 text-stone-500 max-w-lg mx-auto">
                        {t('landing.coursesDesc', 'Оберіть курс, який підходить саме вам')}
                    </p>
                </motion.div>

                {coursesLoading ? (
                    <div className="flex justify-center py-12">
                        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-brand-primary"></div>
                    </div>
                ) : publishedCourses.length === 0 ? (
                    <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-12 text-center max-w-2xl mx-auto">
                        <p className="text-gray-500 text-lg">{t('landing.noCourses', 'Наразі немає доступних курсів')}</p>
                    </div>
                ) : (
                    <div className="flex flex-col space-y-6 max-w-4xl mx-auto">
                        {publishedCourses.map((course, index) => (
                            <motion.div
                                key={course.id}
                                initial={{ opacity: 0, y: 30 }}
                                whileInView={{ opacity: 1, y: 0 }}
                                viewport={{ once: true }}
                                transition={{ duration: 0.6, delay: index * 0.15 }}
                            >
                                <CourseExpandableCard
                                    course={course}
                                    modules={modules || []}
                                    onEdit={() => {}}
                                    onDelete={() => {}}
                                    onEnroll={handleEnroll} // This will trigger when clicking "Придбати"
                                    isCatalogMode={true}
                                />
                            </motion.div>
                        ))}
                    </div>
                )}
            </div>
        </section>
    );
}
