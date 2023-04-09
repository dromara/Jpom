<template>
  <div>
    <a-menu v-if="userInfo && userInfo.systemUser" theme="dark" mode="inline" v-model="mangerMenuOpenkeys"
      @click="mangerMenuClick" :openKeys="mangerMenuOpenkeys">
      <a-menu-item key="admin-manager">
        <template v-if="props.mode === 'normal'">
          <a-icon type="setting" :style="{ fontSize: '18px' }" />
          <span>系统管理</span>
        </template>
        <template v-if="props.mode === 'management'">
          <a-icon type="desktop" :style="{ fontSize: '18px' }" />
          <span>功能管理</span>
        </template>
      </a-menu-item>
    </a-menu>
    <a-menu theme="dark" mode="inline" v-model="selectedKeys" @openChange="openChange" :openKeys="getMenuOpenKeys2">
      <a-sub-menu v-for="menu in props.mode == 'normal' ? getMenus : getManagementMenus" :key="menu.id">
        <span slot="title">
          <a-icon :type="menu.icon_v3" :style="{ fontSize: '18px' }" />
          <span>{{ menu.title }}</span>
        </span>
        <a-menu-item v-for="subMenu in menu.childs" :key="subMenu.id" :p="(subMenu.parent = menu)"
          @click="handleClick(subMenu)">
          <span>{{ subMenu.title }}</span>
        </a-menu-item>
      </a-sub-menu>
    </a-menu>
  </div>
</template>
<script lang="ts" setup>
import { useMenuStore } from '@/stores/menu';
import { useUserStore } from '@/stores/user';
import { notification } from 'ant-design-vue';
import { computed, nextTick, onMounted, ref, toRefs } from 'vue';
import { useRoute, useRouter } from 'vue-router';

const props = defineProps<{
  mode: string;
}>();

const menuStore = useMenuStore()
const userStore = useUserStore()

const route = useRoute()
const router = useRouter()

const menuOpenKeys = ref<string[]>([])
const menuManagementOpenKeys = ref([])
const mangerMenuOpenkeys = ref([])

const { userInfo } = toRefs(userStore)


const mangerMenuClick = () => {
  mangerMenuOpenkeys.value = [];
  nextTick(() => {
    mangerMenuOpenkeys.value = []
    router.push({
      path: props.mode == "normal" ? "/system/management" : "/node/list",
    });
  })
}
// 菜单打开
const openChange = (keys: string[]) => {
  if (keys.length && !this.menuMultipleFlag) {
    // 保留一个打开
    keys = [keys[keys.length - 1]];
  }
  menuOpenKeys.value = keys;
  menuStore[props.mode == "normal" ? "menuOpenKeys" : "menuManagementOpenKeys"] = keys
}


// 点击菜单
const handleClick = (subMenu: any) => {
  // 如果路由不存在
  if (!subMenu.path) {
    notification.error({
      message: "路由无效，无法跳转",
    });
    return false;
  }

  // 如果跳转路由跟当前一致
  if (route.path === subMenu.path) {
    return false;
  }
  // 跳转路由
  router.push({
    query: { ...route.query, sPid: subMenu.parent.id, sId: subMenu.id },
    path: subMenu.path,
  });
}

const selectedKeys = computed(() => {
  // todo 后者需要替换成管理员menu manageMenuStore.getManagementActiveMenuKey
  return props.mode == "normal" ? [menuStore.getActiveMenuKey] : [];
})

onMounted(() => {
  const key = props.mode == "normal" ? "menuOpenKeys" : "menuManagementOpenKeys"
  menuStore[key] = route.query.sPid || ""
})

// export default {

  // computed: {
  //   ...mapGetters(["getMenus", "getManagementMenus", "getActiveMenuKey", "getManagementActiveMenuKey", "getMenuOpenKeys", "getManagementMenuOpenKeys", "getCollapsed", "getGuideCache", "getUserInfo"]),
  //   selectedKeys: {
  //     get() {
  //       return props.mode == "normal" ? [this.getActiveMenuKey] : [this.getManagementActiveMenuKey];
  //     },
  //     set() { },
  //   },
  //   getMenuOpenKeys2() {
  //     if (this.getCollapsed) {
  //       // 折叠的时候使用，用户点击的菜单
  //       return this.menuOpenKeys;
  //     }
  //     // 时候全局缓存的菜单
  //     return props.mode == "normal" ? this.getMenuOpenKeys : this.getManagementMenuOpenKeys;
  //   },
  //   menuMultipleFlag() {
  //     return this.getGuideCache.menuMultipleFlag === undefined ? true : this.getGuideCache.menuMultipleFlag;
  //   },
  // },
  // created() {
  //
  // },
// };
</script>
