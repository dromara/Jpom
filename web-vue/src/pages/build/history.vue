<template>
  <div class="">
    <!-- 数据表格 -->
    <CustomTable
      is-show-tools
      default-auto-refresh
      :auto-refresh-time="30"
      :active-page="activePage"
      table-name="build-history-list"
      :empty-description="$t('pages.build.history.ddecb959')"
      :data-source="list"
      size="middle"
      :columns="columns"
      :pagination="pagination"
      bordered
      row-key="id"
      :row-selection="rowSelection"
      :scroll="{
        x: 'max-content'
      }"
      @change="change"
      @refresh="loadData"
    >
      <template #title>
        <a-space wrap class="search-box">
          <a-input
            v-model:value="listQuery['%buildName%']"
            allow-clear
            class="search-input-item"
            :placeholder="$t('pages.build.history.b7ff5dde')"
            @press-enter="loadData"
          />
          <a-select
            v-model:value="listQuery.status"
            show-search
            :filter-option="
              (input, option) => {
                const children = option.children && option.children()
                return (
                  children &&
                  children[0].children &&
                  children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
                )
              }
            "
            allow-clear
            :placeholder="$t('pages.build.history.56195645')"
            class="search-input-item"
          >
            <a-select-option v-for="(val, key) in statusMap" :key="key">{{ val }}</a-select-option>
          </a-select>
          <a-select
            v-model:value="listQuery.triggerBuildType"
            show-search
            :filter-option="
              (input, option) => {
                const children = option.children && option.children()
                return (
                  children &&
                  children[0].children &&
                  children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
                )
              }
            "
            allow-clear
            :placeholder="$t('pages.build.history.81fc1ba6')"
            class="search-input-item"
          >
            <a-select-option v-for="(val, key) in triggerBuildTypeMap" :key="key">{{ val }}</a-select-option>
          </a-select>
          <a-range-picker :show-time="{ format: 'HH:mm:ss' }" format="YYYY-MM-DD HH:mm:ss" @change="onchangeTime" />
          <a-tooltip :title="$t('pages.build.history.767472f6')">
            <a-button type="primary" :loading="loading" @click="loadData">{{
              $t('pages.build.history.53c2763c')
            }}</a-button>
          </a-tooltip>
          <a-button
            type="primary"
            danger
            :disabled="!tableSelections || tableSelections.length <= 0"
            @click="handleBatchDelete"
          >
            {{ $t('pages.build.history.b5139d46') }}
          </a-button>
        </a-space>
      </template>
      <template #tableHelp>
        <a-tooltip>
          <template #title>
            <div>{{ $t('pages.build.history.37009c8e') }}</div>
            <div>{{ $t('pages.build.history.39bcbda8') }}</div>
            <div>{{ $t('pages.build.history.652264a8') }}</div>
          </template>
          <QuestionCircleOutlined />
        </a-tooltip>
      </template>
      <template #tableBodyCell="{ column, text, record }">
        <template v-if="column.tooltip">
          <a-tooltip :title="text">
            <span>{{ text || '' }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'buildNumberId'">
          <a-tooltip :title="text + `( ${$t('pages.build.history.b51c8bb3')} )`">
            <a-tag color="#108ee9" @click="handleBuildLog(record)">
              #{{ text }}<template v-if="record.fromBuildNumberId">&lt;-{{ record.fromBuildNumberId }}</template>
            </a-tag>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'status'">
          <a-tooltip :title="record.statusMsg || statusMap[text] || $t('pages.build.history.5f51a112')">
            <a-tag :color="statusColor[record.status]">{{
              statusMap[text] || $t('pages.build.history.5f51a112')
            }}</a-tag>
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
            :title="`${$t('pages.build.history.ea860618')}${renderSize(record.resultFileSize)}， ${$t(
              'pages.build.history.6919d16e'
            )} ${renderSize(record.buildLogFileSize)}`"
          >
            <span v-if="record.resultFileSize">{{ renderSize(record.resultFileSize) }}</span>
            <span v-else-if="record.buildLogFileSize">{{ renderSize(record.buildLogFileSize) }}</span>
            <span v-else>-</span>
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'endTime'">
          <a-tooltip
            :title="`${$t('pages.build.history.ac9ef5f5')}${parseTime(record.startTime)}，${
              record.endTime ? $t('pages.build.history.9376939d') + parseTime(record.endTime) : ''
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
            <a-tooltip :title="$t('pages.build.history.3853c6b2')">
              <a-button size="small" type="primary" :disabled="!record.hasLog" @click="handleDownload(record)"
                ><DownloadOutlined />{{ $t('pages.build.history.f637e08') }}</a-button
              >
            </a-tooltip>

            <a-tooltip :title="$t('pages.build.history.44ea38fe')">
              <a-button size="small" type="primary" :disabled="!record.hasFile" @click="handleFile(record)"
                ><DownloadOutlined />
                {{ $t('pages.build.history.72370b9a') }}
              </a-button>
            </a-tooltip>

            <a-dropdown>
              <a @click="(e) => e.preventDefault()">
                {{ $t('pages.build.history.6e071067') }}
                <DownOutlined />
              </a>
              <template #overlay>
                <a-menu>
                  <a-menu-item>
                    <template v-if="record.releaseMethod !== 5">
                      <a-button
                        size="small"
                        :disabled="!record.hasFile || record.releaseMethod === 0"
                        type="primary"
                        danger
                        @click="handleRollback(record)"
                        >{{ $t('pages.build.history.6f9c6f93') }}
                      </a-button>
                    </template>
                    <template v-else>
                      <a-tooltip :title="$t('pages.build.history.b57dad8c')">
                        <a-button size="small" :disabled="true" type="primary" danger
                          >{{ $t('pages.build.history.6f9c6f93') }}
                        </a-button>
                      </a-tooltip>
                    </template>
                  </a-menu-item>
                  <a-menu-item>
                    <a-button size="small" type="primary" danger @click="handleDelete(record)">{{
                      $t('pages.build.history.dd20d11c')
                    }}</a-button>
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
        </template>
      </template>
    </CustomTable>
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
    <!-- 选择确认区域
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
    </div> -->
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
  emits: ['cancel', 'confirm'],
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
          title: this.$t('pages.build.history.b7ff5dde'),
          dataIndex: 'buildName',
          width: 120,
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('pages.build.history.7cf809d7'),
          dataIndex: 'buildNumberId',
          width: '90px',
          align: 'center',
          ellipsis: true
        },
        {
          title: this.$t('pages.build.history.2ae8180f'),
          dataIndex: 'buildRemark',
          width: 120,
          ellipsis: true,
          tooltip: true
        },

        {
          title: this.$t('pages.build.history.9c32c887'),
          dataIndex: 'status',
          width: '100px',
          align: 'center',
          ellipsis: true
        },
        {
          title: this.$t('pages.build.history.4abba04f'),
          dataIndex: 'triggerBuildType',
          align: 'center',
          width: '100px',
          ellipsis: true
        },
        {
          title: this.$t('pages.build.history.c2a7a92f'),
          dataIndex: 'resultFileSize',
          width: '100px',
          sorter: true,
          ellipsis: true
        },
        {
          title: this.$t('pages.build.history.7a013409'),
          dataIndex: 'startTime',
          sorter: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$t('pages.build.history.bc8deb76'),
          dataIndex: 'endTime',
          // sorter: true,

          width: '120px'
        },
        {
          title: this.$t('pages.build.history.5f141679'),
          dataIndex: 'modifyTimeMillis',
          sorter: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$t('pages.build.history.907f888f'),
          dataIndex: 'releaseMethod',
          width: '100px',
          ellipsis: true
        },
        {
          title: this.$t('pages.build.history.ed74cc37'),
          dataIndex: 'modifyUser',
          width: '130px',
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('pages.build.history.3bb962bf'),
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
    activePage() {
      return this.$attrs.routerUrl === this.$route.path
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
    $tl(key, ...args) {
      return this.$t(`pages.build.history.${key}`, ...args)
    },
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
      $confirm({
        title: this.$t('pages.build.history.1da61c06'),
        zIndex: 1009,
        content: this.$t('pages.build.history.511ca141'),
        okText: this.$t('pages.build.history.7da4a591'),
        cancelText: this.$t('pages.build.history.43105e21'),
        onOk: () => {
          // 重新发布
          return rollback(record.id).then((res) => {
            if (res.code === 200) {
              $notification.success({
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
      $confirm({
        title: this.$t('pages.build.history.1da61c06'),
        zIndex: 1009,
        content: this.$t('pages.build.history.14ac7e60'),
        okText: this.$t('pages.build.history.7da4a591'),
        cancelText: this.$t('pages.build.history.43105e21'),
        onOk: () => {
          return deleteBuildHistory(record.id).then((res) => {
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
          message: this.$t('pages.build.history.d1aa06b3')
        })
        return
      }
      $confirm({
        title: this.$t('pages.build.history.1da61c06'),
        zIndex: 1009,
        content: this.$t('pages.build.history.a938924a'),
        okText: this.$t('pages.build.history.7da4a591'),
        cancelText: this.$t('pages.build.history.43105e21'),
        onOk: () => {
          // 删除
          return deleteBuildHistory(this.tableSelections.join(',')).then((res) => {
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
        $notification.warning({
          message: this.$t('pages.build.history.a67c2624')
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
        $notification.warning({
          message: this.$t('pages.build.history.c53f077b')
        })
        return
      }
      this.$emit('confirm', selectData)
    }
  }
}
</script>
