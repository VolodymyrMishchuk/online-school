
import { Heart } from 'lucide-react';

const socials = [
    { label: 'Facebook', url: 'https://www.facebook.com/pages/category/Personal-Blog/%D0%94%D0%BE%D1%83%D0%BB%D0%B0-%D0%9A%D0%B0%D1%82%D1%8F-%D0%9C%D1%96%D1%89%D1%83%D0%BA-264724194394249/' },
    { label: 'Instagram', url: 'https://www.instagram.com/pid_sercem/' },
    { label: 'YouTube', url: 'https://www.youtube.com/channel/UCIzNCzch7YyTwUm59I-1C5Q' },
    { label: 'Telegram', url: 'https://www.t.me/Katya_Mischuk' },
];

export default function Footer() {
    return (
        <footer className="py-12 border-t border-stone-200/50 bg-white/50">
            <div className="max-w-7xl mx-auto px-6">
                <div className="flex flex-col md:flex-row items-center justify-between gap-6">
                    <div className="flex items-center gap-2">
                        <Heart className="w-5 h-5 text-brand-primary fill-brand-primary" />
                        <span className="font-sans text-lg font-semibold text-stone-800">
                            Під<span className="text-brand-primary">Серцем</span>
                        </span>
                    </div>
                    
                    <div className="flex items-center gap-6">
                        {socials.map((s) => (
                            <a
                                key={s.label}
                                href={s.url}
                                target="_blank"
                                rel="noopener noreferrer"
                                className="text-sm text-stone-500 hover:text-brand-primary transition-colors cursor-pointer"
                            >
                                {s.label}
                            </a>
                        ))}
                    </div>
                    
                    <p className="text-xs text-stone-500">
                        © {new Date().getFullYear()} ПідСерцем. Всі права захищені.
                    </p>
                </div>
            </div>
        </footer>
    );
}
