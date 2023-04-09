/**
 * 应用工作空间相关
 */
import { defineStore } from 'pinia'
import { CACHE_WORKSPACE_ID } from '@/utils/const'
import { useRoute } from 'vue-router'

const useAppStore = defineStore('app', {
  state: () => ({
    workspaceId: localStorage.getItem(CACHE_WORKSPACE_ID),
    // 菜单折叠
    isCollapsed: !!localStorage.getItem('collapsed'),
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
  },
  getters: {
    getWorkspaceId(state) {
      const route = useRoute()
      // let wid = router.app.$route.query.wid
      let wid = route.query.wid
      return wid || state.workspaceId
    },
  },
})

export default useAppStore()
