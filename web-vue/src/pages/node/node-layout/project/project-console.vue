<template>
  <div>
    <!-- console -->
    <log-view2 :ref="`logView`" height="calc(100vh - 140px)">
      <template #before>
        <a-space>
          <a-button size="small" :disabled="project.status" :loading="optButtonLoading" type="primary" @click="start">{{
            $t('i18n_8e54ddfe24')
          }}</a-button>
          <a-button
            size="small"
            :disabled="!project.status"
            :loading="optButtonLoading"
            type="primary"
            danger
            @click="restart"
            >{{ $t('i18n_01b4e06f39') }}</a-button
          >
          <a-button
            size="small"
            :disabled="!project.status"
            :loading="optButtonLoading"
            type="primary"
            danger
            @click="stop"
            >{{ $t('i18n_095e938e2a') }}</a-button
          >
          <template v-if="project.runMode === 'Dsl'">
            <template v-if="canReload">
              <a-popover :title="$t('i18n_8b2e274414')">
                <template #content>
                  <template v-if="project.lastReloadResult">
                    <p>
                      <a-tag v-if="project.lastReloadResult.success" color="green">{{ $t('i18n_330363dfc5') }}</a-tag>
                      <a-tag v-else color="green">{{ $t('i18n_330363dfc5') }}</a-tag>
                    </p>
                    <p v-for="(item, index) in project.lastReloadResult.msgs" :key="index">
                      {{ item }}
                    </p>
                  </template>
                  <template v-else>{{ $t('i18n_14dcfcc4fa') }}</template>
                </template>
                <a-button size="small" :loading="optButtonLoading" type="primary" @click="reload">{{
                  $t('i18n_aaeb54633e')
                }}</a-button>
              </a-popover>
            </template>
            <template v-else>
              <a-button size="small" :disabled="true" :loading="optButtonLoading" type="primary">{{
                $t('i18n_aaeb54633e')
              }}</a-button>
            </template>
          </template>
          <a-button size="small" type="primary" @click="goFile">{{ $t('i18n_8780e6b3d1') }}</a-button>
          <a-dropdown v-if="project.dslProcessInfo">
            <template #overlay>
              <a-menu>
                <a-menu-item v-for="(item, index) in project.dslProcessInfo" :key="index">
                  <template v-if="item.status">
                    <a-tag>
                      {{ item.process }}
                    </a-tag>
                    <template v-if="item.type === 'file'">{{ $t('i18n_4df483b9c7') }}{{ item.scriptId }} </template>
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
                        <EditOutlined /> {{ $t('i18n_e0ba3b9145') }}
                      </a-button>
                    </template>
                    <template v-else-if="item.type === 'library'">
                      <a-button type="link" size="small" disabled=""
                        >{{ $t('i18n_91a10b8776') }}{{ item.scriptId }}</a-button
                      >
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
            <a-button size="small" type="primary"> {{ $t('i18n_ce40cd6390') }} <DownOutlined /> </a-button>
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
            {{ $t('i18n_76aebf3cc6') }}: {{ project.logSize || '-' }}
            <!-- 更多 -->
            <FullscreenOutlined />
            <!-- </a-tag> -->
          </a-button>

          |
        </a-space>
      </template>
    </log-view2>
    <!-- 日志备份 -->
    <CustomModal
      v-if="lobbackVisible"
      v-model:open="lobbackVisible"
      destroy-on-close
      :title="$t('i18n_15f01c43e8')"
      width="850px"
      :footer="null"
      :mask-closable="false"
    >
      <ProjectLog v-if="lobbackVisible" :node-id="nodeId" :project-id="projectId"></ProjectLog>
    </CustomModal>
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
          message: `web socket ${this.$t('i18n_7030ff6470')},${this.$t('i18n_226a6f9cdd')}`
        })
        clearInterval(this.heart)
      }
      this.socket.onclose = (err) => {
        //当客户端收到服务端发送的关闭连接请求时，触发onclose事件
        console.error(err)
        $message.warning(this.$t('i18n_d6cdafe552'))
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
              res.data.ports && this.$refs.logView.appendLine(this.$t('i18n_b6c9619081') + res.data.ports)
              res.data.pids && this.$refs.logView.appendLine(this.$t('i18n_2b04210d33') + res.data.pids.join(','))
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
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_989f1f2b61'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          this.sendMsg('restart')
        }
      })
    },
    // 停止
    stop() {
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_010865ca50'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
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
