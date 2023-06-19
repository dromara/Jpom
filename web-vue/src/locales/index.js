import VueI18n from "vue-i18n";
import zh from "./zh";
import en from "./en";

import store from "@/store";
import Vue from "vue";

Vue.use(VueI18n);

const i18n = new VueI18n({
  legacy: false,
  globalInjection: true,
  locale: store.getters.getLocale,
  fallbackLocale: "zh",
  messages: {
    zh: zh,
    en: en,
  },
});

export default i18n;
