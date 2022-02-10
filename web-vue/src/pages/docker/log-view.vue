<template>
  <!-- console -->
  <div>
    <log-view ref="logView" seg="" height="60vh" />
  </div>
</template>
<script>
import { mapGetters } from "vuex";
import { getWebSocketUrl } from "@/utils/const";

import LogView from "@/components/logView";

export default {
  components: {
    LogView,
  },
  props: {
    id: {
      type: String,
    },
    containerId: { type: String },
  },
  data() {
    return {
      socket: null,
    };
  },
  computed: {
    ...mapGetters(["getLongTermToken"]),
    socketUrl() {
      return getWebSocketUrl("/socket/docker_log", `userId=${this.getLongTermToken}&id=${this.id}&type=dockerLog&nodeId=system`);
    },
  },
  mounted() {
    this.initWebSocket();
  },
  beforeDestroy() {
    if (this.socket) {
      this.socket.close();
    }
    clearInterval(this.heart);
  },
  methods: {
    // 初始化
    initWebSocket() {
      this.logContext = "";
      if (!this.socket || this.socket.readyState !== this.socket.OPEN || this.socket.readyState !== this.socket.CONNECTING) {
        this.socket = new WebSocket(this.socketUrl);
      }
      // 连接成功后
      this.socket.onopen = () => {
        // this.logContext = "connect success......\r\n";
        this.sendMsg("showlog");
      };
      this.socket.onerror = (err) => {
        console.error(err);
        this.$notification.error({
          message: "web socket 错误,请检查是否开启 ws 代理",
        });
        clearInterval(this.heart);
      };
      this.socket.onclose = (err) => {
        //当客户端收到服务端发送的关闭连接请求时，触发onclose事件
        console.error(err);
        this.$notification.info({
          message: "会话已经关闭",
        });
        clearInterval(this.heart);
      };
      this.socket.onmessage = (msg) => {
        if (msg.data.indexOf("code") > -1 && msg.data.indexOf("msg") > -1) {
          const res = JSON.parse(msg.data);
          if (res.code === 200) {
            this.$notification.success({
              message: res.msg,
            });
          } else {
            this.$notification.error({
              message: res.msg,
            });
          }
          return;
        }
        this.$refs.logView.appendLine(msg.data);
        clearInterval(this.heart);
        // 创建心跳，防止掉线
        this.heart = setInterval(() => {
          this.sendMsg("heart");
        }, 5000);
      };
    },
    // 发送消息
    sendMsg(op) {
      const data = {
        op: op,
        containerId: this.containerId,
      };
      this.socket.send(JSON.stringify(data));
    },
  },
};
</script>
<style scoped></style>
