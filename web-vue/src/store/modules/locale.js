import { LOCALE_KEY } from "@/utils/const";

const locale = {
  state: {
    locale: localStorage.getItem(LOCALE_KEY) || "zh",
  },
  mutations: {
    setLocale(state, locale) {
      state.locale = locale;
      localStorage.setItem(LOCALE_KEY, locale);
    },
  },
  getters: {
    getLocale(state) {
      return state.locale;
    },
  },
};

export default locale;
