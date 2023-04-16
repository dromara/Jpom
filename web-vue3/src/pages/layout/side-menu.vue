<template>
  <div>
    <a-menu v-if="userInfo && userInfo.systemUser" theme="dark" mode="inline" v-model="mangerMenuOpenkeys"
      @click="mangerMenuClick" :openKeys="mangerMenuOpenkeys">
      <a-menu-item key="admin-manager">
        <template v-if="props.mode === 'normal'">
          <icon type="setting" :style="{ fontSize: '18px' }" />
          <span>系统管理</span>
        </template>
        <template v-if="props.mode === 'management'">
          <icon type="desktop" :style="{ fontSize: '18px' }" />
          <span>功能管理</span>
        </template>
      </a-menu-item>
    </a-menu>
    <a-menu theme="dark" mode="inline" v-model="selectedKeys" @openChange="openChange" :openKeys="getMenuOpenKeys2">
      <a-sub-menu v-for="menu in menus" :key="menu.id">
        <template #title>
          <icon :type="menu.icon_v3" :style="{ fontSize: '18px' }" />
          <span>{{ menu.title }}</span>
        </template>
        <a-menu-item v-for="subMenu in menu.childs" :key="subMenu.id" :p="(subMenu.parent = menu)"
          @click="handleClick(subMenu)">
          <span>{{ subMenu.title }}</span>
        </a-menu-item>
      </a-sub-menu>
    </a-menu>
  </div>
</template>
<script lang="ts" setup>
import Icon from '@/components/Icon'
import { useAppStore } from '@/stores/app'
import { useGuideStore } from '@/stores/guide'
import { useManagementMenuStore } from '@/stores/management-menu'
import { useMenuStore } from '@/stores/menu'
import { useUserStore } from '@/stores/user'

const props = defineProps<{
  mode: string
}>()

const menuStore = useMenuStore()
const managementMenuStore = useManagementMenuStore()
const userStore = useUserStore()
const guideStore = useGuideStore()
const appStore = useAppStore()

const lastMenuStore = props.mode == "normal" ? menuStore : managementMenuStore

const route = useRoute()
const router = useRouter()

const menuOpenKeys = ref<string[]>([])
const mangerMenuOpenkeys = ref([])

const menus = computed(() => lastMenuStore.getMenus)

const getMenuOpenKeys2 = computed(() => {

  if (appStore.getCollapsed) {
    // 折叠的时候使用，用户点击的菜单
    return menuOpenKeys.value;
  }
  // 时候全局缓存的菜单
  return lastMenuStore.getMenuOpenKeys;
})

const { userInfo } = toRefs(userStore)

const mangerMenuClick = () => {
  mangerMenuOpenkeys.value = []
  nextTick(() => {
    mangerMenuOpenkeys.value = []
    router.push({
      path: props.mode == 'normal' ? '/system/management' : '/node/list'
    })
  })
}

// 菜单打开
const openChange = (keys: string[]) => {
  const menuMultipleFlag = guideStore.getGuideCache.menuMultipleFlag
  if (keys.length && !menuMultipleFlag) {
    // 保留一个打开
    keys = [keys[keys.length - 1]]
  }
  menuOpenKeys.value = keys

  lastMenuStore.menuOpenKeys = keys
}

// 点击菜单
const handleClick = (subMenu: any) => {
  // 如果路由不存在
  if (!subMenu.path) {
    $notification.error({
      message: '路由无效，无法跳转'
    })
    return false
  }

  // 如果跳转路由跟当前一致
  if (route.path === subMenu.path) {
    return false
  }
  // 跳转路由
  router.push({
    query: { ...route.query, sPid: subMenu.parent.id, sId: subMenu.id },
    path: subMenu.path
  })
}

const selectedKeys = computed(() => {
  return lastMenuStore.getActiveMenuKey
})

onMounted(() => {
  lastMenuStore.menuOpenKeys = ([route.query.sPid] as string[]) || ['']
})
</script>
