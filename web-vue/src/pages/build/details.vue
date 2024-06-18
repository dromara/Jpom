<template>
  <div>
    <a-space direction="vertical" style="width: 100%">
      <a-descriptions bordered size="small">
        <template #title>
          <a-space>
            {{ data.name }}
            <a-tooltip :title="$t('i18n_f33db5e0b2')">
              <a-button type="link" size="small" @click="refresh"> <ReloadOutlined /></a-button>
            </a-tooltip>
          </a-space>
        </template>

        <a-descriptions-item :label="$t('i18n_829abe5a8d')">
          {{ data.group }}
        </a-descriptions-item>
        <a-descriptions-item :label="$t('i18n_6f854129e9')">
          {{ data.branchName }} {{ data.branchTagName }}
        </a-descriptions-item>
        <a-descriptions-item :label="$t('i18n_17a74824de')">
          <template v-if="data.buildMode === 1">
            <CloudOutlined />
            {{ $t('i18n_685e5de706') }}
          </template>
          <template v-else>
            <CodeOutlined />
            {{ $t('i18n_69c3b873c1') }}
          </template>
        </a-descriptions-item>

        <a-descriptions-item :label="$t('i18n_66aafbdb72')">
          <span v-if="data.buildId <= 0"></span>
          <a-tag v-else color="#108ee9">#{{ data.buildId }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item :label="$t('i18n_248c9aa7aa')">
          <a-tooltip :title="data.statusMsg">
            <a-tag :color="statusColor[data.status]">
              {{ statusMap[data.status] || $t('i18n_1622dc9b6b') }}

              <InfoCircleOutlined v-if="data.statusMsg" />
            </a-tag>
          </a-tooltip>
        </a-descriptions-item>
        <a-descriptions-item :label="$t('i18n_f98994f7ec')"
          >{{ releaseMethodMap[data.releaseMethod] }}
        </a-descriptions-item>
        <a-descriptions-item :label="$t('i18n_db9296212a')">
          {{ data.autoBuildCron }}
        </a-descriptions-item>
        <a-descriptions-item :label="$t('i18n_2f5e828ecd')">
          {{ data.aliasCode }}
        </a-descriptions-item>
        <a-descriptions-item :label="$t('i18n_d175a854a6')">
          <a-tag>{{ data.sourceDirExist ? $t('i18n_df9497ea98') : $t('i18n_d7d11654a7') }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item :label="$t('i18n_eca37cb072')">
          {{ parseTime(data.createTimeMillis) }}
        </a-descriptions-item>
        <a-descriptions-item :label="$t('i18n_4b96762a7e')">
          {{ parseTime(data.modifyTimeMillis) }}</a-descriptions-item
        >
        <a-descriptions-item :label="$t('i18n_3bcc1c7a20')">{{ data.modifyUser }}</a-descriptions-item>
        <a-descriptions-item :label="$t('i18n_7dfcab648d')" :span="3">
          {{ data.resultDirFile }}
        </a-descriptions-item>
        <a-descriptions-item v-if="tempRepository" :label="$t('i18n_b3ef35a359')" :span="3">{{
          `${tempRepository ? tempRepository.name + '[' + tempRepository.gitUrl + ']' : ''}`
        }}</a-descriptions-item>
        <a-descriptions-item :label="$t('i18n_86e9e4dd58')" :span="3">{{
          data.repositoryLastCommitId
        }}</a-descriptions-item>
      </a-descriptions>

      <!-- <a-row type="flex" justify="center"> -->
      <!-- <a-divider v-if="listQuery.total > 0" dashed> 构建历史 </a-divider> -->
      <a-card v-if="listQuery.total > 0" :title="$t('i18n_a05c1667ca')" size="small">
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
                  <span v-if="item.buildRemark">{{ $t('i18n_65571516e2') }}{{ item.buildRemark }}</span>
                </a-space>
              </div>
              <div>
                <a-tooltip :title="item.statusMsg || statusMap[item.status] || $t('i18n_1622dc9b6b')">
                  {{ $t('i18n_bec98b4d6a')
                  }}<a-tag :color="statusColor[item.status]">{{
                    statusMap[item.status] || $t('i18n_1622dc9b6b')
                  }}</a-tag>
                </a-tooltip>
              </div>
              <div>
                {{ $t('i18n_14e6d83ff5') }}{{ parseTime(item.startTime) }} ~
                {{ parseTime(item.endTime) }}
              </div>
              <div>
                {{ $t('i18n_b5a1e1f2d1') }}{{ triggerBuildTypeMap[item.triggerBuildType] || $t('i18n_1622dc9b6b') }}
              </div>
              <div>
                {{ $t('i18n_8dbe0c2ffa') }}{{ renderSize(item.resultFileSize) }}({{ $t('i18n_7dfcab648d') }})/{{
                  renderSize(item.buildLogFileSize)
                }}({{ $t('i18n_456d29ef8b') }})
              </div>

              <div>
                {{ $t('i18n_3c014532b1') }}{{ formatDuration((item.endTime || 0) - (item.startTime || 0), '', 2) }}
              </div>
              <div>
                {{ $t('i18n_e8321f5a61') }}
                <a-tag> {{ releaseMethodMap[item.releaseMethod] || $t('i18n_1622dc9b6b') }}</a-tag>
              </div>
              <div>
                {{ $t('i18n_4a5ab3bc72') }}
                <a-space>
                  <a-tooltip :title="$t('i18n_b38d7db9b0')">
                    <a-button size="small" type="primary" :disabled="!item.hasLog" @click="handleDownload(item)"
                      ><DownloadOutlined />{{ $t('i18n_456d29ef8b') }}</a-button
                    >
                  </a-tooltip>

                  <a-tooltip :title="$t('i18n_02e35447d4')">
                    <a-button size="small" type="primary" :disabled="!item.hasFile" @click="handleFile(item)">
                      <DownloadOutlined />
                      {{ $t('i18n_7dfcab648d') }}
                    </a-button>
                  </a-tooltip>
                  <template v-if="item.releaseMethod !== 5">
                    <a-button
                      size="small"
                      :disabled="!item.hasFile || item.releaseMethod === 0"
                      type="primary"
                      danger
                      @click="handleRollback(item)"
                      >{{ $t('i18n_d00b485b26') }}
                    </a-button>
                  </template>
                  <template v-else>
                    <a-tooltip :title="$t('i18n_2d94b9cf0e')">
                      <a-button size="small" :disabled="true" type="primary" danger
                        >{{ $t('i18n_d00b485b26') }}
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
        title: this.$t('i18n_c4535759ee'),
        content: this.$t('i18n_fb61d4d708'),
        okText: this.$t('i18n_e83a256e4f'),
        zIndex: 1009,
        cancelText: this.$t('i18n_625fb26b4b'),
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
