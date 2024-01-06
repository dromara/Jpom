<template>
  <div class="full-content">
    <workspaceEnv ref="workspaceEnv" :workspaceId="getWorkspaceId" :global="true" />
  </div>
</template>
<script>
import workspaceEnv from '@/pages/system/workspace-env.vue'
import { mapState } from 'pinia'

export default {
  components: {
    workspaceEnv
  },
  data() {
    return {}
  },
  computed: { ...mapGetters(['getWorkspaceId']) },
  created() {},
  methods: {}
}
</script>
<style scoped></style>
