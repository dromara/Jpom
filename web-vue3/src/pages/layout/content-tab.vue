<template>
  <a-tabs v-model:activeKey="activeTabKey" class="my-tabs" hide-add type="editable-card" @edit="onEdit">
    <a-tab-pane v-for="(tab, index) in tabList" :key="tab.key" :closable="tabList.length > 1">
      <template #tab>
        <a-dropdown :trigger="['contextmenu']">
          <span style="display: inline-table">{{ tab.title }}</span>
          <template #overlay>
            <a-menu>
              <a-menu-item
                @click="
                  closeTabs({
                    key: tab.key
                  })
                "
              >
                <a-button type="link" :disabled="tabList.length <= 1">关闭其他</a-button>
              </a-menu-item>
              <a-menu-item
                @click="
                  closeTabs({
                    key: tab.key,
                    position: 'left'
                  })
                "
              >
                <a-button type="link" :disabled="tabList.length <= 1 || index === 0">关闭左侧</a-button>
              </a-menu-item>
              <a-menu-item
                @click="
                  closeTabs({
                    key: tab.key,
                    position: 'right'
                  })
                "
              >
                <a-button type="link" :disabled="tabList.length <= 1 || index === tabList.length - 1"
                  >关闭右侧</a-button
                >
              </a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
      </template>
    </a-tab-pane>
    <template #rightExtra> <user-header :mode="props.mode" /> </template>
  </a-tabs>
</template>
<script lang="ts" setup>
import userHeader from './user-header.vue'
import { useMenuStore } from '@/stores/menu'
import { useManagementMenuStore } from '@/stores/management-menu'

const router = useRouter()
const route = useRoute()

const props = defineProps<{
  mode: string
}>()

const menuStore = props.mode === 'normal' ? useMenuStore() : useManagementMenuStore()

const { activeTabKey, tabList } = toRefs(menuStore)

const activeTab = (key?: string) => {
  key = key || activeTabKey.value
  const index = tabList.value.findIndex((ele) => ele.key === key)
  const activeTab = tabList.value[index]
  router.push({
    query: { ...route.query, sPid: activeTab.parentId, sId: activeTab.id },
    path: activeTab.path
  })

  menuStore.activeMenu = activeTab.id
  menuStore.menuOpenKeys = activeTab.paren
  return activeTab
}

const onEdit = (key: string, action: 'remove') => {
  if (action === 'remove') {
    if (tabList.value.length === 1) {
      $notification.warn({
        message: '不能关闭了'
      })
      return
    }
    menuStore.removeTab(key).then(() => {
      activeTab()
    })
  }
}

// 关闭 tabs
const closeTabs = (data) => {
  $notification.success({
    message: '操作成功'
  })
  menuStore.clearTabs(data).then(() => {
    activeTab()
  })
}
</script>
<style scoped>
.my-tabs {
  flex: auto;
  align-self: center;
  height: 40px;
}
</style>
