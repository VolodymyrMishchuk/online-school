import React from 'react';
import { Edit2, Trash2, BookOpen } from 'lucide-react';
import type { Module } from '../api/modules';
import type { Lesson } from '../api/lessons';

interface ModuleCardProps {
    module: Module;
    lessons: Lesson[];
    onEdit: (module: Module, lessons: Lesson[]) => void;
    onDelete: (id: string) => void;
}

export const ModuleCard: React.FC<ModuleCardProps> = ({
    module,
    lessons,
    onEdit,
    onDelete,
}) => {
    const handleDelete = () => {
        if (window.confirm(`Ви впевнені, що хочете видалити "${module.name}"?`)) {
            onDelete(module.id);
        }
    };

    return (
        <div className="bg-white rounded-lg shadow-md hover:shadow-lg transition-shadow duration-200 overflow-hidden">
            {/* Header */}
            <div className="bg-gradient-to-r from-blue-500 to-blue-600 p-4">
                <div className="flex justify-between items-start">
                    <div className="flex-1">
                        <h3 className="text-xl font-bold text-white mb-1">
                            {module.name}
                        </h3>
                        <p className="text-blue-100 text-sm">
                            {lessons.length} урок{lessons.length === 1 ? '' : (lessons.length > 1 && lessons.length < 5 ? 'и' : 'ів')}
                        </p>
                    </div>
                    <div className="flex gap-2">
                        <button
                            onClick={() => onEdit(module, lessons)}
                            className="p-2 bg-white/20 hover:bg-white/30 rounded-lg transition-colors"
                            title="Редагувати модуль"
                        >
                            <Edit2 size={18} className="text-white" />
                        </button>
                        <button
                            onClick={handleDelete}
                            className="p-2 bg-white/20 hover:bg-red-500 rounded-lg transition-colors"
                            title="Видалити модуль"
                        >
                            <Trash2 size={18} className="text-white" />
                        </button>
                    </div>
                </div>
            </div>

            {/* Body */}
            <div className="p-4">
                {/* Description */}
                <p className="text-gray-600 text-sm mb-4">
                    {module.description}
                </p>

                {/* Lessons List */}
                {lessons.length > 0 ? (
                    <div className="space-y-2">
                        <div className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2">
                            <BookOpen size={16} />
                            <span>Уроки</span>
                        </div>
                        <div className="space-y-1">
                            {lessons.map((lesson, index) => (
                                <div
                                    key={lesson.id}
                                    className="flex items-start gap-2 p-2 bg-gray-50 rounded-lg"
                                >
                                    <span className="text-xs font-medium text-gray-500 mt-0.5">
                                        {index + 1}.
                                    </span>
                                    <div className="flex-1 min-w-0">
                                        <div className="text-sm font-medium text-gray-900 truncate">
                                            {lesson.name}
                                        </div>
                                        {lesson.description && (
                                            <div className="text-xs text-gray-500 truncate">
                                                {lesson.description}
                                            </div>
                                        )}
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>
                ) : (
                    <div className="text-center py-6 text-gray-400">
                        <BookOpen size={32} className="mx-auto mb-2 opacity-50" />
                        <p className="text-sm">Уроки ще не призначено</p>
                    </div>
                )}
            </div>
        </div>
    );
};
