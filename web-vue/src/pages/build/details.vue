<template>
  <div>
    <a-space direction="vertical" style="width: 100%">
      <a-descriptions bordered size="small">
        <template #title>
          <a-space>
            {{ data.name }}
            <a-tooltip :title="$t('pages.build.details.948517a9')">
              <a-button type="link" size="small" @click="refresh"> <ReloadOutlined /></a-button>
            </a-tooltip>
          </a-space>
        </template>

        <a-descriptions-item :label="$t('pages.build.details.3228ecae')">
          {{ data.group }}
        </a-descriptions-item>
        <a-descriptions-item :label="$t('pages.build.details.83d5a14e')">
          {{ data.branchName }} {{ data.branchTagName }}
        </a-descriptions-item>
        <a-descriptions-item :label="$t('pages.build.details.bc1227cd')">
          <template v-if="data.buildMode === 1">
            <CloudOutlined />
            {{ $t('pages.build.details.2e6fea2b') }}
          </template>
          <template v-else>
            <CodeOutlined />
            {{ $t('pages.build.details.75a44c2b') }}
          </template>
        </a-descriptions-item>

        <a-descriptions-item :label="$t('pages.build.details.21dcaeba')">
          <span v-if="data.buildId <= 0"></span>
          <a-tag v-else color="#108ee9">#{{ data.buildId }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item :label="$t('pages.build.details.994b78b1')">
          <a-tooltip :title="data.statusMsg">
            <a-tag :color="statusColor[data.status]">
              {{ statusMap[data.status] || $t('pages.build.details.5f51a112') }}

              <InfoCircleOutlined v-if="data.statusMsg" />
            </a-tag>
          </a-tooltip>
        </a-descriptions-item>
        <a-descriptions-item :label="$t('pages.build.details.907f888f')"
          >{{ releaseMethodMap[data.releaseMethod] }}
        </a-descriptions-item>
        <a-descriptions-item :label="$t('pages.build.details.19083f0e')">
          {{ data.autoBuildCron }}
        </a-descriptions-item>
        <a-descriptions-item :label="$t('pages.build.details.9dfaff03')">
          {{ data.aliasCode }}
        </a-descriptions-item>
        <a-descriptions-item :label="$t('pages.build.details.89ba7e0f')">
          <a-tag>{{
            data.sourceDirExist ? $t('pages.build.details.f03e24c') : $t('pages.build.details.f6eed47f')
          }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item :label="$t('pages.build.details.f5b90169')">
          {{ parseTime(data.createTimeMillis) }}
        </a-descriptions-item>
        <a-descriptions-item :label="$t('pages.build.details.c4dbb7c9')">
          {{ parseTime(data.modifyTimeMillis) }}</a-descriptions-item
        >
        <a-descriptions-item :label="$t('pages.build.details.49942d36')">{{ data.modifyUser }}</a-descriptions-item>
        <a-descriptions-item :label="$t('pages.build.details.213d0278')" :span="3">
          {{ data.resultDirFile }}
        </a-descriptions-item>
        <a-descriptions-item v-if="tempRepository" :label="$t('pages.build.details.1beebcf2')" :span="3">{{
          `${tempRepository ? tempRepository.name + '[' + tempRepository.gitUrl + ']' : ''}`
        }}</a-descriptions-item>
        <a-descriptions-item :label="$t('pages.build.details.736d2699')" :span="3">{{
          data.repositoryLastCommitId
        }}</a-descriptions-item>
      </a-descriptions>

      <!-- <a-row type="flex" justify="center"> -->
      <!-- <a-divider v-if="listQuery.total > 0" dashed> 构建历史 </a-divider> -->
      <a-card v-if="listQuery.total > 0" :title="$t('pages.build.details.8f3fe403')" size="small">
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
                  <span v-if="item.buildRemark">{{ $t('pages.build.details.11757b06') }}{{ item.buildRemark }}</span>
                </a-space>
              </div>
              <div>
                <a-tooltip :title="item.statusMsg || statusMap[item.status] || $t('pages.build.details.5f51a112')">
                  {{ $t('pages.build.details.9c32c887')
                  }}<a-tag :color="statusColor[item.status]">{{
                    statusMap[item.status] || $t('pages.build.details.5f51a112')
                  }}</a-tag>
                </a-tooltip>
              </div>
              <div>
                {{ $t('pages.build.details.8ac17a5e') }}{{ parseTime(item.startTime) }} ~
                {{ parseTime(item.endTime) }}
              </div>
              <div>
                {{ $t('pages.build.details.4abba04f')
                }}{{ triggerBuildTypeMap[item.triggerBuildType] || $t('pages.build.details.5f51a112') }}
              </div>
              <div>
                {{ $t('pages.build.details.2314b40d') }}{{ renderSize(item.resultFileSize) }}({{
                  $t('pages.build.details.213d0278')
                }})/{{ renderSize(item.buildLogFileSize) }}({{ $t('pages.build.details.2823935a') }})
              </div>

              <div>
                {{ $t('pages.build.details.fef0ccf6')
                }}{{ formatDuration((item.endTime || 0) - (item.startTime || 0), '', 2) }}
              </div>
              <div>
                {{ $t('pages.build.details.89952fc1') }}
                <a-tag> {{ releaseMethodMap[item.releaseMethod] || $t('pages.build.details.5f51a112') }}</a-tag>
              </div>
              <div>
                {{ $t('pages.build.details.3bb962bf') }}
                <a-space>
                  <a-tooltip :title="$t('pages.build.details.cafd8c4c')">
                    <a-button size="small" type="primary" :disabled="!item.hasLog" @click="handleDownload(item)"
                      ><DownloadOutlined />{{ $t('pages.build.details.2823935a') }}</a-button
                    >
                  </a-tooltip>

                  <a-tooltip :title="$t('pages.build.details.6a09ee66')">
                    <a-button size="small" type="primary" :disabled="!item.hasFile" @click="handleFile(item)">
                      <DownloadOutlined />
                      {{ $t('pages.build.details.213d0278') }}
                    </a-button>
                  </a-tooltip>
                  <template v-if="item.releaseMethod !== 5">
                    <a-button
                      size="small"
                      :disabled="!item.hasFile || item.releaseMethod === 0"
                      type="primary"
                      danger
                      @click="handleRollback(item)"
                      >{{ $t('pages.build.details.6f9c6f93') }}
                    </a-button>
                  </template>
                  <template v-else>
                    <a-tooltip :title="$t('pages.build.details.53bdfc23')">
                      <a-button size="small" :disabled="true" type="primary" danger
                        >{{ $t('pages.build.details.6f9c6f93') }}
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
        title: this.$t('pages.build.details.7cd89470'),
        content: this.$t('pages.build.details.5d19de17'),
        okText: this.$t('pages.build.details.e8e9db25'),
        zIndex: 1009,
        cancelText: this.$t('pages.build.details.b12468e9'),
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
