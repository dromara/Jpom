<template>
  <div>
    <log-view
      titleName="任务日志"
      :ref="`logView`"
      :visible="visible"
      @close="
        () => {
          $emit('close')
        }
      "
    >
      <template v-slot:before>
        <a-space>
          <a-input-number v-model:value="tail" placeholder="读取行数" style="width: 150px">
            <template #addonBefore>
              <a-tooltip
                title="为避免显示内容太多而造成浏览器卡顿,读取日志最后多少行日志。修改后需要回车才能重新读取，小于 1 则读取所有"
                >行数：
              </a-tooltip>
            </template>
          </a-input-number>
          <div>
            时间戳：
            <a-switch v-model:checked="timestamps" checked-children="显示" un-checked-children="不显示" />
          </div>
          <a-button type="primary" size="small" @click="init"><ReloadOutlined /> 刷新 </a-button>
          |
          <a-button type="primary" :disabled="!this.logId" size="small" @click="download">
            <DownloadOutlined /> 下载
          </a-button>
          |
        </a-space>
      </template>
    </log-view>
  </div>
</template>

<script>
import LogView from '@/components/logView'
import {
  dockerSwarmServicesPullLog,
  dockerSwarmServicesStartLog,
  dockerSwarmServicesDownloaLog
} from '@/api/docker-swarm'
export default {
  components: {
    LogView
  },
  props: {
    dataId: {
      type: String
    },
    id: {
      type: String
    },
    type: {
      type: String
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
      logTimer: null,
      logId: '',
      line: 1,
      tail: 500,
      timestamps: false
    }
  },
  beforeUnmount() {
    this.logTimer && clearTimeout(this.logTimer)
  },
  mounted() {
    //
    this.init()
  },
  methods: {
    init() {
      this.logTimer && clearTimeout(this.logTimer)
      this.$refs.logView.clearLogCache()
      this.line = 1
      //
      dockerSwarmServicesStartLog(this.urlPrefix, {
        type: this.type,
        dataId: this.dataId,
        id: this.id,
        tail: this.tail,
        timestamps: this.timestamps
      }).then((res) => {
        if (res.code === 200) {
          this.logId = res.data
          this.pullLog()
        } else {
          this.$refs.logView.appendLine(res.msg)
        }
      })
    },
    nextPull() {
      this.logTimer && clearTimeout(this.logTimer)
      // 加载构建日志
      this.logTimer = setTimeout(() => {
        this.pullLog()
      }, 2000)
    },
    // 加载日志内容
    pullLog() {
      const params = {
        id: this.logId,
        line: this.line
      }
      dockerSwarmServicesPullLog(this.urlPrefix, params).then((res) => {
        let next = true
        if (res.code === 200) {
          // 停止请求
          const dataLines = res.data.dataLines

          this.$refs.logView.appendLine(dataLines)
          this.line = res.data.line
        }
        // 继续拉取日志
        if (next) this.nextPull()
      })
    },
    // 下载
    download() {
      window.open(dockerSwarmServicesDownloaLog(this.urlPrefix, this.logId), '_blank')
    }
  },
  emits: ['close']
}
</script>
