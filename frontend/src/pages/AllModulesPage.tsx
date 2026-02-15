import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Plus, Loader2 } from 'lucide-react';
import { getModules, getModuleLessons, createModule, updateModule, deleteModule } from '../api/modules';
import type { Module, CreateModuleDto } from '../api/modules';
import { getUnassignedLessons } from '../api/lessons';
import type { Lesson } from '../api/lessons';
import { getCourses } from '../api/courses';
import { ModuleCard } from '../components/ModuleCard';
import { ModuleModal } from '../components/ModuleModal';

export const AllModulesPage: React.FC = () => {
    const queryClient = useQueryClient();
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [editingModule, setEditingModule] = useState<{ module: Module; lessons: Lesson[] } | null>(null);

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

    // Fetch lessons for each module
    const moduleLessonsQueries = useQuery({
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

    const handleDelete = (id: string) => {
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
        <div className="container mx-auto px-4 py-8">
            {/* Header */}
            <div className="flex justify-between items-center mb-8">
                <div>
                    <h1 className="text-3xl font-bold text-gray-800">Всі Модулі</h1>
                    <p className="text-gray-600 mt-1">
                        Керуйте модулями курсів та уроками
                    </p>
                </div>
                <button
                    onClick={handleCreateModule}
                    className="flex items-center gap-2 px-6 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors shadow-md hover:shadow-lg"
                >
                    <Plus size={20} />
                    Додати Модуль
                </button>
            </div>

            {/* Modules Grid */}
            {modules.length === 0 ? (
                <div className="text-center py-16">
                    <div className="text-gray-400 mb-4">
                        <svg
                            className="mx-auto h-24 w-24"
                            fill="none"
                            viewBox="0 0 24 24"
                            stroke="currentColor"
                        >
                            <path
                                strokeLinecap="round"
                                strokeLinejoin="round"
                                strokeWidth={1.5}
                                d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"
                            />
                        </svg>
                    </div>
                    <h3 className="text-xl font-semibold text-gray-700 mb-2">
                        Ще немає модулів
                    </h3>
                    <p className="text-gray-500 mb-6">
                        Почніть зі створення вашого першого модуля
                    </p>
                    <button
                        onClick={handleCreateModule}
                        className="inline-flex items-center gap-2 px-6 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
                    >
                        <Plus size={20} />
                        Створити Модуль
                    </button>
                </div>
            ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                    {modules.map((module) => (
                        <ModuleCard
                            key={module.id}
                            module={module}
                            lessons={moduleLessonsQueries.data?.[module.id] || []}
                            onEdit={handleEditModule}
                            onDelete={handleDelete}
                        />
                    ))}
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
        </div>
    );
};

export default AllModulesPage;
