import React, { useState, useMemo } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Plus, Loader2, FolderOpen, Filter } from 'lucide-react';
import { getModules, getModuleLessons, createModule, updateModule, deleteModule } from '../api/modules';
import type { Module, CreateModuleDto } from '../api/modules';
import { getUnassignedLessons } from '../api/lessons';
import type { Lesson } from '../api/lessons';
import { getCourses } from '../api/courses';
import { ModuleCard } from '../components/ModuleCard';
import { ModuleModal } from '../components/ModuleModal';
import MultiSelect from '../components/MultiSelect';
import { FakeAdminRestrictionModal } from '../components/FakeAdminRestrictionModal';

export const AllModulesPage: React.FC = () => {
    const queryClient = useQueryClient();
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [editingModule, setEditingModule] = useState<{ module: Module; lessons: Lesson[] } | null>(null);
    const [isFakeAdminRestrictionModalOpen, setIsFakeAdminRestrictionModalOpen] = useState(false);
    const userRole = localStorage.getItem('userRole') || 'USER';
    const isAdmin = userRole === 'ADMIN' || userRole === 'FAKE_ADMIN';

    // Fetch all courses (for fallback courseId)
    const { data: courses = [] } = useQuery({
        queryKey: ['courses'],
        queryFn: () => getCourses(),
    });

    // Fetch all modules
    const { data: modules = [], isLoading: modulesLoading } = useQuery({
        queryKey: ['modules'],
        queryFn: () => getModules(),
    });

    // Fetch unassigned lessons
    const { data: unassignedLessons = [] } = useQuery({
        queryKey: ['unassignedLessons'],
        queryFn: getUnassignedLessons,
    });

    const [selectedCourseIds, setSelectedCourseIds] = useState<string[]>([]);

    // Filtering Logic
    const filteredModules = useMemo(() => {
        if (!modules) return [];
        if (selectedCourseIds.length === 0) return modules;

        const explicitCourseIds = selectedCourseIds.filter(id => id !== 'unassigned');
        const includeUnassigned = selectedCourseIds.includes('unassigned');

        return modules.filter(m => {
            if (m.courseId) {
                return explicitCourseIds.includes(m.courseId);
            }
            return includeUnassigned;
        });
    }, [modules, selectedCourseIds]);

    // Fetch lessons for each module
    const moduleLessonsQueries = useQuery({
        // Use filtered modules effectively? Ideally fetch all or just filter list locally.
        // Keeping it simple: fetch for all, filter rendering.
        queryKey: ['moduleLessons', modules.map(m => m.id)],
        queryFn: async () => {
            const lessonsMap: Record<string, Lesson[]> = {};
            await Promise.all(
                modules.map(async (module) => {
                    const lessons = await getModuleLessons(module.id);
                    lessonsMap[module.id] = lessons;
                })
            );
            return lessonsMap;
        },
        enabled: modules.length > 0,
    });

    // Create module mutation
    const createMutation = useMutation({
        mutationFn: createModule,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['modules'] });
            queryClient.invalidateQueries({ queryKey: ['unassignedLessons'] });
            queryClient.invalidateQueries({ queryKey: ['moduleLessons'] });
        },
    });

    // Update module mutation
    const updateMutation = useMutation({
        mutationFn: ({ id, data }: { id: string; data: Partial<CreateModuleDto> }) =>
            updateModule(id, data),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['modules'] });
            queryClient.invalidateQueries({ queryKey: ['unassignedLessons'] });
            queryClient.invalidateQueries({ queryKey: ['moduleLessons'] });
            setEditingModule(null);
        },
    });

    // Delete module mutation
    const deleteMutation = useMutation({
        mutationFn: deleteModule,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['modules'] });
            queryClient.invalidateQueries({ queryKey: ['unassignedLessons'] });
            queryClient.invalidateQueries({ queryKey: ['moduleLessons'] });
        },
    });

    const handleCreateModule = () => {
        setEditingModule(null);
        setIsModalOpen(true);
    };

    const handleEditModule = (module: Module, lessons: Lesson[]) => {
        const user = JSON.parse(localStorage.getItem('user') || '{}');
        if (userRole === 'FAKE_ADMIN' && module.createdBy?.id !== user.id) {
            setIsFakeAdminRestrictionModalOpen(true);
            return;
        }
        setEditingModule({ module, lessons });
        setIsModalOpen(true);
    };

    const handleSubmit = (data: CreateModuleDto) => {
        if (editingModule) {
            updateMutation.mutate({ id: editingModule.module.id, data });
        } else {
            createMutation.mutate(data);
        }
        setIsModalOpen(false);
    };

    const handleDelete = (id: string, module: Module) => {
        const user = JSON.parse(localStorage.getItem('user') || '{}');
        if (userRole === 'FAKE_ADMIN' && module.createdBy?.id !== user.id) {
            setIsFakeAdminRestrictionModalOpen(true);
            return;
        }
        deleteMutation.mutate(id);
    };

    const handleCloseModal = () => {
        setIsModalOpen(false);
        setEditingModule(null);
    };

    if (modulesLoading) {
        return (
            <div className="flex justify-center items-center h-64">
                <Loader2 className="animate-spin text-blue-600" size={48} />
            </div>
        );
    }

    return (
        <div className="container mx-auto px-6 py-12">
            {/* Header */}
            <div className="flex justify-between items-center mb-8">
                <div>
                    <h1 className="text-3xl font-bold text-gray-900">Всі модулі</h1>
                </div>
                {isAdmin && (
                    <button
                        onClick={handleCreateModule}
                        className="flex items-center gap-2 px-4 py-2 text-gray-900 font-medium hover:bg-gray-100 rounded-lg transition-colors"
                    >
                        <Plus size={20} />
                        <span>Додати модуль</span>
                    </button>
                )}
            </div>

            {/* Filters */}
            <div className="flex flex-wrap gap-4 mb-8 bg-white p-3 rounded-lg border border-gray-100 shadow-sm items-center">
                <div className="flex items-center gap-2 text-gray-500 mr-2">
                    <Filter className="w-5 h-5" />
                    <span className="font-medium text-sm">Фільтри:</span>
                </div>

                {/* Course Filter */}
                <div className="min-w-[250px]">
                    <MultiSelect
                        label=""
                        placeholder="Всі курси"
                        options={[
                            { value: 'unassigned', label: 'Без курсу' },
                            ...(courses.map(c => ({ value: c.id, label: c.name })) || [])
                        ]}
                        selectedValues={selectedCourseIds}
                        onChange={setSelectedCourseIds}
                    />
                </div>

                {selectedCourseIds.length > 0 && (
                    <button
                        onClick={() => setSelectedCourseIds([])}
                        className="text-sm text-red-500 hover:text-red-600 font-medium px-2 ml-auto"
                    >
                        Скинути
                    </button>
                )}
            </div>

            {/* Modules Grid */}
            {filteredModules.length === 0 ? (
                <div className="text-center py-20 bg-gray-50 rounded-lg border border-dashed border-gray-200">
                    <div className="w-16 h-16 bg-white rounded-full flex items-center justify-center mx-auto mb-4 shadow-sm">
                        <FolderOpen className="w-8 h-8 text-gray-300" />
                    </div>
                    <h3 className="text-xl font-medium text-gray-900 mb-2">
                        Модулів не знайдено
                    </h3>
                    <p className="text-gray-400 mb-6">
                        Спробуйте змінити параметри фільтрації або створіть новий модуль
                    </p>
                    {isAdmin && (
                        <button
                            onClick={handleCreateModule}
                            className="inline-flex items-center gap-2 px-6 py-3 bg-brand-primary text-white rounded-lg hover:bg-brand-primary/90 transition-colors shadow-sm"
                        >
                            <Plus size={20} />
                            Створити Модуль
                        </button>
                    )}
                </div>
            ) : (
                <div className="flex flex-col space-y-2">
                    {filteredModules.map((module) => {
                        const moduleLessons = moduleLessonsQueries.data?.[module.id] || [];
                        const courseName = courses.find(c => c.id === module.courseId)?.name || module.courseName;
                        const totalDuration = moduleLessons.reduce((acc, lesson) => acc + (lesson.durationMinutes || 0), 0);
                        const totalFilesCount = moduleLessons.reduce((acc, lesson) => acc + (lesson.filesCount || 0), 0);

                        return (
                            <ModuleCard
                                key={module.id}
                                module={module}
                                lessons={moduleLessons}
                                courseName={courseName}
                                durationMinutes={totalDuration}
                                filesCount={totalFilesCount}
                                onEdit={handleEditModule}
                                onDelete={(id) => handleDelete(id, module)}
                                isCatalogMode={true}
                            />
                        );
                    })}
                </div>
            )}

            {/* Module Modal */}
            <ModuleModal
                isOpen={isModalOpen}
                onClose={handleCloseModal}
                onSubmit={handleSubmit}
                unassignedLessons={unassignedLessons}
                courses={courses}
                courseId={editingModule?.module.courseId}
                initialData={
                    editingModule
                        ? {
                            id: editingModule.module.id,
                            name: editingModule.module.name,
                            description: editingModule.module.description,
                            lessons: editingModule.lessons,
                        }
                        : undefined
                }
            />

            <FakeAdminRestrictionModal
                isOpen={isFakeAdminRestrictionModalOpen}
                onClose={() => setIsFakeAdminRestrictionModalOpen(false)}
            />
        </div>
    );
};

export default AllModulesPage;
