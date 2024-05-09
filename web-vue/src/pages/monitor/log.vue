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
      <template #title>
        <a-space wrap class="search-box">
          <a-select
            v-model:value="listQuery.nodeId"
            allow-clear
            :placeholder="$tl('p.pleaseSelectNode')"
            class="search-input-item"
          >
            <a-select-option v-for="(nodeName, key) in nodeMap" :key="key">{{ nodeName }}</a-select-option>
          </a-select>
          <a-select
            v-model:value="listQuery.status"
            allow-clear
            :placeholder="$tl('c.alarmStatus')"
            class="search-input-item"
          >
            <a-select-option :value="1">{{ $tl('c.normal') }}</a-select-option>
            <a-select-option :value="0">{{ $tl('c.abnormal') }}</a-select-option>
          </a-select>
          <a-select
            v-model:value="listQuery.notifyStatus"
            allow-clear
            :placeholder="$tl('c.notificationStatus')"
            class="search-input-item"
          >
            <a-select-option :value="1">{{ $tl('c.success') }}</a-select-option>
            <a-select-option :value="0">{{ $tl('c.failure') }}</a-select-option>
          </a-select>
          <a-range-picker :show-time="{ format: 'HH:mm:ss' }" format="YYYY-MM-DD HH:mm:ss" @change="onchangeTime" />
          <a-tooltip :title="$tl('p.quickReturnToFirstPage')">
            <a-button :loading="loading" type="primary" @click="loadData">{{ $tl('p.search') }}</a-button>
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
          <span>{{ text ? $tl('c.normal') : $tl('c.abnormal') }}</span>
        </template>
        <template v-else-if="column.dataIndex === 'notifyStyle'">
          {{ notifyStyle[text] || $tl('p.unknown') }}
        </template>
        <template v-else-if="column.dataIndex === 'notifyStatus'">
          <span>{{ text ? $tl('c.success') : $tl('c.failure') }}</span>
        </template>
        <template v-else-if="column.dataIndex === 'operation'">
          <a-button size="small" type="primary" @click="handleDetail(record)">{{ $tl('p.details') }}</a-button>
        </template>
      </template>
    </a-table>
    <!-- 详情区 -->
    <a-modal v-model:open="detailVisible" destroy-on-close width="600px" :title="$tl('p.detailInfo')" :footer="null">
      <a-list item-layout="horizontal" :data-source="detailData">
        <template #renderItem="{ item }">
          <a-list-item>
            <a-list-item-meta :description="item.description">
              <template #title>
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
          title: this.$tl('p.alarmTitle'),
          dataIndex: 'title',
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$tl('p.nodeName'),
          dataIndex: 'nodeId',
          width: 100,
          ellipsis: true
        },
        {
          title: this.$tl('p.projectId'),
          dataIndex: 'projectId',
          width: 100,
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$tl('c.alarmStatus'),
          dataIndex: 'status',
          width: 100,
          align: 'center',
          ellipsis: true
        },
        {
          title: this.$tl('p.alarmMethod'),
          dataIndex: 'notifyStyle',
          width: 100,
          align: 'center',
          ellipsis: true
        },
        {
          title: this.$tl('p.alarmTime'),
          dataIndex: 'createTime',
          customRender: ({ text }) => {
            return parseTime(text)
          },
          width: '170px'
        },
        {
          title: this.$tl('c.notificationStatus'),
          dataIndex: 'notifyStatus',
          width: 100,
          ellipsis: true
        },
        {
          title: this.$tl('p.operation'),
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
    $tl(key, ...args) {
      return this.$t(`pages.monitor.log.${key}`, ...args)
    },
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
      this.detailData.push({ title: this.$tl('p.title'), description: this.temp.title })
      this.detailData.push({ title: this.$tl('p.content'), description: this.temp.content })
      this.detailData.push({
        title: this.$tl('p.notificationTarget'),
        description: this.temp.notifyObject
      })
      if (!this.temp.notifyStatus) {
        this.detailData.push({
          title: this.$tl('p.notificationAbnormal'),
          description: this.temp.notifyError
        })
      }
    }
  }
}
</script>
