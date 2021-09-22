<template>
  <upgrade :nodeId="this.node.id"></upgrade>
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
