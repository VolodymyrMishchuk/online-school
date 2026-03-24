import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { ChevronDown } from 'lucide-react';

const modules = [
    {
        title: 'Модуль 1. Вагітність',
        lessons: [
            'Я вагітна – як правильно діяти? Аналізи, дослідження',
            'Сюрпризи вагітності. До чого потрібно бути готовою?',
        ],
    },
    {
        title: 'Модуль 2. Підготовка до пологів',
        lessons: [
            'Розпізнаємо предвісники пологів',
            'Визначаємо дату пологів. Як діяти, коли розпочалися пологи',
            'Коли їхати до пологового будинку? Моделюємо стрімкі пологи',
            'План пологів. Вибираємо свою команду',
        ],
    },
    {
        title: 'Модуль 3. Фізіологія пологів',
        lessons: [
            'Як розпізнати перейми? Родові та тренувальні?',
            'Потуги. Поза для пологів. Правильно зустрічаємо малюка',
            'Пуповина. Народження плаценти. Гормони пологів',
            'Права жінки. Медичні маніпуляції',
        ],
    },
    {
        title: 'Модуль 4. Відчуття під час пологів',
        lessons: [
            'Природні відчуття — це НЕ біль',
            'Замкнене коло страх-біль-напруга',
            'Масаж, дихання та інші способи полегшення',
            'Вчимося розслаблятися',
        ],
    },
    {
        title: 'Модуль 5. Мій малюк',
        lessons: [
            'Золота година. Матриці Грофа. Медичні маніпуляції',
            'Перехідні стани малюка. Що може турбувати маму',
            'Психологічні потреби дитини. Четвертий триместр',
            'Сон, купання, коліки, гуляння...',
        ],
    },
    {
        title: 'Модуль 6. Грудне вигодовування',
        lessons: [
            'Переваги грудного вигодовування',
            'Перше прикладання. Анатомія молочної залози',
            'Три кити ГВ: годування на вимогу, нічні годування, правильне прикладання',
            'Можливі труднощі: тріщини, лактостаз, мало молока',
        ],
    },
    {
        title: 'Модуль 7. Після пологів',
        lessons: [
            'Ранній післяпологовий період. Шви та лохії',
            'Психологія новонародженої матері. Бебі блюз',
            'Післяпологове відновлення тіла',
        ],
    },
    {
        title: 'Модуль 8. Особливі випадки',
        lessons: [
            'Кесарів розтин: екстрений та плановий. Стимуляція пологів',
            'Тазові пологи, двійнята, вагінальні після КС',
            'Недоношені діти. Як допомогти адаптуватися',
        ],
    },
];

function ModuleItem({ module, index, isOpen, toggle }: any) {
    return (
        <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ duration: 0.4, delay: index * 0.05 }}
            className="glass-strong rounded-2xl overflow-hidden"
        >
            <button
                onClick={toggle}
                className="w-full flex items-center justify-between px-6 py-5 text-left hover:bg-white/30 transition-colors cursor-pointer"
            >
                <div className="flex items-center gap-4">
                    <span className="w-8 h-8 rounded-lg bg-brand-primary/10 flex items-center justify-center text-sm font-bold text-brand-primary">
                        {index + 1}
                    </span>
                    <span className="font-semibold text-sm sm:text-base text-stone-800">{module.title}</span>
                </div>
                <ChevronDown className={`w-5 h-5 text-stone-500 transition-transform duration-300 ${isOpen ? 'rotate-180' : ''}`} />
            </button>
            <AnimatePresence>
                {isOpen && (
                    <motion.div
                        initial={{ height: 0, opacity: 0 }}
                        animate={{ height: 'auto', opacity: 1 }}
                        exit={{ height: 0, opacity: 0 }}
                        transition={{ duration: 0.3 }}
                        className="overflow-hidden"
                    >
                        <div className="px-6 pb-5 pt-1">
                            <ul className="space-y-3">
                                {module.lessons.map((lesson: string, j: number) => (
                                    <li key={j} className="flex items-start gap-3 text-sm text-stone-500">
                                        <span className="w-1.5 h-1.5 rounded-full bg-brand-primary mt-2 flex-shrink-0" />
                                        {lesson}
                                    </li>
                                ))}
                            </ul>
                        </div>
                    </motion.div>
                )}
            </AnimatePresence>
        </motion.div>
    );
}

export default function ProgramSection() {
    const [openIndex, setOpenIndex] = useState(0);
    return (
        <section id="program" className="py-24 lg:py-32">
            <div className="max-w-3xl mx-auto px-6">
                <motion.div
                    initial={{ opacity: 0, y: 20 }}
                    whileInView={{ opacity: 1, y: 0 }}
                    viewport={{ once: true }}
                    transition={{ duration: 0.6 }}
                    className="text-center mb-16"
                >
                    <span className="inline-block text-brand-primary font-semibold text-sm tracking-wider uppercase mb-3">
                        Навчання
                    </span>
                    <h2 className="font-sans text-3xl sm:text-4xl lg:text-5xl font-bold text-stone-800">
                        Програма уроків
                    </h2>
                    <p className="mt-4 text-stone-500 max-w-lg mx-auto">
                        8 модулів, що охоплюють все — від вагітності до післяпологового відновлення
                    </p>
                </motion.div>
                
                <div className="space-y-3">
                    {modules.map((mod, i) => (
                        <ModuleItem
                            key={i}
                            module={mod}
                            index={i}
                            isOpen={openIndex === i}
                            toggle={() => setOpenIndex(openIndex === i ? -1 : i)}
                        />
                    ))}
                </div>
            </div>
        </section>
    );
}
