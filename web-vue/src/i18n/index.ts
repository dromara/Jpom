import { createI18n } from 'vue-i18n'
import zhCN from './locales/zh-CN'
import enUS from './locales/en_US'

/** 以中文为主,定义语言类型 */
export type I18nLocaleType = typeof zhCN

const i18n = createI18n({
  legacy: false,
  // TODO 目前手动修改，后续抽离封装
  locale: 'zh-cn', //'en-us', // 默认显示语言
  messages: {
    'zh-cn': zhCN,
    'en-us': enUS
  }
})

export default i18n
