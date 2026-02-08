import { Outlet, NavLink, useNavigate } from 'react-router-dom';
import { BookOpen, FileText, GraduationCap, Heart, LogOut, Settings, BookMarked, FolderOpen, FileCheck, Users, LayoutGrid } from 'lucide-react';

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
        { to: '/dashboard/my-modules', icon: BookMarked, label: 'Мої модулі' },
        { to: '/dashboard/my-lessons', icon: FileCheck, label: 'Мої уроки' },
        { to: '/dashboard/settings', icon: Settings, label: 'Налаштування' },
    ];

    return (
        <div className="h-screen overflow-hidden bg-gray-50 flex">
            {/* Sidebar */}
            <aside className="w-64 bg-white border-r border-gray-200 flex flex-col">
                {/* Logo/Header */}
                <div className="p-6 border-b border-gray-100">
                    <div className="flex items-center gap-3">
                        <div className="bg-brand-light p-2 rounded-full">
                            <Heart className="w-5 h-5 text-brand-secondary fill-brand-secondary" />
                        </div>
                        <div>
                            <h2 className="font-bold text-brand-dark text-lg">Dashboard</h2>
                            <p className="text-xs text-gray-500">{user?.email || 'User'}</p>
                        </div>
                    </div>
                </div>

                {/* Navigation */}
                <nav className="flex-1 p-4 space-y-1">
                    {navItems.map((item) => (
                        <NavLink
                            key={item.to}
                            to={item.to}
                            className={({ isActive }) =>
                                `flex items-center gap-3 px-4 py-3 rounded-xl font-medium transition-all ${isActive
                                    ? 'bg-brand-primary text-white shadow-md'
                                    : 'text-gray-600 hover:bg-gray-50 hover:text-brand-primary'
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
