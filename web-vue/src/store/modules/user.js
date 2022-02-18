/**
 * 用户相关的 store
 * 存储了用户的 token，用于接口请求的权限验证
 * 存储用户的基本信息，用于展示
 * 存储该用户对应的菜单列表，用于渲染侧边栏菜单
 * 同样提供与之对应的更新功能
 */
import { TOKEN_KEY, USER_INFO_KEY, MENU_KEY, LONG_TERM_TOKEN } from "@/utils/const";

import { getUserInfo, loginOut } from "@/api/user";

const user = {
  state: {
    token: localStorage.getItem(TOKEN_KEY),
    longTermToken: localStorage.getItem(LONG_TERM_TOKEN),
    userInfo: JSON.parse(localStorage.getItem(USER_INFO_KEY)),
  },
  mutations: {
    setToken(state, data) {
      state.token = data.token || "";
      state.longTermToken = data.longTermToken || "";
      if (state.token) {
        localStorage.setItem(TOKEN_KEY, data.token);
      } else {
        localStorage.removeItem(TOKEN_KEY);
      }
      if (state.longTermToken) {
        localStorage.setItem(LONG_TERM_TOKEN, data.longTermToken);
      } else {
        localStorage.removeItem(LONG_TERM_TOKEN);
      }
    },
    setUserInfo(state, userInfo) {
      state.userInfo = userInfo;
    },
  },
  actions: {
    // 登录 data = {token: 'xxx', userName: 'name'}
    login({ dispatch, commit }, data) {
      return new Promise((resolve) => {
        commit("setToken", data);
        //commit('setUserName', data.userName);
        // 加载用户信息
        getUserInfo().then((res) => {
          if (res.code === 200) {
            commit("setUserInfo", res.data);
            localStorage.setItem(USER_INFO_KEY, JSON.stringify(res.data));
          }
        });

        // 加载系统菜单 这里需要等待 action 执行完毕返回 promise, 否则第一次登录可能会从 store 里面获取不到 menus 数据而报错
        dispatch("loadSystemMenus").then(() => {
          resolve();
        });
      });
    },
    // 退出登录 移除对应的 store
    logOut({ dispatch, commit }) {
      return new Promise((resolve) => {
        commit("setToken", {});
        commit("setMenus", "");
        dispatch("changeWorkspace", "");
        localStorage.removeItem(MENU_KEY);
        // 调用其他 action
        dispatch("clearTabs", { key: "all" });
        loginOut({}).then(() => {
          resolve();
        });
      });
    },
  },
  getters: {
    getToken(state) {
      return state.token;
    },
    getLongTermToken(state) {
      return state.longTermToken;
    },
    getUserInfo(state) {
      return state.userInfo;
    },
  },
};

export default user;
