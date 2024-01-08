<template>
  <div>
    <!-- 数据表格 -->
    <a-table
      :data-source="list"
      size="middle"
      :columns="columns"
      :pagination="pagination"
      bordered
      :scroll="{
        x: 'max-content'
      }"
      @change="change"
    >
      <template v-slot:title>
        <a-space>
          <a-select v-model:value="listQuery.nodeId" allowClear placeholder="请选择节点" class="search-input-item">
            <a-select-option v-for="(nodeName, key) in nodeMap" :key="key">{{ nodeName }}</a-select-option>
          </a-select>
          <a-select v-model:value="listQuery.status" allowClear placeholder="报警状态" class="search-input-item">
            <a-select-option :value="1">正常</a-select-option>
            <a-select-option :value="0">异常</a-select-option>
          </a-select>
          <a-select v-model:value="listQuery.notifyStatus" allowClear placeholder="通知状态" class="search-input-item">
            <a-select-option :value="1">成功</a-select-option>
            <a-select-option :value="0">失败</a-select-option>
          </a-select>
          <a-range-picker :show-time="{ format: 'HH:mm:ss' }" format="YYYY-MM-DD HH:mm:ss" @change="onchangeTime" />
          <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
            <a-button :loading="loading" type="primary" @click="loadData">搜索</a-button>
          </a-tooltip>
        </a-space>
      </template>
      <template #bodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'nodeId'">
          <a-tooltip placement="topLeft" :title="text">
            <span>{{ nodeMap[text] }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.tooltip">
          <a-tooltip placement="topLeft" :title="text">
            <span>{{ text }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'status'">
          <span>{{ text ? '正常' : '异常' }}</span>
        </template>
        <template v-else-if="column.dataIndex === 'notifyStyle'">
          {{ notifyStyle[text] || '未知' }}
        </template>
        <template v-else-if="column.dataIndex === 'notifyStatus'">
          <span>{{ text ? '成功' : '失败' }}</span>
        </template>
        <template v-else-if="column.dataIndex === 'operation'">
          <a-button size="small" type="primary" @click="handleDetail(record)">详情</a-button>
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
import { getMonitorLogList, notifyStyle } from '@/api/monitor'
import { getNodeListAll } from '@/api/node'
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'

export default {
  data() {
    return {
      loading: false,
      list: [],
      nodeMap: {},
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      temp: {},
      detailVisible: false,
      notifyStyle,
      detailData: [],
      columns: [
        {
          title: '报警标题',
          dataIndex: 'title',
          ellipsis: true,
          tooltip: true
        },
        {
          title: '节点名称',
          dataIndex: 'nodeId',
          width: 100,
          ellipsis: true
        },
        {
          title: '项目 ID',
          dataIndex: 'projectId',
          width: 100,
          ellipsis: true,
          tooltip: true
        },
        {
          title: '报警状态',
          dataIndex: 'status',
          width: 100,
          align: 'center',
          ellipsis: true
        },
        {
          title: '报警方式',
          dataIndex: 'notifyStyle',
          width: 100,
          align: 'center',
          ellipsis: true
        },
        {
          title: '报警时间',
          dataIndex: 'createTime',
          customRender: ({ text }) => {
            return parseTime(text)
          },
          width: '170px'
        },
        {
          title: '通知状态',
          dataIndex: 'notifyStatus',
          width: 100,
          ellipsis: true
        },
        {
          title: '操作',
          dataIndex: 'operation',
          align: 'center',
          fixed: 'right',
          width: '80px'
        }
      ]
    }
  },
  computed: {
    // 分页
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery)
    }
  },
  created() {
    this.loadNodeList(() => {
      this.loadData()
    })
  },
  methods: {
    // 加载 node
    loadNodeList(fn) {
      getNodeListAll().then((res) => {
        if (res.code === 200) {
          res.data.forEach((element) => {
            this.nodeMap[element.id] = element.name
          })
          fn && fn()
        }
      })
    },
    // 加载数据
    loadData(pointerEvent) {
      this.loading = true
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
      getMonitorLogList(this.listQuery).then((res) => {
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
      if (dateString[0]) {
        this.listQuery.createTimeMillis = `${dateString[0]} ~ ${dateString[1]}`
      } else {
        this.listQuery.createTimeMillis = ''
      }
    },

    // 查看详情
    handleDetail(record) {
      this.detailData = []
      this.detailVisible = true
      this.temp = Object.assign({}, record)
      this.detailData.push({ title: '标题', description: this.temp.title })
      this.detailData.push({ title: '内容', description: this.temp.content })
      this.detailData.push({
        title: '通知对象',
        description: this.temp.notifyObject
      })
      if (!this.temp.notifyStatus) {
        this.detailData.push({
          title: '通知异常',
          description: this.temp.notifyError
        })
      }
    }
  }
}
</script>
