import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { FileText, Shield, FileSignature } from 'lucide-react';

export default function LegalPage() {
    const { t } = useTranslation();

    const legalItems = [
        {
            title: t('legal.impressum', 'Імпресум'),
            description: t('legal.impressumDesc', 'Дані про компанію та контакти.'),
            url: '/impressum',
            icon: <FileText className="w-6 h-6 text-brand-primary" />
        },
        {
            title: t('legal.privacy', 'Політика конфіденційності'),
            description: t('legal.privacyDesc', 'Як ми збираємо та використовуємо ваші дані.'),
            url: '/privacy',
            icon: <Shield className="w-6 h-6 text-brand-primary" />
        },
        {
            title: t('legal.terms', 'Публічна оферта'),
            description: t('legal.termsDesc', 'Правила та умови використання нашого сервісу.'),
            url: '/terms',
            icon: <FileSignature className="w-6 h-6 text-brand-primary" />
        }
    ];

    return (
        <div className="p-6 md:p-8 max-w-5xl mx-auto space-y-6">
            <div>
                <h1 className="text-2xl font-bold text-gray-800">{t('legal.title', 'Правова інформація')}</h1>
                <p className="text-gray-500 mt-1">{t('legal.subtitle', 'Офіційна інформація та правила використання сервісу')}</p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {legalItems.map((item, index) => (
                    <Link 
                        key={index} 
                        to={item.url}
                        className="bg-white p-6 rounded-2xl border border-gray-100 shadow-sm hover:shadow-md transition-shadow flex flex-col items-start gap-4 group"
                        target="_blank"
                    >
                        <div className="bg-brand-light p-3 rounded-xl group-hover:bg-brand-primary/10 transition-colors">
                            {item.icon}
                        </div>
                        <div>
                            <h2 className="text-lg font-semibold text-gray-800 mb-1">{item.title}</h2>
                            <p className="text-sm text-gray-500 line-clamp-2">{item.description}</p>
                        </div>
                    </Link>
                ))}
            </div>
        </div>
    );
}
