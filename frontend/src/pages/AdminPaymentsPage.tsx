import { useEffect, useState, useRef, useCallback } from 'react';
import { getAdminPayments, downloadPaymentReceipt, type PaymentResponse } from '../api/payments';
import { format } from 'date-fns';
import { uk, enUS, de } from 'date-fns/locale';
import { useTranslation } from 'react-i18next';
import { Loader2, Search, CreditCard, AlertCircle, ArrowUp, ArrowDown, ArrowUpDown, X, FileText } from 'lucide-react';
import { useDebounce } from '../hooks/useDebounce';

export default function AdminPaymentsPage() {
    const { t, i18n } = useTranslation();
    const [downloadingReceiptId, setDownloadingReceiptId] = useState<string | null>(null);
    const [isTotalActive, setIsTotalActive] = useState(false);

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

    const [payments, setPayments] = useState<PaymentResponse[]>([]);
    const totalAmount = payments.reduce((sum, payment) => sum + payment.amount, 0);
    const [isLoading, setIsLoading] = useState(true);
    const [page, setPage] = useState(0);
    const [hasMore, setHasMore] = useState(true);
    const [isFetchingNextPage, setIsFetchingNextPage] = useState(false);
    const [searchTerm, setSearchTerm] = useState('');
    const debouncedSearch = useDebounce(searchTerm, 500);
    const [sortConfig, setSortConfig] = useState<{ key: string; direction: 'asc' | 'desc' } | null>(null);
    const [startDate, setStartDate] = useState('');
    const [endDate, setEndDate] = useState('');

    const observer = useRef<IntersectionObserver | null>(null);
    const lastElementRef = useCallback((node: HTMLTableRowElement) => {
        if (isLoading || isFetchingNextPage) return;
        if (observer.current) observer.current.disconnect();
        observer.current = new IntersectionObserver(entries => {
            if (entries[0].isIntersecting && hasMore) {
                setPage(prevPage => prevPage + 1);
            }
        });
        if (node) observer.current.observe(node);
    }, [isLoading, isFetchingNextPage, hasMore]);

    const fetchPayments = async (pageNum: number, search: string, reset: boolean = false) => {
        if (isFakeAdmin) {
            setIsLoading(false);
            return;
        }
        if (pageNum === 0) setIsLoading(true);
        else setIsFetchingNextPage(true);
        
        try {
            const data = await getAdminPayments(pageNum, 15, search, sortConfig?.key, sortConfig?.direction, startDate || undefined, endDate || undefined);
            setPayments(prev => reset ? data.content : [...prev, ...data.content]);
            // data.last is not exposed in PaginatedResponse explicitly in TS interface, but we can infer from totalPages
            setHasMore(pageNum < data.totalPages - 1);
        } catch (error) {
            console.error("Failed to fetch payments", error);
        } finally {
            if (pageNum === 0) setIsLoading(false);
            else setIsFetchingNextPage(false);
        }
    };

    // Reload completely when dependencies change
    useEffect(() => {
        setPage(0);
        setHasMore(true);
        fetchPayments(0, debouncedSearch, true);
    }, [debouncedSearch, sortConfig, startDate, endDate]);

    // Fetch next page when page changes
    useEffect(() => {
        if (page > 0) {
            fetchPayments(page, debouncedSearch, false);
        }
    }, [page]);

    const handleSearchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        setSearchTerm(e.target.value);
    };

    const handleDownloadReceipt = async (paymentId: string) => {
        try {
            setDownloadingReceiptId(paymentId);
            const blob = await downloadPaymentReceipt(paymentId);
            const url = window.URL.createObjectURL(blob);
            window.open(url, '_blank');
            // Clean up the URL object after a short delay
            setTimeout(() => window.URL.revokeObjectURL(url), 1000);
        } catch (error) {
            console.error("Failed to download receipt", error);
            alert(t('adminPayments.receiptLoadError'));
        } finally {
            setDownloadingReceiptId(null);
        }
    };

    const handleResetFilters = () => {
        setSearchTerm('');
        setStartDate('');
        setEndDate('');
        setSortConfig(null);
    };

    const handleSort = (key: string) => {
        let direction: 'asc' | 'desc' = 'asc';
        if (sortConfig && sortConfig.key === key && sortConfig.direction === 'asc') {
            direction = 'desc';
        }
        setSortConfig({ key, direction });
    };

    const getSortIcon = (columnKey: string) => {
        if (sortConfig?.key !== columnKey) return <ArrowUpDown size={14} className="ml-1 text-gray-400" />;
        if (sortConfig.direction === 'asc') return <ArrowUp size={14} className="ml-1 text-brand-primary" />;
        return <ArrowDown size={14} className="ml-1 text-brand-primary" />;
    };

    if (isFakeAdmin) {
        return (
            <div className="p-8 max-w-7xl mx-auto h-full flex flex-col items-center justify-center text-center">
                <AlertCircle className="w-16 h-16 text-brand-primary mb-4 opacity-75" />
                <h1 className="text-2xl font-bold text-gray-900 mb-2">{t('adminPayments.title')}</h1>
                <p className="text-gray-500 text-lg max-w-md">
                    {t('adminPayments.fakeAdminMessage')}
                </p>
            </div>
        );
    }

    return (
        <div className="container mx-auto px-6 py-12 space-y-6 flex flex-col h-full max-h-screen">
            <div className="flex justify-between items-center">
                <h1 className="text-3xl font-bold text-gray-900 tracking-tight">{t('adminPayments.title')}</h1>
            </div>

            {/* Controls Bar */}
            <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between shrink-0 bg-white/40 p-2 rounded-xl border border-gray-200 backdrop-blur-sm shadow-sm gap-4">
                
                {/* Search */}
                <div className="relative flex-1 shrink-0">
                    <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                        <Search className="h-4 w-4 text-gray-400" />
                    </div>
                    <input
                        type="text"
                        placeholder={t('adminPayments.searchPlaceholder')}
                        className="block w-full pl-9 pr-3 py-2 border border-white/50 rounded-lg leading-5 bg-white/60 placeholder-gray-500 focus:outline-none focus:ring-1 focus:ring-brand-primary sm:text-sm text-gray-900 shadow-inner"
                        value={searchTerm}
                        onChange={handleSearchChange}
                    />
                </div>
                
                <div className="hidden sm:block w-px h-6 bg-gray-300 shrink-0"></div>

                <div className="flex items-center gap-2 shrink-0">
                    {/* Date Filters */}
                    <div className="flex items-center gap-2">
                        <span className="text-xs font-medium text-gray-500 uppercase tracking-wider px-1 hidden lg:inline-block">{t('adminPayments.periodLabel')}</span>
                        <input 
                            type="date" 
                            value={startDate}
                            onChange={(e) => setStartDate(e.target.value)}
                            className="block w-full sm:w-auto px-3 py-2 border border-white/50 rounded-lg leading-5 bg-white/60 focus:outline-none focus:ring-1 focus:ring-brand-primary sm:text-sm text-gray-700 shadow-inner"
                        />
                        <span className="text-gray-400">-</span>
                        <input 
                            type="date" 
                            value={endDate}
                            onChange={(e) => setEndDate(e.target.value)}
                            className="block w-full sm:w-auto px-3 py-2 border border-white/50 rounded-lg leading-5 bg-white/60 focus:outline-none focus:ring-1 focus:ring-brand-primary sm:text-sm text-gray-700 shadow-inner"
                        />
                    </div>

                    {/* Reset Filters */}
                    {(searchTerm || startDate || endDate || sortConfig) && (
                        <button 
                            onClick={handleResetFilters}
                            title={t('adminPayments.resetFiltersTitle')}
                            className="flex items-center justify-center p-2 text-red-500 bg-white hover:bg-red-50 border border-red-200 rounded-lg transition-colors shadow-sm shrink-0"
                        >
                            <X className="w-5 h-5" />
                        </button>
                    )}
                </div>
            </div>

            <div className="glass-panel overflow-hidden rounded-lg flex-1 flex flex-col min-h-0">
                <div className="overflow-auto flex-1 custom-scrollbar">
                    <table className="min-w-full h-full divide-y divide-gray-200/50">
                        <thead className="bg-white/30 backdrop-blur-sm sticky top-0 z-10">
                            <tr>
                                <th className="px-6 py-3 text-left text-xs font-bold text-gray-600 uppercase tracking-wider whitespace-nowrap w-1">{t('adminPayments.colIndex')}</th>
                                <th onClick={() => handleSort('createdAt')} className="px-6 py-3 text-left text-xs font-bold text-gray-600 uppercase tracking-wider whitespace-nowrap w-1 cursor-pointer hover:bg-white/40 transition-colors">
                                    <div className="flex items-center gap-1">{t('adminPayments.colDate')} {getSortIcon('createdAt')}</div>
                                </th>
                                <th onClick={() => handleSort('personName')} className="px-6 py-3 text-left text-xs font-bold text-gray-600 uppercase tracking-wider whitespace-nowrap w-1 cursor-pointer hover:bg-white/40 transition-colors">
                                    <div className="flex items-center gap-1">{t('adminPayments.colUser')} {getSortIcon('personName')}</div>
                                </th>
                                <th onClick={() => handleSort('country')} className="px-6 py-3 text-left text-xs font-bold text-gray-600 uppercase tracking-wider whitespace-nowrap w-1 cursor-pointer hover:bg-white/40 transition-colors">
                                    <div className="flex items-center gap-1">{t('adminPayments.colRegion')} {getSortIcon('country')}</div>
                                </th>
                                <th onClick={() => handleSort('courseName')} className="px-6 py-3 text-left text-xs font-bold text-gray-600 uppercase tracking-wider w-full cursor-pointer hover:bg-white/40 transition-colors">
                                    <div className="flex items-center gap-1">{t('adminPayments.colCourse')} {getSortIcon('courseName')}</div>
                                </th>
                                <th onClick={() => handleSort('amount')} className="px-6 py-3 text-left text-xs font-bold text-gray-600 uppercase tracking-wider whitespace-nowrap w-1 cursor-pointer hover:bg-white/40 transition-colors">
                                    <div className="flex items-center gap-1">{t('adminPayments.colAmount')} {getSortIcon('amount')}</div>
                                </th>
                                <th onClick={() => handleSort('paymentSystem')} className="px-6 py-3 text-left text-xs font-bold text-gray-600 uppercase tracking-wider whitespace-nowrap w-1 cursor-pointer hover:bg-white/40 transition-colors">
                                    <div className="flex items-center gap-1">{t('adminPayments.colSystem')} {getSortIcon('paymentSystem')}</div>
                                </th>
                                <th onClick={() => handleSort('status')} className="px-6 py-3 text-left text-xs font-bold text-gray-600 uppercase tracking-wider whitespace-nowrap w-1 cursor-pointer hover:bg-white/40 transition-colors">
                                    <div className="flex items-center gap-1">{t('adminPayments.colStatus')} {getSortIcon('status')}</div>
                                </th>
                                <th className="px-6 py-3 text-center text-xs font-bold text-gray-600 uppercase tracking-wider whitespace-nowrap w-1">{t('adminPayments.colReceipt')}</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-200/50">
                            {isLoading && payments.length === 0 ? (
                                <tr>
                                    <td colSpan={9} className="px-6 py-12 text-center">
                                        <Loader2 className="w-8 h-8 animate-spin text-brand-primary mx-auto" />
                                    </td>
                                </tr>
                            ) : payments.length === 0 ? (
                                <tr>
                                    <td colSpan={9} className="px-6 py-12 text-center text-gray-400">
                                        <CreditCard className="w-12 h-12 mx-auto mb-3 opacity-20" />
                                        <p>{t('adminPayments.noPaymentsFound')}</p>
                                    </td>
                                </tr>
                            ) : (
                                payments.map((payment, index) => {
                                    const isLast = index === payments.length - 1;
                                    return (
                                        <tr key={payment.id} ref={isLast ? lastElementRef : null} className="hover:bg-white/40 transition-colors">
                                        <td className="px-6 py-3 whitespace-nowrap text-sm font-medium text-gray-900 w-1">
                                            {index + 1}
                                        </td>
                                        <td className="px-6 py-3 whitespace-nowrap text-sm font-medium text-gray-900 border-r border-gray-200/50 w-1">
                                            {format(new Date(payment.createdAt), 'dd.MM.yyyy HH:mm', { locale: getDateLocale() })}
                                        </td>
                                        <td className="px-6 py-3 whitespace-nowrap w-1">
                                            <div className="text-sm font-medium text-gray-900 border-l pl-2 border-brand-primary/20">{payment.personName}</div>
                                            <div className="text-sm text-gray-500 pl-2 mt-0.5">{payment.personEmail}</div>
                                        </td>
                                        <td className="px-6 py-3 whitespace-nowrap w-1">
                                            {payment.country ? (
                                                <span className="inline-flex items-center px-2 py-1 rounded-md bg-gray-50 border border-gray-100 text-xs font-medium text-gray-600">
                                                    {payment.country}
                                                </span>
                                            ) : (
                                                <span className="text-gray-400 text-xs">-</span>
                                            )}
                                        </td>
                                        <td className="px-6 py-3 w-full">
                                            <div className="inline-flex flex-col px-3 py-1.5 rounded-md bg-white/60 border border-gray-200 shadow-sm">
                                                <span className="text-xs font-bold text-gray-800 leading-tight block">
                                                    {payment.courseName}
                                                </span>
                                            </div>
                                        </td>
                                        <td className="px-6 py-3 whitespace-nowrap text-sm font-bold text-gray-900 w-1">
                                            {payment.amount.toFixed(2)} €
                                        </td>
                                        <td className="px-6 py-3 whitespace-nowrap w-1">
                                            <span className={`px-1.5 py-0.5 rounded text-[10px] font-bold border ${
                                                payment.paymentSystem.includes('PAYPAL')
                                                    ? 'bg-purple-100 text-purple-800 border-purple-200'
                                                    : 'bg-blue-100 text-blue-800 border-blue-200'
                                            }`}>
                                                {payment.paymentSystem}
                                            </span>
                                        </td>
                                        <td className="px-6 py-3 whitespace-nowrap w-1">
                                            <span className={`px-1.5 py-0.5 rounded text-[10px] font-bold border ${
                                                payment.status === 'SUCCESS' ? 'bg-green-100 text-green-800 border-green-200' :
                                                payment.status === 'FAILED' ? 'bg-red-100 text-red-800 border-red-200' :
                                                'bg-yellow-100 text-yellow-800 border-yellow-200'
                                            }`}>
                                                {payment.status}
                                            </span>
                                        </td>
                                        <td className="px-6 py-3 whitespace-nowrap text-center w-1">
                                            {payment.status === 'SUCCESS' && (
                                                <button
                                                    onClick={() => handleDownloadReceipt(payment.id)}
                                                    disabled={downloadingReceiptId === payment.id}
                                                    className="p-1.5 text-gray-400 hover:text-brand-primary hover:bg-brand-primary/10 rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                                                    title={t('adminPayments.receiptTitle')}
                                                >
                                                    {downloadingReceiptId === payment.id ? (
                                                        <Loader2 className="w-5 h-5 animate-spin" />
                                                    ) : (
                                                        <FileText className="w-5 h-5" />
                                                    )}
                                                </button>
                                            )}
                                        </td>
                                    </tr>
                                    );
                                })
                            )}
                            {/* Filler row to push tfoot to the bottom */}
                            <tr className="h-full border-0">
                                <td colSpan={9} className="p-0 border-0"></td>
                            </tr>
                        </tbody>
                        <tfoot className="sticky bottom-0 z-10 bg-white/80 backdrop-blur-md border-t border-black">
                            <tr className={isTotalActive ? 'text-black' : 'text-gray-400'}>
                                <td className="px-6 py-3 whitespace-nowrap text-sm font-medium w-1">
                                    <input 
                                        type="checkbox" 
                                        className="w-4 h-4 text-gray-900 border-gray-300 rounded focus:ring-gray-900 cursor-pointer"
                                        checked={isTotalActive}
                                        onChange={(e) => setIsTotalActive(e.target.checked)}
                                    />
                                </td>
                                <td className="px-6 py-3 whitespace-nowrap text-left font-bold text-sm uppercase tracking-wider">
                                    {t('adminPayments.totalLabel')}
                                </td>
                                <td colSpan={3}></td>
                                <td className="px-6 py-3 whitespace-nowrap text-sm font-bold w-1">
                                    {isTotalActive ? `${totalAmount.toFixed(2)} €` : '-'}
                                </td>
                                <td colSpan={3}></td>
                            </tr>
                        </tfoot>
                    </table>
                </div>

                {isFetchingNextPage && (
                    <div className="p-4 text-center text-gray-500 bg-white/50 backdrop-blur-sm shrink-0">
                        <Loader2 className="w-6 h-6 animate-spin text-brand-primary mx-auto" />
                    </div>
                )}
            </div>
        </div>
    );
}
