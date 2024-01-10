<template>
  <div>
    <a-space direction="vertical" style="width: 100%">
      <a-tag
        >控制台日志只是启动项目输出的日志信息,并非项目日志。可以关闭控制台日志备份功能：<b
          >jpom.project.log.auto-backup-to-file: false</b
        ></a-tag
      >

      <a-tag color="orange" v-if="project.logPath">
        控制台日志路径: {{ project.logPath }}
        <template v-if="project.logSize">
          当前日志文件大小：{{ project.logSize }}
          <a-button @click="handleDownload" type="link" size="small"> <DownloadOutlined />导出 </a-button>
        </template>
      </a-tag>

      <a-tag color="orange" v-if="project.logBackPath">控制台日志备份路径: {{ project.logBackPath }}</a-tag>

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
        <template #bodyCell="{ column, text, record, index }">
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
              <a-button type="primary" @click="handleDownloadLogback(record)">下载</a-button>
              <a-button type="primary" danger @click="handleDelete(record)">删除</a-button>
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
      type: String
    },
    projectId: {
      type: String
    }
  },
  data() {
    return {
      loading: true,
      project: {},
      logBackList: [],
      columns: [
        {
          title: '文件名称',
          dataIndex: 'filename',
          width: 150,
          ellipsis: true
        },
        {
          title: '修改时间',
          dataIndex: 'modifyTimeLong',
          width: 150,
          ellipsis: true
        },
        {
          title: '文件大小',
          dataIndex: 'fileSizeLong',
          width: 100,
          ellipsis: true
        },
        {
          title: '操作',
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
      const that = this
      this.$confirm({
        title: '系统提示',
        zIndex: 1009,
        content: '真的要删除文件么？',
        okText: '确认',
        cancelText: '取消',
        async onOk() {
          return await new Promise((resolve, reject) => {
            // 请求参数
            const params = {
              nodeId: that.nodeId,
              id: that.projectId,

              name: record.filename
            }
            // 删除
            deleteProjectLogBackFile(params)
              .then((res) => {
                if (res.code === 200) {
                  $notification.success({
                    message: res.msg
                  })
                  that.handleLogBack()
                }
                resolve()
              })
              .catch(reject)
          })
        }
      })
    }
  }
}
</script>
