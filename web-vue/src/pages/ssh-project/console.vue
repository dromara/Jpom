<!--

    Copyright (c) 2019 Of Him Code Technology Studio
    Jpom is licensed under Mulan PSL v2.
    You can use this software according to the terms and conditions of the Mulan PSL v2.
    You may obtain a copy of Mulan PSL v2 at:
    			http://license.coscl.org.cn/MulanPSL2
    THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
    See the Mulan PSL v2 for more details.

-->
<template>
  <div>
    <a-card size="small">
      <template #title>
        <a-space wrap>
          <span>{{ project.name || $t('i18n_8215da30b4') }}</span>
          <a-tag :color="isRunning ? 'green' : 'default'">
            {{ isRunning ? $t('i18n_d679aea3aa') : $t('i18n_607ce7db25') }}
          </a-tag>
          <a-tag v-if="statusInfo.pid">PID: {{ statusInfo.pid }}</a-tag>
          <a-tag v-if="statusInfo.port">{{ $t('i18n_c76cfefe72') }}: {{ statusInfo.port }}</a-tag>
        </a-space>
      </template>
      <template #extra>
        <a-space wrap>
          <a-button size="small" @click="refreshStatus">{{ $t('i18n_4c2dbd26ee') }}</a-button>
          <a-tooltip :title="isRunning ? $t('i18n_55ec2b5767') : ''">
            <span>
              <a-button
                size="small"
                type="primary"
                :disabled="isRunning"
                :loading="optLoading === 'start'"
                @click="operate('start')"
                >{{ $t('i18n_8e54ddfe24') }}</a-button
              >
            </span>
          </a-tooltip>
          <a-button
            size="small"
            type="primary"
            danger
            :loading="optLoading === 'restart'"
            @click="operate('restart')"
            >{{ $t('i18n_01b4e06f39') }}</a-button
          >
          <a-tooltip :title="!isRunning ? $t('i18n_cccefb2c86') : ''">
            <span>
              <a-button
                size="small"
                type="primary"
                danger
                :disabled="!isRunning"
                :loading="optLoading === 'stop'"
                @click="operate('stop')"
                >{{ $t('i18n_095e938e2a') }}</a-button
              >
            </span>
          </a-tooltip>
        </a-space>
      </template>
      <a-alert v-if="!connected" type="warning" show-icon :message="$t('i18n_a54ff964ad')">
        <template #action>
          <a-button size="small" type="primary" :loading="connecting" @click="reconnect">{{
            $t('i18n_d6d47a06dd')
          }}</a-button>
        </template>
      </a-alert>
      <a-space wrap class="log-toolbar">
        <a-select
          v-model:value="currentLogKey"
          style="width: 180px"
          :options="logOptions"
          :placeholder="$t('i18n_d1038cf024')"
          @change="switchLog"
        />
        <a-button :loading="downloadLoading" @click="downloadLog('console')">{{ $t('i18n_60831c22d2') }}</a-button>
        <a-button :loading="backupLoading" @click="backupLog">{{ $t('i18n_b49d5a9125') }}</a-button>
        <a-button danger :loading="clearLoading" @click="clearLog">{{ $t('i18n_d01f3a5080') }}</a-button>
        <a-select
          v-model:value="selectedBackup"
          allow-clear
          style="width: 260px"
          :loading="backupListLoading"
          :options="backupOptions"
          :placeholder="$t('i18n_15f01c43e8')"
          @change="handleBackupSelect"
        />
      </a-space>
      <div ref="logBody" class="ssh-project-log">
        <a-empty v-if="!lines.length" :description="$t('i18n_cf69c2f4c2')" style="margin-top: 60px" />
        <template v-else>
          <div v-for="line in lines" :key="line.id">{{ line.text }}</div>
        </template>
      </div>
    </a-card>
  </div>
</template>
<script>
import {
  backupSshProjectLog,
  downloadSshProjectLog,
  getSshProjectItem,
  getSshProjectLogBackupList,
  getSshProjectLogList,
  resetSshProjectLog
} from '@/api/ssh-project'
import { getWebSocketUrl } from '@/api/config'
import { mapState } from 'pinia'
import { useAppStore } from '@/stores/app'
// 日志区最多保留的行数
const MAX_LOG_LINE = 3000
export default {
  data() {
    return {
      project: {},
      statusInfo: {
        status: '',
        pid: '',
        port: '',
        message: ''
      },
      logList: [],
      currentLogKey: 'console',
      backups: [],
      selectedBackup: undefined,
      backupListLoading: false,
      backupLoading: false,
      clearLoading: false,
      downloadLoading: false,
      lines: [],
      lineId: 0,
      socket: null,
      heartTimer: null,
      connected: false,
      connecting: false,
      optLoading: ''
    }
  },
  computed: {
    ...mapState(useAppStore, ['getWorkspaceId']),
    projectId() {
      return this.$route.query.id || ''
    },
    socketUrl() {
      return getWebSocketUrl(
        '/socket/ssh-project',
        `id=${this.projectId}&type=sshProject&workspaceId=${this.getWorkspaceId()}`
      )
    },
    isRunning() {
      return typeof this.statusInfo.status === 'string' && this.statusInfo.status.toLowerCase() === 'running'
    },
    logOptions() {
      return this.logList.map((item) => ({
        label: item.label || item.name,
        value: item.name
      }))
    },
    backupOptions() {
      return this.backups.map((item) => ({
        label: item.name,
        value: item.name
      }))
    }
  },
  watch: {
    projectId() {
      this.loadProject()
      this.loadLogList()
      this.initWebSocket()
    }
  },
  mounted() {
    this.loadProject()
    this.loadLogList()
    this.loadBackups()
    this.initWebSocket()
    // 关闭页面前主动断开，避免服务端残留 tailer
    window.onbeforeunload = () => {
      this.destroySocket()
    }
  },
  beforeUnmount() {
    this.destroySocket()
  },
  methods: {
    // 加载项目信息
    loadProject() {
      if (!this.projectId) {
        return
      }
      getSshProjectItem(this.projectId).then((res) => {
        if (res.code === 200 && res.data) {
          this.project = res.data
          this.statusInfo = {
            status: res.data.lastStatus || '',
            pid: res.data.lastPid,
            port: this.normalizePort(res.data.lastPort),
            message: res.data.lastStatusMsg || ''
          }
        }
      })
    },
    // 加载可查看的日志文件
    loadLogList() {
      if (!this.projectId) {
        return
      }
      getSshProjectLogList(this.projectId)
        .then((res) => {
          if (res.code === 200 && res.data) {
            this.logList = res.data
            if (!this.logList.some((item) => item.name === this.currentLogKey)) {
              this.currentLogKey = this.logList.length ? this.logList[0].name : 'console'
            }
          }
        })
        .catch(() => {
          $notification.warn({ message: this.$t('i18n_b6912d8a31') })
        })
    },
    // 建立 websocket 连接
    initWebSocket() {
      if (!this.projectId) {
        return
      }
      this.destroySocket()
      this.connecting = true
      let socket
      try {
        socket = new WebSocket(this.socketUrl)
      } catch (e) {
        this.connecting = false
        $notification.error({ message: this.$t('i18n_7f7bddcda5'), description: String(e) })
        return
      }
      this.socket = socket
      socket.onopen = () => {
        this.connected = true
        this.connecting = false
        // 连接建立后先要一次状态和当前日志
        this.sendMsg('status')
        this.sendMsg('showlog', { logKey: this.currentLogKey })
        this.startHeart()
      }
      socket.onerror = () => {
        this.connected = false
        this.connecting = false
        this.optLoading = ''
        this.stopHeart()
        $notification.error({ message: this.$t('i18n_7f7bddcda5') })
      }
      socket.onclose = () => {
        this.connected = false
        this.connecting = false
        this.optLoading = ''
        this.stopHeart()
      }
      socket.onmessage = (msg) => {
        this.handleMessage(msg.data)
      }
    },
    // 主动断开
    destroySocket() {
      this.stopHeart()
      if (this.socket) {
        this.socket.onopen = null
        this.socket.onerror = null
        this.socket.onclose = null
        this.socket.onmessage = null
        this.socket.close()
        this.socket = null
      }
      this.connected = false
      this.connecting = false
    },
    // 重新连接
    reconnect() {
      this.initWebSocket()
    },
    startHeart() {
      this.stopHeart()
      this.heartTimer = setInterval(() => {
        this.sendMsg('heart')
      }, 5000)
    },
    stopHeart() {
      if (this.heartTimer) {
        clearInterval(this.heartTimer)
        this.heartTimer = null
      }
    },
    // 发送消息，返回是否发送成功
    sendMsg(op, extra) {
      if (!this.socket || this.socket.readyState !== WebSocket.OPEN) {
        return false
      }
      const data = { op: op, projectId: this.projectId }
      if (extra && extra.logKey) {
        data.logKey = extra.logKey
      }
      this.socket.send(JSON.stringify(data))
      return true
    },
    // 处理服务端消息
    handleMessage(raw) {
      let res
      try {
        res = JSON.parse(raw)
      } catch (e) {
        this.appendLine(raw)
        return
      }
      if (!res || !res.op) {
        this.appendLine(raw)
        return
      }
      if (res.op === 'heart') {
        this.connected = true
        return
      }
      if (typeof res.success === 'boolean') {
        // 操作/状态回执
        this.optLoading = ''
        this.applyStatus(res)
        if (res.op !== 'status' && res.msg) {
          this.appendLine(res.msg)
        }
        if (res.op !== 'status' && !res.success && res.msg) {
          $notification.warn({ message: res.msg })
        }
        return
      }
      // 流式输出：每条日志一行
      if (res.msg) {
        this.appendLine(res.msg)
      }
    },
    // 后端无端口时返回 '-'，这里归一化为空，避免展示无意义的横线
    normalizePort(port) {
      if (!port || port === '-') {
        return ''
      }
      return port
    },
    // 回执里带状态时同步徽标
    applyStatus(res) {
      if (!res.status && !res.pid && !res.port) {
        return
      }
      this.statusInfo = {
        status: res.status || this.statusInfo.status,
        pid: res.pid,
        port: this.normalizePort(res.port),
        message: res.statusMsg || this.statusInfo.message
      }
    },
    // 追加日志（最多保留 3000 行，自动滚动到底）
    appendLine(line) {
      if (line === undefined || line === null || line === '') {
        return
      }
      this.lineId += 1
      this.lines.push({ id: this.lineId, text: String(line) })
      if (this.lines.length > MAX_LOG_LINE) {
        this.lines.splice(0, this.lines.length - MAX_LOG_LINE)
      }
      this.$nextTick(() => {
        const element = this.$refs.logBody
        if (element) {
          element.scrollTop = element.scrollHeight
        }
      })
    },
    // 加载日志备份列表
    loadBackups() {
      if (!this.projectId) {
        return
      }
      this.backupListLoading = true
      getSshProjectLogBackupList(this.projectId)
        .then((res) => {
          if (res.code === 200 && res.data) {
            this.backups = res.data
          }
        })
        .catch(() => {
          $notification.warn({ message: this.$t('i18n_cf54ec1e22') })
        })
        .finally(() => {
          this.backupListLoading = false
        })
    },
    // 备份当前控制台日志
    backupLog() {
      this.backupLoading = true
      backupSshProjectLog(this.projectId)
        .then((res) => {
          if (res.code === 200) {
            $notification.success({
              message: this.$t('i18n_43b375e4b4'),
              description: res.data
            })
            this.loadBackups()
          }
        })
        .finally(() => {
          this.backupLoading = false
        })
    },
    // 清空当前控制台日志（二次确认）
    clearLog() {
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_8d1211d3e4'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => this.doClearLog()
      })
    },
    doClearLog() {
      this.clearLoading = true
      return resetSshProjectLog(this.projectId)
        .then((res) => {
          if (res.code === 200) {
            $notification.success({
              message: res.msg
            })
            // 服务端已清空，同步清空本地视图并重新 follow
            this.lines = []
            this.sendMsg('showlog', { logKey: this.currentLogKey })
          }
        })
        .finally(() => {
          this.clearLoading = false
        })
    },
    // 选中备份文件后直接下载
    handleBackupSelect(name) {
      if (!name) {
        return
      }
      return this.downloadLog(name)
    },
    // 下载日志（console 表示当前日志，其他为备份文件名）
    downloadLog(name) {
      const fileName = name === 'console' ? `${this.project.name || this.projectId}.log` : name
      this.downloadLoading = true
      return downloadSshProjectLog({ id: this.projectId, name })
        .then(async (blob) => {
          if (!(blob instanceof Blob)) {
            $notification.error({ message: this.$t('i18n_95c7b741ef') })
            return
          }
          // 后端出错时会返回 JSON（而不是文件流），这里识别并提示原因
          if (blob.type && blob.type.indexOf('json') > -1) {
            const text = await blob.text()
            let description = text
            try {
              const json = JSON.parse(text)
              description = json.msg || text
            } catch (e) {
              // 非标准 JSON 时保留原文
            }
            $notification.error({ message: this.$t('i18n_95c7b741ef'), description: description })
            return
          }
          const url = window.URL.createObjectURL(blob)
          const link = document.createElement('a')
          link.href = url
          link.download = fileName
          document.body.appendChild(link)
          link.click()
          document.body.removeChild(link)
          window.URL.revokeObjectURL(url)
        })
        .catch(() => {
          $notification.error({ message: this.$t('i18n_95c7b741ef') })
        })
        .finally(() => {
          this.downloadLoading = false
          this.selectedBackup = undefined
        })
    },
    // 切换日志文件
    switchLog() {
      this.lines = []
      this.sendMsg('showlog', { logKey: this.currentLogKey })
    },
    // 刷新状态
    refreshStatus() {
      if (!this.sendMsg('status')) {
        $notification.warn({ message: this.$t('i18n_6a8bc730ef') })
        this.reconnect()
      }
    },
    // 启动/重启/停止（走 websocket，输出流式打印到日志区）
    operate(opt) {
      if (opt === 'stop') {
        $confirm({
          title: this.$t('i18n_c4535759ee'),
          zIndex: 1009,
          content: this.$t('i18n_010865ca50'),
          okText: this.$t('i18n_e83a256e4f'),
          cancelText: this.$t('i18n_625fb26b4b'),
          onOk: () => this.sendOperate(opt)
        })
        return
      }
      if (opt === 'restart') {
        $confirm({
          title: this.$t('i18n_c4535759ee'),
          zIndex: 1009,
          content: this.$t('i18n_989f1f2b61'),
          okText: this.$t('i18n_e83a256e4f'),
          cancelText: this.$t('i18n_625fb26b4b'),
          onOk: () => this.sendOperate(opt)
        })
        return
      }
      this.sendOperate(opt)
    },
    sendOperate(opt) {
      if (!this.sendMsg(opt)) {
        $notification.warn({ message: this.$t('i18n_7f7bddcda5') })
        return
      }
      this.optLoading = opt
    }
  }
}
</script>
<style scoped>
.log-toolbar {
  margin-top: 8px;
}
.ssh-project-log {
  height: calc(100vh - 300px);
  min-height: 320px;
  margin-top: 8px;
  padding: 10px;
  overflow: auto;
  background: #292a2b;
  color: #ffb86c;
  border-radius: 8px;
  font-family:
    Operator Mono,
    Source Code Pro,
    Menlo,
    Monaco,
    Consolas,
    Courier New,
    monospace;
  font-size: 12px;
  line-height: 20px;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
