import { LOCALE_KEY } from '@/utils/const'
import { LocaleType } from '@/locales'

export const useLocaleStore = defineStore('locale', {
  state: () => ( {
    locale: localStorage.getItem(LOCALE_KEY) || 'zh'
  } ),
  actions: {
    changeLocale(locale: LocaleType) {
      this.locale = locale
      localStorage.setItem(LOCALE_KEY, locale)
    }
  },
  getters: {
    getLocale(state): LocaleType {
      return state.locale as LocaleType
    }
  }
})
