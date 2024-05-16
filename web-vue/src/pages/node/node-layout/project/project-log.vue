<template>
  <div>
    <a-space direction="vertical" style="width: 100%">
      <a-tag>{{ $tl('p.consoleLogDescription') }}<b>jpom.project.log.auto-backup-to-file: false</b></a-tag>

      <a-tag v-if="project.logPath" color="orange">
        {{ $tl('p.consoleLogPath') }}: {{ project.logPath }}
        <template v-if="project.logSize">
          {{ $tl('p.currentLogFileSize') }}{{ project.logSize }}
          <a-button type="link" size="small" @click="handleDownload">
            <DownloadOutlined />{{ $tl('p.export') }}
          </a-button>
        </template>
      </a-tag>

      <a-tag v-if="project.logBackPath" color="orange"
        >{{ $tl('p.consoleLogBackupPath') }}{{ project.logBackPath }}</a-tag
      >

      <!-- 数据表格 -->
      <a-table
        :data-source="logBackList"
        :loading="loading"
        :columns="columns"
        :pagination="false"
        bordered
        :scroll="{
          x: 'max-content'
        }"
      >
        <template #bodyCell="{ column, text, record }">
          <template v-if="column.dataIndex === 'filename'">
            <a-tooltip placement="topLeft" :title="text">
              <span>{{ text }}</span>
            </a-tooltip>
          </template>
          <template v-else-if="column.dataIndex === 'fileSizeLong'">
            <a-tooltip placement="topLeft" :title="`${text ? renderSize(text) : item.fileSize}`">
              {{ text ? renderSize(text) : item.fileSize }}
            </a-tooltip>
          </template>
          <template v-else-if="column.dataIndex === 'modifyTimeLong'">
            <a-tooltip :title="`${parseTime(record.modifyTimeLong)}}`">
              <span>{{ parseTime(record.modifyTimeLong) }}</span>
            </a-tooltip>
          </template>
          <template v-else-if="column.dataIndex === 'operation'">
            <a-space>
              <a-button type="primary" @click="handleDownloadLogback(record)">{{ $tl('p.download') }}</a-button>
              <a-button type="primary" danger @click="handleDelete(record)">{{ $tl('p.delete') }}</a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-space>
  </div>
</template>

<script>
import {
  getLogBackList,
  deleteProjectLogBackFile,
  downloadProjectLogBackFile,
  getProjectLogSize,
  downloadProjectLogFile
} from '@/api/node-project'
import { renderSize, parseTime } from '@/utils/const'
export default {
  props: {
    nodeId: {
      type: String,
      default: ''
    },
    projectId: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      loading: true,
      project: {},
      logBackList: [],
      columns: [
        {
          title: this.$tl('p.fileName'),
          dataIndex: 'filename',
          width: 150,
          ellipsis: true
        },
        {
          title: this.$tl('p.modifyTime'),
          dataIndex: 'modifyTimeLong',
          width: 150,
          ellipsis: true
        },
        {
          title: this.$tl('p.fileSize'),
          dataIndex: 'fileSizeLong',
          width: 100,
          ellipsis: true
        },
        {
          title: this.$tl('p.operation'),
          dataIndex: 'operation',
          align: 'center',
          fixed: 'right',
          width: '130px'
        }
      ]
    }
  },
  mounted() {
    this.loadFileSize()
    this.loadData()
  },
  methods: {
    $tl(key, ...args) {
      return this.$t(`pages.node.nodeLayout.project.projectLog.${key}`, ...args)
    },
    renderSize,
    parseTime,
    // 加载日志文件大小
    loadFileSize() {
      const params = {
        nodeId: this.nodeId,
        id: this.projectId
      }
      getProjectLogSize(params).then((res) => {
        if (res.code === 200) {
          this.project = { ...this.project, logSize: res.data.logSize }
        }
      })
    },
    loadData() {
      this.loading = true
      const params = {
        nodeId: this.nodeId,
        id: this.projectId
      }
      getLogBackList(params).then((res) => {
        if (res.code === 200) {
          this.logBackList = res.data.array
          this.project = {
            ...this.project,
            logPath: res.data.logPath,
            logBackPath: res.data.logBackPath
          }
        }
        this.loading = false
      })
    },
    // 下载日志文件
    handleDownload() {
      // 请求参数
      const params = {
        nodeId: this.nodeId,
        id: this.projectId
      }
      // 请求接口拿到 blob
      window.open(downloadProjectLogFile(params), '_blank')
    },
    // 下载日志备份文件
    handleDownloadLogback(record) {
      // 请求参数
      const params = {
        nodeId: this.nodeId,
        id: this.projectId,

        key: record.filename
      }
      // 请求接口拿到 blob
      window.open(downloadProjectLogBackFile(params), '_blank')
    },
    // 删除日志备份文件
    handleDelete(record) {
      $confirm({
        title: this.$tl('p.systemPrompt'),
        zIndex: 1009,
        content: this.$tl('p.confirmDelete'),
        okText: this.$tl('p.confirm'),
        cancelText: this.$tl('p.cancel'),
        onOk: () => {
          return deleteProjectLogBackFile({
            nodeId: this.nodeId,
            id: this.projectId,

            name: record.filename
          }).then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              this.loadData()
            }
          })
        }
      })
    }
  }
}
</script>
