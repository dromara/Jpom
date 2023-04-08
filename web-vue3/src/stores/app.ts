/**
 * 应用相关的 store
 * 存储所有打开的 tab 窗口，并且记录当前激活的 tab
 * 另外提供打开新 tab、跳转 tab、移除 tab 功能
 */
import { defineStore } from 'pinia'

import { CACHE_WORKSPACE_ID } from '@/utils/const'
import { useRoute } from 'vue-router'

export const useAppStore = defineStore('app', {
	state: () => ({
		workspaceId: localStorage.getItem(CACHE_WORKSPACE_ID),
		// 菜单折叠
		isCollapsed: !!localStorage.getItem('collapsed'),
	}),

	actions: {
		// 切换工作空间
		changeWorkspace(workspaceId: string) {
			return new Promise((resolve) => {
				this.workspaceId = workspaceId
				localStorage.setItem(CACHE_WORKSPACE_ID, workspaceId)
				resolve(true)
			})
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
