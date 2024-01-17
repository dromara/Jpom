<template>
  <div>
    <!-- 数据表格 -->
    <a-table
      :data-source="viewOperationLogList"
      :loading="viewOperationLoading"
      :columns="viewOperationLogColumns"
      :pagination="viewOperationLogPagination"
      @change="changeListLog"
      bordered
      size="middle"
      :scroll="{
        x: 'max-content'
      }"
    >
      <template v-slot:title>
        <a-space>
          <a-input
            class="search-input-item"
            @pressEnter="handleListLog"
            v-model:value="viewOperationLogListQuery['userId']"
            placeholder="创建人,全匹配"
          />
          <a-input
            class="search-input-item"
            @pressEnter="handleListLog"
            v-model:value="viewOperationLogListQuery['triggerToken']"
            placeholder="token,全匹配"
          />
          <a-select
            v-model:value="viewOperationLogListQuery.type"
            allowClear
            placeholder="类型"
            class="search-input-item"
          >
            <a-select-option v-for="item in allTypeList" :key="item.name">{{ item.desc }}</a-select-option>
          </a-select>
          <a-range-picker
            :show-time="{ format: 'HH:mm:ss' }"
            format="YYYY-MM-DD HH:mm:ss"
            @change="onchangeListLogTime"
          />
          <a-button type="primary" @click="handleListLog">搜索</a-button>
        </a-space>
      </template>
      <template #bodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'commands'">
          <a-tooltip placement="topLeft" :title="text">
            <a-typography-paragraph
              v-if="text"
              :copyable="{ tooltip: false, text: text }"
              style="display: inline-block; margin-bottom: 0"
            >
            </a-typography-paragraph>
            {{ text }}
          </a-tooltip>
        </template>

        <template v-else-if="column.tooltip">
          <a-tooltip placement="topLeft" :title="text">
            <span>{{ text }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button size="small" type="primary" danger @click="handleDelete(record)">删除</a-button>
          </a-space>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script>
import { getSshOperationLogList } from '@/api/ssh'
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'
import { triggerTokenList, triggerTokenAllType, triggerTokenDelete } from '@/api/trigger-token'
export default {
  components: {},
  props: {},
  computed: {
    viewOperationLogPagination() {
      return COMPUTED_PAGINATION(this.viewOperationLogListQuery)
    }
  },

  data() {
    return {
      viewOperationLoading: false,
      viewOperationLogList: [],
      viewOperationLogListQuery: Object.assign(
        { sshId: this.sshId, machineSshId: this.machineSshId },
        PAGE_DEFAULT_LIST_QUERY
      ),
      viewOperationLogColumns: [
        {
          title: '创建人',
          dataIndex: 'userId',
          width: 100
        },
        {
          title: 'token',
          dataIndex: 'triggerToken',
          width: 100
        },

        {
          title: '关联数据名',
          dataIndex: 'dataName'
          // width: 100
        },
        {
          title: '调用次数',
          dataIndex: 'triggerCount',
          width: 100,
          sorter: true
        },
        {
          title: '关联数据',
          dataIndex: 'dataId',
          width: 100
        },

        {
          title: '创建时间',
          dataIndex: 'createTimeMillis',
          sorter: true,
          customRender: ({ text }) => {
            return parseTime(text)
          },
          width: '180px'
        },
        {
          title: '操作',
          dataIndex: 'operation',
          width: '80px',

          align: 'center',
          fixed: 'right'
        }
      ],
      allTypeList: []
    }
  },
  created() {
    triggerTokenAllType().then((res) => {
      if (res.code === 200) {
        this.allTypeList = res.data || []
      }
    })
    this.handleListLog()
  },
  methods: {
    handleListLog() {
      this.viewOperationLoading = true

      triggerTokenList(this.viewOperationLogListQuery).then((res) => {
        if (res.code === 200) {
          this.viewOperationLogList = res.data.result
          this.viewOperationLogListQuery.total = res.data.total
        }
        this.viewOperationLoading = false
      })
    },
    changeListLog(pagination, filters, sorter) {
      this.viewOperationLogListQuery = CHANGE_PAGE(this.viewOperationLogListQuery, { pagination, sorter })

      this.handleListLog()
    },
    // 选择时间
    onchangeListLogTime(value, dateString) {
      if (dateString[0]) {
        this.viewOperationLogListQuery.createTimeMillis = `${dateString[0]} ~ ${dateString[1]}`
      } else {
        this.viewOperationLogListQuery.createTimeMillis = ''
      }
    },
    // 删除
    handleDelete(record) {
      const that = this
      $confirm({
        title: '系统提示',
        zIndex: 1009,
        content: '真的要删除对应的触发器吗？',
        okText: '确认',
        cancelText: '取消',
        async onOk() {
          return await new Promise((resolve, reject) => {
            // 删除
            triggerTokenDelete({
              id: record.id
            })
              .then((res) => {
                if (res.code === 200) {
                  $notification.success({
                    message: res.msg
                  })
                  that.handleListLog()
                }
                resolve()
              })
              .catch(reject)
          })
        }
      })
    }
  }
}
</script>
