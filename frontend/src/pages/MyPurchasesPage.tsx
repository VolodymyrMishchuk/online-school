import { useEffect, useState, useRef, useCallback } from 'react';
import { getMyPayments, downloadPaymentReceipt, type PaymentResponse } from '../api/payments';
import { format } from 'date-fns';
import { uk, enUS, de } from 'date-fns/locale';
import { useTranslation } from 'react-i18next';
import { Loader2, CreditCard, FileText } from 'lucide-react';
import { Link } from 'react-router-dom';

export default function MyPurchasesPage() {
    const { t, i18n } = useTranslation();
    const [downloadingReceiptId, setDownloadingReceiptId] = useState<string | null>(null);

    const getDateLocale = () => {
        switch (i18n.language) {
            case 'en': return enUS;
            case 'de': return de;
            case 'uk':
            default: return uk;
        }
    };

    const [payments, setPayments] = useState<PaymentResponse[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [page, setPage] = useState(0);
    const [hasMore, setHasMore] = useState(true);
    const [isFetchingNextPage, setIsFetchingNextPage] = useState(false);

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

    const fetchPayments = async (pageNum: number, reset: boolean = false) => {
        if (pageNum === 0) setIsLoading(true);
        else setIsFetchingNextPage(true);

        try {
            const data = await getMyPayments(pageNum, 15);
            setPayments(prev => reset ? data.content : [...prev, ...data.content]);
            setHasMore(pageNum < data.totalPages - 1);
        } catch (error) {
            console.error('Failed to fetch payments', error);
        } finally {
            if (pageNum === 0) setIsLoading(false);
            else setIsFetchingNextPage(false);
        }
    };

    useEffect(() => {
        setPage(0);
        setHasMore(true);
        fetchPayments(0, true);
    }, []);

    useEffect(() => {
        if (page > 0) {
            fetchPayments(page, false);
        }
    }, [page]);

    const handleDownloadReceipt = async (paymentId: string) => {
        try {
            setDownloadingReceiptId(paymentId);
            const blob = await downloadPaymentReceipt(paymentId);
            const url = window.URL.createObjectURL(blob);
            window.open(url, '_blank');
            setTimeout(() => window.URL.revokeObjectURL(url), 1000);
        } catch (error) {
            console.error('Failed to download receipt', error);
            alert(t('myPurchases.receiptLoadError'));
        } finally {
            setDownloadingReceiptId(null);
        }
    };

    return (
        <div className="container mx-auto px-6 py-12 space-y-6 flex flex-col h-full max-h-screen">
            <div className="flex justify-between items-center">
                <h1 className="text-3xl font-bold text-gray-900 tracking-tight">{t('myPurchases.title')}</h1>
            </div>

            <div className="glass-panel overflow-hidden rounded-lg flex-1 flex flex-col min-h-0">
                <div className="overflow-auto flex-1 custom-scrollbar">
                    <table className="min-w-full h-full divide-y divide-gray-200/50">
                        <thead className="bg-white/30 backdrop-blur-sm sticky top-0 z-10">
                            <tr>
                                <th className="px-6 py-3 text-left text-xs font-bold text-gray-600 uppercase tracking-wider whitespace-nowrap w-1">{t('myPurchases.colIndex')}</th>
                                <th className="px-6 py-3 text-left text-xs font-bold text-gray-600 uppercase tracking-wider whitespace-nowrap w-1">
                                    {t('myPurchases.colDate')}
                                </th>
                                <th className="px-6 py-3 text-left text-xs font-bold text-gray-600 uppercase tracking-wider w-full">
                                    {t('myPurchases.colCourse')}
                                </th>
                                <th className="px-6 py-3 text-left text-xs font-bold text-gray-600 uppercase tracking-wider whitespace-nowrap w-1">
                                    {t('myPurchases.colDuration')}
                                </th>
                                <th className="px-6 py-3 text-left text-xs font-bold text-gray-600 uppercase tracking-wider whitespace-nowrap w-1">
                                    {t('myPurchases.colExpires')}
                                </th>
                                <th className="px-6 py-3 text-left text-xs font-bold text-gray-600 uppercase tracking-wider whitespace-nowrap w-1">
                                    {t('myPurchases.colAmount')}
                                </th>
                                <th className="px-6 py-3 text-left text-xs font-bold text-gray-600 uppercase tracking-wider whitespace-nowrap w-1">
                                    {t('myPurchases.colSystem')}
                                </th>
                                <th className="px-6 py-3 text-left text-xs font-bold text-gray-600 uppercase tracking-wider whitespace-nowrap w-1">
                                    {t('myPurchases.colStatus')}
                                </th>
                                <th className="px-6 py-3 text-center text-xs font-bold text-gray-600 uppercase tracking-wider whitespace-nowrap w-1">{t('myPurchases.colReceipt')}</th>
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
                                        <p className="text-sm font-medium mb-1">{t('myPurchases.noPaymentsFound')}</p>
                                        <p className="text-xs text-gray-400 mb-4">{t('myPurchases.noPaymentsDesc')}</p>
                                        <Link
                                            to="/catalog"
                                            className="inline-flex items-center px-4 py-2 bg-brand-primary text-white rounded-lg text-sm font-medium hover:bg-brand-secondary transition-colors"
                                        >
                                            {t('myPurchases.goToCatalog')}
                                        </Link>
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
                                            <td className="px-6 py-3 w-full">
                                                <div className="inline-flex flex-col px-3 py-1.5 rounded-md bg-white/60 border border-gray-200 shadow-sm">
                                                    <span className="text-xs font-bold text-gray-800 leading-tight block">
                                                        {payment.courseName}
                                                    </span>
                                                </div>
                                            </td>
                                            <td className="px-6 py-3 whitespace-nowrap text-sm text-gray-700 w-1">
                                                {payment.accessDurationDays != null
                                                    ? `${payment.accessDurationDays} ${t('myPurchases.daysShort')}`
                                                    : <span className="text-xs text-gray-400">{t('myPurchases.unlimited')}</span>
                                                }
                                            </td>
                                            <td className="px-6 py-3 whitespace-nowrap text-sm w-1">
                                                {payment.accessExpiresAt ? (
                                                    (() => {
                                                        const expires = new Date(payment.accessExpiresAt);
                                                        const isExpired = expires < new Date();
                                                        return (
                                                            <span className={`text-sm font-medium ${ isExpired ? 'text-red-500' : 'text-gray-900' }`}>
                                                                {isExpired && <span className="text-xs text-red-400 block">{t('myPurchases.expired')}</span>}
                                                                {format(expires, 'dd.MM.yyyy', { locale: getDateLocale() })}
                                                            </span>
                                                        );
                                                    })()
                                                ) : (
                                                    <span className="text-xs text-gray-400">{t('myPurchases.unlimited')}</span>
                                                )}
                                            </td>
                                            <td className="px-6 py-3 whitespace-nowrap text-sm font-bold text-gray-900 w-1">
                                                {payment.amount.toFixed(2)} {payment.currency || '€'}
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
                                                        title={t('myPurchases.receiptTitle')}
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
                            {/* Filler row to push content up */}
                            <tr className="h-full border-0">
                                <td colSpan={9} className="p-0 border-0"></td>
                            </tr>
                        </tbody>
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
