///
/// Copyright (c) 2019 Of Him Code Technology Studio
/// Jpom is licensed under Mulan PSL v2.
/// You can use this software according to the terms and conditions of the Mulan PSL v2.
/// You may obtain a copy of Mulan PSL v2 at:
/// 			http://license.coscl.org.cn/MulanPSL2
/// THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
/// See the Mulan PSL v2 for more details.
///

import { t, defaultLocale, normalLang } from '@/i18n'

/**
 * 引导相关 store
 *
 */

const key = 'Jpom-Guide-Cache'

interface IState {
  disabledGuide: boolean
  extendPlugins: string[]
}

interface IStateCache extends IState {
  guideCache: IStateGuideCache
  systemIsDark: boolean
  getThemeView?: Function
  getCatchThemeView?: Function
}

interface IStateGuideCache {
  fullScreenFlag: boolean
  scrollbarFlag: boolean
  menuMultipleFlag: boolean
  fullscreenViewLog: boolean
  close: boolean
  compactView: boolean
  themeView: string
  menuThemeView: string
  locale: string
}

const allowThemeView = ['light', 'dark', 'auto']

export const useGuideStore = defineStore('guide', {
  state: (): IStateCache => ({
    // 引导缓存
    guideCache: localStorage.getItem(key) ? JSON.parse(localStorage.getItem(key)!) : {},
    systemIsDark: window.matchMedia('(prefers-color-scheme: dark)').matches,
    disabledGuide: false,
    extendPlugins: []
  }),

  actions: {
    setSystemIsDark(bool: boolean) {
      this.systemIsDark = bool
    },
    setGuideCache(cache: IStateGuideCache) {
      this.guideCache = cache
      localStorage.setItem(key, JSON.stringify(cache))
    },
    // 页面全屏开关
    toggleFullScreenFlag(): Promise<boolean> {
      return new Promise((resolve) => {
        const cache = this.getGuideCache
        cache.fullScreenFlag = !(cache.fullScreenFlag === undefined ? true : cache.fullScreenFlag)
        this.setGuideCache(cache)
        resolve(cache.fullScreenFlag)
      })
    },
    // 页面滚动条
    toggleScrollbarFlag(): Promise<boolean> {
      return new Promise((resolve) => {
        const cache = this.getGuideCache
        cache.scrollbarFlag = !(cache.scrollbarFlag === undefined ? true : cache.scrollbarFlag)
        this.setGuideCache(cache)
        resolve(cache.scrollbarFlag)
      })
    },
    // 打开多菜单开关
    toggleMenuFlag(): Promise<boolean> {
      return new Promise((resolve) => {
        const cache = this.getGuideCache
        cache.menuMultipleFlag = !(cache.menuMultipleFlag === undefined ? true : cache.menuMultipleFlag)
        this.setGuideCache(cache)
        resolve(cache.menuMultipleFlag)
      })
    },
    // 切换引导开关
    toggleGuideFlag(): Promise<boolean> {
      return new Promise((resolve) => {
        const cache = this.getGuideCache
        cache.close = !cache.close
        this.setGuideCache(cache)
        resolve(cache.close)
      })
    },
    //   切换全屏查看日志
    toggleFullscreenViewLog(): Promise<boolean> {
      return new Promise((resolve) => {
        const cache = this.getGuideCache
        cache.fullscreenViewLog = !cache.fullscreenViewLog
        this.setGuideCache(cache)
        resolve(cache.fullscreenViewLog)
      })
    },
    toggleCompactView(): Promise<boolean> {
      return new Promise((resolve) => {
        const cache = this.getGuideCache
        cache.compactView = !cache.compactView
        this.setGuideCache(cache)
        resolve(cache.compactView)
      })
    },
    toggleThemeView(themeView: string): Promise<string> {
      return new Promise((resolve) => {
        const cache = this.getGuideCache
        cache.themeView = themeView
        this.setGuideCache(cache)
        resolve(cache.themeView)
      })
    },
    toggleMenuThemeView(menuThemeView: string): Promise<string> {
      return new Promise((resolve) => {
        const cache = this.getGuideCache
        cache.menuThemeView = menuThemeView
        this.setGuideCache(cache)
        resolve(cache.menuThemeView)
      })
    },
    // 重置导航
    restGuide(): Promise<boolean> {
      return new Promise((resolve) => {
        this.setGuideCache({} as IStateGuideCache)
        resolve(true)
      })
    },
    commitGuide(guideData: IState) {
      this.disabledGuide = guideData.disabledGuide
      this.extendPlugins = guideData.extendPlugins
    },
    async changeLocale(locale: string) {
      const cache = this.getGuideCache
      cache.locale = locale || 'zh-cn'
      this.setGuideCache(cache)
      //
      location.reload()
    }
  },
  getters: {
    getGuideCache(state): IStateGuideCache {
      return state.guideCache
    },
    getDisabledGuide(state) {
      return state.disabledGuide
    },
    getExtendPlugins(state) {
      return state.extendPlugins
    },
    getCatchThemeView: (state) => {
      return () => {
        // 新用户未设置过为跟随系统
        const theme = state.guideCache.themeView || 'auto'
        return allowThemeView.includes(theme) ? theme : 'auto'
      }
    },
    getSupportThemes: () => {
      return [
        {
          label: t('i18n_71bbc726ac'),
          value: 'auto'
        },
        {
          label: t('i18n_48d0a09bdd'),
          value: 'light'
        },
        {
          label: t('i18n_41e8e8b993'),
          value: 'dark'
        }
      ]
    },
    getThemeView: (state) => {
      return () => {
        const themeView = state?.getCatchThemeView ? state.getCatchThemeView() : 'auto'
        if (themeView === 'auto') {
          return state.systemIsDark ? 'dark' : 'light'
        } else {
          return themeView
        }
      }
    },
    getThemeStyle: (state) => {
      return () => {
        const theme = state?.getThemeView ? state?.getThemeView() : 'light'
        return {
          color: theme === 'light' ? 'rgba(0, 0, 0, 0.88)' : '#fff'
        }
      }
    },
    getMenuThemeView: (state) => {
      return () => {
        const menuThemeView = state.guideCache.menuThemeView || 'dark'
        if (allowThemeView.includes(menuThemeView)) {
          return menuThemeView
        }
        return 'dark'
      }
    },
    // 计算弹窗全屏样式
    getFullscreenViewLogStyle: (state) => {
      return () => {
        const cache = state.guideCache as any

        if (cache.fullscreenViewLog) {
          // 全屏
          return {
            // dialogStyle: {
            //   maxWidth: '100vw',
            //   top: 0,
            //   paddingBottom: 0
            // },
            bodyStyle: {
              padding: '0 10px',
              paddingTop: '10px',
              marginRight: '10px',
              height: 'calc(100vh - 90px)'
            },
            width: '100vw',
            style: {
              maxWidth: '100vw',
              top: 0,
              paddingBottom: 0
            }
          }
        }
        // 非全屏
        return {
          // dialogStyle: {
          //   maxWidth: '100vw',
          //   top: false,
          //   paddingBottom: 0
          // },
          bodyStyle: {
            padding: '0 10px',
            paddingTop: '10px',
            marginRight: '10px',
            height: '70vh'
          },
          width: '80vw',
          style: {
            maxWidth: '100vw',
            top: false,
            paddingBottom: 0
          }
        }
      }
    },
    getLocale: (state) => {
      return () => {
        const locale = state.guideCache.locale || navigator.language
        return normalLang(locale, defaultLocale)
      }
    }
  }
})

// function cacheToJson(state: any) {
//   const cacheStr = state.getGuideCache || ''
//   let cahce
//   try {
//     cahce = JSON.parse(cacheStr)
//   } catch (e) {
//     cahce = {}
//   }
//   return cahce
// }

// const app = {
//   state: {
//     // 引导缓存
//     guideCache: localStorage.getItem(key),
//     disabledGuide: false,
//     inDocker: false,
//   },
//   mutations: {
//     setGuideCache(state, guideCache) {
//       state.guideCache = JSON.stringify(guideCache)
//       localStorage.setItem(key, state.guideCache)
//     },
//     commitGuide(state, guideData) {
//       state.disabledGuide = guideData.disabledGuide
//       state.inDocker = guideData.inDocker
//     },
//   },
//   actions: {},
//   getters: {},
// }

// export default app
