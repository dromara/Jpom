<template>
  <!-- console -->
  <div>
    <log-view2
      ref="logView"
      titleName="容器日志"
      :visible="visible"
      @close="
        () => {
          $emit('close')
        }
      "
    >
      <template v-slot:before>
        <a-space>
          <div>
            <a-input-number v-model:value="tail" placeholder="读取行数" style="width: 150px">
              <template #addonBefore>
                <a-tooltip
                  title="为避免显示内容太多而造成浏览器卡顿,读取日志最后多少行日志。修改后需要回车才能重新读取，小于 1 则读取所有"
                  >行数：
                </a-tooltip>
              </template>
              <!-- <template slot="addonAfter"> </template> -->
            </a-input-number>
          </div>
          <div>
            时间戳：
            <a-switch v-model:checked="timestamps" checked-children="显示" un-checked-children="不显示" />
          </div>
          <a-button type="primary" size="small" @click="initWebSocket"><ReloadOutlined /> 刷新 </a-button>
          |
          <a-button type="primary" :disabled="!this.logId" size="small" @click="download">
            <DownloadOutlined /> 下载
          </a-button>
          |
        </a-space>
      </template>
    </log-view2>
  </div>
</template>

<script>
import { mapState } from 'pinia'
import { getWebSocketUrl } from '@/api/config'
import { dockerContainerDownloaLog } from '@/api/docker-api'
import { useUserStore } from '@/stores/user'
import LogView2 from '@/components/logView'

export default {
  components: {
    LogView2
  },
  props: {
    id: {
      type: String,
      default: ''
    },
    containerId: { type: String },
    machineDockerId: {
      type: String,
      default: ''
    },
    urlPrefix: {
      type: String
    },
    visible: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      socket: null,
      tail: 500,
      timestamps: false,
      logId: ''
    }
  },
  computed: {
    ...mapState(useUserStore, ['getLongTermToken', 'getWorkspaceId']),
    socketUrl() {
      return getWebSocketUrl(
        '/socket/docker_log',
        `userId=${this.getLongTermToken}&id=${this.id}&machineDockerId=${
          this.machineDockerId
        }&type=dockerLog&nodeId=system&workspaceId=${this.getWorkspaceId()}`
      )
    }
  },
  mounted() {
    this.initWebSocket()
    // 监听窗口关闭事件，当窗口关闭时，主动去关闭websocket连接，防止连接还没断开就关闭窗口，server端会抛异常。
    window.onbeforeunload = () => {
      this.close()
    }
  },
  beforeUnmount() {
    this.close()
  },
  methods: {
    close() {
      clearInterval(this.heart)
      this.socket?.close()
    },
    // 初始化
    initWebSocket() {
      this.close()
      this.$refs.logView.clearLogCache()
      //
      let tail = parseInt(this.tail)
      this.tail = isNaN(tail) ? 500 : tail
      //
      this.socket = new WebSocket(this.socketUrl)

      // 连接成功后
      this.socket.onopen = () => {
        this.sendMsg('showlog')
      }
      this.socket.onerror = (err) => {
        console.error(err)
        this.$notification.error({
          message: 'web socket 错误,请检查是否开启 ws 代理'
        })
        clearInterval(this.heart)
      }
      this.socket.onclose = (err) => {
        //当客户端收到服务端发送的关闭连接请求时，触发onclose事件
        console.error(err)
        this.$message.warning('会话已经关闭[docker-log]')
        clearInterval(this.heart)
      }
      this.socket.onmessage = (msg) => {
        if (msg.data.indexOf('code') > -1 && msg.data.indexOf('msg') > -1) {
          try {
            const res = JSON.parse(msg.data)
            if (res.code === 200 && res.msg === 'JPOM_MSG_UUID') {
              this.logId = res.data
              return
            }
          } catch (e) {
            console.error(e)
          }
          //   return;
        }
        const msgLine = msg.data || ''
        // this.$refs.logView.appendLine(msgLine.substring(0, msgLine.lastIndexOf('\n')))
        this.$refs.logView.appendLine(msgLine)
        clearInterval(this.heart)
        // 创建心跳，防止掉线
        this.heart = setInterval(() => {
          this.sendMsg('heart')
        }, 5000)
      }
    },
    // 发送消息
    sendMsg(op) {
      const data = {
        op: op,
        containerId: this.containerId,
        tail: this.tail,
        timestamps: this.timestamps
      }
      this.socket.send(JSON.stringify(data))
    },
    // 下载
    download() {
      window.open(dockerContainerDownloaLog(this.urlPrefix, this.logId), '_blank')
    }
  },
  emits: ['close']
}
</script>
