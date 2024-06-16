<template>
  <CustomDrawer
    destroy-on-close
    placement="right"
    :width="`${getCollapsed ? 'calc(100vw - 80px)' : 'calc(100vw - 200px)'}`"
    :open="true"
    :body-style="{
      padding: '0'
    }"
    :header-style="{
      padding: '0 10px'
    }"
    @close="onClose1"
  >
    <template #title>
      <!-- 集群控制台 -->
      <a-menu v-model:selectedKeys="menuKeyArray" mode="horizontal" class="docker-menu" @click="menuClick">
        <a-menu-item key="node">
          <span class="nav-text">{{ $t('pages.docker.swarm.console.198377d7') }}</span>
        </a-menu-item>
        <a-menu-item key="server">
          <span class="nav-text">{{ $t('pages.docker.swarm.console.5dd8de32') }}</span>
        </a-menu-item>
        <a-menu-item key="task">
          <span class="nav-text">{{ $t('pages.docker.swarm.console.ce8244b7') }}</span>
        </a-menu-item>
      </a-menu>
    </template>

    <!-- <a-layout-header style="height: 48px; padding: 0"> </a-layout-header> -->

    <div class="layout-content">
      <swarm-node v-show="menuKey === 'node'" :id="id" :visible="visible" :url-prefix="urlPrefix" />
      <swarm-service v-show="menuKey === 'server'" :id="id" :visible="visible" :url-prefix="urlPrefix" />
      <swarm-task v-show="menuKey === 'task'" :id="id" :visible="visible" :url-prefix="urlPrefix" />
    </div>
  </CustomDrawer>
</template>
<script>
import SwarmNode from './node'
import SwarmService from './service'
import SwarmTask from './task'
import { mapState } from 'pinia'
import { useGuideStore } from '@/stores/guide'
export default {
  components: {
    SwarmNode,
    SwarmService,
    SwarmTask
  },
  props: {
    id: {
      type: String,
      default: ''
    },
    initMenu: {
      type: String,
      default: 'node'
    },
    visible: {
      type: Boolean,
      default: false
    },
    urlPrefix: {
      type: String,
      default: ''
    }
  },
  emits: ['close'],
  data() {
    return {
      menuKeyArray: [],
      menuKey: ''
    }
  },
  computed: {
    ...mapState(useGuideStore, ['getCollapsed'])
  },
  mounted() {
    this.menuKey = this.initMenu
    this.menuKeyArray = [this.initMenu]
  },
  methods: {
    menuClick(item) {
      this.menuKey = item.key
    },
    onClose1() {
      this.$emit('close')
    }
  }
}
</script>
<style scoped>
.docker-menu {
  border-bottom: 0;
}
.layout-content {
  padding: 0;
  margin: 15px;
}
</style>
