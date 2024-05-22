<template>
  <div>
    <a-tabs default-active-key="1">
      <a-tab-pane key="1" :tab="$tl('p.workspaceManagement')"><workspaceList></workspaceList> </a-tab-pane>
      <a-tab-pane key="2" :tab="$tl('p.clusterManagement')">
        <clusterList></clusterList>
      </a-tab-pane>
    </a-tabs>
  </div>
</template>

<script>
import workspaceList from './workspace-list.vue'
import clusterList from './cluster-list.vue'
export default {
  components: {
    workspaceList,
    clusterList
  },
  data() {
    return {}
  },
  computed: {},
  created() {},
  methods: {
    $tl(key, ...args) {
      return this.$t(`pages.system.workspace.${key}`, ...args)
    }
  }
}
</script>
