
import { motion } from 'framer-motion';
import { Check, Star, Users } from 'lucide-react';

const plans = [
    {
        name: 'Під Серцем',
        price: '3 000',
        badge: null,
        icon: Star,
        features: [
            '8 модулів навчання',
            '3 місяці доступу',
            '12 онлайн-сесій',
            'Домашні завдання',
            'Додаткові матеріали до кожного модуля',
            'Закритий Telegram чат',
        ],
        cta: 'Приєднатися',
        highlighted: false,
    },
    {
        name: 'Супер-БАТЬКИ',
        price: '3 500',
        oldPrice: '4 000',
        badge: 'Популярний',
        icon: Users,
        features: [
            '8 модулів навчання',
            '3 місяці доступу',
            '12 онлайн-сесій',
            'Домашні завдання',
            'Додаткові матеріали до кожного модуля',
            'Закритий Telegram чат',
            'Курс СУПЕР-ТАТО в подарунок',
        ],
        cta: 'Обрати пакет',
        highlighted: true,
    },
];

export default function PricingSection() {
    return (
        <section id="pricing" className="py-24 lg:py-32 relative text-stone-800">
            <div className="absolute top-20 left-10 w-64 h-64 bg-brand-primary/5 rounded-full blur-3xl" />
            <div className="absolute bottom-20 right-10 w-80 h-80 bg-brand-secondary/10 rounded-full blur-3xl" />
            
            <div className="max-w-5xl mx-auto px-6 relative z-10">
                <motion.div
                    initial={{ opacity: 0, y: 20 }}
                    whileInView={{ opacity: 1, y: 0 }}
                    viewport={{ once: true }}
                    transition={{ duration: 0.6 }}
                    className="text-center mb-16"
                >
                    <span className="inline-block text-brand-primary font-semibold text-sm tracking-wider uppercase mb-3">
                        Тарифи
                    </span>
                    <h2 className="font-sans text-3xl sm:text-4xl lg:text-5xl font-bold">
                        Форми навчання
                    </h2>
                    <p className="mt-4 text-stone-500 max-w-lg mx-auto">
                        Оберіть зручний формат та почніть підготовку до найважливішої події у вашому житті
                    </p>
                </motion.div>
                
                <div className="grid md:grid-cols-2 gap-8 max-w-4xl mx-auto">
                    {plans.map((plan, i) => (
                        <motion.div
                            key={i}
                            initial={{ opacity: 0, y: 30 }}
                            whileInView={{ opacity: 1, y: 0 }}
                            viewport={{ once: true }}
                            transition={{ duration: 0.6, delay: i * 0.15 }}
                            className={`relative rounded-3xl p-8 lg:p-10 transition-all duration-500 ${
                                plan.highlighted
                                    ? 'bg-gradient-to-br from-brand-primary/10 to-brand-secondary/20 glass-strong ring-2 ring-brand-primary/30 shadow-2xl'
                                    : 'glass-strong hover:shadow-2xl'
                            }`}
                        >
                            {plan.badge && (
                                <div className="absolute -top-3 left-1/2 -translate-x-1/2">
                                    <span className="px-4 py-1.5 rounded-full bg-brand-primary text-white text-xs font-bold shadow-lg">
                                        {plan.badge}
                                    </span>
                                </div>
                            )}
                            
                            <div className="text-center mb-8">
                                <div className="w-14 h-14 rounded-2xl bg-brand-primary/10 flex items-center justify-center mx-auto mb-4">
                                    <plan.icon className="w-7 h-7 text-brand-primary" />
                                </div>
                                <h3 className="font-sans text-2xl font-bold mb-4">{plan.name}</h3>
                                <div className="flex items-baseline justify-center gap-2">
                                    {plan.oldPrice && (
                                        <span className="text-lg text-stone-500 line-through">{plan.oldPrice} грн</span>
                                    )}
                                    <span className="text-4xl font-bold text-brand-primary">{plan.price}</span>
                                    <span className="text-stone-500">грн</span>
                                </div>
                            </div>
                            
                            <ul className="space-y-4 mb-8">
                                {plan.features.map((feature, j) => (
                                    <li key={j} className="flex items-start gap-3">
                                        <div className="w-5 h-5 rounded-full bg-brand-primary/10 flex items-center justify-center flex-shrink-0 mt-0.5">
                                            <Check className="w-3 h-3 text-brand-primary" />
                                        </div>
                                        <span className="text-sm text-stone-800/80">{feature}</span>
                                    </li>
                                ))}
                            </ul>
                            
                            <button
                                className={`w-full py-4 rounded-full font-semibold text-base transition-all cursor-pointer ${
                                    plan.highlighted
                                        ? 'bg-brand-primary text-white shadow-xl shadow-brand-primary/30 hover:shadow-2xl hover:opacity-90'
                                        : 'glass-panel hover:bg-white/50 text-stone-800'
                                }`}
                            >
                                {plan.cta}
                            </button>
                        </motion.div>
                    ))}
                </div>
            </div>
        </section>
    );
}
