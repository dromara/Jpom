///
/// Copyright (c) 2019 Of Him Code Technology Studio
/// Jpom is licensed under Mulan PSL v2.
/// You can use this software according to the terms and conditions of the Mulan PSL v2.
/// You may obtain a copy of Mulan PSL v2 at:
/// 			http://license.coscl.org.cn/MulanPSL2
/// THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
/// See the Mulan PSL v2 for more details.
///

import { createI18n } from 'vue-i18n'

type LangType = {
  antd: () => Promise<any>
  local: () => Promise<any>
}

export const langDict: { [key: string]: LangType } = {
  'zh-cn': {
    antd: () => import(/* @vite-ignore  */ 'ant-design-vue/es/locale/zh_CN'),
    local: () => import(/* @vite-ignore  */ './locales/zh_cn.json')
  },
  'en-us': {
    antd: () => import(/* @vite-ignore  */ 'ant-design-vue/es/locale/en_US'),
    local: () => import(/* @vite-ignore  */ './locales/en_us.json')
  }
}
export const defaultLocale = 'zh-cn'

const i18n = createI18n<Record<string, any>>({
  legacy: false,
  locale: defaultLocale, // 默认显示语言
  fallbackLocale: defaultLocale, // 默认显示语言
  warnHtmlMessage: false
})

export const changeLang = async (langKey: string) => {
  langKey = langKey.toLowerCase()
  const lang = langDict[langKey || defaultLocale]
  await loadLanguageAsync(langKey, lang)
  return await lang.antd()
}

export const setI18nLanguage = (langKey: string) => {
  // @ts-ignore
  i18n.global.locale = langKey
  return langKey
}
export const loadLanguageAsync = async (langKey: string, langDict: LangType) => {
  const langFile = await langDict.local()
  // 动态加载对应的语言包
  i18n.global.setLocaleMessage(langKey, langFile)
  return setI18nLanguage(langKey) // 返回并且设置
}

export default i18n

export const { t } = i18n.global

export const supportLang = [
  {
    label: '\u7b80\u4f53\u4e2d\u6587',
    value: 'zh-cn'
  },
  {
    label: '\u7e41\u4f53\u4e2d\u6587\uff08\u4e2d\u56fd\u9999\u6e2f\uff09',
    value: 'zh-hk'
  },
  {
    label: '\u7e41\u4f53\u4e2d\u6587\uff08\u4e2d\u56fd\u53f0\u6e7e\uff09',
    value: 'zh-tw'
  },
  {
    label: 'English',
    value: 'en-us'
  }
]

export const supportLangArray = supportLang.map((item) => item.value)
