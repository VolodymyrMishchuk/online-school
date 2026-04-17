
import { motion } from 'framer-motion';
import { Stethoscope, Flower2, Baby, Zap, Milk, Building2 } from 'lucide-react';
import { useTranslation } from 'react-i18next';

export default function TopicsSection() {
    const { t } = useTranslation();

    const topics = [
        {
            icon: Stethoscope,
            title: t('landing.topics.topic1Title', 'Фізіологія вагітності та пологів'),
            desc: t('landing.topics.topic1Desc', 'Як працює тіло, гормони та мозок на кожному етапі пологів. Що відбувається після пологів та як відновитись.'),
        },
        {
            icon: Flower2,
            title: t('landing.topics.topic2Title', 'Природні пологи'),
            desc: t('landing.topics.topic2Desc', 'Як запам\'ятати пологи святом та зберегти здоров\'я собі та малюку. КР, VBAC, тазові пологи.'),
        },
        {
            icon: Baby,
            title: t('landing.topics.topic3Title', 'Малюк'),
            desc: t('landing.topics.topic3Desc', 'Як розуміти свого малюка, задовольняти його базові потреби та організувати побут.'),
        },
        {
            icon: Zap,
            title: t('landing.topics.topic4Title', 'Біль в пологах'),
            desc: t('landing.topics.topic4Desc', 'Чому болить та дієві немедикаментозні способи знеболення. Масаж, дихання та розслаблення.'),
        },
        {
            icon: Milk,
            title: t('landing.topics.topic5Title', 'Грудне вигодовування'),
            desc: t('landing.topics.topic5Desc', 'Як влаштована лактація. Як годувати груддю легко та впоратись з труднощами.'),
        },
        {
            icon: Building2,
            title: t('landing.topics.topic6Title', 'Пологовий будинок'),
            desc: t('landing.topics.topic6Desc', 'Ваші права та обов\'язки медперсоналу. Як вести діалог з лікарем. Медичні процедури.'),
        },
    ];

const container = {
    hidden: {},
    show: { transition: { staggerChildren: 0.1 } },
};

const item = {
    hidden: { opacity: 0, y: 30 },
    show: { opacity: 1, y: 0, transition: { duration: 0.5 } },
};


    return (
        <section className="py-24 lg:py-32 relative bg-gradient-to-b from-brand-secondary/20 to-transparent text-stone-800">
            <div className="max-w-7xl mx-auto px-6">
                <motion.div
                    initial={{ opacity: 0, y: 20 }}
                    whileInView={{ opacity: 1, y: 0 }}
                    viewport={{ once: true }}
                    transition={{ duration: 0.6 }}
                    className="text-center mb-16"
                >
                    <span className="inline-block text-brand-primary font-semibold text-sm tracking-wider uppercase mb-3">
                        {t('landing.topics.tag', 'Програма')}
                    </span>
                    <h2 className="font-sans text-3xl sm:text-4xl lg:text-5xl font-bold">
                        {t('landing.topics.title', 'Що ви дізнаєтесь?')}
                    </h2>
                </motion.div>
                
                <motion.div
                    variants={container}
                    initial="hidden"
                    whileInView="show"
                    viewport={{ once: true }}
                    className="grid sm:grid-cols-2 lg:grid-cols-3 gap-6"
                >
                    {topics.map((topic, i) => (
                        <motion.div
                            key={i}
                            variants={item}
                            className="glass-strong rounded-3xl p-8 hover:shadow-2xl hover:-translate-y-1 transition-all duration-500 group"
                        >
                            <div className="w-12 h-12 rounded-2xl bg-gradient-to-br from-brand-primary/20 to-brand-secondary/30 flex items-center justify-center mb-5 group-hover:scale-110 transition-transform">
                                <topic.icon className="w-6 h-6 text-brand-primary" />
                            </div>
                            <h3 className="font-sans text-lg font-semibold mb-3">{topic.title}</h3>
                            <p className="text-stone-500 text-sm leading-relaxed">{topic.desc}</p>
                        </motion.div>
                    ))}
                </motion.div>
            </div>
        </section>
    );
}
