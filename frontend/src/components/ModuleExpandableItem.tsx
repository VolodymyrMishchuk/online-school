import { useState } from 'react';
import { useTranslation } from 'react-i18next';
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
    isTransparent?: boolean;
    isCatalogMode?: boolean;
}

export function ModuleExpandableItem({ module, isLocked = false, onEditLesson, onDeleteLesson, isTransparent = false, isCatalogMode = false }: ModuleExpandableItemProps) {
    const { t } = useTranslation();
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
        <div className={`border rounded-lg overflow-hidden mb-2 last:mb-0 transition-colors ${isTransparent ? 'border-white/20 bg-white/40 backdrop-blur-md' : 'border-gray-100 bg-white'}`}>
            <div
                className={`flex items-center justify-between p-3 cursor-pointer hover:bg-gray-50/50 transition-colors ${isExpanded ? (isTransparent ? 'bg-white/50' : 'bg-gray-50') : (isTransparent ? 'bg-transparent' : 'bg-white')}`}
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
                                    t('moduleExpandableItem.lessonsCount', { count: module.lessonsNumber || 0 })
                                }
                            </span>
                        </div>
                        {module.durationMinutes && module.durationMinutes > 0 && (
                            <div className="flex items-center gap-1.5">
                                <Clock size={16} />
                                <span>
                                    {module.durationMinutes < 60
                                        ? t('moduleExpandableItem.durationMinutes', '{{count}} хв', { count: module.durationMinutes })
                                        : t('moduleExpandableItem.durationHoursMinutes', '{{hours}} год {{minutes}} хв', {
                                            hours: Math.floor(module.durationMinutes / 60),
                                            minutes: module.durationMinutes % 60
                                        }).replace(' 0 хв', '')
                                    }
                                </span>
                            </div>
                        )}
                    </div>
                    {isExpanded ? <ChevronUp size={20} className="text-gray-400" /> : <ChevronDown size={20} className="text-gray-400" />}
                </div>
            </div>

            <div className={`grid transition-all duration-300 ease-in-out ${isExpanded ? 'grid-rows-[1fr] opacity-100' : 'grid-rows-[0fr] opacity-0'}`}>
                <div className="overflow-hidden">
                    <div className={`p-3 border-t ${isTransparent ? 'bg-white/30 border-white/20' : 'bg-gray-50 border-gray-100'}`}>
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
                                        isTransparent={isTransparent}
                                        isCatalogMode={isCatalogMode}
                                    />
                                ))}
                            </div>
                        ) : (
                            <div className="text-center py-4 text-gray-500 italic">
                                {t('moduleExpandableItem.noLessonsYet', 'У цьому модулі поки немає уроків.')}
                            </div>
                        )}
                    </div>
                </div>
            </div>

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
