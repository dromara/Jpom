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
  guideCache: string
}

interface IStateGuideCache {
  fullScreenFlag: boolean
  scrollbarFlag: boolean
  menuMultipleFlag: boolean
}

export const useGuideStore = defineStore('guide', {
  state: (): IStateCache => ({
    // 引导缓存
    guideCache: localStorage.getItem(key) || '',
    disabledGuide: false,
    extendPlugins: []
  }),

  actions: {
    setGuideCache(cache: IStateGuideCache) {
      this.guideCache = JSON.stringify(cache)
      localStorage.setItem(key, this.guideCache)
    },
    // 页面全屏开关
    toggleFullScreenFlag() {
      return new Promise((resolve) => {
        const cache = this.getGuideCache
        cache.fullScreenFlag = !(cache.fullScreenFlag === undefined ? true : cache.fullScreenFlag)
        this.setGuideCache(cache)
        resolve(cache.fullScreenFlag)
      })
    },
    // 页面滚动条
    toggleScrollbarFlag() {
      return new Promise((resolve) => {
        const cache = this.getGuideCache
        cache.scrollbarFlag = !(cache.scrollbarFlag === undefined ? true : cache.scrollbarFlag)
        this.setGuideCache(cache)
        resolve(cache.scrollbarFlag)
      })
    },
    // 打开多菜单开关
    toggleMenuFlag() {
      return new Promise((resolve) => {
        const cache = this.getGuideCache
        cache.menuMultipleFlag = !(cache.menuMultipleFlag === undefined ? true : cache.menuMultipleFlag)
        this.setGuideCache(cache)
        resolve(cache.menuMultipleFlag)
      })
    },
    commitGuide(guideData: IState) {
      this.disabledGuide = guideData.disabledGuide
      this.extendPlugins = guideData.extendPlugins
    }
  },
  getters: {
    getGuideCache(state): IStateGuideCache {
      const cacheStr = state.guideCache || ''
      let cahce
      try {
        cahce = JSON.parse(cacheStr)
      } catch (e) {
        cahce = {}
      }
      return cahce
    },
    getDisabledGuide(state) {
      return state.disabledGuide
    },
    getExtendPlugins(state) {
      return state.extendPlugins
    }
  }
})

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
