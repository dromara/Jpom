<template>
  <div>
    <!-- <div ref="filter" class="filter"> -->
    <!-- <template v-if="copyId">
        <a-space>
          <a-button :disabled="replicaStatus" :loading="optButtonLoading" type="primary" @click="start">启动</a-button>
          <a-button :disabled="!replicaStatus" :loading="optButtonLoading" type="danger" @click="restart">重启</a-button>
          <a-button :disabled="!replicaStatus" :loading="optButtonLoading" type="danger" @click="stop">停止</a-button>
          <a-button type="primary" @click="handleDownload">导出日志</a-button>
          <a-tag color="#87d068">文件大小: {{ project.logSize }}</a-tag>
          <a-switch checked-children="自动滚动" un-checked-children="关闭滚动" v-model="logScroll" />
        </a-space>
      </template> -->
    <!-- <template> </template> -->
    <!-- </div> -->
    <!-- console -->
    <log-view :ref="`logView`" height="calc(100vh - 140px)">
      <template slot="before">
        <a-space>
          <a-button size="small" :disabled="project.status" :loading="optButtonLoading" type="primary" @click="start">{{$t('common.start')}}</a-button>
          <a-button size="small" :disabled="!project.status" :loading="optButtonLoading" type="danger" @click="restart">{{$t('common.restart')}}</a-button>
          <a-button size="small" :disabled="!project.status" :loading="optButtonLoading" type="danger" @click="stop">{{$t('common.stop')}}</a-button>

          <a-button size="small" v-if="!copyId" type="primary" @click="goFile">{{$t('common.fileMan')}}</a-button>

          <a-dropdown>
            <!-- <a type="link" class="ant-dropdown-link"> 更多<a-icon type="down" /> </a> -->
            <a-button
              size="small"
              @click="
                (e) => {
                  e.preventDefault();
                  handleLogBack();
                }
              "
            >
              <!-- <a-tag> -->
              {{$t('common.log')+$t('common.size')}}: {{ project.logSize || "-" }}
              <!-- 更多 -->
              <a-icon type="fullscreen" />
              <!-- </a-tag> -->
            </a-button>
            <!-- <a-menu slot="overlay">
              <a-menu-item>
                <a-button type="primary" size="small" :disabled="!project.logSize" @click="handleDownload">导出日志</a-button>
              </a-menu-item>
              <a-menu-item>
                <a-button type="primary" size="small" @click="handleLogBack">备份列表</a-button>
              </a-menu-item>
            </a-menu> -->
          </a-dropdown>
        </a-space>
      </template>
    </log-view>
    <!-- 日志备份 -->
    <a-modal destroyOnClose v-model="lobbackVisible" :title=$t('common.logBackupList') width="850px" :footer="null" :maskClosable="false">
      <ProjectLog v-if="lobbackVisible" :nodeId="this.nodeId" :copyId="this.copyId" :projectId="this.projectId"></ProjectLog>
    </a-modal>
  </div>
</template>
<script>
import { getProjectData, getProjectLogSize } from "@/api/node-project";
import { mapGetters } from "vuex";
import { getWebSocketUrl } from "@/utils/const";
import LogView from "@/components/logView";
import ProjectLog from "./project-log.vue";

export default {
  components: {
    LogView,
    ProjectLog,
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
    copyId: {
      type: String,
    },
  },
  data() {
    return {
      project: {},
      optButtonLoading: true,
      loading: false,
      socket: null,
      logExist: false,
      lobbackVisible: false,

      heart: null,
    };
  },
  computed: {
    ...mapGetters(["getLongTermToken", "getWorkspaceId"]),
    socketUrl() {
      return getWebSocketUrl("/socket/console", `userId=${this.getLongTermToken}&id=${this.id}&nodeId=${this.nodeId}&type=console&copyId=${this.copyId || ""}&workspaceId=${this.getWorkspaceId}`);
    },
  },
  mounted() {
    this.loadProject();
    this.initWebSocket();
    // 监听窗口关闭事件，当窗口关闭时，主动去关闭websocket连接，防止连接还没断开就关闭窗口，server端会抛异常。
    window.onbeforeunload = () => {
      this.close();
    };
  },
  beforeDestroy() {
    this.close();
  },
  methods: {
    close() {
      this.socket?.close();

      clearInterval(this.heart);
    },
    // 加载项目
    loadProject() {
      const params = {
        id: this.projectId,
        nodeId: this.nodeId,
      };
      getProjectData(params).then((res) => {
        if (res.code === 200) {
          this.project = { ...this.project, ...res.data };
          if (this.copyId) {
            if (this.project.javaCopyItemList) {
              const finds = this.project.javaCopyItemList.filter((item) => item.id === this.copyId);
              if (finds.length) {
                this.project = { ...this.project, log: finds[0].log, logBack: finds[0].logBack };
              } else {
                this.$notification.error({
                  message: this.$t('node.node_layout.project.console.noCopyFound'),
                });
              }
            } else {
              this.$notification.error({
                message: this.$t('node.node_layout.project.console.noCopy'),
              });
            }
          }
          // 加载日志文件大小
          this.loadFileSize();
        }
      });
    },
    // 初始化
    initWebSocket() {
      //this.logContext = "";
      if (!this.socket || this.socket.readyState !== this.socket.OPEN || this.socket.readyState !== this.socket.CONNECTING) {
        this.socket = new WebSocket(this.socketUrl);
      }
      // 连接成功后
      this.socket.onopen = () => {
        this.sendMsg("status");
        this.sendMsg("showlog");
      };
      this.socket.onerror = (err) => {
        console.error(err);
        this.$notification.error({
          message: this.$t('node.node_layout.project.console.socketErr'),
        });
        clearInterval(this.heart);
      };
      this.socket.onclose = (err) => {
        //当客户端收到服务端发送的关闭连接请求时，触发onclose事件
        console.error(err);
        this.$message.warning(this.$t('node.node_layout.project.console.sessionClosed'));
        clearInterval(this.heart);
      };
      this.socket.onmessage = (msg) => {
        if (msg.data.indexOf("JPOM_MSG") > -1 && msg.data.indexOf("op") > -1) {
          // console.log(msg.data);
          const res = JSON.parse(msg.data);
          if (res.op === "stop" || res.op === "start" || res.op === "restart" || res.op === "status") {
            this.optButtonLoading = false;
            this.$message.info(res.msg);
            if (res.code === 200) {
              // 如果操作是启动或者停止
              if (res.op === "stop") {
                this.project = { ...this.project, status: false };
              } else if (res.op === "start") {
                this.project = { ...this.project, status: true };
              } else if (res.op === "status") {
                // 如果是 status
                this.project = { ...this.project, status: true };
              }
            } else {
              this.project = { ...this.project, status: false };
            }
            // return;
          }
        }
        this.$refs.logView.appendLine(msg.data);

        clearInterval(this.heart);
        // 创建心跳，防止掉线
        this.heart = setInterval(() => {
          this.sendMsg("heart");
          this.loadFileSize();
        }, 5000);
      };
    },
    // 发送消息
    sendMsg(op) {
      const data = {
        op: op,
        projectId: this.projectId,
        copyId: this.copyId,
      };
      this.socket.send(JSON.stringify(data));
      if (op === "stop" || op === "start" || op === "restart") {
        this.optButtonLoading = true;
      }
    },

    // 加载日志文件大小
    loadFileSize() {
      const params = {
        nodeId: this.nodeId,
        id: this.projectId,
        copyId: this.copyId,
      };
      getProjectLogSize(params).then((res) => {
        if (res.code === 200) {
          this.project = { ...this.project, logSize: res.data.logSize };
          if (!this.logExist && res.data?.logSize) {
            this.sendMsg("showlog");
            this.logExist = true;
          }
        }
      });
    },
    // 启动
    start() {
      this.sendMsg("start");
    },
    // 重启
    restart() {
      this.$confirm({
        title: this.$t('common.systemPrompt'),
        content: this.$t('node.node_layout.project.console.ifRestart'),
        okText: this.$t('common.confirm'),
        cancelText: this.$t('common.cancel'),
        onOk: () => {
          this.sendMsg("restart");
        },
      });
    },
    // 停止
    stop() {
      this.$confirm({
        title: this.$t('common.systemPrompt'),
        content: this.$t('node.node_layout.project.console.ifStop'),
        okText: this.$t('common.confirm'),
        cancelText: this.$t('common.cancel'),
        onOk: () => {
          this.sendMsg("stop");
        },
      });
    },

    // 日志备份列表
    handleLogBack() {
      // 设置显示的数据
      // this.detailData = [];
      this.lobbackVisible = true;
    },

    goFile() {
      this.$emit("goFile");
    },
  },
};
</script>
