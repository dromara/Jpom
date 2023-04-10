<template>
  <div>
    <!-- <div ref="filter" class="filter">
        <a-space> </a-space>
      </div> -->
    <!-- console -->
    <div>
      <log-view :ref="`logView`" height="calc(100vh - 140px)">
        <template slot="before">
          <a-space>
            <a-button size="small" :loading="btnLoading" :disabled="scriptStatus !== 0" type="primary" @click="start"
              >执行</a-button
            >
            <a-button size="small" :loading="btnLoading" :disabled="scriptStatus !== 1" type="primary" @click="stop"
              >停止</a-button
            >
          </a-space>
        </template>
      </log-view>
    </div>

    <!--远程下载  -->
    <a-modal destroyOnClose v-model="editArgs" title="添加运行参数" @ok="startExecution" :maskClosable="false">
      <a-form :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }" ref="ruleForm">
        <a-form-item
          label="命令参数"
          :help="`${
            commandParams.length
              ? '所有参数将拼接成字符串以空格分隔形式执行脚本,需要注意参数顺序和未填写值的参数将自动忽略'
              : ''
          }`"
        >
          <a-row v-for="(item, index) in commandParams" :key="item.key">
            <a-col :span="22">
              <a-input
                :addon-before="`参数${index + 1}值`"
                v-model="item.value"
                :placeholder="`参数值 ${item.desc ? ',' + item.desc : ''}`"
              >
                <template slot="suffix">
                  <a-tooltip v-if="item.desc" :title="item.desc">
                    <a-icon type="info-circle" style="color: rgba(0, 0, 0, 0.45)" />
                  </a-tooltip>
                </template>
              </a-input>
            </a-col>

            <a-col v-if="!item.desc" :span="2">
              <a-row type="flex" justify="center" align="middle">
                <a-col>
                  <a-icon type="minus-circle" @click="() => commandParams.splice(index, 1)" style="color: #ff0000" />
                </a-col>
              </a-row>
            </a-col>
          </a-row>
          <a-button type="primary" size="small" @click="() => commandParams.push({})">添加参数</a-button>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>
<script>
import { mapGetters } from 'vuex'
import { getWebSocketUrl } from '@/utils/const'
import LogView from '@/components/logView'

export default {
  components: {
    LogView
  },
  props: {
    nodeId: { type: String },
    scriptId: {
      type: String
    },
    id: {
      type: String
    },
    defArgs: { type: String }
  },
  data() {
    return {
      project: {},
      socket: null,
      // script 状态 {0: 未运行, 1: 运行中}
      scriptStatus: 0,
      editArgs: false,
      temp: {
        // args: "",
      },
      commandParams: [],
      // 日志内容
      // logContext: "loading ...",
      btnLoading: true
    }
  },
  computed: {
    ...mapGetters(['getLongTermToken', 'getWorkspaceId']),
    socketUrl() {
      return getWebSocketUrl(
        '/socket/node/script_run',
        `userId=${this.getLongTermToken}&id=${this.id}&nodeId=${this.nodeId}&type=nodeScript&workspaceId=${this.getWorkspaceId}`
      )
    }
  },
  mounted() {
    this.initWebSocket()
    if (typeof this.defArgs === 'string' && this.defArgs) {
      this.commandParams = JSON.parse(this.defArgs)
    } else {
      this.commandParams = []
    }
  },
  beforeDestroy() {
    if (this.socket) {
      this.socket.close()
    }
    clearInterval(this.heart)
  },
  methods: {
    // 初始化
    initWebSocket() {
      this.logContext = ''
      if (
        !this.socket ||
        this.socket.readyState !== this.socket.OPEN ||
        this.socket.readyState !== this.socket.CONNECTING
      ) {
        this.socket = new WebSocket(this.socketUrl)
      }
      // 连接成功后
      this.socket.onopen = () => {
        // this.logContext = "connect success......\r\n";
        this.btnLoading = false
      }
      this.socket.onerror = (err) => {
        console.error(err)
        this.$notification.error({
          message: 'web socket 错误,请检查是否开启 ws 代理'
        })
        clearInterval(this.heart)
        this.btnLoading = true
      }
      this.socket.onclose = (err) => {
        //当客户端收到服务端发送的关闭连接请求时，触发onclose事件
        console.error(err)
        this.$message.warning('会话已经关闭')
        clearInterval(this.heart)
        this.btnLoading = true
      }
      this.socket.onmessage = (msg) => {
        if (msg.data.indexOf('JPOM_MSG') > -1 && msg.data.indexOf('op') > -1) {
          const res = JSON.parse(msg.data)
          if (res.code === 200) {
            this.$notification.success({
              message: res.msg
            })
            // 如果操作是启动或者停止
            if (res.op === 'stop') {
              this.scriptStatus = 0
            }
            if (res.op === 'start') {
              this.scriptStatus = 1
            }
            if (res.executeId) {
              this.temp = { ...this.temp, executeId: res.executeId }
            }
          } else {
            this.$notification.error({
              message: res.msg
            })
            this.scriptStatus = 0
          }
          return
        }
        // this.logContext += `${msg.data}\r\n`;
        this.$refs.logView.appendLine(msg.data)
        clearInterval(this.heart)
        // 创建心跳，防止掉线
        this.heart = setInterval(() => {
          this.sendMsg('heart')
        }, 5000)
      }
    },
    startExecution() {
      this.editArgs = false
      this.sendMsg('start')
    },
    // 发送消息
    sendMsg(op) {
      const data = {
        op: op,
        scriptId: this.scriptId,
        args: JSON.stringify(this.commandParams),
        executeId: this.temp.executeId
      }
      this.socket.send(JSON.stringify(data))
    },
    // 启动
    start() {
      this.editArgs = true
    },
    // 停止
    stop() {
      this.sendMsg('stop')
    }
  }
}
</script>
<style scoped>
.script-console-layout {
  padding: 0;
  margin: 0;
}

.filter {
  margin: 0 0 10px;
}
</style>
