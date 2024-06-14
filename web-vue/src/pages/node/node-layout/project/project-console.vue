<template>
  <div>
    <!-- console -->
    <log-view2 :ref="`logView`" height="calc(100vh - 140px)">
      <template #before>
        <a-space>
          <a-button size="small" :disabled="project.status" :loading="optButtonLoading" type="primary" @click="start">{{
            $t('pages.node.node-layout.project.project-console.15f9c981')
          }}</a-button>
          <a-button
            size="small"
            :disabled="!project.status"
            :loading="optButtonLoading"
            type="primary"
            danger
            @click="restart"
            >{{ $t('pages.node.node-layout.project.project-console.80c35a66') }}</a-button
          >
          <a-button
            size="small"
            :disabled="!project.status"
            :loading="optButtonLoading"
            type="primary"
            danger
            @click="stop"
            >{{ $t('pages.node.node-layout.project.project-console.5c03f4ad') }}</a-button
          >
          <template v-if="project.runMode === 'Dsl'">
            <template v-if="canReload">
              <a-popover :title="$t('pages.node.node-layout.project.project-console.376f2896')">
                <template #content>
                  <template v-if="project.lastReloadResult">
                    <p>
                      <a-tag v-if="project.lastReloadResult.success" color="green">{{
                        $t('pages.node.node-layout.project.project-console.6e06fe4f')
                      }}</a-tag>
                      <a-tag v-else color="green">{{
                        $t('pages.node.node-layout.project.project-console.6e06fe4f')
                      }}</a-tag>
                    </p>
                    <p v-for="(item, index) in project.lastReloadResult.msgs" :key="index">
                      {{ item }}
                    </p>
                  </template>
                  <template v-else>{{ $t('pages.node.node-layout.project.project-console.fdc695b2') }}</template>
                </template>
                <a-button size="small" :loading="optButtonLoading" type="primary" @click="reload">{{
                  $t('pages.node.node-layout.project.project-console.b7be5506')
                }}</a-button>
              </a-popover>
            </template>
            <template v-else>
              <a-button size="small" :disabled="true" :loading="optButtonLoading" type="primary">{{
                $t('pages.node.node-layout.project.project-console.b7be5506')
              }}</a-button>
            </template>
          </template>
          <a-button size="small" type="primary" @click="goFile">{{
            $t('pages.node.node-layout.project.project-console.502f94')
          }}</a-button>
          <a-dropdown v-if="project.dslProcessInfo">
            <template #overlay>
              <a-menu>
                <a-menu-item v-for="(item, index) in project.dslProcessInfo" :key="index">
                  <template v-if="item.status">
                    <a-tag>
                      {{ item.process }}
                    </a-tag>
                    <template v-if="item.type === 'file'"
                      >{{ $t('pages.node.node-layout.project.project-console.dcbad93b') }}{{ item.scriptId }}
                    </template>
                    <template v-else-if="item.type === 'script'">
                      <a-button
                        type="link"
                        size="small"
                        @click="
                          () => {
                            temp = { scriptId: item.scriptId }
                            editScriptVisible = true
                          }
                        "
                      >
                        <EditOutlined /> {{ $t('pages.node.node-layout.project.project-console.e0417750') }}
                      </a-button>
                    </template>
                  </template>
                  <template v-else>
                    <a-space>
                      <a-tag>
                        {{ item.process }}
                      </a-tag>

                      <ExclamationCircleOutlined />
                      {{ item.msg }}
                    </a-space>
                  </template>
                </a-menu-item>
              </a-menu>
            </template>
            <a-button size="small" type="primary">
              {{ $t('pages.node.node-layout.project.project-console.5453aa0b') }} <DownOutlined />
            </a-button>
          </a-dropdown>
          <a-button
            size="small"
            @click="
              (e) => {
                e.preventDefault()
                handleLogBack()
              }
            "
          >
            <!-- <a-tag> -->
            {{ $t('pages.node.node-layout.project.project-console.1246c314') }}: {{ project.logSize || '-' }}
            <!-- 更多 -->
            <FullscreenOutlined />
            <!-- </a-tag> -->
          </a-button>

          |
        </a-space>
      </template>
    </log-view2>
    <!-- 日志备份 -->
    <a-modal
      v-model:open="lobbackVisible"
      destroy-on-close
      :title="$t('pages.node.node-layout.project.project-console.111eece')"
      width="850px"
      :footer="null"
      :mask-closable="false"
    >
      <ProjectLog v-if="lobbackVisible" :node-id="nodeId" :project-id="projectId"></ProjectLog>
    </a-modal>
    <!-- 编辑区 -->
    <ScriptEdit
      v-if="editScriptVisible"
      :node-id="nodeId"
      :script-id="temp.scriptId"
      @close="
        () => {
          editScriptVisible = false
        }
      "
    ></ScriptEdit>
  </div>
</template>
<script>
import { getProjectData, getProjectLogSize } from '@/api/node-project'
import { getWebSocketUrl } from '@/api/config'
import { mapState } from 'pinia'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import LogView2 from '@/components/logView/index2'
import ProjectLog from './project-log'
import ScriptEdit from '@/pages/node/script-edit'
export default {
  components: {
    LogView2,
    ProjectLog,
    ScriptEdit
  },
  props: {
    nodeId: {
      type: String,
      default: ''
    },
    projectId: {
      type: String,
      default: ''
    },
    id: {
      type: String,
      default: ''
    }
  },
  emits: ['goFile'],
  data() {
    return {
      project: {},
      optButtonLoading: true,
      loading: false,
      socket: null,
      logExist: false,
      lobbackVisible: false,
      canReload: false,
      heart: null,
      editScriptVisible: false
    }
  },
  computed: {
    ...mapState(useUserStore, ['getLongTermToken']),
    ...mapState(useAppStore, ['getWorkspaceId']),
    socketUrl() {
      return getWebSocketUrl(
        '/socket/console',
        `userId=${this.getLongTermToken()}&id=${this.id}&nodeId=${
          this.nodeId
        }&type=console&workspaceId=${this.getWorkspaceId()}`
      )
    }
  },
  mounted() {
    this.loadProject()
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
      this.socket?.close()

      clearInterval(this.heart)
    },
    // 加载项目
    loadProject(loading) {
      const params = {
        id: this.projectId,
        nodeId: this.nodeId
      }
      getProjectData(params, loading).then((res) => {
        if (res.code === 200) {
          this.project = { ...this.project, ...res.data }

          // 加载日志文件大小
          this.loadFileSize()
        }
      })
    },
    // 初始化
    initWebSocket() {
      //this.logContext = "";
      if (
        !this.socket ||
        this.socket.readyState !== this.socket.OPEN ||
        this.socket.readyState !== this.socket.CONNECTING
      ) {
        this.socket = new WebSocket(this.socketUrl)
      }
      // 连接成功后
      this.socket.onopen = () => {
        this.sendMsg('status')
        this.sendMsg('showlog')
      }
      this.socket.onerror = (err) => {
        console.error(err)
        $notification.error({
          message: `web socket ${this.$t('pages.node.node-layout.project.project-console.d75d207f')},${this.$t(
            'pages.node.node-layout.project.project-console.763330b'
          )}`
        })
        clearInterval(this.heart)
      }
      this.socket.onclose = (err) => {
        //当客户端收到服务端发送的关闭连接请求时，触发onclose事件
        console.error(err)
        $message.warning(this.$t('pages.node.node-layout.project.project-console.8a2aae09'))
        clearInterval(this.heart)
      }
      this.socket.onmessage = (msg) => {
        if (msg.data.indexOf('JPOM_MSG') > -1 && msg.data.indexOf('op') > -1) {
          // console.log(msg.data);
          const res = JSON.parse(msg.data)
          if (
            res.op === 'stop' ||
            res.op === 'start' ||
            res.op === 'restart' ||
            res.op === 'status' ||
            res.op === 'reload'
          ) {
            this.optButtonLoading = false
            $message.info(res.msg)
            if (res.code === 200) {
              // 如果操作是启动或者停止
              if (res.op === 'stop') {
                this.project = { ...this.project, status: false }
              } else if (res.op === 'start') {
                this.project = { ...this.project, status: true }
              } else if (res.op === 'status') {
                // 如果是 status
                this.project = { ...this.project, status: true }
              }
              if (res.op === 'reload') {
                // 刷新项目信息（reload页面消息）
                this.loadProject()
              }
            } else {
              this.project = { ...this.project, status: false }
            }
            this.canReload = res.canReload
            if (res.data) {
              this.$refs.logView.appendLine(res.data.statusMsg)
              if (res.data.msgs) {
                res.data.msgs.forEach((element) => {
                  this.$refs.logView.appendLine(element)
                })
              }
              res.data.ports &&
                this.$refs.logView.appendLine(
                  this.$t('pages.node.node-layout.project.project-console.a6c4bfd7') + res.data.ports
                )
              res.data.pids &&
                this.$refs.logView.appendLine(
                  this.$t('pages.node.node-layout.project.project-console.3cf2b4f7') + res.data.pids.join(',')
                )
            }
            this.$refs.logView.appendLine(res.op + ' ' + res.msg)
            return
          }
        }
        this.$refs.logView.appendLine(msg.data)

        clearInterval(this.heart)
        // 创建心跳，防止掉线
        this.heart = setInterval(() => {
          this.sendMsg('heart')
          this.loadFileSize()
        }, 5000)
      }
    },
    // 发送消息
    sendMsg(op) {
      const data = {
        op: op,
        projectId: this.projectId
      }
      this.socket.send(JSON.stringify(data))
      if (op === 'stop' || op === 'start' || op === 'restart') {
        this.optButtonLoading = true
      }
    },

    // 加载日志文件大小
    loadFileSize() {
      const params = {
        nodeId: this.nodeId,
        id: this.projectId
      }
      getProjectLogSize(params).then((res) => {
        if (res.code === 200) {
          this.project = { ...this.project, logSize: res.data.logSize }
          if (!this.logExist && res.data?.logSize) {
            this.sendMsg('showlog')
            this.logExist = true
          }
        }
      })
    },
    // 启动
    start() {
      this.sendMsg('start')
    },
    // 重载
    reload() {
      this.sendMsg('reload')
    },
    // 重启
    restart() {
      $confirm({
        title: this.$t('pages.node.node-layout.project.project-console.1da61c06'),
        zIndex: 1009,
        content: this.$t('pages.node.node-layout.project.project-console.fb372d35'),
        okText: this.$t('pages.node.node-layout.project.project-console.7da4a591'),
        cancelText: this.$t('pages.node.node-layout.project.project-console.43105e21'),
        onOk: () => {
          this.sendMsg('restart')
        }
      })
    },
    // 停止
    stop() {
      $confirm({
        title: this.$t('pages.node.node-layout.project.project-console.1da61c06'),
        zIndex: 1009,
        content: this.$t('pages.node.node-layout.project.project-console.2e391eba'),
        okText: this.$t('pages.node.node-layout.project.project-console.7da4a591'),
        cancelText: this.$t('pages.node.node-layout.project.project-console.43105e21'),
        onOk: () => {
          this.sendMsg('stop')
        }
      })
    },

    // 日志备份列表
    handleLogBack() {
      // 设置显示的数据
      // this.detailData = [];
      this.lobbackVisible = true
    },

    goFile() {
      this.$emit('goFile')
    }
  }
}
</script>
