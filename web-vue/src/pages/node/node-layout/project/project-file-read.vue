<template>
  <div>
    <!-- <div ref="filter" class="filter">
      <template>
        <a-space>
          |

          <a-input-group compact style="width: 200px">
            <a-select  :getPopupContainer="
              (triggerNode) => {
                return triggerNode.parentNode || document.body;
              }
            " v-model="logScroll">
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
    </div> -->
    <!-- console -->
    <log-view :ref="`logView`" height="calc(100vh - 140px)">
      <template slot="before"> <a-button type="primary" @click="goFile">文件管理</a-button></template>
    </log-view>
  </div>
</template>
<script>
// import { getProjectData, getProjectLogSize, downloadProjectLogFile, getLogBackList, downloadProjectLogBackFile, deleteProjectLogBackFile } from "@/api/node-project";
import {mapGetters} from "vuex";
import {getWebSocketUrl} from "@/utils/const";
import LogView from "@/components/logView";

export default {
  components: {
    LogView,
  },
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

      heart: null,
    };
  },
  computed: {
    ...mapGetters(["getLongTermToken"]),
    socketUrl() {
      return getWebSocketUrl("/socket/console", `userId=${this.getLongTermToken}&id=${this.id}&nodeId=${this.nodeId}&type=console&copyId=`);
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
        this.$notification.info({
          message: "会话已经关闭",
        });
        clearInterval(this.heart);
      };
      this.socket.onmessage = (msg) => {
        this.$refs.logView.appendLine(msg.data);

        clearInterval(this.heart);
        // 创建心跳，防止掉线
        this.heart = setInterval(() => {
          this.sendMsg("heart");
          // this.loadFileSize();
        }, 5000);
      };
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
</style>
