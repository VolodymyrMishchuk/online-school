import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Edit2, Trash2, Folder, BookOpen, ChevronDown, ChevronUp, Clock, Paperclip } from 'lucide-react';
import type { Module } from '../api/modules';
import type { Lesson } from '../api/lessons';
import { ConfirmModal } from './ConfirmModal';

interface ModuleCardProps {
    module: Module;
    lessons: Lesson[];
    courseName?: string;
    durationMinutes?: number;
    filesCount?: number;
    onEdit: (module: Module, lessons: Lesson[]) => void;
    onDelete: (id: string) => void;
    isCatalogMode?: boolean;
}

export const ModuleCard: React.FC<ModuleCardProps> = ({
    module,
    lessons,
    courseName,
    durationMinutes,
    filesCount,
    onEdit,
    onDelete,
    isCatalogMode = false,
}) => {
    const { t } = useTranslation();
    const [isExpanded, setIsExpanded] = useState(false);
    const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);

    const userRole = localStorage.getItem('userRole') || 'USER';
    const isStandardUser = userRole === 'USER' || userRole === 'FAKE_USER';
    const isAdmin = !isStandardUser;

    const handleDelete = (e: React.MouseEvent) => {
        e.stopPropagation();
        setIsDeleteModalOpen(true);
    };

    const confirmDelete = () => {
        onDelete(module.id);
    };

    const handleEdit = (e: React.MouseEvent) => {
        e.stopPropagation();
        onEdit(module, lessons);
    };

    return (
        <div
            className={`bg-white rounded-lg p-5 shadow-sm border border-gray-100 transition-all duration-300 w-full hover:shadow-lg group ${isExpanded ? 'ring-2 ring-brand-primary/10' : ''} ${isCatalogMode && isStandardUser ? 'cursor-default' : 'cursor-pointer'}`}
            onClick={() => {
                if (isCatalogMode && isStandardUser) return;
                setIsExpanded(!isExpanded);
            }}
        >
            <div className="flex items-start gap-4">
                {/* Icon */}
                <div className="bg-brand-primary/10 p-3 rounded-2xl h-fit shrink-0">
                    <Folder className="w-6 h-6 text-brand-primary" />
                </div>

                {/* Main Header Content */}
                <div className="flex-1 min-w-0">
                    <div className="flex items-center justify-between mb-2">
                        <div className="flex-1 pr-4">
                            <h3 className="text-xl font-bold text-gray-900 mb-2 truncate">
                                {module.name}
                            </h3>

                            <div className="flex flex-col gap-1">
                                {courseName && (
                                    <p className="text-sm font-medium text-gray-700">
                                        <span className="text-brand-primary/80 mr-2">{t('moduleCard.course', 'Курс')}:</span>
                                        {courseName}
                                    </p>
                                )}
                                <div className="flex items-center gap-1.5 text-sm font-medium text-gray-700">
                                    <span className="text-brand-primary/80 mr-2">{t('moduleCard.lessons', 'Уроки')}:</span>
                                    <span>{lessons.length}</span>
                                </div>
                            </div>
                        </div>

                        {/* Actions & Expand Icon */}
                        <div className="flex items-center gap-4 text-gray-400 shrink-0">
                            {(filesCount !== undefined && filesCount > 0) && (
                                <div className="flex items-center gap-1.5 text-sm font-medium" title={t('moduleCard.filesTooltip', '{{count}} файлів', { count: filesCount })}>
                                    <Paperclip className="w-4 h-4" />
                                    <span>{filesCount}</span>
                                </div>
                            )}

                            {durationMinutes !== undefined && durationMinutes > 0 && (
                                <div className="flex items-center gap-1.5 text-sm font-medium" title={t('moduleCard.durationTooltip', '{{count}} хв', { count: durationMinutes })}>
                                    <Clock className="w-4 h-4" />
                                    <span>{durationMinutes} {t('moduleCard.minShort', 'хв')}</span>
                                </div>
                            )}

                            {isAdmin && (
                                <div className="flex items-center gap-1" onClick={(e) => e.stopPropagation()}>
                                    <button
                                        onClick={handleEdit}
                                        className="p-2 hover:bg-gray-100 rounded-lg text-gray-500 hover:text-brand-primary transition-colors"
                                        title={t('common.editBtn', 'Редагувати')}
                                    >
                                        <Edit2 className="w-4 h-4" />
                                    </button>
                                    <button
                                        onClick={handleDelete}
                                        className="p-2 hover:bg-red-50 rounded-lg text-gray-500 hover:text-red-500 transition-colors"
                                        title={t('common.deleteBtn', 'Видалити')}
                                    >
                                        <Trash2 className="w-4 h-4" />
                                    </button>
                                </div>
                            )}

                            {!(isCatalogMode && isStandardUser) && (
                                isExpanded ? <ChevronUp className="w-5 h-5" /> : <ChevronDown className="w-5 h-5" />
                            )}
                        </div>
                    </div>

                    {/* Expanded Content */}
                    <div className={`grid transition-all duration-500 ease-in-out ${isExpanded ? 'grid-rows-[1fr] opacity-100 mt-6' : 'grid-rows-[0fr] opacity-0'}`}>
                        <div className="overflow-hidden">
                            {/* Description */}
                            {module.description && (
                                <div className="mb-6">
                                    <p className="text-gray-600 leading-relaxed whitespace-pre-wrap">
                                        {module.description}
                                    </p>
                                </div>
                            )}

                            {/* Lessons List */}
                            <div className="mt-4 pt-6 border-t border-gray-100">
                                <h4 className="text-sm font-semibold text-gray-900 mb-4 uppercase tracking-wide flex items-center gap-2">
                                    <BookOpen className="w-4 h-4" />
                                    {t('moduleCard.lessonsList', 'Список уроків')} ({lessons.length})
                                </h4>

                                {lessons.length > 0 ? (
                                    <div className="flex flex-col gap-2">
                                        {lessons.map((lesson, index) => (
                                            <div
                                                key={lesson.id}
                                                className="flex items-center gap-3 p-3 bg-gray-50 rounded-xl w-full"
                                            >
                                                <span className="text-xs font-medium text-gray-400 w-6 font-mono shrink-0">
                                                    {(index + 1).toString().padStart(2, '0')}
                                                </span>
                                                <div className="flex-1 min-w-0">
                                                    <p className="font-medium text-gray-700 truncate text-sm">
                                                        {lesson.name}
                                                    </p>

                                                    {lesson.description && (
                                                        <p className="text-xs text-gray-400 truncate mt-0.5">
                                                            {lesson.description}
                                                        </p>
                                                    )}
                                                </div>

                                                <div className="flex items-center gap-4 shrink-0">
                                                    {lesson.filesCount !== undefined && lesson.filesCount > 0 && (
                                                        <div className="flex items-center gap-1.5 text-sm font-medium text-gray-400" title={t('moduleCard.files', 'Файли')}>
                                                            <Paperclip className="w-4 h-4" />
                                                            <span>{lesson.filesCount}</span>
                                                        </div>
                                                    )}
                                                    {lesson.durationMinutes !== undefined && lesson.durationMinutes > 0 && (
                                                        <div className="flex items-center gap-1.5 text-sm font-medium text-gray-400" title={t('moduleCard.duration', 'Тривалість')}>
                                                            <Clock className="w-4 h-4" />
                                                            <span>{lesson.durationMinutes} {t('moduleCard.minShort', 'хв')}</span>
                                                        </div>
                                                    )}
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                ) : (
                                    <div className="text-center py-6 text-gray-400 bg-gray-50/50 rounded-lg border border-dashed border-gray-100">
                                        <BookOpen size={24} className="mx-auto mb-2 opacity-30" />
                                        <p className="text-xs font-medium">{t('moduleCard.noLessonsAssigned', 'Уроки ще не призначено')}</p>
                                    </div>
                                )}
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            <ConfirmModal
                isOpen={isDeleteModalOpen}
                onClose={() => setIsDeleteModalOpen(false)}
                onConfirm={confirmDelete}
                title={t('confirmModal.deleteTitle', 'Підтвердження видалення')}
                message={t('confirmModal.deleteModuleMessage', 'Ви впевнені, що хочете видалити модуль "{{name}}"? Цю дію неможливо скасувати.', { name: module.name })}
                confirmText={t('confirmModal.deleteBtn', 'Видалити')}
                cancelText={t('confirmModal.cancelBtn', 'Скасувати')}
            />
        </div>
    );
};
