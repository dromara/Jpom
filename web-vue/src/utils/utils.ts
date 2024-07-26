///
/// Copyright (c) 2019 Of Him Code Technology Studio
/// Jpom is licensed under Mulan PSL v2.
/// You can use this software according to the terms and conditions of the Mulan PSL v2.
/// You may obtain a copy of Mulan PSL v2 at:
/// 			http://license.coscl.org.cn/MulanPSL2
/// THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
/// See the Mulan PSL v2 for more details.
///

export function getHashQuery() {
  const querys: Record<string, string> = {}
  location.hash.replace(/[?&]+([^=&]+)=([^&]*)/gi, (_match: string, key: string, value: string) => {
    querys[key] = value
    return ''
  })
  return querys
}

// root 元素
const root = document.documentElement
// 获取计算后的样式
// https://developer.mozilla.org/en-US/docs/Web/API/Window/getComputedStyle
const style = getComputedStyle(root)
const zIndexStart = Number(style.getPropertyValue('--increase-z-index'))
let incCount = 0

export function increaseZIndex() {
  const useIndex = zIndexStart + incCount++
  // 设置全局变量,避免全局提示，下拉框，下拉菜单显示错位
  document.documentElement.style.setProperty('--increase-z-index', String(zIndexStart + incCount))
  return useIndex
}
