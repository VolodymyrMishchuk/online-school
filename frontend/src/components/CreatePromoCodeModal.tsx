import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { X, Ticket, Calendar, Plus, Trash2, Search, User, CheckCircle } from 'lucide-react';
import { promoCodesApi, PromoCodeScope, PromoCodeStatus, DiscountType, type PromoCodeResponseDto } from '../api/promoCodes';
import type { PromoCodeCreateFormDto } from '../api/promoCodes';
import { getCourses, type CourseDto } from '../api/courses';
import { getPaginatedUsers } from '../api/users';

interface CreatePromoCodeModalProps {
    isOpen: boolean;
    promoCodeToEdit?: PromoCodeResponseDto | null;
    onClose: () => void;
    onSuccess: () => void;
}

export const CreatePromoCodeModal: React.FC<CreatePromoCodeModalProps> = ({ isOpen, promoCodeToEdit, onClose, onSuccess }) => {
    const { t } = useTranslation();
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [courses, setCourses] = useState<CourseDto[]>([]);

    const [formData, setFormData] = useState<PromoCodeCreateFormDto>({
        code: '',
        status: PromoCodeStatus.ACTIVE,
        scope: PromoCodeScope.GLOBAL,
        targetPersonIds: [],
        validFrom: '',
        validUntil: '',
        discounts: [{ courseId: null, discountType: DiscountType.PERCENTAGE, discountValue: 0 }]
    });

    const [alwaysValid, setAlwaysValid] = useState(true);

    // User Search State
    const [searchUserTerm, setSearchUserTerm] = useState('');
    const [debouncedSearchUser, setDebouncedSearchUser] = useState('');
    const [usersList, setUsersList] = useState<any[]>([]);
    const [isSearchingUsers, setIsSearchingUsers] = useState(false);
    const [selectedUsers, setSelectedUsers] = useState<any[]>([]);
    const [usersPage, setUsersPage] = useState(0);
    const [hasMoreUsers, setHasMoreUsers] = useState(true);

    // Nested Modal State
    const [isUserModalOpen, setIsUserModalOpen] = useState(false);
    const [tempSelectedUserIds, setTempSelectedUserIds] = useState<string[]>([]);
    const [tempSelectedUserObjects, setTempSelectedUserObjects] = useState<any[]>([]);

    const handleOpenUserModal = () => {
        setTempSelectedUserIds(formData.targetPersonIds || []);
        setTempSelectedUserObjects(selectedUsers);
        setSearchUserTerm('');
        setUsersList([]);
        setUsersPage(0);
        setHasMoreUsers(true);
        setIsUserModalOpen(true);
        loadUsers(0, false);
    };

    const handleApplyUserModal = () => {
        setFormData(prev => ({ ...prev, targetPersonIds: tempSelectedUserIds }));
        setSelectedUsers(tempSelectedUserObjects);
        setIsUserModalOpen(false);
    };

    const handleCancelUserModal = () => {
        setIsUserModalOpen(false);
    };

    const toggleTempUser = (user: any) => {
        if (tempSelectedUserIds.includes(user.id)) {
            setTempSelectedUserIds(prev => prev.filter(id => id !== user.id));
            setTempSelectedUserObjects(prev => prev.filter(u => u.id !== user.id));
        } else {
            setTempSelectedUserIds(prev => [...prev, user.id]);
            setTempSelectedUserObjects(prev => [...prev, { id: user.id, name: `${user.firstName} ${user.lastName}`, email: user.email, phone: user.phoneNumber }]);
        }
    };

    useEffect(() => {
        if (isOpen) {
            getCourses().then(setCourses).catch(console.error);
        }
    }, [isOpen]);

    useEffect(() => {
        const timer = setTimeout(() => {
            setDebouncedSearchUser(searchUserTerm);
        }, 500);
        return () => clearTimeout(timer);
    }, [searchUserTerm]);

    const loadUsers = async (page: number, append: boolean = false) => {
        setIsSearchingUsers(true);
        try {
            const res = await getPaginatedUsers(page, 10, debouncedSearchUser);
            if (append) {
                setUsersList(prev => [...prev, ...res.content]);
            } else {
                setUsersList(res.content);
            }
            setHasMoreUsers(!res.last);
            setUsersPage(page);
        } catch (err) {
            console.error(err);
        } finally {
            setIsSearchingUsers(false);
        }
    };

    useEffect(() => {
        if (formData.scope === PromoCodeScope.PERSONAL) {
            loadUsers(0, false);
        } else {
            setUsersList([]);
            setSearchUserTerm('');
        }
    }, [debouncedSearchUser, formData.scope]);

    const handleScrollUsers = (e: React.UIEvent<HTMLDivElement>) => {
        const bottom = e.currentTarget.scrollHeight - e.currentTarget.scrollTop <= e.currentTarget.clientHeight + 10;
        if (bottom && hasMoreUsers && !isSearchingUsers) {
            loadUsers(usersPage + 1, true);
        }
    };

    useEffect(() => {
        if (isOpen) {
            if (promoCodeToEdit) {
                // Formatting date for datetime-local input (YYYY-MM-DDTHH:mm)
                const formatForInput = (dateString?: string) => {
                    if (!dateString) return '';
                    const d = new Date(dateString);
                    // use local time for the input
                    const pad = (n: number) => n.toString().padStart(2, '0');
                    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
                };

                setFormData({
                    code: promoCodeToEdit.code,
                    status: promoCodeToEdit.status || PromoCodeStatus.ACTIVE,
                    scope: promoCodeToEdit.scope,
                    targetPersonIds: promoCodeToEdit.targetPersons?.map(p => p.id) || [],
                    validFrom: formatForInput(promoCodeToEdit.validFrom),
                    validUntil: formatForInput(promoCodeToEdit.validUntil),
                    discounts: promoCodeToEdit.discounts.length > 0
                        ? promoCodeToEdit.discounts.map(d => ({
                            courseId: d.courseId || null,
                            discountType: d.discountType,
                            discountValue: d.discountValue
                        }))
                        : [{ courseId: null, discountType: DiscountType.PERCENTAGE, discountValue: 0 }]
                });

                if (!promoCodeToEdit.validFrom && !promoCodeToEdit.validUntil) {
                    setAlwaysValid(true);
                } else {
                    setAlwaysValid(false);
                }

                if (promoCodeToEdit.scope === PromoCodeScope.PERSONAL && promoCodeToEdit.targetPersons) {
                    setSelectedUsers(promoCodeToEdit.targetPersons);
                } else {
                    setSelectedUsers([]);
                }
            } else {
                setFormData({
                    code: '',
                    status: PromoCodeStatus.ACTIVE,
                    scope: PromoCodeScope.GLOBAL,
                    targetPersonIds: [],
                    validFrom: '',
                    validUntil: '',
                    discounts: [{ courseId: null, discountType: DiscountType.PERCENTAGE, discountValue: 0 }]
                });
                setAlwaysValid(true);
                setSelectedUsers([]);
            }
            setSearchUserTerm('');
            setError(null);
        }
    }, [isOpen, promoCodeToEdit]);

    if (!isOpen) return null;

    const handleGenerateCode = () => {
        const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
        let result = '';
        for (let i = 0; i < 6; i++) {
            result += chars.charAt(Math.floor(Math.random() * chars.length));
        }
        setFormData(prev => ({ ...prev, code: result }));
    };


    const handleRemoveUser = (userId: string) => {
        setFormData(prev => ({ ...prev, targetPersonIds: prev.targetPersonIds?.filter(id => id !== userId) || [] }));
        setSelectedUsers(prev => prev.filter(u => u.id !== userId));
    };

    const handleAddDiscount = () => {
        setFormData(prev => ({
            ...prev,
            discounts: [...prev.discounts, { courseId: null, discountType: DiscountType.PERCENTAGE, discountValue: 0 }]
        }));
    };

    const handleRemoveDiscount = (index: number) => {
        setFormData(prev => ({
            ...prev,
            discounts: prev.discounts.filter((_, i) => i !== index)
        }));
    };

    const handleDiscountChange = (index: number, field: string, value: any) => {
        setFormData(prev => {
            const newDiscounts = [...prev.discounts];
            newDiscounts[index] = { ...newDiscounts[index], [field]: value };
            return { ...prev, discounts: newDiscounts };
        });
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setLoading(true);
        setError(null);

        if (!formData.code.trim()) {
            setError(t('promoCodes.errorEmptyCode', 'Введіть або згенеруйте код'));
            setLoading(false);
            return;
        }

        if (formData.scope === PromoCodeScope.PERSONAL && (!formData.targetPersonIds || formData.targetPersonIds.length === 0)) {
            setError(t('promoCodes.errorEmptyUser', 'Оберіть користувача для персонального промокоду'));
            setLoading(false);
            return;
        }

        if (!alwaysValid && !formData.validFrom && !formData.validUntil) {
            setError(t('promoCodes.errorDatesRequired', 'Оберіть принаймні один часовий параметр (Діє з, або Діє до)'));
            setLoading(false);
            return;
        }

        if (formData.discounts.length === 0) {
            setError(t('promoCodes.errorNoDiscounts', 'Додайте хоча б одну знижку'));
            setLoading(false);
            return;
        }

        try {
            // Re-format dates to match ISO string for LocalDateTime
            const dataToSubmit = { ...formData };
            if (alwaysValid) {
                // Ignore dates
                dataToSubmit.validFrom = null;
                dataToSubmit.validUntil = null;
            } else {
                if (dataToSubmit.validFrom) {
                    dataToSubmit.validFrom = new Date(dataToSubmit.validFrom).toISOString();
                } else {
                    dataToSubmit.validFrom = null;
                }
                if (dataToSubmit.validUntil) {
                    dataToSubmit.validUntil = new Date(dataToSubmit.validUntil).toISOString();
                } else {
                    dataToSubmit.validUntil = null;
                }
            }

            if (promoCodeToEdit) {
                await promoCodesApi.update(promoCodeToEdit.id, dataToSubmit);
            } else {
                await promoCodesApi.create(dataToSubmit);
            }
            
            setFormData({
                code: '',
                status: PromoCodeStatus.ACTIVE,
                scope: PromoCodeScope.GLOBAL,
                targetPersonIds: [],
                validFrom: '',
                validUntil: '',
                discounts: [{ courseId: null, discountType: DiscountType.PERCENTAGE, discountValue: 0 }]
            });
            setAlwaysValid(true);
            setSelectedUsers([]);
            setSearchUserTerm('');
            onSuccess();
            onClose();
        } catch (err: any) {
            setError(err.response?.data?.message || t('promoCodes.createError', 'Помилка при збереженні промокоду'));
        } finally {
            setLoading(false);
        }
    };

    return (
        <>
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-white/30 backdrop-blur-md animate-in fade-in duration-200">
            <div
                className="glass-panel w-full max-w-2xl flex flex-col overflow-hidden animate-in zoom-in-95 duration-200 relative rounded-lg shadow-xl"
                style={{ background: 'rgba(255, 255, 255, 0.9)', maxHeight: '90vh' }}
            >
                <div className="flex items-center justify-between px-6 py-5 border-b border-gray-100 bg-white shrink-0 z-10 relative shadow-sm">
                    <div className="flex items-center gap-3">
                        <div className="flex items-center justify-center w-10 h-10 rounded-full bg-brand-light/50 text-brand-primary ring-2 ring-white shadow-sm">
                            <Ticket className="w-5 h-5" />
                        </div>
                        <h2 className="text-xl font-bold text-brand-dark">
                            {promoCodeToEdit ? t('promoCodes.editTitle', 'Редагувати промокод') : t('promoCodes.createNew', 'Створити промокод')}
                        </h2>
                    </div>
                    <button
                        onClick={onClose}
                        className="p-2 text-gray-400 hover:text-brand-primary hover:bg-white/50 rounded-lg transition-all"
                    >
                        <X className="w-5 h-5" />
                    </button>
                </div>

                <div className="flex-1 overflow-y-auto px-8 py-6 custom-scrollbar flex flex-col gap-6">
                    {error && (
                        <div className="text-sm text-red-600 bg-red-50 p-3 rounded-lg border border-red-100">
                            {error}
                        </div>
                    )}

                    <form id="create-promo-form" onSubmit={handleSubmit} className="space-y-6">
                        {/* Basic Info */}
                        <div className="flex flex-col gap-4">
                            <div>
                                <label className="text-sm font-medium text-gray-700 mb-2 block">{t('promoCodes.codeField', 'Код промокоду')}</label>
                                <div className="flex gap-2">
                                    <input
                                        type="text"
                                        required
                                        value={formData.code}
                                        onChange={(e) => setFormData({ ...formData, code: e.target.value.toUpperCase() })}
                                        placeholder="SALE2024"
                                        className="flex-1 px-4 py-3 rounded-lg border border-gray-200 bg-white/50 outline-none transition-all focus:border-brand-primary focus:ring-2 focus:ring-brand-light focus:bg-white uppercase font-mono"
                                    />
                                    <button
                                        type="button"
                                        onClick={handleGenerateCode}
                                        className="flex items-center justify-center gap-1 text-sm font-medium text-brand-primary hover:text-brand-light bg-brand-light/30 px-4 py-3 rounded-lg transition-colors border border-brand-light/50 shadow-sm"
                                    >
                                        {t('promoCodes.generate', 'Генерувати')}
                                    </button>
                                </div>
                            </div>
                            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                                <div>
                                    <label className="text-sm font-medium text-gray-700 mb-2 block">{t('promoCodes.scopeField', 'Тип (для кого)')}</label>
                                    <div className="relative">
                                        <select
                                            value={formData.scope}
                                            onChange={(e) => {
                                                setFormData({ ...formData, scope: e.target.value as PromoCodeScope, targetPersonIds: [] });
                                                setSelectedUsers([]);
                                            }}
                                            className="w-full px-4 py-3 rounded-lg border border-gray-200 bg-white/50 outline-none transition-all focus:border-brand-primary focus:ring-2 focus:ring-brand-light focus:bg-white pr-10 appearance-none cursor-pointer"
                                        >
                                            <option value={PromoCodeScope.GLOBAL}>{t('promoCodes.scopeGlobal', 'Для всіх')}</option>
                                            <option value={PromoCodeScope.PERSONAL}>{t('promoCodes.scopePersonal', 'Персональний')}</option>
                                        </select>
                                        <div className="absolute inset-y-0 right-0 flex items-center px-4 pointer-events-none text-gray-500">
                                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 9l-7 7-7-7" />
                                            </svg>
                                        </div>
                                    </div>
                                </div>
                                <div>
                                    <label className="text-sm font-medium text-gray-700 mb-2 block">{t('common.status', 'Статус')}</label>
                                    <div className="relative">
                                        <select
                                            value={formData.status}
                                            onChange={(e) => setFormData({ ...formData, status: e.target.value as PromoCodeStatus })}
                                            className="w-full px-4 py-3 rounded-lg border border-gray-200 bg-white/50 outline-none transition-all focus:border-brand-primary focus:ring-2 focus:ring-brand-light focus:bg-white pr-10 appearance-none cursor-pointer"
                                        >
                                            <option value={PromoCodeStatus.ACTIVE}>{t('common.active', 'Активний')}</option>
                                            <option value={PromoCodeStatus.INACTIVE}>{t('common.inactive', 'Неактивний')}</option>
                                        </select>
                                        <div className="absolute inset-y-0 right-0 flex items-center px-4 pointer-events-none text-gray-500">
                                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 9l-7 7-7-7" />
                                            </svg>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>

                        {/* User Selection if Personal */}
                        {formData.scope === PromoCodeScope.PERSONAL && (
                            <div className="bg-gray-50/50 p-4 rounded-lg border border-gray-200">
                                <label className="text-sm font-medium text-gray-700 mb-2 block">{t('promoCodes.selectUser', 'Оберіть користувача')}</label>
                                
                                <div className="space-y-3">
                                    <button 
                                        type="button"
                                        onClick={handleOpenUserModal}
                                        className="w-full text-left px-4 py-3 border border-gray-200 rounded-lg bg-white/50 outline-none transition-all focus:border-brand-primary focus:ring-2 focus:ring-brand-light focus:bg-white text-sm flex justify-between items-center hover:bg-white"
                                    >
                                        <span className={selectedUsers.length > 0 ? "text-gray-900" : "text-gray-500"}>
                                            {selectedUsers.length > 0 
                                                ? t('promoCodes.usersSelected', { count: selectedUsers.length }) 
                                                : t('promoCodes.selectUser')}
                                        </span>
                                        <Search className="w-4 h-4 text-gray-400" />
                                    </button>

                                    {selectedUsers.length > 0 && (
                                        <div className="flex flex-wrap gap-2 mt-3">
                                            {selectedUsers.map(user => (
                                                <div key={user.id} className="flex items-center justify-between bg-white px-3 py-1.5 border border-brand-primary/50 shadow-sm rounded-lg group">
                                                    <div className="flex items-center gap-2 text-brand-dark">
                                                        <User size={14} />
                                                        <span className="font-medium text-xs">{user.name} ({user.email})</span>
                                                    </div>
                                                    <button 
                                                        type="button" 
                                                        onClick={() => handleRemoveUser(user.id)} 
                                                        className="ml-2 text-gray-400 hover:text-red-500 transition-colors opacity-70 group-hover:opacity-100 focus:outline-none"
                                                    >
                                                        <X size={14} />
                                                    </button>
                                                </div>
                                            ))}
                                        </div>
                                    )}
                                </div>
                            </div>
                        )}

                        {/* Dates */}
                        <div className="bg-gray-50/50 p-4 rounded-lg border border-gray-100 flex flex-col gap-4">
                            <label className="flex items-center gap-2 cursor-pointer w-max group">
                                <div className="relative flex items-center">
                                    <input 
                                        type="checkbox" 
                                        checked={alwaysValid}
                                        onChange={(e) => {
                                            setAlwaysValid(e.target.checked);
                                            if (e.target.checked) setFormData({ ...formData, validFrom: '', validUntil: '' });
                                        }}
                                        className="peer h-5 w-5 cursor-pointer appearance-none rounded-md border border-gray-300 transition-all checked:border-brand-primary checked:bg-brand-primary group-hover:border-brand-primary"
                                    />
                                    <CheckCircle className="absolute pointer-events-none opacity-0 peer-checked:opacity-100 text-white w-3.5 h-3.5 left-[3px] top-[3px]" />
                                </div>
                                <span className="text-sm font-medium text-gray-700 group-hover:text-brand-dark transition-colors">{t('promoCodes.alwaysValid', 'Діє завжди')}</span>
                            </label>

                            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                                <div>
                                    <label className={`flex items-center gap-2 text-sm font-medium mb-2 ${alwaysValid ? 'text-gray-400' : 'text-gray-700'}`}>
                                        <Calendar className="w-4 h-4" />
                                        {t('promoCodes.validFrom')}
                                    </label>
                                    <input
                                        type="datetime-local"
                                        value={formData.validFrom || ''}
                                        onChange={(e) => setFormData({ ...formData, validFrom: e.target.value })}
                                        disabled={alwaysValid}
                                        className={`w-full px-4 py-3 rounded-lg border border-gray-200 bg-white/50 outline-none transition-all focus:border-brand-primary focus:ring-2 focus:ring-brand-light focus:bg-white text-sm ${alwaysValid ? 'opacity-50 cursor-not-allowed' : ''}`}
                                    />
                                </div>
                                <div>
                                    <label className={`flex items-center gap-2 text-sm font-medium mb-2 ${alwaysValid ? 'text-gray-400' : 'text-gray-700'}`}>
                                        <Calendar className="w-4 h-4" />
                                        {t('promoCodes.validUntil')}
                                    </label>
                                    <input
                                        type="datetime-local"
                                        value={formData.validUntil || ''}
                                        onChange={(e) => setFormData({ ...formData, validUntil: e.target.value })}
                                        disabled={alwaysValid}
                                        className={`w-full px-4 py-3 rounded-lg border border-gray-200 bg-white/50 outline-none transition-all focus:border-brand-primary focus:ring-2 focus:ring-brand-light focus:bg-white text-sm ${alwaysValid ? 'opacity-50 cursor-not-allowed' : ''}`}
                                    />
                                </div>
                            </div>
                        </div>

                        {/* Discounts */}
                        <div>
                            <div className="flex items-center justify-between mb-3 border-b border-gray-200 pb-2">
                                <h3 className="text-brand-dark font-medium">{t('promoCodes.discountsTitle', 'Налаштування знижок')}</h3>
                                <button
                                    type="button"
                                    onClick={handleAddDiscount}
                                    className="flex items-center gap-1 text-sm font-medium text-brand-primary hover:text-brand-light bg-brand-light/30 px-3 py-1.5 rounded-lg transition-colors border border-brand-light/50 shadow-sm"
                                >
                                    <Plus size={16} /> {t('common.add', 'Додати')}
                                </button>
                            </div>
                            
                            <div className="space-y-3">
                                {formData.discounts.map((discount, index) => (
                                    <div key={index} className="flex flex-wrap md:flex-nowrap gap-3 items-end bg-white p-4 rounded-lg border border-gray-200 relative group shadow-sm hover:border-brand-primary/30 transition-colors">
                                        <div className="flex-1 min-w-[200px]">
                                            <label className="text-xs font-medium text-gray-500 mb-1.5 block">{t('promoCodes.course', 'Курс')}</label>
                                            <div className="relative">
                                                <select
                                                    value={discount.courseId || ''}
                                                    onChange={(e) => handleDiscountChange(index, 'courseId', e.target.value ? e.target.value : null)}
                                                    className="w-full px-3 py-2 border border-gray-200 rounded-md text-sm bg-white/50 outline-none transition-all focus:border-brand-primary focus:ring-2 focus:ring-brand-light focus:bg-white pr-10 appearance-none cursor-pointer"
                                                >
                                                    <option value="">{t('promoCodes.allCourses', 'Всі курси')}</option>
                                                    {courses.map(c => (
                                                        <option key={c.id} value={c.id}>{c.name}</option>
                                                    ))}
                                                </select>
                                                <div className="absolute inset-y-0 right-0 flex items-center px-3 pointer-events-none text-gray-500">
                                                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 9l-7 7-7-7" />
                                                    </svg>
                                                </div>
                                            </div>
                                        </div>
                                        <div className="w-36 shrink-0">
                                            <label className="text-xs font-medium text-gray-500 mb-1.5 block">{t('promoCodes.type', 'Тип')}</label>
                                            <div className="relative">
                                                <select
                                                    value={discount.discountType}
                                                    onChange={(e) => handleDiscountChange(index, 'discountType', e.target.value)}
                                                    className="w-full px-3 py-2 border border-gray-200 rounded-md text-sm bg-white/50 outline-none transition-all focus:border-brand-primary focus:ring-2 focus:ring-brand-light focus:bg-white pr-10 appearance-none cursor-pointer"
                                                >
                                                    <option value={DiscountType.PERCENTAGE}>{t('promoCodes.typePercentage', 'Відсоток (%)')}</option>
                                                    <option value={DiscountType.FIXED_AMOUNT}>{t('promoCodes.typeFixedAmount', 'Фікс. Знижка (€)')}</option>
                                                    <option value={DiscountType.FIXED_PRICE}>{t('promoCodes.typeFixedPrice', 'Фікс. Ціна (€)')}</option>
                                                </select>
                                                <div className="absolute inset-y-0 right-0 flex items-center px-3 pointer-events-none text-gray-500">
                                                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 9l-7 7-7-7" />
                                                    </svg>
                                                </div>
                                            </div>
                                        </div>
                                        <div className="w-28 shrink-0">
                                            <label className="text-xs font-medium text-gray-500 mb-1.5 block">{t('promoCodes.value', 'Значення')}</label>
                                            <input
                                                type="number"
                                                min="0"
                                                step="0.01"
                                                required
                                                value={Number.isNaN(discount.discountValue) ? '' : discount.discountValue}
                                                onChange={(e) => handleDiscountChange(index, 'discountValue', e.target.value ? parseFloat(e.target.value) : '')}
                                                className="w-full px-3 py-2 border border-gray-200 rounded-md text-sm bg-white/50 outline-none transition-all focus:border-brand-primary focus:ring-2 focus:ring-brand-light focus:bg-white font-medium text-gray-900"
                                            />
                                        </div>
                                        
                                        {formData.discounts.length > 1 && (
                                            <button
                                                type="button"
                                                onClick={() => handleRemoveDiscount(index)}
                                                className="h-9 w-9 flex items-center justify-center text-gray-400 hover:text-white rounded-md bg-gray-50 hover:bg-red-500 transition-colors shrink-0 border border-gray-200 hover:border-red-500"
                                            >
                                                <Trash2 size={16} />
                                            </button>
                                        )}
                                    </div>
                                ))}
                            </div>
                        </div>

                    </form>
                </div>

                <div className="flex gap-4 p-6 border-t border-gray-100 bg-white/50 backdrop-blur-sm shrink-0">
                    <button
                        type="button"
                        onClick={onClose}
                        className="flex-1 py-3 font-bold text-gray-700 bg-white hover:bg-gray-50 border border-gray-200 shadow-sm rounded-lg transition-colors"
                    >
                        {t('common.cancelBtn', 'Скасувати')}
                    </button>
                    <button
                        type="submit"
                        form="create-promo-form"
                        disabled={loading}
                        className="flex-1 py-3 font-bold text-white bg-brand-primary hover:bg-brand-secondary rounded-lg transition-all shadow-lg hover:shadow-xl disabled:opacity-70 flex items-center justify-center gap-2"
                    >
                        {loading && <div className="animate-spin h-4 w-4 border-2 border-white/30 border-t-white rounded-full"></div>}
                        {loading ? t('common.saving') : (promoCodeToEdit ? t('common.save') : t('common.create'))}
                    </button>
                </div>
            </div>
        </div>

        {/* Nested User Selection Modal */}
        {isUserModalOpen && (
            <div className="fixed inset-0 z-[60] flex items-center justify-center p-4 bg-white/30 backdrop-blur-md animate-in fade-in duration-200">
                <div className="glass-panel bg-white/95 rounded-lg shadow-2xl shadow-brand-primary/10 w-full max-w-lg max-h-[85vh] flex flex-col overflow-hidden animate-in zoom-in-95 duration-200 relative border border-brand-primary/20">
                    <div className="px-6 py-4 flex items-center justify-between border-b border-brand-primary/10">
                        <h3 className="text-lg font-bold text-brand-dark">{t('promoCodes.selectUser')}</h3>
                        <button type="button" onClick={handleCancelUserModal} className="text-gray-400 hover:text-gray-600 focus:outline-none">
                            <X size={20} />
                        </button>
                    </div>
                    
                    <div className="p-4 border-b border-gray-100">
                        <div className="relative">
                            <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                                <Search className="h-4 w-4 text-gray-400" />
                            </div>
                            <input
                                type="text"
                                autoComplete="off"
                                value={searchUserTerm}
                                onChange={(e) => setSearchUserTerm(e.target.value)}
                                placeholder={t('promoCodes.searchUserPlaceholder')}
                                className="w-full pl-9 pr-4 py-3 border border-gray-200 rounded-lg bg-white/50 outline-none transition-all focus:border-brand-primary focus:ring-2 focus:ring-brand-light focus:bg-white text-sm"
                                autoFocus
                            />
                        </div>
                    </div>
                    
                    <div 
                        className="flex-1 overflow-y-auto p-2 min-h-[300px]"
                        onScroll={handleScrollUsers}
                    >
                        {usersList.length > 0 ? (
                            <ul>
                                {usersList.map(user => {
                                    const isSelected = tempSelectedUserIds.includes(user.id);
                                    return (
                                        <li
                                            key={user.id}
                                            onClick={() => toggleTempUser(user)}
                                            className="px-4 py-3 hover:bg-brand-light/30 cursor-pointer text-sm border-b border-gray-50 last:border-0 transition-colors flex items-center justify-between rounded-lg group"
                                        >
                                            <div>
                                                <div className="font-bold text-gray-900 group-hover:text-brand-dark transition-colors">{user.firstName} {user.lastName}</div>
                                                <div className="text-gray-500 text-xs mt-0.5">{user.email}</div>
                                            </div>
                                            <div className="relative flex items-center">
                                                <input 
                                                    type="checkbox" 
                                                    checked={isSelected} 
                                                    readOnly 
                                                    className="peer h-5 w-5 cursor-pointer appearance-none rounded-md border border-gray-300 transition-all checked:border-brand-primary checked:bg-brand-primary group-hover:border-brand-primary pointer-events-none" 
                                                />
                                                <CheckCircle className="absolute pointer-events-none opacity-0 peer-checked:opacity-100 text-white w-3.5 h-3.5 left-[3px] top-[3px]" />
                                            </div>
                                        </li>
                                    );
                                })}
                            </ul>
                        ) : searchUserTerm.length >= 2 ? (
                            isSearchingUsers ? (
                                <div className="py-8 text-center flex flex-col items-center justify-center text-gray-500">
                                    <div className="animate-spin h-6 w-6 border-2 border-brand-primary rounded-full border-t-transparent mb-2"></div>
                                    <span className="text-sm">{t('promoCodes.searching', 'Пошук...')}</span>
                                </div>
                            ) : (
                                <div className="py-8 text-center text-gray-500 text-sm">
                                    {t('common.noData', 'Даних не знайдено')}
                                </div>
                            )
                        ) : (
                            <div className="py-8 text-center text-gray-400 text-sm">
                                {t('promoCodes.typeToSearch', 'Введіть мінімум 2 символи для пошуку')}
                            </div>
                        )}
                    </div>
                    
                    <div className="p-4 border-t border-gray-100 flex items-center justify-end gap-3 bg-gray-50">
                        <button
                            type="button"
                            onClick={handleCancelUserModal}
                            className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 focus:outline-none transition-colors shadow-sm"
                        >
                            {t('common.cancelBtn', 'Скасувати')}
                        </button>
                        <button
                            type="button"
                            onClick={handleApplyUserModal}
                            className="px-4 py-2 text-sm font-medium text-white bg-brand-primary rounded-lg hover:bg-brand-primary/90 focus:outline-none transition-colors flex items-center gap-2 shadow-sm shadow-brand-primary/20"
                        >
                            {t('common.applyBtn', 'Застосувати')} {tempSelectedUserIds.length > 0 && `(${tempSelectedUserIds.length})`}
                        </button>
                    </div>
                </div>
            </div>
        )}
        </>
    );
};
