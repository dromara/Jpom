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
}

interface IStateGuideCache {
  fullScreenFlag: boolean
  scrollbarFlag: boolean
  menuMultipleFlag: boolean
  fullscreenViewLog: boolean
  close: boolean
}

export const useGuideStore = defineStore('guide', {
  state: (): IStateCache => ({
    // 引导缓存
    guideCache: localStorage.getItem(key) ? JSON.parse(localStorage.getItem(key)!) : {},
    disabledGuide: false,
    extendPlugins: []
  }),

  actions: {
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
    // 计算弹窗全屏样式
    getFullscreenViewLogStyle(state) {
      const cache = cacheToJson(state)
      if (cache.fullscreenViewLog) {
        // 全屏
        return {
          dialogStyle: {
            maxWidth: '100vw',
            top: 0,
            paddingBottom: 0
          },
          bodyStyle: {
            padding: '0 10px',
            paddingTop: '10px',
            marginRight: '10px',
            height: 'calc(100vh - 68px)'
          },
          width: '100vw'
        }
      }
      // 非全屏
      return {
        dialogStyle: {
          maxWidth: '100vw',
          top: false,
          paddingBottom: 0
        },
        bodyStyle: {
          padding: '0 10px',
          paddingTop: '10px',
          marginRight: '10px',
          height: '70vh'
        },
        width: '80vw'
      }
    }
  }
})

function cacheToJson(state: any) {
  const cacheStr = state.guideCache || ''
  let cahce
  try {
    cahce = JSON.parse(cacheStr)
  } catch (e) {
    cahce = {}
  }
  return cahce
}

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
