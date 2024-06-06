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
// @ts-ignore 兼容i8n_tools插件
import { zhCN, enUS } from './locales'

import antdZhCN from 'ant-design-vue/es/locale/zh_CN'
import antdEnUS from 'ant-design-vue/es/locale/en_US'

/** 以中文为主,定义语言类型 */
export type I18nLocaleType = typeof zhCN

// 深层递归合并，防止出现其他语言未覆盖问题
const deepMerge = (...objects: Record<string, any>[]) => {
  const result: Record<string, any> = {}
  for (const obj of objects) {
    for (const key in obj) {
      if (Object.prototype.hasOwnProperty.call(obj, key)) {
        if (typeof obj[key] === 'object' && obj[key] !== null) {
          result[key] = deepMerge(result[key] || {}, obj[key])
        } else {
          result[key] = obj[key]
        }
      }
    }
  }
  return result
}

const usLocal = deepMerge(zhCN, enUS)

export const lang: { [key: string]: any } = {
  'zh-cn': {
    antd: antdZhCN,
    local: zhCN
  },
  'zh-CN': {
    antd: antdZhCN,
    local: zhCN
  },
  'en-us': {
    antd: antdEnUS,
    local: usLocal
  },
  'en-US': {
    antd: antdEnUS,
    local: usLocal
  }
}

const i18n = createI18n({
  legacy: false,
  locale: 'zh-cn', //'en-us', // 默认显示语言
  messages: Object.keys(lang).reduce((pre: { [key: string]: any }, key: string) => {
    const { local } = lang[key]
    pre[key] = local
    return pre
  }, {})
})

export default i18n
