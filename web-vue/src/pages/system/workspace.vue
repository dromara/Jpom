<template>
  <div>
    <a-tabs default-active-key="1">
      <a-tab-pane key="1" tab="工作空间管理"><workspaceList></workspaceList> </a-tab-pane>
      <a-tab-pane key="2" tab="集群管理">
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
  methods: {}
}
</script>
