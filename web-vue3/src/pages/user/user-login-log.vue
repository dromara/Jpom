<template>
  <div class="full-content">
    <!-- 数据表格 -->
    <a-table
      size="middle"
      :data-source="list"
      :columns="columns"
      :pagination="pagination"
      bordered
      :rowKey="(record, index) => index"
      @change="change"
    >
      <template #title>
        <a-space>
          <a-input
            v-model="listQuery['%username%']"
            @pressEnter="loadData"
            placeholder="用户名"
            class="search-input-item"
          />
          <a-input v-model="listQuery['%ip%']" @pressEnter="loadData" placeholder="登录IP" class="search-input-item" />
          <a-range-picker
            class="search-input-item"
            :show-time="{ format: 'HH:mm:ss' }"
            format="YYYY-MM-DD HH:mm:ss"
            @change="onchangeTime"
          />
          <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
            <a-button type="primary" :loading="loading" @click="loadData">搜索</a-button>
          </a-tooltip>
        </a-space>
      </template>
      <a-tooltip #success slot-scope="text" placement="topLeft" :title="text ? '成功' : '失败'">
        <a-tag v-if="text" color="green">成功</a-tag>
        <a-tag v-else color="pink">失败</a-tag>
      </a-tooltip>
      <a-tooltip #useMfa slot-scope="text" placement="topLeft" :title="text ? '使用' : '未使用'">
        <a-tag>{{ text ? '使用' : '未使用' }}</a-tag>
      </a-tooltip>

      <a-tooltip #tooltip slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>

      <a-tooltip #operateCode slot-scope="text" placement="topLeft" :title="operateCode[text] || '未知'">
        {{ operateCode[text] || '未知' }}
      </a-tooltip>
    </a-table>
  </div>
</template>
<script>
import { userLoginLgin, operateCodeMap } from '@/api/user/user-login-log'

import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'

export default {
  components: {},
  data() {
    return {
      loading: false,
      list: [],
      operateCode: operateCodeMap,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),

      columns: [
        { title: '用户ID', dataIndex: 'modifyUser', ellipsis: true, scopedSlots: { customRender: 'tooltip' } },
        { title: '用户名称', dataIndex: 'username', ellipsis: true, scopedSlots: { customRender: 'tooltip' } },
        { title: 'IP', dataIndex: 'ip', ellipsis: true, scopedSlots: { customRender: 'tooltip' } },
        { title: '浏览器', dataIndex: 'userAgent', ellipsis: true, scopedSlots: { customRender: 'tooltip' } },
        {
          title: '是否成功',
          dataIndex: 'success',
          ellipsis: true,
          width: '100px',
          scopedSlots: { customRender: 'success' }
        },
        {
          title: '是否使用MFA',
          dataIndex: 'useMfa',
          ellipsis: true,
          width: '130px',
          scopedSlots: { customRender: 'useMfa' }
        },
        {
          title: '结果描述',
          dataIndex: 'operateCode',
          /*width: 240,*/ ellipsis: true,
          scopedSlots: { customRender: 'operateCode' }
        },

        {
          title: '登录时间',
          dataIndex: 'createTimeMillis',
          sorter: true,
          customRender: (text, item) => {
            return parseTime(text || item.optTime)
          },
          width: '170px'
        }
      ]
    }
  },
  computed: {
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery)
    }
  },
  created() {
    this.loadData()
  },
  methods: {
    // 加载数据
    loadData(pointerEvent) {
      this.loading = true
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
      userLoginLgin(this.listQuery).then((res) => {
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
      this.listQuery.createTimeMillis = `${dateString[0]} ~ ${dateString[1]}`
    }
  }
}
</script>
<style scoped></style>
