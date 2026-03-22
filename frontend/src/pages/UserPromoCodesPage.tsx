import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Ticket, Search, CheckCircle, AlertCircle, BookOpen } from 'lucide-react';
import { promoCodesApi, DiscountType } from '../api/promoCodes';
import type { PromoCodeCheckResponseDto } from '../api/promoCodes';
import { getCourses } from '../api/courses';
import type { CourseDto } from '../api/courses';
import { getModules } from '../api/modules';
import CourseExpandableCard from '../components/CourseExpandableCard';
import { ConfirmModal } from '../components/ConfirmModal';

export const UserPromoCodesPage: React.FC = () => {
    const { t } = useTranslation();
    const queryClient = useQueryClient();
    const [code, setCode] = useState('');
    const [loading, setLoading] = useState(false);
    const [result, setResult] = useState<PromoCodeCheckResponseDto | null>(null);
    const [error, setError] = useState<string | null>(null);

    // Modal state
    const [courseToEnroll, setCourseToEnroll] = useState<CourseDto | null>(null);
    const [enrollLoading, setEnrollLoading] = useState(false);
    const [alertOpen, setAlertOpen] = useState(false);
    const [alertMessage, setAlertMessage] = useState('');
    const [alertType, setAlertType] = useState<'info' | 'danger'>('info');
    const [alertTitle, setAlertTitle] = useState('');

    // Fetch real courses & modules exactly like MyCoursesPage / AllCoursesPage
    const { data: allCourses, refetch: refetchCourses } = useQuery({
        queryKey: ['allCourses'],
        queryFn: () => getCourses(),
    });

    const { data: modules } = useQuery({
        queryKey: ['allModules'],
        queryFn: () => getModules(),
    });

    const handleCheckCode = async (e: React.FormEvent) => {
        e.preventDefault();
        const trimmedCode = code.trim();
        if (!trimmedCode) return;

        setLoading(true);
        setError(null);
        setResult(null);

        try {
            const data = await promoCodesApi.check(trimmedCode);
            setResult(data);
        } catch (err: any) {
            setError(err.response?.data?.message || t('promoCodes.user.invalidCode'));
        } finally {
            setLoading(false);
        }
    };

    // Called when user clicks "Придбати курс" — open confirmation modal
    const handleEnroll = (courseId: string) => {
        if (!result) return;

        const course = promoCourses.find(c => c.id === courseId);
        if (!course) return;

        // Check if already enrolled
        const realCourse = allCourses?.find(c => c.id === courseId);
        if (realCourse?.isEnrolled) {
            showAlert(
                t('common.notification', 'Сповіщення'),
                t('allCourses.alreadyEnrolled', 'Ви вже придбали цей курс!'),
                'info'
            );
            return;
        }

        setCourseToEnroll(course);
    };

    // Called when user confirms purchase in the modal
    const confirmEnroll = async () => {
        if (!result || !courseToEnroll) return;

        setEnrollLoading(true);
        try {
            await promoCodesApi.use(result.code, courseToEnroll.id);
            await refetchCourses();
            await queryClient.invalidateQueries({ queryKey: ['allCourses'] });

            setCourseToEnroll(null);
            showAlert(
                t('promoCodes.user.purchaseSuccessTitle', 'Успішна покупка!'),
                t('promoCodes.user.purchaseSuccessMsg', 'Ви успішно придбали курс "{{name}}" зі знижкою за промокодом. Бажаємо успішного навчання!', { name: courseToEnroll.name }),
                'info'
            );
        } catch (err: any) {
            setCourseToEnroll(null);
            showAlert(
                t('promoCodes.user.purchaseErrorTitle', 'Помилка'),
                err.response?.data?.message || t('promoCodes.user.purchaseErrorMsg', 'Не вдалося придбати курс. Спробуйте ще раз.'),
                'danger'
            );
        } finally {
            setEnrollLoading(false);
        }
    };

    const showAlert = (title: string, message: string, type: 'info' | 'danger') => {
        setAlertTitle(title);
        setAlertMessage(message);
        setAlertType(type);
        setAlertOpen(true);
    };

    // Calculate promo price for display in modal
    const getPromoPrice = (course: CourseDto): string | null => {
        if (!result) return null;
        for (const discount of result.discounts) {
            const matches = !discount.courseId || discount.courseId === course.id;
            if (matches) {
                const originalPrice = (allCourses?.find(c => c.id === course.id)?.price) ?? course.price ?? 0;
                switch (discount.discountType) {
                    case DiscountType.PERCENTAGE: {
                        const promo = originalPrice * (1 - discount.discountValue / 100);
                        return `${Math.max(0, promo).toFixed(2)}€`;
                    }
                    case DiscountType.FIXED_AMOUNT: {
                        const promo = originalPrice - discount.discountValue;
                        return `${Math.max(0, promo).toFixed(2)}€`;
                    }
                    case DiscountType.FIXED_PRICE:
                        return `${discount.discountValue.toFixed(2)}€`;
                }
            }
        }
        return null;
    };

    // Build the list of real CourseDto with promo prices overlaid
    const getPromoCourses = (): CourseDto[] => {
        if (!result || !allCourses) return [];

        const promoCourses: CourseDto[] = [];
        const addedIds = new Set<string>();

        for (const discount of result.discounts) {
            if (discount.courseId) {
                const realCourse = allCourses.find(c => c.id === discount.courseId);
                if (realCourse && !addedIds.has(realCourse.id)) {
                    promoCourses.push(applyDiscount(realCourse, discount.discountType, discount.discountValue));
                    addedIds.add(realCourse.id);
                }
            } else {
                for (const course of allCourses) {
                    if (!addedIds.has(course.id)) {
                        promoCourses.push(applyDiscount(course, discount.discountType, discount.discountValue));
                        addedIds.add(course.id);
                    }
                }
            }
        }

        return promoCourses;
    };

    const applyDiscount = (course: CourseDto, discountType: DiscountType, discountValue: number): CourseDto => {
        const copy = { ...course };
        switch (discountType) {
            case DiscountType.PERCENTAGE:
                copy.discountPercentage = discountValue;
                copy.discountAmount = undefined;
                break;
            case DiscountType.FIXED_AMOUNT:
                copy.discountAmount = discountValue;
                copy.discountPercentage = undefined;
                break;
            case DiscountType.FIXED_PRICE:
                copy.price = discountValue;
                copy.discountAmount = undefined;
                copy.discountPercentage = undefined;
                break;
        }
        return copy;
    };

    const promoCourses = getPromoCourses();

    return (
        <div className="container mx-auto px-6 py-12">
            <div className="flex flex-col gap-2 mb-8">
                <h1 className="text-3xl font-bold text-brand-dark">{t('promoCodes.user.title')}</h1>
                <p className="text-gray-500 text-sm max-w-2xl">
                    {t('promoCodes.user.description')}
                </p>
            </div>

            <div className="max-w-2xl mb-8">
                <div className="glass-panel rounded-lg overflow-hidden">
                    <div className="px-8 py-6 border-b border-gray-200/50 bg-white/30 backdrop-blur-sm">
                        <h2 className="text-xl font-bold text-brand-dark flex items-center gap-2">
                            <Ticket className="w-5 h-5 text-brand-primary" />
                            {t('promoCodes.user.checkBtn')}
                        </h2>
                    </div>

                    <div className="p-8">
                        <form onSubmit={handleCheckCode} className="flex flex-col sm:flex-row gap-4">
                            <div className="flex-1 relative">
                                <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                                    <Ticket className="h-5 w-5 text-gray-400" />
                                </div>
                                <input
                                    type="text"
                                    value={code}
                                    onChange={(e) => setCode(e.target.value.toUpperCase())}
                                    placeholder={t('promoCodes.user.inputPlaceholder')}
                                    className="w-full pl-12 pr-4 py-3 border border-gray-200 rounded-lg focus:border-brand-primary focus:ring-2 focus:ring-brand-light outline-none transition-all bg-white/50 focus:bg-white uppercase font-mono tracking-widest"
                                />
                            </div>
                            <button
                                type="submit"
                                disabled={loading || !code.trim()}
                                className="py-3 px-6 bg-brand-primary hover:bg-brand-secondary text-white font-bold rounded-lg transition-colors shadow-lg hover:shadow-xl transform active:scale-95 disabled:opacity-70 disabled:transform-none flex items-center justify-center gap-2 whitespace-nowrap"
                            >
                                {loading ? (
                                    <div className="animate-spin h-5 w-5 border-2 border-white/30 border-t-white rounded-full"></div>
                                ) : (
                                    <Search className="w-5 h-5" />
                                )}
                                <span>{loading ? t('promoCodes.user.checking') : t('promoCodes.user.checkBtn')}</span>
                            </button>
                        </form>

                        {error && (
                            <div className="mt-6 flex items-start gap-3 p-4 bg-red-50 text-red-700 rounded-lg border border-red-100">
                                <AlertCircle className="w-5 h-5 shrink-0 mt-0.5" />
                                <div>
                                    <h4 className="font-semibold text-red-800">{t('promoCodes.user.errorTitle')}</h4>
                                    <p className="text-sm mt-1 opacity-90">{error}</p>
                                </div>
                            </div>
                        )}
                    </div>
                </div>
            </div>

            {/* Course cards — identical to All Courses, with promo prices + buy button */}
            {result && promoCourses.length > 0 && (
                <div>
                    <div className="flex items-center gap-2 text-green-600 bg-green-50 px-4 py-2 rounded-lg w-max border border-green-200 mb-6">
                        <CheckCircle className="w-5 h-5" />
                        <span className="font-semibold">{t('promoCodes.user.successMsg')}</span>
                    </div>

                    <div className="flex items-center gap-3 mb-6">
                        <BookOpen className="w-6 h-6 text-brand-primary" />
                        <h3 className="text-2xl font-bold text-brand-dark">{t('promoCodes.user.availableCourses')}</h3>
                        <span className="text-gray-400 font-medium">({promoCourses.length})</span>
                    </div>

                    <div className="flex flex-col gap-6">
                        {promoCourses.map((course) => (
                            <CourseExpandableCard
                                key={course.id}
                                course={course}
                                modules={modules || []}
                                onEdit={() => {}}
                                onDelete={() => {}}
                                onEnroll={handleEnroll}
                                onEditLesson={undefined}
                                onDeleteLesson={undefined}
                                isCatalogMode={true}
                            />
                        ))}
                    </div>
                </div>
            )}

            {result && promoCourses.length === 0 && (
                <div className="text-center py-12 text-gray-500">
                    <BookOpen size={48} className="mx-auto text-gray-300 mb-4" />
                    <p className="text-lg">{t('promoCodes.user.noCourses')}</p>
                </div>
            )}

            {/* Purchase confirmation modal */}
            <ConfirmModal
                isOpen={!!courseToEnroll}
                onClose={() => setCourseToEnroll(null)}
                onConfirm={confirmEnroll}
                title={t('promoCodes.user.purchaseConfirmTitle', 'Підтвердження покупки')}
                message={t('promoCodes.user.purchaseConfirmMsg', 'Ви бажаєте придбати курс "{{name}}" за ціною {{price}} з промокодом {{code}}?', {
                    name: courseToEnroll?.name || '',
                    price: courseToEnroll ? getPromoPrice(courseToEnroll) || '' : '',
                    code: result?.code || ''
                })}
                confirmText={t('promoCodes.user.purchaseBtn', 'Придбати')}
                cancelText={t('common.cancelBtn', 'Скасувати')}
                type="info"
                isLoading={enrollLoading}
            />

            {/* Success / Error alert modal */}
            <ConfirmModal
                isOpen={alertOpen}
                onClose={() => setAlertOpen(false)}
                onConfirm={() => setAlertOpen(false)}
                title={alertTitle}
                message={alertMessage}
                isAlert={true}
                type={alertType}
            />
        </div>
    );
};
