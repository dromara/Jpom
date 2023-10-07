<template>
  <repository :workspaceId="this.getWorkspaceId" :global="true" :choose="choose" @confirm="confirm"> </repository>
</template>
<script>
import repository from "./repository-list.vue";
import { mapGetters } from "vuex";
export default {
  props: {
    choose: {
      type: Boolean,
      default: false,
    },
  },
  components: { repository },
  computed: { ...mapGetters(["getWorkspaceId"]) },
  methods: {
    confirm(data) {
      this.$emit("confirm", data);
    },
  },
};
</script>
