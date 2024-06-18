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
            :placeholder="$t('i18n_f8a613d247')"
            class="search-input-item"
          >
            <a-select-option v-for="(nodeName, key) in nodeMap" :key="key">{{ nodeName }}</a-select-option>
          </a-select>
          <a-select
            v-model:value="listQuery.status"
            allow-clear
            :placeholder="$t('i18n_db4470d98d')"
            class="search-input-item"
          >
            <a-select-option :value="1">{{ $t('i18n_fd6e80f1e0') }}</a-select-option>
            <a-select-option :value="0">{{ $t('i18n_c195df6308') }}</a-select-option>
          </a-select>
          <a-select
            v-model:value="listQuery.notifyStatus"
            allow-clear
            :placeholder="$t('i18n_8023baf064')"
            class="search-input-item"
          >
            <a-select-option :value="1">{{ $t('i18n_330363dfc5') }}</a-select-option>
            <a-select-option :value="0">{{ $t('i18n_acd5cb847a') }}</a-select-option>
          </a-select>
          <a-range-picker :show-time="{ format: 'HH:mm:ss' }" format="YYYY-MM-DD HH:mm:ss" @change="onchangeTime" />
          <a-tooltip :title="$t('i18n_4838a3bd20')">
            <a-button :loading="loading" type="primary" @click="loadData">{{ $t('i18n_e5f71fc31e') }}</a-button>
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
          <span>{{ text ? $t('i18n_fd6e80f1e0') : $t('i18n_c195df6308') }}</span>
        </template>
        <template v-else-if="column.dataIndex === 'notifyStyle'">
          {{ notifyStyle[text] || $t('i18n_1622dc9b6b') }}
        </template>
        <template v-else-if="column.dataIndex === 'notifyStatus'">
          <span>{{ text ? $t('i18n_330363dfc5') : $t('i18n_acd5cb847a') }}</span>
        </template>
        <template v-else-if="column.dataIndex === 'operation'">
          <a-button size="small" type="primary" @click="handleDetail(record)">{{ $t('i18n_f26225bde6') }}</a-button>
        </template>
      </template>
    </a-table>
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
            <a-list-item-meta :description="item.description">
              <template #title>
                <h4>{{ item.title }}</h4>
              </template>
            </a-list-item-meta>
          </a-list-item>
        </template>
      </a-list>
    </CustomModal>
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
          title: this.$t('i18n_36b3f3a2f6'),
          dataIndex: 'title',
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('i18n_b1785ef01e'),
          dataIndex: 'nodeId',
          width: 100,
          ellipsis: true
        },
        {
          title: this.$t('i18n_4fdd2213b5'),
          dataIndex: 'projectId',
          width: 100,
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('i18n_db4470d98d'),
          dataIndex: 'status',
          width: 100,
          align: 'center',
          ellipsis: true
        },
        {
          title: this.$t('i18n_52eedb4a12'),
          dataIndex: 'notifyStyle',
          width: 100,
          align: 'center',
          ellipsis: true
        },
        {
          title: this.$t('i18n_4741e596ac'),
          dataIndex: 'createTime',
          customRender: ({ text }) => {
            return parseTime(text)
          },
          width: '170px'
        },
        {
          title: this.$t('i18n_8023baf064'),
          dataIndex: 'notifyStatus',
          width: 100,
          ellipsis: true
        },
        {
          title: this.$t('i18n_2b6bc0f293'),
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
      this.detailData.push({ title: this.$t('i18n_32c65d8d74'), description: this.temp.title })
      this.detailData.push({ title: this.$t('i18n_2d711b09bd'), description: this.temp.content })
      this.detailData.push({
        title: this.$t('i18n_59c75681b4'),
        description: this.temp.notifyObject
      })
      if (!this.temp.notifyStatus) {
        this.detailData.push({
          title: this.$t('i18n_fcb4c2610a'),
          description: this.temp.notifyError
        })
      }
    }
  }
}
</script>
