<template>
  <div class="full-content">
    <!-- 数据表格 -->
    <a-table
      :data-source="list"
      size="middle"
      :columns="columns"
      :pagination="pagination"
      bordered
      rowKey="id"
      @change="change"
      :row-selection="rowSelection"
    >
      <template #title>
        <a-space>
          <a-input
            allowClear
            class="search-input-item"
            @pressEnter="loadData"
            v-model="listQuery['%buildName%']"
            placeholder="构建名称"
          />
          <a-select
            show-search
            option-filter-prop="children"
            v-model="listQuery.status"
            allowClear
            placeholder="请选择状态"
            class="search-input-item"
          >
            <a-select-option v-for="(val, key) in statusMap" :key="key">{{ val }}</a-select-option>
          </a-select>
          <a-select
            show-search
            option-filter-prop="children"
            v-model="listQuery.triggerBuildType"
            allowClear
            placeholder="请选择触发类型"
            class="search-input-item"
          >
            <a-select-option v-for="(val, key) in triggerBuildTypeMap" :key="key">{{ val }}</a-select-option>
          </a-select>
          <a-range-picker
            class="search-input-item"
            :show-time="{ format: 'HH:mm:ss' }"
            format="YYYY-MM-DD HH:mm:ss"
            @change="onchangeTime"
          />
          <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
            <a-button type="primary" :loading="loading" @click="loadData">搜索</a-button>
          </a-tooltip>
          <a-button
            type="danger"
            :disabled="!tableSelections || tableSelections.length <= 0"
            @click="handleBatchDelete"
          >
            批量删除
          </a-button>
          <a-tooltip>
            <template #title>
              <div>构建历史是用于记录每次构建的信息,可以保留构建产物信息,构建日志。同时还可以快速回滚发布</div>
              <div>如果不需要保留较多构建历史信息可以到服务端修改构建相关配置参数</div>
              <div>构建历史可能占有较多硬盘空间,建议根据实际情况配置保留个数</div>
            </template>
            <question-circle-filled />
          </a-tooltip>
        </a-space>
      </template>
      <a-tooltip #tooltip slot-scope="text" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-tooltip #buildNumberId slot-scope="text, record" :title="text + ' ( 点击查看日志 ) '">
        <a-tag color="#108ee9" @click="handleBuildLog(record)">#{{ text }}</a-tag>
      </a-tooltip>
      <a-tooltip #status slot-scope="text, item" :title="item.statusMsg || statusMap[text] || '未知'">
        <a-tag :color="statusColor[item.status]">{{ statusMap[text] || '未知' }}</a-tag>
      </a-tooltip>
      <a-tooltip #releaseMethod slot-scope="text" :title="releaseMethodMap[text]">
        <span>{{ releaseMethodMap[text] }}</span>
      </a-tooltip>
      <a-tooltip #triggerBuildType slot-scope="text" :title="triggerBuildTypeMap[text]">
        <span>{{ triggerBuildTypeMap[text] }}</span>
      </a-tooltip>

      <a-tooltip
        #resultFileSize
        slot-scope="text, record"
        :title="`产物文件大小：${renderSize(record.resultFileSize)}， 日志文件： ${renderSize(
          record.buildLogFileSize
        )}`"
      >
        <span>{{ renderSize(record.resultFileSize) }}</span>
        <!-- <div>{{ parseTime(record.endTime) }}</div> -->
      </a-tooltip>

      <a-tooltip
        #endTime
        slot-scope="text, record"
        :title="`开始时间：${parseTime(record.startTime)}，${
          record.endTime ? '结束时间：' + parseTime(record.endTime) : ''
        }`"
      >
        <span v-if="record.endTime">{{ formatDuration((record.endTime || 0) - (record.startTime || 0), '', 2) }}</span>
        <span v-else>-</span>
      </a-tooltip>

      <template #operation slot-scope="text, record">
        <a-space>
          <a-tooltip title="下载构建日志,如果按钮不可用表示日志文件不存在,一般是构建历史相关文件被删除">
            <a-button
              size="small"
              icon="download"
              type="primary"
              :disabled="!record.hasLog"
              @click="handleDownload(record)"
              >日志</a-button
            >
          </a-tooltip>

          <a-tooltip
            title="下载构建产物,如果按钮不可用表示产物文件不存在,一般是构建没有产生对应的文件或者构建历史相关文件被删除"
          >
            <a-button
              size="small"
              icon="download"
              type="primary"
              :disabled="!record.hasFile"
              @click="handleFile(record)"
            >
              产物
            </a-button>
          </a-tooltip>

          <a-dropdown>
            <a class="ant-dropdown-link" @click="(e) => e.preventDefault()">
              更多
              <down-outlined />
            </a>
            <a-menu #overlay>
              <a-menu-item>
                <template v-if="record.releaseMethod !== 5">
                  <a-button
                    size="small"
                    :disabled="!record.hasFile || record.releaseMethod === 0"
                    type="danger"
                    @click="handleRollback(record)"
                    >回滚
                  </a-button>
                </template>
                <template v-else>
                  <a-tooltip title="Dockerfile 构建方式不支持在这里回滚">
                    <a-button size="small" :disabled="true" type="danger">回滚 </a-button>
                  </a-tooltip>
                </template>
              </a-menu-item>
              <a-menu-item>
                <a-button size="small" type="danger" @click="handleDelete(record)">删除</a-button>
              </a-menu-item>
            </a-menu>
          </a-dropdown>
        </a-space>
      </template>
    </a-table>
    <!-- 构建日志 -->
    <a-modal
      destroyOnClose
      :width="'80vw'"
      v-model="buildLogVisible"
      title="构建日志"
      :footer="null"
      :maskClosable="false"
      @cancel="closeBuildLogModel"
    >
      <build-log v-if="buildLogVisible" :temp="temp" />
    </a-modal>
  </div>
</template>
<script>
import BuildLog from './log'
import {
  deleteBuildHistory,
  downloadBuildFile,
  downloadBuildLog,
  geteBuildHistory,
  releaseMethodMap,
  rollback,
  statusMap,
  statusColor,
  triggerBuildTypeMap
} from '@/api/build-info'
import {
  CHANGE_PAGE,
  COMPUTED_PAGINATION,
  PAGE_DEFAULT_LIST_QUERY,
  formatDuration,
  parseTime,
  renderSize
} from '@/utils/const'

export default {
  components: {
    BuildLog
  },
  data() {
    return {
      releaseMethodMap: releaseMethodMap,
      triggerBuildTypeMap: triggerBuildTypeMap,
      loading: false,
      list: [],

      total: 0,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      statusMap,
      statusColor,
      temp: {},
      buildLogVisible: false,
      tableSelections: [],
      columns: [
        {
          title: '构建名称',
          dataIndex: 'buildName',
          width: 120,
          ellipsis: true,
          scopedSlots: { customRender: 'tooltip' }
        },
        {
          title: '构建 ID',
          dataIndex: 'buildNumberId',
          width: '90px',
          align: 'center',
          ellipsis: true,
          scopedSlots: { customRender: 'buildNumberId' }
        },
        {
          title: '备注',
          dataIndex: 'buildRemark',
          width: 120,
          ellipsis: true,
          scopedSlots: { customRender: 'tooltip' }
        },

        {
          title: '状态',
          dataIndex: 'status',
          width: '100px',
          align: 'center',
          ellipsis: true,
          scopedSlots: { customRender: 'status' }
        },
        {
          title: '触发类型',
          dataIndex: 'triggerBuildType',
          align: 'center',
          width: '100px',
          ellipsis: true,
          scopedSlots: { customRender: 'triggerBuildType' }
        },
        {
          title: '占用空间',
          dataIndex: 'resultFileSize',
          width: '100px',
          sorter: true,
          ellipsis: true,
          scopedSlots: { customRender: 'resultFileSize' }
        },
        {
          title: '开始时间',
          dataIndex: 'startTime',
          sorter: true,
          customRender: (text) => parseTime(text),
          width: '170px'
        },
        {
          title: '耗时',
          dataIndex: 'endTime',
          // sorter: true,
          scopedSlots: { customRender: 'endTime' },
          width: '120px'
        },
        {
          title: '数据更新时间',
          dataIndex: 'modifyTimeMillis',
          sorter: true,
          customRender: (text) => parseTime(text),
          width: '170px'
        },
        {
          title: '发布方式',
          dataIndex: 'releaseMethod',
          width: '100px',
          ellipsis: true,
          scopedSlots: { customRender: 'releaseMethod' }
        },
        {
          title: '构建人',
          dataIndex: 'modifyUser',
          width: '130px',
          ellipsis: true,
          scopedSlots: { customRender: 'modifyUser' }
        },
        {
          title: '操作',
          dataIndex: 'operation',
          scopedSlots: { customRender: 'operation' },
          width: '200px',
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
    rowSelection() {
      return {
        onChange: this.tableSelectionChange,
        selectedRowKeys: this.tableSelections
      }
    }
  },
  created() {
    // this.loadBuildList();
    this.loadData()
  },
  methods: {
    parseTime,
    renderSize,
    formatDuration,

    // 加载数据
    loadData(pointerEvent) {
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page

      this.loading = true

      geteBuildHistory(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result
          this.listQuery.total = res.data.total
        }
        this.loading = false
      })
    },
    // 分页、排序、筛选变化时触发
    change(pagination, filters, sorter) {
      this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter })
      this.loadData()
    },
    // 选择时间
    onchangeTime(value, dateString) {
      if (!dateString[0] || !dateString[1]) {
        this.listQuery.startTime = ''
      } else {
        this.listQuery.startTime = `${dateString[0]} ~ ${dateString[1]}`
      }
    },

    // 下载构建日志
    handleDownload(record) {
      window.open(downloadBuildLog(record.id), '_blank')
    },

    // 下载构建产物
    handleFile(record) {
      window.open(downloadBuildFile(record.id), '_blank')
    },

    // 回滚
    handleRollback(record) {
      $confirm({
        title: '系统提示',
        content: '真的要回滚该构建历史记录么？',
        okText: '确认',
        cancelText: '取消',
        onOk: () => {
          // 重新发布
          rollback(record.id).then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              this.loadData()
            }
          })
        }
      })
    },
    // 删除
    handleDelete(record) {
      $confirm({
        title: '系统提示',
        content: '真的要删除构建历史记录么？',
        okText: '确认',
        cancelText: '取消',
        onOk: () => {
          // 删除
          deleteBuildHistory(record.id).then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              this.loadData()
            }
          })
        }
      })
    },
    // 批量删除
    handleBatchDelete() {
      if (!this.tableSelections || this.tableSelections.length <= 0) {
        $notification.warning({
          message: '没有选择任何数据'
        })
        return
      }
      $confirm({
        title: '系统提示',
        content: '真的要删除这些构建历史记录么？',
        okText: '确认',
        cancelText: '取消',
        onOk: () => {
          // 删除
          deleteBuildHistory(this.tableSelections.join(',')).then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              this.tableSelections = []
              this.loadData()
            }
          })
        }
      })
    },
    // 查看构建日志
    handleBuildLog(record) {
      this.temp = {
        id: record.buildDataId,
        buildId: record.buildNumberId
      }
      this.buildLogVisible = true
    },
    // 关闭日志对话框
    closeBuildLogModel() {
      this.loadData()
    },
    // 多选相关
    tableSelectionChange(selectedRowKeys) {
      this.tableSelections = selectedRowKeys
    }
  }
}
</script>
<style scoped></style>
