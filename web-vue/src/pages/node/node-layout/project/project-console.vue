<template>
  <div>
    <div ref="filter" class="filter">
      <a-button :disabled="project.status" type="primary" @click="start">启动</a-button>
      <a-button :disabled="!project.status" type="danger" @click="restart">重启</a-button>
      <a-button :disabled="!project.status" type="danger" @click="stop">停止</a-button>
      <a-button type="primary" @click="handleDownload">导出日志</a-button>
      <a-button type="primary" >备份列表</a-button>
      <a-tag color="#87d068">文件大小: {{project.logSize}}</a-tag>
    </div>
    <div>
      <a-input class="console" v-model="logContext" readOnly type="textarea" style="resize: none;"/>
    </div>
  </div>
</template>
<script>
import { getProjectLogSize, downloadProjectLogFile } from '../../../../api/node-project';
import { mapGetters } from 'vuex';
export default {
  props: {
    node: {
      type: Object
    },
    project: {
      type: Object
    },
    copyId: {
      type: String
    }
  },
  data() {
    return {
      socket: null,
      // 日志内容
      logContext: '',
    }
  },
  computed: {
    ...mapGetters([
      'getToken'
    ]),
    socketUrl() {
      const protocol = location.protocol === 'https' ? 'wss://' : 'ws://';
      return `${protocol}${location.host}/console?userId=${this.getToken}&projectId=${this.project.id}&nodeId=${this.node.id}&type=console&copyId=${this.copyId}`;
    }
  },
  mounted() {
    this.initWebSocket();
    this.loadFileSize();
  },
  beforeDestroy() {
    this.socket.close();
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
        this.sendMsg('status');
        this.sendMsg('showlog');
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
              this.project.status = false;
            }
            if (res.op === 'start') {
              this.project.status = true;
            }
          } else {
            this.$notification.error({
              message: res.msg,
              duration: 5
            });
          }
        }
        this.logContext += `${msg.data}\r\n`;
      }
    },
    // 发送消息
    sendMsg(op) {
      const data = {
        op: op,
        projectId: this.project.id,
        copyId: this.copyId
      }
      this.socket.send(JSON.stringify(data));
    },
    // 加载日志文件大小
    loadFileSize() {
      const params = {
        nodeId: this.node.id,
        id: this.project.id,
        copyId: this.copyId
      }
      getProjectLogSize(params).then(res => {
        if (res.code === 200) {
          this.project.logSize = res.data.logSize;
        }
      })
    },
    // 启动
    start() {
      this.sendMsg('start');
    },
    // 重启
    restart() {
      this.$confirm({
        title: '系统提示',
        content: '真的要重启项目么？',
        okText: '确认',
        cancelText: '取消',
        onOk: () => {
          this.sendMsg('restart');
        }
      });
    },
    // 停止
    stop() {
      this.$confirm({
        title: '系统提示',
        content: '真的要重启项目么？',
        okText: '确认',
        cancelText: '取消',
        onOk: () => {
          this.sendMsg('stop');
        }
      });
    },
    // 下载日志文件
    handleDownload() {
      this.$notification.info({
        message: '正在下载，请稍等...',
        duration: 5
      });
      // 请求参数
      const params = {
        nodeId: this.node.id,
        id: this.project.id,
        copyId: this.copyId
      }
      // 请求接口拿到 blob
      downloadProjectLogFile(params).then(blob => {
        const url = window.URL.createObjectURL(blob);
        let link = document.createElement('a');
        link.style.display = 'none';
        link.href = url;
        link.setAttribute('download', this.project.log);
        document.body.appendChild(link);
        link.click();
      })
    }
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