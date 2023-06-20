import { createI18n } from 'vue-i18n';
import zh from './zh';
import en from './en';

import { LOCALE_KEY } from '@/utils/const'

const i18n = createI18n({
  globalInjection: true,
  legacy: false,
  locale: localStorage.getItem(LOCALE_KEY) || 'zh',
  fallbackLocale: 'zh',
  messages: {
    zh: zh,
    en: en,
  }
});

export type LocaleType = 'zh' | 'en';

export default i18n;
