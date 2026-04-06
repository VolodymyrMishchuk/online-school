import { useState, useRef, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { Check } from 'lucide-react';
import { updateLanguage } from '../api/persons';

const LANGUAGES = [
    { code: 'uk', label: 'Українська', flag: '🇺🇦', flagUrl: 'https://flagcdn.com/ua.svg' },
    { code: 'en', label: 'English', flag: '🇬🇧', flagUrl: 'https://flagcdn.com/gb.svg' },
    { code: 'de', label: 'Deutsch', flag: '🇩🇪', flagUrl: 'https://flagcdn.com/de.svg' },
];

export function LanguageSwitcher() {
    const { i18n } = useTranslation();
    const [isOpen, setIsOpen] = useState(false);
    const dropdownRef = useRef<HTMLDivElement>(null);

    const currentLanguage = LANGUAGES.find(lang => lang.code === i18n.language) || LANGUAGES[0];

    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
                setIsOpen(false);
            }
        };

        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    const changeLanguage = async (lng: string) => {
        i18n.changeLanguage(lng);
        setIsOpen(false);

        const userId = localStorage.getItem('userId');
        if (userId) {
            try {
                await updateLanguage(userId, lng);

                // Also update local storage user object
                const userStr = localStorage.getItem('user');
                if (userStr) {
                    const user = JSON.parse(userStr);
                    user.language = lng;
                    localStorage.setItem('user', JSON.stringify(user));
                }
            } catch (error) {
                console.error('Failed to sync language to backend:', error);
            }
        }
    };

    return (
        <div className="relative flex items-center h-full" ref={dropdownRef}>
            <button
                onClick={() => setIsOpen(!isOpen)}
                className="flex items-center justify-center w-14 h-8 bg-white/40 hover:bg-white/60 transition-colors rounded-tr-3xl rounded-bl-[1.5rem] shadow-sm relative overflow-hidden"
                title="Change language"
            >
                <span className="text-xl leading-none">{currentLanguage.flag}</span>
            </button>

            {isOpen && (
                <div className="absolute top-[2.5rem] right-0 w-48 bg-white/90 backdrop-blur-md border border-gray-100 rounded-xl shadow-lg shadow-gray-200/50 py-2 z-50 animate-in fade-in slide-in-from-top-2 duration-200">
                    {LANGUAGES.map((lang) => (
                        <button
                            key={lang.code}
                            onClick={() => changeLanguage(lang.code)}
                            className={`w-full px-4 py-2 flex items-center justify-between hover:bg-gray-50 transition-colors ${i18n.language === lang.code ? 'bg-brand-light/10 text-brand-primary' : 'text-gray-700'
                                }`}
                        >
                            <span className="flex items-center gap-2">
                                <span className="text-lg">{lang.flag}</span>
                                <span className="font-medium">{lang.label}</span>
                            </span>
                            {i18n.language === lang.code && <Check className="w-4 h-4" />}
                        </button>
                    ))}
                </div>
            )}
        </div>
    );
}
