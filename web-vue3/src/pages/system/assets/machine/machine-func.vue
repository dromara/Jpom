<template>
  <a-drawer
    destroyOnClose
    placement="right"
    :width="`${this.getCollapsed ? 'calc(100vw - 80px)' : 'calc(100vw - 200px)'}`"
    :open="true"
    @close="
      () => {
        $emit('close')
      }
    "
    :headerStyle="{
      padding: '5px 20px'
    }"
  >
    <template v-slot:title>
      <a-space>
        <div>
          <a-tabs
            v-model:activeKey="current"
            :tabBarStyle="{
              margin: '0',
              borderBottom: '0'
            }"
          >
            <a-tab-pane key="info" tab="基本信息"></a-tab-pane>
            <a-tab-pane key="cache" tab="缓存监控"></a-tab-pane>
            <a-tab-pane key="config" tab="系统配置"></a-tab-pane>
            <a-tab-pane key="path-config" tab="授权配置"></a-tab-pane>
            <a-tab-pane key="upgrade" tab="在线升级"></a-tab-pane>
            <a-tab-pane key="log" tab="系统日志"></a-tab-pane>
          </a-tabs>
        </div>
      </a-space>
    </template>
    <!-- 机器信息组件 -->
    <machine-info v-if="current === 'info'" :machineId="machineId" />
    <upgrade v-if="current === 'upgrade'" :machineId="machineId" />
    <white-list v-if="current === 'path-config'" :machineId="machineId" />
    <cache v-if="current === 'cache'" :machineId="machineId" />
    <log v-if="current === 'log'" :machineId="machineId" />
    <config-file v-if="current === 'config'" :machineId="machineId" />
  </a-drawer>
</template>

<script>
import { mapState } from 'pinia'
import machineInfo from './machine-info'
import upgrade from '@/components/upgrade'
import WhiteList from '@/pages/node/node-layout/system/white-list.vue'
import Cache from '@/pages/node/node-layout/system/cache'
import Log from '@/pages/node/node-layout/system/log.vue'
import ConfigFile from '@/pages/node/node-layout/system/config-file.vue'
import { useAppStore } from '@/stores/app'
export default {
  props: {
    machineId: {
      type: String
    },
    name: {
      type: String
    },
    tab: {
      type: String,
      default: ''
    }
  },
  components: {
    machineInfo,
    upgrade,
    WhiteList,
    Cache,
    ConfigFile,
    Log
  },
  computed: {
    ...mapState(useAppStore, ['getCollapsed'])
  },
  data() {
    return {
      current: this.tab || 'info'
    }
  },
  mounted() {},
  emits: ['close']
}
</script>
