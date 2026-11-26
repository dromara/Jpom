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
  <CustomDrawer
    :open="open"
    destroy-on-close
    placement="right"
    width="90vw"
    :title="$t('i18n_4caaaf4705')"
    :body-style="{ padding: '12px', height: 'calc(100vh - 110px)', overflow: 'auto' }"
    @close="handleClose"
  >
    <div class="session-replay">
      <!-- 工具条：会话元信息、倍速、下载 -->
      <div class="session-replay__toolbar">
        <a-space wrap size="middle">
          <span>
            <strong>{{ $t('i18n_a5dd0b6b79') }}</strong>
            <span class="session-replay__meta">{{ sessionInfo.machineSshName || '-' }}</span>
          </span>
          <span>
            <strong>{{ $t('i18n_1fd02a90c3') }}</strong>
            <span class="session-replay__meta">{{ sessionInfo.userId || '-' }}</span>
          </span>
          <span>
            <strong>{{ $t('i18n_592c595891') }}</strong>
            <span class="session-replay__meta">{{ parseTime(sessionInfo.startTime) }}</span>
          </span>
          <span>
            <strong>{{ $t('i18n_5bdfd7ee4d') }}</strong>
            <span class="session-replay__meta">{{ formatDuration(sessionInfo.duration) }}</span>
          </span>
          <span>
            <strong>{{ $t('i18n_58f9666705') }}</strong>
            <span class="session-replay__meta">{{ renderSize(sessionInfo.bytes) }}</span>
          </span>
          <a-tag v-if="sessionInfo.status !== undefined && sessionInfo.status !== null" :color="statusColor">
            {{ statusMap[sessionInfo.status] || '-' }}
          </a-tag>
        </a-space>
      </div>
      <div class="session-replay__toolbar">
        <a-space wrap size="middle">
          <span>{{ $t('i18n_d37d357dee') }}</span>
          <a-radio-group v-model:value="speed" size="small" button-style="solid" @change="handleSpeedChange">
            <a-radio-button v-for="item in speedList" :key="item" :value="item">{{ item }}x</a-radio-button>
          </a-radio-group>
          <a-button size="small" type="primary" @click="handleDownload">
            <DownloadOutlined />{{ $t('i18n_f26ef91424') }}
          </a-button>
        </a-space>
      </div>
      <a-alert v-if="errorMessage" type="warning" show-icon :message="errorMessage" class="session-replay__alert" />
      <!-- 左侧播放器，右侧命令索引 -->
      <div class="session-replay__body">
        <div class="session-replay__main">
          <a-spin :spinning="playerLoading">
            <div ref="playerBox" class="session-replay__player"></div>
          </a-spin>
        </div>
        <div class="session-replay__side">
          <div class="session-replay__side-title">{{ $t('i18n_bbef4170b1') }}</div>
          <a-spin :spinning="commandsLoading">
            <div ref="commandListEl" class="session-replay__command-list">
              <a-empty v-if="!commands || commands.length <= 0" :description="$t('i18n_38a7355b1f')" />
              <div
                v-for="(item, index) in commands"
                :key="item.seq || index"
                class="session-replay__command"
                :class="{ 'is-active': activeIndex === index }"
                @click="handleCommandClick(index)"
              >
                <div class="session-replay__command-head">
                  <a-tag v-if="activeIndex === index" color="blue">{{ index + 1 }}</a-tag>
                  <span v-else class="session-replay__command-no">{{ index + 1 }}</span>
                  <span class="session-replay__command-time">{{ formatSeconds(item.time) }}</span>
                </div>
                <div class="session-replay__command-text">{{ item.command }}</div>
              </div>
            </div>
          </a-spin>
        </div>
      </div>
    </div>
  </CustomDrawer>
</template>
<script>
import {
  getSshTerminalSessionCastUrl,
  getSshTerminalSessionCommands,
  sshTerminalSessionStatusColorMap,
  sshTerminalSessionStatusMap
} from '@/api/ssh-terminal-session'
import { parseTime, renderSize } from '@/utils/const'

// 播放器模块缓存：只在客户端动态加载，避免 asciinema-player 被打进主包
let playerModulePromise = null
function loadAsciinemaPlayer() {
  if (!playerModulePromise) {
    playerModulePromise = Promise.all([
      import('asciinema-player'),
      // 样式随播放器一起按需加载，避免污染主包样式
      import('asciinema-player/dist/bundle/asciinema-player.css').catch(() => null)
    ])
      .then(([playerModule]) => {
        // 3.x 导出：优先 default，其次命名导出 create
        const playerLib = playerModule && (playerModule.default || playerModule)
        if (playerLib && typeof playerLib.create === 'function') {
          return playerLib
        }
        return null
      })
      .catch((e) => {
        // 加载失败后允许下次重试
        playerModulePromise = null
        throw e
      })
  }
  return playerModulePromise
}

export default {
  props: {
    // 抽屉是否打开
    open: {
      type: Boolean,
      default: false
    },
    // 会话对象（列表行数据）
    session: {
      type: Object,
      default: () => ({})
    },
    // 仅传会话 id 时也可使用
    sessionId: {
      type: String,
      default: ''
    }
  },
  emits: ['update:open', 'close'],
  data() {
    return {
      commands: [],
      commandsLoading: false,
      playerLoading: false,
      errorMessage: '',
      // 当前高亮的命令下标
      activeIndex: -1,
      // 倍速
      speed: 1,
      speedList: [1, 2, 4, 8],
      isPlaying: false,
      statusMap: sshTerminalSessionStatusMap
    }
  },
  computed: {
    sessionInfo() {
      return this.session || {}
    },
    currentSessionId() {
      return this.sessionInfo.id || this.sessionId
    },
    statusColor() {
      return sshTerminalSessionStatusColorMap[this.sessionInfo.status] || 'default'
    },
    // 播放器可选参数
    playerOptions() {
      const options = {
        cols: this.sessionInfo.cols,
        rows: this.sessionInfo.rows,
        preload: true,
        idleTimeLimit: 2,
        poster: 'npt:0:00',
        theme: 'asciinema',
        speed: this.speed
      }
      const markers = this.buildMarkers()
      if (markers.length > 0) {
        options.markers = markers
      }
      return options
    }
  },
  watch: {
    open(value) {
      if (value) {
        this.init()
      } else {
        this.disposePlayer()
      }
    }
  },
  created() {
    // 播放器实例与 marker 状态不放进 data，避免被响应式代理
    this.playerInstance = null
    this.hasMarkers = false
  },
  mounted() {
    if (this.open) {
      this.init()
    }
  },
  beforeUnmount() {
    this.disposePlayer()
  },
  methods: {
    // 初始化：先取命令索引（marker 需要），再创建播放器
    async init() {
      this.activeIndex = -1
      this.errorMessage = ''
      this.speed = 1
      await this.loadCommands()
      await this.$nextTick()
      await this.initPlayer()
    },
    // 命令索引（用于跳转与高亮）
    async loadCommands() {
      const id = this.currentSessionId
      if (!id) {
        this.commands = []
        return
      }
      this.commandsLoading = true
      try {
        const res = await getSshTerminalSessionCommands(id)
        this.commands = res.code === 200 && Array.isArray(res.data) ? res.data : []
      } catch (e) {
        this.commands = []
      } finally {
        this.commandsLoading = false
      }
    },
    // 构造播放器 marker，下标与命令列表一致，便于 seek({ marker: index })
    buildMarkers() {
      return (this.commands || []).map((item) => {
        // asciicast v2 的时间轴单位为秒
        const time = Number(item.time)
        return [isNaN(time) ? 0 : time, item.command || '']
      })
    },
    // 抽屉内容是按需挂载的，等待播放器容器出现（最多 2 秒）
    async waitForPlayerBox(timeout = 2000) {
      const startTime = Date.now()
      while (Date.now() - startTime < timeout) {
        if (this.$refs.playerBox) {
          return this.$refs.playerBox
        }
        await new Promise((resolve) => setTimeout(resolve, 50))
      }
      return this.$refs.playerBox || null
    },
    // 创建播放器
    async initPlayer() {
      const box = await this.waitForPlayerBox()
      if (!box || !this.currentSessionId) {
        return
      }
      this.playerLoading = true
      try {
        const playerLib = await loadAsciinemaPlayer()
        if (!playerLib) {
          this.errorMessage = this.$t('i18n_3f331434b2')
          return
        }
        box.innerHTML = ''
        this.createPlayer(playerLib, box, 0, false)
      } catch (e) {
        this.errorMessage = this.$t('i18n_3f331434b2')
      } finally {
        this.playerLoading = false
      }
    },
    // 真正的创建动作（切换倍速时复用）
    createPlayer(playerLib, box, startTime, autoPlay) {
      this.disposePlayer()
      const player = playerLib.create(
        { url: getSshTerminalSessionCastUrl(this.currentSessionId) },
        box,
        this.playerOptions
      )
      this.playerInstance = player
      this.hasMarkers = !!(this.playerOptions.markers && this.playerOptions.markers.length > 0)
      player.addEventListener('marker', (event) => {
        this.handleMarker(event)
      })
      player.addEventListener('play', () => {
        this.isPlaying = true
      })
      player.addEventListener('pause', () => {
        this.isPlaying = false
      })
      player.addEventListener('ended', () => {
        this.isPlaying = false
      })
      if (startTime > 0) {
        player.seek(startTime)
      }
      if (autoPlay) {
        player.play()
      }
    },
    // 播放进度越过命令索引时回写高亮
    handleMarker(event) {
      if (!event || typeof event.index !== 'number') {
        return
      }
      this.activeIndex = event.index
      this.$nextTick(() => {
        const listEl = this.$refs.commandListEl
        if (!listEl) {
          return
        }
        const items = listEl.querySelectorAll('.session-replay__command')
        const target = items[event.index]
        if (target && target.scrollIntoView) {
          target.scrollIntoView({ block: 'nearest' })
        }
      })
    },
    // 点击命令跳转到对应时刻
    handleCommandClick(index) {
      this.activeIndex = index
      if (!this.playerInstance || typeof this.playerInstance.seek !== 'function') {
        return
      }
      if (this.hasMarkers) {
        // 有 marker 时按下标跳转，与命令列表一一对应
        this.playerInstance.seek({ marker: index })
        return
      }
      // 没有 marker（命令索引缺失）时按时间跳转兜底
      const command = this.commands[index]
      const time = command ? Number(command.time) : 0
      this.playerInstance.seek(isNaN(time) ? 0 : Math.max(0, time))
    },
    // 切换倍速：3.x 没有 setSpeed，按新倍速重建并 seek 回原位置
    async handleSpeedChange() {
      const player = this.playerInstance
      const currentTime = player && typeof player.getCurrentTime === 'function' ? player.getCurrentTime() : 0
      const autoPlay = this.isPlaying
      this.disposePlayer()
      await this.$nextTick()
      try {
        const playerLib = await loadAsciinemaPlayer()
        const box = await this.waitForPlayerBox()
        if (!playerLib || !box) {
          this.errorMessage = this.$t('i18n_3f331434b2')
          return
        }
        box.innerHTML = ''
        this.createPlayer(playerLib, box, currentTime || 0, autoPlay)
      } catch (e) {
        this.errorMessage = this.$t('i18n_3f331434b2')
      }
    },
    // 释放播放器
    disposePlayer() {
      if (this.playerInstance) {
        try {
          this.playerInstance.dispose()
        } catch (e) {
          // 释放异常不影响关闭
        }
        this.playerInstance = null
      }
      this.isPlaying = false
    },
    // 下载 .cast 录制文件
    handleDownload() {
      if (!this.currentSessionId) {
        return
      }
      window.open(getSshTerminalSessionCastUrl(this.currentSessionId), '_blank')
    },
    // 关闭抽屉
    handleClose() {
      this.disposePlayer()
      this.$emit('update:open', false)
      this.$emit('close')
    },
    // 秒数格式化为 mm:ss，超过 1 小时显示 hh:mm:ss
    formatSeconds(seconds) {
      const total = Math.max(0, Math.floor(Number(seconds) || 0))
      const hour = Math.floor(total / 3600)
      const minute = Math.floor((total % 3600) / 60)
      const second = total % 60
      const pad = (value) => (value < 10 ? `0${value}` : `${value}`)
      return hour > 0 ? `${hour}:${pad(minute)}:${pad(second)}` : `${pad(minute)}:${pad(second)}`
    },
    // 毫秒时长格式化
    formatDuration(duration) {
      return this.formatSeconds(Math.floor((Number(duration) || 0) / 1000))
    },
    parseTime,
    renderSize
  }
}
</script>
<style scoped>
.session-replay {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.session-replay__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 0 8px 0;
  font-size: 13px;
}

.session-replay__meta {
  margin-left: 4px;
}

.session-replay__alert {
  margin-bottom: 8px;
}

.session-replay__body {
  display: flex;
  flex: 1;
  min-height: 0;
  gap: 8px;
}

.session-replay__main {
  flex: 1;
  min-width: 0;
  min-height: 0;
  /* 小视口时允许滚动，保证播放器控制条不会被裁掉 */
  overflow: auto;
}

.session-replay__player {
  height: calc(100vh - 340px);
  min-height: 200px;
  /* 播放器按宽度自适应后可能比容器高，露出内容交给外层滚动而不是裁切 */
  overflow: visible;
}

.session-replay__side {
  display: flex;
  flex-direction: column;
  width: 320px;
  flex: 0 0 320px;
  min-height: 0;
}

.session-replay__side-title {
  font-weight: 600;
  padding: 0 0 6px 0;
}

.session-replay__command-list {
  height: calc(100vh - 350px);
  min-height: 260px;
  overflow: auto;
  border-left: 1px solid var(--ant-color-border, transparent);
  padding-left: 8px;
}

.session-replay__command {
  cursor: pointer;
  padding: 4px 6px;
  border-radius: 4px;
}

.session-replay__command.is-active {
  font-weight: 600;
  background-color: var(--ant-color-fill-quaternary, transparent);
}

.session-replay__command-head {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
}

.session-replay__command-no {
  min-width: 18px;
  text-align: center;
}

.session-replay__command-time {
  opacity: 0.65;
}

.session-replay__command-text {
  font-family: monospace;
  word-break: break-all;
}
</style>
