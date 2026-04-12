import { Heart } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';

const socials = [
    { label: 'Facebook', url: 'https://www.facebook.com/pages/category/Personal-Blog/%D0%94%D0%BE%D1%83%D0%BB%D0%B0-%D0%9A%D0%B0%D1%82%D1%8F-%D0%9C%D1%96%D1%89%D1%83%D0%BA-264724194394249/' },
    { label: 'Instagram', url: 'https://www.instagram.com/pid_sercem/' },
    { label: 'YouTube', url: 'https://www.youtube.com/channel/UCIzNCzch7YyTwUm59I-1C5Q' },
    { label: 'Telegram', url: 'https://www.t.me/Katya_Mischuk' },
];

export default function Footer() {
    const { t } = useTranslation();

    return (
        <footer className="py-12 border-t border-stone-200/50 bg-white/50">
            <div className="max-w-7xl mx-auto px-6">
                <div className="flex flex-col md:flex-row items-center justify-between gap-6 mb-8 mt-4">
                    <Link to="/" className="flex items-center gap-2">
                        <Heart className="w-5 h-5 text-brand-primary fill-brand-primary" />
                        <span className="font-sans text-lg font-semibold text-stone-800">
                            Під<span className="text-brand-primary">Серцем</span>
                        </span>
                    </Link>
                    
                    <div className="flex items-center gap-6">
                        {socials.map((s) => (
                            <a
                                key={s.label}
                                href={s.url}
                                target="_blank"
                                rel="noopener noreferrer"
                                className="text-sm font-medium text-stone-500 hover:text-brand-primary transition-colors cursor-pointer"
                            >
                                {s.label}
                            </a>
                        ))}
                    </div>
                </div>

                <div className="flex flex-col md:flex-row items-center justify-between gap-6 pt-6 border-t border-stone-200/50">
                    <div className="flex flex-wrap justify-center gap-x-6 gap-y-2">
                        <Link to="/impressum" className="text-sm text-stone-500 hover:text-brand-primary transition-colors">
                            {t('footer.impressum', 'Імпресум')}
                        </Link>
                        <Link to="/privacy" className="text-sm text-stone-500 hover:text-brand-primary transition-colors">
                            {t('footer.privacy', 'Політика конфіденційності')}
                        </Link>
                        <Link to="/terms" className="text-sm text-stone-500 hover:text-brand-primary transition-colors">
                            {t('footer.terms', 'Публічна оферта')}
                        </Link>
                    </div>
                    
                    <p className="text-xs text-stone-400">
                        © {new Date().getFullYear()} ПідСерцем. {t('footer.rights', 'Всі права захищені.')}
                    </p>
                </div>
            </div>
        </footer>
    );
}
