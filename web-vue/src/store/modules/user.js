/**
 * 用户相关的 store
 */
import {
  USER_NAME_KEY,
  TOKEN_KEY,
  MENU_KEY
} from '../../utils/const'

import { getSystemMenu } from '../../api/menu';

const user = {
  state: {
    userName: localStorage.getItem(USER_NAME_KEY),
    token: localStorage.getItem(TOKEN_KEY),
    menus: JSON.parse(localStorage.getItem(MENU_KEY))
  },
  mutations: {
    setUserName(state, name) {
      state.userName = name
    },
    setToken(state, token) {
      state.token = token
    },
    setMenus(state, menus) {
      state.menus = menus
    }
  },
  actions: {
    // 登录 data = {token: 'xxx', userName: 'name'}
    login({commit}, data) {
      return new Promise((resolve, reject) => {
        commit('setToken', data.token);
        commit('setUserName', data.userName);
        localStorage.setItem(TOKEN_KEY, data.token);
        localStorage.setItem(USER_NAME_KEY, data.userName);
        // 加载系统菜单
        getSystemMenu().then(res => {
          commit('setMenus', res.data);
          localStorage.setItem(MENU_KEY, JSON.stringify(res.data));
          resolve();
        }).catch(error => {
          reject(error)
        })
      })
    },
    // 退出登录 移除对应的 store
    logOut({commit}) {
      return new Promise((resolve) => {
        commit('setToken', '');
        commit('setUserName', '');
        commit('setMenus', '');
        localStorage.removeItem(TOKEN_KEY);
        localStorage.removeItem(USER_NAME_KEY);
        localStorage.removeItem(MENU_KEY);
        resolve();
      })
    }
  },
  getters: {
    getToken(state) {
      return state.token;
    },
    getUserName(state) {
      return state.userName;
    },
    getMenus(state) {
      return state.menus;
    }
  }
}

export default user
