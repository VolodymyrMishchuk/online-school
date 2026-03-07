import { FileCheck } from 'lucide-react';
import { useTranslation } from 'react-i18next';

export default function MyLessonsPage() {
    const { t } = useTranslation();
    return (
        <div className="container mx-auto px-6 py-12">
            <div className="flex items-center gap-3 mb-8">
                <FileCheck className="w-8 h-8 text-brand-primary" />
                <h1 className="text-3xl font-bold text-brand-dark">{t('myLessons.title', 'Мої уроки')}</h1>
            </div>

            <div className="bg-white rounded-3xl shadow-sm border border-gray-100 p-16 text-center">
                <p className="text-gray-500">{t('myLessons.inDevelopment', 'Сторінка в розробці. Тут будуть уроки з ваших курсів.')}</p>
            </div>
        </div>
    );
}
