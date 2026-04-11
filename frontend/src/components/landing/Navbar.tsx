import { useState, useEffect } from 'react';
import { Heart, Menu, X } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { Link } from 'react-router-dom';

const navLinks = [
    { label: 'Про курс', href: '#about' },
    { label: 'Програма', href: '#program' },
    { label: 'Автор', href: '#author' },
    { label: 'Курси', href: '#courses' },
    { label: 'Відгуки', href: '#reviews' },
];

export default function Navbar() {
    const [scrolled, setScrolled] = useState(false);
    const [mobileOpen, setMobileOpen] = useState(false);

    useEffect(() => {
        const handler = () => setScrolled(window.scrollY > 40);
        window.addEventListener('scroll', handler);
        return () => window.removeEventListener('scroll', handler);
    }, []);

    const scrollTo = (href: string) => {
        setMobileOpen(false);
        const el = document.querySelector(href);
        if (el) el.scrollIntoView({ behavior: 'smooth' });
    };

    return (
        <motion.nav
            initial={{ y: -80 }}
            animate={{ y: 0 }}
            transition={{ duration: 0.6, ease: 'easeOut' }}
            className={`fixed top-0 left-0 right-0 z-50 transition-all duration-500 ${
                scrolled ? 'glass-strong py-3' : 'bg-transparent py-5'
            }`}
        >
            <div className="max-w-7xl mx-auto px-6 flex items-center justify-between">
                <button onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })} className="flex items-center gap-2 group cursor-pointer">
                    <Heart className="w-7 h-7 text-brand-primary fill-brand-primary group-hover:scale-110 transition-transform" />
                    <span className="font-sans text-xl font-semibold tracking-tight text-stone-800">
                        Під<span className="text-brand-primary">Серцем</span>
                    </span>
                </button>
                
                <div className="hidden md:flex items-center gap-8">
                    {navLinks.map((link) => (
                        <button
                            key={link.href}
                            onClick={() => scrollTo(link.href)}
                            className="text-sm font-medium text-stone-500 hover:text-brand-primary transition-colors cursor-pointer"
                        >
                            {link.label}
                        </button>
                    ))}
                    
                    <div className="flex items-center gap-4 ml-4">
                        <Link
                            to="/login"
                            className="text-sm font-semibold text-stone-700 hover:text-brand-primary transition-colors"
                        >
                            Увійти
                        </Link>
                        <button
                            onClick={() => scrollTo('#courses')}
                            className="px-5 py-2.5 rounded-full bg-brand-primary text-white text-sm font-semibold hover:opacity-90 transition-opacity shadow-lg shadow-brand-primary/25 cursor-pointer"
                        >
                            Приєднатися
                        </button>
                    </div>
                </div>
                
                <button
                    className="md:hidden p-2 rounded-xl hover:bg-white/20 transition-colors cursor-pointer"
                    onClick={() => setMobileOpen(!mobileOpen)}
                >
                    {mobileOpen ? <X className="w-6 h-6 text-stone-800" /> : <Menu className="w-6 h-6 text-stone-800" />}
                </button>
            </div>
            
            <AnimatePresence>
                {mobileOpen && (
                    <motion.div
                        initial={{ opacity: 0, height: 0 }}
                        animate={{ opacity: 1, height: 'auto' }}
                        exit={{ opacity: 0, height: 0 }}
                        className="md:hidden glass-strong mt-2 mx-4 rounded-2xl overflow-hidden"
                    >
                        <div className="p-6 flex flex-col gap-4">
                            {navLinks.map((link) => (
                                <button
                                    key={link.href}
                                    onClick={() => scrollTo(link.href)}
                                    className="text-left text-base font-medium text-stone-700 hover:text-brand-primary transition-colors py-2"
                                >
                                    {link.label}
                                </button>
                            ))}
                            
                            <hr className="border-stone-200 my-2" />
                            
                            <Link
                                to="/login"
                                className="text-center py-2 text-base font-semibold text-stone-700 hover:text-brand-primary transition-colors"
                            >
                                Увійти до кабінету
                            </Link>
                            
                            <button
                                onClick={() => scrollTo('#courses')}
                                className="mt-2 px-5 py-3 rounded-full bg-brand-primary text-white text-sm font-semibold w-full"
                            >
                                Приєднатися до курсу
                            </button>
                        </div>
                    </motion.div>
                )}
            </AnimatePresence>
        </motion.nav>
    );
}
