<template>
  <a-layout id="app-layout">
    <a-layout-sider
      v-model="collapsed"
      :trigger="null"
      collapsible
      :class="`${fullScreenFlag ? 'sider-scroll' : 'sider-full-screen'}`"
    >
      <a-tooltip placement="right" title="点击可以折叠左侧菜单栏">
        <div class="logo" @click="changeCollapsed()">
          <img :src="logoUrl" />
          {{ subTitle }}
        </div>
      </a-tooltip>
      <side-menu class="side-menu" :mode="mode" />
    </a-layout-sider>
    <a-layout>
      <a-layout-header class="app-header">
        <content-tab :mode="mode" />
      </a-layout-header>
      <a-layout-content
        :class="`layout-content ${fullScreenFlag ? 'layout-content-scroll' : 'layout-content-full-screen'}`"
      >
        <keep-alive>
          <router-view />
        </keep-alive>
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>
<script setup lang="ts">
import SideMenu from './side-menu'
// import UserHeader from "./user-header";
import ContentTab from './content-tab'
import { checkSystem } from '@/api/install'
import { executionRequest } from '@/api/external'
import { parseTime, pageBuildInfo } from '@/utils/const'
import { useAppStore } from '@/stores/app'
import { useGuideStore } from '@/stores/guide'

defineProps({
  mode: {
    type: String,
    required: true
  }
})

const collapsed = ref(false)
const subTitle = ref('项目管理')
const logoUrl = ref('')
const fullScreenFlag = ref(false)
const appStore = useAppStore()
const guideStore = useGuideStore()
onMounted(() => {
  checkSystemHannder()

  collapsed.value = appStore.getCollapsed
})

const router = useRouter()
const route = useRoute()

// 检查是否需要初始化
const checkSystemHannder = () => {
  checkSystem().then((res) => {
    if (res.data) {
      jpomWindow.routerBase = res.data.routerBase || ''
      if (res.data.subTitle) {
        subTitle.value = res.data.subTitle
      }
      logoUrl.value = ((res.data.routerBase || '') + '/logo_image').replace(new RegExp('//', 'gm'), '/')

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
      router.push('/system/ipAccess')
    } else if (res.code === 222) {
      router.push('/install')
    }
  })
  // 控制台输出版本号信息
  const buildInfo = pageBuildInfo()
  executionRequest('https://jpom.top/docs/versions.show', { ...buildInfo, p: route.path }).then((data) => {
    console.log(
      '\n %c ' + parseTime(buildInfo.t) + ' %c vs %c ' + buildInfo.v + ' %c vs %c ' + data,
      'color: #ffffff; background: #f1404b; padding:5px 0;',
      'background: #1890ff; padding:5px 0;',
      'color: #ffffff; background: #f1404b; padding:5px 0;',
      'background: #1890ff; padding:5px 0;',
      'color: #ffffff; background: #f1404b; padding:5px 0;'
    )
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
  color: #fff;
  font-weight: bold;
  overflow: hidden;
  padding: 0 16px;
}

#app-layout .logo img {
  height: 26px;
  vertical-align: sub;
  margin-right: 6px;
}

.app-header {
  display: flex;
  background: #fff;
  padding: 10px 10px 0;
  height: auto;
}

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
}
</style>

<style>
.layout-content {
  margin: 0;
  padding: 15px 15px 0;
  background: #fff;
  /* min-height: 280px; */
}

.drawer-layout-content {
  min-height: calc(100vh - 85px);
  overflow-y: auto;
}
</style>
