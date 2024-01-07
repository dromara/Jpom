<template>
  <div class="full-content">
    <workspaceEnv ref="workspaceEnv" :workspaceId="this.getWorkspaceId()" :global="true" />
  </div>
</template>

<script>
import workspaceEnv from '@/pages/system/workspace-env.vue'
import { mapGetters } from 'vuex'

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
