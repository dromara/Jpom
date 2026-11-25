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
  <div>
    <CustomTable
      is-show-tools
      table-name="pipelineRunList"
      :active-page="open"
      :auto-refresh-time="5"
      :columns="columns"
      :data-source="list"
      :loading="loading"
      :pagination="pagination"
      :scroll="{ x: 'max-content' }"
      row-key="id"
      size="middle"
      bordered
      :empty-description="$t('i18n_pipeline_run_empty')"
      @change="handleTableChange"
      @refresh="loadData"
    >
      <template #title>
        <a-space wrap class="search-box">
          <span class="run-title">{{ pipelineName }}</span>
          <a-button type="primary" :loading="loading" @click="loadData">{{ $t('i18n_e5f71fc31e') }}</a-button>
        </a-space>
      </template>
      <template #tableBodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'runNumber'">
          <a-tag color="blue">#{{ record.runNumber }}</a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'status'">
          <a-tooltip :title="record.statusMsg || pipelineStatusMap[record.status]">
            <a-tag :color="pipelineStatusColor[record.status]">
              {{ pipelineStatusMap[record.status] || $t('i18n_pipeline_status_unknown') }}
            </a-tag>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'triggerType'">
          {{ triggerTypeText(record.triggerType) }}
        </template>
        <template v-else-if="column.dataIndex === 'startTime'">
          {{ parseTime(record.startTime) }}
        </template>
        <template v-else-if="column.dataIndex === 'duration'">
          {{ duration(record) }}
        </template>
        <template v-else-if="column.dataIndex === 'stages'">
          <a-tooltip :title="stageTooltip(record)">
            <a-space :size="4" class="stage-inline">
              <a-tag v-for="stage in parseStages(record)" :key="stage.stageId" :color="stageColor(stage.status)">
                {{ stage.stageName }}
              </a-tag>
            </a-space>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button size="small" type="primary" data-testid="pipeline-run-log" @click="handleLog(record)">
              {{ $t('i18n_pipeline_run_log') }}
            </a-button>
            <a-button
              size="small"
              type="primary"
              danger
              data-testid="pipeline-run-stop"
              :disabled="!isRunning(record)"
              @click="handleCancel(record)"
            >
              {{ $t('i18n_095e938e2a') }}
            </a-button>
          </a-space>
        </template>
      </template>
    </CustomTable>

    <PipelineLog :open="logVisible" :run="logRun" @close="logVisible = false" />
  </div>
</template>

<script>
import { message } from 'ant-design-vue'
import {
  getPipelineRuns,
  cancelPipeline,
  pipelineStatusMap,
  pipelineStatusColor
} from '@/api/pipeline'
import { parseTime, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, CHANGE_PAGE } from '@/utils/const'
import PipelineLog from './log.vue'

export default {
  name: 'PipelineRun',
  components: { PipelineLog },
  props: {
    open: { type: Boolean, default: false },
    pipelineId: { type: String, default: '' },
    pipelineName: { type: String, default: '' }
  },
  emits: ['close'],
  data() {
    return {
      pipelineStatusMap,
      pipelineStatusColor,
      loading: false,
      list: [],
      listQuery: Object.assign({ pipelineId: this.pipelineId }, PAGE_DEFAULT_LIST_QUERY),
      logVisible: false,
      logRun: null,
      columns: [
        { title: this.$t('i18n_pipeline_run_number'), dataIndex: 'runNumber', key: 'runNumber', width: 100 },
        { title: this.$t('i18n_3fea7ca76c'), dataIndex: 'status', key: 'status', width: 200 },
        { title: this.$t('i18n_pipeline_trigger_type'), dataIndex: 'triggerType', key: 'triggerType', width: 110 },
        { title: this.$t('i18n_592c595891'), dataIndex: 'startTime', key: 'startTime', width: 170 },
        { title: this.$t('i18n_pipeline_duration'), dataIndex: 'duration', key: 'duration', width: 110 },
        { title: this.$t('i18n_pipeline_stage_result'), dataIndex: 'stages', key: 'stages' },
        {
          title: this.$t('i18n_2b6bc0f293'),
          dataIndex: 'operation',
          key: 'operation',
          width: '1%',
          align: 'center',
          fixed: 'right'
        }
      ]
    }
  },
  computed: {
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery)
    }
  },
  mounted() {
    // 抽屉是 v-if 直开（open 初始即为 true），watch 不会触发：挂载时主动拉一次
    if (this.open) {
      this.listQuery.pipelineId = this.pipelineId
      this.loadData()
    }
  },
  watch: {
    open(value) {
      if (value) {
        this.listQuery.pipelineId = this.pipelineId
        this.listQuery.page = 1
        this.loadData()
      }
    },
    pipelineId() {
      if (this.open) {
        this.listQuery.pipelineId = this.pipelineId
        this.listQuery.page = 1
        this.loadData()
      }
    }
  },
  methods: {
    parseTime,
    handleTableChange(pagination) {
      this.listQuery = CHANGE_PAGE(this.listQuery, { pagination })
      this.loadData()
    },
    loadData() {
      if (!this.pipelineId) {
        return
      }
      this.loading = true
      getPipelineRuns(this.listQuery)
        .then((res) => {
          if (res.code === 200) {
            this.list = (res.data && res.data.result) || []
            this.listQuery.total = res.data.total || 0
          } else {
            message.warn({ message: res.msg })
          }
        })
        .finally(() => {
          this.loading = false
        })
    },
    parseStages(record) {
      try {
        return JSON.parse(record.stagesResult || '[]')
      } catch (e) {
        return []
      }
    },
    stageTooltip(record) {
      return this.parseStages(record)
        .map((stage) => stage.stageName + '：' + stage.status + (stage.message ? ' · ' + stage.message : ''))
        .join('\n')
    },
    stageColor(status) {
      if (status === 'SUCCESS') return 'green'
      if (status === 'FAILED') return 'red'
      if (status === 'CANCELLED') return 'blue'
      if (status === 'SKIPPED') return 'default'
      return ''
    },
    triggerTypeText(type) {
      const map = {
        0: this.$t('i18n_pipeline_trigger_manual'),
        1: this.$t('i18n_pipeline_trigger_cron'),
        2: this.$t('i18n_pipeline_trigger_system'),
        3: this.$t('i18n_pipeline_trigger_rerun')
      }
      return map[type] || '-'
    },
    duration(record) {
      if (!record.startTime || !record.endTime) {
        return '-'
      }
      return this.formatDuration(record.endTime - record.startTime)
    },
    formatDuration(ms) {
      const total = Math.max(0, ms)
      const seconds = Math.floor(total / 1000)
      if (seconds < 60) return seconds + 's'
      const minutes = Math.floor(seconds / 60)
      if (minutes < 60) return minutes + 'm ' + (seconds % 60) + 's'
      return Math.floor(minutes / 60) + 'h ' + (minutes % 60) + 'm'
    },
    isRunning(record) {
      return [1, 4, 9, 11].indexOf(record.status) > -1
    },
    handleLog(record) {
      this.logRun = record
      this.logVisible = true
    },
    handleCancel(record) {
      cancelPipeline({ id: this.pipelineId }).then((res) => {
        message.info({ message: res.data === 'REQUESTED' ? this.$t('i18n_pipeline_cancel_requested') : res.data })
        this.loadData()
      })
    }
  }
}
</script>

<style scoped>
.run-title {
  font-weight: 600;
}
.stage-inline {
  white-space: nowrap;
}
</style>
