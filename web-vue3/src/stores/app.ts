/**
 * 应用工作空间相关
 */
import { CACHE_WORKSPACE_ID } from '@/utils/const'
import { getHashQuery } from '@/utils/utils'
import { RouteLocationNormalized } from 'vue-router'
import { executionRequest } from '@/api/external'
import { parseTime, pageBuildInfo } from '@/utils/const'

export const useAppStore = defineStore('app', {
  state: () => ({
    workspaceId: localStorage.getItem(CACHE_WORKSPACE_ID),
    // 菜单折叠
    isCollapsed: localStorage.getItem('collapsed') === 'true',
    isShowInfo: false
  }),

  actions: {
    // 切换工作空间
    changeWorkspace(workspaceId: string) {
      this.workspaceId = workspaceId
      localStorage.setItem(CACHE_WORKSPACE_ID, workspaceId)
    },
    collapsed(isCollapsed: boolean) {
      this.isCollapsed = isCollapsed
      localStorage.setItem('collapsed', String(isCollapsed))
    },
    showInfo(to: RouteLocationNormalized) {
      if (this.isShowInfo) {
        return
      }
      this.isShowInfo = true
      // 控制台输出版本号信息
      const buildInfo = pageBuildInfo()
      executionRequest('https://jpom.top/docs/versions.show', { ...buildInfo, p: to.path })
        .then((data) => {
          console.log(
            '\n %c ' + parseTime(buildInfo.t) + ' %c vs %c ' + buildInfo.v + ' %c vs %c ' + data,
            'color: #ffffff; background: #f1404b; padding:5px 0;',
            'background: #1890ff; padding:5px 0;',
            'color: #ffffff; background: #f1404b; padding:5px 0;',
            'background: #1890ff; padding:5px 0;',
            'color: #ffffff; background: #f1404b; padding:5px 0;'
          )
        })
        .catch(() => {
          // 解锁
          this.isShowInfo = false
        })
    }
  },
  getters: {
    getWorkspaceId(state) {
      const query = getHashQuery()
      return query.wid || state.workspaceId
    },
    getCollapsed(state) {
      return state.isCollapsed
    }
  }
})

// export default useAppStore()
