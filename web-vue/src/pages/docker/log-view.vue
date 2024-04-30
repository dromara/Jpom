<template>
  <!-- console -->
  <div>
    <log-view2
      ref="logView"
      :title-name="$tl('p.containerLog')"
      :visible="visible"
      @close="
        () => {
          $emit('close')
        }
      "
    >
      <template #before>
        <a-space>
          <div>
            <a-input-number v-model:value="tail" :placeholder="$tl('p.readRowCount')" style="width: 150px">
              <template #addonBefore>
                <a-tooltip :title="$tl('p.readLogLastLines')">{{ $tl('p.rowCount') }} </a-tooltip>
              </template>
            </a-input-number>
          </div>
          <div>
            {{ $tl('p.timestamp') }}
            <a-switch
              v-model:checked="timestamps"
              :checked-children="$tl('p.show')"
              un-checked-children="不{{$tl('p.show')}}"
            />
          </div>
          <a-button type="primary" size="small" @click="initWebSocket"
            ><ReloadOutlined /> {{ $tl('p.refresh') }}
          </a-button>
          |
          <a-button type="primary" :disabled="!logId" size="small" @click="download">
            <DownloadOutlined /> {{ $tl('p.download') }}
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
import { useAppStore } from '@/stores/app'
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
    containerId: { type: String, default: '' },
    machineDockerId: {
      type: String,
      default: ''
    },
    urlPrefix: {
      type: String,
      default: ''
    },
    visible: {
      type: Boolean,
      default: false
    }
  },
  emits: ['close'],
  data() {
    return {
      socket: null,
      tail: 500,
      timestamps: false,
      logId: ''
    }
  },
  computed: {
    ...mapState(useUserStore, ['getLongTermToken']),
    ...mapState(useAppStore, ['getWorkspaceId']),
    socketUrl() {
      return getWebSocketUrl(
        '/socket/docker_log',
        `userId=${this.getLongTermToken()}&id=${this.id}&machineDockerId=${this.machineDockerId}&type=dockerLog&nodeId=system&workspaceId=${this.getWorkspaceId()}`
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
    $tl(key, ...args) {
      return this.$t(`pages.docker.logView.${key}`, ...args)
    },
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
        $notification.error({
          message: `web socket ${this.$tl('p.error')},${this.$tl('p.checkWsProxy')}`
        })
        clearInterval(this.heart)
      }
      this.socket.onclose = (err) => {
        //当客户端收到服务端发送的关闭连接请求时，触发onclose事件
        console.error(err)
        clearInterval(this.heart)
        $message.warning(this.$tl('p.sessionClosed'))
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
  }
}
</script>
