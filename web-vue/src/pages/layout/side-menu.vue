<template>
  <div>
    <a-menu
      :theme="theme"
      v-if="getUserInfo && getUserInfo.systemUser"
      mode="inline"
      v-model:value="mangerMenuOpenkeys"
      @click="mangerMenuClick"
      :openKeys="mangerMenuOpenkeys"
    >
      <a-menu-item key="admin-manager">
        <template v-if="mode === 'normal'">
          <SettingOutlined :style="{ fontSize: '18px' }" />

          <span>系统管理</span>
        </template>
        <template v-if="mode === 'management'">
          <DesktopOutlined :style="{ fontSize: '18px' }" />

          <span>功能管理</span>
        </template>
      </a-menu-item>
    </a-menu>
    <a-menu
      :theme="theme"
      mode="inline"
      v-model:selectedKeys="selectedKeys"
      @openChange="openChange"
      :openKeys="getMenuOpenKeys2"
    >
      <template v-for="menu in getMenus">
        <template v-if="menu.childs && menu.childs.length">
          <a-sub-menu :key="menu.id">
            <template v-slot:title>
              <span>
                <icon :type="menu.icon_v3" :style="{ fontSize: '18px' }" />
                <span>{{ menu.title }}</span>
              </span>
            </template>
            <a-menu-item
              v-for="subMenu in menu.childs"
              :key="subMenu.id"
              :p="(subMenu.parent = menu)"
              @click="handleClick(subMenu)"
            >
              <span>{{ subMenu.title }}</span>
            </a-menu-item>
          </a-sub-menu>
        </template>
        <template v-else>
          <a-menu-item :key="menu.id" @click="handleClick(menu)">
            <icon :type="menu.icon_v3" :style="{ fontSize: '18px' }" />
            <span>{{ menu.title }}</span>
          </a-menu-item>
        </template>
      </template>
    </a-menu>
  </div>
</template>

<script>
import { mapState } from 'pinia'
import Icon from '@/components/Icon'
import { useAllMenuStore } from '@/stores/menu2'
import { useUserStore } from '@/stores/user'
import { useGuideStore } from '@/stores/guide'
export default {
  components: {
    Icon
  },
  props: {
    mode: {
      type: String
    },
    theme: {
      type: String
    }
  },
  data() {
    return {
      mangerMenuOpenkeys: []
    }
  },
  computed: {
    ...mapState(useUserStore, ['getUserInfo']),
    ...mapState(useGuideStore, ['getGuideCache', 'getCollapsed']),
    selectedKeys: {
      get() {
        return [useAllMenuStore().getActiveMenuKey(this.mode)]
      },
      set() {}
    },
    getMenus() {
      return useAllMenuStore().getMenus(this.mode)
    },
    getMenuOpenKeys2() {
      if (this.getCollapsed) {
        // 折叠的时候使用，用户点击的菜单
        return this.menuOpenKeys
      }
      // 时候全局缓存的菜单
      return useAllMenuStore().getMenuOpenKeys(this.mode)
    },
    menuMultipleFlag() {
      return this.getGuideCache.menuMultipleFlag === undefined ? true : this.getGuideCache.menuMultipleFlag
    }
  },
  created() {
    useAllMenuStore().menuOpenKeys(this.mode, this.$route.query.sPid || '')
  },
  beforeUnmount() {},
  methods: {
    mangerMenuClick() {
      this.mangerMenuOpenkeys = []
      this.$nextTick(() => {
        this.mangerMenuOpenkeys = []
        this.$router.push({
          path: this.mode == 'normal' ? '/system/management' : '/overview'
        })
      })
    },
    // 菜单打开
    openChange(keys) {
      if (keys.length && !this.menuMultipleFlag) {
        // 保留一个打开
        keys = [keys[keys.length - 1]]
      }

      useAllMenuStore().menuOpenKeys(this.mode, keys)
    },
    // 点击菜单
    handleClick(subMenu) {
      // 如果路由不存在
      if (!subMenu.path) {
        $notification.error({
          message: '路由无效，无法跳转'
        })
        return false
      }
      // 如果跳转路由跟当前一致
      if (this.$route.path === subMenu.path) {
        // $notification({
        //   message: "已经在当前页面了",
        // });
        return false
      }
      // 跳转路由
      this.$router.push({
        query: {
          ...this.$route.query,
          sPid: subMenu.parent?.id,
          sId: subMenu.id
        },
        path: subMenu.path
      })
      // this.$router.push()
    }
  }
}
</script>
