<template>
  <a-layout id="app-layout">
    <a-layout-sider v-model:collapsed="collapsed" :theme="menuTheme" :trigger="null" collapsible>
      <a-layout-sider v-model:collapsed="collapsed" class="sider" :theme="menuTheme" :trigger="null" collapsible>
        <div class="sider-content">
          <a-tooltip placement="right" title="点击可以折叠左侧菜单栏">
            <div
              class="logo"
              :style="`color:${menuTheme === 'light' && theme === 'light' ? '#000' : '#fff'}`"
              @click="changeCollapsed()"
            >
              <img :src="logoUrl || defaultLogo" alt="logo" />
              {{ !collapsed ? subTitle : '' }}
            </div>
          </a-tooltip>
          <div class="sider-menu">
            <side-menu :mode="mode" :theme="menuTheme" />
          </div>
        </div>
      </a-layout-sider>
    </a-layout-sider>
    <a-layout>
      <div
        class="app-header"
        :class="{
          'app-header-dark': theme == 'dark'
        }"
        :style="{
          background: theme === 'light' ? '#fff' : '#141414'
        }"
      >
        <content-tab
          :mode="mode"
          :style="{
            width: collapsed ? 'calc(100vw - 80px - 20px)' : 'calc(100vw - 200px - 20px)'
          }"
        />
      </div>
      <a-layout-content
        :style="{
          width: collapsed ? 'calc(100vw - 80px)' : 'calc(100vw - 210px)',
          overflowY: 'auto',
          backgroundColor: theme === 'light' ? '#fff' : ''
        }"
        class="layout-content"
      >
        <router-view v-slot="{ Component, route }">
          <keep-alive :include="menuTabKeyList">
            <component :is="wrap(String(route.name), Component)" :key="String(route.name)" />
          </keep-alive>
        </router-view>
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>
<script setup lang="ts">
import SideMenu from './side-menu.vue'
// import UserHeader from "./user-header";
import ContentTab from './content-tab.vue'
import { checkSystem, loadingLogo } from '@/api/install'
import { useAppStore } from '@/stores/app'
import { useGuideStore } from '@/stores/guide'
import defaultLogo from '@/assets/images/jpom.svg'
import { useAllMenuStore } from '@/stores/menu2'

defineProps({
  mode: {
    type: String,
    required: true
  }
})

// 页面缓存对象
const wrapperMap = shallowRef(new Map())
// 组件套壳，动态添加name属性
const wrap = (name: string, component: any) => {
  let wrapper
  const wrapperName = name
  if (wrapperMap.value.has(wrapperName)) {
    wrapper = wrapperMap.value.get(wrapperName)
  } else {
    //包裹组件
    wrapper = {
      name: wrapperName,
      render() {
        return h('div', component)
      }
    }
    wrapperMap.value.set(wrapperName, wrapper)
  }
  return h(wrapper)
}
const menuStore = useAllMenuStore()
// 获取两个菜单中的tab key
const menuTabKeyList = computed(() => {
  return [...menuStore.normal_tabList, ...menuStore.management_tabList].map((item: { key: string }) => item.key)
})
// 监听menuTabKeyList变化
watch(
  menuTabKeyList,
  (newKeys, oldKeys) => {
    // 获取已被删除的key
    oldKeys
      ?.filter((key) => {
        return !newKeys.includes(key)
      })
      .forEach((key) => {
        // 删除缓存
        wrapperMap.value.delete(key)
      })
  },
  {
    immediate: true
  }
)

const collapsed = ref(false)
const subTitle = ref('项目运维')
const logoUrl = ref('')

const appStore = useAppStore()
const guideStore = useGuideStore()
onMounted(() => {
  checkSystemHannder()

  collapsed.value = appStore.getCollapsed
})

const router = useRouter()
// const route = useRoute()

const menuTheme = computed(() => {
  return guideStore.getMenuThemeView()
})

const theme = computed(() => {
  return guideStore.getThemeView()
})
const jpomWindow_ = jpomWindow()
// 检查是否需要初始化
const checkSystemHannder = () => {
  checkSystem().then((res) => {
    if (res.data) {
      jpomWindow_.routerBase = res.data.routerBase || ''
      if (res.data.subTitle) {
        subTitle.value = res.data.subTitle
      }

      // 禁用导航
      guideStore.commitGuide({
        disabledGuide: res.data.disabledGuide ? true : false,
        extendPlugins: res.data.extendPlugins as string[]
      })

      $notification.config({
        placement: res.data.notificationPlacement ? res.data.notificationPlacement : 'topRight'
      })
    }
    if (res.code !== 200) {
      $notification.warn({
        message: res.msg
      })
    }
    if (res.code === 999) {
      router.push('/prohibit-access')
    } else if (res.code === 222) {
      router.push('/install')
    }
  })

  loadingLogo().then((res) => {
    logoUrl.value = res.data || ''
  })
}
const changeCollapsed = () => {
  collapsed.value = !collapsed.value

  appStore.collapsed(collapsed.value)
}
</script>
<style scoped lang="less">
#app-layout {
  min-height: 100vh;
}

#app-layout .icon-btn {
  float: left;
  font-size: 18px;
  line-height: 64px;
  padding: 0 14px;
  cursor: pointer;
  transition: color 0.3s;
}
#app-layout .trigger:hover {
  color: #1890ff;
}
#app-layout .logo {
  flex-shrink: 0;
  width: 100%;
  cursor: pointer;
  height: 32px;
  margin: 20px 0 12px;
  font-size: 20px;

  font-weight: bold;
  overflow: hidden;
  padding: 0 16px;
  text-align: center;
}
#app-layout .logo img {
  height: 26px;
  vertical-align: sub;
}

.app-header {
  display: flex;
  /* background: #fff; */
  padding: 10px 10px 0;
  height: auto;
  position: sticky;
  top: 0;
  z-index: 10;
  // 背景色由计算属性实现
  // background: #fff;
  // border-bottom: 1px solid #eee;
  // box-shadow: 0 0px 8px 0px rgba(0, 0, 0, 0.18);
}

.sider {
  border-inline-end: 1px solid rgba(5, 5, 5, 0.06);
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
}
.sider-content {
  display: flex;
  flex-direction: column;
  height: 100vh;
}
.sider-menu {
  flex: 1;
  overflow-y: auto;
}
.layout-content {
  overflow-x: auto;
  padding: 15px;
  /* margin: 15px; */
}
/*
.sider-scroll {
  min-height: 100vh;
  overflow-y: auto;
}

.sider-full-screen {
  height: 100vh;
  overflow-y: scroll;
}

.layout-content-scroll {
  overflow-y: auto;
}

.layout-content-full-screen {
  height: calc(100vh - 120px);
  overflow-y: scroll;
} */
</style>

<style>
/* .layout-content { */
/* margin: 0; */
/* padding: 15px 15px 0; */
/* background: #fff; */
/* min-height: 280px; */
/* } */

/* .drawer-layout-content { */
/* min-height: calc(100vh - 85px); */
/* overflow-y: auto; */
/* } */
</style>
