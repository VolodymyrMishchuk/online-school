import { useQuery } from '@tanstack/react-query';
import { getCourses } from '../api/courses';
import { getModules } from '../api/modules';
// import { getLessons } from '../api/lessons';
import { useNavigate } from 'react-router-dom';
import { useEffect } from 'react';
import { Book, GraduationCap } from 'lucide-react';
import CourseExpandableCard from '../components/CourseExpandableCard';

export default function MyCoursesPage() {
    const navigate = useNavigate();
    const userStr = localStorage.getItem('user');
    const user = userStr ? JSON.parse(userStr) : null;
    const userId = user?.userId || localStorage.getItem('userId') || '';

    useEffect(() => {
        if (!userId) {
            navigate('/login');
        }
    }, [userId, navigate]);

    const { data: allCourses, isLoading: coursesLoading } = useQuery({
        queryKey: ['allCourses', userId],
        queryFn: () => getCourses(userId),
        enabled: !!userId
    });

    const { data: modules } = useQuery({
        queryKey: ['allModules'],
        queryFn: () => getModules()
    });


    // Filter only enrolled courses
    const enrolledCourses = allCourses?.filter(course => course.isEnrolled) || [];

    if (!userId) return null;
    if (coursesLoading) {
        return (
            <div className="flex items-center justify-center min-h-screen">
                <div className="text-brand-primary font-medium">Завантаження...</div>
            </div>
        );
    }

    return (
        <div className="container mx-auto px-6 py-12">
            <div className="flex items-center gap-3 mb-8">
                <GraduationCap className="w-8 h-8 text-brand-primary" />
                <h1 className="text-3xl font-bold text-brand-dark">Мої курси</h1>
                <span className="text-gray-400 font-medium">({enrolledCourses.length})</span>
            </div>

            {enrolledCourses.length === 0 ? (
                <div className="bg-white rounded-3xl shadow-sm border border-gray-100 p-16 text-center">
                    <div className="w-20 h-20 bg-brand-light/50 rounded-full flex items-center justify-center mx-auto mb-6">
                        <Book className="w-10 h-10 text-brand-secondary" />
                    </div>
                    <h3 className="text-2xl font-bold text-brand-dark mb-2">Почніть своє навчання</h3>
                    <p className="text-gray-500 max-w-md mx-auto mb-8">
                        Ви ще не записалися на жоден курс. Перегляньте каталог і знайдіть курс, який вас надихне.
                    </p>
                    <button
                        onClick={() => navigate('/dashboard/all-courses')}
                        className="px-8 py-3 rounded-full bg-brand-primary text-white font-bold hover:bg-brand-secondary transition-all shadow-lg hover:shadow-xl hover:-translate-y-1"
                    >
                        Переглянути курси
                    </button>
                </div>
            ) : (
                <div className="flex flex-col gap-6">
                    {enrolledCourses.map((course) => {
                        return (
                            <CourseExpandableCard
                                key={course.id}
                                course={course}
                                modules={modules || []}
                                onEdit={() => { }} // Students cannot edit courses
                                onDelete={() => { }} // Students cannot delete courses
                                onEnroll={undefined} // Already enrolled
                                // Lesson handlers can be undefined for now
                                onEditLesson={undefined}
                                onDeleteLesson={undefined}
                            />
                        );
                    })}
                </div>
            )}
        </div>
    );
}
