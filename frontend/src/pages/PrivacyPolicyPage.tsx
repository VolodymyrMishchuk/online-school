import { useTranslation } from 'react-i18next';

export default function PrivacyPolicyPage() {
    const { t } = useTranslation();

    return (
        <div className="max-w-4xl mx-auto px-6 py-16 w-full flex-grow text-stone-800">
            <h1 className="text-3xl md:text-4xl font-bold mb-8 text-brand-dark">
                {t('legal.privacy', 'Політика конфіденційності')}
            </h1>
            
            <div className="prose prose-stone max-w-none prose-a:text-brand-primary placeholder-content">
                <p className="lead text-xl text-stone-600 mb-8">
                    {t('legal.privacyDesc', 'Як ми збираємо та використовуємо ваші дані.')}
                </p>
                
                <section className="mb-8">
                    <h2 className="text-2xl font-bold mb-4">1. Збір та використання інформації</h2>
                    <p>Ми збираємо різні типи інформації з різною метою, щоб надавати та покращувати наші послуги для вас.</p>
                    <p className="mt-4 text-stone-500 italic">[Тут має бути детальний юридичний текст про те, які дані ви збираєте: email, імена, куки (cookies), дані аналітики тощо.]</p>
                </section>

                <section className="mb-8">
                    <h2 className="text-2xl font-bold mb-4">2. Захист даних</h2>
                    <p>Безпека ваших даних є надзвичайно важливою для нас. Ми застосовуємо комерційно прийнятні способи захисту ваших особистих даних.</p>
                </section>
                
                <section className="mb-8">
                    <h2 className="text-2xl font-bold mb-4">3. Треті сторони</h2>
                    <p>Ми можемо залучати сторонні компанії для надання наших послуг (наприклад, платіжні системи, аналітика).</p>
                </section>
            </div>
        </div>
    );
}
