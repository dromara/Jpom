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
 * 用户相关的 store
 * 存储了用户的 token，用于接口请求的权限验证
 * 存储用户的基本信息，用于展示
 * 存储该用户对应的菜单列表，用于渲染侧边栏菜单
 * 同样提供与之对应的更新功能
 */
import { TOKEN_KEY, USER_INFO_KEY, MENU_KEY, LONG_TERM_TOKEN } from "@/utils/const";

import { getUserInfo, loginOut } from "@/api/user/user";

const user = {
	state: {
		token: localStorage.getItem(TOKEN_KEY),
		longTermToken: localStorage.getItem(LONG_TERM_TOKEN),
		userInfo: JSON.parse(localStorage.getItem(USER_INFO_KEY)) || {},
		reloadUserInfo: false,
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
		refreshUserInfo({ commit }) {
			// 加载用户信息
			return new Promise((resolve) => {
				getUserInfo().then((res) => {
					if (res.code === 200) {
						commit("setUserInfo", res.data);
						localStorage.setItem(USER_INFO_KEY, JSON.stringify(res.data));
						resolve();
					}
				});
			});
		},
		// 页面刷新 加载用户信息
		pageReloadRefreshUserInfo({ dispatch, state }) {
			if (state.reloadUserInfo) {
				return;
			}
			dispatch("refreshUserInfo").then(() => {
				state.reloadUserInfo = true;
			});
		},
		// 登录 data = {token: 'xxx', userName: 'name'}
		login({ dispatch, commit }, data) {
			return new Promise((resolve) => {
				commit("setToken", data);
				//commit('setUserName', data.userName);
				dispatch("refreshUserInfo");
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
