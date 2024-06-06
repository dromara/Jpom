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
            :placeholder="$t('pages.monitor.log.2c33c91c')"
            class="search-input-item"
          >
            <a-select-option v-for="(nodeName, key) in nodeMap" :key="key">{{ nodeName }}</a-select-option>
          </a-select>
          <a-select
            v-model:value="listQuery.status"
            allow-clear
            :placeholder="$t('pages.monitor.log.95aeba54')"
            class="search-input-item"
          >
            <a-select-option :value="1">{{ $t('pages.monitor.log.3483f3b7') }}</a-select-option>
            <a-select-option :value="0">{{ $t('pages.monitor.log.346ba9a') }}</a-select-option>
          </a-select>
          <a-select
            v-model:value="listQuery.notifyStatus"
            allow-clear
            :placeholder="$t('pages.monitor.log.6abae0da')"
            class="search-input-item"
          >
            <a-select-option :value="1">{{ $t('pages.monitor.log.9d77d967') }}</a-select-option>
            <a-select-option :value="0">{{ $t('pages.monitor.log.d3ec0514') }}</a-select-option>
          </a-select>
          <a-range-picker :show-time="{ format: 'HH:mm:ss' }" format="YYYY-MM-DD HH:mm:ss" @change="onchangeTime" />
          <a-tooltip :title="$t('pages.monitor.log.767472f6')">
            <a-button :loading="loading" type="primary" @click="loadData">{{
              $t('pages.monitor.log.53c2763c')
            }}</a-button>
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
          <span>{{ text ? $t('pages.monitor.log.3483f3b7') : $t('pages.monitor.log.346ba9a') }}</span>
        </template>
        <template v-else-if="column.dataIndex === 'notifyStyle'">
          {{ notifyStyle[text] || $t('pages.monitor.log.ca1cdfa6') }}
        </template>
        <template v-else-if="column.dataIndex === 'notifyStatus'">
          <span>{{ text ? $t('pages.monitor.log.9d77d967') : $t('pages.monitor.log.d3ec0514') }}</span>
        </template>
        <template v-else-if="column.dataIndex === 'operation'">
          <a-button size="small" type="primary" @click="handleDetail(record)">{{
            $t('pages.monitor.log.151c73eb')
          }}</a-button>
        </template>
      </template>
    </a-table>
    <!-- 详情区 -->
    <a-modal
      v-model:open="detailVisible"
      destroy-on-close
      width="600px"
      :title="$t('pages.monitor.log.7990de3b')"
      :footer="null"
    >
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
          title: this.$t('pages.monitor.log.f00e2365'),
          dataIndex: 'title',
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('pages.monitor.log.fa8d810f'),
          dataIndex: 'nodeId',
          width: 100,
          ellipsis: true
        },
        {
          title: this.$t('pages.monitor.log.4eaba425'),
          dataIndex: 'projectId',
          width: 100,
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('pages.monitor.log.95aeba54'),
          dataIndex: 'status',
          width: 100,
          align: 'center',
          ellipsis: true
        },
        {
          title: this.$t('pages.monitor.log.b93a2f3c'),
          dataIndex: 'notifyStyle',
          width: 100,
          align: 'center',
          ellipsis: true
        },
        {
          title: this.$t('pages.monitor.log.ec06406f'),
          dataIndex: 'createTime',
          customRender: ({ text }) => {
            return parseTime(text)
          },
          width: '170px'
        },
        {
          title: this.$t('pages.monitor.log.6abae0da'),
          dataIndex: 'notifyStatus',
          width: 100,
          ellipsis: true
        },
        {
          title: this.$t('pages.monitor.log.3bb962bf'),
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
      this.detailData.push({ title: this.$t('pages.monitor.log.a1b6e465'), description: this.temp.title })
      this.detailData.push({ title: this.$t('pages.monitor.log.99ff48c8'), description: this.temp.content })
      this.detailData.push({
        title: this.$t('pages.monitor.log.18ba4cd5'),
        description: this.temp.notifyObject
      })
      if (!this.temp.notifyStatus) {
        this.detailData.push({
          title: this.$t('pages.monitor.log.65f01a1b'),
          description: this.temp.notifyError
        })
      }
    }
  }
}
</script>
