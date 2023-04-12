/**
 * 应用工作空间相关
 */
import { CACHE_WORKSPACE_ID } from '@/utils/const'
import { getHashQuery } from '@/utils/utils'

export const useAppStore = defineStore('app', {
  state: () => ({
    workspaceId: localStorage.getItem(CACHE_WORKSPACE_ID),
    // 菜单折叠
    isCollapsed: !!localStorage.getItem('collapsed')
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
