import { TOKEN_KEY, USER_INFO_KEY, MENU_KEY, LONG_TERM_TOKEN } from '@/utils/const'

import { getUserInfo, loginOut } from '@/api/user/user'
import { defineStore } from 'pinia'
import { useMenuStore } from './menu'

const menuStore = useMenuStore()

const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    longTermToken: localStorage.getItem(LONG_TERM_TOKEN) || '',
    userInfo: localStorage.getItem(USER_INFO_KEY) ? JSON.parse(localStorage.getItem(USER_INFO_KEY)!) : {},
    reloadUserInfo: false,
  }),
  actions: {
    async refreshUserInfo() {
      return await getUserInfo().then((res) => {
        if (res.code === 200) {
          this.userInfo = res.data
          localStorage.setItem(USER_INFO_KEY, JSON.stringify(res.data))
          return res.data
        }
        return Promise.reject()
      })
    },
    // 页面刷新 加载用户信息
    pageReloadRefreshUserInfo() {
      if (this.reloadUserInfo) {
        return
      }
      this.refreshUserInfo().then(() => {
        this.reloadUserInfo = true
      })
    },
    // 登录 data = {token: 'xxx', userName: 'name'}
    async reLogin(data: any) {
      this.token = data.token || ''
      this.longTermToken = data.longTermToken || ''
      if (this.token) {
        localStorage.setItem(TOKEN_KEY, data.token)
      } else {
        localStorage.removeItem(TOKEN_KEY)
      }
      if (this.longTermToken) {
        localStorage.setItem(LONG_TERM_TOKEN, data.longTermToken)
      } else {
        localStorage.removeItem(LONG_TERM_TOKEN)
      }
      await this.refreshUserInfo()
      return true
    },
    // 退出登录 移除对应的 store
    async logOut() {
      localStorage.removeItem(TOKEN_KEY)
      menuStore.menus = []
      localStorage.removeItem(MENU_KEY)
      // 调用其他 action
      menuStore.clearTabs({ key: 'all' })
      return loginOut({})
    },
  },
  getters: {
    getToken(state) {
      return state.token
    },
    getLongTermToken(state) {
      return state.longTermToken
    },
    getUserInfo(state) {
      return state.userInfo
    },
  },
})

export default useUserStore
