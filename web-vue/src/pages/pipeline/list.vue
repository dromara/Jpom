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
      table-name="pipelineList"
      :active-page="true"
      default-auto-refresh
      :auto-refresh-time="5"
      :columns="columns"
      :data-source="list"
      :loading="loading"
      :pagination="pagination"
      :scroll="{ x: 'max-content' }"
      row-key="id"
      size="middle"
      bordered
      :empty-description="$t('i18n_pipeline_list_empty')"
      @change="handleTableChange"
      @refresh="loadData"
    >
      <template #title>
        <a-space wrap class="search-box">
          <a-input
            v-model:value="listQuery['%name%']"
            allow-clear
            class="search-input-item"
            :placeholder="$t('i18n_pipeline_name')"
            @press-enter="handleSearch"
          />
          <a-input
            v-model:value="listQuery['%groupName%']"
            allow-clear
            class="search-input-item"
            :placeholder="$t('i18n_829abe5a8d')"
            @press-enter="handleSearch"
          />
          <a-select
            v-model:value="listQuery.status"
            allow-clear
            class="search-input-item"
            :placeholder="$t('i18n_3fea7ca76c')"
            :options="statusOptions"
          />
          <a-button type="primary" :loading="loading" @click="handleSearch">{{ $t('i18n_e5f71fc31e') }}</a-button>
          <a-button type="primary" data-testid="pipeline-add" @click="handleAdd">{{ $t('i18n_pipeline_add') }}</a-button>
        </a-space>
      </template>
      <template #tableBodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'name'">
          <a-button type="link" size="small" style="padding: 0" data-testid="pipeline-row-edit" @click="handleEdit(record)">
            {{ record.name }}
          </a-button>
        </template>
        <template v-else-if="column.dataIndex === 'status'">
          <a-tooltip :title="record.statusMsg || pipelineStatusMap[record.status]">
            <a-tag :color="pipelineStatusColor[record.status]">
              {{ pipelineStatusMap[record.status] || $t('i18n_pipeline_status_never') }}
            </a-tag>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'stageCount'">
          {{ stageCount(record) }}
        </template>
        <template v-else-if="column.dataIndex === 'sourceCount'">
          {{ sourceCount(record) }}
        </template>
        <template v-else-if="column.dataIndex === 'runNumber'">
          <a-tag>{{ record.runNumber || 0 }}</a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'modifyTimeMillis'">
          {{ parseTime(record.modifyTimeMillis) }}
        </template>
        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button
              v-if="isRunning(record)"
              size="small"
              type="primary"
              danger
              data-testid="pipeline-list-stop"
              @click="handleCancel(record)"
            >
              {{ $t('i18n_095e938e2a') }}
            </a-button>
            <a-button v-else size="small" type="primary" data-testid="pipeline-list-run" @click="handleRun(record)">
              {{ $t('i18n_pipeline_run') }}
            </a-button>
            <a-button size="small" type="primary" data-testid="pipeline-list-runs" @click="handleRuns(record)">
              {{ $t('i18n_pipeline_run_record') }}
            </a-button>
            <a-dropdown>
              <a class="ant-dropdown-link" @click.prevent>
                {{ $t('i18n_pipeline_more') }}
                <DownOutlined />
              </a>
              <template #overlay>
                <a-menu>
                  <a-menu-item>
                    <a-button size="small" type="primary" data-testid="pipeline-list-edit" @click="handleEdit(record)">
                      {{ $t('i18n_95b351c862') }}
                    </a-button>
                  </a-menu-item>
                  <a-menu-item>
                    <a-popconfirm :title="$t('i18n_pipeline_delete_confirm')" @confirm="handleDelete(record)">
                      <a-button size="small" type="primary" danger>{{ $t('i18n_2f4aaddde3') }}</a-button>
                    </a-popconfirm>
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
        </template>
      </template>
    </CustomTable>

    <!-- 编辑：抽屉（与在线构建的编辑入口一致，不再占用整页） -->
    <CustomDrawer
      v-if="editVisible"
      destroy-on-close
      placement="right"
      width="92vw"
      :open="editVisible"
      :body-style="{ padding: '12px' }"
      @close="handleEditClose"
    >
      <template #title>
        <a-space>
          <span>{{ editingId ? $t('i18n_pipeline_edit') : $t('i18n_pipeline_add') }}</span>
          <a-tag v-if="editingName">{{ editingName }}</a-tag>
        </a-space>
      </template>
      <PipelineEdit v-if="editVisible" :id="editingId" @close="handleEditClose" @saved="handleSaved" />
    </CustomDrawer>

    <!-- 运行记录：抽屉 -->
    <CustomDrawer
      v-if="runVisible"
      destroy-on-close
      placement="right"
      width="82vw"
      :open="runVisible"
      :body-style="{ padding: '12px' }"
      @close="runVisible = false"
    >
      <template #title>{{ $t('i18n_pipeline_run_record') }}</template>
      <PipelineRun
        v-if="runVisible"
        :open="runVisible"
        :pipeline-id="runningId"
        :pipeline-name="runningName"
      />
    </CustomDrawer>
  </div>
</template>

<script>
import { message } from 'ant-design-vue'
import {
  getPipelineList,
  deletePipeline,
  runPipeline,
  cancelPipeline,
  pipelineStatusMap,
  pipelineStatusColor
} from '@/api/pipeline'
import { parseTime, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, CHANGE_PAGE } from '@/utils/const'
import PipelineEdit from './edit.vue'
import PipelineRun from './run.vue'

export default {
  name: 'PipelineList',
  components: { PipelineEdit, PipelineRun },
  data() {
    return {
      pipelineStatusMap,
      pipelineStatusColor,
      loading: false,
      list: [],
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY, {
        '%name%': '',
        '%groupName%': '',
        status: undefined
      }),
      editVisible: false,
      editingId: '',
      editingName: '',
      runVisible: false,
      runningId: '',
      runningName: '',
      columns: [
        { title: this.$t('i18n_pipeline_name'), dataIndex: 'name', key: 'name', width: 170, ellipsis: true },
        { title: this.$t('i18n_829abe5a8d'), dataIndex: 'groupName', key: 'groupName', width: 110, ellipsis: true },
        { title: this.$t('i18n_3fea7ca76c'), dataIndex: 'status', key: 'status', width: 120, align: 'center' },
        // dataIndex 必须有值：CustomTable 的列设置以 dataIndex 作唯一标识，缺失会被并成一列
        { title: this.$t('i18n_pipeline_col_stage'), dataIndex: 'stageCount', key: 'stageCount', width: 80, align: 'center' },
        { title: this.$t('i18n_pipeline_col_source'), dataIndex: 'sourceCount', key: 'sourceCount', width: 90, align: 'center' },
        { title: this.$t('i18n_pipeline_run_total'), dataIndex: 'runNumber', key: 'runNumber', width: 80, align: 'center' },
        { title: this.$t('i18n_pipeline_update_time'), dataIndex: 'modifyTimeMillis', key: 'modifyTimeMillis', width: 155 },
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
    },
    statusOptions() {
      const list = []
      Object.keys(this.pipelineStatusMap).forEach((key) => {
        list.push({ value: Number(key), label: this.pipelineStatusMap[key] })
      })
      return list
    }
  },
  mounted() {
    this.loadData()
  },
  methods: {
    parseTime,
    handleTableChange(pagination) {
      this.listQuery = CHANGE_PAGE(this.listQuery, { pagination })
      this.loadData()
    },
    handleSearch() {
      this.listQuery.page = 1
      this.loadData()
    },
    loadData() {
      this.loading = true
      if (!this.listQuery.order_field) {
        // 与在线构建列表一致的默认排序：最近修改在前（表里只有 modifyTimeMillis，没有 modifyTime）
        this.listQuery.order_field = 'modifyTimeMillis'
        this.listQuery.order = 'descend'
      }
      getPipelineList(this.listQuery)
        .then((res) => {
          if (res.code === 200) {
            this.list = (res.data && res.data.result) || []
            this.listQuery.total = (res.data && res.data.total) || 0
          } else {
            message.warn({ message: res.msg })
          }
        })
        .finally(() => {
          this.loading = false
        })
    },
    stageCount(record) {
      try {
        return (JSON.parse(record.stagesJson || '[]') || []).length
      } catch (e) {
        return 0
      }
    },
    sourceCount(record) {
      try {
        return (JSON.parse(record.sourcesJson || '[]') || []).length
      } catch (e) {
        return 0
      }
    },
    isRunning(record) {
      return [1, 4, 9, 11].indexOf(record.status) > -1
    },
    handleAdd() {
      this.editingId = ''
      this.editingName = ''
      this.editVisible = true
    },
    handleEdit(record) {
      this.editingId = record.id
      this.editingName = record.name
      this.editVisible = true
    },
    handleEditClose() {
      this.editVisible = false
      this.editingId = ''
      this.editingName = ''
    },
    handleSaved() {
      this.loadData()
    },
    handleRuns(record) {
      this.runningId = record.id
      this.runningName = record.name
      this.runVisible = true
    },
    handleRun(record) {
      runPipeline({ id: record.id }).then((res) => {
        if (res.code === 200) {
          message.success({ message: this.$t('i18n_pipeline_run_submitted') })
          this.handleRuns(record)
          this.loadData()
        } else {
          message.warn({ message: res.msg })
        }
      })
    },
    handleCancel(record) {
      cancelPipeline({ id: record.id }).then((res) => {
        if (res.code === 200) {
          // 后端已按三态（已受理/已在取消中/未找到执行体）返回可读文案，直接展示
          message.info({ message: res.data || this.$t('i18n_pipeline_cancel_requested') })
        } else {
          message.warn({ message: res.msg })
        }
        this.loadData()
      })
    },
    handleDelete(record) {
      deletePipeline({ id: record.id }).then((res) => {
        if (res.code === 200) {
          message.success({ message: res.msg })
          this.loadData()
        } else {
          message.warn({ message: res.msg })
        }
      })
    }
  }
}
</script>

<style scoped>
.ant-dropdown-link {
  white-space: nowrap;
}
</style>
