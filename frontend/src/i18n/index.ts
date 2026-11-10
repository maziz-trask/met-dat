import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import cs from './locales/cs';
import en from './locales/en';

export type Language = 'en' | 'cs';
const KEY = 'metdat.lang';

const read = (): Language | null => {
  try { const v = localStorage.getItem(KEY); return v === 'cs' || v === 'en' ? v : null; } catch { return null; }
};
const detect = (): Language => (typeof navigator !== 'undefined' && navigator.language?.toLowerCase().startsWith('cs') ? 'cs' : 'en');

i18n.use(initReactI18next).init({
  resources: { en: { translation: en }, cs: { translation: cs } },
  lng: read() ?? detect(),
  fallbackLng: 'en',
  interpolation: { escapeValue: false },   // React already escapes
});

if (typeof document !== 'undefined') document.documentElement.lang = i18n.language;

export function setLanguage(lang: Language): void {
  void i18n.changeLanguage(lang);
  try { localStorage.setItem(KEY, lang); } catch { /* storage unavailable */ }
  if (typeof document !== 'undefined') document.documentElement.lang = lang;
}

/** BCP-47 locale used for dates and numbers. */
export const currentLocale = (): string => (i18n.language === 'cs' ? 'cs-CZ' : 'en-GB');

export default i18n;
