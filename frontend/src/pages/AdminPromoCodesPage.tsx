import React, { useEffect, useState, useRef, useCallback } from 'react';
import { promoCodesApi } from '../api/promoCodes';
import type { PromoCodeResponseDto } from '../api/promoCodes';
import * as Icons from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { CreatePromoCodeModal } from '../components/CreatePromoCodeModal';
import { ConfirmModal } from '../components/ConfirmModal';

export const AdminPromoCodesPage: React.FC = () => {
    const { t } = useTranslation();
    const [promoCodes, setPromoCodes] = useState<PromoCodeResponseDto[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
    const [promoCodeToEdit, setPromoCodeToEdit] = useState<PromoCodeResponseDto | null>(null);
    const [promoCodeToDelete, setPromoCodeToDelete] = useState<string | null>(null);
    const [isDeleting, setIsDeleting] = useState(false);
    
    // Search & Sort State
    const [searchTerm, setSearchTerm] = useState('');
    const [debouncedSearch, setDebouncedSearch] = useState('');
    const [sortConfig, setSortConfig] = useState<{ key: string; direction: 'asc' | 'desc' } | null>(null);
    const [statusSort, setStatusSort] = useState<'top' | 'bottom' | null>(null);

    // Pagination State
    const [page, setPage] = useState(0);
    const [hasMore, setHasMore] = useState(true);
    const [isFetchingNextPage, setIsFetchingNextPage] = useState(false);

    const observer = useRef<IntersectionObserver | null>(null);
    const lastElementRef = useCallback((node: HTMLTableRowElement) => {
        if (loading || isFetchingNextPage) return;
        if (observer.current) observer.current.disconnect();
        observer.current = new IntersectionObserver(entries => {
            if (entries[0].isIntersecting && hasMore) {
                setPage(prevPage => prevPage + 1);
            }
        });
        if (node) observer.current.observe(node);
    }, [loading, isFetchingNextPage, hasMore]);

    const fetchPromoCodes = async (pageNum: number, reset: boolean = false) => {
        if (pageNum === 0) setLoading(true);
        else setIsFetchingNextPage(true);

        try {
            const data = await promoCodesApi.getPaginated(
                pageNum,
                20,
                debouncedSearch,
                sortConfig?.key,
                sortConfig?.direction,
                statusSort || undefined
            );

            setPromoCodes(prev => reset ? data.content : [...prev, ...data.content]);
            setHasMore(!data.last);
            setError(null);
        } catch (err) {
            setError(t('promoCodes.loadError', 'Не вдалося завантажити промокоди.'));
        } finally {
            if (pageNum === 0) setLoading(false);
            else setIsFetchingNextPage(false);
        }
    };

    const confirmDelete = async () => {
        if (!promoCodeToDelete) return;
        setIsDeleting(true);
        try {
            await promoCodesApi.delete(promoCodeToDelete);
            setPromoCodes(promoCodes.filter(promo => promo.id !== promoCodeToDelete));
            setPromoCodeToDelete(null);
        } catch (err) {
            setError(t('promoCodes.deleteError', 'Не вдалося видалити промокод.'));
        } finally {
            setIsDeleting(false);
        }
    };

    useEffect(() => {
        const timer = setTimeout(() => {
            setDebouncedSearch(searchTerm);
        }, 300);
        return () => clearTimeout(timer);
    }, [searchTerm]);

    useEffect(() => {
        setPage(0);
        setHasMore(true);
        fetchPromoCodes(0, true);
    }, [debouncedSearch, sortConfig, statusSort]);

    useEffect(() => {
        if (page > 0) {
            fetchPromoCodes(page, false);
        }
    }, [page]);

    const handleSort = (key: string) => {
        let direction: 'asc' | 'desc' = 'asc';
        if (sortConfig && sortConfig.key === key && sortConfig.direction === 'asc') {
            direction = 'desc';
        }
        setSortConfig({ key, direction });
    };

    const toggleStatusSort = (e: React.MouseEvent) => {
        e.stopPropagation();
        if (statusSort === null) setStatusSort('top');
        else if (statusSort === 'top') setStatusSort('bottom');
        else setStatusSort(null);
    };

    const getSortIcon = (columnKey: string) => {
        if (sortConfig?.key !== columnKey) return <Icons.ArrowUpDown size={14} className="ml-1 text-gray-400" />;
        if (sortConfig.direction === 'asc') return <Icons.ArrowUp size={14} className="ml-1 text-blue-600" />;
        return <Icons.ArrowDown size={14} className="ml-1 text-blue-600" />;
    };

    if (loading && promoCodes.length === 0) {
        return <div className="p-8 text-center text-gray-500">{t('dashboard.loading', 'Завантаження...')}</div>;
    }

    return (
        <div className="container mx-auto px-6 py-12 space-y-6">
            <div className="flex justify-between items-center">
                <h1 className="text-3xl font-bold text-gray-900">{t('promoCodes.adminTitle', 'Управління промокодами')}</h1>
                <button
                    onClick={() => {
                        setPromoCodeToEdit(null);
                        setIsCreateModalOpen(true);
                    }}
                    className="flex items-center space-x-2 px-4 py-2 text-gray-900 font-medium hover:bg-gray-100 rounded-lg transition-colors"
                >
                    <Icons.Plus size={20} />
                    <span>{t('promoCodes.createBtn', 'Створити промокод')}</span>
                </button>
            </div>

            {error && (
                <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded">
                    {error}
                </div>
            )}

            <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                    <Icons.Search className="h-5 w-5 text-gray-400" />
                </div>
                <input
                    type="text"
                    placeholder={t('promoCodes.searchPlaceholder', "Пошук промокодів...")}
                    className="block w-full pl-10 pr-3 py-2 border border-gray-300 rounded-lg leading-5 bg-white placeholder-gray-500 focus:outline-none focus:placeholder-gray-400 focus:border-brand-primary focus:ring-2 focus:ring-brand-light sm:text-sm text-gray-900 transition-all"
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                />
            </div>

            <div className="glass-panel overflow-hidden rounded-lg">
                <table className="min-w-full divide-y divide-gray-200/50">
                    <thead className="bg-white/30 backdrop-blur-sm">
                        <tr>
                            <th scope="col" className="px-6 py-3 text-left text-xs font-bold text-gray-600 uppercase tracking-wider whitespace-nowrap w-1">
                                #
                            </th>
                            <th
                                scope="col"
                                className="px-6 py-3 text-left text-xs font-bold text-gray-600 uppercase tracking-wider cursor-pointer hover:bg-white/40 transition-colors whitespace-nowrap w-1"
                                onClick={() => handleSort('code')}
                            >
                                <div className="flex items-center gap-2">
                                    <div className="flex items-center gap-1">
                                        {t('promoCodes.codeHeader', "Промокод")}
                                        {getSortIcon('code')}
                                    </div>
                                    <div
                                        className={`
                                            flex items-center gap-1 px-1.5 py-0.5 rounded text-[10px] font-bold border transition-colors
                                            ${statusSort
                                                ? 'bg-blue-100 text-blue-800 border-blue-200 hover:bg-blue-200'
                                                : 'bg-gray-100 text-gray-500 border-gray-200 hover:bg-gray-200'
                                            }
                                        `}
                                        onClick={toggleStatusSort}
                                        title={t('promoCodes.sortStatus', "Сортувати активні")}
                                    >
                                        <span>{t('promoCodes.status')}</span>
                                        <div className="flex flex-col -space-y-0.5">
                                            <Icons.ChevronUp size={8} className={statusSort === 'top' ? 'text-current' : 'text-gray-400/50'} />
                                            <Icons.ChevronDown size={8} className={statusSort === 'bottom' ? 'text-current' : 'text-gray-400/50'} />
                                        </div>
                                        {statusSort && (
                                            <div
                                                className="pl-1 ml-1 border-l border-blue-200/60 hover:text-blue-950 flex items-center"
                                                onClick={(e) => { e.stopPropagation(); setStatusSort(null); }}
                                                title={t('users.clearSort', "Скинути сортування")}
                                            >
                                                <Icons.X size={10} />
                                            </div>
                                        )}
                                    </div>
                                </div>
                            </th>
                            <th
                                scope="col"
                                className="px-6 py-3 text-left text-xs font-bold text-gray-600 uppercase tracking-wider cursor-pointer hover:bg-white/40 transition-colors whitespace-nowrap w-1"
                                onClick={() => handleSort('person')}
                            >
                                <div className="flex items-center gap-1">
                                    {t('promoCodes.userHeader', "Користувач")}
                                    {getSortIcon('person')}
                                </div>
                            </th>
                            <th scope="col" className="px-6 py-3 text-left text-xs font-bold text-gray-600 uppercase tracking-wider whitespace-nowrap w-full">
                                {t('promoCodes.functionHeader', "Функціонал (Знижки)")}
                            </th>
                            <th
                                scope="col"
                                className="px-6 py-3 text-left text-xs font-bold text-gray-600 uppercase tracking-wider cursor-pointer hover:bg-white/40 transition-colors whitespace-nowrap w-1"
                                onClick={() => handleSort('createdAt')}
                            >
                                <div className="flex items-center gap-1">
                                    {t('promoCodes.dateHeader', "Дата / Валідність")}
                                    {getSortIcon('createdAt')}
                                </div>
                            </th>
                            <th scope="col" className="px-6 py-3 text-right text-xs font-bold text-gray-600 uppercase tracking-wider whitespace-nowrap w-1">
                                {t('promoCodes.actionsHeader', 'ДІЇ')}
                            </th>
                        </tr>
                    </thead>
                    <tbody className="">
                        {promoCodes.flatMap((promo, promoIndex) => {
                            const isLastPromo = promoIndex === promoCodes.length - 1;
                            
                            if (promo.scope === 'GLOBAL' || !promo.targetPersons || promo.targetPersons.length === 0) {
                                return [(
                                    <tr key={`${promo.id}-global`} ref={isLastPromo ? lastElementRef : null} className="hover:bg-white/40 transition-colors border-t-2 border-brand-primary/20">
                                        <td className="px-6 py-4 whitespace-nowrap w-1 text-sm text-gray-500 align-top">
                                            {promoIndex + 1}
                                        </td>
                                        <td className="px-6 py-4 whitespace-nowrap w-1 align-top">
                                            <div className="flex items-center gap-2">
                                                <span className="text-sm font-mono font-bold text-gray-900">{promo.code}</span>
                                                <span className={`px-1.5 py-0.5 rounded text-[10px] font-bold ${promo.status === 'ACTIVE' ? 'bg-green-100 text-green-800 border border-green-200' : 'bg-red-100 text-red-800 border border-red-200'}`}>
                                                    {promo.status === 'ACTIVE' ? t('common.active') : t('common.inactive')}
                                                </span>
                                            </div>
                                        </td>
                                        <td className="px-6 py-4 whitespace-nowrap w-1 align-top">
                                            {promo.scope === 'GLOBAL' ? (
                                                <span className="text-sm font-medium text-gray-500 italic bg-gray-100/50 px-2 py-1 rounded inline-flex items-center gap-1">
                                                    <Icons.Globe size={14} /> {t('promoCodes.globalUser', 'Усі користувачі')}
                                                </span>
                                            ) : (
                                                <span className="text-sm text-gray-500 italic">{t('promoCodes.noUsers', 'Немає користувачів')}</span>
                                            )}
                                        </td>
                                        <td className="px-6 py-4 align-top">
                                            <div className="flex flex-wrap gap-2">
                                                {promo.discounts.map((d, i) => (
                                                    <div key={i} className="inline-flex flex-col px-3 py-1.5 rounded-md bg-white/60 border border-gray-200 shadow-sm w-max">
                                                        <span className="text-xs font-bold text-gray-800 leading-tight block">
                                                            {d.courseId ? d.courseName : t('promoCodes.allCoursesFallback', 'На всі курси')}
                                                        </span>
                                                        <span className="text-[10px] text-brand-primary font-bold leading-tight flex items-center gap-1 mt-0.5">
                                                            <span>
                                                                {d.discountType === 'PERCENTAGE' && `-${d.discountValue}%`}
                                                                {d.discountType === 'FIXED_AMOUNT' && `-${d.discountValue}€`}
                                                                {d.discountType === 'FIXED_PRICE' && `${d.discountValue}€`}
                                                            </span>
                                                            {d.originalCoursePrice != null && d.discountType !== 'FIXED_PRICE' && (
                                                                <>
                                                                    <span className="text-gray-300 mx-0.5">|</span>
                                                                    <span className="text-gray-400 line-through font-normal">
                                                                        {d.originalCoursePrice % 1 === 0 ? d.originalCoursePrice.toFixed(0) : d.originalCoursePrice.toFixed(2)}€
                                                                    </span>
                                                                    <span className="text-green-600">
                                                                        {(
                                                                            d.discountType === 'PERCENTAGE'
                                                                                ? Math.max(0, d.originalCoursePrice * (1 - d.discountValue / 100))
                                                                                : Math.max(0, d.originalCoursePrice - d.discountValue)
                                                                        ) % 1 === 0 ? 
                                                                            (d.discountType === 'PERCENTAGE' ? Math.max(0, d.originalCoursePrice * (1 - d.discountValue / 100)) : Math.max(0, d.originalCoursePrice - d.discountValue)).toFixed(0) : 
                                                                            (d.discountType === 'PERCENTAGE' ? Math.max(0, d.originalCoursePrice * (1 - d.discountValue / 100)) : Math.max(0, d.originalCoursePrice - d.discountValue)).toFixed(2)
                                                                        }€
                                                                    </span>
                                                                </>
                                                            )}
                                                            {d.originalCoursePrice != null && d.discountType === 'FIXED_PRICE' && (
                                                                <>
                                                                    <span className="text-gray-300 mx-0.5">|</span>
                                                                    <span className="text-gray-400 line-through font-normal">
                                                                        {d.originalCoursePrice % 1 === 0 ? d.originalCoursePrice.toFixed(0) : d.originalCoursePrice.toFixed(2)}€
                                                                    </span>
                                                                    <span className="text-green-600">
                                                                        {d.discountValue % 1 === 0 ? d.discountValue.toFixed(0) : d.discountValue.toFixed(2)}€
                                                                    </span>
                                                                </>
                                                            )}
                                                            {d.originalCoursePrice == null && d.discountType === 'FIXED_PRICE' && (
                                                                <>
                                                                    <span className="text-gray-300 mx-0.5">|</span>
                                                                    <span className="text-green-600">
                                                                        {d.discountValue % 1 === 0 ? d.discountValue.toFixed(0) : d.discountValue.toFixed(2)}€
                                                                    </span>
                                                                </>
                                                            )}
                                                        </span>
                                                    </div>
                                                ))}
                                            </div>
                                        </td>
                                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 w-1 align-top">
                                            <div className="flex flex-col text-xs space-y-1">
                                                <span className={promo.pendingActivation ? "text-blue-600 font-medium" : ""}>
                                                    {t('promoCodes.validFromShort', 'Від:')} {promo.validFromDisplay || '—'}
                                                </span>
                                                <span>{t('promoCodes.validUntilShort', 'До:')} {promo.validUntil ? promo.validUntilDisplay : t('promoCodes.indefinite')}</span>
                                            </div>
                                        </td>
                                        <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium space-x-3 w-1 align-top">
                                            <button
                                                onClick={() => {
                                                    setPromoCodeToEdit(promo);
                                                    setIsCreateModalOpen(true);
                                                }}
                                                className="text-blue-600 hover:text-blue-900"
                                                title={t('promoCodes.editBtn', "Редагувати")}
                                            >
                                                <Icons.Edit2 size={18} />
                                            </button>
                                            <button
                                                onClick={() => setPromoCodeToDelete(promo.id)}
                                                className="text-red-600 hover:text-red-900"
                                                title={t('promoCodes.deleteBtn', "Видалити")}
                                            >
                                                <Icons.Trash2 size={18} />
                                            </button>
                                        </td>
                                    </tr>
                                )];
                            }

                            return promo.targetPersons.map((person, personIndex) => {
                                const isFirstPerson = personIndex === 0;
                                const isLastPerson = personIndex === promo.targetPersons!.length - 1;
                                const isLastPersonAndPromo = isLastPromo && isLastPerson;
                                
                                const padCls = `${isFirstPerson ? 'pt-4' : 'pt-1'} ${isLastPerson ? 'pb-4' : 'pb-1'}`;
                                
                                return (
                                    <tr key={`${promo.id}-${person.id}`} ref={isLastPersonAndPromo ? lastElementRef : null} className={`hover:bg-white/40 transition-colors ${isFirstPerson ? 'border-t-2 border-brand-primary/20' : 'border-none'}`}>
                                        <td className={`px-6 ${padCls} whitespace-nowrap w-1 text-sm text-gray-500 align-top`}>
                                            {isFirstPerson ? promoIndex + 1 : ''}
                                        </td>
                                        <td className={`px-6 ${padCls} whitespace-nowrap w-1 align-top`}>
                                            {isFirstPerson ? (
                                                <div className="flex items-center gap-2">
                                                    <span className="text-sm font-mono font-bold text-gray-900">{promo.code}</span>
                                                    <span className={`px-1.5 py-0.5 rounded text-[10px] font-bold ${promo.status === 'ACTIVE' ? 'bg-green-100 text-green-800 border border-green-200' : 'bg-red-100 text-red-800 border border-red-200'}`}>
                                                        {promo.status === 'ACTIVE' ? t('common.active') : t('common.inactive')}
                                                    </span>
                                                </div>
                                            ) : null}
                                        </td>
                                        <td className={`px-6 ${padCls} whitespace-nowrap w-1 align-top`}>
                                            <div className="bg-gray-50 p-2 rounded-lg border border-gray-100">
                                                <div className="text-sm font-medium text-gray-900">{person.name || t('promoCodes.anonymous', 'Анонім')}</div>
                                                <div className="text-sm text-gray-500">{person.email}</div>
                                            </div>
                                        </td>
                                        <td className={`px-6 ${padCls} align-top`}>
                                            <div className="flex flex-wrap gap-2">
                                                {promo.discounts.map((d, i) => {
                                                    const isUsed = d.courseId ? person.usedCourseIds?.includes(d.courseId) : (person.usedCourseIds && person.usedCourseIds.length > 0);
                                                    return (
                                                        <div key={i} className={`inline-flex flex-col px-3 py-1.5 rounded-md shadow-sm w-max border ${isUsed ? 'bg-red-50 border-red-200' : 'bg-white/60 border-gray-200'}`}>
                                                            <span className={`text-xs font-bold leading-tight block ${isUsed ? 'text-red-800' : 'text-gray-800'}`}>
                                                                {d.courseId ? d.courseName : t('promoCodes.allCoursesFallback', 'На всі курси')}
                                                            </span>
                                                            <span className={`text-[10px] font-bold leading-tight flex items-center gap-1 mt-0.5 ${isUsed ? 'text-red-600' : 'text-brand-primary'}`}>
                                                                <span>
                                                                    {d.discountType === 'PERCENTAGE' && `-${d.discountValue}%`}
                                                                    {d.discountType === 'FIXED_AMOUNT' && `-${d.discountValue}€`}
                                                                    {d.discountType === 'FIXED_PRICE' && `${d.discountValue}€`}
                                                                </span>
                                                                {d.originalCoursePrice != null && d.discountType !== 'FIXED_PRICE' && (
                                                                    <>
                                                                        <span className={`${isUsed ? 'text-red-300' : 'text-gray-300'} mx-0.5`}>|</span>
                                                                        <span className={`${isUsed ? 'text-red-400' : 'text-gray-400'} line-through font-normal`}>
                                                                            {d.originalCoursePrice % 1 === 0 ? d.originalCoursePrice.toFixed(0) : d.originalCoursePrice.toFixed(2)}€
                                                                        </span>
                                                                        <span className={isUsed ? 'text-red-700' : 'text-green-600'}>
                                                                            {(
                                                                                d.discountType === 'PERCENTAGE'
                                                                                    ? Math.max(0, d.originalCoursePrice * (1 - d.discountValue / 100))
                                                                                    : Math.max(0, d.originalCoursePrice - d.discountValue)
                                                                            ) % 1 === 0 ? 
                                                                                (d.discountType === 'PERCENTAGE' ? Math.max(0, d.originalCoursePrice * (1 - d.discountValue / 100)) : Math.max(0, d.originalCoursePrice - d.discountValue)).toFixed(0) : 
                                                                                (d.discountType === 'PERCENTAGE' ? Math.max(0, d.originalCoursePrice * (1 - d.discountValue / 100)) : Math.max(0, d.originalCoursePrice - d.discountValue)).toFixed(2)
                                                                            }€
                                                                        </span>
                                                                    </>
                                                                )}
                                                                {d.originalCoursePrice != null && d.discountType === 'FIXED_PRICE' && (
                                                                    <>
                                                                        <span className={`${isUsed ? 'text-red-300' : 'text-gray-300'} mx-0.5`}>|</span>
                                                                        <span className={`${isUsed ? 'text-red-400' : 'text-gray-400'} line-through font-normal`}>
                                                                            {d.originalCoursePrice % 1 === 0 ? d.originalCoursePrice.toFixed(0) : d.originalCoursePrice.toFixed(2)}€
                                                                        </span>
                                                                        <span className={isUsed ? 'text-red-700' : 'text-green-600'}>
                                                                            {d.discountValue % 1 === 0 ? d.discountValue.toFixed(0) : d.discountValue.toFixed(2)}€
                                                                        </span>
                                                                    </>
                                                                )}
                                                                {d.originalCoursePrice == null && d.discountType === 'FIXED_PRICE' && (
                                                                    <>
                                                                        <span className={`${isUsed ? 'text-red-300' : 'text-gray-300'} mx-0.5`}>|</span>
                                                                        <span className={isUsed ? 'text-red-700' : 'text-green-600'}>
                                                                            {d.discountValue % 1 === 0 ? d.discountValue.toFixed(0) : d.discountValue.toFixed(2)}€
                                                                        </span>
                                                                    </>
                                                                )}
                                                            </span>
                                                        </div>
                                                    );
                                                })}
                                            </div>
                                        </td>
                                        <td className={`px-6 ${padCls} whitespace-nowrap text-sm text-gray-500 w-1 align-top`}>
                                            {isFirstPerson ? (
                                                <div className="flex flex-col text-xs space-y-1">
                                                    <span className={promo.pendingActivation ? "text-blue-600 font-medium" : ""}>
                                                        {t('promoCodes.validFromShort', 'Від:')} {promo.validFromDisplay || '—'}
                                                    </span>
                                                    <span>{t('promoCodes.validUntilShort', 'До:')} {promo.validUntil ? promo.validUntilDisplay : t('promoCodes.indefinite')}</span>
                                                </div>
                                            ) : null}
                                        </td>
                                        <td className={`px-6 ${padCls} whitespace-nowrap text-right text-sm font-medium space-x-3 w-1 align-top`}>
                                            {isFirstPerson ? (
                                                <div className="flex justify-end gap-3 h-full pt-1">
                                                    <button
                                                        onClick={() => {
                                                            setPromoCodeToEdit(promo);
                                                            setIsCreateModalOpen(true);
                                                        }}
                                                        className="text-blue-600 hover:text-blue-900"
                                                        title={t('promoCodes.editBtn', "Редагувати")}
                                                    >
                                                        <Icons.Edit2 size={18} />
                                                    </button>
                                                    <button
                                                        onClick={() => setPromoCodeToDelete(promo.id)}
                                                        className="text-red-600 hover:text-red-900"
                                                        title={t('promoCodes.deleteBtn', "Видалити")}
                                                    >
                                                        <Icons.Trash2 size={18} />
                                                    </button>
                                                </div>
                                            ) : null}
                                        </td>
                                    </tr>
                                );
                            });
                        })}
                    </tbody>
                </table>
                {isFetchingNextPage && (
                    <div className="p-4 text-center text-gray-500">
                        {t('dashboard.loadingMore', 'Завантаження...')}
                    </div>
                )}
            </div>

            {isCreateModalOpen && (
                <CreatePromoCodeModal
                    isOpen={isCreateModalOpen}
                    promoCodeToEdit={promoCodeToEdit}
                    onClose={() => {
                        setIsCreateModalOpen(false);
                        setPromoCodeToEdit(null);
                    }}
                    onSuccess={() => fetchPromoCodes(0, true)}
                />
            )}

            {promoCodeToDelete && (
                <ConfirmModal
                    isOpen={!!promoCodeToDelete}
                    onClose={() => setPromoCodeToDelete(null)}
                    onConfirm={confirmDelete}
                    title={t('promoCodes.deleteConfirmTitle', 'Видалити промокод')}
                    message={t('promoCodes.deleteConfirmMessage', 'Ви впевнені, що хочете видалити цей промокод? Цю дію неможливо скасувати.')}
                    isLoading={isDeleting}
                />
            )}
        </div>
    );
};
