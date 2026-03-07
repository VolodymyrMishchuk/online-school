import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { createCourse, deleteCourse, getCourses, updateCourse } from '../api/courses';
import type { CourseDto, CreateCourseDto } from '../api/courses';
import { enrollInCourse } from '../api/enrollments';

import { getModules } from '../api/modules';
import { CourseModal } from '../components/CourseModal';
import CourseExpandableCard from '../components/CourseExpandableCard';
import { BookOpen, Plus } from 'lucide-react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { ConfirmModal } from '../components/ConfirmModal';

export default function AllCoursesPage() {
    const { t } = useTranslation();
    const queryClient = useQueryClient();
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [editingCourse, setEditingCourse] = useState<CourseDto | undefined>(undefined);
    const [courseToDelete, setCourseToDelete] = useState<CourseDto | null>(null);

    // Alert Modal State
    const [isAlertOpen, setIsAlertOpen] = useState(false);
    const [alertMessage, setAlertMessage] = useState('');

    const showAlert = (message: string) => {
        setAlertMessage(message);
        setIsAlertOpen(true);
    };

    const userRole = localStorage.getItem('userRole') || 'USER';
    const isAdmin = userRole === 'ADMIN' || userRole === 'FAKE_ADMIN';

    const { data: courses, isLoading: coursesLoading } = useQuery({
        queryKey: ['allCourses'], // No userId dependency for general catalog
        queryFn: () => getCourses() // Fetch generic courses to decouple from "My Courses" statuses
    });

    const { data: modules } = useQuery({
        queryKey: ['allModules'],
        queryFn: () => getModules()
    });



    const createMutation = useMutation({
        mutationFn: ({ data, file }: { data: CreateCourseDto; file?: File }) => createCourse(data, file),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['allCourses'] });
            queryClient.invalidateQueries({ queryKey: ['allModules'] }); // Update modules status
        }
    });

    const updateMutation = useMutation({
        mutationFn: ({ id, dto, file }: { id: string; dto: any; file?: File }) => updateCourse(id, dto, file),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['allCourses'] });
            queryClient.invalidateQueries({ queryKey: ['allModules'] }); // Update modules status
        }
    });

    const deleteMutation = useMutation({
        mutationFn: deleteCourse,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['allCourses'] });
            queryClient.invalidateQueries({ queryKey: ['allModules'] }); // Update modules status
            setCourseToDelete(null);
        }
    });

    const enrollMutation = useMutation({
        mutationFn: ({ studentId, courseId }: { studentId: string; courseId: string }) =>
            enrollInCourse(studentId, courseId),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['allCourses'] });
            queryClient.invalidateQueries({ queryKey: ['myEnrollments'] });
        }
    });

    const handleRefresh = () => {
        queryClient.invalidateQueries({ queryKey: ['allCourses'] });
        queryClient.invalidateQueries({ queryKey: ['allModules'] });
    };

    const handleEnroll = (courseId: string) => {
        const course = courses?.find(c => c.id === courseId);
        if (course?.isEnrolled) {
            showAlert(t('allCourses.alreadyEnrolled', 'Ви вже придбали цей курс!'));
            return;
        }

        const userId = localStorage.getItem('userId') || '';
        if (userId) {
            enrollMutation.mutate({ studentId: userId, courseId });
        }
    };

    const handleCreateClick = () => {
        setEditingCourse(undefined);
        setIsModalOpen(true);
    };

    const handleEditClick = (course: CourseDto) => {
        setEditingCourse(course);
        setIsModalOpen(true);
    };

    const handleDeleteClick = (course: CourseDto) => {
        setCourseToDelete(course);
    };

    const confirmDelete = () => {
        if (courseToDelete) {
            deleteMutation.mutate(courseToDelete.id);
        }
    };

    const handleModalSubmit = async (data: CreateCourseDto, file?: File) => {
        if (editingCourse) {
            await updateMutation.mutateAsync({
                id: editingCourse.id,
                dto: {
                    ...data,
                    status: editingCourse.status // Preserve status for now
                },
                file
            });
        } else {
            await createMutation.mutateAsync({ data, file });
        }
    };

    if (coursesLoading) {
        return (
            <div className="flex items-center justify-center min-h-screen">
                <div className="text-brand-primary font-medium">{t('allCourses.loading', 'Завантаження курсів...')}</div>
            </div>
        );
    }

    return (
        <div className="container mx-auto px-6 py-8">
            <div className="flex justify-between items-center mb-8">
                <h1 className="text-3xl font-bold text-brand-dark">{t('allCourses.title', 'Всі курси')}</h1>
                {isAdmin && (
                    <button
                        onClick={handleCreateClick}
                        className="flex items-center space-x-2 px-4 py-2 text-gray-900 font-medium hover:bg-gray-100 rounded-lg transition-colors"
                    >
                        <Plus size={20} />
                        <span>{t('allCourses.addCourseBtn', 'Додати курс')}</span>
                    </button>
                )}
            </div>

            {courses?.length === 0 ? (
                <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-12 text-center">
                    <BookOpen size={48} className="mx-auto text-gray-300 mb-4" />
                    <p className="text-gray-500 mb-6 text-lg">{t('allCourses.noCourses', 'Курсів поки що немає')}</p>
                    {isAdmin && (
                        <button
                            onClick={handleCreateClick}
                            className="inline-flex items-center gap-2 px-6 py-3 bg-brand-primary text-white rounded-lg hover:bg-brand-primary/90 transition-colors"
                        >
                            <Plus size={20} />
                            {t('allCourses.createFirstCourseBtn', 'Створити Перший Курс')}
                        </button>
                    )}
                </div>
            ) : (
                <div className="flex flex-col space-y-2">
                    {courses?.map((course) => (
                        <CourseExpandableCard
                            key={course.id}
                            course={course}
                            modules={modules || []}
                            onEdit={handleEditClick}
                            onDelete={handleDeleteClick}
                            onEnroll={handleEnroll}
                            onEditLesson={undefined}
                            onDeleteLesson={undefined}
                            isCatalogMode={true}
                            onRefresh={handleRefresh}
                        />
                    ))}
                </div>
            )}

            <CourseModal
                isOpen={isModalOpen}
                onClose={() => setIsModalOpen(false)}
                onSubmit={handleModalSubmit}
                modules={modules || []}
                courses={courses || []}
                initialData={editingCourse}
                initialModuleIds={
                    editingCourse && modules
                        ? modules.filter(m => m.courseId === editingCourse.id).map(m => m.id)
                        : []
                }
            />

            <ConfirmModal
                isOpen={!!courseToDelete}
                onClose={() => setCourseToDelete(null)}
                onConfirm={confirmDelete}
                title={t('courseExpandableCard.deleteConfirmTitle', 'Підтвердження видалення')}
                message={t('courseExpandableCard.deleteConfirmMessage', 'Ви дійсно бажаєте видалити курс "{{name}}"?', { name: courseToDelete?.name })}
                warningMessage={t('courseExpandableCard.deleteWarningMessage', 'Ця дія є незворотною. Після видалення ви та всі учні втратять доступ до матеріалів курсу. Усі файли та уроки будуть назавжди стерті з системи. Кошти автоматично не повертаються.')}
                confirmText={t('allCourses.deleteBtn', 'Видалити')}
                cancelText={t('allCourses.cancelBtn', 'Скасувати')}
            />

            <ConfirmModal
                isOpen={isAlertOpen}
                onClose={() => setIsAlertOpen(false)}
                onConfirm={() => setIsAlertOpen(false)}
                title={t('common.notification', 'Сповіщення')}
                message={alertMessage}
                isAlert={true}
                type="info"
            />
        </div>
    );
}
