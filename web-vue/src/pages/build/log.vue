<template>
  <log-view
    :ref="`logView`"
    :title-name="$t('pages.build.log.fc20bf28')"
    :visible="visible"
    @close="
      () => {
        $emit('close')
      }
    "
  >
    <template #before>
      <a-space>
        <span v-if="status">
          {{ $t('pages.build.log.df91bc15') }}
          <a-tooltip
            :title="`${$t('pages.build.log.df91bc15')} ${statusMap[status]} ${
              statusMsg ? $t('pages.build.log.7f491810') + statusMsg : ''
            }`"
          >
            <a-tag :color="statusColor[status]" style="margin-right: 0">
              {{ statusMap[status] || $t('pages.build.log.3ebb81e0') }}
            </a-tag>
          </a-tooltip>
        </span>
        <span>
          {{ $t('pages.build.log.7cf809d7') }}
          <a-tag>{{ temp && temp.buildId }}</a-tag>
        </span>
        <a-button type="primary" :disabled="!logId" size="small" @click="handleDownload"
          ><DownloadOutlined />
          {{ $t('pages.build.log.42c8e9c6') }}
        </a-button>
        |
      </a-space>
    </template>
  </log-view>
</template>

<script>
import LogView from '@/components/logView'

import { loadBuildLog, downloadBuildLog, statusColor, statusMap } from '@/api/build-info'
export default {
  components: {
    LogView
  },
  props: {
    temp: {
      /**
       * {
       * id:'',
       * buildId:''
       * }
       */
      type: Object,
      default: () => {}
    },
    visible: {
      type: Boolean,
      default: false
    }
  },
  emits: ['close'],
  data() {
    return {
      statusMap,
      statusColor,
      logTimer: null,
      // logText: "loading...",
      line: 1,
      logId: '',
      status: null,
      statusMsg: ''
    }
  },
  beforeUnmount() {
    this.logTimer && clearTimeout(this.logTimer)
  },
  created() {},
  mounted() {
    this.pullLog()
  },
  methods: {
    nextPull() {
      if (this.logTimer) {
        clearTimeout(this.logTimer)
      }
      // 加载构建日志
      this.logTimer = setTimeout(() => {
        this.pullLog()
      }, 2000)
    },
    // 加载日志内容
    pullLog() {
      const params = {
        id: this.temp.id,
        buildId: this.temp.buildId,
        line: this.line
      }
      loadBuildLog(params).then((res) => {
        let next = true
        if (res.code === 200) {
          // 停止请求
          if (res.data.run === false) {
            clearInterval(this.logTimer)
            next = false
          }
          this.$refs.logView.appendLine(res.data.dataLines)
          this.line = res.data.line
          this.logId = res.data.logId
          this.status = res.data.status
          this.statusMsg = res.data.statusMsg
        } else if (res.code !== 201) {
          // 201 是当前构建且没有日志
          $notification.warn({
            message: res.msg
          })
          clearInterval(this.logTimer)
          next = false
          this.$refs.logView.appendLine(res.msg)
        }
        // 继续拉取日志
        if (next) this.nextPull()
      })
    },
    // 下载构建日志
    handleDownload() {
      window.open(downloadBuildLog(this.logId), '_blank')
    }
  }
}
</script>
