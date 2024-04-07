<template>
  <a-config-provider
    :locale="zhCN"
    :theme="{
      algorithm: themeAlgorithm
      // token: {
      //   colorBgLayout: token.colorBgContainer
      // }
    }"
  >
    <a-spin v-bind="globalLoadingProps">
      <router-view v-if="routerActivation" />
      <div v-if="pageloading" class="pageLoading"></div>
      <a-back-top />
    </a-spin>
  </a-config-provider>
</template>

<script setup lang="ts">
import zhCN from 'ant-design-vue/es/locale/zh_CN'
// TODO 国家化共用hooks抽离
// import enUS from 'ant-design-vue/es/locale/en_US'
import { theme } from 'ant-design-vue'
import { onMounted, onUnmounted } from 'vue'
import { SpinProps } from 'ant-design-vue/es/spin/Spin'
const routerActivation = ref(true)
const useGuideStore = guideStore()
const getGuideCache = useGuideStore.getGuideCache
// const { useToken } = theme
// const { token } = useToken()
// console.log(token.value)
//console.log(theme)

// 监听系统主题模式
const onMatchMediaChange = (e: MediaQueryListEvent) => {
  useGuideStore.setSystemIsDark(e.matches)
}
onMounted(() => {
  window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', onMatchMediaChange)
})

onUnmounted(() => {
  window.matchMedia('(prefers-color-scheme: dark)').removeEventListener('change', onMatchMediaChange)
})

// https://www.antdv.com/docs/vue/customize-theme-cn
// theme.defaultAlgorithm
// theme.darkAlgorithm
// theme.compactAlgorithm
const themeAlgorithm = computed(() => {
  const algorithm: any = []
  if (getGuideCache.compactView) {
    algorithm.push(theme.compactAlgorithm)
  }
  const themeDiy = useGuideStore.getThemeView()
  if (themeDiy === 'light') {
    algorithm.push(theme.defaultAlgorithm)
  } else if (themeDiy === 'dark') {
    algorithm.push(theme.darkAlgorithm)
  }

  return algorithm
})

const pageloading = ref(true)
const pageLoadingTimeout = ref()

const useAppStore = appStore()

const pageLoading = computed(() => {
  return useAppStore.loading
})
watch(pageLoading, (newValue) => {
  //
  if (newValue === 2) {
    clearTimeout(pageLoadingTimeout.value)
    globalLoading(false)
    pageloading.value = false
  } else {
    pageLoadingTimeout.value = setTimeout(() => {
      pageloading.value = true
      globalLoading({
        spinning: true,
        tip: '页面资源加载中....'
      })
    }, 500)
  }
})
// 打包后无效
// useAppStore.$subscribe((mutation, state) => {
//   const events: any = mutation.events
//   if (events && events.key === 'loading') {
//     if (events.newValue === 2) {
//       clearTimeout(pageLoadingTimeout.value)
//       globalLoading(false)
//       pageloading.value = false
//     } else {
//       pageLoadingTimeout.value = setTimeout(() => {
//         pageloading.value = true
//         globalLoading({
//           spinning: true,
//           tip: '页面资源加载中....'
//         })
//       }, 500)
//     }
//   }
// })

const reload = () => {
  routerActivation.value = false
  nextTick(() => {
    // const menuStore = useMenuStore()
    // 刷新菜单
    // menuStore.restLoadSystemMenus()
    routerActivation.value = true
  })
}

const globalLoadingProps = ref<SpinProps>({
  spinning: false,
  tip: '加载中...',
  size: 'large',
  delay: 500,
  wrapperClassName: ''
})

/**
 * 全局 loading
 * @param props 参数
 */
const globalLoading = (props: boolean | string | SpinProps) => {
  let newProps: any = {}
  if (typeof props === 'boolean') {
    newProps = { spinning: props }
  } else if (typeof props === 'string') {
    newProps = { tip: props }
  } else if (Object.prototype.toString.call(props) === '[object Object]') {
    newProps = props
  } else {
    console.error('不支持的类型', props, Object.prototype.toString.call(props))
  }
  // 避免无法弹窗
  newProps.wrapperClassName = newProps.spinning ? 'globalLoading' : ''
  globalLoadingProps.value = { ...globalLoadingProps.value, ...newProps }
}

provide('reload', reload)
provide('globalLoading', globalLoading)
</script>

<style lang="less">
#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  // color: #2c3e50;
  margin: 0;
  padding: 0;
}

.globalLoading {
  z-index: 99999;
  // background-color: #1f1f1f;
  // background-color: rgba(0, 0, 0, 0.7);
  background-color: rgba(140, 140, 140, 0.2);
  opacity: 0.8;
  height: 100vh;
}
</style>

<style scoped>
.pageLoading {
  position: absolute;
  display: flex;
  justify-content: center;
  align-items: center;
  flex-direction: column;
  min-height: 100vh;
  height: 100%;
  flex: 1;
}
</style>
