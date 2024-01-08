<template>
  <div>
    <!-- 数据表格 -->
    <a-table
      size="middle"
      :data-source="list"
      :columns="columns"
      :pagination="pagination"
      @change="changePage"
      bordered
      :scroll="{
        x: 'max-content'
      }"
    >
      <template v-slot:title>
        <a-space>
          <a-select v-model:value="listQuery.nodeId" allowClear placeholder="请选择节点" class="search-input-item">
            <a-select-option v-for="node in nodeList" :key="node.id">{{ node.name }}</a-select-option>
          </a-select>
          <a-select v-model:value="listQuery.outGivingId" allowClear placeholder="分发项目" class="search-input-item">
            <a-select-option v-for="dispatch in dispatchList" :key="dispatch.id">{{ dispatch.name }}</a-select-option>
          </a-select>
          <a-select v-model:value="listQuery.status" allowClear placeholder="请选择状态" class="search-input-item">
            <a-select-option v-for="(item, key) in dispatchStatusMap" :key="key" :value="key">{{
              item
            }}</a-select-option>
          </a-select>
          <a-range-picker
            class="search-input-item"
            :show-time="{ format: 'HH:mm:ss' }"
            format="YYYY-MM-DD HH:mm:ss"
            @change="onchangeTime"
          />
          <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
            <a-button :loading="loading" type="primary" @click="loadData">搜索</a-button>
          </a-tooltip>
        </a-space>
      </template>
      <template #bodyCell="{ column, text, record, index }">
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
          <a-tooltip placement="topLeft" :title="`${dispatchMode[text] || ''}  关联数据：${record.modeData || ''}`">
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
          <a-tag v-if="text === 2" color="green">{{ dispatchStatusMap[text] || '未知' }}</a-tag>
          <a-tag v-else-if="text === 1 || text === 0 || text === 5" color="orange">{{
            dispatchStatusMap[text] || '未知'
          }}</a-tag>
          <a-tag v-else-if="text === 3 || text === 4 || text === 6" color="red">{{
            dispatchStatusMap[text] || '未知'
          }}</a-tag>
          <a-tag v-else>{{ dispatchStatusMap[text] || '未知' }}</a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'operation'">
          <a-button type="primary" size="small" @click="handleDetail(record)">详情</a-button>
        </template>
      </template>
    </a-table>
    <!-- 详情区 -->
    <a-modal destroyOnClose v-model:open="detailVisible" width="600px" title="详情信息" :footer="null">
      <a-list item-layout="horizontal" :data-source="detailData">
        <template #renderItem="{ item }">
          <a-list-item>
            <a-list-item-meta :description="item.description">
              <template v-slot:title>
                <h4>{{ item.title }}</h4>
              </template>
            </a-list-item-meta>
          </a-list-item>
        </template>
      </a-list>
    </a-modal>
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
          title: '分发项目 ID',
          dataIndex: 'outGivingId',
          width: 100,
          ellipsis: true
        },

        {
          title: '节点名称',
          dataIndex: 'nodeName',
          ellipsis: true,
          width: 150
        },
        {
          title: '项目 ID',
          dataIndex: 'projectId',
          ellipsis: true,
          width: 100
        },
        {
          title: '分发方式',
          dataIndex: 'mode',
          ellipsis: true,
          width: '100px'
        },
        {
          title: '分发结果',
          dataIndex: 'outGivingResultMsg',
          ellipsis: true,
          width: 200
        },
        {
          title: '分发状态消息',
          dataIndex: 'outGivingResultMsgData',
          ellipsis: true,
          width: 100
        },
        {
          title: '分发耗时',
          dataIndex: 'outGivingResultTime',
          width: '120px'
        },
        {
          title: '文件大小',
          dataIndex: 'outGivingResultSize',
          width: '100px'
        },
        {
          title: '开始时间',
          dataIndex: 'startTime',
          customRender: ({ text }) => {
            return parseTime(text)
          },
          sorter: true,
          width: '170px'
        },
        {
          title: '结束时间',
          dataIndex: 'endTime',
          sorter: true,
          customRender: ({ text }) => {
            return parseTime(text)
          },
          width: '170px'
        },
        {
          title: '操作人',
          dataIndex: 'modifyUser',
          ellipsis: true,

          width: 120
        },
        {
          title: '状态',
          dataIndex: 'status',
          width: 100,
          ellipsis: true,
          fixed: 'right'
        },
        { title: '操作', dataIndex: 'operation', align: 'center', width: '100px', fixed: 'right' }
      ]
    }
  },
  computed: {
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery)
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
      this.detailData.push({ title: '分发结果', description: this.temp.result })
    },
    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter })
      this.loadData()
    }
  }
}
</script>
