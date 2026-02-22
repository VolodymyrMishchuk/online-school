import React, { useEffect, useState } from 'react';
import {
    getUsersWithEnrollments,
    createPerson,
    updatePerson,
    deletePerson
} from '../api/users';
import type {
    PersonWithEnrollments,
    CreatePersonDto,
    UpdatePersonDto
} from '../api/users';
import { getCourses } from '../api/courses';
import type { CourseDto } from '../api/courses';
import { CreateUserModal } from '../components/CreateUserModal';
import { EditUserModal } from '../components/EditUserModal';
import { ManageAccessModal } from '../components/ManageAccessModal';
import { FakeAdminRestrictionModal } from '../components/FakeAdminRestrictionModal';
import * as Icons from 'lucide-react';

export const UsersPage: React.FC = () => {
    const [users, setUsers] = useState<PersonWithEnrollments[]>([]);
    const [courses, setCourses] = useState<CourseDto[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
    const [isEditModalOpen, setIsEditModalOpen] = useState(false);
    const [isManageAccessModalOpen, setIsManageAccessModalOpen] = useState(false);
    const [selectedUser, setSelectedUser] = useState<PersonWithEnrollments | null>(null);
    const [searchTerm, setSearchTerm] = useState('');
    const [sortConfig, setSortConfig] = useState<{ key: string; direction: 'asc' | 'desc' } | null>(null);
    const [blockedSort, setBlockedSort] = useState<'top' | 'bottom' | null>(null);
    const [adminSort, setAdminSort] = useState<'top' | 'bottom' | null>(null);
    const [isFakeAdminRestrictionModalOpen, setIsFakeAdminRestrictionModalOpen] = useState(false);

    const userStr = localStorage.getItem('user');
    const currentUser = userStr ? JSON.parse(userStr) : null;
    const currentUserId = currentUser?.userId || localStorage.getItem('userId') || '';
    const userRole = localStorage.getItem('userRole') || 'USER';

    const fetchData = async () => {
        setLoading(true);
        try {
            const [usersData, coursesData] = await Promise.all([
                getUsersWithEnrollments(),
                getCourses()
            ]);
            setUsers(usersData);
            setCourses(coursesData);

            // Sync selectedUser with fresh data if modal is open
            if (selectedUser) {
                const updatedUser = usersData.find(u => u.id === selectedUser.id);
                if (updatedUser) {
                    setSelectedUser(updatedUser);
                }
            }

            setError(null);
        } catch (err) {
            setError('Не вдалося завантажити дані.');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchData();
    }, []);

    const handleCreateUser = async (data: CreatePersonDto) => {
        await createPerson(data);
        await fetchData();
    };

    const handleUpdateUser = async (id: string, data: UpdatePersonDto) => {
        await updatePerson(id, data);
        await fetchData();
    };

    const handleDeleteUser = async (id: string) => {
        if (window.confirm('Ви впевнені, що хочете видалити цього користувача? Цю дію неможливо скасувати.')) {
            try {
                await deletePerson(id);
                await fetchData();
            } catch (err) {
                alert('Не вдалося видалити користувача.');
            }
        }
    };

    const openEditModal = (user: PersonWithEnrollments) => {
        setSelectedUser(user);
        setIsEditModalOpen(true);
    };

    const openManageAccessModal = (user: PersonWithEnrollments) => {
        setSelectedUser(user);
        setIsManageAccessModalOpen(true);
    };

    const handleSort = (key: string) => {
        let direction: 'asc' | 'desc' = 'asc';
        if (sortConfig && sortConfig.key === key && sortConfig.direction === 'asc') {
            direction = 'desc';
        }
        setSortConfig({ key, direction });
    };

    const toggleBlockedSort = (e: React.MouseEvent) => {
        e.stopPropagation();
        setAdminSort(null); // Clear admin sort
        if (blockedSort === null) setBlockedSort('top');
        else if (blockedSort === 'top') setBlockedSort('bottom');
        else setBlockedSort(null);
    };

    const clearBlockedSort = (e: React.MouseEvent) => {
        e.stopPropagation();
        setBlockedSort(null);
    };

    const toggleAdminSort = (e: React.MouseEvent) => {
        e.stopPropagation();
        setBlockedSort(null); // Clear blocked sort
        if (adminSort === null) setAdminSort('top');
        else if (adminSort === 'top') setAdminSort('bottom');
        else setAdminSort(null);
    };

    const clearAdminSort = (e: React.MouseEvent) => {
        e.stopPropagation();
        setAdminSort(null);
    };

    const getRemainingTimeMs = (enrollmentDate?: string, courseId?: string) => {
        if (!enrollmentDate || !courseId) return Infinity; // Treated as "Forever" (longest time)

        const course = courses.find(c => c.id === courseId);
        if (!course || !course.accessDuration) return Infinity;

        const startDate = new Date(enrollmentDate);
        const endDate = new Date(startDate);
        endDate.setDate(startDate.getDate() + course.accessDuration);

        const today = new Date();
        return endDate.getTime() - today.getTime();
    };

    const calculateTimeLeft = (enrollmentDate?: string, courseId?: string) => {
        const diffTime = getRemainingTimeMs(enrollmentDate, courseId);

        if (diffTime === Infinity) return 'Назавжди';
        if (diffTime < 0) return 'Минув';

        const diffHours = Math.ceil(diffTime / (1000 * 60 * 60));
        if (diffHours < 48) {
            return `${diffHours} год`;
        }

        const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
        const lastDigit = diffDays % 10;
        const lastTwoDigits = diffDays % 100;

        if (lastDigit === 1 && lastTwoDigits !== 11) {
            return `${diffDays} день`;
        } else if ([2, 3, 4].includes(lastDigit) && ![12, 13, 14].includes(lastTwoDigits)) {
            return `${diffDays} дні`;
        } else {
            return `${diffDays} днів`;
        }
    };

    const hasBlockedUsers = users.some(u => u.status === 'BLOCKED');
    const hasAdminUsers = users.some(u => u.role === 'ADMIN');

    const safeDate = (dateStr?: string | number | Date): number => {
        if (!dateStr) return 0;
        const timestamp = new Date(dateStr).getTime();
        return isNaN(timestamp) ? 0 : timestamp;
    };

    const sortedUsers = React.useMemo(() => {
        let sortableItems = [...users];

        // First filter
        sortableItems = sortableItems.filter(user =>
            (user.email || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
            (user.firstName || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
            (user.lastName || '').toLowerCase().includes(searchTerm.toLowerCase())
        );

        // Sort by blocked status if active
        if (blockedSort) {
            sortableItems.sort((a, b) => {
                const aBlocked = a.status === 'BLOCKED';
                const bBlocked = b.status === 'BLOCKED';

                if (aBlocked === bBlocked) return 0;

                if (blockedSort === 'top') {
                    return aBlocked ? -1 : 1;
                } else {
                    return aBlocked ? 1 : -1;
                }
            });
        }

        // Sort by admin role if active
        if (adminSort) {
            sortableItems.sort((a, b) => {
                const aAdmin = a.role === 'ADMIN';
                const bAdmin = b.role === 'ADMIN';

                if (aAdmin === bAdmin) return 0;

                if (adminSort === 'top') {
                    return aAdmin ? -1 : 1;
                } else {
                    return aAdmin ? 1 : -1;
                }
            });
        }

        // Then apply standard sort within groups
        if (sortConfig !== null) {
            sortableItems.sort((a, b) => {
                // Determine if we need to respect custom sort order first
                if (blockedSort) {
                    const aBlocked = a.status === 'BLOCKED';
                    const bBlocked = b.status === 'BLOCKED';
                    if (aBlocked !== bBlocked) {
                        return blockedSort === 'top'
                            ? (aBlocked ? -1 : 1)
                            : (aBlocked ? 1 : -1);
                    }
                }

                if (adminSort) {
                    const aAdmin = a.role === 'ADMIN';
                    const bAdmin = b.role === 'ADMIN';
                    if (aAdmin !== bAdmin) {
                        return adminSort === 'top'
                            ? (aAdmin ? -1 : 1)
                            : (aAdmin ? 1 : -1);
                    }
                }

                let aValue: any = '';
                let bValue: any = '';

                switch (sortConfig.key) {
                    case 'name':
                        aValue = `${a.firstName || ''} ${a.lastName || ''}`.toLowerCase();
                        bValue = `${b.firstName || ''} ${b.lastName || ''}`.toLowerCase();
                        break;
                    case 'contacts':
                        aValue = (a.email || '').toLowerCase();
                        bValue = (b.email || '').toLowerCase();
                        break;
                    case 'role':
                        aValue = a.role || '';
                        bValue = b.role || '';
                        break;
                    case 'status':
                        aValue = a.status || '';
                        bValue = b.status || '';
                        break;
                    case 'createdAt':
                        aValue = a.createdAt ? new Date(a.createdAt).getTime() : 0;
                        bValue = b.createdAt ? new Date(b.createdAt).getTime() : 0;
                        break;
                    case 'enrollments': // Default sort by count
                        aValue = a.enrollments ? a.enrollments.length : 0;
                        bValue = b.enrollments ? b.enrollments.length : 0;
                        break;
                    case 'enrollment_name':
                        // Sort by first course name (alphabetical)
                        aValue = a.enrollments?.[0]?.courseName?.toLowerCase() || '';
                        bValue = b.enrollments?.[0]?.courseName?.toLowerCase() || '';
                        break;
                    case 'enrollment_date':
                        if (sortConfig.direction === 'asc') {
                            // ASC: Sort by EARLIEST enrollment (Oldest First)
                            // Use MAX_SAFE_INTEGER for empty to push them to the bottom
                            aValue = a.enrollments?.length
                                ? Math.min(...a.enrollments.map(e => safeDate(e.createdAt)))
                                : Number.MAX_SAFE_INTEGER;
                            bValue = b.enrollments?.length
                                ? Math.min(...b.enrollments.map(e => safeDate(e.createdAt)))
                                : Number.MAX_SAFE_INTEGER;
                        } else {
                            // DESC: Sort by LATEST enrollment (Newest First)
                            // Use 0 for empty to push them to the bottom
                            aValue = a.enrollments?.length
                                ? Math.max(...a.enrollments.map(e => safeDate(e.createdAt)))
                                : 0;
                            bValue = b.enrollments?.length
                                ? Math.max(...b.enrollments.map(e => safeDate(e.createdAt)))
                                : 0;
                        }
                        break;
                    case 'enrollment_timeLeft':
                        // Sort by least time remaining (urgent first)
                        // If no enrollments, treat as Infinity (bottom of list in ASC, top in DESC?)
                        // Let's standardise: No enrollments = Infinity.
                        aValue = a.enrollments?.length
                            ? Math.min(...a.enrollments.map(e => getRemainingTimeMs(e.createdAt, e.courseId)))
                            : Infinity;
                        bValue = b.enrollments?.length
                            ? Math.min(...b.enrollments.map(e => getRemainingTimeMs(e.createdAt, e.courseId)))
                            : Infinity;
                        break;
                    default:
                        return 0;
                }

                if (aValue < bValue) {
                    return sortConfig.direction === 'asc' ? -1 : 1;
                }
                if (aValue > bValue) {
                    return sortConfig.direction === 'asc' ? 1 : -1;
                }
                return 0;
            });
        }
        return sortableItems;
    }, [users, sortConfig, searchTerm, blockedSort, adminSort, courses]); // Added courses dependency for time calc

    const getSortIcon = (columnKey: string) => {
        if (sortConfig?.key !== columnKey) return <Icons.ArrowUpDown size={14} className="ml-1 text-gray-400" />;
        if (sortConfig.direction === 'asc') return <Icons.ArrowUp size={14} className="ml-1 text-blue-600" />;
        return <Icons.ArrowDown size={14} className="ml-1 text-blue-600" />;
    };

    if (loading && users.length === 0) {
        return <div className="p-8 text-center text-gray-500">Завантаження...</div>;
    }

    return (
        <div className="container mx-auto px-6 py-12 space-y-6">
            <div className="flex justify-between items-center">
                <h1 className="text-3xl font-bold text-gray-900">Управління користувачами</h1>
                <button
                    onClick={() => setIsCreateModalOpen(true)}
                    className="flex items-center space-x-2 px-4 py-2 text-gray-900 font-medium hover:bg-gray-100 rounded-lg transition-colors"
                >
                    <Icons.Plus size={20} />
                    <span>Створити користувача</span>
                </button>
            </div>

            {error && (
                <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded">
                    {error}
                </div>
            )}

            {/* Search Bar */}
            <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                    <Icons.Search className="h-5 w-5 text-gray-400" />
                </div>
                <input
                    type="text"
                    placeholder="Пошук за ім'ям або email..."
                    className="block w-full pl-10 pr-3 py-2 border border-gray-300 rounded-lg leading-5 bg-white placeholder-gray-500 focus:outline-none focus:placeholder-gray-400 focus:ring-1 focus:ring-blue-500 focus:border-blue-500 sm:text-sm text-gray-900"
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                />
            </div>

            {/* Users Table */}
            <div className="glass-panel overflow-hidden rounded-lg">
                <table className="min-w-full divide-y divide-gray-200/50">
                    <thead className="bg-white/30 backdrop-blur-sm">
                        <tr>
                            <th
                                scope="col"
                                className="px-6 py-3 text-left text-xs font-bold text-gray-600 uppercase tracking-wider cursor-pointer hover:bg-white/40 transition-colors whitespace-nowrap w-1"
                                onClick={() => handleSort('name')}
                            >
                                <div className="flex items-center gap-2">
                                    <div className="flex items-center gap-1">
                                        Ім'я
                                        {getSortIcon('name')}
                                    </div>

                                    {hasBlockedUsers && (
                                        <div
                                            className={`
                                                flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px] font-bold border transition-colors
                                                ${blockedSort
                                                    ? 'bg-red-100 text-red-800 border-red-200 hover:bg-red-200'
                                                    : 'bg-gray-100 text-gray-500 border-gray-200 hover:bg-gray-200'
                                                }
                                            `}
                                            onClick={toggleBlockedSort}
                                            title="Сортувати заблокованих"
                                        >
                                            <span>BLOCKED</span>

                                            {/* Sort Indicators */}
                                            <div className="flex flex-col -space-y-0.5">
                                                <Icons.ChevronUp size={8} className={blockedSort === 'top' ? 'text-current' : 'text-gray-400/50'} />
                                                <Icons.ChevronDown size={8} className={blockedSort === 'bottom' ? 'text-current' : 'text-gray-400/50'} />
                                            </div>

                                            {/* Clear Button */}
                                            {blockedSort && (
                                                <div
                                                    className="pl-1 ml-1 border-l border-red-200/60 hover:text-red-950 flex items-center"
                                                    onClick={clearBlockedSort}
                                                    title="Скинути сортування"
                                                >
                                                    <Icons.X size={10} />
                                                </div>
                                            )}
                                        </div>
                                    )}

                                    {hasAdminUsers && (
                                        <div
                                            className={`
                                                flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px] font-bold border transition-colors
                                                ${adminSort
                                                    ? 'bg-purple-100 text-purple-800 border-purple-200 hover:bg-purple-200'
                                                    : 'bg-gray-100 text-gray-500 border-gray-200 hover:bg-gray-200'
                                                }
                                            `}
                                            onClick={toggleAdminSort}
                                            title="Сортувати адміністраторів"
                                        >
                                            <span>ADMIN</span>

                                            {/* Sort Indicators */}
                                            <div className="flex flex-col -space-y-0.5">
                                                <Icons.ChevronUp size={8} className={adminSort === 'top' ? 'text-current' : 'text-gray-400/50'} />
                                                <Icons.ChevronDown size={8} className={adminSort === 'bottom' ? 'text-current' : 'text-gray-400/50'} />
                                            </div>

                                            {/* Clear Button */}
                                            {adminSort && (
                                                <div
                                                    className="pl-1 ml-1 border-l border-purple-200/60 hover:text-purple-950 flex items-center"
                                                    onClick={clearAdminSort}
                                                    title="Скинути сортування"
                                                >
                                                    <Icons.X size={10} />
                                                </div>
                                            )}
                                        </div>
                                    )}
                                </div>
                            </th>
                            <th
                                scope="col"
                                className="px-6 py-3 text-left text-xs font-bold text-gray-600 uppercase tracking-wider cursor-pointer hover:bg-white/40 transition-colors whitespace-nowrap w-1"
                                onClick={() => handleSort('contacts')}
                            >
                                <div className="flex items-center gap-1">
                                    Контакти
                                    {getSortIcon('contacts')}
                                </div>
                            </th>
                            <th
                                scope="col"
                                className="px-6 py-3 text-left text-xs font-bold text-gray-600 uppercase tracking-wider cursor-pointer hover:bg-white/40 transition-colors whitespace-nowrap w-1"
                                onClick={() => handleSort('createdAt')}
                            >
                                <div className="flex items-center gap-1">
                                    Реєстрація
                                    {getSortIcon('createdAt')}
                                </div>
                            </th>
                            <th
                                scope="col"
                                className="px-6 py-3 text-left text-xs font-bold text-gray-600 uppercase tracking-wider cursor-pointer hover:bg-white/40 transition-colors w-full"
                            >
                                <div className="flex items-center gap-3">
                                    <div
                                        className="flex items-center gap-1 hover:text-blue-600 transition-colors"
                                        onClick={() => handleSort('enrollment_name')}
                                    >
                                        Курси
                                        {getSortIcon('enrollment_name')}
                                    </div>

                                    <div
                                        className={`
                                            flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px] font-bold border transition-colors cursor-pointer
                                            ${sortConfig?.key === 'enrollment_date'
                                                ? 'bg-green-100 text-green-800 border-green-200 hover:bg-green-200'
                                                : 'bg-gray-100 text-gray-500 border-gray-200 hover:bg-gray-200'
                                            }
                                        `}
                                        onClick={(e) => { e.stopPropagation(); handleSort('enrollment_date'); }}
                                        title="Сортувати за датою початку (придбання)"
                                    >
                                        <span>СТАРТ</span>

                                        {/* Sort Indicators */}
                                        <div className="flex flex-col -space-y-0.5">
                                            <Icons.ChevronUp size={8} className={sortConfig?.key === 'enrollment_date' && sortConfig.direction === 'asc' ? 'text-current' : 'text-gray-400/50'} />
                                            <Icons.ChevronDown size={8} className={sortConfig?.key === 'enrollment_date' && sortConfig.direction === 'desc' ? 'text-current' : 'text-gray-400/50'} />
                                        </div>

                                        {/* Clear Button */}
                                        {sortConfig?.key === 'enrollment_date' && (
                                            <div
                                                className="pl-1 ml-1 border-l border-green-200/60 hover:text-green-950 flex items-center"
                                                onClick={(e) => { e.stopPropagation(); setSortConfig(null); }}
                                                title="Скинути сортування"
                                            >
                                                <Icons.X size={10} />
                                            </div>
                                        )}
                                    </div>

                                    <div
                                        className={`
                                            flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px] font-bold border transition-colors cursor-pointer
                                            ${sortConfig?.key === 'enrollment_timeLeft'
                                                ? 'bg-orange-100 text-orange-800 border-orange-200 hover:bg-orange-200'
                                                : 'bg-gray-100 text-gray-500 border-gray-200 hover:bg-gray-200'
                                            }
                                        `}
                                        onClick={(e) => { e.stopPropagation(); handleSort('enrollment_timeLeft'); }}
                                        title="Сортувати за часом завершення"
                                    >
                                        <span>КІНЕЦЬ</span>

                                        {/* Sort Indicators */}
                                        <div className="flex flex-col -space-y-0.5">
                                            <Icons.ChevronUp size={8} className={sortConfig?.key === 'enrollment_timeLeft' && sortConfig.direction === 'asc' ? 'text-current' : 'text-gray-400/50'} />
                                            <Icons.ChevronDown size={8} className={sortConfig?.key === 'enrollment_timeLeft' && sortConfig.direction === 'desc' ? 'text-current' : 'text-gray-400/50'} />
                                        </div>

                                        {/* Clear Button */}
                                        {sortConfig?.key === 'enrollment_timeLeft' && (
                                            <div
                                                className="pl-1 ml-1 border-l border-orange-200/60 hover:text-orange-950 flex items-center"
                                                onClick={(e) => { e.stopPropagation(); setSortConfig(null); }}
                                                title="Скинути сортування"
                                            >
                                                <Icons.X size={10} />
                                            </div>
                                        )}
                                    </div>
                                </div>
                            </th>
                            <th scope="col" className="px-6 py-3 text-center text-xs font-bold text-gray-600 uppercase tracking-wider whitespace-nowrap w-1">
                                Дії
                            </th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-gray-200/50">
                        {sortedUsers.map((user) => (
                            <tr key={user.id} className="hover:bg-white/40 transition-colors">
                                <td className="px-6 py-2 whitespace-nowrap w-1">
                                    <div className="flex items-center gap-2">
                                        <div className="text-sm font-medium text-gray-900">
                                            {user.firstName} {user.lastName}
                                        </div>
                                        {user.role === 'ADMIN' && (
                                            <span className="px-1.5 py-0.5 rounded text-[10px] font-bold bg-purple-100 text-purple-800 border border-purple-200">
                                                ADMIN
                                            </span>
                                        )}
                                        {user.role === 'INSTRUCTOR' && (
                                            <span className="px-1.5 py-0.5 rounded text-[10px] font-bold bg-blue-100 text-blue-800 border border-blue-200">
                                                INSTRUCTOR
                                            </span>
                                        )}
                                        {user.status !== 'ACTIVE' && (
                                            <span className="px-1.5 py-0.5 rounded text-[10px] font-bold bg-red-100 text-red-800 border border-red-200">
                                                {user.status}
                                            </span>
                                        )}
                                    </div>
                                </td>
                                <td className="px-6 py-2 whitespace-nowrap w-1">
                                    <div className="text-sm text-gray-900">{user.email}</div>
                                    <div className="text-sm text-gray-500">{user.phoneNumber}</div>
                                </td>
                                <td className="px-6 py-2 whitespace-nowrap text-sm text-gray-500 w-1">
                                    {user.createdAt ? new Date(user.createdAt).toLocaleDateString('uk-UA') : '-'}
                                </td>
                                <td className="px-6 py-2">
                                    <div className="flex flex-wrap gap-2">
                                        {user.enrollments.length > 0 ? (
                                            [...user.enrollments]
                                                .sort((a, b) => {
                                                    // Context-aware sorting within the row
                                                    if (sortConfig?.key === 'enrollment_timeLeft') {
                                                        const timeA = getRemainingTimeMs(a.createdAt, a.courseId);
                                                        const timeB = getRemainingTimeMs(b.createdAt, b.courseId);
                                                        return timeA - timeB; // Shortest time left first
                                                    }
                                                    if (sortConfig?.key === 'enrollment_date') {
                                                        const dateA = safeDate(a.createdAt);
                                                        const dateB = safeDate(b.createdAt);

                                                        // Respect sort direction within the row
                                                        if (sortConfig.direction === 'asc') {
                                                            return dateA - dateB; // Oldest date first (earlier on the left)
                                                        } else {
                                                            return dateB - dateA; // Newest date first (later on the left)
                                                        }
                                                    }
                                                    // Default: Sort by alphanumeric name
                                                    return (a.courseName || '').localeCompare(b.courseName || '', undefined, { numeric: true });
                                                })
                                                .map(enrollment => {
                                                    const timeLeft = calculateTimeLeft(enrollment.createdAt, enrollment.courseId);
                                                    return (
                                                        <div key={enrollment.id} className="inline-flex flex-col px-3 py-1.5 rounded-md bg-white/60 border border-gray-200 shadow-sm">
                                                            <span className="text-xs font-bold text-gray-800 leading-tight block">
                                                                {enrollment.courseName || 'Курс'}
                                                            </span>
                                                            <span className="text-[10px] text-gray-500 leading-tight block mt-0.5 flex items-center gap-1">
                                                                <span className={sortConfig?.key === 'enrollment_date' ? 'bg-green-100 text-green-800 px-1 rounded -ml-1' : ''}>
                                                                    {enrollment.createdAt ? new Date(enrollment.createdAt).toLocaleDateString('uk-UA') : 'Невідомо'}
                                                                </span>
                                                                {timeLeft && (
                                                                    <>
                                                                        <span>|</span>
                                                                        <span className={sortConfig?.key === 'enrollment_timeLeft' ? 'bg-orange-100 text-orange-800 px-1 rounded' : ''}>
                                                                            {timeLeft}
                                                                        </span>
                                                                    </>
                                                                )}
                                                            </span>
                                                        </div>
                                                    );
                                                })
                                        ) : (
                                            <span className="text-sm text-gray-400 font-italic">Немає курсів</span>
                                        )}
                                    </div>
                                </td>
                                <td className="px-6 py-2 whitespace-nowrap text-right text-sm font-medium space-x-3">
                                    <button
                                        onClick={() => openManageAccessModal(user)}
                                        className="text-indigo-600 hover:text-indigo-900"
                                        title="Управління доступом"
                                    >
                                        <Icons.BookOpen size={18} />
                                    </button>
                                    <button
                                        onClick={() => openEditModal(user)}
                                        className="text-blue-600 hover:text-blue-900"
                                        title="Редагувати"
                                    >
                                        <Icons.Edit2 size={18} />
                                    </button>
                                    <button
                                        onClick={() => {
                                            if (userRole === 'FAKE_ADMIN' && user.createdBy?.id !== currentUserId) {
                                                setIsFakeAdminRestrictionModalOpen(true);
                                            } else {
                                                handleDeleteUser(user.id);
                                            }
                                        }}
                                        className="text-red-600 hover:text-red-900"
                                        title="Видалити"
                                    >
                                        <Icons.Trash2 size={18} />
                                    </button>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>

            <CreateUserModal
                isOpen={isCreateModalOpen}
                onClose={() => setIsCreateModalOpen(false)}
                onSubmit={handleCreateUser}
            />

            <EditUserModal
                isOpen={isEditModalOpen}
                onClose={() => {
                    setIsEditModalOpen(false);
                    setSelectedUser(null);
                }}
                user={selectedUser}
                onSubmit={handleUpdateUser}
                isReadonlyForFakeAdmin={selectedUser ? (userRole === 'FAKE_ADMIN' && selectedUser.createdBy?.id !== currentUserId) : false}
                onShowRestriction={() => setIsFakeAdminRestrictionModalOpen(true)}
            />

            <ManageAccessModal
                isOpen={isManageAccessModalOpen}
                onClose={() => {
                    setIsManageAccessModalOpen(false);
                    setSelectedUser(null);
                }}
                user={selectedUser}
                onRefresh={fetchData}
                isReadonlyForFakeAdmin={selectedUser ? (userRole === 'FAKE_ADMIN' && selectedUser.createdBy?.id !== currentUserId) : false}
                onShowRestriction={() => setIsFakeAdminRestrictionModalOpen(true)}
            />

            <FakeAdminRestrictionModal
                isOpen={isFakeAdminRestrictionModalOpen}
                onClose={() => setIsFakeAdminRestrictionModalOpen(false)}
            />
        </div>
    );
};
