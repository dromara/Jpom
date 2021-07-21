<template>
  <div>
    <div ref="filter" class="filter">
      <a-button :disabled="scriptStatus !== 0" type="primary" @click="start">执行</a-button>
      <a-button :disabled="scriptStatus !== 1" type="primary" @click="stop">停止</a-button>
    </div>
    <!-- console -->
    <div>
      <a-input class="console" v-model="logContext" readOnly type="textarea" style="resize: none;"/>
    </div>
  </div>
</template>
<script>
import { mapGetters } from 'vuex';
export default {
  props: {
    nodeId: {
      type: String
    },
    scriptId: {
      type: String
    }
  },
  data() {
    return {
      project: {},
      socket: null,
      // script 状态 {0: 未运行, 1: 运行中}
      scriptStatus: 0,
      // 日志内容
      logContext: 'choose file loading context...'
    }
  },
  computed: {
    ...mapGetters([
      'getLongTermToken'
    ]),
    socketUrl() {
      const protocol = location.protocol === 'https:' ? 'wss://' : 'ws://';
      const domain = window.routerBase;
      const url =  (domain + '/script_run').replace(new RegExp('//','gm'), '/');
      return `${protocol}${location.host}${url}?userId=${this.getLongTermToken}&scriptId=${this.scriptId}&nodeId=${this.nodeId}&type=script`;
    }
  },
  mounted() {
    this.initWebSocket();
  },
  beforeDestroy() {
    if (this.socket) {
      this.socket.close();
    }
  },
  methods: {
    // 初始化
    initWebSocket() {
      this.logContext = '';
      if (!this.socket || this.socket.readyState !== this.socket.OPEN || this.socket.readyState !== this.socket.CONNECTING) {
        this.socket = new WebSocket(this.socketUrl);
      }
      // 连接成功后
      this.socket.onopen = () => {
        this.logContext = 'connect success......\r\n'
      }
      this.socket.onmessage = (msg) => {
        if (msg.data.indexOf('code') > -1 && msg.data.indexOf('msg') > -1) {
          const res = JSON.parse(msg.data);
          if (res.code === 200) {
            this.$notification.success({
              message: res.msg,
              duration: 3
            });
            // 如果操作是启动或者停止
            if (res.op === 'stop') {
              this.scriptStatus = 0;
            }
            if (res.op === 'start') {
              this.scriptStatus = 1;
            }
          } else {
            this.$notification.error({
              message: res.msg,
              duration: 5
            });
            this.scriptStatus = 0;
          }
        }
        this.logContext += `${msg.data}\r\n`;
      }
    },
    // 发送消息
    sendMsg(op) {
      const data = {
        op: op,
        scriptId: this.scriptId,
        args: this.args
      }
      this.socket.send(JSON.stringify(data));
    },
    // 启动
    start() {
      this.sendMsg('start');
    },
    // 停止
    stop() {
      this.sendMsg('stop');
    },
  }
}
</script>
<style scoped>
.filter {
  margin: 0 0 10px;
}
.ant-btn {
  margin-right: 10px;
}
.console {
  padding: 5px;
  color: #fff;
  font-size: 14px;
  background-color: black;
  width: 100%;
  height: calc(100vh - 120px);
  overflow-y: auto;
  border: 1px solid #e2e2e2;
  border-radius: 5px 5px;
}
</style>