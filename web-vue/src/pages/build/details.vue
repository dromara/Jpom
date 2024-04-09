<template>
  <div>
    <a-space direction="vertical" style="width: 100%">
      <a-descriptions bordered size="small">
        <template #title>
          <a-space>
            {{ data.name }}
            <a-tooltip :title="$tl('p.clickToRefreshBuildInfo')">
              <a-button type="link" size="small" @click="refresh"> <ReloadOutlined /></a-button>
            </a-tooltip>
          </a-space>
        </template>

        <a-descriptions-item :label="$tl('p.grouping')">
          {{ data.group }}
        </a-descriptions-item>
        <a-descriptions-item :label="$tl('p.tag')">
          {{ data.branchName }} {{ data.branchTagName }}
        </a-descriptions-item>
        <a-descriptions-item :label="$tl('p.buildMethod')">
          <template v-if="data.buildMode === 1">
            <CloudOutlined />
            {{ $tl('p.containerBuild') }}
          </template>
          <template v-else>
            <CodeOutlined />
            {{ $tl('p.localBuild') }}
          </template>
        </a-descriptions-item>

        <a-descriptions-item :label="$tl('p.latestBuildId')">
          <span v-if="data.buildId <= 0"></span>
          <a-tag v-else color="#108ee9">#{{ data.buildId }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item :label="$tl('p.buildStatus')">
          <a-tooltip :title="data.statusMsg">
            <a-tag :color="statusColor[data.status]">
              {{ statusMap[data.status] || $tl('c.unknown') }}

              <InfoCircleOutlined v-if="data.statusMsg" />
            </a-tag>
          </a-tooltip>
        </a-descriptions-item>
        <a-descriptions-item :label="$tl('p.publishMethod')"
          >{{ releaseMethodMap[data.releaseMethod] }}
        </a-descriptions-item>
        <a-descriptions-item :label="$tl('p.scheduleBuild')">
          {{ data.autoBuildCron }}
        </a-descriptions-item>
        <a-descriptions-item :label="$tl('p.aliasCode')">
          {{ data.aliasCode }}
        </a-descriptions-item>
        <a-descriptions-item :label="$tl('p.buildDirectory')">
          <a-tag>{{ data.sourceDirExist ? $tl('p.existence') : $tl('p.nonExistence') }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item :label="$tl('p.createTime')">
          {{ parseTime(data.createTimeMillis) }}
        </a-descriptions-item>
        <a-descriptions-item :label="$tl('p.lastModificationTime')">
          {{ parseTime(data.modifyTimeMillis) }}</a-descriptions-item
        >
        <a-descriptions-item :label="$tl('p.lastModifier')">{{ data.modifyUser }}</a-descriptions-item>
        <a-descriptions-item :label="$tl('c.product')" :span="3">
          {{ data.resultDirFile }}
        </a-descriptions-item>
        <a-descriptions-item v-if="tempRepository" :label="$tl('p.sourceRepository')" :span="3">{{
          `${tempRepository ? tempRepository.name + '[' + tempRepository.gitUrl + ']' : ''}`
        }}</a-descriptions-item>
        <a-descriptions-item :label="$tl('p.repositoryLastCommit')" :span="3">{{
          data.repositoryLastCommitId
        }}</a-descriptions-item>
      </a-descriptions>

      <!-- <a-row type="flex" justify="center"> -->
      <!-- <a-divider v-if="listQuery.total > 0" dashed> 构建历史 </a-divider> -->
      <a-card v-if="listQuery.total > 0" :title="$tl('p.buildHistory')" size="small">
        <template #extra>
          <a-pagination
            v-model:current="listQuery.page"
            v-model:pageSize="listQuery.limit"
            size="small"
            :show-total="
              (total) => {
                return PAGE_DEFAULT_SHOW_TOTAL(total, listQuery)
              }
            "
            :show-size-changer="true"
            :page-size-options="PAGE_DEFAULT_SIZW_OPTIONS"
            :total="listQuery.total"
            :hide-on-single-page="true"
            show-less-items
            @show-size-change="
              (current, size) => {
                listQuery.limit = size
                listHistory()
              }
            "
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
                  <span v-if="item.buildRemark">{{ $tl('p.buildNote') }}{{ item.buildRemark }}</span>
                </a-space>
              </div>
              <div>
                <a-tooltip :title="item.statusMsg || statusMap[item.status] || $tl('c.unknown')">
                  {{ $tl('p.status')
                  }}<a-tag :color="statusColor[item.status]">{{ statusMap[item.status] || $tl('c.unknown') }}</a-tag>
                </a-tooltip>
              </div>
              <div>
                {{ $tl('p.time') }}{{ parseTime(item.startTime) }} ~
                {{ parseTime(item.endTime) }}
              </div>
              <div>{{ $tl('p.triggerType') }}{{ triggerBuildTypeMap[item.triggerBuildType] || $tl('c.unknown') }}</div>
              <div>
                {{ $tl('p.occupiedSpace') }}{{ renderSize(item.resultFileSize) }}({{ $tl('c.product') }})/{{
                  renderSize(item.buildLogFileSize)
                }}({{ $tl('c.log') }})
              </div>

              <div>
                {{ $tl('p.buildDuration') }}{{ formatDuration((item.endTime || 0) - (item.startTime || 0), '', 2) }}
              </div>
              <div>
                {{ $tl('p.publishMethodDetail') }}
                <a-tag> {{ releaseMethodMap[item.releaseMethod] || $tl('c.unknown') }}</a-tag>
              </div>
              <div>
                {{ $tl('p.operation') }}
                <a-space>
                  <a-tooltip :title="$tl('p.downloadBuildLog')">
                    <a-button size="small" type="primary" :disabled="!item.hasLog" @click="handleDownload(item)"
                      ><DownloadOutlined />{{ $tl('c.log') }}</a-button
                    >
                  </a-tooltip>

                  <a-tooltip :title="$tl('p.downloadProduct')">
                    <a-button size="small" type="primary" :disabled="!item.hasFile" @click="handleFile(item)">
                      <DownloadOutlined />
                      {{ $tl('c.product') }}
                    </a-button>
                  </a-tooltip>
                  <template v-if="item.releaseMethod !== 5">
                    <a-button
                      size="small"
                      :disabled="!item.hasFile || item.releaseMethod === 0"
                      type="primary"
                      danger
                      @click="handleRollback(item)"
                      >{{ $tl('c.rollback') }}
                    </a-button>
                  </template>
                  <template v-else>
                    <a-tooltip :title="$tl('p.dockerfileBuildNotSupported')">
                      <a-button size="small" :disabled="true" type="primary" danger>{{ $tl('c.rollback') }} </a-button>
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
} from '@/api/build/build-info'
import {
  parseTime,
  PAGE_DEFAULT_LIST_QUERY,
  PAGE_DEFAULT_SIZW_OPTIONS,
  PAGE_DEFAULT_SHOW_TOTAL,
  renderSize,
  formatDuration
} from '@/utils/const'
import { getRepositoryInfo } from '@/api/build/repository'
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
      buildLogVisible: 0
    }
  },
  computed: {},
  created() {
    if (this.id) {
      this.refresh()
    }
  },
  methods: {
    $tl(key, ...args) {
      return this.$t(`pages.build.details.${key}`, ...args)
    },
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
      $confirm({
        title: this.$tl('p.systemHint'),
        content: this.$tl('p.confirmRollbackBuildHistory'),
        okText: this.$tl('p.confirm'),
        zIndex: 1009,
        cancelText: this.$tl('p.cancel'),
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
