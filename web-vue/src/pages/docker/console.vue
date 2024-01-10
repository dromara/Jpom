<template>
  <div>
    <!-- 控制台 -->
    <a-drawer
      destroyOnClose
      placement="right"
      :width="`${this.getCollapsed ? 'calc(100vw - 80px)' : 'calc(100vw - 200px)'}`"
      :open="true"
      @close="onClose"
      :bodyStyle="{
        padding: '0'
      }"
      :headerStyle="{
        padding: '0 10px'
      }"
    >
      <template #title>
        <a-menu mode="horizontal" class="docker-menu" v-model:selectedKeys="menuKeyArray" @click="menuClick">
          <a-menu-item key="containers">
            <span class="nav-text">独立容器</span>
          </a-menu-item>
          <a-menu-item key="docker-compose">
            <span class="nav-text">docker-compose</span>
          </a-menu-item>
          <a-menu-item key="images">
            <span class="nav-text">镜像</span>
          </a-menu-item>
          <a-menu-item key="volumes">
            <span class="nav-text">卷</span>
          </a-menu-item>
          <a-menu-item key="networks">
            <span class="nav-text">网络</span>
          </a-menu-item>
          <a-menu-item key="info">
            <span class="nav-text">信息</span>
          </a-menu-item>
          <a-menu-item key="prune">
            <span class="nav-text">裁剪</span>
          </a-menu-item>
        </a-menu>
      </template>

      <div class="layout-content">
        <!-- <a-layout-content> -->
        <container
          v-if="menuKey === 'containers'"
          type="container"
          :id="this.id"
          :machineDockerId="this.machineDockerId"
          :visible="this.visible"
          :urlPrefix="this.urlPrefix"
        />
        <container
          v-else-if="menuKey === 'docker-compose'"
          type="compose"
          :id="this.id"
          :machineDockerId="this.machineDockerId"
          :visible="this.visible"
          :urlPrefix="this.urlPrefix"
        />
        <images
          v-if="menuKey === 'images'"
          :id="this.id"
          :machineDockerId="this.machineDockerId"
          :visible="this.visible"
          :urlPrefix="this.urlPrefix"
        />
        <volumes
          v-if="menuKey === 'volumes'"
          :id="this.id"
          :machineDockerId="this.machineDockerId"
          :visible="this.visible"
          :urlPrefix="this.urlPrefix"
        />
        <info
          v-if="menuKey === 'info'"
          :id="this.id"
          :machineDockerId="this.machineDockerId"
          :visible="this.visible"
          :urlPrefix="this.urlPrefix"
        />
        <networks
          v-if="menuKey === 'networks'"
          :id="this.id"
          :machineDockerId="this.machineDockerId"
          :visible="this.visible"
          :urlPrefix="this.urlPrefix"
        />
        <prune
          v-if="menuKey === 'prune'"
          :id="this.id"
          :machineDockerId="this.machineDockerId"
          :visible="this.visible"
          :urlPrefix="this.urlPrefix"
        />
        <!-- </a-layout-content> -->
      </div>
    </a-drawer>
  </div>
</template>

<script>
import Container from './container'
import Images from './images'
import Volumes from './volumes'
import Info from './info'
import Networks from './networks'
import Prune from './prune'
import { mapState } from 'pinia'
import { useGuideStore } from '@/stores/guide'
export default {
  props: {
    id: {
      type: String
    },
    visible: {
      type: Boolean,
      default: false
    },
    machineDockerId: {
      type: String
    },
    urlPrefix: {
      type: String
    }
  },
  components: {
    Container,
    Images,
    Volumes,
    Info,
    Networks,
    Prune
  },
  data() {
    return {
      menuKeyArray: ['containers'],
      menuKey: 'containers'
    }
  },
  computed: {
    ...mapState(useGuideStore, ['getCollapsed'])
  },
  mounted() {},
  methods: {
    menuClick(item) {
      this.menuKey = item.key
    },
    onClose() {
      this.$emit('close')
    }
  },
  emits: ['close']
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
