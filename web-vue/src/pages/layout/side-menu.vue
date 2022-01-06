<template>
  <a-menu theme="dark" mode="inline" v-model="selectedKeys" @openChange="openChange" :openKeys="getMenuOpenKeys">
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
    return {};
  },
  computed: {
    ...mapGetters(["getMenus", "getActiveMenuKey", "getMenuOpenKeys"]),
    selectedKeys: {
      get() {
        return [this.getActiveMenuKey];
      },
      set() {},
    },
  },
  created() {
    this.$store.dispatch("menuOpenKeys", this.$route.query.sPid || "");
  },
  beforeDestroy() {},
  methods: {
    openChange(keys) {
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
