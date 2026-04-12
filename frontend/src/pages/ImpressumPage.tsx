import { useTranslation } from 'react-i18next';

export default function ImpressumPage() {
    const { t } = useTranslation();

    return (
        <div className="max-w-4xl mx-auto px-6 py-16 w-full flex-grow text-stone-800">
            <h1 className="text-3xl md:text-4xl font-bold mb-8 text-brand-dark">
                {t('legal.impressum', 'Імпресум')}
            </h1>
            
            <div className="prose prose-stone max-w-none prose-a:text-brand-primary placeholder-content">
                <p className="lead text-xl text-stone-600 mb-8">
                    {t('legal.impressumDesc', 'Дані про компанію та контакти.')}
                </p>
                
                <section className="mb-8">
                    <h2 className="text-2xl font-bold mb-4">Власник сайту</h2>
                    <p>Тут буде розміщена офіційна інформація про власника бізнесу (ФОП, ТОВ тощо).</p>
                    <ul className="list-disc pl-6 mt-4 space-y-2">
                        <li><strong>Назва компанії / ФОП:</strong> [Ваша Назва]</li>
                        <li><strong>ІПН / ЄДРПОУ:</strong> [Ваш Код]</li>
                        <li><strong>Юридична адреса:</strong> [Ваша Адреса]</li>
                    </ul>
                </section>

                <section className="mb-8">
                    <h2 className="text-2xl font-bold mb-4">Контакти</h2>
                    <ul className="list-disc pl-6 space-y-2">
                        <li><strong>Email:</strong> [Ваш Email]</li>
                        <li><strong>Телефон:</strong> [Ваш Телефон]</li>
                    </ul>
                </section>
            </div>
        </div>
    );
}
