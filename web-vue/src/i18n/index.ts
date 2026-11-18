/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
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
import { GlobalWindow } from '@/interface/common'
import dayjs from 'dayjs'

type LangType = {
  label: string
  isLoad?: boolean
  antdLang?: any
  isDayjsLoad?: boolean
  antd: () => Promise<any>
  local: () => Promise<any>
  dayjs: () => Promise<any>
}

export const langDict: { [key: string]: LangType } = {
  'zh-cn': {
    // 🇨🇳
    label: '\u7b80\u4f53\u4e2d\u6587',
    antd: () => import(/* @vite-ignore  */ 'ant-design-vue/es/locale/zh_CN'),
    local: () => import(/* @vite-ignore  */ './locales/zh_cn.json'),
    dayjs: () => import(/* @vite-ignore  */ 'dayjs/locale/zh-cn')
  },
  'zh-hk': {
    // 🇭🇰 繁體中文（中國香港）
    label: '\u7e41\u9ad4\u4e2d\u6587\uff08\u4e2d\u570b\u9999\u6e2f\uff09',
    antd: () => import(/* @vite-ignore  */ 'ant-design-vue/es/locale/zh_HK'),
    local: () => import(/* @vite-ignore  */ './locales/zh_hk.json'),
    dayjs: () => import(/* @vite-ignore  */ 'dayjs/locale/zh-hk')
  },
  'zh-tw': {
    // 🇨🇳 繁體中文（中國臺灣）
    label: '\u7e41\u9ad4\u4e2d\u6587\uff08\u4e2d\u570b\u81fa\u7063\uff09',
    antd: () => import(/* @vite-ignore  */ 'ant-design-vue/es/locale/zh_TW'),
    local: () => import(/* @vite-ignore  */ './locales/zh_tw.json'),
    dayjs: () => import(/* @vite-ignore  */ 'dayjs/locale/zh-tw')
  },
  'en-us': {
    // 🇺🇸
    label: 'English',
    antd: () => import(/* @vite-ignore  */ 'ant-design-vue/es/locale/en_US'),
    local: () => import(/* @vite-ignore  */ './locales/en_us.json'),
    dayjs: () => import(/* @vite-ignore  */ 'dayjs/locale/en')
  }
}

export const supportLang = Object.keys(langDict).map((key: string) => {
  return {
    label: langDict[key].label,
    value: key
  }
})

export const supportLangArray = supportLang.map((item) => item.value)

export const normalLang = (locale: string, def: string) => {
  locale = locale.replace('_', '-').toLowerCase()
  if (supportLangArray.includes(locale)) {
    // 避免非法字符串
    return locale
  }
  console.warn(`[i18n] ${locale} is not support, use ${def} instead`)
  return def
}
// 默认语言优先读取服务端配置
const jw = window as unknown as GlobalWindow
let defaultLocaleTemp = jw.jpomDefaultLocale === '<jpomDefaultLocale>' ? 'zh-cn' : jw.jpomDefaultLocale
defaultLocaleTemp = normalLang(defaultLocaleTemp, 'zh-cn')
if (!langDict[defaultLocaleTemp]) {
  defaultLocaleTemp = 'zh-cn'
}
export const defaultLocale = defaultLocaleTemp

const i18n = createI18n<Record<string, any>, any, any>({
  legacy: false,
  locale: defaultLocale, // 默认显示语言
  fallbackLocale: defaultLocale, // 默认显示语言
  warnHtmlMessage: false
})

export default i18n
export const changeLang = async (langKey: string) => {
  langKey = langKey?.toLowerCase()
  const lang = langDict[langKey] || langDict[defaultLocale]
  const global = i18n.global as any
  if (!lang.isLoad) {
    // 动态加载对应的语言包
    const langFile = await lang.local()
    global.setLocaleMessage(langKey, langFile)
    if (i18n.mode === 'legacy') {
      global.locale = langKey
    } else {
      global.locale.value = langKey
    }
  }
  if (!lang.antdLang) {
    lang.antdLang = await lang.antd()
  }
  if (!lang.isDayjsLoad) {
    await lang.dayjs()
    lang.isDayjsLoad = true
  }

  dayjs.locale(langKey)
  lang.isLoad = true
  return lang.antdLang
}
// @ts-ignore
export const { t } = i18n.global
