<template>
  <a-drawer
    destroy-on-close
    placement="right"
    :width="`${getCollapsed ? 'calc(100vw - 80px)' : 'calc(100vw - 200px)'}`"
    :header-style="{
      padding: '0 10px'
    }"
    :body-style="{
      padding: '0'
    }"
    :open="true"
    @close="
      () => {
        $emit('close')
      }
    "
  >
    <template #title>
      <a-space>
        <!-- {{ name }} -->
        <div>
          <a-tabs
            v-model:activeKey="current"
            :tab-bar-style="{
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
      <project-search v-if="current === 'project'" :node-id="id" />
      <script-list v-else-if="current === 'scripct'" :node-id="id"></script-list>
      <script-log v-else-if="current === 'scripct-log'" :node-id="id"></script-log>
    </div>
  </a-drawer>
</template>

<script>
import { mapState } from 'pinia'
import ScriptList from '@/pages/node/script-list'
import ScriptLog from '@/pages/node/node-layout/other/script-log'
import { useGuideStore } from '@/stores/guide'
export default {
  components: {
    ScriptList,
    ScriptLog,
    projectSearch: defineAsyncComponent(() => import('@/pages/node/search'))
  },
  props: {
    name: {
      type: String,
      default: ''
    },
    id: {
      type: String,
      default: ''
    },
    tabs: {
      type: Array,
      default: function () {
        return ['project', 'scripct', 'scripct-log']
      }
    }
  },
  emits: ['close'],
  data() {
    return {
      current: null
    }
  },
  computed: {
    ...mapState(useGuideStore, ['getCollapsed'])
  },
  created() {
    //
    this.current = this.tabs[0]
  }
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
