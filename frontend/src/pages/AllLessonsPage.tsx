import { useState } from 'react';
import { useQuery, useQueryClient, useMutation } from '@tanstack/react-query';
import { FileText, Plus } from 'lucide-react';
import { getLessons, deleteLesson } from '../api/lessons';
import type { Lesson } from '../api/lessons';
import { getLessonFiles, type FileDto } from '../api/files';
import LessonCard from '../components/LessonCard';
import LessonModal from '../components/LessonModal';
import ImagePreviewModal from '../components/ImagePreviewModal';

export default function AllLessonsPage() {
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [editingLesson, setEditingLesson] = useState<Lesson | null>(null);
    const [previewImage, setPreviewImage] = useState<string | null>(null);
    const queryClient = useQueryClient();

    const { data: lessons, isLoading } = useQuery({
        queryKey: ['allLessons'],
        queryFn: getLessons,
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
            alert('Помилка видалення уроку: ' + (error.response?.data?.message || error.message));
        },
    });

    const handleSuccess = () => {
        queryClient.invalidateQueries({ queryKey: ['allLessons'] });
        queryClient.invalidateQueries({ queryKey: ['lessonFiles'] });
        setEditingLesson(null);
    };

    const handleEdit = (lesson: Lesson) => {
        setEditingLesson(lesson);
        setIsModalOpen(true);
    };

    const handleDelete = (lessonId: string) => {
        if (window.confirm('Ви впевнені, що хочете видалити цей урок?')) {
            deleteMutation.mutate(lessonId);
        }
    };

    const handleFileDelete = () => {
        // Refresh lesson files after deletion
        queryClient.invalidateQueries({ queryKey: ['lessonFiles'] });
    };

    if (isLoading) {
        return (
            <div className="flex items-center justify-center min-h-screen">
                <div className="text-brand-primary font-medium">Завантаження уроків...</div>
            </div>
        );
    }

    return (
        <div className="container mx-auto px-6 py-12">
            <div className="flex items-center justify-between mb-8">
                <div className="flex items-center gap-3">
                    <FileText className="w-8 h-8 text-brand-primary" />
                    <h1 className="text-3xl font-bold text-brand-dark">Всі уроки</h1>
                    <span className="text-gray-400 font-medium">({lessons?.length || 0})</span>
                </div>

                <button
                    onClick={() => {
                        setEditingLesson(null);
                        setIsModalOpen(true);
                    }}
                    className="flex items-center gap-2 px-6 py-3 bg-brand-primary text-white font-semibold rounded-2xl hover:bg-brand-primary/90 transition-all shadow-sm hover:shadow-md"
                >
                    <Plus className="w-5 h-5" />
                    Додати урок
                </button>
            </div>

            {lessons?.length === 0 ? (
                <div className="bg-white rounded-3xl shadow-sm border border-gray-100 p-16 text-center">
                    <FileText className="w-16 h-16 text-gray-300 mx-auto mb-4" />
                    <p className="text-gray-500 text-lg mb-2">Уроків поки що немає</p>
                    <p className="text-gray-400 text-sm mb-6">
                        Створіть перший урок, натиснувши кнопку "Додати урок"
                    </p>
                    <button
                        onClick={() => {
                            setEditingLesson(null);
                            setIsModalOpen(true);
                        }}
                        className="inline-flex items-center gap-2 px-6 py-3 bg-brand-primary text-white font-semibold rounded-2xl hover:bg-brand-primary/90 transition-all"
                    >
                        <Plus className="w-5 h-5" />
                        Створити урок
                    </button>
                </div>
            ) : (
                <div className="flex flex-col space-y-4">
                    {lessons?.map((lesson) => (
                        <LessonCard
                            key={lesson.id}
                            lesson={lesson}
                            files={lessonFilesQueries.data?.[lesson.id] || []}
                            onImageClick={setPreviewImage}
                            onEdit={handleEdit}
                            onDelete={handleDelete}
                            onFileDelete={handleFileDelete}
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
        </div>
    );
}
