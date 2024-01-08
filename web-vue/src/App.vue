<template>
  <a-config-provider
    :locale="zhCN"
    :theme="{
      algorithm: themeAlgorithm
    }"
  >
    <div>
      <router-view v-if="routerActivation" />
      <template>
        <a-back-top />
      </template>
    </div>
  </a-config-provider>
</template>

<script setup lang="ts">
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import { theme } from 'ant-design-vue'
import { useGuideStore } from '@/stores/guide'
// theme.defaultAlgorithm
// theme.darkAlgorithm
// theme.compactAlgorithm
const routerActivation = ref(true)
const guideStore = useGuideStore()
const getGuideCache = guideStore.getGuideCache
// const scrollbarFlag = computed(() => {
//   return getGuideCache.scrollbarFlag ?? true
// })

const themeAlgorithm = computed(() => {
  const algorithm: any = []
  if (getGuideCache.compactView) {
    algorithm.push(theme.compactAlgorithm)
  }
  const themeDiy = guideStore.getThemeView()
  if (themeDiy === 'light') {
    algorithm.push(theme.defaultAlgorithm)
  } else if (themeDiy === 'dark') {
    algorithm.push(theme.darkAlgorithm)
  }
  console.log(algorithm)

  return algorithm
})

onMounted(() => {})

const reload = () => {
  routerActivation.value = false
  nextTick(() => {
    // const menuStore = useMenuStore()
    // 刷新菜单
    // menuStore.restLoadSystemMenus()
    routerActivation.value = true
  })
}

provide('reload', reload)
</script>

<style lang="less">
body {
  margin: 0;
}
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

@color-border-last: rgba(140, 140, 140, 1);
@color-neutral-last: rgba(140, 140, 140, 0.2);
@scrollbar-size: 5px;

// 兼容火狐
* {
  scrollbar-width: thin;
  scrollbar-color: @color-border-last @color-neutral-last;
}
// 滚动条样式
::-webkit-scrollbar {
  width: @scrollbar-size;
  height: @scrollbar-size;
  border-radius: @scrollbar-size;
  background-color: transparent;
}
// 滚动条-活动按钮
::-webkit-scrollbar-thumb {
  background: @color-border-last;
  border-radius: @scrollbar-size;
  box-shadow: outset 0 0 @scrollbar-size @color-border-last;
}
// 滚动条背景
::-webkit-scrollbar-track {
  background-color: @color-neutral-last;
  border-radius: @scrollbar-size;
  box-shadow: outset 0 0 @scrollbar-size @color-neutral-last;
}

.search-input-item {
  width: 140px;
  /* margin-right: 10px; */
}

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

.text-overflow-hidden {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
