<template>
  <a-config-provider :locale="zhCN">
    <div id="app">
      <router-view />
      <template>
        <a-back-top />
      </template>
    </div>
  </a-config-provider>
</template>

<script lang="ts">
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import { notification, message } from 'ant-design-vue'
import { onMounted, provide, computed } from 'vue'
import { useMenuStore } from '@/stores/menu'


onMounted(() => {
  notification.config({
    top: '100px',
    duration: 4,
  })

  message.config({ duration: 4 })
})

const reload = () => {
  const menuStore = useMenuStore()
  // 刷新菜单
  menuStore.restLoadSystemMenus();
}


provide('app', {
  reload,
})

</script>

<style lang="less">
#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #2c3e50;
  margin: 0;
  padding: 0;
}

.full-content {
  min-height: calc(100vh - 120px);
  padding-bottom: 20px;
}

.node-full-content {
  min-height: calc(100vh - 130px) !important;
}

.globalLoading {
  height: 100vh;
  width: 100vw;
  padding: 20vh;
  background-color: rgba(0, 0, 0, 0.7);
  position: fixed !important;
  z-index: 99999;
  top: 0;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
}

.ant-spin-text {
  text-shadow: 0 0 black !important;
}
</style>

<style>
.hide-scrollbar *::-webkit-scrollbar {
  width: 0 !important;
  display: none;
}

.hide-scrollbar * {
  -ms-overflow-style: none;
  scrollbar-width: none;
}

.hide-scrollbar pre::-webkit-scrollbar {
  width: 0 !important;
  display: none;
}

.hide-scrollbar pre {
  -ms-overflow-style: none;
  scrollbar-width: none;
}
</style>
