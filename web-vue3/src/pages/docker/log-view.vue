<template>
  <!-- console -->
  <div>
    <log-view ref="logView" seg="" height="60vh" marginTop="-10px">
      <template slot="before">
        <a-space>
          <a-tooltip
            title="为避免显示内容太多而造成浏览器卡顿,读取日志最后多少行日志。修改后需要回车才能重新读取，小于 1 则读取所有"
          >
            读取行数：
            <a-input-number v-model="tail" placeholder="读取行数">
              <!-- <template slot="addonAfter"> </template> -->
            </a-input-number>
          </a-tooltip>
          <div>
            时间戳：
            <a-switch v-model="timestamps" checked-children="显示" un-checked-children="不显示" />
          </div>
          <a-button type="primary" icon="reload" size="small" @click="initWebSocket"> 刷新 </a-button>

          |
        </a-space>
      </template>
    </log-view>
  </div>
</template>
<script>
import { mapGetters } from 'vuex'
import { getWebSocketUrl } from '@/api/config'

import LogView from '@/components/logView'

export default {
  components: {
    LogView
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
    }
  },
  data() {
    return {
      socket: null,
      tail: 500,
      timestamps: false
    }
  },
  computed: {
    ...mapGetters(['getLongTermToken', 'getWorkspaceId']),
    socketUrl() {
      return getWebSocketUrl(
        '/socket/docker_log',
        `userId=${this.getLongTermToken}&id=${this.id}&machineDockerId=${this.machineDockerId}&type=dockerLog&nodeId=system&workspaceId=${this.getWorkspaceId}`
      )
    }
  },
  mounted() {
    this.initWebSocket()
  },
  beforeDestroy() {
    this.close()
  },
  methods: {
    close() {
      if (this.socket) {
        this.socket.close()
      }
      clearInterval(this.heart)
    },
    // 初始化
    initWebSocket() {
      this.close()
      this.$refs.logView.clearLogCache()
      //
      let tail = parseInt(this.tail)
      this.tail = isNaN(tail) ? 500 : tail
      //
      if (
        !this.socket ||
        this.socket.readyState !== this.socket.OPEN ||
        this.socket.readyState !== this.socket.CONNECTING
      ) {
        this.socket = new WebSocket(this.socketUrl)
      }
      // 连接成功后
      this.socket.onopen = () => {
        this.sendMsg('showlog')
      }
      this.socket.onerror = (err) => {
        console.error(err)
        $notification.error({
          message: 'web socket 错误,请检查是否开启 ws 代理'
        })
        clearInterval(this.heart)
      }
      this.socket.onclose = (err) => {
        //当客户端收到服务端发送的关闭连接请求时，触发onclose事件
        console.error(err)
        $message.warning('会话已经关闭')
        clearInterval(this.heart)
      }
      this.socket.onmessage = (msg) => {
        // if (msg.data.indexOf("code") > -1 && msg.data.indexOf("msg") > -1) {
        //   const res = JSON.parse(msg.data);
        //   if (res.code === 200) {
        //     $notification.success({
        //       message: res.msg,
        //     });
        //   } else {
        //     $notification.error({
        //       message: res.msg,
        //     });
        //   }
        //   return;
        // }
        const msgLine = msg.data || ''
        this.$refs.logView.appendLine(msgLine.substring(0, msgLine.lastIndexOf('\n')))
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
    }
  }
}
</script>
<style scoped></style>
