import { Outlet, NavLink, useNavigate } from 'react-router-dom';
import { BookOpen, FileText, GraduationCap, Heart, LogOut, Settings, FolderOpen, Users, Bell } from 'lucide-react';

import { useRef } from 'react';
import { ScrollToTop } from '../components/ScrollToTop';

export default function DashboardLayout() {
    const navigate = useNavigate();
    const userStr = localStorage.getItem('user');
    const user = userStr ? JSON.parse(userStr) : null;
    const mainRef = useRef<HTMLElement>(null);

    const handleLogout = () => {
        localStorage.clear();
        navigate('/');
    };

    const navItems = [
        { to: '/dashboard/all-courses', icon: BookOpen, label: 'Всі курси' },
        { to: '/dashboard/all-modules', icon: FolderOpen, label: 'Всі модулі' },
        { to: '/dashboard/all-lessons', icon: FileText, label: 'Всі уроки' },
        { to: '/dashboard/my-courses', icon: GraduationCap, label: 'Мої курси' },
        { to: '/dashboard/notifications', icon: Bell, label: 'Сповіщення' },
        { to: '/dashboard/settings', icon: Settings, label: 'Налаштування' },
    ];



    // TEMPORARY: Show Users tab for everyone since roles are not fully implemented yet
    navItems.push({ to: '/dashboard/users', icon: Users, label: 'Користувачі' });

    return (
        <div className="h-screen overflow-hidden flex bg-transparent">
            {/* Sidebar */}
            <aside className="w-64 glass-sidebar flex flex-col z-20 m-4 rounded-3xl h-[calc(100vh-2rem)] transition-all duration-300">
                {/* Logo/Header */}
                <div className="p-6 border-b border-white/20">
                    <div className="flex items-center gap-3">
                        <div className="bg-white/50 p-2 rounded-full backdrop-blur-sm">
                            <Heart className="w-5 h-5 text-brand-secondary fill-brand-secondary" />
                        </div>
                        <div>
                            <h2 className="font-bold text-gray-800 text-lg">Dashboard</h2>
                            <p className="text-xs text-gray-500 font-medium">
                                {user?.firstName && user?.lastName
                                    ? `${user.firstName} ${user.lastName}`
                                    : (user?.email || 'User')}
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
                            <item.icon className="w-5 h-5" />
                            <span>{item.label}</span>
                        </NavLink>
                    ))}
                </nav>

                {/* Logout */}
                <div className="p-4 border-t border-gray-100">
                    <button
                        onClick={handleLogout}
                        className="w-full flex items-center gap-3 px-4 py-3 rounded-xl font-medium text-red-500 hover:bg-red-50 transition-all"
                    >
                        <LogOut className="w-5 h-5" />
                        <span>Вийти</span>
                    </button>
                </div>
            </aside>

            {/* Main Content */}
            <main ref={mainRef} className="flex-1 overflow-auto relative">
                <Outlet />
                <ScrollToTop scrollContainerRef={mainRef} />
            </main>
        </div>
    );
}
