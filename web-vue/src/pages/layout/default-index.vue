<template>
  <a-layout id="app-layout">
    <a-layout-sider class="sider" :theme="menuTheme" v-model:collapsed="collapsed" :trigger="null" collapsible>
      <a-tooltip placement="right" title="点击可以折叠左侧菜单栏">
        <div
          class="logo"
          @click="changeCollapsed()"
          :style="`color:${menuTheme === 'light' && theme === 'light' ? '#000' : '#fff'}`"
        >
          <img :src="logoUrl || defaultLogo" alt="logo" />
          {{ !collapsed ? subTitle : '' }}
        </div>
      </a-tooltip>
      <side-menu :mode="mode" :theme="menuTheme" />
    </a-layout-sider>
    <a-layout>
      <div class="app-header" :style="`background-color:${theme === 'light' ? '#fff' : ''}`">
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
          overflowY: 'scroll',
          backgroundColor: theme === 'light' ? '#fff' : ''
        }"
        class="layout-content"
      >
        <router-view v-slot="{ Component }">
          <keep-alive> <component :is="Component" /> </keep-alive>
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

defineProps({
  mode: {
    type: String,
    required: true
  }
})

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
const route = useRoute()

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
    } else {
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
<style scoped>
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
}

.sider {
  border-inline-end: 1px solid rgba(5, 5, 5, 0.06);
}
.layout-content {
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
