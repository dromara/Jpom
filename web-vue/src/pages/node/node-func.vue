<template>
  <a-drawer
    destroyOnClose
    placement="right"
    :width="`${getCollapsed ? 'calc(100vw - 80px)' : 'calc(100vw - 200px)'}`"
    @close="
      () => {
        $emit('close')
      }
    "
    :headerStyle="{
      padding: '0 10px'
    }"
    :bodyStyle="{
      padding: '0'
    }"
    :open="true"
  >
    <template #title>
      <a-space>
        <!-- {{ name }} -->
        <div>
          <a-tabs
            v-model:activeKey="current"
            :tabBarStyle="{
              margin: '0'
            }"
          >
            <a-tab-pane v-if="tabs.includes('project')" key="project" tab="项目管理"></a-tab-pane>
            <a-tab-pane v-if="tabs.includes('scripct')" key="scripct" tab="脚本管理"></a-tab-pane>
            <a-tab-pane v-if="tabs.includes('scripct-log')" key="scripct-log" tab="脚本日志"></a-tab-pane>
          </a-tabs>
        </div>
      </a-space>
    </template>
    <div class="layout-content">
      <project-search v-if="current === 'project'" :nodeId="this.id" />
      <script-list v-else-if="current === 'scripct'" :nodeId="this.id"></script-list>
      <script-log v-else-if="current === 'scripct-log'" :nodeId="this.id"></script-log>
    </div>
  </a-drawer>
</template>

<script>
import { mapState } from 'pinia'
import ScriptList from '@/pages/node/script-list'
import ScriptLog from '@/pages/node/node-layout/other/script-log'
import { useGuideStore } from '@/stores/guide'
export default {
  props: {
    name: {
      type: String,
      default: ''
    },
    id: {
      type: String
    },
    tabs: {
      type: Array,
      default: function () {
        return ['project', 'scripct', 'scripct-log']
      }
    }
  },
  components: {
    ScriptList,
    ScriptLog,
    projectSearch: defineAsyncComponent(() => import('@/pages/node/search'))
  },
  computed: {
    ...mapState(useGuideStore, ['getCollapsed'])
  },
  created() {
    //
    this.current = this.tabs[0]
  },
  data() {
    return {
      current: null
    }
  },
  emits: ['close']
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
