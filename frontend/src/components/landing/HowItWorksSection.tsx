
import { motion } from 'framer-motion';
import { MonitorPlay, MessageCircle, BookOpen, FileText, Video } from 'lucide-react';

const features = [
    {
        icon: MonitorPlay,
        title: 'Лекції в особистому кабінеті',
        desc: 'Всі модулі курсу доступні одразу. Доступ приходить в день оплати на пошту.',
    },
    {
        icon: MessageCircle,
        title: 'Закритий чат в Telegram',
        desc: 'Для підтримки, спілкування та відповідей на питання.',
    },
    {
        icon: BookOpen,
        title: 'Домашні завдання',
        desc: 'Після кожного модуля домашні завдання та тести.',
    },
    {
        icon: FileText,
        title: 'Додаткові матеріали',
        desc: 'Документи, відео та протоколи для закріплення знань.',
    },
    {
        icon: Video,
        title: 'Zoom-зустрічі',
        desc: 'Щотижневі онлайн зустрічі Питання/Відповідь + запис.',
    },
];

const container = {
    hidden: {},
    show: { transition: { staggerChildren: 0.1 } },
};

const item = {
    hidden: { opacity: 0, y: 20 },
    show: { opacity: 1, y: 0, transition: { duration: 0.5 } },
};

export default function HowItWorksSection() {
    return (
        <section className="py-24 lg:py-32 bg-gradient-to-b from-transparent via-brand-secondary/15 to-transparent text-stone-800">
            <div className="max-w-7xl mx-auto px-6">
                <motion.div
                    initial={{ opacity: 0, y: 20 }}
                    whileInView={{ opacity: 1, y: 0 }}
                    viewport={{ once: true }}
                    transition={{ duration: 0.6 }}
                    className="text-center mb-16"
                >
                    <span className="inline-block text-brand-primary font-semibold text-sm tracking-wider uppercase mb-3">
                        Формат
                    </span>
                    <h2 className="font-sans text-3xl sm:text-4xl lg:text-5xl font-bold">
                        Як це працює?
                    </h2>
                </motion.div>
                
                <motion.div
                    variants={container}
                    initial="hidden"
                    whileInView="show"
                    viewport={{ once: true }}
                    className="grid sm:grid-cols-2 lg:grid-cols-5 gap-6"
                >
                    {features.map((f, i) => (
                        <motion.div
                            key={i}
                            variants={item}
                            className="glass-strong rounded-3xl p-6 text-center hover:shadow-2xl hover:-translate-y-1 transition-all duration-500 group"
                        >
                            <div className="w-14 h-14 rounded-2xl bg-gradient-to-br from-brand-primary/15 to-brand-secondary/25 flex items-center justify-center mx-auto mb-5 group-hover:scale-110 transition-transform">
                                <f.icon className="w-7 h-7 text-brand-primary" />
                            </div>
                            <h3 className="font-semibold text-sm mb-2">{f.title}</h3>
                            <p className="text-xs text-stone-500 leading-relaxed">{f.desc}</p>
                        </motion.div>
                    ))}
                </motion.div>
            </div>
        </section>
    );
}
