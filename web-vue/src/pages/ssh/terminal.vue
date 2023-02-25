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
    sshId: {
      type: String,
      default: "",
    },
    machineSshId: {
      type: String,
      default: "",
    },
  },
  data() {
    return {};
  },
  computed: {
    ...mapGetters(["getLongTermToken"]),
    socketUrl() {
      return getWebSocketUrl("/socket/ssh", `userId=${this.getLongTermToken}&id=${this.sshId}&machineSshId=${this.machineSshId}&nodeId=system&type=ssh`);
    },
  },
  mounted() {},
  beforeDestroy() {},
  methods: {},
};
</script>
