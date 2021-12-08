<template>
  <div :class="`${node.id ? 'node-full-content' : 'full-content'}`">
    <upgrade :nodeId="this.node.id"></upgrade>
  </div>
</template>
<script>
import upgrade from "@/components/upgrade";
export default {
  components: {
    upgrade,
  },
  props: {
    node: {
      type: Object,
    },
  },
  data() {
    return {};
  },
  mounted() {},
  methods: {},
};
</script>
