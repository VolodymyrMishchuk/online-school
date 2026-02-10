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

    const calculateTimeLeft = (enrollmentDate?: string, courseId?: string) => {
        if (!enrollmentDate || !courseId) return null;

        const course = courses.find(c => c.id === courseId);
        if (!course || !course.accessDuration) return 'Назавжди';

        const startDate = new Date(enrollmentDate);
        const endDate = new Date(startDate);
        endDate.setDate(startDate.getDate() + course.accessDuration);

        const today = new Date();
        const diffTime = endDate.getTime() - today.getTime();

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

    const sortedUsers = React.useMemo(() => {
        let sortableItems = [...users];

        // First filter
        sortableItems = sortableItems.filter(user =>
            (user.email || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
            (user.firstName || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
            (user.lastName || '').toLowerCase().includes(searchTerm.toLowerCase())
        );

        // Then sort
        if (sortConfig !== null) {
            sortableItems.sort((a, b) => {
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
                    case 'enrollments':
                        aValue = a.enrollments ? a.enrollments.length : 0;
                        bValue = b.enrollments ? b.enrollments.length : 0;
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
    }, [users, sortConfig, searchTerm]);

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
                    className="flex items-center space-x-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
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
                                <div className="flex items-center gap-1">
                                    Ім'я
                                    {getSortIcon('name')}
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
                                    Дата реєстрації
                                    {getSortIcon('createdAt')}
                                </div>
                            </th>
                            <th
                                scope="col"
                                className="px-6 py-3 text-left text-xs font-bold text-gray-600 uppercase tracking-wider cursor-pointer hover:bg-white/40 transition-colors w-full"
                                onClick={() => handleSort('enrollments')}
                            >
                                <div className="flex items-center gap-1">
                                    Придбані курси
                                    {getSortIcon('enrollments')}
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
                                            user.enrollments.map(enrollment => {
                                                const timeLeft = calculateTimeLeft(enrollment.createdAt, enrollment.courseId);
                                                return (
                                                    <div key={enrollment.id} className="inline-flex flex-col px-3 py-1.5 rounded-md bg-white/60 border border-gray-200 shadow-sm">
                                                        <span className="text-xs font-bold text-gray-800 leading-tight block">
                                                            {enrollment.courseName || 'Курс'}
                                                        </span>
                                                        <span className="text-[10px] text-gray-500 leading-tight block mt-0.5">
                                                            {enrollment.createdAt ? new Date(enrollment.createdAt).toLocaleDateString('uk-UA') : 'Невідомо'}
                                                            {timeLeft && <span> | {timeLeft}</span>}
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
                                        onClick={() => handleDeleteUser(user.id)}
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
            />

            <ManageAccessModal
                isOpen={isManageAccessModalOpen}
                onClose={() => {
                    setIsManageAccessModalOpen(false);
                    setSelectedUser(null);
                }}
                user={selectedUser}
                onRefresh={fetchData}
            />
        </div>
    );
};
