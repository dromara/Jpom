import { ACTIVE_MENU_KEY, ACTIVE_TAB_KEY, TAB_LIST_KEY } from '@/utils/const'
import { MANAGEMENT_ACTIVE_TAB_KEY, MANAGEMENT_TAB_LIST_KEY, MANAGEMENT_ACTIVE_MENU_KEY } from '@/utils/const'

import { getMenu, getSystemMenu } from '@/api/menu'
import routeMenuMap from '@/router/route-menu'

const api: any = {
  normal: getMenu,
  management: getSystemMenu
}
const tabListKey: any = {
  normal: TAB_LIST_KEY,
  management: MANAGEMENT_TAB_LIST_KEY
}

const aTabKey: any = {
  normal: ACTIVE_TAB_KEY,
  management: MANAGEMENT_ACTIVE_TAB_KEY
}

const aMenuKey: any = {
  normal: ACTIVE_MENU_KEY,
  management: MANAGEMENT_ACTIVE_MENU_KEY
}

export const useAllMenuStore = defineStore('menu2', {
  state: (): any => ({
    normal_activeTabKey: localStorage.getItem(ACTIVE_TAB_KEY) || '',
    normal_tabList: localStorage.getItem(TAB_LIST_KEY) ? JSON.parse(localStorage.getItem(TAB_LIST_KEY)!) : [],
    normal_activeMenuKey: localStorage.getItem(ACTIVE_MENU_KEY) || '',
    normal_menuOpenKeys: [],
    normal_menus: [],
    management_activeTabKey: localStorage.getItem(MANAGEMENT_ACTIVE_TAB_KEY) || '',
    management_tabList: localStorage.getItem(MANAGEMENT_TAB_LIST_KEY)
      ? JSON.parse(localStorage.getItem(MANAGEMENT_TAB_LIST_KEY)!)
      : [],
    management_activeMenuKey: localStorage.getItem(MANAGEMENT_ACTIVE_MENU_KEY) || '',
    management_menuOpenKeys: [],
    management_menus: []
  }),
  actions: {
    // 加载系统菜单 action
    async loadSystemMenus(mode: any) {
      if (this[mode + '_menus'].length) {
        // 避免重复加载
        return true
      }
      return await this.restLoadSystemMenus(mode)
    },
    async restLoadSystemMenus(mode: any) {
      await api[mode]()
        .then((res: any) => {
          if (res.data) {
            res.data = res.data?.map((element: any) => {
              if (element.childs?.length > 0) {
                const childs = element.childs.map((child: any) => {
                  return {
                    ...child,
                    path: routeMenuMap[child.id]
                  }
                })
                element.childs = childs
              }
              element.path = routeMenuMap[element.id]
              return element
            })
            this[mode + '_menus'] = res.data

            Promise.resolve()
          } else {
            Promise.reject()
          }
        })
        .catch((error: any) => {
          Promise.reject(error)
        })
    },
    clearMenus(mode: any) {
      this[mode + '_menus'] = []
    },
    // 保存
    setTabList(mode: any, list: any) {
      this[mode + '_tabList'] = list
      localStorage.setItem(tabListKey[mode], JSON.stringify(list))
    },
    // 添加 tab
    addTab(mode: any, tab: any) {
      return new Promise((resolve) => {
        // 从 store 里面拿到 menus 匹配 path 得到当前的菜单，设置 tab 的标题
        const menus = this[mode + '_menus']
        let currentMenu: any = undefined
        let firstMenu: any = undefined
        menus.forEach((menu: any) => {
          if (menu.childs && menu.childs.length) {
            menu.childs.forEach((subMenu: any) => {
              if (!firstMenu) {
                firstMenu = subMenu
              }
              if (subMenu.path === tab.path) {
                currentMenu = subMenu
                currentMenu.parent = menu
              }
            })
          } else {
            if (!firstMenu) {
              firstMenu = menu
            }
            if (menu.path === tab.path) {
              currentMenu = menu
              // currentMenu.parent = menu;
            }
          }
        })
        let tabList = this[mode + '_tabList'] || []
        // 过滤已经不能显示的菜单
        tabList = tabList.filter((item: any) => {
          return (
            menus.filter((menu: any) => {
              return (
                menu.path == item.path ||
                menu.childs?.filter((subMenu: any) => {
                  return subMenu.path === item.path
                }).length > 0
              )
            }).length > 0
          )
        })
        this.setTabList(mode, tabList)

        if (!currentMenu) {
          // 打开第一个菜单
          resolve(firstMenu)
          return
        }
        // 获取下标 -1 表示可以添加 否则就是已经存在
        const index = tabList.findIndex((ele: any) => ele.key === tab.key)
        //
        tab.title = currentMenu.title
        tab.id = currentMenu.id
        tab.parentId = currentMenu.parent?.id

        if (index > -1) {
          // 设置 activeTabKey
        } else {
          // 新增

          tabList.push(tab)
          this.setTabList(mode, tabList)
        }
        // 设置当前选择的菜单
        // this.activeTabKey = tab.key
        this.activeTabKey(mode, tab.key)
        // this.activeMenu = tab.id
        this.activeMenu(mode, tab.id)
        resolve(true)
      })
    },
    // 删除 tab
    removeTab(mode: any, key: any) {
      return new Promise((resolve) => {
        let tabList = this[mode + '_tabList']
        const index = tabList.findIndex((ele: any) => ele.key === key)
        tabList.splice(index, 1)

        this.setTabList(mode, tabList)

        // 如果删除的是 activeTabKey
        if (this[mode + '_activeTabKey'] === key) {
          // 寻找下一个
          const tempTab = tabList[Math.min(index, 0)]
          // 如果还是原来激活的 tab 就不用更新
          if (this[mode + '_activeTabKey'] !== tempTab.key) {
            this.activeTabKey(mode, tempTab.key)
            resolve(true)
          }
        }
      })
    },
    // 清除 tabs
    clearTabs(mode: any, { key, position }: any) {
      return new Promise((resolve) => {
        let tabList = this[mode + '_tabList']
        key = key || this[mode + '_activeTabKey']
        if (key === 'all') {
          // 清空全部
          this.setTabList(mode, [])
          this.activeTabKey(mode, '')
          return
        }
        // 找到当前 index
        const index = tabList.findIndex((ele: any) => ele.key === key)
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
        this.setTabList(mode, tabList)
        //
        if (this[mode + '_activeTabKey'] !== key) {
          this.activeTabKey(mode, key)
          resolve(key)
        }
      })
    },
    activeTabKey(mode: any, key: any) {
      this[mode + '_activeTabKey'] = key
      localStorage.setItem(aTabKey[mode], key)
    },
    // 选中当前菜单
    activeMenu(mode: any, activeMenuKey: any) {
      this[mode + '_activeMenuKey'] = activeMenuKey
      localStorage.setItem(aMenuKey[mode], activeMenuKey)
    },
    // 打开的菜单
    menuOpenKeys(mode: any, keys: any) {
      if (Array.isArray(keys)) {
        this[mode + '_menuOpenKeys'] = keys
      } else if (typeof keys == 'string') {
        const nowKeys = this[mode + '_menuOpenKeys']
        if (!nowKeys.includes(keys)) {
          nowKeys.push(keys)
          // commit('setMenuOpenKeys', nowKeys)
          this[mode + '_menuOpenKeys'] = nowKeys
        }
      }
    }
  },
  getters: {
    getMenus(state) {
      return (mode: any) => {
        return state[mode + '_menus']
      }
    },
    getMenuOpenKeys(state) {
      return (mode: any) => {
        return state[mode + '_menuOpenKeys']
      }
    },
    getActiveMenuKey(state) {
      // return state.activeMenuKey
      return (mode: any) => {
        return state[mode + '_activeMenuKey']
      }
    },
    getActiveTabKey(state) {
      return (mode: any) => {
        return state[mode + '_activeTabKey']
      }
    },
    getTabList(state) {
      return (mode: any) => {
        return state[mode + '_tabList']
      }
    }
  }
})
