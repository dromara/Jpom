<template>
  <div>
    <a-descriptions bordered size="small">
      <template #title>
        <a-space>
          {{ data.name }}
          <a-tooltip title="点击刷新构建信息">
            <a-button type="link" size="small" @click="refresh"> <ReloadOutlined /></a-button>
          </a-tooltip>
        </a-space>
      </template>

      <a-descriptions-item label="分组">
        {{ data.group }}
      </a-descriptions-item>
      <a-descriptions-item label="分组/标签"> {{ data.branchName }} {{ data.branchTagName }} </a-descriptions-item>
      <a-descriptions-item label="构建方式">
        <template v-if="data.buildMode === 1">
          <CloudOutlined />
          容器构建
        </template>
        <template v-else>
          <CodeOutlined />
          本地构建
        </template>
      </a-descriptions-item>

      <a-descriptions-item label="最新构建ID">
        <span v-if="data.buildId <= 0"></span>
        <a-tag v-else color="#108ee9">#{{ data.buildId }}</a-tag>
      </a-descriptions-item>
      <a-descriptions-item label="构建状态">
        <a-tooltip :title="data.statusMsg">
          <a-tag :color="statusColor[data.status]">
            {{ statusMap[data.status] || '未知' }}

            <InfoCircleOutlined v-if="data.statusMsg" />
          </a-tag>
        </a-tooltip>
      </a-descriptions-item>
      <a-descriptions-item label="发布方式">{{ releaseMethodMap[data.releaseMethod] }} </a-descriptions-item>
      <a-descriptions-item label="定时构建">
        {{ data.autoBuildCron }}
      </a-descriptions-item>
      <a-descriptions-item label="别名码">
        {{ data.aliasCode }}
      </a-descriptions-item>
      <a-descriptions-item label="构建目录">
        <a-tag>{{ data.sourceDirExist ? '存在' : '不存在' }}</a-tag>
      </a-descriptions-item>
      <a-descriptions-item label="创建时间">
        {{ parseTime(data.createTimeMillis) }}
      </a-descriptions-item>
      <a-descriptions-item label="最后修改时间"> {{ parseTime(data.modifyTimeMillis) }}</a-descriptions-item>
      <a-descriptions-item label="最后修改人">{{ data.modifyUser }}</a-descriptions-item>
      <a-descriptions-item label="产物" :span="3">
        {{ data.resultDirFile }}
      </a-descriptions-item>
      <a-descriptions-item label="源仓库" :span="3" v-if="tempRepository">{{
        `${tempRepository ? tempRepository.name + '[' + tempRepository.gitUrl + ']' : ''}`
      }}</a-descriptions-item>
      <a-descriptions-item label="仓库lastcommit" :span="3">{{ data.repositoryLastCommitId }}</a-descriptions-item>
    </a-descriptions>

    <a-row type="flex" justify="center">
      <a-divider v-if="listQuery.total > 0" dashed>构建历史</a-divider>
      <a-timeline mode="alternate" style="width: 100%">
        <a-timeline-item v-for="item in this.historyList" :key="item.id" :color="statusColor[item.status]">
          <a-space direction="vertical" style="width: 100%">
            <div>
              <a-space>
                <span :style="`color: ${statusColor[item.status]};`" @click="handleBuildLog(item)"
                  >#{{ item.buildNumberId }} <EyeOutlined
                /></span>
                <span v-if="item.buildRemark">构建备注：{{ item.buildRemark }}</span>
              </a-space>
            </div>
            <div>
              <a-tooltip :title="item.statusMsg || statusMap[item.status] || '未知'">
                状态：<a-tag :color="statusColor[item.status]">{{ statusMap[item.status] || '未知' }}</a-tag>
              </a-tooltip>
            </div>
            <div>
              时间：{{ parseTime(item.startTime) }} ~
              {{ parseTime(item.endTime) }}
            </div>
            <div>触发类型：{{ triggerBuildTypeMap[item.triggerBuildType] || '未知' }}</div>
            <div>
              占用空间：{{ renderSize(item.resultFileSize) }}(产物)/{{ renderSize(item.buildLogFileSize) }}(日志)
            </div>

            <div>构建耗时：{{ formatDuration((item.endTime || 0) - (item.startTime || 0), '', 2) }}</div>
            <div>
              发布方式：
              <a-tag> {{ releaseMethodMap[item.releaseMethod] || '未知' }}</a-tag>
            </div>
            <div>
              操作：
              <a-space>
                <a-tooltip title="下载构建日志,如果按钮不可用表示日志文件不存在,一般是构建历史相关文件被删除">
                  <a-button size="small" type="primary" :disabled="!item.hasLog" @click="handleDownload(item)"
                    ><DownloadOutlined />日志</a-button
                  >
                </a-tooltip>

                <a-tooltip
                  title="下载构建产物,如果按钮不可用表示产物文件不存在,一般是构建没有产生对应的文件或者构建历史相关文件被删除"
                >
                  <a-button size="small" type="primary" :disabled="!item.hasFile" @click="handleFile(item)">
                    <DownloadOutlined />
                    产物
                  </a-button>
                </a-tooltip>
                <template v-if="item.releaseMethod !== 5">
                  <a-button
                    size="small"
                    :disabled="!item.hasFile || item.releaseMethod === 0"
                    type="primary"
                    danger
                    @click="handleRollback(item)"
                    >回滚
                  </a-button>
                </template>
                <template v-else>
                  <a-tooltip title="Dockerfile 构建方式不支持在这里回滚">
                    <a-button size="small" :disabled="true" type="primary" danger>回滚 </a-button>
                  </a-tooltip>
                </template>
              </a-space>
            </div>
          </a-space>
        </a-timeline-item>
      </a-timeline>
      <a-divider v-if="listQuery.total / listQuery.limit > 1" dashed />
      <a-col>
        <a-pagination
          v-model:value="listQuery.page"
          :showTotal="
            (total) => {
              return PAGE_DEFAULT_SHOW_TOTAL(total, listQuery)
            }
          "
          :showSizeChanger="true"
          :pageSizeOptions="PAGE_DEFAULT_SIZW_OPTIONS"
          :pageSize="listQuery.limit"
          :total="listQuery.total"
          :hideOnSinglePage="true"
          @showSizeChange="
            (current, size) => {
              this.listQuery.limit = size
              this.listHistory()
            }
          "
          @change="this.listHistory"
          show-less-items
        />
      </a-col>
    </a-row>

    <!-- 构建日志 -->
    <build-log
      v-if="buildLogVisible > 0"
      :temp="temp"
      :visible="buildLogVisible != 0"
      @close="
        () => {
          buildLogVisible = 0
        }
      "
    />
  </div>
</template>

<script>
import {
  getBuildGet,
  releaseMethodMap,
  statusMap,
  geteBuildHistory,
  statusColor,
  triggerBuildTypeMap,
  downloadBuildFile,
  downloadBuildLog,
  rollback
} from '@/api/build-info'
import {
  parseTime,
  PAGE_DEFAULT_LIST_QUERY,
  PAGE_DEFAULT_SIZW_OPTIONS,
  PAGE_DEFAULT_SHOW_TOTAL,
  renderSize,
  formatDuration
} from '@/utils/const'
import { getRepositoryInfo } from '@/api/repository'
import BuildLog from './log'

export default {
  props: {
    id: {
      type: String
    }
  },
  components: {
    BuildLog
  },
  data() {
    return {
      PAGE_DEFAULT_SIZW_OPTIONS,
      triggerBuildTypeMap,
      releaseMethodMap,
      statusColor,
      statusMap,
      data: {},
      listQuery: Object.assign({ buildDataId: this.id }, PAGE_DEFAULT_LIST_QUERY),
      historyList: [],
      tempRepository: null,
      buildLogVisible: 0
    }
  },
  computed: {},
  created() {
    this.refresh()
  },
  methods: {
    parseTime,
    formatDuration,
    PAGE_DEFAULT_SHOW_TOTAL,
    renderSize,
    refresh() {
      this.getData()
      this.listHistory()
    },
    // 选择仓库
    getRepositpry() {
      getRepositoryInfo({
        id: this.data?.repositoryId
      }).then((res) => {
        if (res.code === 200) {
          this.tempRepository = res.data
        }
      })
    },
    // 获取构建数据
    getData() {
      // 构建基础信息
      getBuildGet({
        id: this.id
      }).then((res) => {
        if (res.data) {
          this.data = res.data
          this.getRepositpry()
        }
      })
    },
    listHistory() {
      // 构建历史
      geteBuildHistory(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.historyList = res.data.result
          this.listQuery.total = res.data.total
        }
      })
    },
    // 下载构建日志
    handleDownload(record) {
      window.open(downloadBuildLog(record.id), '_blank')
    },

    // 下载构建产物
    handleFile(record) {
      window.open(downloadBuildFile(record.id), '_blank')
    },
    // 查看构建日志
    handleBuildLog(record) {
      this.temp = {
        id: record.buildDataId,
        buildId: record.buildNumberId
      }
      this.buildLogVisible = new Date() * Math.random()
    },

    // 回滚
    handleRollback(record) {
      const that = this
      this.$confirm({
        title: '系统提示',
        content: '真的要回滚该构建历史记录么？',
        okText: '确认',
        zIndex: 1009,
        cancelText: '取消',
        async onOk() {
          // 重新发布
          return await new Promise((resolve, reject) => {
            rollback(record.id)
              .then((res) => {
                if (res.code === 200) {
                  $notification.success({
                    message: res.msg
                  })
                  that.refresh()
                  // 弹窗
                  that.temp = {
                    id: record.buildDataId,
                    buildId: res.data
                  }
                  that.buildLogVisible = new Date() * Math.random()
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
