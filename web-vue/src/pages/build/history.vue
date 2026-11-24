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
  <div class="">
    <!-- 数据表格 -->
    <CustomTable
      is-show-tools
      default-auto-refresh
      :auto-refresh-time="30"
      :active-page="activePage"
      table-name="build-history-list"
      :empty-description="$t('i18n_2b36926bc1')"
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
            :placeholder="$t('i18n_50a299c847')"
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
            :placeholder="$t('i18n_e1c965efff')"
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
            :placeholder="$t('i18n_9057ac9664')"
            class="search-input-item"
          >
            <a-select-option v-for="(val, key) in triggerBuildTypeMap" :key="key">{{ val }}</a-select-option>
          </a-select>
          <a-range-picker :show-time="{ format: 'HH:mm:ss' }" format="YYYY-MM-DD HH:mm:ss" @change="onchangeTime" />
          <a-tooltip :title="$t('i18n_4838a3bd20')">
            <a-button type="primary" :loading="loading" @click="loadData">{{ $t('i18n_e5f71fc31e') }}</a-button>
          </a-tooltip>
          <a-button
            type="primary"
            danger
            :disabled="!tableSelections || tableSelections.length <= 0"
            @click="handleBatchDelete"
          >
            {{ $t('i18n_7fb62b3011') }}
          </a-button>
        </a-space>
      </template>
      <template #tableHelp>
        <a-tooltip>
          <template #title>
            <div>{{ $t('i18n_005de9a4eb') }}</div>
            <div>{{ $t('i18n_9cd0554305') }}</div>
            <div>{{ $t('i18n_952232ca52') }}</div>
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
          <a-tooltip :title="text + `( ${$t('i18n_aac62bc255')} )`">
            <a-tag color="#108ee9" @click="handleBuildLog(record)">
              #{{ text }}<template v-if="record.fromBuildNumberId">&lt;-{{ record.fromBuildNumberId }}</template>
            </a-tag>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'status'">
          <a-tooltip :title="record.statusMsg || statusMap[text] || $t('i18n_1622dc9b6b')">
            <a-tag :color="statusColor[record.status]">{{ statusMap[text] || $t('i18n_1622dc9b6b') }}</a-tag>
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
        <template v-else-if="column.dataIndex === 'buildNodeName'">
          <a-tooltip placement="topLeft" :title="record.buildNodeName || $t('i18n_4d1636e43f')">
            <span>{{ record.buildNodeName || $t('i18n_4d1636e43f') }}</span>
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'resultFileSize'">
          <a-tooltip
            :title="`${$t('i18n_16646e46b1')}${renderSize(record.resultFileSize)}， ${$t(
              'i18n_77e501b44b'
            )} ${renderSize(record.buildLogFileSize)}`"
          >
            <span v-if="record.resultFileSize">{{ renderSize(record.resultFileSize) }}</span>
            <span v-else-if="record.buildLogFileSize">{{ renderSize(record.buildLogFileSize) }}</span>
            <span v-else>-</span>
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'endTime'">
          <a-tooltip
            :title="`${$t('i18n_61e84eb5bb')}${parseTime(record.startTime)}，${
              record.endTime ? $t('i18n_590dbb68cf') + parseTime(record.endTime) : ''
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
            <a-tooltip :title="$t('i18n_b38d7db9b0')">
              <a-button size="small" type="primary" :disabled="!record.hasLog" @click="handleDownload(record)"
                ><DownloadOutlined />{{ $t('i18n_456d29ef8b') }}</a-button
              >
            </a-tooltip>

            <a-tooltip :title="$t('i18n_02e35447d4')">
              <a-button size="small" type="primary" :disabled="!record.hasFile" @click="handleFile(record)"
                ><DownloadOutlined />
                {{ $t('i18n_7dfcab648d') }}
              </a-button>
            </a-tooltip>

            <a-dropdown>
              <a @click="(e) => e.preventDefault()">
                {{ $t('i18n_0ec9eaf9c3') }}
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
                        >{{ $t('i18n_61fc0c2a2c') }}
                      </a-button>
                    </template>
                    <template v-else>
                      <a-tooltip :title="$t('i18n_2d94b9cf0e')">
                        <a-button size="small" :disabled="true" type="primary" danger
                          >{{ $t('i18n_61fc0c2a2c') }}
                        </a-button>
                      </a-tooltip>
                    </template>
                  </a-menu-item>
                  <a-menu-item>
                    <a-tooltip :title="$t('i18n_1f63281a79')">
                      <a-button size="small" type="primary" @click="handleQuickRollback(record)"
                        >{{ $t('i18n_c845ff5941') }}
                      </a-button>
                    </a-tooltip>
                  </a-menu-item>
                  <a-menu-item>
                    <a-button size="small" type="primary" danger @click="handleDelete(record)">{{
                      $t('i18n_2f4aaddde3')
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
    <!-- 快速回滚：节点版本快照 -->
    <a-modal
      v-model:open="versionVisible"
      :title="$t('i18n_1f63281a79')"
      :width="'70vw'"
      :ok-text="$t('i18n_757d7164bb')"
      :ok-button-props="{ disabled: !selectedVersion }"
      :confirm-loading="versionLoading"
      @ok="handleSwitchVersion"
    >
      <a-alert
        v-if="versionReachable === false || versionMessage"
        :type="versionReachable === false ? 'error' : 'warning'"
        show-icon
        :message="versionMessage || $t('i18n_5b8e2d7a13')"
        style="margin-bottom: 12px"
      />
      <a-empty
        v-if="!versionLoading && versionList.length === 0"
        :description="versionTargetType === 'sshProject' ? $t('i18n_7c1d5a9e42') : $t('i18n_4807c5a15f')"
      />
      <a-table
        v-else
        size="small"
        bordered
        :data-source="versionList"
        :columns="versionColumns"
        :loading="versionLoading"
        :row-key="versionRowKey"
        :row-selection="versionRowSelection"
        :pagination="false"
        :scroll="{ x: 'max-content' }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'nodeId'">
            <a-tooltip :title="`${record.projectId} / ${record.nodeId}`">
              <span>{{ record.projectId }} / {{ record.nodeId }}</span>
            </a-tooltip>
          </template>
          <template v-else-if="column.dataIndex === 'projectId'">
            <a-tooltip :title="record.projectName || record.projectId">
              <span>{{ record.projectName || record.projectId }}</span>
            </a-tooltip>
          </template>
          <template v-else-if="column.dataIndex === 'status'">
            <a-tag :color="versionStatusColor(record.status)">{{ versionStatusText(record.status) }}</a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'exist'">
            <a-tooltip :title="versionBlockedReason(record)">
              <CheckOutlined v-if="record.exist !== false" style="color: #52c41a" />
              <CloseOutlined v-else style="color: #ff4d4f" />
            </a-tooltip>
          </template>
          <template v-else-if="column.dataIndex === 'fileSize'">
            <span>{{ renderSize(record.fileSize) }}</span>
          </template>
          <template v-else-if="column.dataIndex === 'createTime'">
            <span>{{ parseTime(record.createTime) }}</span>
          </template>
        </template>
      </a-table>
    </a-modal>
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
  getBuildVersionList,
  releaseMethodMap,
  rollback,
  rollbackBuildVersion,
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
      // 快速回滚：节点版本快照
      versionVisible: false,
      versionLoading: false,
      versionLogId: '',
      versionList: [],
      versionSelections: [],
      // 版本快照的目标类型：node（插件端项目） / sshProject（SSH 项目）
      versionTargetType: 'node',
      versionReachable: true,
      versionMessage: '',
      versionBaseColumns: [
        {
          title: this.$t('i18n_3bf3c0a8d6'),
          dataIndex: 'nodeId',
          width: '220px',
          ellipsis: true
        },
        {
          title: this.$t('i18n_31ecc0e65b'),
          dataIndex: 'projectId',
          width: '140px',
          ellipsis: true
        },
        {
          title: this.$t('i18n_fe2df04a16'),
          dataIndex: 'versionId',
          width: '160px',
          ellipsis: true
        },
        {
          title: this.$t('i18n_3fea7ca76c'),
          dataIndex: 'status',
          width: '100px',
          align: 'center'
        },
        {
          title: this.$t('i18n_6f28517a5c'),
          dataIndex: 'exist',
          width: '90px',
          align: 'center'
        },
        {
          title: this.$t('i18n_ad35f58fb3'),
          dataIndex: 'fileSize',
          width: '100px',
          align: 'center'
        },
        {
          title: this.$t('i18n_eca37cb072'),
          dataIndex: 'createTime',
          width: '170px'
        }
      ],
      columns: [
        {
          title: this.$t('i18n_50a299c847'),
          dataIndex: 'buildName',
          width: 120,
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('i18n_46e4265791'),
          dataIndex: 'buildNumberId',
          width: '90px',
          align: 'center',
          ellipsis: true
        },
        {
          title: this.$t('i18n_2432b57515'),
          dataIndex: 'buildRemark',
          width: 120,
          ellipsis: true,
          tooltip: true
        },

        {
          title: this.$t('i18n_3fea7ca76c'),
          dataIndex: 'status',
          width: '100px',
          align: 'center',
          ellipsis: true
        },
        {
          title: this.$t('i18n_ff9814bf6b'),
          dataIndex: 'triggerBuildType',
          align: 'center',
          width: '100px',
          ellipsis: true
        },
        {
          title: this.$t('i18n_1eb1c081f0'),
          dataIndex: 'buildNodeName',
          width: '120px',
          ellipsis: true
        },
        {
          title: this.$t('i18n_ad35f58fb3'),
          dataIndex: 'resultFileSize',
          width: '100px',
          sorter: true,
          ellipsis: true
        },
        {
          title: this.$t('i18n_592c595891'),
          dataIndex: 'startTime',
          sorter: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$t('i18n_39f1374d36'),
          dataIndex: 'endTime',
          // sorter: true,

          width: '120px'
        },
        {
          title: this.$t('i18n_af427d2541'),
          dataIndex: 'modifyTimeMillis',
          sorter: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$t('i18n_f98994f7ec'),
          dataIndex: 'releaseMethod',
          width: '100px',
          ellipsis: true
        },
        {
          title: this.$t('i18n_f9ac4b2aa6'),
          dataIndex: 'modifyUser',
          width: '130px',
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('i18n_2b6bc0f293'),
          dataIndex: 'operation',

          width: '1%',
          align: 'center',
          fixed: 'right'
        }
      ]
    }
  },
  computed: {
    // 版本快照列：SSH 项目没有节点维度，不展示「节点」列（项目列显示 SSH 项目名称）
    versionColumns() {
      if (this.versionTargetType === 'sshProject') {
        return this.versionBaseColumns.filter((column) => column.dataIndex !== 'nodeId')
      }
      return this.versionBaseColumns
    },
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
    },
    // 快速回滚：选中的版本快照
    selectedVersion() {
      const key = this.versionSelections && this.versionSelections[0]
      if (!key) {
        return null
      }
      return this.versionList.find((item) => this.versionRowKey(item) === key) || null
    },
    // 快速回滚：行选择（不可切换的行禁用勾选）
    versionRowSelection() {
      return {
        type: 'radio',
        selectedRowKeys: this.versionSelections,
        onChange: (selectedRowKeys) => {
          this.versionSelections = selectedRowKeys
        },
        getCheckboxProps: (record) => ({
          disabled: !this.versionSwitchable(record)
        })
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
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_fb61d4d708'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
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
    // 快速回滚：版本快照行 key（SSH 项目没有节点维度）
    versionRowKey(record) {
      return `${record.targetType || 'node'}_${record.projectId}_${record.nodeId || ''}_${record.versionId}`
    },
    // 快速回滚：状态文案
    versionStatusText(status) {
      const map = {
        1: this.$t('i18n_49799437d0'),
        2: this.$t('i18n_34601006f4'),
        3: this.$t('i18n_b7dad624f2')
      }
      return map[status] || '-'
    },
    // 快速回滚：状态颜色
    versionStatusColor(status) {
      const map = {
        1: 'blue',
        2: 'green',
        3: 'default'
      }
      return map[status] || 'default'
    },
    // 快速回滚：该版本是否可以切换
    versionSwitchable(record) {
      return record.exist !== false && record.reachable !== false
    },
    // 快速回滚：不可切换的原因
    versionBlockedReason(record) {
      if (record.reachable === false) {
        return this.$t('i18n_6e36069fab')
      }
      if (record.exist === false) {
        return this.$t('i18n_67c59b0ded')
      }
      return ''
    },
    // 快速回滚：打开版本快照弹窗
    handleQuickRollback(record) {
      this.versionLogId = record.id
      this.versionList = []
      this.versionSelections = []
      // SSH 项目的版本快照在目标机器目录上，列结构不同；发布方式 7 = SSH 项目
      this.versionTargetType = record.releaseMethod === 7 ? 'sshProject' : 'node'
      this.versionReachable = true
      this.versionMessage = ''
      this.versionVisible = true
      this.loadBuildVersionList()
    },
    // 快速回滚：加载节点版本快照
    loadBuildVersionList() {
      this.versionLoading = true
      getBuildVersionList({
        logId: this.versionLogId
      })
        .then((res) => {
          if (res.code === 200) {
            const data = res.data || {}
            this.versionList = data.list || []
            if (data.targetType) {
              this.versionTargetType = data.targetType
            }
            this.versionReachable = data.reachable !== false
            this.versionMessage = data.message || ''
          }
        })
        .finally(() => {
          this.versionLoading = false
        })
    },
    // 快速回滚：切换到选中版本
    handleSwitchVersion() {
      const versionRecord = this.selectedVersion
      if (!versionRecord) {
        return
      }
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1010,
        content: this.$t('i18n_45813c1bef', { v: versionRecord.versionId }),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          this.versionLoading = true
          return rollbackBuildVersion({
            logId: this.versionLogId,
            restart: true,
            // SSH 项目：按版本行定位项目（发布方式支持多选），插件端项目由后端按构建编号切换全部节点
            versionId: versionRecord.versionId,
            projectId: versionRecord.projectId
          })
            .then((res) => {
              if (res.code === 200) {
                $notification.success({
                  message: res.msg
                })
                this.versionVisible = false
                this.loadData()
              }
            })
            .finally(() => {
              this.versionLoading = false
            })
        }
      })
    },
    // 删除
    handleDelete(record) {
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_ad8b626496'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
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
          message: this.$t('i18n_5d817c403e')
        })
        return
      }
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_02d46f7e6f'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
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
          message: this.$t('i18n_2b4cf3d74e')
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
          message: this.$t('i18n_a637a42173')
        })
        return
      }
      this.$emit('confirm', selectData)
    }
  }
}
</script>
