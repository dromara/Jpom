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
            <a-tab-pane key="info" :tab="$t('i18n_9e5ffa068e')"></a-tab-pane>
            <a-tab-pane key="cache" :tab="$t('i18n_e976b537f1')"></a-tab-pane>
            <a-tab-pane key="config" :tab="$t('i18n_787fdcca55')"></a-tab-pane>
            <a-tab-pane key="freeScript" :tab="$t('i18n_7760785daf')"></a-tab-pane>
            <a-tab-pane key="path-config" :tab="$t('i18n_3d48c9da09')"></a-tab-pane>
            <a-tab-pane key="upgrade" :tab="$t('i18n_da8cb77838')"></a-tab-pane>
            <a-tab-pane key="log" :tab="$t('i18n_84aa0038cf')"></a-tab-pane>
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
