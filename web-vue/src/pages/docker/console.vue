<template>
  <a-layout class="docker-layout">
    <a-layout-sider breakpoint="xs" width="100px">
      <a-menu theme="light" mode="vertical-right" class="docker-menu" v-model="menuKeyArray" @click="menuClick">
        <a-menu-item key="containers">
          <span class="nav-text">容器</span>
        </a-menu-item>
        <a-menu-item key="containers1">
          <span class="nav-text">容器1</span>
        </a-menu-item>
      </a-menu>
    </a-layout-sider>
    <a-layout :style="{ background: '#fff', padding: '0 5px' }">
      <!-- <a-layout-header :style="{ background: '#fff', padding: 0 }" /> -->
      <!-- :style="{ margin: '24px 16px 0' }" -->
      <a-layout-content><Container v-if="menuKey === 'containers'" :id="this.id" /> </a-layout-content>
    </a-layout>
  </a-layout>
</template>
<script>
import Container from "./container.vue";
export default {
  props: {
    id: {
      type: String,
    },
  },
  components: {
    Container,
  },
  data() {
    return {
      menuKeyArray: ["containers"],
      menuKey: "containers",
    };
  },
  mounted() {},
  methods: {
    menuClick(item) {
      this.menuKey = item.key;
    },
  },
};
</script>
<style scoped>
.docker-layout {
  background-color: #fff;
}
.docker-menu {
  height: 100%;
}
</style>
