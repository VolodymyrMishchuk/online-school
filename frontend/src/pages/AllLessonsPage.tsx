import { useState, useMemo } from 'react';
import { useQuery, useQueryClient, useMutation } from '@tanstack/react-query';
import { Plus, Filter } from 'lucide-react';
import { getLessons, deleteLesson } from '../api/lessons';
import type { Lesson } from '../api/lessons';
import { getLessonFiles, type FileDto } from '../api/files';
import { getCourses } from '../api/courses';
import { getModules } from '../api/modules';
import LessonCard from '../components/LessonCard';
import LessonModal from '../components/LessonModal';
import ImagePreviewModal from '../components/ImagePreviewModal';
import MultiSelect from '../components/MultiSelect';
import { FakeAdminRestrictionModal } from '../components/FakeAdminRestrictionModal';
import { ConfirmModal } from '../components/ConfirmModal';
import { useTranslation } from 'react-i18next';

export default function AllLessonsPage() {
    const { t } = useTranslation();
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [editingLesson, setEditingLesson] = useState<Lesson | null>(null);
    const [previewImage, setPreviewImage] = useState<string | null>(null);
    const [isFakeAdminRestrictionModalOpen, setIsFakeAdminRestrictionModalOpen] = useState(false);

    // Deletion confirmation state
    const [isConfirmModalOpen, setIsConfirmModalOpen] = useState(false);
    const [lessonToDelete, setLessonToDelete] = useState<Lesson | null>(null);

    // Alert Modal state
    const [isAlertOpen, setIsAlertOpen] = useState(false);
    const [alertMessage, setAlertMessage] = useState('');
    const [alertTitle, setAlertTitle] = useState('');

    const showAlert = (message: string, title = t('common.error', 'Помилка')) => {
        setAlertMessage(message);
        setAlertTitle(title);
        setIsAlertOpen(true);
    };

    // Filter states (Arrays for multi-select)
    const [selectedCourseIds, setSelectedCourseIds] = useState<string[]>([]);
    const [selectedModuleIds, setSelectedModuleIds] = useState<string[]>([]);

    const queryClient = useQueryClient();
    const userId = localStorage.getItem('userId') || '';
    const userRole = localStorage.getItem('userRole') || 'USER';
    const isAdmin = userRole === 'ADMIN' || userRole === 'FAKE_ADMIN';

    // Fetch Data
    const { data: lessons, isLoading: lessonsLoading } = useQuery({
        queryKey: ['allLessons'],
        queryFn: getLessons,
    });

    const { data: courses } = useQuery({
        queryKey: ['allCourses', userId],
        queryFn: () => getCourses(userId),
        enabled: !!userId
    });

    const { data: modules } = useQuery({
        queryKey: ['allModules'],
        queryFn: () => getModules(),
    });

    // Fetch files for each lesson
    const lessonFilesQueries = useQuery({
        queryKey: ['lessonFiles', lessons?.map(l => l.id)],
        queryFn: async (): Promise<Record<string, FileDto[]>> => {
            if (!lessons) return {};
            const filesData = await Promise.all(
                lessons.map(async (lesson) => ({
                    lessonId: lesson.id,
                    files: await getLessonFiles(lesson.id),
                }))
            );
            return Object.fromEntries(
                filesData.map(({ lessonId, files }) => [lessonId, files])
            );
        },
        enabled: !!lessons && lessons.length > 0,
    });

    // Delete lesson mutation
    const deleteMutation = useMutation({
        mutationFn: deleteLesson,
        onSuccess: () => {
            handleSuccess();
        },
        onError: (error: any) => {
            showAlert(t('allLessons.deleteError', 'Помилка видалення уроку: ') + (error.response?.data?.message || error.message));
        },
    });

    const handleSuccess = () => {
        queryClient.invalidateQueries({ queryKey: ['allLessons'] });
        queryClient.invalidateQueries({ queryKey: ['lessonFiles'] });
        setEditingLesson(null);
    };

    const handleEdit = (lesson: Lesson) => {
        const user = JSON.parse(localStorage.getItem('user') || '{}');
        if (userRole === 'FAKE_ADMIN' && lesson.createdBy?.id !== user.id) {
            setIsFakeAdminRestrictionModalOpen(true);
            return;
        }
        setEditingLesson(lesson);
        setIsModalOpen(true);
    };

    const handleDelete = (lesson: Lesson) => {
        const user = JSON.parse(localStorage.getItem('user') || '{}');
        if (userRole === 'FAKE_ADMIN' && lesson.createdBy?.id !== user.id) {
            setIsFakeAdminRestrictionModalOpen(true);
            return;
        }
        setLessonToDelete(lesson);
        setIsConfirmModalOpen(true);
    };

    const confirmDelete = () => {
        if (lessonToDelete) {
            deleteMutation.mutate(lessonToDelete.id);
            setIsConfirmModalOpen(false);
            setLessonToDelete(null);
        }
    };

    const handleFileDelete = () => {
        queryClient.invalidateQueries({ queryKey: ['lessonFiles'] });
    };

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

    // Map module ID to course ID for efficient lookup
    const moduleCourseMap = useMemo(() => {
        const map = new Map<string, string>();
        modules?.forEach(m => {
            if (m.courseId) map.set(m.id, m.courseId);
        });
        return map;
    }, [modules]);

    const filteredLessons = useMemo(() => {
        if (!lessons) return [];
        return lessons.filter(lesson => {
            // Filter by Course
            if (selectedCourseIds.length > 0) {
                const courseId = moduleCourseMap.get(lesson.moduleId);
                const explicitCourseIds = selectedCourseIds.filter(id => id !== 'unassigned');
                const includeUnassigned = selectedCourseIds.includes('unassigned');

                const matchesExplicit = courseId && explicitCourseIds.includes(courseId);
                const matchesUnassigned = !courseId && includeUnassigned;

                if (!matchesExplicit && !matchesUnassigned) {
                    return false;
                }
            }

            // Filter by Module
            if (selectedModuleIds.length > 0) {
                const explicitModuleIds = selectedModuleIds.filter(id => id !== 'unassigned');
                const includeUnassigned = selectedModuleIds.includes('unassigned');

                const matchesExplicit = lesson.moduleId && explicitModuleIds.includes(lesson.moduleId);
                // Check if lesson has no module (or empty string/null)
                const matchesUnassigned = !lesson.moduleId && includeUnassigned;

                if (!matchesExplicit && !matchesUnassigned) {
                    return false;
                }
            }

            return true;
        });
    }, [lessons, selectedCourseIds, selectedModuleIds, moduleCourseMap]);

    // Derived counts
    const totalLessons = lessons?.length || 0;
    const displayedLessons = filteredLessons.length;

    if (lessonsLoading) {
        return (
            <div className="flex items-center justify-center min-h-screen">
                <div className="text-brand-primary font-medium">{t('allLessons.loading', 'Завантаження уроків...')}</div>
            </div>
        );
    }

    const courseOptions = [
        { value: 'unassigned', label: t('allLessons.noCourseLabel', 'Без курсу') },
        ...(courses?.map(c => ({ value: c.id, label: c.name })) || [])
    ];

    const moduleOptions = [
        { value: 'unassigned', label: t('allLessons.noModuleLabel', 'Без модуля') },
        ...filteredModules.map(m => ({ value: m.id, label: m.name }))
    ];

    return (
        <div className="container mx-auto px-6 py-12">
            {/* Header Section */}
            <div className="flex items-center justify-between mb-8">
                <div className="flex items-center gap-3">
                    <h1 className="text-3xl font-bold text-brand-dark">{t('allLessons.title', 'Всі уроки')}</h1>
                    <span className="px-3 py-1 bg-transparent text-rose-900 text-xl font-normal rounded-lg border-2 border-rose-900 self-center leading-none">
                        {displayedLessons}{displayedLessons !== totalLessons ? ` / ${totalLessons}` : ''}
                    </span>
                </div>

                {isAdmin && (
                    <button
                        onClick={() => {
                            setEditingLesson(null);
                            setIsModalOpen(true);
                        }}
                        className="flex items-center gap-2 px-4 py-2 text-gray-900 font-medium hover:bg-gray-100 rounded-lg transition-colors"
                    >
                        <Plus className="w-5 h-5" />
                        <span>{t('allLessons.addLessonBtn', 'Додати урок')}</span>
                    </button>
                )}
            </div>

            {/* Filters */}
            <div className="flex flex-wrap gap-4 mb-8 bg-white p-3 rounded-lg border border-gray-100 shadow-sm items-center">
                <div className="flex items-center gap-2 text-gray-500 mr-2">
                    <Filter className="w-5 h-5" />
                    <span className="font-medium text-sm">{t('allLessons.filters', 'Фільтри:')}</span>
                </div>

                {/* Course Filter */}
                <div className="min-w-[250px]">
                    <MultiSelect
                        label=""
                        placeholder={t('allLessons.allCoursesPlaceholder', 'Всі курси')}
                        options={courseOptions}
                        selectedValues={selectedCourseIds}
                        onChange={(ids) => {
                            setSelectedCourseIds(ids);
                        }}
                    />
                </div>

                {/* Module Filter */}
                <div className="min-w-[250px]">
                    <MultiSelect
                        label=""
                        placeholder={t('allLessons.allModulesPlaceholder', 'Всі модулі')}
                        options={moduleOptions}
                        selectedValues={selectedModuleIds}
                        onChange={setSelectedModuleIds}
                    />
                </div>

                {(selectedCourseIds.length > 0 || selectedModuleIds.length > 0) && (
                    <button
                        onClick={() => {
                            setSelectedCourseIds([]);
                            setSelectedModuleIds([]);
                        }}
                        className="text-sm text-red-500 hover:text-red-600 font-medium px-2 ml-auto"
                    >
                        {t('allLessons.resetAllBtn', 'Скинути всі')}
                    </button>
                )}
            </div>

            {/* Lessons List */}
            {filteredLessons.length === 0 ? (
                <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-16 text-center">
                    <div className="w-16 h-16 bg-gray-50 rounded-full flex items-center justify-center mx-auto mb-4">
                        <Filter className="w-8 h-8 text-gray-300" />
                    </div>
                    <p className="text-gray-500 text-lg mb-2">{t('allLessons.noLessonsFound', 'Уроків не знайдено')}</p>
                    <p className="text-gray-400 text-sm">
                        {t('allLessons.noLessonsDesc', 'Спробуйте змінити параметри фільтрації або створіть новий урок')}
                    </p>
                </div>
            ) : (
                <div className="flex flex-col space-y-2">
                    {filteredLessons.map((lesson) => (
                        <LessonCard
                            key={lesson.id}
                            lesson={lesson}
                            files={lessonFilesQueries.data?.[lesson.id] || []}
                            onImageClick={setPreviewImage}
                            onEdit={handleEdit}
                            onDelete={() => handleDelete(lesson)}
                            onFileDelete={handleFileDelete}
                            isCatalogMode={true}
                        />
                    ))}
                </div>
            )}

            <LessonModal
                isOpen={isModalOpen}
                onClose={() => {
                    setIsModalOpen(false);
                    setEditingLesson(null);
                }}
                onSuccess={handleSuccess}
                initialData={editingLesson}
            />

            <ImagePreviewModal
                isOpen={!!previewImage}
                imageUrl={previewImage || ''}
                onClose={() => setPreviewImage(null)}
            />

            <FakeAdminRestrictionModal
                isOpen={isFakeAdminRestrictionModalOpen}
                onClose={() => setIsFakeAdminRestrictionModalOpen(false)}
            />

            <ConfirmModal
                isOpen={isConfirmModalOpen}
                onClose={() => {
                    setIsConfirmModalOpen(false);
                    setLessonToDelete(null);
                }}
                onConfirm={confirmDelete}
                title={t('allLessons.deleteConfirmTitle', 'Підтвердження видалення')}
                message={t('allLessons.deleteConfirmMessage', 'Ви впевнені, що хочете видалити урок "{{name}}"?', { name: lessonToDelete?.name || '' })}
                warningMessage={t('allLessons.deleteWarningMessage', 'Ця дія незворотна. Після видалення всі файли та матеріали цього уроку будуть видалені з системи назавжди.')}
                confirmText={t('common.deleteBtn', 'Видалити')}
                cancelText={t('common.cancelBtn', 'Скасувати')}
                type="danger"
                isLoading={deleteMutation.isPending}
            />

            <ConfirmModal
                isOpen={isAlertOpen}
                onClose={() => setIsAlertOpen(false)}
                onConfirm={() => setIsAlertOpen(false)}
                title={alertTitle}
                message={alertMessage}
                isAlert={true}
                type="warning"
            />
        </div>
    );
}
