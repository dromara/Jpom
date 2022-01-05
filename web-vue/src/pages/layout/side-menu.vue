<template>
  <a-menu theme="dark" mode="inline" v-model="selectedKeys" :default-open-keys="defaultOpenKey">
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
      selectedKeys: [],
      // timer: null,
    };
  },
  computed: {
    ...mapGetters(["getMenus", "getActiveMenuKey"]),
    defaultOpenKey() {
      return [this.$route.query.sPid || "", this.$route.query.sId || ""];
    },
  },
  created() {
    this.activeMenu();
  },
  beforeDestroy() {
    // if (this.timer) {
    //   clearInterval(this.timer);
    // }
  },
  methods: {
    // 点击菜单
    handleClick(subMenu) {
      // console.log(sub);
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
    // 自动激活当前菜单
    activeMenu() {
      this.selectedKeys = [this.getActiveMenuKey];
    },
  },
};
</script>
