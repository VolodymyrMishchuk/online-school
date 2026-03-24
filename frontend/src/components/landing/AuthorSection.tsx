
import { motion } from 'framer-motion';
import { Award, GraduationCap, Heart, Star } from 'lucide-react';

const AUTHOR_IMG = '/images/author-kateryna.png';

const credentials = [
    { icon: Heart, text: 'Доула з медичною освітою' },
    { icon: GraduationCap, text: 'Акушерка' },
    { icon: Award, text: 'Сертифікований спеціаліст з грудного вигодовування' },
    { icon: Star, text: 'Автор програми підготовки до природніх м\'яких пологів' },
];

export default function AuthorSection() {
    return (
        <section id="author" className="py-24 lg:py-32 relative text-stone-800">
            <div className="absolute top-1/2 right-0 w-72 h-72 bg-brand-secondary/20 rounded-full blur-3xl -translate-y-1/2" />
            
            <div className="max-w-7xl mx-auto px-6 relative z-10">
                <div className="grid lg:grid-cols-2 gap-12 lg:gap-16 items-center">
                    {/* Image */}
                    <motion.div
                        initial={{ opacity: 0, x: -30 }}
                        whileInView={{ opacity: 1, x: 0 }}
                        viewport={{ once: true }}
                        transition={{ duration: 0.7 }}
                        className="flex justify-center"
                    >
                        <div className="relative">
                            <div className="absolute -inset-6 rounded-full bg-gradient-to-br from-brand-primary/15 to-brand-secondary/25 blur-2xl" />
                            <img
                                src={AUTHOR_IMG}
                                alt="Катерина Міщук — доула та акушерка"
                                className="relative rounded-3xl w-72 h-72 lg:w-96 lg:h-96 object-cover shadow-2xl"
                            />
                            <motion.div
                                initial={{ opacity: 0, scale: 0.8 }}
                                whileInView={{ opacity: 1, scale: 1 }}
                                viewport={{ once: true }}
                                transition={{ delay: 0.5, duration: 0.5 }}
                                className="absolute -bottom-4 -right-4 glass-strong rounded-2xl px-5 py-3"
                            >
                                <p className="text-2xl font-bold text-brand-primary">8+</p>
                                <p className="text-xs text-stone-500">років досвіду</p>
                            </motion.div>
                        </div>
                    </motion.div>
                    
                    {/* Text */}
                    <motion.div
                        initial={{ opacity: 0, x: 30 }}
                        whileInView={{ opacity: 1, x: 0 }}
                        viewport={{ once: true }}
                        transition={{ duration: 0.7, delay: 0.1 }}
                    >
                        <span className="inline-block text-brand-primary font-semibold text-sm tracking-wider uppercase mb-3">
                            Автор курсу
                        </span>
                        <h2 className="font-sans text-3xl sm:text-4xl lg:text-5xl font-bold mb-6">
                            Катерина Міщук
                        </h2>
                        <p className="text-stone-500 leading-relaxed mb-8 text-lg">
                            Я та, хто знає, як народити без болю та зайвих медичних втручань. Та, хто завжди на твоєму боці.
                            Знає, що тіло жінки ідеально створено для того, щоб народжувати легко та безболісно.
                        </p>
                        
                        <div className="space-y-4">
                            {credentials.map((cred, i) => (
                                <div key={i} className="flex items-center gap-4 glass-panel rounded-2xl px-5 py-4 hover:bg-white/50 transition-colors">
                                    <div className="w-10 h-10 rounded-xl bg-brand-primary/10 flex items-center justify-center flex-shrink-0">
                                        <cred.icon className="w-5 h-5 text-brand-primary" />
                                    </div>
                                    <span className="text-sm font-medium">{cred.text}</span>
                                </div>
                            ))}
                        </div>
                    </motion.div>
                </div>
            </div>
        </section>
    );
}
