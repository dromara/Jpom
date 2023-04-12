<template>
  <a-tabs v-model="activeKey" class="my-tabs" hide-add type="editable-card" @edit="onEdit" @change="changeTab">
    <a-tab-pane v-for="(tab, index) in nowTabList" :key="tab.key" :closable="nowTabList.length > 1">
      <template slot="tab">
        <a-dropdown :trigger="['contextmenu']">
          <span style="display: inline-table">{{ tab.title }}</span>
          <a-menu slot="overlay">
            <a-menu-item
              @click="
                closeTabs({
                  key: tab.key
                })
              "
            >
              <a-button type="link" :disabled="nowTabList.length <= 1">关闭其他</a-button>
            </a-menu-item>
            <a-menu-item
              @click="
                closeTabs({
                  key: tab.key,
                  position: 'left'
                })
              "
            >
              <a-button type="link" :disabled="nowTabList.length <= 1 || index === 0">关闭左侧</a-button>
            </a-menu-item>
            <a-menu-item
              @click="
                closeTabs({
                  key: tab.key,
                  position: 'right'
                })
              "
            >
              <a-button type="link" :disabled="nowTabList.length <= 1 || index === nowTabList.length - 1"
                >关闭右侧</a-button
              >
            </a-menu-item>
          </a-menu>
        </a-dropdown>
      </template>
    </a-tab-pane>
    <template slot="tabBarExtraContent"> <user-header :mode="props.mode" /> </template>
  </a-tabs>
</template>
<script lang="ts" setup>
import UserHeader from './user-header'

import { mapGetters } from 'vuex'

const props = defineProps<{
  mode: string
}>()

const onEdit = (key, action) => {
  if (action === 'remove') {
    if (this.nowTabList.length === 1) {
      $notification.warn({
        message: '不能关闭了'
      })
      return
    }
    this.$store.dispatch(props.mode === 'normal' ? 'removeTab' : 'removeManagementTab', key).then(() => {
      this.activeTab()
    })
  }
}

// 关闭 tabs
const closeTabs = (data) => {
  $notification.success({
    message: '操作成功'
  })
  this.$store.dispatch(props.mode === 'normal' ? 'clearTabs' : 'clearManagementTabs', data).then(() => {
    this.activeTab()
  })
}

const activeTab = (key) => {
  key = key || this.activeKey
  const index = this.nowTabList.findIndex((ele) => ele.key === key)
  const activeTab = this.nowTabList[index]
  this.$router.push({
    query: { ...this.$route.query, sPid: activeTab.parentId, sId: activeTab.id },
    path: activeTab.path
  })
  //
  this.$store.dispatch(props.mode === 'normal' ? 'activeMenu' : 'activeManagementMenu', activeTab.id)
  this.$store.dispatch(props.mode === 'normal' ? 'menuOpenKeys' : 'menuManagementOpenKeys', activeTab.parentId)
  return activeTab
}

// computed: {
//     ...mapGetters(["getActiveTabKey", "getManagementActiveTabKey", "getTabList", "getManagementTabList", "getCollapsed"]),
//     activeKey: {
//       get() {
//         return props.mode === "normal" ? this.getActiveTabKey : this.getManagementActiveTabKey;
//       },
//       set(value) {
//         this.activeTab(value);
//       },
//     },
//     nowTabList() {
//       return props.mode === "normal" ? this.getTabList : this.getManagementTabList;
//     },
//   },
</script>
<style scoped>
.my-tabs {
  flex: auto;
  /* margin-right: 20px; */
  align-self: center;
  height: 40px;
}
</style>
