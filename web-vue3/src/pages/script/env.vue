<template>
  <div>
    <workspaceEnv ref="workspaceEnv" :workspaceId="this.getWorkspaceId()" :global="true" />
  </div>
</template>

<script>
import workspaceEnv from '@/pages/system/workspace-env.vue'
import { mapState } from 'pinia'

import { useAppStore } from '@/stores/app'

export default {
  components: {
    workspaceEnv
  },
  data() {
    return {}
  },
  computed: { ...mapState(useAppStore, ['getWorkspaceId']) },
  created() {},
  methods: {}
}
</script>
