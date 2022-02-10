<template>
  <terminal :url="this.socketUrl" />
</template>
<script>
import { mapGetters } from "vuex";
import { getWebSocketUrl } from "@/utils/const";
import terminal from "@/components/terminal";

// https://blog.csdn.net/qq_41840688/article/details/108636267

export default {
  components: {
    terminal,
  },
  props: {
    id: {
      type: String,
    },
    containerId: {
      type: String,
    },
  },
  data() {
    return {};
  },
  computed: {
    ...mapGetters(["getLongTermToken"]),
    socketUrl() {
      return getWebSocketUrl("/socket/docker_cli", `userId=${this.getLongTermToken}&id=${this.id}&nodeId=system&type=docker&containerId=${this.containerId}`);
    },
  },
  mounted() {},
  beforeDestroy() {},
  methods: {},
};
</script>
