<template>
  <div>
    <a-space direction="vertical" style="width: 100%">
      <a-descriptions bordered size="small">
        <template #title>
          <a-space>
            {{ data.name }}
            <a-tooltip :title="$tl('tooltip.refresh')">
              <a-button type="link" size="small" @click="refresh"> <ReloadOutlined /></a-button>
            </a-tooltip>
          </a-space>
        </template>

        <a-descriptions-item :label="$tl('group')">
          {{ data.group }}
        </a-descriptions-item>
        <a-descriptions-item :label="$tl('branchTagName')">
          {{ data.branchName }} {{ data.branchTagName }}
        </a-descriptions-item>
        <a-descriptions-item :label="$tl('buildType')">
          <template v-if="data.buildMode === 1">
            <CloudOutlined />
            {{ $tl('buildMode.container') }}
          </template>
          <template v-else>
            <CodeOutlined />
            {{ $tl('buildMode.local') }}
          </template>
        </a-descriptions-item>

        <a-descriptions-item :label="$tl('buildId')">
          <span v-if="data.buildId <= 0"></span>
          <a-tag v-else color="#108ee9">#{{ data.buildId }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item :label="$tl('buildStatus')">
          <a-tooltip :title="data.statusMsg">
            <a-tag :color="statusColor[data.status]">
              {{ statusMap[data.status] || $tl('unknown') }}

              <InfoCircleOutlined v-if="data.statusMsg" />
            </a-tag>
          </a-tooltip>
        </a-descriptions-item>
        <a-descriptions-item :label="$tl('releaseMethod')"
          >{{ releaseMethodMap[data.releaseMethod] }}
        </a-descriptions-item>
        <a-descriptions-item :label="$tl('autoBuildCron')">
          {{ data.autoBuildCron }}
        </a-descriptions-item>
        <a-descriptions-item :label="$tl('aliasCode')">
          {{ data.aliasCode }}
        </a-descriptions-item>
        <a-descriptions-item :label="$tl('sourceDirExist')">
          <a-tag>{{ data.sourceDirExist ? $tl('exist') : $tl('notExist') }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item :label="$tl('createTime')">
          {{ parseTime(data.createTimeMillis) }}
        </a-descriptions-item>
        <a-descriptions-item :label="$tl('modifyTime')"> {{ parseTime(data.modifyTimeMillis) }}</a-descriptions-item>
        <a-descriptions-item :label="$tl('modifyUser')">
          {{ data.modifyUser }}
        </a-descriptions-item>
        <a-descriptions-item :label="$tl('resultDirFile')" :span="3">
          {{ data.resultDirFile }}
        </a-descriptions-item>
        <a-descriptions-item v-if="tempRepository" :label="$tl('tempRepository.name')" :span="3">{{
          `${tempRepository ? tempRepository.name + '[' + tempRepository.gitUrl + ']' : ''}`
        }}</a-descriptions-item>
        <a-descriptions-item :label="$tl('repositoryLastCommit')" :span="3">
          {{ data.repositoryLastCommitId }}
        </a-descriptions-item>
      </a-descriptions>

      <!-- <a-row type="flex" justify="center"> -->
      <!-- <a-divider v-if="listQuery.total > 0" dashed> 构建历史 </a-divider> -->
      <a-card v-if="listQuery.total > 0" :label="$tl('buildHistory')" size="small">
        <template #extra>
          <a-pagination
            v-model:current="listQuery.page"
            v-model:pageSize="listQuery.limit"
            size="small"
            :show-total="showTotal"
            :show-size-changer="true"
            :page-size-options="PAGE_DEFAULT_SIZW_OPTIONS"
            :total="listQuery.total"
            :hide-on-single-page="true"
            show-less-items
            @show-size-change="showSizeChange"
            @change="listHistory"
          />
        </template>
        <a-timeline mode="alternate" style="width: 100%">
          <a-timeline-item v-for="item in historyList" :key="item.id" :color="statusColor[item.status]">
            <a-space direction="vertical" style="width: 100%">
              <div>
                <a-space>
                  <span :style="`color: ${statusColor[item.status]};`" @click="handleBuildLog(item)"
                    >#{{ item.buildNumberId }} <EyeOutlined
                  /></span>
                  <span v-if="item.buildRemark">{{ $tl('buildRemarks') }}：{{ item.buildRemark }}</span>
                </a-space>
              </div>
              <div>
                <a-tooltip :title="item.statusMsg || statusMap[item.status] || $tl('unknown')">
                  {{ $tl('status') }}：<a-tag :color="statusColor[item.status]">{{
                    statusMap[item.status] || $tl('unknown')
                  }}</a-tag>
                </a-tooltip>
              </div>
              <div>
                {{ $tl('time') }}：{{ parseTime(item.startTime) }} ~
                {{ parseTime(item.endTime) }}
              </div>
              <div>{{ $tl('triggerType') }}：{{ triggerBuildTypeMap[item.triggerBuildType] || $tl('unknown') }}</div>
              <div>
                {{ $tl('occupySpace') }}：{{ renderSize(item.resultFileSize) }}({{ $tl('product') }})/{{
                  renderSize(item.buildLogFileSize)
                }}({{ $tl('logs') }})
              </div>

              <div>
                {{ $tl('constructionTime') }}：{{ formatDuration((item.endTime || 0) - (item.startTime || 0), '', 2) }}
              </div>
              <div>
                {{ $tl('publishingMethod') }}：
                <a-tag> {{ releaseMethodMap[item.releaseMethod] || $tl('unknown') }}</a-tag>
              </div>
              <div>
                {{ $tl('publishingMethod') }}：
                <a-space>
                  <a-tooltip :title="$tl('tooltip.logs')">
                    <a-button size="small" type="primary" :disabled="!item.hasLog" @click="handleDownload(item)"
                      ><DownloadOutlined /> {{ $tl('logs') }}</a-button
                    >
                  </a-tooltip>

                  <a-tooltip :title="$tl('tooltip.product')">
                    <a-button size="small" type="primary" :disabled="!item.hasFile" @click="handleFile(item)">
                      <DownloadOutlined />
                      {{ $tl('product') }}
                    </a-button>
                  </a-tooltip>
                  <template v-if="item.releaseMethod !== 5">
                    <a-button
                      size="small"
                      :disabled="!item.hasFile || item.releaseMethod === 0"
                      type="primary"
                      danger
                      @click="handleRollback(item)"
                    >
                      {{ $tl('rollback') }}
                    </a-button>
                  </template>
                  <template v-else>
                    <a-tooltip :title="$tl('tooltip.rollback')">
                      <a-button size="small" :disabled="true" type="primary" danger>
                        {{ $tl('rollback') }}
                      </a-button>
                    </a-tooltip>
                  </template>
                </a-space>
              </div>
            </a-space>
          </a-timeline-item>
        </a-timeline>
      </a-card>
    </a-space>
    <!-- <a-divider v-if="listQuery.total / listQuery.limit > 1" dashed />
      <a-col>

      </a-col> -->
    <!-- </a-row> -->

    <!-- 构建日志 -->
    <build-log v-if="buildLogVisible > 0" :temp="temp" :visible="buildLogVisible != 0" @close="buildLogVisible = 0" />
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
  components: {
    BuildLog
  },
  props: {
    id: {
      type: String,
      default: ''
    }
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
      buildLogVisible: 0,
      pagePath: 'pages.build.details.'
    }
  },
  computed: {},
  created() {
    if (this.id) {
      this.refresh()
    }
  },
  methods: {
    parseTime,
    formatDuration,
    PAGE_DEFAULT_SHOW_TOTAL,
    $tl(key, ...arg) {
      return this.$t.call(this, this.pagePath + key, ...arg)
    },
    showTotal(total) {
      return PAGE_DEFAULT_SHOW_TOTAL(total, this.listQuery)
    },
    renderSize,
    refresh() {
      this.getData()
      this.listHistory()
    },
    showSizeChange(current, size) {
      this.listQuery.limit = size
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
      $confirm({
        title: '系统提示',
        content: '真的要回滚该构建历史记录么？',
        okText: '确认',
        zIndex: 1009,
        cancelText: '取消',
        onOk: () => {
          return rollback(record.id).then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              this.refresh()
              // 弹窗
              this.temp = {
                id: record.buildDataId,
                buildId: res.data
              }
              this.buildLogVisible = new Date() * Math.random()
            }
          })
        }
      })
    }
  }
}
</script>
