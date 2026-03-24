
import { motion } from 'framer-motion';
import { Baby, Heart, Sparkles } from 'lucide-react';

const audiences = [
    {
        title: 'Для тих, хто вперше стає мамою',
        icon: Baby,
        points: [
            'Хто хоче впоратись з болем',
            'Хто хоче народжувати без зайвих медичних втручань',
            'Хто бажає знати все необхідне про природні пологи',
            'Хто хоче народжувати без страху',
        ],
    },
    {
        title: 'Для мам з досвідом',
        icon: Heart,
        points: [
            'Хто планує вагінальні пологи після Кесарського розтину',
            'Хто мав негативний досвід і не хоче його повторення',
            'Хто бажає годувати груддю або має сумніви',
            'Хто бажає знати як правильно відновитись після пологів',
        ],
    },
];

const container = {
    hidden: {},
    show: { transition: { staggerChildren: 0.15 } },
};

const item = {
    hidden: { opacity: 0, y: 30 },
    show: { opacity: 1, y: 0, transition: { duration: 0.6 } },
};

export default function ForWhomSection() {
    return (
        <section id="about" className="py-24 lg:py-32 relative text-stone-800">
            <div className="absolute top-0 left-1/2 -translate-x-1/2 w-96 h-96 bg-brand-primary/5 rounded-full blur-3xl" />
            
            <div className="max-w-7xl mx-auto px-6 relative z-10">
                <motion.div
                    initial={{ opacity: 0, y: 20 }}
                    whileInView={{ opacity: 1, y: 0 }}
                    viewport={{ once: true }}
                    transition={{ duration: 0.6 }}
                    className="text-center mb-16"
                >
                    <span className="inline-block text-brand-primary font-semibold text-sm tracking-wider uppercase mb-3">
                        Для кого
                    </span>
                    <h2 className="font-sans text-3xl sm:text-4xl lg:text-5xl font-bold">
                        Для кого курс «Під Серцем»?
                    </h2>
                </motion.div>
                
                <motion.div
                    variants={container}
                    initial="hidden"
                    whileInView="show"
                    viewport={{ once: true }}
                    className="grid md:grid-cols-2 gap-8"
                >
                    {audiences.map((aud, i) => (
                        <motion.div
                            key={i}
                            variants={item}
                            className="glass-strong rounded-3xl p-8 lg:p-10 hover:shadow-2xl transition-shadow duration-500 group"
                        >
                            <div className="w-14 h-14 rounded-2xl bg-brand-primary/10 flex items-center justify-center mb-6 group-hover:bg-brand-primary/20 transition-colors">
                                <aud.icon className="w-7 h-7 text-brand-primary" />
                            </div>
                            <h3 className="font-sans text-xl lg:text-2xl font-semibold mb-6">{aud.title}</h3>
                            <ul className="space-y-4">
                                {aud.points.map((point, j) => (
                                    <li key={j} className="flex items-start gap-3">
                                        <Sparkles className="w-4 h-4 text-brand-primary mt-1 flex-shrink-0" />
                                        <span className="text-stone-600 leading-relaxed font-medium">{point}</span>
                                    </li>
                                ))}
                            </ul>
                        </motion.div>
                    ))}
                </motion.div>
            </div>
        </section>
    );
}
