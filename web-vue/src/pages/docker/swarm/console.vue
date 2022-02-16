<template>
  <a-layout class="docker-layout">
    <a-layout-header style="height: 48px; padding: 0">
      <a-menu theme="light" mode="horizontal" class="docker-menu" v-model="menuKeyArray" @click="menuClick">
        <a-menu-item key="node">
          <span class="nav-text">集群节点</span>
        </a-menu-item>
        <a-menu-item key="server">
          <span class="nav-text">集群服务</span>
        </a-menu-item>
        <a-menu-item key="task">
          <span class="nav-text">集群任务</span>
        </a-menu-item>
      </a-menu>
    </a-layout-header>

    <a-layout class="layout-content drawer-layout-content">
      <a-layout-content>
        <swarm-node v-if="menuKey === 'node'" :id="this.id" :visible="this.visible" />
        <swarm-service v-if="menuKey === 'server'" :id="this.id" :visible="this.visible" />
        <swarm-task v-if="menuKey === 'task'" :id="this.id" :visible="this.visible" />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>
<script>
import SwarmNode from "./node";
import SwarmService from "./service";
import SwarmTask from "./task";
export default {
  props: {
    id: {
      type: String,
    },
    initMenu: {
      type: String,
      default: "node",
    },
    visible: {
      type: Boolean,
      default: false,
    },
  },
  components: {
    SwarmNode,
    SwarmService,
    SwarmTask,
  },
  data() {
    return {
      menuKeyArray: [],
      menuKey: "",
    };
  },
  mounted() {
    this.menuKey = this.initMenu;
    this.menuKeyArray = [this.initMenu];
  },
  methods: {
    menuClick(item) {
      this.menuKey = item.key;
    },
  },
};
</script>
<style scoped>
.drawer-layout-content {
  min-height: calc(100vh - 133px);
}
</style>
