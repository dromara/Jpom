<template>
  <a-layout class="docker-layout">
    <a-layout-sider width="100px" style="min-height: calc(100vh - 85px)">
      <a-menu theme="light" mode="inline" class="docker-menu" v-model="menuKeyArray" @click="menuClick">
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
    </a-layout-sider>

    <a-layout class="layout-content drawer-layout-content">
      <a-layout-content>
        <swarm-node v-if="menuKey === 'node'" :id="this.id" />
        <swarm-service v-if="menuKey === 'server'" :id="this.id" />
        <swarm-task v-if="menuKey === 'task'" :id="this.id" />
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
  },
  components: {
    SwarmNode,
    SwarmService,
    SwarmTask,
  },
  data() {
    return {
      menuKeyArray: ["node"],
      menuKey: "node",
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
.docker-menu {
  height: 100%;
}
</style>
