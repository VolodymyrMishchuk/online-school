import { useState } from 'react';
import { X, Image as ImageIcon, Phone, Instagram, Send as TelegramIcon, MessageCircle, Mail, ChevronsUpDown, Check } from 'lucide-react';
import PhoneInput, { isValidPhoneNumber } from 'react-phone-number-input';
import 'react-phone-number-input/style.css';
import { createPublicAppeal } from '../../api/appeals';
import { AppealSuccessModal } from '../AppealSuccessModal';
import { ConfirmModal } from '../ConfirmModal';
import { useTranslation } from 'react-i18next';
import { useCountryCode } from '../../hooks/useCountryCode';

const CONTACT_METHODS = [
    { id: 'MOBILE', label: 'Мобільний телефон', labelKey: 'appeal.mobile', icon: Phone, color: 'text-emerald-500' },
    { id: 'INSTAGRAM', label: 'Instagram', labelKey: 'appeal.instagram', icon: Instagram, color: 'text-pink-500' },
    { id: 'TELEGRAM', label: 'Telegram', labelKey: 'appeal.telegram', icon: TelegramIcon, color: 'text-blue-500' },
    { id: 'WHATSAPP', label: 'WhatsApp', labelKey: 'appeal.whatsapp', icon: MessageCircle, color: 'text-green-500' },
    { id: 'EMAIL', label: 'Email', labelKey: 'appeal.email', icon: Mail, color: 'text-stone-800' },
] as const;

type ContactMethodType = typeof CONTACT_METHODS[number]['id'];

export default function ContactFormSection() {
    const { t } = useTranslation();
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [isSuccessModalOpen, setIsSuccessModalOpen] = useState(false);

    // Alert Modal State
    const [isAlertOpen, setIsAlertOpen] = useState(false);
    const [alertMessage, setAlertMessage] = useState('');
    const [alertTitle, setAlertTitle] = useState('');

    const [name, setName] = useState('');
    const [contactMethod, setContactMethod] = useState<ContactMethodType>('MOBILE');
    const [isDropdownOpen, setIsDropdownOpen] = useState(false);
    const [contactDetails, setContactDetails] = useState('');
    const [message, setMessage] = useState('');
    const [photos, setPhotos] = useState<File[]>([]);
    const defaultCountry = useCountryCode('UA');

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
        if (!name.trim()) {
            showAlert("Будь ласка, введіть Ваше ім'я.");
            return;
        }

        if (!contactDetails.trim()) {
            showAlert(t('appeal.placeholderDefault', 'Введіть контактні дані'));
            return;
        }

        if ((contactMethod === 'MOBILE' || contactMethod === 'WHATSAPP') && !isValidPhoneNumber(contactDetails)) {
            showAlert(t('settings.invalidPhoneError', "Введіть коректний номер телефону"));
            return;
        }

        if (contactMethod === 'EMAIL') {
            const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
            if (!emailRegex.test(contactDetails)) {
                showAlert(t('appeal.invalidEmailError', 'Введіть коректну електронну адресу'));
                return;
            }
        }

        if (!message.trim()) {
            showAlert(t('appeal.alertNoMessage', 'Введіть текст звернення'));
            return;
        }

        try {
            setIsSubmitting(true);
            const formData = new FormData();
            formData.append('name', name);
            formData.append('contactMethod', contactMethod);
            formData.append('contactDetails', contactDetails);
            formData.append('message', message);

            photos.forEach(photo => {
                formData.append('photos', photo);
            });

            await createPublicAppeal(formData);
            setIsSuccessModalOpen(true);
            
            // Clear form
            setName('');
            setContactDetails('');
            setMessage('');
            setPhotos([]);
            setContactMethod('MOBILE');
        } catch (error) {
            showAlert(t('appeal.alertSubmitError', 'Не вдалося відправити звернення.'));
            console.error('Submit error', error);
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <section id="contact" className="py-24 lg:py-32 relative bg-white/50">
            <div className="absolute top-0 left-0 w-full h-px bg-gradient-to-r from-transparent via-brand-primary/20 to-transparent" />
            
            <div className="max-w-3xl mx-auto px-6 relative z-10">
                <div className="text-center mb-12">
                    <h2 className="text-3xl sm:text-4xl font-bold text-stone-900 mb-4">
                        Залишилися питання?
                    </h2>
                    <p className="text-stone-500 text-lg">
                        Напишіть нам, і ми з радістю проконсультуємо вас щодо курсу.
                    </p>
                </div>

                <div className="glass-strong rounded-3xl overflow-hidden shadow-xl p-8 lg:p-12">
                    <div className="space-y-6">
                        {/* Name */}
                        <div>
                            <label className="flex items-center gap-2 text-sm font-medium text-stone-700 mb-2">
                                Ваше ім'я
                            </label>
                            <input
                                type="text"
                                value={name}
                                onChange={(e) => setName(e.target.value)}
                                placeholder="Як до вас звертатися?"
                                className="w-full px-4 py-3 rounded-xl border border-stone-200 focus:border-brand-primary focus:ring-2 focus:ring-brand-light outline-none transition-all bg-white hover:bg-white/90"
                            />
                        </div>

                        {/* Contact Method */}
                        <div className="relative">
                            <label className="flex items-center gap-2 text-sm font-medium text-stone-700 mb-2">
                                {t('appeal.contactMethodLabel', 'Спосіб звʼязку')}
                            </label>
                            <button
                                type="button"
                                onClick={() => setIsDropdownOpen(!isDropdownOpen)}
                                className="w-full px-4 py-3 rounded-xl border border-stone-200 focus:border-brand-primary focus:ring-2 focus:ring-brand-light outline-none transition-all bg-white hover:bg-white/90 flex justify-between items-center"
                            >
                                <div className="flex items-center gap-3">
                                    {(() => {
                                        const method: any = CONTACT_METHODS.find(m => m.id === contactMethod);
                                        const Icon = method?.icon || Phone;
                                        return (
                                            <>
                                                <Icon className={`w-5 h-5 ${method?.color}`} />
                                                <span className="text-stone-700 font-medium">
                                                    {method?.labelKey ? t(method.labelKey) : method?.label}
                                                </span>
                                            </>
                                        );
                                    })()}
                                </div>
                                <ChevronsUpDown className="w-4 h-4 text-stone-400" />
                            </button>

                            {isDropdownOpen && (
                                <>
                                    <div
                                        className="fixed inset-0 z-10"
                                        onClick={() => setIsDropdownOpen(false)}
                                    />
                                    <div className="absolute z-20 w-full mt-2 bg-white border border-stone-100 rounded-xl shadow-xl py-2 animate-in fade-in slide-in-from-top-2 duration-200">
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
                                                    className={`w-full px-4 py-2.5 flex items-center justify-between hover:bg-stone-50 transition-colors ${isSelected ? 'bg-brand-light/10' : ''}`}
                                                >
                                                    <div className="flex items-center gap-3">
                                                        <div className={`w-8 h-8 rounded-lg flex items-center justify-center ${isSelected ? 'bg-white shadow-sm border border-stone-100' : 'bg-transparent'}`}>
                                                            <Icon className={`w-4 h-4 ${method.color}`} />
                                                        </div>
                                                        <span className={`font-medium ${isSelected ? 'text-stone-900' : 'text-stone-600'}`}>
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
                            <label className="flex items-center gap-2 text-sm font-medium text-stone-700 mb-2">
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
                                        className="w-full px-4 py-3 rounded-xl border border-stone-200 focus-within:border-brand-primary focus-within:ring-2 focus-within:ring-brand-light outline-none transition-all bg-white hover:bg-white/90 phone-input-override"
                                    />
                                    {contactDetails && !isValidPhoneNumber(contactDetails) && (
                                        <p className="text-red-500 text-xs mt-1.5 ml-1">{t('settings.invalidPhoneError', 'Введіть коректний номер телефону')}</p>
                                    )}
                                </>
                            ) : (
                                <>
                                    <input
                                        type="email"
                                        value={contactDetails}
                                        onChange={(e) => setContactDetails(e.target.value)}
                                        placeholder={getPlaceholder()}
                                        className="w-full px-4 py-3 rounded-xl border border-stone-200 focus:border-brand-primary focus:ring-2 focus:ring-brand-light outline-none transition-all bg-white hover:bg-white/90"
                                    />
                                    {contactMethod === 'EMAIL' && contactDetails && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(contactDetails) && (
                                        <p className="text-red-500 text-xs mt-1.5 ml-1">{t('appeal.invalidEmailError', 'Введіть коректну електронну адресу')}</p>
                                    )}
                                </>
                            )}
                        </div>

                        {/* Message */}
                        <div>
                            <label className="flex items-center gap-2 text-sm font-medium text-stone-700 mb-2">
                                Ваше питання
                            </label>
                            <textarea
                                rows={4}
                                value={message}
                                onChange={(e) => setMessage(e.target.value)}
                                placeholder="Опишіть, що вас цікавить..."
                                className="w-full px-4 py-3 rounded-xl border border-stone-200 focus:border-brand-primary focus:ring-2 focus:ring-brand-light outline-none transition-all bg-white hover:bg-white/90 resize-none"
                            />
                        </div>

                        {/* Image Upload */}
                        <div>
                            <label className="flex items-center gap-2 text-sm font-medium text-stone-700 mb-3">
                                {t('appeal.attachImages', 'Прикріпити зображення (макс. 10)')}
                            </label>

                            <div
                                onDragEnter={handleDragEnter}
                                onDragOver={handleDragOver}
                                onDragLeave={handleDragLeave}
                                onDrop={handleDrop}
                                className="relative w-full rounded-xl transition-all"
                            >
                                <div className={`flex gap-4 flex-wrap ${photos.length > 0 ? 'mb-4' : ''}`}>
                                    {photos.map((photo, index) => (
                                        <div key={index} className="relative group w-20 h-20 rounded-xl overflow-hidden border border-stone-200 shadow-sm">
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
                                                    className="p-1.5 bg-red-500 text-white rounded-full hover:bg-red-600 transition-colors shadow-lg"
                                                >
                                                    <X className="w-4 h-4" />
                                                </button>
                                            </div>
                                        </div>
                                    ))}

                                    {photos.length === 0 ? (
                                        <label className="w-full min-h-[140px] flex flex-col items-center justify-center border-2 border-dashed border-stone-300 rounded-xl text-stone-500 hover:border-brand-primary hover:text-brand-primary hover:bg-brand-primary/5 transition-all cursor-pointer bg-white/50">
                                            <ImageIcon className="w-8 h-8 mb-2 opacity-50" />
                                            <span className="text-sm font-medium text-center px-4">{t('appeal.dragDropLabel', 'Натисніть для вибору або перетягніть файли сюди')}</span>
                                            <span className="text-xs text-stone-400 mt-1">{t('appeal.dragDropFormat', 'JPEG, PNG (до 10 зображень)')}</span>
                                            <input
                                                type="file"
                                                multiple
                                                accept="image/jpeg, image/png, image/jpg"
                                                className="hidden"
                                                onChange={handleFileChange}
                                            />
                                        </label>
                                    ) : photos.length < 10 ? (
                                        <label className="w-20 h-20 flex flex-col items-center justify-center gap-1 border-2 border-dashed border-stone-300 rounded-xl text-stone-500 hover:border-brand-primary hover:text-brand-primary hover:bg-brand-primary/5 transition-all cursor-pointer bg-white/50">
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
                                    <div className="absolute inset-0 z-10 bg-brand-primary/10 border-2 border-dashed border-brand-primary rounded-xl flex items-center justify-center backdrop-blur-[2px]">
                                        <div className="bg-white px-6 py-3 rounded-full shadow-lg text-brand-primary font-medium flex items-center gap-2">
                                            <ImageIcon className="w-5 h-5" />
                                            {t('appeal.dropFilesHere', 'Відпустіть файли тут')}
                                        </div>
                                    </div>
                                )}
                            </div>
                        </div>

                        {/* Actions */}
                        <div className="mt-8 pt-6 border-t border-stone-100 flex justify-end">
                            <button
                                onClick={handleSubmit}
                                disabled={isSubmitting}
                                className="w-full sm:w-auto px-8 py-3.5 bg-brand-primary text-white font-bold rounded-xl hover:bg-brand-secondary transition-colors disabled:opacity-70 disabled:cursor-not-allowed shadow-xl shadow-brand-primary/25 hover:shadow-2xl hover:shadow-brand-primary/30 transform hover:-translate-y-0.5 active:translate-y-0 duration-200 flex items-center justify-center gap-2"
                            >
                                {isSubmitting ? (
                                    <>
                                        <div className="animate-spin rounded-full h-5 w-5 border-2 border-white/30 border-t-white"></div>
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
                onClose={() => {
                    setIsSuccessModalOpen(false);
                    // Clear form or perform other actions if nested here
                }}
                redirectOnClose={false}
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
        </section>
    );
}
