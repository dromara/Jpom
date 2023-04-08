/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
/**
 * 应用相关的 store
 * 存储所有打开的 tab 窗口，并且记录当前激活的 tab
 * 另外提供打开新 tab、跳转 tab、移除 tab 功能
 */
import { MANAGEMENT_ACTIVE_TAB_KEY, MANAGEMENT_TAB_LIST_KEY, MANAGEMENT_ACTIVE_MENU_KEY } from "@/utils/const";

import { getSystemMenu } from "@/api/menu";
import routeMenuMap from "@/router/route-menu";

const app = {
  state: {
    activeTabKey: localStorage.getItem(MANAGEMENT_ACTIVE_TAB_KEY),
    tabList: JSON.parse(localStorage.getItem(MANAGEMENT_TAB_LIST_KEY)),
    activeMenuKey: localStorage.getItem(MANAGEMENT_ACTIVE_MENU_KEY),

    menuOpenKeys: [],
    menus: [],
  },
  mutations: {
    setManagementActiveTabKey(state, activeKey) {
      state.activeTabKey = activeKey;
    },
    setManagementTabList(state, tabList) {
      state.tabList = tabList;
      localStorage.setItem(MANAGEMENT_TAB_LIST_KEY, JSON.stringify(tabList));
    },
    setManagementActiveMenuKey(state, activeMenuKey) {
      state.activeMenuKey = activeMenuKey;
    },
    setManagementMenuOpenKeys(state, keys) {
      state.menuOpenKeys = keys;
    },
    setManagementMenus(state, menus) {
      state.menus = menus;
    },
  },
  actions: {
    // 加载系统菜单 action
    loadManagementSystemMenus({ commit, state }) {
      return new Promise((resolve, reject) => {
        if (state.menus.length) {
          // 避免重复加载
          resolve();
          return;
        }
        // 加载系统菜单
        getSystemMenu()
          .then((res) => {
            res.data.forEach((element) => {
              if (element.childs.length > 0) {
                const childs = element.childs.map((child) => {
                  return {
                    ...child,
                    path: routeMenuMap[child.id],
                  };
                });
                element.childs = childs;
              }
            });
            commit("setManagementMenus", res.data);
            resolve();
          })
          .catch((error) => {
            reject(error);
          });
      });
    },
    // 添加 tab
    addManagementTab({ commit, state, dispatch }, tab) {
      return new Promise((resolve) => {
        // 从 store 里面拿到 menus 匹配 path 得到当前的菜单，设置 tab 的标题
        const menus = state.menus;
        let currentMenu = null,
          firstMenu = null;
        menus.forEach((menu) => {
          menu.childs.forEach((subMenu) => {
            if (!firstMenu) {
              firstMenu = subMenu;
            }
            if (subMenu.path === tab.path) {
              currentMenu = subMenu;
              currentMenu.parent = menu;
            }
          });
        });
        let tabList = state.tabList || [];
        // 过滤已经不能显示的菜单
        tabList = tabList.filter((item) => {
          return (
            menus.filter((menu) => {
              return (
                menu.childs.filter((subMenu) => {
                  return subMenu.path === item.path;
                }).length > 0
              );
            }).length > 0
          );
        });
        commit("setManagementTabList", tabList);
        if (!currentMenu) {
          // 打开第一个菜单
          resolve(firstMenu);
          return;
        }
        // 获取下标 -1 表示可以添加 否则就是已经存在
        const index = tabList.findIndex((ele) => ele.key === tab.key);
        //
        tab.title = currentMenu.title;
        tab.id = currentMenu.id;
        tab.parentId = currentMenu.parent.id;

        if (index > -1) {
          // 设置 activeTabKey
        } else {
          // 新增
          tabList.push(tab);
          commit("setManagementTabList", tabList);
        }
        // 设置当前选择的菜单
        dispatch("activeManagementTabKey", tab.key);
        dispatch("activeManagementMenu", tab.id);
        resolve();
      });
    },
    // 删除 tab
    removeManagementTab({ commit, state, dispatch }, key) {
      return new Promise((resolve) => {
        let tabList = state.tabList;
        const index = tabList.findIndex((ele) => ele.key === key);
        tabList.splice(index, 1);

        commit("setManagementTabList", tabList);
        localStorage.setItem(MANAGEMENT_TAB_LIST_KEY, JSON.stringify(tabList));
        // 如果删除的是 activeTabKey
        if (state.activeTabKey === key) {
          // 寻找下一个
          const tempTab = tabList[Math.min(index, 0)];
          // 如果还是原来激活的 tab 就不用更新
          if (state.activeTabKey !== tempTab.key) {
            dispatch("activeTabKey", tempTab.key);
            resolve();
          }
        }
      });
    },
    // 清除 tabs
    clearManagementTabs({ commit, state, dispatch }, { key, position }) {
      return new Promise((resolve) => {
        let tabList = state.tabList;
        key = key || state.activeTabKey;
        if (key === "all") {
          // 清空全部
          commit("setManagementTabList", []);
          dispatch("activeTabKey", "");
          return;
        }
        // 找到当前 index
        const index = tabList.findIndex((ele) => ele.key === key);
        const currentTab = tabList[index];
        //console.log(index, key, state.activeTabKey, position);
        if (position === "left") {
          // 关闭左侧
          tabList = tabList.slice(index, tabList.length);
        } else if (position === "right") {
          // 关闭右侧
          tabList = tabList.slice(0, index + 1);
        } else {
          // 只保留当前
          tabList = [currentTab];
        }
        commit("setManagementTabList", tabList);
        localStorage.setItem(MANAGEMENT_TAB_LIST_KEY, JSON.stringify(tabList));
        //
        if (state.activeTabKey !== key) {
          dispatch("activeManagementTabKey", key);
          resolve(key);
        }
      });
    },
    activeManagementTabKey({ commit }, key) {
      commit("setManagementActiveTabKey", key);
      localStorage.setItem(MANAGEMENT_ACTIVE_TAB_KEY, key);
    },
    // 选中当前菜单
    activeManagementMenu({ commit }, activeMenuKey) {
      commit("setManagementActiveMenuKey", activeMenuKey);
      localStorage.setItem(MANAGEMENT_ACTIVE_MENU_KEY, activeMenuKey);
    },

    // 打开的菜单
    menuManagementOpenKeys({ commit, state }, keys) {
      if (Array.isArray(keys)) {
        commit("setManagementMenuOpenKeys", keys);
      } else if (typeof keys == "string") {
        const nowKeys = state.menuOpenKeys;
        if (!nowKeys.includes(keys)) {
          nowKeys.push(keys);
          commit("setManagementMenuOpenKeys", nowKeys);
        }
      }
    },
  },
  getters: {
    getManagementMenus(state) {
      return state.menus;
    },
    getManagementTabList(state) {
      return state.tabList;
    },
    getManagementActiveTabKey(state) {
      return state.activeTabKey;
    },
    getManagementActiveMenuKey(state) {
      return state.activeMenuKey;
    },

    getManagementMenuOpenKeys(state) {
      return state.menuOpenKeys;
    },
  },
};

export default app;
