import { useQuery, useMutation } from '@tanstack/react-query';
import { getCourses } from '../api/courses';
import { enrollInCourse } from '../api/enrollments';
import { BookOpen, Clock, ArrowRight } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

export default function CatalogPage() {
    const { data: courses, isLoading } = useQuery({ queryKey: ['courses'], queryFn: getCourses });
    const navigate = useNavigate();

    // Get user from local storage
    const userStr = localStorage.getItem('user');
    const user = userStr ? JSON.parse(userStr) : null;

    const enrollMutation = useMutation({
        mutationFn: (courseId: string) => {
            if (!user) throw new Error("User not logged in");
            return enrollInCourse(user.userId, courseId);
        },
        onSuccess: () => {
            alert('Enrolled successfully!');
        },
        onError: (error: any) => {
            alert('Failed to enroll: ' + (error.response?.data?.message || 'Unknown error'));
        }
    });

    const handleEnroll = (courseId: string) => {
        if (!user) {
            navigate('/login');
            return;
        }
        enrollMutation.mutate(courseId);
    };

    if (isLoading) return <div className="min-h-screen flex items-center justify-center text-brand-primary font-medium">Loading courses...</div>;

    return (
        <div className="min-h-screen bg-white font-sans">
            <div className="bg-brand-light/30 py-20">
                <div className="container mx-auto px-6 text-center">
                    <h1 className="text-4xl md:text-5xl font-bold text-brand-dark mb-4">Explore Our Courses</h1>
                    <p className="text-gray-600 max-w-2xl mx-auto text-lg">Find the perfect course to start your new journey.</p>
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
                                        <span>{course.modulesNumber || 0} Modules</span>
                                    </div>
                                    <div className="flex items-center gap-2">
                                        <Clock className="w-4 h-4 text-brand-secondary" />
                                        <span>Flexible</span>
                                    </div>
                                </div>

                                <button
                                    onClick={() => handleEnroll(course.id)}
                                    className="w-full py-3.5 px-4 rounded-full bg-brand-dark text-white font-bold hover:bg-brand-primary transition-all flex items-center justify-center gap-2 group"
                                >
                                    <span>Enroll Now</span>
                                    <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
                                </button>
                            </div>
                        </div>
                    ))}
                </div>
            </div>
        </div>
    );
}
