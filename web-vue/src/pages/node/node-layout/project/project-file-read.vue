<template>
  <div>
    <div ref="filter" class="filter">
      <template>
        <a-space>
          <a-button type="primary" @click="goFile">文件管理</a-button>
          |

          <a-input-group compact style="width: 200px">
            <a-select v-model="logScroll">
              <a-select-option value="true"> 自动滚动 </a-select-option>
              <a-select-option value="false"> 关闭滚动 </a-select-option>
            </a-select>
            <a-input style="width: 50%" v-model="logShowLine" placeholder="显示行数" />
          </a-input-group>
          <a-tag> 文件: {{ this.readFilePath }}</a-tag>
          <a-button type="link" @click="clearLogCache" icon="delete"> 清空 </a-button>
          <a-input-search placeholder="搜索关键词" style="width: 200px" @search="onSearch" />
        </a-space>
      </template>
    </div>
    <!-- console -->
    <pre class="console" id="logScrollArea">
      loading context...
    </pre>
  </div>
</template>
<script>
// import { getProjectData, getProjectLogSize, downloadProjectLogFile, getLogBackList, downloadProjectLogBackFile, deleteProjectLogBackFile } from "@/api/node-project";
import { mapGetters } from "vuex";

export default {
  props: {
    nodeId: {
      type: String,
    },
    projectId: {
      type: String,
    },
    id: {
      type: String,
    },
    readFilePath: {
      type: String,
    },
  },
  data() {
    return {
      project: {},
      optButtonLoading: true,
      loading: false,
      socket: null,
      searchReg: null,
      regReplaceText: "<b style='color:red;'>$1</b>",
      // 日志内容
      logContextArray: [],
      logScroll: "true",
      logShowLine: 500,
      defLogShowLine: 500,
      heart: null,
    };
  },
  computed: {
    ...mapGetters(["getLongTermToken"]),
    socketUrl() {
      const protocol = location.protocol === "https:" ? "wss://" : "ws://";
      const domain = window.routerBase;
      const url = (domain + "/console").replace(new RegExp("//", "gm"), "/");
      return `${protocol}${location.host}${url}?userId=${this.getLongTermToken}&id=${this.id}&nodeId=${this.nodeId}&type=console&copyId=`;
    },
  },
  mounted() {
    // this.loadProject();
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
      //this.logContext = "";
      if (!this.socket || this.socket.readyState !== this.socket.OPEN || this.socket.readyState !== this.socket.CONNECTING) {
        this.socket = new WebSocket(this.socketUrl);
      }
      // 连接成功后
      this.socket.onopen = () => {
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
        this.$notification.error({
          message: "会话已经关闭",
        });
        clearInterval(this.heart);
      };
      this.socket.onmessage = (msg) => {
        if (this.searchReg) {
          this.logContextArray.push(msg.data.replace(this.searchReg, this.regReplaceText));
        } else {
          this.logContextArray.push(msg.data);
        }

        let logShowLineTemp = parseInt(this.logShowLine);
        logShowLineTemp = isNaN(logShowLineTemp) ? this.defLogShowLine : logShowLineTemp;
        logShowLineTemp = logShowLineTemp > 0 ? logShowLineTemp : 1;
        if (this.logScroll === "true") {
          this.logContextArray = this.logContextArray.slice(-logShowLineTemp);
        }

        // 自动滚动到底部
        this.$nextTick(() => {
          const projectConsole = document.getElementById("logScrollArea");
          projectConsole.innerHTML = this.logContextArray.join("</br>");
          if (this.logScroll === "true") {
            setTimeout(() => {
              projectConsole.scrollTop = projectConsole.scrollHeight;
            }, 100);
          }
        });

        clearInterval(this.heart);
        // 创建心跳，防止掉线
        this.heart = setInterval(() => {
          this.sendMsg("heart");
          // this.loadFileSize();
        }, 5000);
      };
    },
    // 搜索
    onSearch(value) {
      this.searchReg = value ? new RegExp("(" + value + ")", "ig") : null;

      this.logContextArray = this.logContextArray.map((item) => {
        item = item.replace(/<b[^>]*>([^>]*)<\/b[^>]*>/gi, "$1");
        if (this.searchReg) {
          item = item.replace(this.searchReg, this.regReplaceText);
        }
        return item;
      });
      this.$nextTick(() => {
        const projectConsole = document.getElementById("logScrollArea");
        projectConsole.innerHTML = this.logContextArray.join("</br>");
      });
    },
    // 发送消息
    sendMsg(op) {
      const data = {
        op: op,
        projectId: this.projectId,
        fileName: this.readFilePath,
      };
      this.socket.send(JSON.stringify(data));
    },
    clearLogCache() {
      this.logContextArray = [];
      this.$nextTick(() => {
        const projectConsole = document.getElementById("logScrollArea");
        projectConsole.innerHTML = "loading context...";
      });
    },

    goFile() {
      this.$emit("goFile");
    },
  },
};
</script>
<style scoped>
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
