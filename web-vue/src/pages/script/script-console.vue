<template>
  <a-layout class="script-console-layout">
    <div>
      <div ref="filter" class="filter">
        <a-space>
          <a-button :disabled="scriptStatus !== 0" type="primary" @click="start">执行</a-button>
          <a-button :disabled="scriptStatus !== 1" type="primary" @click="stop">停止</a-button>
        </a-space>
      </div>
      <!-- console -->
      <div>
        <a-input class="console" v-model="logContext" readOnly type="textarea" style="resize: none" />
      </div>
    </div>
    <!--远程下载  -->
    <a-modal v-model="editArgs" title="添加运行参数" @ok="startExecution" @cancel="this.editArgs = false" :maskClosable="false">
      <a-form-model :model="temp" :label-col="{ span: 5 }" :wrapper-col="{ span: 24 }" ref="ruleForm">
        <a-form-model-item label="执行参数" prop="args">
          <a-input v-model="temp.args" placeholder="执行参数,没有参数可以不填写" />
        </a-form-model-item>
      </a-form-model>
    </a-modal>
  </a-layout>
</template>
<script>
import { mapGetters } from "vuex";

export default {
  props: {
    id: {
      type: String,
    },
    defArgs: { type: String },
  },
  data() {
    return {
      project: {},
      socket: null,
      // script 状态 {0: 未运行, 1: 运行中}
      scriptStatus: 0,
      editArgs: false,
      temp: {
        args: "",
      },
      // 日志内容
      logContext: "loading ...",
    };
  },
  computed: {
    ...mapGetters(["getLongTermToken"]),
    socketUrl() {
      const protocol = location.protocol === "https:" ? "wss://" : "ws://";
      const domain = window.routerBase;
      const url = (domain + "/script_run").replace(new RegExp("//", "gm"), "/");
      return `${protocol}${location.host}${url}?userId=${this.getLongTermToken}&id=${this.id}&type=script&nodeId=system`;
    },
  },
  mounted() {
    this.initWebSocket();
    this.temp.args = this.defArgs;
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
      };
      this.socket.onerror = (err) => {
        console.error(err);
        this.$notification.error({
          message: "web socket 错误,请检查是否开启 ws 代理,或者没有对应的权限",
        });
      };
      this.socket.onmessage = (msg) => {
        if (msg.data.indexOf("code") > -1 && msg.data.indexOf("msg") > -1) {
          const res = JSON.parse(msg.data);
          if (res.code === 200) {
            this.$notification.success({
              message: res.msg,
            });
            // 如果操作是启动或者停止
            if (res.op === "stop") {
              this.scriptStatus = 0;
            }
            if (res.op === "start") {
              this.scriptStatus = 1;
            }
            if (res.executeId) {
              this.temp = { ...this.temp, executeId: res.executeId };
            }
          } else {
            this.$notification.error({
              message: res.msg,
            });
            this.scriptStatus = 0;
          }
          return;
        }
        this.logContext += `${msg.data}\r\n`;
        clearInterval(this.heart);
        // 创建心跳，防止掉线
        this.heart = setInterval(() => {
          this.sendMsg("heart");
        }, 5000);
      };
    },
    startExecution() {
      this.editArgs = false;
      this.sendMsg("start");
    },
    // 发送消息
    sendMsg(op) {
      const data = {
        op: op,
        id: this.id,
        args: this.temp.args,
        executeId: this.temp.executeId,
      };
      this.socket.send(JSON.stringify(data));
    },
    // 启动
    start() {
      this.editArgs = true;
    },
    // 停止
    stop() {
      this.sendMsg("stop");
    },
  },
};
</script>
<style scoped>
.script-console-layout {
  padding: 0;
  margin: 0;
}

.filter {
  margin: 0 0 10px;
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
