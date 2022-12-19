/**
 * 应用相关的 store
 * 存储所有打开的 tab 窗口，并且记录当前激活的 tab
 * 另外提供打开新 tab、跳转 tab、移除 tab 功能
 */
import { CACHE_WORKSPACE_ID } from "@/utils/const";

const app = {
  state: {
    workspaceId: localStorage.getItem(CACHE_WORKSPACE_ID),
    // 菜单折叠
    collapsed: localStorage.getItem("collapsed"),
  },
  mutations: {
    setWorkspace(state, workspaceId) {
      state.workspaceId = workspaceId;
    },
    setCollapsed(state, collapsed) {
      state.collapsed = collapsed;
    },
  },
  actions: {
    // 切换工作空间
    changeWorkspace({ commit }, workspaceId) {
      return new Promise((resolve) => {
        commit("setWorkspace", workspaceId);
        localStorage.setItem(CACHE_WORKSPACE_ID, workspaceId);
        //
        resolve();
      });
    },
    collapsed({ commit }, collapsed) {
      commit("setCollapsed", collapsed);
      localStorage.setItem("collapsed", collapsed);
    },
  },
  getters: {
    getWorkspaceId(state) {
      return state.workspaceId;
    },
    getCollapsed(state) {
      if (state.collapsed === undefined || state.collapsed === null) {
        return 0;
      }
      return parseInt(state.collapsed);
    },
  },
};

export default app;
