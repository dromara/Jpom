<template>
  <a-layout class="node-layout">
    <a-layout-sider theme="light" class="sider">
      <a-menu theme="light" mode="inline">
        <template v-for="menu in nodeMenuList">
          <a-sub-menu v-if="menu.childs" :key="menu.id">
            <span slot="title">
              <a-icon type="apartment" />
              <span>{{menu.title}}</span>
            </span>
            <a-menu-item v-for="subMenu in menu.childs" :key="subMenu.id" @click="handleClick(subMenu.path)">
              <span>{{subMenu.title}}</span>
            </a-menu-item>
          </a-sub-menu>
          <a-menu-item v-else :key="menu.id" @click="handleClick(subMenu.path)">
            <span>{{menu.title}}</span>
          </a-menu-item>
        </template>
      </a-menu>
    </a-layout-sider>
    <a-layout-content class="layout-content">

    </a-layout-content>
  </a-layout>
</template>
<script>
import { getNodeMenu } from '../../../api/menu';
export default {
  data() {
    return {
      nodeMenuList: []
    }
  },
  created() {
    this.loadNodeMenu()
  },
  methods: {
    // 加载菜单
    loadNodeMenu() {
      getNodeMenu('test1').then(res => {
        if (res.code === 200) {
          this.nodeMenuList = res.data;
        }
      })
    }
  }
}
</script>
<style scoped>
.sider {
  height: calc(100vh - 75px);
}
/* .node-layout {
  padding: 10px;
} */
</style>