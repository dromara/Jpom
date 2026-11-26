/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

/**
 * 主题变量层：把 Ant Design Vue 的设计 token 映射成全局 CSS 变量（--jp-*）。
 *
 * 为什么需要它：系统主题（亮色 / 暗色 / 紧凑）由 App.vue 的 a-config-provider algorithm 统一切换，
 * 但 antd 4.2.x 没有 theme.cssVar（拿不到 --ant-* 变量），页面里一旦写死 #fff / #fafbfc 这类色值，
 * 暗色主题下就只剩这些页面是白底黑字。约定：**页面自定义样式只许引用 var(--jp-*)，禁止写死颜色**，
 * 规则见 runbooks/ui-interaction-guidelines.md §5。
 *
 * 装配点只有一个：App.vue 里 `ThemeVarsProvider`（挂在 a-config-provider 内部，useToken 只在这里拿得到
 * 当前算法下的 token）。新增变量往 THEME_CSS_VARS 加一行即可，页面侧不需要任何改动。
 */

import { defineComponent, watchEffect } from 'vue'
import { theme } from 'ant-design-vue'

/** --jp-<key> → GlobalToken 字段名（唯一映射表） */
export const THEME_CSS_VARS: Record<string, string> = {
  'color-text': 'colorText',
  'color-text-secondary': 'colorTextSecondary',
  'color-text-tertiary': 'colorTextTertiary',
  'color-text-quaternary': 'colorTextQuaternary',
  'color-border': 'colorBorder',
  'color-border-secondary': 'colorBorderSecondary',
  'color-split': 'colorSplit',
  'color-bg-container': 'colorBgContainer',
  'color-bg-elevated': 'colorBgElevated',
  'color-bg-layout': 'colorBgLayout',
  'color-fill-tertiary': 'colorFillTertiary',
  'color-fill-quaternary': 'colorFillQuaternary',
  'color-fill-alter': 'colorFillAlter',
  'color-primary': 'colorPrimary',
  'color-primary-bg': 'colorPrimaryBg',
  'color-primary-border': 'colorPrimaryBorder',
  'color-success': 'colorSuccess',
  'color-success-text': 'colorSuccessText',
  'color-success-bg': 'colorSuccessBg',
  'color-warning': 'colorWarning',
  // 文本场景用 *Text（antd 的 color-7，与浅色底对比度足够）；纯色 colorWarning(#faad14) 只适合图标
  'color-warning-text': 'colorWarningText',
  'color-warning-bg': 'colorWarningBg',
  'color-warning-border': 'colorWarningBorder',
  'color-error': 'colorError',
  'color-error-text': 'colorErrorText',
  'color-error-bg': 'colorErrorBg',
  'color-error-border': 'colorErrorBorder',
  shadow: 'boxShadowTertiary',
  'radius-sm': 'borderRadiusSM',
  radius: 'borderRadius',
  'radius-lg': 'borderRadiusLG',
  'font-size-sm': 'fontSizeSM',
  'font-size': 'fontSize'
}

/** CSS 变量前缀 */
export const THEME_VAR_PREFIX = '--jp-'

/**
 * 语义色做「文字」时的对比度目标。
 *
 * 背景：ant-design-vue 4.2.6 的 `colorWarningText` 与 `colorWarning` 取值相同（`genColorMapToken` 里
 * `colorWarningText = warningColors[9]`，而该版本的调色板到第 6 档就是基色），所以直接拿 `*Text` token
 * 当正文色，在浅色底上是 `#faad14` 这种「亮黄小字」，对比度只有 2.1:1，比改造前的 `#d46b08`(3.46:1) 更差。
 * 这里不写死色值，而是按 WCAG 对比度从主题基色自动派生：浅色底往黑混、深色底往白混，直到达到目标对比度。
 * 目标取 4.5（WCAG AA 正文标准）。改造前的 #d46b08 / #389e0d / #cf1322 分别是 3.56 / 3.46 / 5.57，
 * 因此浅色下语义文字会比改造前略深（success 几乎一致），属可读性提升；暗色下基色本身已达标，原样保留。
 */
const SEMANTIC_TEXT_CONTRAST = 4.5

/** 语义文字色的来源 token（基色） */
const SEMANTIC_TEXT_SOURCES: Record<string, string> = {
  'color-success-text': 'colorSuccess',
  'color-warning-text': 'colorWarning',
  'color-error-text': 'colorError'
}

type Rgb = { r: number; g: number; b: number }

function parseHexColor(input: string): Rgb | null {
  const m = /^#([0-9a-fA-F]{3,8})$/.exec(String(input || '').trim())
  if (!m) {
    return null
  }
  let hex = m[1]
  if (hex.length === 3 || hex.length === 4) {
    hex = hex
      .split('')
      .map((c) => c + c)
      .join('')
  }
  const num = (i: number) => parseInt(hex.slice(i, i + 2), 16)
  return { r: num(0), g: num(2), b: num(4) }
}

function toHex({ r, g, b }: Rgb): string {
  const h = (v: number) => Math.max(0, Math.min(255, Math.round(v))).toString(16).padStart(2, '0')
  return '#' + h(r) + h(g) + h(b)
}

function relativeLuminance({ r, g, b }: Rgb): number {
  const f = (v: number) => {
    const x = v / 255
    return x <= 0.03928 ? x / 12.92 : Math.pow((x + 0.055) / 1.055, 2.4)
  }
  return 0.2126 * f(r) + 0.7152 * f(g) + 0.0722 * f(b)
}

function contrastRatio(a: Rgb, b: Rgb): number {
  const la = relativeLuminance(a)
  const lb = relativeLuminance(b)
  const light = Math.max(la, lb)
  const dark = Math.min(la, lb)
  return (light + 0.05) / (dark + 0.05)
}

function mix(from: Rgb, to: Rgb, ratio: number): Rgb {
  return {
    r: from.r + (to.r - from.r) * ratio,
    g: from.g + (to.g - from.g) * ratio,
    b: from.b + (to.b - from.b) * ratio
  }
}

/** 把语义基色朝黑/白混到目标对比度（二分，取满足条件的最小偏移量，尽量保留基色观感） */
function readableTextColor(base: string, background: string): string | null {
  const fg = parseHexColor(base)
  const bg = parseHexColor(background)
  if (!fg || !bg) {
    return null
  }
  if (contrastRatio(fg, bg) >= SEMANTIC_TEXT_CONTRAST) {
    return toHex(fg)
  }
  // 浅色底 → 往黑压；深色底 → 往白提
  const target: Rgb = relativeLuminance(bg) > 0.5 ? { r: 0, g: 0, b: 0 } : { r: 255, g: 255, b: 255 }
  let low = 0
  let high = 1
  let best = mix(fg, target, 1)
  for (let i = 0; i < 12; i++) {
    const mid = (low + high) / 2
    const candidate = mix(fg, target, mid)
    if (contrastRatio(candidate, bg) >= SEMANTIC_TEXT_CONTRAST) {
      best = candidate
      high = mid
    } else {
      low = mid
    }
  }
  return toHex(best)
}

/**
 * 把当前主题算法下的设计 token 写到 :root 上（同名变量会被覆盖，不会累积）
 * @param token theme.useToken() 返回的 token
 */
export function applyThemeVars(token?: Record<string, any> | null): void {
  if (!token || typeof document === 'undefined') {
    return
  }
  const root = document.documentElement
  Object.keys(THEME_CSS_VARS).forEach((name) => {
    const value = token[THEME_CSS_VARS[name]]
    if (value === undefined || value === null || value === '') {
      return
    }
    // 数值型 token（圆角、字号）补 px；颜色/阴影本身就是带单位的字符串
    const cssValue = typeof value === 'number' ? value + 'px' : String(value)
    root.style.setProperty(THEME_VAR_PREFIX + name, cssValue)
  })
  // 语义文字色：按主题底色自适应（见 SEMANTIC_TEXT_CONTRAST 说明），不写死色值
  Object.keys(SEMANTIC_TEXT_SOURCES).forEach((name) => {
    const base = token[SEMANTIC_TEXT_SOURCES[name]]
    const background = token.colorBgContainer
    if (!base || !background) {
      return
    }
    const derived = readableTextColor(String(base), String(background))
    if (derived) {
      root.style.setProperty(THEME_VAR_PREFIX + name, derived)
    }
  })
}

/**
 * 主题变量注入组件（渲染为空节点）。
 *
 * 必须挂在 `a-config-provider` 内部：`theme.useToken()` 读取的是**最近的 ConfigProvider** 的主题，
 * 在 App.vue 的 setup 里调用拿到的是默认（亮色）token，主题切换不会反映到变量上。
 */
export const ThemeVarsProvider = defineComponent({
  name: 'ThemeVarsProvider',
  setup() {
    const { token } = theme.useToken()
    watchEffect(() => applyThemeVars(token.value))
    return () => null
  }
})
