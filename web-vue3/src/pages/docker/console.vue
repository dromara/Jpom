<template>
  <div :class="`${this.scrollbarFlag ? '' : 'hide-scrollbar'}`">
    <!-- 控制台 -->
    <a-drawer
      destroyOnClose
      placement="right"
      :width="`${this.getCollapsed ? 'calc(100vw - 80px)' : 'calc(100vw - 200px)'}`"
      :visible="true"
      @close="onClose"
      :bodyStyle="{
        padding: '0'
      }"
    >
      <template #title>
        <a-space>
          【控制台】
          <a-menu theme="light" mode="horizontal" class="docker-menu" v-model="menuKeyArray" @click="menuClick">
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
        </a-space>
      </template>

      <a-layout
        :class="`layout-content drawer-layout-content ${scrollbarFlag ? '' : 'hide-scrollbar'}`"
        style="padding-bottom: 10px"
      >
        <a-layout-content>
          <container
            :key="menuKey"
            v-if="menuKey === 'containers'"
            type="container"
            :id="id"
            :machineDockerId="machineDockerId"
            :visible="visible"
            :urlPrefix="urlPrefix"
          />
          <container
            :key="`docker-compose`"
            v-else-if="menuKey === 'docker-compose'"
            type="compose"
            :id="id"
            :machineDockerId="machineDockerId"
            :visible="visible"
            :urlPrefix="urlPrefix"
          />
          <images
            v-if="menuKey === 'images'"
            :id="id"
            :machineDockerId="machineDockerId"
            :visible="visible"
            :urlPrefix="urlPrefix"
          />
          <volumes
            v-if="menuKey === 'volumes'"
            :id="id"
            :machineDockerId="machineDockerId"
            :visible="visible"
            :urlPrefix="urlPrefix"
          />
          <info
            v-if="menuKey === 'info'"
            :id="id"
            :machineDockerId="machineDockerId"
            :visible="visible"
            :urlPrefix="urlPrefix"
          />
          <networks
            v-if="menuKey === 'networks'"
            :id="id"
            :machineDockerId="machineDockerId"
            :visible="visible"
            :urlPrefix="urlPrefix"
          />
          <prune
            v-if="menuKey === 'prune'"
            :id="id"
            :machineDockerId="machineDockerId"
            :visible="visible"
            :urlPrefix="urlPrefix"
          />
        </a-layout-content>
      </a-layout>
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
import { mapGetters } from 'vuex'
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
    ...mapGetters(['getCollapsed', 'getGuideCache']),
    scrollbarFlag() {
      return this.getGuideCache.scrollbarFlag === undefined ? true : this.getGuideCache.scrollbarFlag
    }
  },
  mounted() {},
  methods: {
    menuClick(item) {
      this.menuKey = item.key
    },
    onClose() {
      this.$emit('close')
    }
  }
}
</script>
<style scoped></style>
