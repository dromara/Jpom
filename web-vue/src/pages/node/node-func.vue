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
            }"
          >
            <a-tab-pane key="project" tab="项目管理"></a-tab-pane>
            <a-tab-pane key="scripct" tab="脚本管理"></a-tab-pane>
            <a-tab-pane key="scripct-log" tab="脚本日志"></a-tab-pane>
          </a-tabs>
        </div>
      </a-space>
    </template>

    <project-search v-if="current === 'project'" :nodeId="this.id" />
    <script-list v-else-if="current === 'scripct'" :nodeId="this.id"></script-list>
    <script-log v-else-if="current === 'scripct-log'" :nodeId="this.id"></script-log>
  </a-drawer>
</template>

<script>
import { mapGetters } from "vuex";
import ProjectSearch from "./search";
import ScriptList from "./script-list";
import ScriptLog from "@/pages/node/node-layout/other/script-log";
export default {
  props: {
    name: {
      type: String,
    },
    id: {
      type: String,
    },
  },
  components: {
    ProjectSearch,
    ScriptList,
    ScriptLog,
  },
  computed: {
    ...mapGetters(["getCollapsed"]),
  },
  data() {
    return {
      current: "project",
    };
  },
};
</script>
<style>
/deep/ .ant-drawer-header {
}

/deep/ .ant-tabs-bar {
}
</style>
