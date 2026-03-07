import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import LanguageDetector from 'i18next-browser-languagedetector';

import ukTranslations from './locales/uk/translation.json';
import enTranslations from './locales/en/translation.json';
import deTranslations from './locales/de/translation.json';

const resources = {
    uk: { translation: ukTranslations },
    en: { translation: enTranslations },
    de: { translation: deTranslations },
};

i18n
    .use(LanguageDetector)
    .use(initReactI18next)
    .init({
        resources,
        fallbackLng: 'uk',
        supportedLngs: ['uk', 'en', 'de'],
        interpolation: {
            escapeValue: false,
        },
        detection: {
            order: ['localStorage', 'navigator'],
            caches: ['localStorage'],
        }
    });

export default i18n;
