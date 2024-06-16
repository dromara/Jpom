<template>
  <CustomDrawer
    destroy-on-close
    placement="right"
    :width="`${getCollapsed ? 'calc(100vw - 80px)' : 'calc(100vw - 200px)'}`"
    :open="true"
    :header-style="{
      padding: '0 10px'
    }"
    :body-style="{
      padding: '0'
    }"
    @close="
      () => {
        $emit('close')
      }
    "
  >
    <template #title>
      <a-space>
        <div>
          <a-tabs
            v-model:activeKey="current"
            :tab-bar-style="{
              margin: '0'
            }"
          >
            <a-tab-pane key="info" :tab="$t('pages.system.assets.machine.machine-func.ccf301b7')"></a-tab-pane>
            <a-tab-pane key="cache" :tab="$t('pages.system.assets.machine.machine-func.720b993a')"></a-tab-pane>
            <a-tab-pane key="config" :tab="$t('pages.system.assets.machine.machine-func.44816e4')"></a-tab-pane>
            <a-tab-pane key="freeScript" :tab="$t('pages.system.assets.machine.machine-func.62b3921c')"></a-tab-pane>
            <a-tab-pane key="path-config" :tab="$t('pages.system.assets.machine.machine-func.d2edad8b')"></a-tab-pane>
            <a-tab-pane key="upgrade" :tab="$t('pages.system.assets.machine.machine-func.a1b1093a')"></a-tab-pane>
            <a-tab-pane key="log" :tab="$t('pages.system.assets.machine.machine-func.b891517f')"></a-tab-pane>
          </a-tabs>
        </div>
      </a-space>
    </template>
    <div class="layout-content">
      <!-- 机器信息组件 -->
      <machine-info v-if="current === 'info'" :machine-id="machineId" />
      <upgrade v-if="current === 'upgrade'" :machine-id="machineId" />
      <white-list v-if="current === 'path-config'" :machine-id="machineId" />
      <cache v-if="current === 'cache'" :machine-id="machineId" />
      <log v-if="current === 'log'" :machine-id="machineId" />
      <config-file v-if="current === 'config'" :machine-id="machineId" />
      <freeScript v-if="current === 'freeScript'" :machine-id="machineId" />
    </div>
  </CustomDrawer>
</template>
<script>
import { mapState } from 'pinia'
import machineInfo from './machine-info'
import freeScript from './free-script'
import upgrade from '@/components/upgrade'
import WhiteList from '@/pages/node/node-layout/system/white-list.vue'
import Cache from '@/pages/node/node-layout/system/cache'
import Log from '@/pages/node/node-layout/system/log.vue'
import ConfigFile from '@/pages/node/node-layout/system/config-file.vue'
import { useAppStore } from '@/stores/app'
export default {
  components: {
    machineInfo,
    upgrade,
    WhiteList,
    Cache,
    ConfigFile,
    Log,
    freeScript
  },
  props: {
    machineId: {
      type: String,
      default: ''
    },
    name: {
      type: String,
      default: ''
    },
    tab: {
      type: String,
      default: ''
    }
  },
  emits: ['close'],
  data() {
    return {
      current: this.tab || 'info'
    }
  },

  computed: {
    ...mapState(useAppStore, ['getCollapsed'])
  },
  mounted() {},
  methods: {}
}
</script>
<style scoped>
.layout-content {
  padding: 0;
  margin: 15px;
}
:deep(.ant-tabs-nav::before) {
  border-bottom: 0;
}
</style>
