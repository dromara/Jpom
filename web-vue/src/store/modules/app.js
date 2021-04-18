/**
 * 应用相关的 store
 * 存储所有打开的 tab 窗口，并且记录当前激活的 tab
 * 另外提供打开新 tab、跳转 tab、移除 tab 功能
 */
import {
  ACTIVE_TAB_KEY,
  TAB_LIST_KEY,
  ACTIVE_MENU_KEY,
  GUIDE_FLAG_KEY
} from '../../utils/const'

const app = {
  state: {
    activeTabKey: localStorage.getItem(ACTIVE_TAB_KEY),
    tabList: JSON.parse(localStorage.getItem(TAB_LIST_KEY)),
    activeMenuKey: localStorage.getItem(ACTIVE_MENU_KEY),
    guideFlag: localStorage.getItem(GUIDE_FLAG_KEY) === 'true' ? true : false
  },
  mutations: {
    setActiveTabKey(state, activeKey) {
      state.activeTabKey = activeKey;
    },
    setTabList(state, tabList) {
      state.tabList = tabList;
    },
    setActiveMenuKey(state, activeMenuKey) {
      state.activeMenuKey = activeMenuKey;
    },
    setGuideFlag(state, guideFlag) {
      state.guideFlag = guideFlag;
    }
  },
  actions: {
    // 添加 tab
    addTab({commit, state, rootGetters}, tab) {
      return new Promise((resolve) => {
        // 从 store 里面拿到 menus 匹配 path 得到当前的菜单，设置 tab 的标题
        const menus = rootGetters.getMenus;
        let currentMenu = {};
        menus.forEach(menu => {
          menu.childs.forEach(subMenu => {
            if (subMenu.path === tab.path) {
              currentMenu = subMenu;
            }
          })
        });
        tab.title = currentMenu.title;
        tab.id = currentMenu.id;
        let tabList = state.tabList || [];
        // 获取下标 -1 表示可以添加 否则就是已经存在
        const index = tabList.findIndex(ele => ele.key === tab.key);
        if (index > -1) {
          // 设置 activeTabKey
          commit('setActiveTabKey', tab.key);
          localStorage.setItem(ACTIVE_TAB_KEY, tab.key);
        } else {
          // 新增
          tabList.push(tab);
          commit('setTabList', tabList);
          commit('setActiveTabKey', tab.key);
          localStorage.setItem(ACTIVE_TAB_KEY, tab.key);
          localStorage.setItem(TAB_LIST_KEY, JSON.stringify(tabList));
        }
        // 设置当前选择的菜单
        commit('setActiveMenuKey', tab.id);
        localStorage.setItem(ACTIVE_MENU_KEY, tab.id);
        resolve()
      })
    },
    // 删除 tab
    removeTab({commit, state}, key) {
      return new Promise((resolve) => {
        let tabList = state.tabList;
        const index = tabList.findIndex(ele => ele.key === key);
        tabList.splice(index, 1);
        // 如果删除的是 activeTabKey
        if (state.activeTabKey === key) {
          // 寻找下一个
          const tempTab = tabList[Math.min(index, 0)];
          // 如果还是原来激活的 tab 就不用更新
          if (state.activeTabKey !== tempTab.key) {
            commit('setActiveTabKey', tempTab.key);
            localStorage.setItem(ACTIVE_TAB_KEY, tempTab.key);
          }
        }
        commit('setTabList', tabList);
        localStorage.setItem(TAB_LIST_KEY, JSON.stringify(tabList));
        resolve()
      })
    },
    // 清除 tabs
    clearTabs({commit, state}) {
      let tabList = state.tabList;
      const index = tabList.findIndex(ele => ele.key === state.activeTabKey);
      const currentTab = tabList[index];
      tabList = [currentTab];
      commit('setTabList', tabList);
      localStorage.setItem(TAB_LIST_KEY, JSON.stringify(tabList));
    },
    // 选中当前菜单
    activeMenu({commit}, activeMenuKey) {
      commit('setActiveMenuKey', activeMenuKey);
      localStorage.setItem(ACTIVE_MENU_KEY, activeMenuKey);
    },
    // 切换引导开关
    toggleGuideFlag({commit, state}) {
      return new Promise((resolve) => {
        const flag = state.guideFlag;
        commit('setGuideFlag', !flag);
        localStorage.setItem(GUIDE_FLAG_KEY, !flag);
        resolve();
      })
    }
  },
  getters: {
    getTabList(state) {
      return state.tabList;
    },
    getActiveTabKey(state) {
      return state.activeTabKey;
    },
    getActiveMenuKey(state) {
      return state.activeMenuKey;
    },
    getGuideFlag(state) {
      return state.guideFlag;
    }
  }
}

export default app