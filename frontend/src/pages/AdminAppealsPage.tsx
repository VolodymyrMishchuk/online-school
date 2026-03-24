import { useEffect, useState } from 'react';
import { createPortal } from 'react-dom';
import { useLocation } from 'react-router-dom';
import { getAppeals, updateAppealStatus, deleteAppeal } from '../api/appeals';
import type { AppealResponse, FileDto } from '../api/appeals';
import { format } from 'date-fns';
import { uk, enUS, de } from 'date-fns/locale';
import { Download, CheckCircle, Trash2, Mail, Phone, Instagram, Send, Loader2, MessageSquare, Image as ImageIcon, ZoomIn, ZoomOut, X, Maximize2, ChevronLeft, ChevronRight, Copy, Check, MessageCircle } from 'lucide-react';
import { downloadFile } from '../api/files';
import { useTranslation } from 'react-i18next';
import { ConfirmModal } from '../components/ConfirmModal';

function CopyButton({ text }: { text: string }) {
    const { t } = useTranslation();
    const [copied, setCopied] = useState(false);

    const handleCopy = (e: React.MouseEvent) => {
        e.stopPropagation();
        navigator.clipboard.writeText(text);
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
    };

    return (
        <button
            onClick={handleCopy}
            className="p-1.5 rounded-md text-gray-400 hover:text-gray-600 hover:bg-gray-100 transition-colors focus:outline-none flex-shrink-0"
            title={t('adminAppeals.copyBtn', 'Копіювати')}
        >
            {copied ? <Check className="w-3.5 h-3.5 text-emerald-500" /> : <Copy className="w-3.5 h-3.5" />}
        </button>
    );
}

function AppealImageGallery({ photos }: { photos: FileDto[] }) {
    const { t } = useTranslation();
    const [blobUrls, setBlobUrls] = useState<Record<string, string>>({});
    const [isOpen, setIsOpen] = useState(false);
    const [activeIndex, setActiveIndex] = useState(0);
    const [zoom, setZoom] = useState(1);

    // Panning state
    const [isDragging, setIsDragging] = useState(false);
    const [position, setPosition] = useState({ x: 0, y: 0 });
    const [dragStart, setDragStart] = useState({ x: 0, y: 0 });

    useEffect(() => {
        let active = true;
        const fetchImages = async () => {
            for (const photo of photos) {
                if (!blobUrls[photo.id]) {
                    try {
                        const blob = await downloadFile(photo.id);
                        if (active) {
                            setBlobUrls(prev => ({ ...prev, [photo.id]: URL.createObjectURL(blob) }));
                        }
                    } catch (e) {
                        console.error("Error loading image", e);
                    }
                }
            }
        };
        if (photos.length > 0) {
            fetchImages();
        }
        return () => {
            active = false;
        };
    }, [photos]);

    // Cleanup object URLs on unmount
    useEffect(() => {
        return () => {
            Object.values(blobUrls).forEach(url => URL.revokeObjectURL(url));
        };
    }, []);

    const handleDownload = (photoId: string, originalName: string, e?: React.MouseEvent) => {
        if (e) e.stopPropagation();
        const url = blobUrls[photoId];
        if (!url) return;
        const a = document.createElement('a');
        a.href = url;
        a.download = originalName || 'attachment.jpg';
        a.click();
    };

    const nextImage = (e: React.MouseEvent) => {
        e.stopPropagation();
        if (activeIndex < photos.length - 1) {
            setActiveIndex(activeIndex + 1);
            setZoom(1);
            setPosition({ x: 0, y: 0 });
        }
    };

    const prevImage = (e: React.MouseEvent) => {
        e.stopPropagation();
        if (activeIndex > 0) {
            setActiveIndex(activeIndex - 1);
            setZoom(1);
            setPosition({ x: 0, y: 0 });
        }
    };

    // Panning handlers
    const handleMouseDown = (e: React.MouseEvent) => {
        if (zoom <= 1) return;
        e.preventDefault(); // Prevent default image drag
        setIsDragging(true);
        setDragStart({ x: e.clientX - position.x, y: e.clientY - position.y });
    };

    const handleMouseMove = (e: React.MouseEvent) => {
        if (!isDragging || zoom <= 1) return;
        setPosition({
            x: e.clientX - dragStart.x,
            y: e.clientY - dragStart.y
        });
    };

    const handleMouseUpOrLeave = () => {
        setIsDragging(false);
    };

    const handleDownloadAll = (e: React.MouseEvent) => {
        e.stopPropagation();
        photos.forEach((photo) => {
            const url = blobUrls[photo.id];
            if (!url) return;
            const a = document.createElement('a');
            a.href = url;
            a.download = photo.originalName || 'attachment.jpg';
            document.body.appendChild(a); // Append to body for broader browser compat before clicking
            a.click();
            document.body.removeChild(a);
        });
    };

    const activePhoto = photos[activeIndex];
    const activeUrl = activePhoto ? blobUrls[activePhoto.id] : null;

    return (
        <div className="flex gap-2 overflow-x-auto pb-2 scrollbar-hide">
            {photos.map((photo, index) => {
                const url = blobUrls[photo.id];
                return (
                    <button
                        key={photo.id}
                        onClick={(e) => { e.stopPropagation(); setActiveIndex(index); setIsOpen(true); setZoom(1); }}
                        className="relative flex-shrink-0 w-16 h-16 rounded-lg border border-gray-200 overflow-hidden group/img hover:border-brand-primary transition-colors focus:outline-none focus:ring-2 focus:ring-brand-primary/50 cursor-pointer"
                        title={t('adminAppeals.clickToZoomTitle', 'Натисніть щоб збільшити')}
                    >
                        {url ? (
                            <img src={url} alt={t('adminAppeals.appealAttachmentAlt', 'Додаток до звернення')} className="w-full h-full object-cover" />
                        ) : (
                            <div className="absolute inset-0 bg-gray-50 flex items-center justify-center animate-pulse">
                                <ImageIcon className="w-6 h-6 text-gray-300" />
                            </div>
                        )}
                        <div className="absolute inset-0 bg-black/40 opacity-0 group-hover/img:opacity-100 flex items-center justify-center transition-opacity">
                            <Maximize2 className="w-4 h-4 text-white" />
                        </div>
                    </button>
                );
            })}

            {isOpen && createPortal(
                <div
                    className="fixed inset-0 z-[100] flex items-center justify-center bg-black/90 backdrop-blur-sm p-4"
                    onClick={(e) => { e.stopPropagation(); setIsOpen(false); }}
                >
                    <div
                        className="bg-white rounded-2xl shadow-2xl w-full max-w-5xl h-[95vh] flex flex-col overflow-hidden relative"
                        onClick={e => e.stopPropagation()}
                    >
                        {/* Header */}
                        <div className="flex items-center justify-between p-4 border-b border-gray-100 flex-shrink-0 bg-white z-20">
                            <div className="flex items-center gap-3">
                                <h3 className="font-bold text-gray-900 truncate">
                                    {activePhoto?.originalName || t('adminAppeals.imageFallback', 'Зображення')}
                                </h3>
                                <span className="px-2.5 py-1 text-xs font-semibold text-gray-500 bg-gray-100 rounded-full">
                                    {activeIndex + 1} {t('adminAppeals.of', 'з')} {photos.length}
                                </span>
                            </div>
                            <button
                                onClick={(e) => { e.stopPropagation(); setIsOpen(false); }}
                                className="p-2 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-full transition-colors focus:outline-none"
                            >
                                <X className="w-5 h-5" />
                            </button>
                        </div>

                        {/* Image Container with Zoom & Navigation */}
                        <div className="relative flex-1 bg-gray-100/50 overflow-hidden flex items-center justify-center group/container">

                            {/* Previous Button */}
                            {activeIndex > 0 && (
                                <button
                                    onClick={prevImage}
                                    className="absolute left-4 top-1/2 -translate-y-1/2 p-3 bg-white/80 hover:bg-white text-gray-700 rounded-full shadow-lg backdrop-blur transition-all focus:outline-none z-20 opacity-0 group-hover/container:opacity-100 disabled:opacity-50"
                                >
                                    <ChevronLeft className="w-6 h-6" />
                                </button>
                            )}

                            {/* Main Image Viewport with Panning */}
                            <div
                                className={`w-full h-full overflow-hidden flex items-center justify-center ${zoom > 1 ? (isDragging ? 'cursor-grabbing' : 'cursor-grab') : ''}`}
                                onMouseDown={handleMouseDown}
                                onMouseMove={handleMouseMove}
                                onMouseUp={handleMouseUpOrLeave}
                                onMouseLeave={handleMouseUpOrLeave}
                            >
                                {activeUrl ? (
                                    <img
                                        src={activeUrl}
                                        alt="Preview"
                                        className="max-w-none transition-transform duration-200 ease-out origin-center select-none"
                                        style={{
                                            transform: `translate(${position.x}px, ${position.y}px) scale(${zoom})`,
                                            maxHeight: zoom <= 1 ? '100%' : 'none',
                                            maxWidth: zoom <= 1 ? '100%' : 'none',
                                            // Provide smoother transition for zoom but not panning
                                            transitionProperty: isDragging ? 'none' : 'transform'
                                        }}
                                        draggable="false"
                                    />
                                ) : (
                                    <Loader2 className="w-10 h-10 animate-spin text-brand-primary" />
                                )}
                            </div>

                            {/* Next Button */}
                            {activeIndex < photos.length - 1 && (
                                <button
                                    onClick={nextImage}
                                    className="absolute right-4 top-1/2 -translate-y-1/2 p-3 bg-white/80 hover:bg-white text-gray-700 rounded-full shadow-lg backdrop-blur transition-all focus:outline-none z-20 opacity-0 group-hover/container:opacity-100 disabled:opacity-50"
                                >
                                    <ChevronRight className="w-6 h-6" />
                                </button>
                            )}

                            {/* Zoom Controls Overlay */}
                            <div className="absolute bottom-6 right-6 flex items-center bg-white/95 backdrop-blur shadow-lg rounded-xl border border-gray-200/60 overflow-hidden z-20">
                                <button
                                    onClick={(e) => { e.stopPropagation(); setZoom(z => Math.max(0.5, z - 0.25)); }}
                                    className="p-3 text-gray-600 hover:bg-gray-100 hover:text-brand-primary transition-colors focus:outline-none"
                                    title={t('adminAppeals.zoomOutBtn', 'Зменшити')}
                                >
                                    <ZoomOut className="w-5 h-5" />
                                </button>
                                <button
                                    onClick={(e) => { e.stopPropagation(); setZoom(1); setPosition({ x: 0, y: 0 }); }}
                                    className="px-4 py-2 text-sm font-bold text-gray-700 min-w-[3.5rem] text-center border-x border-gray-100 hover:bg-gray-50 focus:outline-none transition-colors"
                                    title={t('adminAppeals.resetZoomBtn', 'Скинути масштаб')}
                                >
                                    {Math.round(zoom * 100)}%
                                </button>
                                <button
                                    onClick={(e) => { e.stopPropagation(); setZoom(z => Math.min(4, z + 0.25)); }}
                                    className="p-3 text-gray-600 hover:bg-gray-100 hover:text-brand-primary transition-colors focus:outline-none"
                                    title={t('adminAppeals.zoomInBtn', 'Збільшити')}
                                >
                                    <ZoomIn className="w-5 h-5" />
                                </button>
                            </div>
                        </div>

                        {/* Footer / Actions */}
                        <div className="flex items-center justify-end gap-3 p-4 border-t border-gray-100 bg-white flex-shrink-0 z-20">
                            <button
                                onClick={(e) => { e.stopPropagation(); setIsOpen(false); }}
                                className="px-5 py-2.5 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-gray-200 transition-colors"
                            >
                                {t('adminAppeals.closeBtn', 'Закрити')}
                            </button>
                            {photos.length > 1 && (
                                <button
                                    onClick={handleDownloadAll}
                                    className="px-5 py-2.5 text-sm font-medium text-brand-primary bg-brand-light/20 border border-brand-primary/20 rounded-lg hover:bg-brand-primary hover:text-white transition-colors focus:outline-none"
                                >
                                    {t('adminAppeals.downloadAllPrefix', 'Завантажити всі (')}{photos.length}{t('adminAppeals.downloadAllSuffix', ')')}
                                </button>
                            )}
                            <button
                                onClick={(e) => handleDownload(activePhoto.id, activePhoto.originalName, e)}
                                disabled={!activeUrl}
                                className="px-5 py-2.5 text-sm font-medium text-white bg-brand-primary rounded-lg hover:bg-brand-secondary focus:outline-none focus:ring-2 focus:ring-brand-primary/50 transition-colors inline-flex items-center gap-2 shadow-sm disabled:opacity-50"
                            >
                                <Download className="w-4 h-4" />
                                {t('adminAppeals.downloadPhotoBtn', 'Завантажити це фото')}
                            </button>
                        </div>
                    </div>
                </div>,
                document.body
            )}
        </div>
    );
}

export default function AdminAppealsPage() {
    const { t, i18n } = useTranslation();

    const getDateLocale = () => {
        switch (i18n.language) {
            case 'en': return enUS;
            case 'de': return de;
            case 'uk':
            default: return uk;
        }
    };

    const userRole = localStorage.getItem('role');
    const isFakeAdmin = userRole === 'FAKE_ADMIN';

    const [appeals, setAppeals] = useState<AppealResponse[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(1);
    const location = useLocation();
    const queryParams = new URLSearchParams(location.search);
    const targetId = queryParams.get('id');

    // Alert Modal State
    const [isAlertOpen, setIsAlertOpen] = useState(false);
    const [alertMessage, setAlertMessage] = useState('');
    const [alertTitle, setAlertTitle] = useState('');

    const showAlert = (message: string, title = t('common.error', 'Помилка')) => {
        setAlertMessage(message);
        setAlertTitle(title);
        setIsAlertOpen(true);
    };

    const fetchAppeals = async (pageNumber: number) => {
        if (isFakeAdmin) {
            setIsLoading(false);
            return;
        }
        try {
            setIsLoading(true);
            const data = await getAppeals(pageNumber, 20);
            setAppeals(data.content);
            setTotalPages(data.totalPages);
            setPage(data.number);
        } catch (error) {
            showAlert(t('adminAppeals.loadError', 'Помилка завантаження звернень'));
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        fetchAppeals(page);
    }, [page]);

    useEffect(() => {
        if (targetId && appeals.length > 0) {
            const element = document.getElementById(`appeal-${targetId}`);
            if (element) {
                element.scrollIntoView({ behavior: 'smooth', block: 'center' });
                element.classList.add('ring-4', 'ring-brand-primary/50');
                setTimeout(() => element.classList.remove('ring-4', 'ring-brand-primary/50'), 3000);
            }
        }
    }, [appeals, targetId]);

    const handleStatusChange = async (id: string, newStatus: 'NEW' | 'PROCESSED') => {
        try {
            await updateAppealStatus(id, newStatus);
            setAppeals(prev => prev.map(a => a.id === id ? { ...a, status: newStatus } : a));
        } catch (error) {
            showAlert(t('adminAppeals.updateStatusError', 'Помилка оновлення статусу'));
        }
    };

    const handleDelete = async (id: string, e: React.MouseEvent) => {
        e.stopPropagation();
        try {
            await deleteAppeal(id);
            setAppeals(prev => prev.filter(a => a.id !== id));
        } catch (error) {
            showAlert(t('adminAppeals.deleteError', 'Помилка видалення звернення'));
        }
    };

    const getIconForMethod = (method: string) => {
        switch (method) {
            case 'MOBILE': return <Phone className="w-4 h-4 text-emerald-500 flex-shrink-0" />;
            case 'INSTAGRAM': return <Instagram className="w-4 h-4 text-pink-500 flex-shrink-0" />;
            case 'TELEGRAM': return <Send className="w-4 h-4 text-blue-500 flex-shrink-0" />;
            case 'WHATSAPP': return <MessageCircle className="w-4 h-4 text-green-500 flex-shrink-0" />;
            case 'EMAIL': return <Mail className="w-4 h-4 text-gray-400 flex-shrink-0" />;
            default: return <MessageSquare className="w-4 h-4 text-gray-500 flex-shrink-0" />;
        }
    };



    if (isFakeAdmin) {
        return (
            <div className="p-8 max-w-7xl mx-auto h-full flex flex-col items-center justify-center text-center">
                <MessageSquare className="w-16 h-16 text-brand-primary mb-4 opacity-75" />
                <h1 className="text-2xl font-bold text-gray-900 mb-2">Звернення</h1>
                <p className="text-gray-500 text-lg max-w-md">
                    Більш детальна інформація з цієї вкладки доступна лише адміністраторам платформи.
                </p>
            </div>
        );
    }

    if (isLoading && appeals.length === 0) {
        return (
            <div className="flex h-full items-center justify-center">
                <Loader2 className="w-8 h-8 animate-spin text-brand-primary" />
            </div>
        );
    }

    return (
        <div className="p-8 max-w-7xl mx-auto h-full overflow-auto">
            <div className="mb-8 flex justify-between items-end">
                <div>
                    <h1 className="text-3xl font-bold text-gray-900 tracking-tight">{t('adminAppeals.title', 'Звернення')}</h1>
                </div>
            </div>

            <div className="space-y-2">
                {appeals.map(appeal => (
                    <div
                        key={appeal.id}
                        id={`appeal-${appeal.id}`}
                        className={`
                            relative p-5 rounded-lg border transition-all duration-300 group overflow-hidden
                            ${appeal.status === 'NEW' ? 'bg-amber-50 border-amber-200 shadow-sm' : 'bg-white border-gray-100 opacity-70'}
                            hover:shadow-lg
                        `}
                    >
                        <div className="flex gap-5 relative z-10 transition-all duration-300 group-hover:pr-[90px]">
                            {/* Left Icon Box */}
                            <div className="shrink-0 mt-1">
                                <div className={`w-14 h-14 rounded-lg flex items-center justify-center bg-white shadow-sm transition-opacity duration-300 ${appeal.status === 'PROCESSED' ? 'opacity-50' : ''}`}>
                                    <MessageSquare className={`w-8 h-8 ${appeal.status === 'NEW' ? 'text-amber-500' : 'text-gray-400'}`} />
                                </div>
                            </div>

                            {/* Main Content Area */}
                            <div className="flex-1 min-w-0">
                                <div className="flex items-start justify-between gap-4 mb-4">
                                    <div className="space-y-1">
                                        <div className="flex items-center gap-3">
                                            <h4 className={`text-lg font-bold transition-colors duration-300 ${appeal.status === 'PROCESSED' ? 'text-gray-600' : 'text-gray-900'}`}>
                                                {appeal.guestName 
                                                    ? appeal.guestName 
                                                    : (appeal.userFirstName || appeal.userLastName)
                                                        ? `${appeal.userFirstName || ''} ${appeal.userLastName || ''}`.trim()
                                                        : appeal.userEmail}
                                            </h4>
                                            {appeal.guestName && (
                                                <span className="px-2.5 py-0.5 rounded-full bg-brand-light/30 text-brand-primary text-xs font-semibold border border-brand-primary/20">
                                                    Гість (Лендінг)
                                                </span>
                                            )}
                                        </div>
                                        <p className="text-xs text-gray-400 mt-0.5">
                                            {format(new Date(appeal.createdAt), 'dd MMMM yyyy, HH:mm', { locale: getDateLocale() })}
                                        </p>
                                    </div>
                                </div>

                                <div className="flex flex-col md:flex-row gap-6 mb-6">
                                    <div className="flex-1 bg-white/50 rounded-xl p-3 border border-gray-100 h-fit space-y-2">
                                        <div className="flex items-center gap-1.5 text-sm text-gray-700">
                                            <div className="w-5 flex justify-center flex-shrink-0">
                                                {getIconForMethod(appeal.contactMethod)}
                                            </div>
                                            <span className="font-medium truncate">{appeal.contactDetails}</span>
                                            <CopyButton text={appeal.contactDetails} />
                                        </div>
                                        {appeal.userEmail && (
                                            <div className="flex items-center gap-1.5 text-sm text-gray-500">
                                                <div className="w-5 flex justify-center flex-shrink-0">
                                                    <Mail className="w-3.5 h-3.5 flex-shrink-0" />
                                                </div>
                                                <span className="truncate">{appeal.userEmail}</span>
                                                <CopyButton text={appeal.userEmail} />
                                            </div>
                                        )}
                                    </div>

                                    {appeal.photos && appeal.photos.length > 0 && (
                                        <div className="flex-1">
                                            <h4 className="text-xs font-semibold text-gray-500 mb-2 uppercase tracking-wider">
                                                {t('adminAppeals.attachmentsPrefix', 'Прикріплені зображення (')}{appeal.photos.length}{t('adminAppeals.attachmentsSuffix', ')')}
                                            </h4>
                                            <AppealImageGallery photos={appeal.photos} />
                                        </div>
                                    )}
                                </div>

                                <div className="mb-6">
                                    <p className={`text-sm whitespace-pre-wrap leading-relaxed transition-colors duration-300 ${appeal.status === 'PROCESSED' ? 'text-gray-500' : 'text-gray-700'}`}>
                                        {appeal.message}
                                    </p>
                                </div>

                                <div className="flex gap-2 pt-4 border-t border-gray-100 mt-auto">
                                    {appeal.status === 'NEW' ? (
                                        <button
                                            onClick={(e) => { e.stopPropagation(); handleStatusChange(appeal.id, 'PROCESSED'); }}
                                            className="inline-flex items-center gap-2 px-4 py-2 bg-emerald-500 text-white rounded-lg text-sm font-medium hover:bg-emerald-600 transition-all shadow-sm hover:shadow"
                                        >
                                            <CheckCircle className="w-4 h-4" />
                                            {t('adminAppeals.processedBtn', 'Опрацьовано')}
                                        </button>
                                    ) : (
                                        <button
                                            onClick={(e) => { e.stopPropagation(); handleStatusChange(appeal.id, 'NEW'); }}
                                            className="inline-flex items-center gap-2 px-4 py-2 bg-white border border-gray-200 text-gray-600 rounded-lg text-sm font-medium hover:bg-gray-50 transition-all shadow-sm hover:shadow"
                                        >
                                            {t('adminAppeals.revertStatusBtn', 'Повернути статус')}
                                        </button>
                                    )}
                                </div>
                            </div>
                        </div>

                        {/* Hover Delete Action */}
                        <div
                            onClick={(e) => handleDelete(appeal.id, e)}
                            className="absolute right-0 top-0 bottom-0 w-[100px] bg-red-50/90 backdrop-blur-[2px] border-l border-red-100 flex items-center justify-center translate-x-full group-hover:translate-x-0 transition-transform duration-300 z-20 hover:bg-red-500 group/delete cursor-pointer"
                        >
                            <Trash2 className="w-6 h-6 text-red-500 transition-colors group-hover/delete:text-white" />
                        </div>
                    </div>
                ))}

                {appeals.length === 0 && !isLoading && (
                    <div className="col-span-full py-16 text-center text-gray-400">
                        <MessageSquare className="w-12 h-12 mx-auto mb-3 opacity-20" />
                        <p>{t('adminAppeals.noAppeals', 'Звернень поки що немає')}</p>
                    </div>
                )}
            </div>

            {totalPages > 1 && (
                <div className="mt-8 flex justify-center gap-2">
                    {Array.from({ length: totalPages }).map((_, i) => (
                        <button
                            key={i}
                            onClick={() => setPage(i)}
                            className={`w-8 h-8 rounded-lg text-sm font-medium transition-colors ${page === i
                                ? 'bg-brand-primary text-white shadow-md shadow-brand-primary/20'
                                : 'bg-white text-gray-600 hover:bg-gray-50 border border-gray-200'
                                }`}
                        >
                            {i + 1}
                        </button>
                    ))}
                </div>
            )}

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
