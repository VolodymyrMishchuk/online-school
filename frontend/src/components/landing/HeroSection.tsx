
import { motion } from 'framer-motion';
import { ArrowDown, Play } from 'lucide-react';

const HERO_IMG = 'https://media.base44.com/images/public/69c007c8e122ca819f9b1851/ac8417c63_generated_c6acbbd3.png';

export default function HeroSection() {
    const scrollTo = (href: string) => {
        const el = document.querySelector(href);
        if (el) el.scrollIntoView({ behavior: 'smooth' });
    };

    return (
        <section id="hero" className="relative min-h-screen flex items-center overflow-hidden">
            {/* Background gradient */}
            <div className="absolute inset-0 bg-gradient-to-br from-brand-secondary/40 via-brand-light to-brand-secondary/50" />

            {/* Floating decorative elements */}
            <motion.div
                animate={{ y: [0, -20, 0], rotate: [0, 5, 0] }}
                transition={{ duration: 8, repeat: Infinity, ease: 'easeInOut' }}
                className="absolute top-32 left-10 w-32 h-32 rounded-full bg-brand-primary/10 blur-2xl"
            />
            <motion.div
                animate={{ y: [0, 15, 0], rotate: [0, -3, 0] }}
                transition={{ duration: 10, repeat: Infinity, ease: 'easeInOut' }}
                className="absolute bottom-40 right-20 w-48 h-48 rounded-full bg-brand-secondary/30 blur-3xl"
            />
            
            <div className="relative z-10 max-w-7xl mx-auto px-6 pt-28 pb-20 w-full">
                <div className="grid lg:grid-cols-2 gap-12 lg:gap-8 items-center">
                    {/* Text */}
                    <motion.div
                        initial={{ opacity: 0, x: -40 }}
                        animate={{ opacity: 1, x: 0 }}
                        transition={{ duration: 0.8, delay: 0.2 }}
                        className="order-2 lg:order-1"
                    >
                        <div className="inline-flex items-center gap-2 glass-panel rounded-full px-4 py-2 mb-8">
                            <span className="w-2 h-2 rounded-full bg-brand-primary animate-pulse" />
                            <span className="text-sm font-medium text-stone-500">Онлайн-курс для майбутніх мам</span>
                        </div>
                        <h1 className="font-sans text-4xl sm:text-5xl lg:text-6xl font-bold leading-tight mb-6 text-stone-800">
                            Підготовка до{' '}
                            <span className="text-brand-primary italic">здорових</span>{' '}
                            пологів та щасливого материнства
                        </h1>
                        <p className="text-lg text-stone-500 leading-relaxed mb-10 max-w-lg">
                            Народження та материнство без болю, страху та сумнівів. Більше 1000 жінок вже пройшли цей шлях разом з нами.
                        </p>
                        <div className="flex flex-wrap gap-4">
                            <button
                                onClick={() => scrollTo('#pricing')}
                                className="px-8 py-4 rounded-full bg-brand-primary text-white font-semibold text-base hover:opacity-90 transition-all shadow-xl shadow-brand-primary/30 hover:shadow-2xl hover:shadow-brand-primary/40 hover:-translate-y-0.5 cursor-pointer"
                            >
                                Приєднатися до курсу
                            </button>
                            <button
                                onClick={() => scrollTo('#about')}
                                className="px-8 py-4 rounded-full glass-panel font-semibold text-base text-stone-800 hover:bg-white/50 transition-all flex items-center gap-2 group cursor-pointer"
                            >
                                <Play className="w-4 h-4 group-hover:scale-110 transition-transform" />
                                Дізнатися більше
                            </button>
                        </div>
                    </motion.div>
                    
                    {/* Image */}
                    <motion.div
                        initial={{ opacity: 0, x: 40 }}
                        animate={{ opacity: 1, x: 0 }}
                        transition={{ duration: 0.8, delay: 0.4 }}
                        className="order-1 lg:order-2 flex justify-center"
                    >
                        <div className="relative">
                            <div className="absolute -inset-4 rounded-3xl bg-gradient-to-br from-brand-primary/20 to-brand-secondary/30 blur-2xl" />
                            <img
                                src={HERO_IMG}
                                alt="Щаслива вагітна жінка"
                                className="relative rounded-3xl w-full max-w-md lg:max-w-lg object-cover shadow-2xl"
                            />
                            {/* Glass stat card */}
                            <motion.div
                                initial={{ opacity: 0, scale: 0.8 }}
                                animate={{ opacity: 1, scale: 1 }}
                                transition={{ delay: 1, duration: 0.5 }}
                                className="absolute -bottom-6 -left-6 glass-strong rounded-2xl px-6 py-4"
                            >
                                <p className="text-3xl font-bold text-brand-primary">1000+</p>
                                <p className="text-sm text-stone-500">щасливих мам</p>
                            </motion.div>
                        </div>
                    </motion.div>
                </div>
                
                {/* Scroll indicator */}
                <motion.div
                    animate={{ y: [0, 8, 0] }}
                    transition={{ duration: 2, repeat: Infinity }}
                    className="hidden lg:flex justify-center mt-16"
                >
                    <button onClick={() => scrollTo('#about')} className="p-3 rounded-full glass-panel hover:bg-white/40 transition-colors cursor-pointer">
                        <ArrowDown className="w-5 h-5 text-stone-500" />
                    </button>
                </motion.div>
            </div>
        </section>
    );
}
