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
      :scroll="{
        x: 'max-content'
      }"
    >
      <template v-slot:title>
        <a-space>
          <a-input
            allowClear
            class="search-input-item"
            @pressEnter="loadData"
            v-model:value="listQuery['%buildName%']"
            placeholder="构建名称"
          />
          <a-select
            show-search
            option-filter-prop="children"
            v-model:value="listQuery.status"
            allowClear
            placeholder="请选择状态"
            class="search-input-item"
          >
            <a-select-option v-for="(val, key) in statusMap" :key="key">{{ val }}</a-select-option>
          </a-select>
          <a-select
            show-search
            option-filter-prop="children"
            v-model:value="listQuery.triggerBuildType"
            allowClear
            placeholder="请选择触发类型"
            class="search-input-item"
          >
            <a-select-option v-for="(val, key) in triggerBuildTypeMap" :key="key">{{ val }}</a-select-option>
          </a-select>
          <a-range-picker :show-time="{ format: 'HH:mm:ss' }" format="YYYY-MM-DD HH:mm:ss" @change="onchangeTime" />
          <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
            <a-button type="primary" :loading="loading" @click="loadData">搜索</a-button>
          </a-tooltip>
          <a-button
            type="primary"
            danger
            :disabled="!tableSelections || tableSelections.length <= 0"
            @click="handleBatchDelete"
          >
            批量删除
          </a-button>
          <a-tooltip>
            <template v-slot:title>
              <div>构建历史是用于记录每次构建的信息,可以保留构建产物信息,构建日志。同时还可以快速回滚发布</div>
              <div>如果不需要保留较多构建历史信息可以到服务端修改构建相关配置参数</div>
              <div>构建历史可能占有较多硬盘空间,建议根据实际情况配置保留个数</div>
            </template>
            <QuestionCircleOutlined />
          </a-tooltip>
        </a-space>
      </template>
      <template #bodyCell="{ column, text, record, index }">
        <template v-if="column.tooltip">
          <a-tooltip :title="text">
            <span>{{ (text || '').slice(0, 10) }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'buildNumberId'">
          <a-tooltip :title="text + ' ( 点击查看日志 ) '">
            <a-tag color="#108ee9" @click="handleBuildLog(record)">
              #{{ text }}<template v-if="record.fromBuildNumberId">&lt;-{{ record.fromBuildNumberId }}</template>
            </a-tag>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'status'">
          <a-tooltip :title="record.statusMsg || statusMap[text] || '未知'">
            <a-tag :color="statusColor[record.status]">{{ statusMap[text] || '未知' }}</a-tag>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'releaseMethod'">
          <a-tooltip :title="releaseMethodMap[text]">
            <span>{{ releaseMethodMap[text] }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'triggerBuildType'">
          <a-tooltip :title="triggerBuildTypeMap[text]">
            <span>{{ triggerBuildTypeMap[text] }}</span>
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'resultFileSize'">
          <a-tooltip
            :title="`产物文件大小：${renderSize(record.resultFileSize)}， 日志文件： ${renderSize(
              record.buildLogFileSize
            )}`"
          >
            <span v-if="record.resultFileSize">{{ renderSize(record.resultFileSize) }}</span>
            <span v-else-if="record.buildLogFileSize">{{ renderSize(record.buildLogFileSize) }}</span>
            <span v-else>-</span>
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'endTime'">
          <a-tooltip
            :title="`开始时间：${parseTime(record.startTime)}，${
              record.endTime ? '结束时间：' + parseTime(record.endTime) : ''
            }`"
          >
            <span v-if="record.endTime">{{
              formatDuration((record.endTime || 0) - (record.startTime || 0), '', 2)
            }}</span>
            <span v-else>-</span>
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <a-tooltip title="下载构建日志,如果按钮不可用表示日志文件不存在,一般是构建历史相关文件被删除">
              <a-button size="small" type="primary" :disabled="!record.hasLog" @click="handleDownload(record)"
                ><DownloadOutlined />日志</a-button
              >
            </a-tooltip>

            <a-tooltip
              title="下载构建产物,如果按钮不可用表示产物文件不存在,一般是构建没有产生对应的文件或者构建历史相关文件被删除"
            >
              <a-button size="small" type="primary" :disabled="!record.hasFile" @click="handleFile(record)"
                ><DownloadOutlined />
                产物
              </a-button>
            </a-tooltip>

            <a-dropdown>
              <a @click="(e) => e.preventDefault()">
                更多
                <DownOutlined />
              </a>
              <template v-slot:overlay>
                <a-menu>
                  <a-menu-item>
                    <template v-if="record.releaseMethod !== 5">
                      <a-button
                        size="small"
                        :disabled="!record.hasFile || record.releaseMethod === 0"
                        type="primary"
                        danger
                        @click="handleRollback(record)"
                        >回滚
                      </a-button>
                    </template>
                    <template v-else>
                      <a-tooltip title="Dockerfile 构建方式不支持在这里回滚">
                        <a-button size="small" :disabled="true" type="primary" danger>回滚 </a-button>
                      </a-tooltip>
                    </template>
                  </a-menu-item>
                  <a-menu-item>
                    <a-button size="small" type="primary" danger @click="handleDelete(record)">删除</a-button>
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
        </template>
      </template>
    </a-table>
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
    <!-- 选择确认区域 -->
    <div style="padding-top: 50px" v-if="this.choose">
      <div
        :style="{
          position: 'absolute',
          right: 0,
          bottom: 0,
          width: '100%',
          borderTop: '1px solid #e9e9e9',
          padding: '10px 16px',
          background: '#fff',
          textAlign: 'right',
          zIndex: 1
        }"
      >
        <a-space>
          <a-button
            @click="
              () => {
                this.$emit('cancel')
              }
            "
          >
            取消
          </a-button>
          <a-button type="primary" @click="handerConfirm"> 确定 </a-button>
        </a-space>
      </div>
    </div>
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
  props: {
    choose: {
      type: String,
      default: ''
    },
    buildId: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      releaseMethodMap,
      triggerBuildTypeMap,
      loading: false,
      list: [],

      total: 0,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      statusMap,
      statusColor,
      temp: {},
      buildLogVisible: 0,
      tableSelections: [],
      columns: [
        {
          title: '构建名称',
          dataIndex: 'buildName',
          width: 120,
          ellipsis: true,
          tooltip: true
        },
        {
          title: '构建 ID',
          dataIndex: 'buildNumberId',
          width: '90px',
          align: 'center',
          ellipsis: true
        },
        {
          title: '备注',
          dataIndex: 'buildRemark',
          width: 120,
          ellipsis: true,
          tooltip: true
        },

        {
          title: '状态',
          dataIndex: 'status',
          width: '100px',
          align: 'center',
          ellipsis: true
        },
        {
          title: '触发类型',
          dataIndex: 'triggerBuildType',
          align: 'center',
          width: '100px',
          ellipsis: true
        },
        {
          title: '占用空间',
          dataIndex: 'resultFileSize',
          width: '100px',
          sorter: true,
          ellipsis: true
        },
        {
          title: '开始时间',
          dataIndex: 'startTime',
          sorter: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: '耗时',
          dataIndex: 'endTime',
          // sorter: true,

          width: '120px'
        },
        {
          title: '数据更新时间',
          dataIndex: 'modifyTimeMillis',
          sorter: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: '发布方式',
          dataIndex: 'releaseMethod',
          width: '100px',
          ellipsis: true
        },
        {
          title: '操作人',
          dataIndex: 'modifyUser',
          width: '130px',
          ellipsis: true,
          tooltip: true
        },
        {
          title: '操作',
          dataIndex: 'operation',

          width: '220px',
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
        selectedRowKeys: this.tableSelections,
        type: this.choose || 'checkbox'
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
      this.buildId && (this.listQuery.buildDataId = this.buildId)
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
      this.$confirm({
        title: '系统提示',
        zIndex: 1009,
        content: '真的要回滚该构建历史记录么？',
        okText: '确认',
        cancelText: '取消',
        onOk: () => {
          // 重新发布
          rollback(record.id).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg
              })
              this.loadData()
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
    },
    // 删除
    handleDelete(record) {
      this.$confirm({
        title: '系统提示',
        zIndex: 1009,
        content: '真的要删除构建历史记录么？',
        okText: '确认',
        cancelText: '取消',
        onOk: () => {
          // 删除
          deleteBuildHistory(record.id).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
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
        this.$notification.warning({
          message: '没有选择任何数据'
        })
        return
      }
      this.$confirm({
        title: '系统提示',
        zIndex: 1009,
        content: '真的要删除这些构建历史记录么？',
        okText: '确认',
        cancelText: '取消',
        onOk: () => {
          // 删除
          deleteBuildHistory(this.tableSelections.join(',')).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
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
      this.buildLogVisible = new Date() * Math.random()
    },
    // 关闭日志对话框
    closeBuildLogModel() {
      this.loadData()
    },
    // 多选相关
    tableSelectionChange(selectedRowKeys) {
      this.tableSelections = selectedRowKeys
    },
    // 选择确认
    handerConfirm() {
      if (!this.tableSelections.length) {
        this.$notification.warning({
          message: '请选择要使用的构建'
        })
        return
      }
      const selectData = this.list
        .filter((item) => {
          return this.tableSelections.indexOf(item.id) > -1
        })
        .filter((item) => {
          return item.hasFile
        })
        .map((item) => {
          return item.buildNumberId
        })
      if (!selectData.length) {
        this.$notification.warning({
          message: '选择的构建历史产物已经不存在啦'
        })
        return
      }
      this.$emit('confirm', selectData)
    }
  },
  emits: ['cancel', 'confirm']
}
</script>
