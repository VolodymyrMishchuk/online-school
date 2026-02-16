import { useState } from 'react';
import { ChevronDown, ChevronUp, Layers, BookOpen, Clock } from 'lucide-react';
import type { Module } from '../api/modules';
import type { Lesson } from '../api/lessons';
import { useQuery } from '@tanstack/react-query';
import { getModuleLessons } from '../api/modules';
import LessonCard from './LessonCard';
import type { FileDto } from '../api/files';
import { getLessonFiles } from '../api/files';
import ImagePreviewModal from './ImagePreviewModal';

interface ModuleExpandableItemProps {
    module: Module;
    isLocked?: boolean;
    onEditLesson?: (lesson: Lesson) => void;
    onDeleteLesson?: (lessonId: string) => void;
}

export function ModuleExpandableItem({ module, isLocked = false, onEditLesson, onDeleteLesson }: ModuleExpandableItemProps) {
    const [isExpanded, setIsExpanded] = useState(false);

    const { data: lessons, isLoading: lessonsLoading } = useQuery({
        queryKey: ['moduleLessons', module.id],
        queryFn: () => getModuleLessons(module.id),
        enabled: isExpanded, // Only fetch when expanded
    });

    // Fetch files for the lessons in this module
    const { data: filesMap = {} } = useQuery({
        queryKey: ['moduleLessonFiles', module.id],
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
        enabled: !!lessons && lessons.length > 0 && !isLocked, // Disable file fetching if locked
    });

    // Helper to get files for a specific lesson
    const getLessonFilesById = (lessonId: string) => {
        return filesMap[lessonId] || [];
    };

    const [previewImage, setPreviewImage] = useState<string | null>(null);

    return (
        <div className="border border-gray-100 rounded-lg overflow-hidden mb-2 last:mb-0">
            <div
                className={`flex items-center justify-between p-3 cursor-pointer hover:bg-gray-50 transition-colors ${isExpanded ? 'bg-gray-50' : 'bg-white'}`}
                onClick={() => setIsExpanded(!isExpanded)}
            >
                <div className="flex items-center gap-4">
                    <div className="bg-purple-100 p-2 rounded-lg">
                        <Layers className="text-purple-600" size={20} />
                    </div>
                    <div>
                        <h4 className="font-semibold text-gray-800">{module.name}</h4>
                        {module.description && (
                            <p className="text-sm text-gray-500 line-clamp-1">{module.description}</p>
                        )}
                    </div>
                </div>
                <div className="flex items-center gap-4">
                    <div className="flex items-center gap-4 text-sm text-gray-500">
                        <div className="flex items-center gap-1.5">
                            <BookOpen size={16} />
                            <span>
                                {module.lessonsNumber || 0} {
                                    (() => {
                                        const count = module.lessonsNumber || 0;
                                        if (count % 10 === 1 && count % 100 !== 11) return 'урок';
                                        if ([2, 3, 4].includes(count % 10) && ![12, 13, 14].includes(count % 100)) return 'уроки';
                                        return 'уроків';
                                    })()
                                }
                            </span>
                        </div>
                        {module.durationMinutes && module.durationMinutes > 0 && (
                            <div className="flex items-center gap-1.5">
                                <Clock size={16} />
                                <span>
                                    {module.durationMinutes < 60
                                        ? `${module.durationMinutes} хв`
                                        : `${Math.floor(module.durationMinutes / 60)} год ${module.durationMinutes % 60 > 0 ? `${module.durationMinutes % 60} хв` : ''}`
                                    }
                                </span>
                            </div>
                        )}
                    </div>
                    {isExpanded ? <ChevronUp size={20} className="text-gray-400" /> : <ChevronDown size={20} className="text-gray-400" />}
                </div>
            </div>

            {isExpanded && (
                <div className="bg-gray-50 p-3 border-t border-gray-100">
                    {lessonsLoading ? (
                        <div className="flex justify-center py-4">
                            <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-brand-primary"></div>
                        </div>
                    ) : lessons && lessons.length > 0 ? (
                        <div className="flex flex-col gap-4">
                            {lessons.map(lesson => (
                                <LessonCard
                                    key={lesson.id}
                                    lesson={lesson}
                                    files={getLessonFilesById(lesson.id)}
                                    isLocked={isLocked}
                                    onImageClick={(url) => setPreviewImage(url)}
                                    onEdit={onEditLesson}
                                    onDelete={onDeleteLesson}
                                />
                            ))}
                        </div>
                    ) : (
                        <div className="text-center py-4 text-gray-500 italic">
                            У цьому модулі поки немає уроків.
                        </div>
                    )}
                </div>
            )}

            {/* Image Preview Modal */}
            <ImagePreviewModal
                isOpen={!!previewImage}
                imageUrl={previewImage || ''}
                onClose={() => setPreviewImage(null)}
            />
        </div>
    );
}

export default ModuleExpandableItem;
