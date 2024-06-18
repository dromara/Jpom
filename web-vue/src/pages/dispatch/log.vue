<template>
  <div>
    <!-- 数据表格 -->
    <CustomTable
      is-show-tools
      default-auto-refresh
      :auto-refresh-time="30"
      :active-page="activePage"
      table-name="dispatch-log-list"
      :empty-description="$t('i18n_8d1286cd2e')"
      size="middle"
      :data-source="list"
      :columns="columns"
      :pagination="pagination"
      bordered
      :scroll="{
        x: 'max-content'
      }"
      @change="changePage"
      @refresh="loadData"
    >
      <template #title>
        <a-space wrap class="search-box">
          <a-select
            v-model:value="listQuery.nodeId"
            allow-clear
            :placeholder="$t('i18n_f8a613d247')"
            class="search-input-item"
          >
            <a-select-option v-for="node in nodeList" :key="node.id">{{ node.name }}</a-select-option>
          </a-select>
          <a-select
            v-model:value="listQuery.outGivingId"
            allow-clear
            :placeholder="$t('i18n_bc8752e529')"
            class="search-input-item"
          >
            <a-select-option v-for="dispatch in dispatchList" :key="dispatch.id">{{ dispatch.name }}</a-select-option>
          </a-select>
          <a-select
            v-model:value="listQuery.status"
            allow-clear
            :placeholder="$t('i18n_e1c965efff')"
            class="search-input-item"
          >
            <a-select-option v-for="(item, key) in dispatchStatusMap" :key="key" :value="key">{{
              item
            }}</a-select-option>
          </a-select>
          <a-range-picker :show-time="{ format: 'HH:mm:ss' }" format="YYYY-MM-DD HH:mm:ss" @change="onchangeTime" />
          <a-tooltip :title="$t('i18n_4838a3bd20')">
            <a-button :loading="loading" type="primary" @click="loadData">{{ $t('i18n_e5f71fc31e') }}</a-button>
          </a-tooltip>
        </a-space>
      </template>
      <template #tableBodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'outGivingId'">
          <a-tooltip placement="topLeft" :title="text">
            <span>{{ text }}</span>
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'nodeName'">
          <a-tooltip
            placement="topLeft"
            :title="
              nodeList.filter((item) => item.id === record.nodeId) &&
              nodeList.filter((item) => item.id === record.nodeId)[0] &&
              nodeList.filter((item) => item.id === record.nodeId)[0].name
            "
          >
            <span>{{
              nodeList.filter((item) => item.id === record.nodeId) &&
              nodeList.filter((item) => item.id === record.nodeId)[0] &&
              nodeList.filter((item) => item.id === record.nodeId)[0].name
            }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'projectId'">
          <a-tooltip placement="topLeft" :title="text">
            <span>{{ text }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'mode'">
          <a-tooltip
            placement="topLeft"
            :title="`${dispatchMode[text] || ''}  ${$t('i18n_b04209e785')}${record.modeData || ''}`"
          >
            <span>{{ dispatchMode[text] || '' }}</span>
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'outGivingResultMsg'">
          <a-tooltip placement="topLeft" :title="readJsonStrField(record.result, 'msg')">
            <span
              >{{ readJsonStrField(record.result, 'code') }}-{{
                readJsonStrField(record.result, 'msg') || record.result
              }}</span
            >
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'outGivingResultTime'">
          <a-tooltip placement="topLeft" :title="readJsonStrField(record.result, 'upload_duration')">
            <span>{{ readJsonStrField(record.result, 'upload_duration') }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'outGivingResultSize'">
          <a-tooltip placement="topLeft" :title="readJsonStrField(record.result, 'upload_file_size')">
            {{ readJsonStrField(record.result, 'upload_file_size') }}
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'outGivingResultMsgData'">
          <a-tooltip placement="topLeft" :title="`${readJsonStrField(record.result, 'data')}`">
            <template v-if="record.fileSize">
              {{ Math.floor((record.progressSize / record.fileSize) * 100) }}%
            </template>
            {{ readJsonStrField(record.result, 'data') }}
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'status'">
          <!-- {{ dispatchStatusMap[text] || "未知" }} -->
          <a-tag v-if="text === 2" color="green">{{ dispatchStatusMap[text] || $t('i18n_1622dc9b6b') }}</a-tag>
          <a-tag v-else-if="text === 1 || text === 0 || text === 5" color="orange">{{
            dispatchStatusMap[text] || $t('i18n_1622dc9b6b')
          }}</a-tag>
          <a-tag v-else-if="text === 3 || text === 4 || text === 6" color="red">{{
            dispatchStatusMap[text] || $t('i18n_1622dc9b6b')
          }}</a-tag>
          <a-tag v-else>{{ dispatchStatusMap[text] || $t('i18n_1622dc9b6b') }}</a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'operation'">
          <a-button type="primary" size="small" @click="handleDetail(record)">{{ $t('i18n_f26225bde6') }}</a-button>
        </template>
      </template>
    </CustomTable>
    <!-- 详情区 -->
    <CustomModal
      v-if="detailVisible"
      v-model:open="detailVisible"
      destroy-on-close
      width="600px"
      :title="$t('i18n_3032257aa3')"
      :footer="null"
    >
      <a-list item-layout="horizontal" :data-source="detailData">
        <template #renderItem="{ item }">
          <a-list-item>
            <a-list-item-meta>
              <template #title>
                <h4>{{ item.title }}</h4>
              </template>
              <template #description>
                <code>{{ item.description }}</code>
              </template>
            </a-list-item-meta>
          </a-list-item>
        </template>
      </a-list>
    </CustomModal>
  </div>
</template>
<script>
import { getNodeListAll } from '@/api/node'
import { dispatchStatusMap, getDishPatchListAll, getDishPatchLogList, dispatchMode } from '@/api/dispatch'
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, readJsonStrField, parseTime } from '@/utils/const'

export default {
  data() {
    return {
      dispatchMode,
      loading: true,
      list: [],
      nodeList: [],
      dispatchList: [],
      total: 0,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      dispatchStatusMap: dispatchStatusMap,
      temp: {},
      detailVisible: false,
      detailData: [],
      columns: [
        {
          title: this.$t('i18n_b714160f52'),
          dataIndex: 'outGivingId',
          width: 100,
          ellipsis: true
        },

        {
          title: this.$t('i18n_b1785ef01e'),
          dataIndex: 'nodeName',
          ellipsis: true,
          width: 150
        },
        {
          title: this.$t('i18n_4fdd2213b5'),
          dataIndex: 'projectId',
          ellipsis: true,
          width: 100
        },
        {
          title: this.$t('i18n_174062da44'),
          dataIndex: 'mode',
          ellipsis: true,
          width: '100px'
        },
        {
          title: this.$t('i18n_0ef396cbcc'),
          dataIndex: 'outGivingResultMsg',
          ellipsis: true,
          width: 200
        },

        {
          title: this.$t('i18n_4cd49caae4'),
          dataIndex: 'outGivingResultTime',
          width: '120px'
        },
        {
          title: this.$t('i18n_396b7d3f91'),
          dataIndex: 'outGivingResultSize',
          width: '100px'
        },
        {
          title: this.$t('i18n_592c595891'),
          dataIndex: 'startTime',
          customRender: ({ text }) => {
            return parseTime(text)
          },
          sorter: true,
          width: '170px'
        },
        {
          title: this.$t('i18n_f782779e8b'),
          dataIndex: 'endTime',
          sorter: true,
          customRender: ({ text }) => {
            return parseTime(text)
          },
          width: '170px'
        },
        {
          title: this.$t('i18n_543de6ff04'),
          dataIndex: 'outGivingResultMsgData',
          ellipsis: true,
          width: 100
        },
        {
          title: this.$t('i18n_f9ac4b2aa6'),
          dataIndex: 'modifyUser',
          ellipsis: true,

          width: 120
        },
        {
          title: this.$t('i18n_3fea7ca76c'),
          dataIndex: 'status',
          width: 100,
          ellipsis: true,
          fixed: 'right'
        },
        {
          title: this.$t('i18n_2b6bc0f293'),
          dataIndex: 'operation',
          align: 'center',
          width: '100px',
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
    }
  },
  created() {
    this.handleFilter()
  },
  methods: {
    readJsonStrField,
    // 搜索
    handleFilter() {
      this.loadNodeList()
      this.loadDispatchList()
      this.loadData()
    },
    // 加载 node
    loadNodeList() {
      getNodeListAll().then((res) => {
        if (res.code === 200) {
          this.nodeList = res.data
        }
      })
    },
    // 加载分发项目
    loadDispatchList() {
      getDishPatchListAll().then((res) => {
        if (res.code === 200) {
          this.dispatchList = res.data
        }
      })
    },
    // 加载数据
    loadData(pointerEvent) {
      this.loading = true
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
      getDishPatchLogList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result
          this.listQuery.total = res.data.total
        }
        this.loading = false
      })
    },
    // 选择时间
    onchangeTime(value, dateString) {
      this.listQuery.createTimeMillis = `${dateString[0]} ~ ${dateString[1]}`
    },
    // 查看详情
    handleDetail(record) {
      this.detailData = []
      this.detailVisible = true
      this.temp = Object.assign({}, record)

      this.detailData.push({ title: this.$t('i18n_0ef396cbcc'), description: this.temp.result })
    },
    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter })
      this.loadData()
    }
  }
}
</script>
