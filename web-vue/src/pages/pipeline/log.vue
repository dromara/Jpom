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
    v-if="open"
    destroy-on-close
    placement="right"
    width="76vw"
    :open="open"
    @close="handleClose"
  >
    <template #title>
      <a-space>
        <span>{{ $t('i18n_pipeline_run_log') }}</span>
        <a-tag v-if="run">#{{ run.runNumber }}</a-tag>
      </a-space>
    </template>
    <template #extra>
      <a-space>
        <a-tag v-if="status !== null" :color="pipelineStatusColor[status]">
          {{ pipelineStatusMap[status] || $t('i18n_pipeline_status_unknown') }}
        </a-tag>
        <a-tooltip :title="statusMsg">
          <span v-if="statusMsg" class="log-status-msg">{{ statusMsg }}</span>
        </a-tooltip>
        <a-button v-if="logId" type="primary" size="small" @click="handleDownload">
          <DownloadOutlined />
          {{ $t('i18n_pipeline_log_download') }}
        </a-button>
      </a-space>
    </template>
    <viewPre ref="logView" :height="'calc(92vh - 120px)'" :config="config" />
  </CustomDrawer>
</template>

<script>
import viewPre from '@/components/logView/view-pre'
import { message } from 'ant-design-vue'
import { getPipelineRunLog, downloadPipelineRunLog, pipelineStatusMap, pipelineStatusColor } from '@/api/pipeline'

export default {
  name: 'PipelineLog',
  components: { viewPre },
  props: {
    open: { type: Boolean, default: false },
    run: { type: Object, default: () => null }
  },
  emits: ['close'],
  data() {
    return {
      pipelineStatusMap,
      pipelineStatusColor,
      config: { logScroll: true, wordBreak: false },
      timer: null,
      line: 1,
      logId: '',
      status: null,
      statusMsg: '',
      polling: false
    }
  },
  watch: {
    open(value) {
      if (value) {
        this.start()
      } else {
        this.stop()
      }
    }
  },
  mounted() {
    if (this.open) {
      this.start()
    }
  },
  beforeUnmount() {
    this.stop()
  },
  methods: {
    start() {
      this.line = 1
      this.logId = ''
      this.status = null
      this.statusMsg = ''
      this.polling = true
      this.$nextTick(() => this.pull())
    },
    stop() {
      this.polling = false
      if (this.timer) {
        clearTimeout(this.timer)
        this.timer = null
      }
    },
    handleClose() {
      this.stop()
      this.$emit('close')
    },
    nextPull(delay) {
      if (!this.polling) return
      if (this.timer) clearTimeout(this.timer)
      const wait = delay === undefined ? 2000 : delay
      this.timer = setTimeout(() => this.pull(), wait)
    },
    pull() {
      if (!this.run || !this.run.id) return
      getPipelineRunLog({ runId: this.run.id, line: this.line })
        .then((res) => {
          if (res.code === 200) {
            const data = res.data || {}
            this.$refs.logView && this.$refs.logView.appendLine(data.dataLines)
            this.line = data.line || this.line
            this.status = data.status
            this.statusMsg = data.statusMsg || ''
            this.logId = this.run.id
            if (data.hasMore === true) {
              // 日志超过一页（1000 行）：立即拉下一页，保证能看到完整日志
              this.nextPull(0)
              return
            }
            if (data.run !== true) {
              this.polling = false
              return
            }
          } else if (res.code === 201) {
            // 已开始但还没有日志内容：继续轮询
          } else {
            this.polling = false
            if (res.msg) {
              message.warning(res.msg)
              this.$refs.logView && this.$refs.logView.appendLine([res.msg])
            }
            return
          }
          this.nextPull()
        })
        .catch((e) => {
          this.polling = false
          console.warn('[pipeline-log] 日志拉取失败', e)
        })
    },
    handleDownload() {
      window.open(downloadPipelineRunLog(this.run.id), '_blank')
    }
  }
}
</script>

<style scoped>
.log-status-msg {
  max-width: 320px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #8f959e;
  font-size: 12px;
}
</style>
