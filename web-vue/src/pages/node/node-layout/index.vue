<template>
  <a-layout class="node-layout">
    <!-- 侧边栏 节点管理菜单 -->
    <a-layout-sider theme="light" class="sider">
      <a-menu theme="light" mode="inline" :default-selected-keys="selectedKeys">
        <template v-for="menu in nodeMenuList">
          <a-sub-menu v-if="menu.childs" :key="menu.id">
            <span slot="title">
              <a-icon type="apartment" />
              <span>{{menu.title}}</span>
            </span>
            <a-menu-item v-for="subMenu in menu.childs" :key="subMenu.id" @click="handleMenuClick(subMenu.id)">
              <span>{{subMenu.title}}</span>
            </a-menu-item>
          </a-sub-menu>
          <a-menu-item v-else :key="menu.id" @click="handleMenuClick(menu.id)">
            <span>{{menu.title}}</span>
          </a-menu-item>
        </template>
      </a-menu>
    </a-layout-sider>
    <!-- 节点管理的各个组件 -->
    <a-layout-content class="layout-content">
      <welcome v-if="currentId === 'welcome'" :node="node" />
      <project-list v-if="currentId === 'manageList'" :node="node" />
    </a-layout-content>
  </a-layout>
</template>
<script>
import { getNodeMenu } from '../../../api/menu';
import Welcome from './welcome';
import ProjectList from './manage/project-list';
export default {
  components: {
    Welcome,
    ProjectList
  },
  props: {
    node: {
      type: Object
    }
  },
  data() {
    return {
      nodeMenuList: [],
      selectedKeys: ['welcome']
    }
  },
  computed: {
    currentId() {
      return this.selectedKeys[0];
    }
  },
  created() {
    this.loadNodeMenu()
  },
  methods: {
    // 加载菜单
    loadNodeMenu() {
      getNodeMenu(this.node.id).then(res => {
        if (res.code === 200) {
          this.nodeMenuList = res.data;
        }
      })
    },
    // 点击菜单
    handleMenuClick(id) {
      this.selectedKeys = [id];
    }
  }
}
</script>
<style scoped>
.sider {
  height: calc(100vh - 75px);
  overflow-y: auto;
}
.layout-content {
  height: calc(100vh - 95px);
  overflow-y: auto;
}
/* .node-layout {
  padding: 10px;
} */
</style>