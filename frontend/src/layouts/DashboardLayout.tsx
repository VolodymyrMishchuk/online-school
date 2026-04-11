import { Outlet, NavLink, useNavigate } from 'react-router-dom';
import { BookOpen, FileText, GraduationCap, Heart, LogOut, Settings, FolderOpen, Users, Bell, MessageSquare, Ticket } from 'lucide-react';
import { getPerson } from '../api/persons';
import { enrollInCourse } from '../api/enrollments';
import { useQueryClient } from '@tanstack/react-query';

import { useEffect, useState, useRef } from 'react';
import { getUnreadCount } from '../api/notifications';
import { ScrollToTop } from '../components/ScrollToTop';
import { LanguageSwitcher } from '../components/LanguageSwitcher';
import { useTranslation } from 'react-i18next';

export default function DashboardLayout() {
    const navigate = useNavigate();
    const queryClient = useQueryClient();
    const mainRef = useRef<HTMLElement>(null);
    const [user, setUser] = useState<any>(() => {
        // Only on initial load: check if URL contains OAuth2 redirect parameters
        const searchParams = new URLSearchParams(window.location.search);
        const token = searchParams.get('token');
        const searchUserId = searchParams.get('userId');
        
        if (token && searchUserId) {
            const role = searchParams.get('role') || 'USER';
            const firstName = searchParams.get('firstName') || '';
            const lastName = searchParams.get('lastName') || '';
            const email = searchParams.get('email') || '';
            const avatarUrl = searchParams.get('avatarUrl') || '';
            
            const oauthUser = { userId: searchUserId, role, firstName, lastName, email, avatarUrl };
            
            // Save to localStorage
            localStorage.setItem('token', token);
            localStorage.setItem('userId', searchUserId);
            localStorage.setItem('userRole', role);
            localStorage.setItem('user', JSON.stringify(oauthUser));
            
            // Clean up the URL to remove the sensitive parameters
            window.history.replaceState({}, document.title, window.location.pathname);
            
            return oauthUser;
        }

        const userStr = localStorage.getItem('user');
        return userStr ? JSON.parse(userStr) : null;
    });
    const [unreadCount, setUnreadCount] = useState(0);
    const { t, i18n } = useTranslation();

    const userRole = localStorage.getItem('userRole') || 'USER';
    const isAdmin = userRole === 'ADMIN' || userRole === 'FAKE_ADMIN';

    const handleLogout = () => {
        localStorage.clear();
        navigate('/');
    };

    const fetchUnreadCount = async () => {
        const token = localStorage.getItem('token');
        if (!token) return;

        try {
            const count = await getUnreadCount();
            setUnreadCount(count);
        } catch (error) {
            console.error('Failed to fetch unread count', error);
        }
    };

    const fetchUserProfile = async () => {
        const token = localStorage.getItem('token');
        const userId = localStorage.getItem('userId');
        if (!token || !userId) return;

        try {
            const data = await getPerson(userId);

            // Sync language if different
            if (data.language && data.language !== i18n.language) {
                i18n.changeLanguage(data.language);
            }

            // Sync user object
            const updatedUser = { ...user, ...data };
            setUser(updatedUser);
            localStorage.setItem('user', JSON.stringify(updatedUser));
        } catch (error) {
            console.error('Failed to fetch user profile in layout', error);
        }
    };

    useEffect(() => {
        fetchUnreadCount();
        fetchUserProfile();

        // Optional: Set up interval to refresh count periodically
        const interval = setInterval(fetchUnreadCount, 60000); // Every minute
        return () => clearInterval(interval);
    }, []);

    // Handle pending enrollments (e.g. from landing page)
    useEffect(() => {
        const pendingCourseId = localStorage.getItem('pendingEnrollment');
        const id = localStorage.getItem('userId');
        const tokenStr = localStorage.getItem('token');
        
        if (pendingCourseId && id && tokenStr) {
            localStorage.removeItem('pendingEnrollment'); // clear to prevent loop
            enrollInCourse(id, pendingCourseId)
                .then(() => {
                    queryClient.invalidateQueries({ queryKey: ['myCourses'] });
                })
                .catch(err => {
                    console.error('Failed to process pending enrollment:', err);
                });
        }
    }, [user, queryClient]);

    const navItems = [
        { to: '/dashboard/all-courses', icon: BookOpen, label: t('sidebar.all_courses') },
        ...(isAdmin ? [
            { to: '/dashboard/all-modules', icon: FolderOpen, label: t('sidebar.all_modules') },
            { to: '/dashboard/all-lessons', icon: FileText, label: t('sidebar.all_lessons') },
            { to: '/dashboard/users', icon: Users, label: t('sidebar.users') },
            { to: '/dashboard/appeals', icon: MessageSquare, label: t('sidebar.appeals') },
        ] : [
            { to: '/dashboard/appeal', icon: MessageSquare, label: t('sidebar.contact') }
        ]),
        { to: '/dashboard/my-courses', icon: GraduationCap, label: t('sidebar.my_courses') },
        { to: '/dashboard/promo-codes', icon: Ticket, label: t('sidebar.promo_codes') },
        { to: '/dashboard/notifications', icon: Bell, label: t('sidebar.notifications') },
        { to: '/dashboard/settings', icon: Settings, label: t('sidebar.settings') },
    ];

    return (
        <div className="h-screen overflow-hidden flex bg-transparent">
            {/* Sidebar */}
            <aside className="w-64 glass-sidebar relative flex flex-col z-20 m-4 rounded-3xl h-[calc(100vh-2rem)] transition-all duration-300">
                {/* Language Switcher - Top Right */}
                <div className="absolute top-0 right-0 z-50">
                    <LanguageSwitcher />
                </div>

                {/* Logo/Header */}
                <div className="p-6 border-b border-white/20">
                    <div className="flex items-center gap-3">
                        {user?.avatarUrl ? (
                            <img src={user.avatarUrl} alt="Avatar" className="w-10 h-10 rounded-full object-cover shadow-sm bg-white" referrerPolicy="no-referrer" />
                        ) : (
                            <div className="bg-white/50 p-2.5 rounded-full backdrop-blur-sm">
                                <Heart className="w-5 h-5 text-brand-secondary fill-brand-secondary" />
                            </div>
                        )}
                        <div>
                            <h2 className="font-bold text-gray-800 text-lg">{t('sidebar.dashboardTitle', 'Дошбоард')}</h2>
                            <p className="text-xs text-gray-500 font-medium">
                                {user?.firstName && user?.lastName
                                    ? `${user.firstName} ${user.lastName}`
                                    : (user?.email || t('sidebar.user', 'Користувач'))}
                            </p>
                        </div>
                    </div>
                </div>

                {/* Navigation */}
                <nav className="flex-1 p-4 space-y-2">
                    {navItems.map((item) => (
                        <NavLink
                            key={item.to}
                            to={item.to}
                            className={({ isActive }) =>
                                `flex items-center gap-3 px-4 py-3 rounded-xl font-medium transition-all duration-200 ${isActive
                                    ? 'bg-brand-primary/90 text-white shadow-lg shadow-brand-primary/30 backdrop-blur-md'
                                    : 'text-gray-600 hover:bg-white/40 hover:text-brand-primary'
                                }`
                            }
                        >
                            <div className="relative">
                                <item.icon className="w-5 h-5" />
                                {item.label === t('sidebar.notifications') && unreadCount > 0 && (
                                    <span className="absolute -top-1 -right-1 w-2 h-2 bg-red-500 rounded-full animate-pulse" />
                                )}
                            </div>
                            <span>{item.label}</span>
                            {item.label === t('sidebar.notifications') && unreadCount > 0 && (
                                <span className="ml-auto bg-red-500 text-white text-[10px] font-bold px-1.5 py-0.5 rounded-full min-w-[18px] text-center">
                                    {unreadCount > 99 ? '99+' : unreadCount}
                                </span>
                            )}
                        </NavLink>
                    ))}
                </nav>

                {/* Bottom Actions */}
                <div className="p-4 border-t border-gray-100 flex flex-col gap-2">
                    <button
                        onClick={handleLogout}
                        className="w-full flex items-center gap-3 px-4 py-3 rounded-xl font-medium text-red-500 hover:bg-red-50 transition-all"
                    >
                        <LogOut className="w-5 h-5" />
                        <span>{t('sidebar.logout')}</span>
                    </button>
                </div>
            </aside>

            {/* Main Content */}
            <main ref={mainRef} className="flex-1 overflow-auto relative">
                <Outlet context={{ refreshUnreadCount: fetchUnreadCount }} />
                <ScrollToTop scrollContainerRef={mainRef} />
            </main>
        </div>
    );
}
