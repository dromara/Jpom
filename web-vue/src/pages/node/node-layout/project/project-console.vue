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
          <a-button size="small" :disabled="project.status" :loading="optButtonLoading" type="primary" @click="start">启动</a-button>
          <a-button size="small" :disabled="!project.status" :loading="optButtonLoading" type="danger" @click="restart">重启</a-button>
          <a-button size="small" :disabled="!project.status" :loading="optButtonLoading" type="danger" @click="stop">停止</a-button>

          <a-button size="small" v-if="!copyId" type="primary" @click="goFile">文件管理</a-button>

          <a-dropdown>
            <!-- <a type="link" class="ant-dropdown-link"> 更多<a-icon type="down" /> </a> -->
            <a class="ant-dropdown-link" @click="(e) => e.preventDefault()">
              <a-tag>
                文件大小: {{ project.logSize || "-" }}
                <!-- 更多 -->
                <a-icon type="down" />
              </a-tag>
            </a>
            <a-menu slot="overlay">
              <a-menu-item>
                <a-button type="primary" :disabled="!project.logSize" @click="handleDownload">导出日志</a-button>
              </a-menu-item>
              <a-menu-item>
                <a-button type="primary" @click="handleLogBack">备份列表</a-button>
              </a-menu-item>
            </a-menu>
          </a-dropdown>
        </a-space>
      </template>
    </log-view>
    <!-- 日志备份 -->
    <a-modal v-model="lobbackVisible" title="日志备份列表" width="850px" :footer="null" :maskClosable="false">
      <div ref="model-filter" class="filter">
        <a-space direction="vertical">
          <a-tag>控制台日志只是启动项目输出的日志信息,并非项目日志。可以关闭控制台日志备份功能：<b>log.autoBackConsoleCron: none</b></a-tag>

          <a-tag color="orange">控制台日志路径: {{ project.log }}</a-tag>
          <a-tag color="orange">控制台日志备份路径: {{ project.logBack }}</a-tag>
        </a-space>
      </div>
      <!-- 数据表格 -->
      <a-table :data-source="logBackList" :loading="loading" :columns="columns" :scroll="{ y: 400 }" :pagination="false" bordered :rowKey="(record, index) => index">
        <a-tooltip slot="filename" slot-scope="text" placement="topLeft" :title="text">
          <span>{{ text }}</span>
        </a-tooltip>
        <a-tooltip slot="fileSize" slot-scope="text" placement="topLeft" :title="text">
          <span>{{ text }}</span>
        </a-tooltip>
        <template slot="operation" slot-scope="text, record">
          <a-space>
            <a-button type="primary" @click="handleDownloadLogback(record)">下载</a-button>
            <a-button type="danger" @click="handleDelete(record)">删除</a-button>
          </a-space>
        </template>
      </a-table>
    </a-modal>
  </div>
</template>
<script>
import {deleteProjectLogBackFile, downloadProjectLogBackFile, downloadProjectLogFile, getLogBackList, getProjectData, getProjectLogSize} from "@/api/node-project";
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

      lobbackVisible: false,
      logBackList: [],
      columns: [
        { title: "文件名称", dataIndex: "filename", width: 150, ellipsis: true, scopedSlots: { customRender: "filename" } },
        { title: "修改时间", dataIndex: "modifyTime", width: 150, ellipsis: true, scopedSlots: { customRender: "modifyTime" } },
        { title: "文件大小", dataIndex: "fileSize", width: 100, ellipsis: true, scopedSlots: { customRender: "fileSize" } },
        { title: "操作", dataIndex: "operation", scopedSlots: { customRender: "operation" }, width: 130 },
      ],
      heart: null,
    };
  },
  computed: {
    ...mapGetters(["getLongTermToken"]),
    socketUrl() {
      return getWebSocketUrl("/socket/console", `userId=${this.getLongTermToken}&id=${this.id}&nodeId=${this.nodeId}&type=console&copyId=${this.copyId || ""}`);
    },
  },
  mounted() {
    this.loadProject();
    this.initWebSocket();
  },
  beforeDestroy() {
    if (this.socket) {
      this.socket.close();
    }
    clearInterval(this.heart);
  },
  methods: {
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
                  message: "没有找到副本",
                });
              }
            } else {
              this.$notification.error({
                message: "没有副本",
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
        if (msg.data.indexOf("JPOM_MSG") > -1 && msg.data.indexOf("op") > -1) {
          // console.log(msg.data);
          const res = JSON.parse(msg.data);
          if (res.op === "stop" || res.op === "start" || res.op === "restart" || res.op === "status") {
            this.optButtonLoading = false;
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg,
              });
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
              this.$notification.error({
                message: res.msg,
              });
              this.project = { ...this.project, status: false };
            }
            // return;
          }
        }
        this.$refs.logView.appendLine(msg.data);
        // if (this.searchReg) {
        //   this.logContextArray.push(msg.data.replace(this.searchReg, this.regReplaceText));
        // } else {
        //   this.logContextArray.push(msg.data);
        // }
        // let logShowLineTemp = parseInt(this.logShowLine);
        // logShowLineTemp = isNaN(logShowLineTemp) ? this.defLogShowLine : logShowLineTemp;
        // logShowLineTemp = logShowLineTemp > 0 ? logShowLineTemp : 1;
        // if (this.logScroll === "true") {
        //   this.logContextArray = this.logContextArray.slice(-logShowLineTemp);
        // }

        // // 自动滚动到底部
        // this.$nextTick(() => {
        //   const projectConsole = document.getElementById("project-console");
        //   projectConsole.innerHTML = this.logContextArray.join("</br>");
        //   if (this.logScroll === "true") {
        //     setTimeout(() => {
        //       projectConsole.scrollTop = projectConsole.scrollHeight;
        //     }, 100);
        //   }
        // });

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
        title: "系统提示",
        content: "真的要重启项目么？",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          this.sendMsg("restart");
        },
      });
    },
    // 停止
    stop() {
      this.$confirm({
        title: "系统提示",
        content: "真的要停止项目么？",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          this.sendMsg("stop");
        },
      });
    },
    // 下载日志文件
    handleDownload() {
      this.$notification.info({
        message: "正在下载，请稍等...",
      });
      // 请求参数
      const params = {
        nodeId: this.nodeId,
        id: this.projectId,
        copyId: this.copyId,
      };
      // 请求接口拿到 blob
      downloadProjectLogFile(params).then((blob) => {
        const url = window.URL.createObjectURL(blob);
        let link = document.createElement("a");
        link.style.display = "none";
        link.href = url;
        if (this.copyId) {
          link.setAttribute("download", `${this.projectId}-${this.copyId}.log`);
        } else {
          link.setAttribute("download", `${this.projectId}.log`);
        }
        document.body.appendChild(link);
        link.click();
      });
    },
    // 日志备份列表
    handleLogBack() {
      this.loading = true;
      // 设置显示的数据
      this.detailData = [];
      this.lobbackVisible = true;
      const params = {
        nodeId: this.nodeId,
        id: this.projectId,
        copyId: this.copyId,
      };
      getLogBackList(params).then((res) => {
        if (res.code === 200) {
          this.logBackList = res.data.array;
        }
        this.loading = false;
      });
    },
    // 下载日志备份文件
    handleDownloadLogback(record) {
      this.$notification.info({
        message: "正在下载，请稍等...",
      });
      // 请求参数
      const params = {
        nodeId: this.nodeId,
        id: this.projectId,
        copyId: this.copyId,
        key: record.filename,
      };
      // 请求接口拿到 blob
      downloadProjectLogBackFile(params).then((blob) => {
        const url = window.URL.createObjectURL(blob);
        let link = document.createElement("a");
        link.style.display = "none";
        link.href = url;
        link.setAttribute("download", record.filename);
        document.body.appendChild(link);
        link.click();
      });
    },
    // 删除日志备份文件
    handleDelete(record) {
      this.$confirm({
        title: "系统提示",
        content: "真的要删除文件么？",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 请求参数
          const params = {
            nodeId: this.nodeId,
            id: this.projectId,
            copyId: this.copyId,
            name: record.filename,
          };
          // 删除
          deleteProjectLogBackFile(params).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg,
              });
              this.handleLogBack();
            }
          });
        },
      });
    },
    goFile() {
      this.$emit("goFile");
    },
  },
};
</script>
<style scoped>
/* .filter {
  margin: 0 0 10px;
} */
/*
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
} */
</style>
