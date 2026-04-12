import { useTranslation } from 'react-i18next';

export default function TermsPage() {
    const { t } = useTranslation();

    return (
        <div className="max-w-4xl mx-auto px-6 py-16 w-full flex-grow text-stone-800">
            <h1 className="text-3xl md:text-4xl font-bold mb-8 text-brand-dark">
                {t('legal.terms', 'Публічна оферта')}
            </h1>
            
            <div className="prose prose-stone max-w-none prose-a:text-brand-primary placeholder-content">
                <p className="lead text-xl text-stone-600 mb-8">
                    {t('legal.termsDesc', 'Правила та умови використання нашого сервісу.')}
                </p>
                
                <section className="mb-8">
                    <h2 className="text-2xl font-bold mb-4">1. Загальні положення</h2>
                    <p>Цей договір є публічною офертою. Використовуючи наш сайт та послуги, ви погоджуєтесь з цими умовами.</p>
                    <p className="mt-4 text-stone-500 italic">[Тут має бути детальний юридичний текст угоди користувача, умови оплати, повернення коштів, доступ до курсів тощо.]</p>
                </section>

                <section className="mb-8">
                    <h2 className="text-2xl font-bold mb-4">2. Оплата та доступ</h2>
                    <p>Доступ до навчальних матеріалів надається після повної або часткової передоплати згідно з обраним тарифом.</p>
                </section>
                
                <section className="mb-8">
                    <h2 className="text-2xl font-bold mb-4">3. Повернення коштів</h2>
                    <p>Умови та строки повернення коштів регулюються чинним законодавством (Закон України "Про захист прав споживачів").</p>
                </section>
            </div>
        </div>
    );
}
