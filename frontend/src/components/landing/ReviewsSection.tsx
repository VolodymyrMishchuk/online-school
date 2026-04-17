
import { motion } from 'framer-motion';
import { Star, Quote } from 'lucide-react';
import { useTranslation } from 'react-i18next';

const container = {
    hidden: {},
    show: { transition: { staggerChildren: 0.12 } },
};

const item = {
    hidden: { opacity: 0, y: 30 },
    show: { opacity: 1, y: 0, transition: { duration: 0.5 } },
};


export default function ReviewsSection() {
    const { t } = useTranslation();
    const rawReviews = t('landing.reviews.items', { returnObjects: true });
    const reviews: any[] = Array.isArray(rawReviews) ? rawReviews : [];
    return (
        <section id="reviews" className="py-24 lg:py-32 bg-gradient-to-b from-transparent via-brand-secondary/10 to-transparent">
            <div className="max-w-7xl mx-auto px-6">
                <motion.div
                    initial={{ opacity: 0, y: 20 }}
                    whileInView={{ opacity: 1, y: 0 }}
                    viewport={{ once: true }}
                    transition={{ duration: 0.6 }}
                    className="text-center mb-16"
                >
                    <span className="inline-block text-brand-primary font-semibold text-sm tracking-wider uppercase mb-3">
                        {t('landing.reviews.tag', 'Відгуки')}
                    </span>
                    <h2 className="font-sans text-3xl sm:text-4xl lg:text-5xl font-bold text-stone-800">
                        {t('landing.reviews.title', 'Що говорять учасниці')}
                    </h2>
                </motion.div>
                
                <motion.div
                    variants={container}
                    initial="hidden"
                    whileInView="show"
                    viewport={{ once: true }}
                    className="grid sm:grid-cols-2 lg:grid-cols-4 gap-6"
                >
                    {reviews.map((review, i) => (
                        <motion.div
                            key={i}
                            variants={item}
                            className="glass-strong rounded-3xl p-7 hover:shadow-2xl transition-all duration-500 flex flex-col"
                        >
                            <Quote className="w-8 h-8 text-brand-primary/30 mb-4" />
                            <p className="text-sm text-stone-500 leading-relaxed flex-1 mb-5">
                                "{review.text}"
                            </p>
                            <div className="flex items-center justify-between">
                                <span className="font-semibold text-sm text-stone-800">{review.name}</span>
                                <div className="flex gap-0.5">
                                    {Array.from({ length: review.rating || 5 }).map((_, j) => (
                                        <Star key={j} className="w-3.5 h-3.5 text-brand-primary fill-brand-primary" />
                                    ))}
                                </div>
                            </div>
                        </motion.div>
                    ))}
                </motion.div>
            </div>
        </section>
    );
}
