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
import { CACHE_WORKSPACE_ID } from "@/utils/const";
import router from "@/router";
import { executionRequest } from "@/api/external";
import { parseTime, pageBuildInfo } from "@/utils/const";

const app = {
  state: {
    workspaceId: localStorage.getItem(CACHE_WORKSPACE_ID),
    // 菜单折叠
    collapsed: localStorage.getItem("collapsed"),
    showInfo: false,
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
    showInfo({ state }, to) {
      if (state.showInfo) {
        return;
      }
      // 控制台输出版本号信息
      const buildInfo = pageBuildInfo();
      executionRequest("https://jpom.top/docs/versions.show", { ...buildInfo, p: to.path }).then((data) => {
        console.log(
          "\n %c " + parseTime(buildInfo.t) + " %c vs %c " + buildInfo.v + " %c vs %c " + data,
          "color: #ffffff; background: #f1404b; padding:5px 0;",
          "background: #1890ff; padding:5px 0;",
          "color: #ffffff; background: #f1404b; padding:5px 0;",
          "background: #1890ff; padding:5px 0;",
          "color: #ffffff; background: #f1404b; padding:5px 0;"
        );
        state.showInfo = true;
      });
    },
  },
  getters: {
    getWorkspaceId(state) {
      let wid = router.app.$route.query.wid;
      if (!wid) {
        wid = getHashVars().wid;
      }
      return wid || state.workspaceId;
    },
    getCollapsed(state) {
      if (state.collapsed === undefined || state.collapsed === null) {
        return 0;
      }
      return parseInt(state.collapsed);
    },
  },
};

function getHashVars() {
  var vars = {};
  location.hash.replace(/[?&]+([^=&]+)=([^&]*)/gi, function (m, key, value) {
    vars[key] = value;
  });
  return vars;
}

export default app;
