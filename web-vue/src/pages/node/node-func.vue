<template>
  <CustomDrawer
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
            <a-tab-pane
              v-if="tabs.includes('project')"
              key="project"
              :tab="$t('pages.node.node-func.37f14108')"
            ></a-tab-pane>
            <a-tab-pane
              v-if="tabs.includes('scripct')"
              key="scripct"
              :tab="$t('pages.node.node-func.2b56402a')"
            ></a-tab-pane>
            <a-tab-pane
              v-if="tabs.includes('scripct-log')"
              key="scripct-log"
              :tab="$t('pages.node.node-func.52cb3797')"
            ></a-tab-pane>
          </a-tabs>
        </div>
      </a-space>
    </template>
    <div class="layout-content">
      <project-search v-if="current === 'project'" :node-id="id" />
      <script-list v-else-if="current === 'scripct'" :node-id="id"></script-list>
      <script-log v-else-if="current === 'scripct-log'" :node-id="id"></script-log>
    </div>
  </CustomDrawer>
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
  },
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
