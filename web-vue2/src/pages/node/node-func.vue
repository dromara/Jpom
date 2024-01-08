<template>
  <a-drawer
    destroyOnClose
    placement="right"
    :width="`${getCollapsed ? 'calc(100vw - 80px)' : 'calc(100vw - 200px)'}`"
    @close="
      () => {
        $emit('close');
      }
    "
    :headerStyle="{
      padding: '10px 20px',
    }"
    :visible="true"
  >
    <template #title>
      <a-space>
        {{ name }}
        <div style="display: inline-block">
          <a-tabs
            v-model="current"
            :tabBarStyle="{
              margin: '0',
              borderBottom: '0',
            }"
          >
            <a-tab-pane v-if="tabs.includes('project')" key="project" tab="项目管理"></a-tab-pane>
            <a-tab-pane v-if="tabs.includes('scripct')" key="scripct" tab="脚本管理"></a-tab-pane>
            <a-tab-pane v-if="tabs.includes('scripct-log')" key="scripct-log" tab="脚本日志"></a-tab-pane>
          </a-tabs>
        </div>
      </a-space>
    </template>

    <component :is="projectSearch" v-if="current === 'project'" :nodeId="this.id" />
    <script-list v-else-if="current === 'scripct'" :nodeId="this.id"></script-list>
    <script-log v-else-if="current === 'scripct-log'" :nodeId="this.id"></script-log>
  </a-drawer>
</template>

<script>
import { mapGetters } from "vuex";
import ScriptList from "@/pages/node/script-list";
import ScriptLog from "@/pages/node/node-layout/other/script-log";
export default {
  props: {
    name: {
      type: String,
      default: "",
    },
    id: {
      type: String,
    },
    tabs: {
      type: Array,
      default: function () {
        return ["project", "scripct", "scripct-log"];
      },
    },
  },
  components: {
    ScriptList,
    ScriptLog,
  },
  computed: {
    ...mapGetters(["getCollapsed"]),
  },
  created() {
    // 异步加载组件
    this.projectSearch = () => import("@/pages/node/search");
    //
    this.current = this.tabs[0];
  },
  data() {
    return {
      current: null,
      projectSearch: null,
    };
  },
};
</script>
