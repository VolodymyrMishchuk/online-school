import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { X, Image as ImageIcon, Phone, Instagram, Send as TelegramIcon, MessageCircle, Mail, ChevronsUpDown, Check } from 'lucide-react';
import PhoneInput, { isValidPhoneNumber } from 'react-phone-number-input';
import 'react-phone-number-input/style.css';
import { createAppeal } from '../api/appeals';
import { AppealSuccessModal } from '../components/AppealSuccessModal';
import { ConfirmModal } from '../components/ConfirmModal';
import { useTranslation } from 'react-i18next';

const CONTACT_METHODS = [
    { id: 'MOBILE', label: 'Мобільний телефон', labelKey: 'appeal.mobile', icon: Phone, color: 'text-emerald-500' },
    { id: 'INSTAGRAM', label: 'Instagram', labelKey: 'appeal.instagram', icon: Instagram, color: 'text-pink-500' },
    { id: 'TELEGRAM', label: 'Telegram', labelKey: 'appeal.telegram', icon: TelegramIcon, color: 'text-blue-500' },
    { id: 'WHATSAPP', label: 'WhatsApp', labelKey: 'appeal.whatsapp', icon: MessageCircle, color: 'text-green-500' },
    { id: 'EMAIL', label: 'Email', labelKey: 'appeal.email', icon: Mail, color: 'text-black' },
] as const;

type ContactMethodType = typeof CONTACT_METHODS[number]['id'];

export default function UserAppealPage() {
    const { t } = useTranslation();
    const navigate = useNavigate();
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [isSuccessModalOpen, setIsSuccessModalOpen] = useState(false);

    // Alert Modal State
    const [isAlertOpen, setIsAlertOpen] = useState(false);
    const [alertMessage, setAlertMessage] = useState('');
    const [alertTitle, setAlertTitle] = useState('');

    const [contactMethod, setContactMethod] = useState<ContactMethodType>('MOBILE');
    const [isDropdownOpen, setIsDropdownOpen] = useState(false);
    const [contactDetails, setContactDetails] = useState('');
    const [message, setMessage] = useState('');
    const [photos, setPhotos] = useState<File[]>([]);

    const [defaultCountry, setDefaultCountry] = useState<any>('UA');

    useEffect(() => {
        fetch('https://ipapi.co/json/')
            .then(res => res.json())
            .then(data => {
                if (data && data.country_code) {
                    setDefaultCountry(data.country_code);
                }
            })
            .catch(err => console.error('Error fetching country:', err));
    }, []);

    const [isDragging, setIsDragging] = useState(false);
    const [dragCounter, setDragCounter] = useState(0);

    const showAlert = (message: string, title = t('common.error', 'Помилка')) => {
        setAlertMessage(message);
        setAlertTitle(title);
        setIsAlertOpen(true);
    };

    const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        if (e.target.files) {
            const selectedFiles = Array.from(e.target.files);

            // Filter non-image files
            const imageFiles = selectedFiles.filter(file => file.type.startsWith('image/'));

            if (imageFiles.length !== selectedFiles.length) {
                showAlert(t('appeal.alertInvalidImage', 'Дозволені тільки зображення (JPEG, PNG).'));
            }

            setPhotos(prev => {
                const combined = [...prev, ...imageFiles];
                if (combined.length > 10) {
                    showAlert(t('appeal.alertMaxImages', 'Можна прикріпити макс. 10 зображень.'));
                    return combined.slice(0, 10);
                }
                return combined;
            });
        }
    };

    const removeFile = (index: number) => {
        setPhotos(prev => prev.filter((_, i) => i !== index));
    };

    const handleDragEnter = (e: React.DragEvent<HTMLDivElement>) => {
        e.preventDefault();
        e.stopPropagation();
        setDragCounter(prev => prev + 1);
        setIsDragging(true);
    };

    const handleDragOver = (e: React.DragEvent<HTMLDivElement>) => {
        e.preventDefault();
        e.stopPropagation();
    };

    const handleDragLeave = (e: React.DragEvent<HTMLDivElement>) => {
        e.preventDefault();
        e.stopPropagation();
        setDragCounter(prev => prev - 1);
        if (dragCounter - 1 === 0) {
            setIsDragging(false);
        }
    };

    const handleDrop = (e: React.DragEvent<HTMLDivElement>) => {
        e.preventDefault();
        e.stopPropagation();
        setDragCounter(0);
        setIsDragging(false);

        if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
            const selectedFiles = Array.from(e.dataTransfer.files);
            const imageFiles = selectedFiles.filter(file => file.type.startsWith('image/'));

            if (imageFiles.length !== selectedFiles.length) {
                showAlert(t('appeal.alertInvalidImage', 'Дозволені тільки зображення (JPEG, PNG).'));
            }

            if (imageFiles.length > 0) {
                setPhotos(prev => {
                    const combined = [...prev, ...imageFiles];
                    if (combined.length > 10) {
                        showAlert(t('appeal.alertMaxImages', 'Можна прикріпити макс. 10 зображень.'));
                        return combined.slice(0, 10);
                    }
                    return combined;
                });
            }
        }
    };

    const getPlaceholder = () => {
        switch (contactMethod) {
            case 'MOBILE': return t('appeal.placeholderMobile', 'Введіть номер телефону');
            case 'INSTAGRAM': return t('appeal.placeholderInstagram', 'Введіть нікнейм в Instagram');
            case 'TELEGRAM': return t('appeal.placeholderTelegram', 'Введіть номер телефону, або нікнейм через @');
            case 'WHATSAPP': return t('appeal.placeholderWhatsapp', 'Введіть номер WhatsApp');
            case 'EMAIL': return t('appeal.placeholderEmail', 'Введіть електронну адресу');
            default: return t('appeal.placeholderDefault', 'Введіть контактні дані');
        }
    };

    const handleSubmit = async () => {
        if (!contactDetails.trim()) {
            showAlert(t('appeal.placeholderDefault', 'Введіть контактні дані'));
            return;
        }

        if ((contactMethod === 'MOBILE' || contactMethod === 'WHATSAPP') && !isValidPhoneNumber(contactDetails)) {
            showAlert(t('settings.invalidPhoneError', "Введіть коректний номер телефону"));
            return;
        }

        if (!message.trim()) {
            showAlert(t('appeal.alertNoMessage', 'Введіть текст звернення'));
            return;
        }

        try {
            setIsSubmitting(true);
            const formData = new FormData();
            formData.append('contactMethod', contactMethod);
            formData.append('contactDetails', contactDetails);
            formData.append('message', message);

            photos.forEach(photo => {
                formData.append('photos', photo);
            });

            await createAppeal(formData);
            setIsSuccessModalOpen(true);
        } catch (error) {
            showAlert(t('appeal.alertSubmitError', 'Не вдалося відправити звернення.'));
            console.error('Submit error', error);
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <div className="container mx-auto px-6 py-12">
            <div className="flex flex-col gap-2 mb-8">
                <h1 className="text-3xl font-bold text-brand-dark">{t('appeal.title', 'Звернутися')}</h1>
                <p className="text-gray-500 text-sm max-w-2xl">
                    {t('appeal.subtitle', 'Маєте питання чи пропозиції? Залиште звернення, і ми зв\'яжемося з Вами')}
                </p>
            </div>

            <div className="max-w-2xl">
                <div className="glass-panel rounded-lg overflow-hidden">
                    <div className="px-8 py-6 border-b border-gray-200/50 bg-white/30 backdrop-blur-sm">
                        <h2 className="text-xl font-bold text-brand-dark">{t('appeal.newAppeal', 'Нове звернення')}</h2>
                    </div>

                    <div className="p-8">
                        <div className="space-y-6">
                            {/* Contact Method */}
                            <div className="relative">
                                <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2">
                                    {t('appeal.contactMethodLabel', 'Спосіб звʼязку')}
                                </label>
                                <button
                                    type="button"
                                    onClick={() => setIsDropdownOpen(!isDropdownOpen)}
                                    className="w-full px-4 py-3 rounded-lg border border-gray-200 focus:border-brand-primary focus:ring-2 focus:ring-brand-light outline-none transition-all bg-white/50 focus:bg-white flex justify-between items-center"
                                >
                                    <div className="flex items-center gap-3">
                                        {(() => {
                                            const method: any = CONTACT_METHODS.find(m => m.id === contactMethod);
                                            const Icon = method?.icon || Phone;
                                            return (
                                                <>
                                                    <Icon className={`w-5 h-5 ${method?.color}`} />
                                                    <span className="text-gray-700 font-medium">
                                                        {method?.labelKey ? t(method.labelKey) : method?.label}
                                                    </span>
                                                </>
                                            );
                                        })()}
                                    </div>
                                    <ChevronsUpDown className="w-4 h-4 text-gray-400" />
                                </button>

                                {isDropdownOpen && (
                                    <>
                                        <div
                                            className="fixed inset-0 z-10"
                                            onClick={() => setIsDropdownOpen(false)}
                                        />
                                        <div className="absolute z-20 w-full mt-2 bg-white border border-gray-100 rounded-xl shadow-lg shadow-gray-200/50 py-2 animate-in fade-in slide-in-from-top-2 duration-200">
                                            {CONTACT_METHODS.map((method: any) => {
                                                const Icon = method.icon;
                                                const isSelected = contactMethod === method.id;
                                                return (
                                                    <button
                                                        key={method.id}
                                                        type="button"
                                                        onClick={() => {
                                                            if (contactMethod !== method.id) {
                                                                setContactDetails('');
                                                            }
                                                            setContactMethod(method.id);
                                                            setIsDropdownOpen(false);
                                                        }}
                                                        className={`w-full px-4 py-2.5 flex items-center justify-between hover:bg-gray-50 transition-colors ${isSelected ? 'bg-brand-light/10' : ''}`}
                                                    >
                                                        <div className="flex items-center gap-3">
                                                            <div className={`w-8 h-8 rounded-lg flex items-center justify-center ${isSelected ? 'bg-white shadow-sm border border-gray-100' : 'bg-transparent'}`}>
                                                                <Icon className={`w-4 h-4 ${method.color}`} />
                                                            </div>
                                                            <span className={`font-medium ${isSelected ? 'text-brand-dark' : 'text-gray-600'}`}>
                                                                {method.labelKey ? t(method.labelKey) : method.label}
                                                            </span>
                                                        </div>
                                                        {isSelected && <Check className="w-4 h-4 text-brand-primary" />}
                                                    </button>
                                                );
                                            })}
                                        </div>
                                    </>
                                )}
                            </div>

                            {/* Contact Details */}
                            <div>
                                <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2">
                                    {t('appeal.contactDetails', 'Контактні дані')}
                                </label>
                                {contactMethod === 'MOBILE' || contactMethod === 'WHATSAPP' ? (
                                    <>
                                        <style>{`
                                            .phone-input-override .PhoneInputInput {
                                                border: none;
                                                outline: none;
                                                background: transparent;
                                                width: 100%;
                                                color: inherit;
                                            }
                                            .phone-input-override .PhoneInputCountry {
                                                margin-right: 0.5rem;
                                            }
                                            .phone-input-override .PhoneInputCountrySelect {
                                                outline: none;
                                            }
                                        `}</style>
                                        <PhoneInput
                                            international
                                            defaultCountry={defaultCountry}
                                            value={contactDetails}
                                            onChange={(val) => setContactDetails(val || '')}
                                            placeholder={getPlaceholder()}
                                            className="w-full px-4 py-3 rounded-lg border border-gray-200 focus-within:border-brand-primary focus-within:ring-2 focus-within:ring-brand-light outline-none transition-all bg-white/50 focus-within:bg-white phone-input-override"
                                        />
                                        {contactDetails && !isValidPhoneNumber(contactDetails) && (
                                            <p className="text-red-500 text-xs mt-1.5 ml-1">{t('settings.invalidPhoneError', 'Введіть коректний номер телефону')}</p>
                                        )}
                                    </>
                                ) : (
                                    <input
                                        type="text"
                                        value={contactDetails}
                                        onChange={(e) => setContactDetails(e.target.value)}
                                        placeholder={getPlaceholder()}
                                        className="w-full px-4 py-3 rounded-lg border border-gray-200 focus:border-brand-primary focus:ring-2 focus:ring-brand-light outline-none transition-all bg-white/50 focus:bg-white"
                                    />
                                )}
                            </div>

                            {/* Message */}
                            <div>
                                <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-2">
                                    {t('appeal.messageLabel', 'Тема/Текст звернення')}
                                </label>
                                <textarea
                                    rows={5}
                                    value={message}
                                    onChange={(e) => setMessage(e.target.value)}
                                    placeholder={t('appeal.messagePlaceholder', 'Опишіть Вашу проблему чи пропозицію детально...')}
                                    className="w-full px-4 py-3 rounded-lg border border-gray-200 focus:border-brand-primary focus:ring-2 focus:ring-brand-light outline-none transition-all bg-white/50 focus:bg-white resize-none"
                                />
                            </div>

                            {/* Image Upload */}
                            <div>
                                <label className="flex items-center gap-2 text-sm font-medium text-gray-700 mb-3">
                                    {t('appeal.attachImages', 'Прикріпити зображення (макс. 10)')}
                                </label>

                                <div
                                    onDragEnter={handleDragEnter}
                                    onDragOver={handleDragOver}
                                    onDragLeave={handleDragLeave}
                                    onDrop={handleDrop}
                                    className="relative w-full rounded-xl transition-all"
                                >
                                    <div className={`flex gap-4 flex-wrap ${photos.length > 0 ? 'mt-2' : ''}`}>
                                        {photos.map((photo, index) => (
                                            <div key={index} className="relative group w-20 h-20 rounded-lg overflow-hidden border border-gray-200 shadow-sm">
                                                <img
                                                    src={URL.createObjectURL(photo)}
                                                    alt="Preview"
                                                    className="w-full h-full object-cover"
                                                />
                                                <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
                                                    <button
                                                        type="button"
                                                        onClick={(e) => {
                                                            e.preventDefault();
                                                            removeFile(index);
                                                        }}
                                                        className="p-1.5 bg-red-500 text-white rounded-full hover:bg-red-600 transition-colors shadow-lg shadow-red-500/30"
                                                    >
                                                        <X className="w-4 h-4" />
                                                    </button>
                                                </div>
                                            </div>
                                        ))}

                                        {photos.length === 0 ? (
                                            <label className="w-full min-h-[140px] flex flex-col items-center justify-center border-2 border-dashed border-gray-300 rounded-xl text-gray-500 hover:border-brand-primary hover:text-brand-primary hover:bg-brand-primary/5 transition-all cursor-pointer bg-white/50">
                                                <ImageIcon className="w-8 h-8 mb-2" />
                                                <span className="text-sm font-medium text-center px-4">{t('appeal.dragDropLabel', 'Натисніть для вибору або перетягніть файли сюди')}</span>
                                                <span className="text-xs text-gray-400 mt-1">{t('appeal.dragDropFormat', 'JPEG, PNG (до 10 зображень)')}</span>
                                                <input
                                                    type="file"
                                                    multiple
                                                    accept="image/jpeg, image/png, image/jpg"
                                                    className="hidden"
                                                    onChange={handleFileChange}
                                                />
                                            </label>
                                        ) : photos.length < 10 ? (
                                            <label className="w-20 h-20 flex flex-col items-center justify-center gap-1 border-2 border-dashed border-gray-300 rounded-lg text-gray-500 hover:border-brand-primary hover:text-brand-primary hover:bg-brand-primary/5 transition-all cursor-pointer">
                                                <ImageIcon className="w-5 h-5 mb-1" />
                                                <span className="text-[10px] font-medium text-center px-1">{t('appeal.addMore', 'Додати')}</span>
                                                <input
                                                    type="file"
                                                    multiple
                                                    accept="image/jpeg, image/png, image/jpg"
                                                    className="hidden"
                                                    onChange={handleFileChange}
                                                />
                                            </label>
                                        ) : null}
                                    </div>

                                    {isDragging && (
                                        <div className="absolute inset-0 z-10 bg-brand-primary/10 border-2 border-dashed border-brand-primary rounded-xl flex items-center justify-center backdrop-blur-[1px]">
                                            <div className="bg-white px-6 py-3 rounded-full shadow-lg text-brand-primary font-medium flex items-center gap-2">
                                                <ImageIcon className="w-5 h-5" />
                                                {t('appeal.dropFilesHere', 'Відпустіть файли тут')}
                                            </div>
                                        </div>
                                    )}
                                </div>
                            </div>
                        </div>

                        {/* Actions */}
                        <div className="mt-8 pt-6 border-t border-gray-100 grid grid-cols-2 gap-4">
                            <button
                                onClick={() => navigate('/dashboard/all-courses')}
                                disabled={isSubmitting}
                                className="flex items-center justify-center gap-2 px-6 py-3 bg-white text-gray-700 font-bold rounded-lg border border-gray-200 hover:bg-gray-50 transition-colors disabled:opacity-70 disabled:cursor-not-allowed shadow-sm hover:shadow-md transform active:scale-95 duration-200"
                            >
                                {t('appeal.cancel', 'Скасувати')}
                            </button>
                            <button
                                onClick={handleSubmit}
                                disabled={isSubmitting}
                                className="flex items-center justify-center gap-2 px-6 py-3 bg-brand-primary text-white font-bold rounded-lg hover:bg-brand-secondary transition-colors disabled:opacity-70 disabled:cursor-not-allowed shadow-lg hover:shadow-xl transform active:scale-95 duration-200"
                            >
                                {isSubmitting ? (
                                    <>
                                        <div className="animate-spin rounded-full h-4 w-4 border-2 border-white/30 border-t-white"></div>
                                        {t('appeal.sending', 'Відправка...')}
                                    </>
                                ) : (
                                    <>
                                        {t('appeal.send', 'Відправити')}
                                    </>
                                )}
                            </button>
                        </div>
                    </div>
                </div>
            </div>

            <AppealSuccessModal
                isOpen={isSuccessModalOpen}
                onClose={() => setIsSuccessModalOpen(false)}
            />

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
