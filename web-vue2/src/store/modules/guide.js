/**
 * 引导相关 store
 *
 */
import Vue from "vue";

const key = "Jpom-Guide-Cache";

const app = {
  state: {
    // 引导缓存
    guideCache: localStorage.getItem(key),
    disabledGuide: false,
    extendPlugins: [],
  },
  mutations: {
    setGuideCache(state, guideCache) {
      state.guideCache = JSON.stringify(guideCache);
      localStorage.setItem(key, state.guideCache);
    },
    commitGuide(state, guideData) {
      state.disabledGuide = guideData.disabledGuide;
      state.extendPlugins = guideData.extendPlugins || [];
    },
  },
  actions: {
    // 页面全屏开关
    toggleFullScreenFlag({ commit, rootGetters }) {
      return new Promise((resolve) => {
        const cache = rootGetters.getGuideCache;
        cache.fullScreenFlag = !(cache.fullScreenFlag === undefined ? true : cache.fullScreenFlag);
        commit("setGuideCache", cache);
        resolve(cache.fullScreenFlag);
      });
    },
    // 页面滚动条
    toggleScrollbarFlag({ commit, rootGetters }) {
      return new Promise((resolve) => {
        const cache = rootGetters.getGuideCache;
        cache.scrollbarFlag = !(cache.scrollbarFlag === undefined ? true : cache.scrollbarFlag);
        commit("setGuideCache", cache);
        resolve(cache.scrollbarFlag);
      });
    },
    // 打开多菜单开关
    toggleMenuFlag({ commit, rootGetters }) {
      return new Promise((resolve) => {
        const cache = rootGetters.getGuideCache;
        cache.menuMultipleFlag = !(cache.menuMultipleFlag === undefined ? true : cache.menuMultipleFlag);
        commit("setGuideCache", cache);
        resolve(cache.menuMultipleFlag);
      });
    },
    // 切换引导开关
    toggleGuideFlag({ commit, rootGetters }) {
      return new Promise((resolve) => {
        const cache = rootGetters.getGuideCache;
        cache.close = !cache.close;
        commit("setGuideCache", cache);
        resolve(cache.close);
      });
    },
    //   切换全屏查看日志
    toggleFullscreenViewLog({ commit, rootGetters }) {
      return new Promise((resolve) => {
        const cache = rootGetters.getGuideCache;
        cache.fullscreenViewLog = !cache.fullscreenViewLog;
        commit("setGuideCache", cache);
        resolve(cache.fullscreenViewLog);
      });
    },
    commitGuide({ commit }, value) {
      commit("commitGuide", value);
    },
    // 尝试打开引导
    tryOpenGuide({ commit, rootGetters }, { key, beforeKey, options }) {
      return new Promise((resolve) => {
        const cache = rootGetters.getGuideCache;
        if (cache.close || rootGetters.getDisabledGuide) {
          // 全局关闭
          return;
        }
        if (beforeKey && cache[beforeKey] !== "show") {
          // 判断在显示某个引导后才显示
          return;
        }
        // 判断是否显示过
        if (cache[key] !== "show") {
          Vue.prototype.$introJs
            .setOptions(options)
            .start()
            .onexit(() => {
              cache[key] = "show";
              commit("setGuideCache", cache);
            });
          resolve();
          return;
        }
        Vue.prototype.$introJs.exit();
      });
    },
    // 重置导航
    restGuide({ commit }) {
      return new Promise((resolve) => {
        commit("setGuideCache", {});
        resolve();
      });
    },
  },
  getters: {
    getGuideCache(state) {
      return cacheToJson(state);
    },
    getDisabledGuide(state) {
      return state.disabledGuide;
    },
    getExtendPlugins(state) {
      return state.extendPlugins || [];
    },
    // 计算弹窗全屏样式
    getFullscreenViewLogStyle(state) {
      const cache = cacheToJson(state);
      if (cache.fullscreenViewLog) {
        // 全屏
        return {
          dialogStyle: {
            maxWidth: "100vw",
            top: 0,
            paddingBottom: 0,
          },
          bodyStyle: {
            padding: "0 10px",
            paddingTop: "10px",
            marginRight: "10px",
            height: "calc(100vh - 68px)",
          },
          width: "100vw",
        };
      }
      // 非全屏
      return {
        dialogStyle: {
          maxWidth: "100vw",
          top: false,
          paddingBottom: 0,
        },
        bodyStyle: {
          padding: "0 10px",
          paddingTop: "10px",
          marginRight: "10px",
          height: "70vh",
        },
        width: "80vw",
      };
    },
  },
};

function cacheToJson(state) {
  const cacheStr = state.guideCache || "";
  let cahce;
  try {
    cahce = JSON.parse(cacheStr);
  } catch (e) {
    cahce = {};
  }
  return cahce;
}

export default app;
