<template>
  <div>
    <a-menu v-if="this.getUserInfo && this.getUserInfo.systemUser" theme="dark" mode="inline" v-model="mangerMenuOpenkeys" @click="mangerMenuClick" :openKeys="mangerMenuOpenkeys">
      <a-menu-item key="admin-manager">
        <template v-if="this.mode === 'normal'">
          <a-icon type="setting" :style="{ fontSize: '18px' }" />
          <span>系统管理</span>
        </template>
        <template v-if="this.mode === 'management'">
          <a-icon type="desktop" :style="{ fontSize: '18px' }" />
          <span>功能管理</span>
        </template>
      </a-menu-item>
    </a-menu>
    <a-menu theme="dark" mode="inline" v-model="selectedKeys" @openChange="openChange" :openKeys="getMenuOpenKeys2">
      <template v-for="menu in this.mode == 'normal' ? getMenus : getManagementMenus">
        <template v-if="menu.childs && menu.childs.length">
          <a-sub-menu :key="menu.id">
            <span slot="title">
              <a-icon :type="menu.icon_v3" :style="{ fontSize: '18px' }" />
              <span>{{ menu.title }}</span>
            </span>
            <a-menu-item v-for="subMenu in menu.childs" :key="subMenu.id" :p="(subMenu.parent = menu)" @click="handleClick(subMenu)">
              <span>{{ subMenu.title }}</span>
            </a-menu-item>
          </a-sub-menu>
        </template>
        <template v-else>
          <a-menu-item :key="menu.id" @click="handleClick(menu)">
            <a-icon :type="menu.icon_v3" :style="{ fontSize: '18px' }" />
            <span>{{ menu.title }}</span>
          </a-menu-item>
        </template>
      </template>
    </a-menu>
  </div>
</template>
<script>
import { mapGetters } from "vuex";
export default {
  props: {
    mode: {
      type: String,
    },
  },
  data() {
    return {
      menuOpenKeys: [],
      mangerMenuOpenkeys: [],
    };
  },
  computed: {
    ...mapGetters(["getMenus", "getManagementMenus", "getActiveMenuKey", "getManagementActiveMenuKey", "getMenuOpenKeys", "getManagementMenuOpenKeys", "getCollapsed", "getGuideCache", "getUserInfo"]),
    selectedKeys: {
      get() {
        return this.mode == "normal" ? [this.getActiveMenuKey] : [this.getManagementActiveMenuKey];
      },
      set() {},
    },
    getMenuOpenKeys2() {
      if (this.getCollapsed) {
        // 折叠的时候使用，用户点击的菜单
        return this.menuOpenKeys;
      }
      // 时候全局缓存的菜单
      return this.mode == "normal" ? this.getMenuOpenKeys : this.getManagementMenuOpenKeys;
    },
    menuMultipleFlag() {
      return this.getGuideCache.menuMultipleFlag === undefined ? true : this.getGuideCache.menuMultipleFlag;
    },
  },
  created() {
    this.$store.dispatch(this.mode == "normal" ? "menuOpenKeys" : "menuManagementOpenKeys", this.$route.query.sPid || "");
  },
  beforeDestroy() {},
  methods: {
    mangerMenuClick() {
      this.mangerMenuOpenkeys = [];
      this.$nextTick(() => {
        this.mangerMenuOpenkeys = [];
        this.$router.push({
          path: this.mode == "normal" ? "/system/management" : "/overview",
        });
      });
    },
    // 菜单打开
    openChange(keys) {
      if (keys.length && !this.menuMultipleFlag) {
        // 保留一个打开
        keys = [keys[keys.length - 1]];
      }
      this.menuOpenKeys = keys;
      this.$store.dispatch(this.mode == "normal" ? "menuOpenKeys" : "menuManagementOpenKeys", keys);
    },
    // 点击菜单
    handleClick(subMenu) {
      // 如果路由不存在
      if (!subMenu.path) {
        this.$notification.error({
          message: "路由无效，无法跳转",
        });
        return false;
      }
      // 如果跳转路由跟当前一致
      if (this.$route.path === subMenu.path) {
        // this.$notification.warn({
        //   message: "已经在当前页面了",
        // });
        return false;
      }
      // 跳转路由
      this.$router.push({
        query: { ...this.$route.query, sPid: subMenu.parent?.id, sId: subMenu.id },
        path: subMenu.path,
      });
      // this.$router.push()
    },
  },
};
</script>
