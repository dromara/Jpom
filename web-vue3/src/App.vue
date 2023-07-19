<template>
  <a-config-provider :locale='locale'>
    <div :class="`${scrollbarFlag ? '' : 'hide-scrollbar'}`">
      <router-view v-if='routerActivation' />
      <template>
        <a-back-top />
      </template>
    </div>
  </a-config-provider>
</template>

<script setup lang='ts'>
import { useMenuStore } from '@/stores/menu'
import { useGuideStore } from '@/stores/guide'
import { useLocaleStore } from '@/stores/locale'
import enUS from 'ant-design-vue/es/locale/en_US'
import zhCN from 'ant-design-vue/es/locale/zh_CN'

const localeStore = useLocaleStore()
const routerActivation = ref(true)
const guideStore = useGuideStore().getGuideCache
const scrollbarFlag = computed(() => {
  return guideStore.scrollbarFlag ?? true
})
const locale = computed(() => {
  switch (localeStore.getLocale) {
    case 'en':
      return enUS
    case 'zh':
    default:
      return zhCN
  }
})

onMounted(() => { })

const reload = () => {
  routerActivation.value = false
  nextTick(() => {
    const menuStore = useMenuStore()
    // 刷新菜单
    menuStore.restLoadSystemMenus()
    routerActivation.value = true
  })
}

provide('reload', reload)
</script>

<style lang='less'>
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

.search-wrapper {
  margin-bottom: 20px;
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
