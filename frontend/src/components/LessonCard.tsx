import { useState } from 'react';
import { FileText, Paperclip, ChevronDown, ChevronUp, Clock, FileIcon, FileImage, Pencil, Trash2 } from 'lucide-react';
import type { Lesson } from '../api/lessons';
import type { FileDto } from '../api/files';
import { downloadFile, deleteFile } from '../api/files';
import { VideoPlayer } from './VideoPlayer';

interface LessonCardProps {
    lesson: Lesson;
    files?: FileDto[];
    onImageClick: (url: string) => void;
    onEdit?: (lesson: Lesson) => void;
    onDelete?: (lessonId: string) => void;
    onFileDelete?: (fileId: string) => void;
}

export default function LessonCard({ lesson, files = [], onImageClick, onEdit, onDelete, onFileDelete }: LessonCardProps) {
    const [isExpanded, setIsExpanded] = useState(false);

    const getFileIcon = (contentType: string) => {
        if (contentType.startsWith('image/')) return <FileImage className="w-5 h-5 text-purple-500" />;
        if (contentType === 'application/pdf') return <FileText className="w-5 h-5 text-red-500" />;
        return <FileIcon className="w-5 h-5 text-gray-500" />;
    };

    const handleFileDelete = async (fileId: string, fileName: string, e: React.MouseEvent) => {
        e.stopPropagation();

        if (!confirm(`Ви впевнені, що хочете видалити файл "${fileName}"?`)) {
            return;
        }

        try {
            await deleteFile(fileId);
            if (onFileDelete) {
                onFileDelete(fileId);
            }
        } catch (error) {
            console.error('Failed to delete file:', error);
            alert('Помилка при видаленні файлу');
        }
    };

    const handleFileClick = async (file: FileDto) => {
        if (!file.downloadUrl) return;

        try {
            const blob = await downloadFile(file.id);
            const url = URL.createObjectURL(blob);

            if (file.contentType.startsWith('image/')) {
                onImageClick(url);
            } else {
                window.open(url, '_blank');
                // Clean up object URL after a delay to allow opening
                setTimeout(() => URL.revokeObjectURL(url), 1000);
            }
        } catch (error) {
            console.error('Error downloading file:', error);
            // alert('Помилка при завантаженні файлу. Перевірте, чи ви авторизовані.');
        }
    };

    return (
        <div
            className={`bg-white rounded-3xl p-6 shadow-sm border border-gray-100 hover:shadow-lg transition-all duration-300 w-full cursor-pointer ${isExpanded ? 'ring-2 ring-brand-primary/10' : ''}`}
            onClick={() => setIsExpanded(!isExpanded)}
        >
            <div className="flex items-start gap-4">
                <div className="bg-brand-primary/10 p-3 rounded-2xl h-fit">
                    <FileText className="w-6 h-6 text-brand-primary" />
                </div>

                <div className="flex-1 min-w-0">
                    <div className="flex items-center justify-between mb-2">
                        <div className="flex-1 pr-4">
                            <h3 className="text-xl font-bold text-brand-dark mb-1 truncate">
                                {lesson.name}
                            </h3>
                            <div className="flex flex-col gap-0.5 text-sm">
                                {lesson.courseName && (
                                    <p className="text-gray-600 font-medium flex items-center gap-1.5">
                                        <span className="text-brand-primary">Курс:</span> {lesson.courseName}
                                    </p>
                                )}
                                {lesson.moduleName && (
                                    <p className="text-gray-500 font-medium flex items-center gap-1.5">
                                        <span className="text-brand-primary/80">Модуль:</span> {lesson.moduleName}
                                    </p>
                                )}
                            </div>
                        </div>
                        <div className="flex items-center gap-4 text-gray-400 shrink-0">
                            {files.length > 0 && (
                                <div className="flex items-center gap-1.5" title={`${files.length} файлів`}>
                                    <Paperclip className="w-4 h-4" />
                                    <span className="text-sm font-medium">{files.length}</span>
                                </div>
                            )}
                            {lesson.durationMinutes && (
                                <div className="flex items-center gap-1.5" title={`${lesson.durationMinutes} хв`}>
                                    <Clock className="w-4 h-4" />
                                    <span className="text-sm font-medium">{lesson.durationMinutes} хв</span>
                                </div>
                            )}

                            {/* Action Buttons */}
                            <div className="flex items-center gap-1" onClick={(e) => e.stopPropagation()}>
                                {onEdit && (
                                    <button
                                        onClick={() => onEdit(lesson)}
                                        className="p-2 hover:bg-gray-100 rounded-lg text-gray-500 hover:text-brand-primary transition-colors"
                                        title="Редагувати"
                                    >
                                        <Pencil className="w-4 h-4" />
                                    </button>
                                )}
                                {onDelete && (
                                    <button
                                        onClick={() => onDelete(lesson.id)}
                                        className="p-2 hover:bg-red-50 rounded-lg text-gray-500 hover:text-red-500 transition-colors"
                                        title="Видалити"
                                    >
                                        <Trash2 className="w-4 h-4" />
                                    </button>
                                )}
                            </div>

                            {isExpanded ? <ChevronUp className="w-5 h-5" /> : <ChevronDown className="w-5 h-5" />}
                        </div>
                    </div>

                    {/* Content visible when expanded */}
                    <div className={`grid transition-all duration-500 ease-in-out ${isExpanded ? 'grid-rows-[1fr] opacity-100 mt-6' : 'grid-rows-[0fr] opacity-0'}`}>
                        <div className="overflow-hidden">
                            {/* Description */}
                            <div className="mb-6">
                                <p className="text-gray-600 leading-relaxed whitespace-pre-wrap">
                                    {lesson.description}
                                </p>
                            </div>

                            {/* Video Player */}
                            {lesson.videoUrl && (
                                <div className="mb-6">
                                    <VideoPlayer
                                        url={lesson.videoUrl}
                                        title={lesson.name}
                                    />
                                </div>
                            )}

                            {/* Files */}
                            {files.length > 0 && (
                                <div className="mt-8 pt-6 border-t border-gray-100">
                                    <h4 className="text-sm font-semibold text-gray-900 mb-4 uppercase tracking-wide flex items-center gap-2">
                                        <Paperclip className="w-4 h-4" />
                                        Матеріали ({files.length})
                                    </h4>
                                    <div className="flex flex-col gap-2">
                                        {files.map((file) => (
                                            <div
                                                key={file.id}
                                                className="flex items-center gap-3 p-3 bg-gray-50 hover:bg-white border border-gray-100 hover:border-brand-primary/30 rounded-xl transition-all group w-full"
                                            >
                                                <div
                                                    className="flex items-center gap-3 flex-1 min-w-0 cursor-pointer"
                                                    onClick={(e) => {
                                                        e.stopPropagation();
                                                        handleFileClick(file);
                                                    }}
                                                >
                                                    <div className="p-2 bg-white rounded-lg shadow-sm group-hover:scale-110 transition-transform">
                                                        {getFileIcon(file.contentType)}
                                                    </div>
                                                    <div className="flex-1 min-w-0">
                                                        <p className="font-medium text-gray-700 truncate text-sm group-hover:text-brand-primary transition-colors">
                                                            {file.fileName}
                                                        </p>
                                                        <p className="text-xs text-gray-400">
                                                            {file.fileSize ? (file.fileSize / 1024 / 1024).toFixed(2) + ' MB' : 'Unknown size'}
                                                        </p>
                                                    </div>
                                                </div>
                                                <button
                                                    onClick={(e) => handleFileDelete(file.id, file.fileName, e)}
                                                    className="p-2 text-gray-400 hover:text-red-500 hover:bg-red-50 rounded-lg transition-colors"
                                                    title="Видалити файл"
                                                >
                                                    <Trash2 className="w-4 h-4" />
                                                </button>
                                            </div>
                                        ))}
                                    </div>
                                </div>
                            )}
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}
