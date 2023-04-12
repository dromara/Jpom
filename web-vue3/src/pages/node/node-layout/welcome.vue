<template>
  <div class="node-full-content">
    <machine-info :nodeId="node.id"></machine-info>
  </div>
</template>
<script>
import machineInfo from '@/pages/system/assets/machine/machine-info'

export default {
  components: {
    machineInfo
  },
  props: {
    node: {
      type: Object
    }
  },
  data() {
    return {}
  },
  mounted() {},
  destroyed() {},
  watch: {},
  methods: {}
}
</script>
<style scoped></style>
