/**
 * 应用相关的 store
 * 存储所有打开的 tab 窗口，并且记录当前激活的 tab
 * 另外提供打开新 tab、跳转 tab、移除 tab 功能
 */
import {
  MANAGEMENT_ACTIVE_TAB_KEY as ACTIVE_TAB_KEY,
  MANAGEMENT_TAB_LIST_KEY as TAB_LIST_KEY,
  MANAGEMENT_ACTIVE_MENU_KEY as ACTIVE_MENU_KEY
} from '@/utils/const'

import { getSystemMenu as getMenu } from '@/api/menu'
import routeMenuMap from '@/router/route-menu'

import { IMenuState } from '@/stores/menu'

export const useManagementMenuStore = defineStore('MANAGEMENT-menu', {
  state: (): IMenuState => ({
    activeTabKey: localStorage.getItem(ACTIVE_TAB_KEY) || '',
    tabList: localStorage.getItem(TAB_LIST_KEY) ? JSON.parse(localStorage.getItem(TAB_LIST_KEY)!) : [],
    activeMenuKey: localStorage.getItem(ACTIVE_MENU_KEY) || '',
    menuOpenKeys: [],
    menus: [],
    activeMenu: ''
  }),

  actions: {
    // 加载系统菜单 action
    async loadSystemMenus() {
      if (this.menus.length) {
        // 避免重复加载
        return true
      }
      return await this.restLoadSystemMenus()
    },
    async restLoadSystemMenus() {
      // return new Promise((resolve, reject) => {
      // 	// 加载系统菜单

      // })

      await getMenu()
        .then((res) => {
          if (res.data) {
            res.data?.forEach((element: any) => {
              if (element.childs.length > 0) {
                element.childs = element.childs.map((child: any) => {
                  return {
                    ...child,
                    path: routeMenuMap[child.id]
                  }
                })
              }
            })
            this.menus = res.data
            Promise.resolve()
          } else {
            Promise.reject()
          }
        })
        .catch((error) => {
          Promise.reject(error)
        })
    },
    // 添加 tab
    addTab(tab: any) {
      return new Promise((resolve) => {
        // 从 store 里面拿到 menus 匹配 path 得到当前的菜单，设置 tab 的标题
        const menus = this.menus
        let currentMenu: any = undefined
        let firstMenu: any = undefined
        menus.forEach((menu) => {
          menu.childs.forEach((subMenu: any) => {
            if (!firstMenu) {
              firstMenu = subMenu
            }
            if (subMenu.path === tab.path) {
              currentMenu = subMenu
              currentMenu.parent = menu
            }
          })
        })
        let tabList = this.tabList || []
        // 过滤已经不能显示的菜单
        tabList = tabList.filter((item) => {
          return (
            menus.filter((menu) => {
              return (
                menu.childs.filter((subMenu: any) => {
                  return subMenu.path === item.path
                }).length > 0
              )
            }).length > 0
          )
        })
        this.tabList = tabList
        if (!currentMenu) {
          // 打开第一个菜单
          resolve(firstMenu)
          return
        }
        // 获取下标 -1 表示可以添加 否则就是已经存在
        const index = tabList.findIndex((ele) => ele.key === tab.key)
        //
        tab.title = currentMenu.title
        tab.id = currentMenu.id
        tab.parentId = currentMenu.parent.id

        if (index > -1) {
          // 设置 activeTabKey
        } else {
          // 新增
          tabList.push(tab)
          this.tabList = tabList
          localStorage.setItem(TAB_LIST_KEY, JSON.stringify(tabList))
        }
        // 设置当前选择的菜单
        this.activeTabKey = tab.key
        this.activeMenu = tab.id
        resolve(true)
      })
    },
    // 删除 tab
    removeTab(key: string) {
      return new Promise((resolve) => {
        let tabList = this.tabList
        const index = tabList.findIndex((ele) => ele.key === key)
        tabList.splice(index, 1)

        this.tabList = tabList
        localStorage.setItem(TAB_LIST_KEY, JSON.stringify(tabList))
        // 如果删除的是 activeTabKey
        if (this.activeTabKey === key) {
          // 寻找下一个
          const tempTab = tabList[Math.min(index, 0)]
          // 如果还是原来激活的 tab 就不用更新
          if (this.activeTabKey !== tempTab.key) {
            this.activeTabKey = tempTab.key
            resolve(true)
          }
        }
      })
    },
    // 清除 tabs
    clearTabs({ key, position }: { key?: string; position?: string }) {
      return new Promise((resolve) => {
        let tabList = this.tabList
        key = key || this.activeTabKey
        if (key === 'all') {
          // 清空全部
          this.tabList = []
          this.activeTabKey = ''
          localStorage.setItem(TAB_LIST_KEY, JSON.stringify(this.tabList))
          return
        }
        // 找到当前 index
        const index = tabList.findIndex((ele) => ele.key === key)
        const currentTab = tabList[index]
        //console.log(index, key, state.activeTabKey, position);
        if (position === 'left') {
          // 关闭左侧
          tabList = tabList.slice(index, tabList.length)
        } else if (position === 'right') {
          // 关闭右侧
          tabList = tabList.slice(0, index + 1)
        } else {
          // 只保留当前
          tabList = [currentTab]
        }
        this.tabList = tabList
        localStorage.setItem(TAB_LIST_KEY, JSON.stringify(tabList))
        if (this.activeTabKey !== key) {
          this.activeTabKey = key
          resolve(key)
        }
      })
    },
    setActiveTabKey(key: string) {
      this.activeTabKey = key
      localStorage.setItem(ACTIVE_TAB_KEY, key)
    },
    // 选中当前菜单
    setActiveMenu(activeMenuKey: string) {
      this.activeMenuKey = activeMenuKey
      localStorage.setItem(ACTIVE_MENU_KEY, activeMenuKey)
    },

    // 打开的菜单
    setMenuOpenKeys(keys: string[] | string) {
      if (Array.isArray(keys)) {
        this.menuOpenKeys = keys
      } else {
        {
          const nowKeys = this.menuOpenKeys
          if (!nowKeys.includes(keys)) {
            nowKeys.push(keys)
            this.menuOpenKeys = nowKeys
          }
        }
      }
    }
  },
  getters: {
    getMenus(state) {
      return state.menus
    },
    getTabList(state) {
      return state.tabList
    },
    getActiveTabKey(state) {
      return state.activeTabKey
    },
    getActiveMenuKey(state) {
      return state.activeMenuKey
    },

    getMenuOpenKeys(state) {
      return state.menuOpenKeys
    }
  }
})

// export default useMenuStore()
