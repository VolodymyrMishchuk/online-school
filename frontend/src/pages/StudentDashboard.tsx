import { useQuery } from '@tanstack/react-query';
import { getMyEnrollments } from '../api/enrollments';
import { useNavigate } from 'react-router-dom';
import { useEffect } from 'react';
import { PlayCircle, LogOut, Book, Heart } from 'lucide-react';

export default function StudentDashboard() {
    const navigate = useNavigate();
    // Get user from local storage
    const userStr = localStorage.getItem('user');
    const user = userStr ? JSON.parse(userStr) : null;

    useEffect(() => {
        if (!user) {
            navigate('/login');
        }
    }, [user, navigate]);

    const { data: enrollments, isLoading } = useQuery({
        queryKey: ['myEnrollments', user?.userId],
        queryFn: () => getMyEnrollments(user.userId),
        enabled: !!user?.userId
    });

    if (!user) return null;
    if (isLoading) return <div className="min-h-screen flex items-center justify-center text-brand-primary font-medium">Loading dashboard...</div>;

    return (
        <div className="min-h-screen bg-gray-50 font-sans">
            {/* Dashboard Header */}
            <div className="bg-white border-b border-gray-100">
                <div className="container mx-auto px-6 py-5 flex justify-between items-center">
                    <div className="flex items-center gap-3">
                        <div className="bg-brand-light p-2 rounded-full">
                            <Heart className="w-5 h-5 text-brand-secondary fill-brand-secondary" />
                        </div>
                        <h1 className="text-xl font-bold text-brand-dark">My Learning Dashboard</h1>
                    </div>

                    <div className="flex items-center gap-4">
                        <button onClick={() => navigate('/catalog')} className="text-sm font-medium text-gray-500 hover:text-brand-primary transition-colors">Browse Catalog</button>
                        <div className="h-4 w-px bg-gray-200"></div>
                        <button
                            onClick={() => { localStorage.clear(); navigate('/'); }}
                            className="flex items-center gap-2 text-sm font-medium text-red-500 hover:text-red-600 transition-colors"
                        >
                            <LogOut className="w-4 h-4" />
                            Logout
                        </button>
                    </div>
                </div>
            </div>

            <div className="container mx-auto px-6 py-12">
                <div className="flex items-end gap-2 mb-8">
                    <h2 className="text-3xl font-bold text-brand-dark">Your Courses</h2>
                    <span className="text-gray-400 font-medium mb-1">({enrollments?.length || 0})</span>
                </div>

                {enrollments?.length === 0 ? (
                    <div className="bg-white rounded-3xl shadow-sm border border-gray-100 p-16 text-center">
                        <div className="w-20 h-20 bg-brand-light/50 rounded-full flex items-center justify-center mx-auto mb-6">
                            <Book className="w-10 h-10 text-brand-secondary" />
                        </div>
                        <h3 className="text-2xl font-bold text-brand-dark mb-2">Start Your Learning Journey</h3>
                        <p className="text-gray-500 max-w-md mx-auto mb-8">You haven't enrolled in any courses yet. Browse our catalog to find a course that inspires you.</p>

                        <button
                            onClick={() => navigate('/catalog')}
                            className="px-8 py-3 rounded-full bg-brand-primary text-white font-bold hover:bg-brand-secondary transition-all shadow-lg hover:shadow-xl hover:-translate-y-1"
                        >
                            Browse Catalog
                        </button>
                    </div>
                ) : (
                    <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
                        {enrollments?.map((enrollment) => (
                            <div key={enrollment.id} className="bg-white rounded-3xl p-6 shadow-sm border border-gray-100 hover:shadow-lg transition-all flex flex-col group">
                                <div className="flex justify-between items-start mb-4">
                                    <span className={`inline-flex items-center px-3 py-1 rounded-full text-xs font-bold uppercase tracking-wide ${enrollment.status === 'ACTIVE' ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-600'}`}>
                                        {enrollment.status}
                                    </span>
                                </div>

                                <h3 className="text-xl font-bold text-brand-dark mb-2">Course ID: {enrollment.courseId}</h3>
                                <div className="h-1 w-full bg-gray-100 rounded-full overflow-hidden mb-6 mt-auto">
                                    <div className="h-full bg-brand-secondary w-0 group-hover:w-1/3 transition-all duration-1000"></div>
                                </div>

                                <button className="w-full flex items-center justify-center gap-2 py-3 px-4 rounded-xl bg-gray-50 text-brand-dark font-bold hover:bg-brand-light hover:text-brand-secondary transition-colors">
                                    <PlayCircle className="w-5 h-5" />
                                    Continue
                                </button>
                            </div>
                        ))}
                    </div>
                )}
            </div>
        </div>
    );
}
