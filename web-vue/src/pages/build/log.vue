<template>
  <log-view
    :ref="`logView`"
    :title-name="$t('i18n_7c0ee78130')"
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
          {{ $t('i18n_e703c7367c') }}
          <a-tooltip
            :title="`${$t('i18n_e703c7367c')} ${statusMap[status]} ${
              statusMsg ? $t('i18n_8d13037eb7') + statusMsg : ''
            }`"
          >
            <a-tag :color="statusColor[status]" style="margin-right: 0">
              {{ statusMap[status] || $t('i18n_903b25f64e') }}
            </a-tag>
          </a-tooltip>
        </span>
        <span>
          {{ $t('i18n_31aaaaa6ec') }}
          <a-tag>{{ temp && temp.buildId }}</a-tag>
        </span>
        <a-button type="primary" :disabled="!logId" size="small" @click="handleDownload">
          <DownloadOutlined />
          {{ $t('i18n_f26ef91424') }}
        </a-button>
        <a-button type="primary" :disabled="!logId" size="small" @click="() => (environmentVisible = true)">
          <UnorderedListOutlined />{{ $t('i18n_3867e350eb') }}
        </a-button>
        |
      </a-space>
    </template>
  </log-view>

  <CustomModal
    v-if="environmentVisible"
    v-model:open="environmentVisible"
    destroy-on-close
    :title="$t('i18n_aacd9caa4a')"
    :footer="null"
    :mask-closable="false"
    width="50%"
  >
    <a-list size="small" bordered :data-source="Object.keys(environment)">
      <template #renderItem="{ item }">
        <a-list-item style="display: block">
          <a-row :wrap="true">
            <a-col :span="12" :flex="12" class="text-overflow-hidden">
              <a-tooltip placement="topLeft" :title="item">
                {{ item }}
              </a-tooltip>
            </a-col>

            <a-col :span="12" :flex="12" class="text-overflow-hidden">
              <a-tooltip
                placement="topLeft"
                :title="environment[item].privacy ? $t('i18n_b12d003367') : environment[item].value"
              >
                <EyeInvisibleOutlined v-if="environment[item].privacy" />{{ environment[item].value }}
              </a-tooltip>
            </a-col>
          </a-row>
        </a-list-item>
      </template>
      <template #header>
        <b>{{ $t('i18n_c0ad27a701') }}</b>
      </template>
      <!-- <template #footer>
        <div>Footer</div>
      </template> -->
    </a-list>
  </CustomModal>
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
      statusMsg: '',
      environmentVisible: false,
      environment: {}
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
          this.environment = res.data.environment || {}
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
