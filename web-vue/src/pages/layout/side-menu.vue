<template>
  <a-menu theme="dark" mode="inline" v-model="selectedKeys" @openChange="openChange" :openKeys="getMenuOpenKeys2">
    <a-sub-menu v-for="menu in getMenus" :key="menu.id">
      <span slot="title">
        <a-icon :type="menu.icon_v3" :style="{ fontSize: '18px' }" />
        <span>{{ menu.title }}</span>
      </span>
      <a-menu-item v-for="subMenu in menu.childs" :key="subMenu.id" :p="(subMenu.parent = menu)" @click="handleClick(subMenu)">
        <span>{{ subMenu.title }}</span>
      </a-menu-item>
    </a-sub-menu>
  </a-menu>
</template>
<script>
import { mapGetters } from "vuex";
export default {
  data() {
    return {
      menuOpenKeys: [],
    };
  },
  computed: {
    ...mapGetters(["getMenus", "getActiveMenuKey", "getMenuOpenKeys", "getCollapsed", "getGuideCache"]),
    selectedKeys: {
      get() {
        return [this.getActiveMenuKey];
      },
      set() {},
    },
    getMenuOpenKeys2() {
      if (this.getCollapsed) {
        // 折叠的时候使用，用户点击的菜单
        return this.menuOpenKeys;
      }
      // 时候全局缓存的菜单
      return this.getMenuOpenKeys;
    },
    menuMultipleFlag() {
      return this.getGuideCache.menuMultipleFlag === undefined ? true : this.getGuideCache.menuMultipleFlag;
    },
  },
  created() {
    this.$store.dispatch("menuOpenKeys", this.$route.query.sPid || "");
  },
  beforeDestroy() {},
  methods: {
    openChange(keys) {
      if (keys.length && !this.menuMultipleFlag) {
        // 保留一个打开
        keys = [keys[keys.length - 1]];
      }
      this.menuOpenKeys = keys;
      this.$store.dispatch("menuOpenKeys", keys);
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
        this.$notification.warn({
          message: "已经在当前页面了",
        });
        return false;
      }
      // 跳转路由
      this.$router.push({
        query: { ...this.$route.query, sPid: subMenu.parent.id, sId: subMenu.id },
        path: subMenu.path,
      });
      // this.$router.push()
    },
  },
};
</script>
