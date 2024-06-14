<template>
  <div>
    <!-- 数据表格 -->
    <a-table
      :data-source="viewOperationLogList"
      :loading="viewOperationLoading"
      :columns="viewOperationLogColumns"
      :pagination="viewOperationLogPagination"
      bordered
      size="middle"
      :scroll="{
        x: 'max-content'
      }"
      @change="changeListLog"
    >
      <template #title>
        <a-space wrap class="search-box">
          <a-input
            v-model:value="viewOperationLogListQuery['userId']"
            class="search-input-item"
            :placeholder="$t('pages.system.trigger-token.46cac1')"
            @press-enter="handleListLog"
          />
          <a-input
            v-model:value="viewOperationLogListQuery['triggerToken']"
            class="search-input-item"
            :placeholder="$t('pages.system.trigger-token.ef1afc6c')"
            @press-enter="handleListLog"
          />
          <a-select
            v-model:value="viewOperationLogListQuery.type"
            allow-clear
            :placeholder="$t('pages.system.trigger-token.698bb532')"
            class="search-input-item"
          >
            <a-select-option v-for="item in allTypeList" :key="item.name">{{ item.desc }}</a-select-option>
          </a-select>
          <a-range-picker
            :show-time="{ format: 'HH:mm:ss' }"
            format="YYYY-MM-DD HH:mm:ss"
            @change="onchangeListLogTime"
          />
          <a-button type="primary" @click="handleListLog">{{ $t('pages.system.trigger-token.53c2763c') }}</a-button>
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
            <a-button size="small" type="primary" danger @click="handleDelete(record)">{{
              $t('pages.system.trigger-token.dd20d11c')
            }}</a-button>
          </a-space>
        </template>
      </template>
    </a-table>
  </div>
</template>
<script>
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'
import { triggerTokenList, triggerTokenAllType, triggerTokenDelete } from '@/api/trigger-token'
export default {
  components: {},
  props: {},

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
          title: this.$t('pages.system.trigger-token.db3c9202'),
          dataIndex: 'userId',
          width: 100
        },
        {
          title: 'token',
          dataIndex: 'triggerToken',
          width: 100
        },

        {
          title: this.$t('pages.system.trigger-token.8e4d106'),
          dataIndex: 'dataName'
          // width: 100
        },
        {
          title: this.$t('pages.system.trigger-token.948a121d'),
          dataIndex: 'triggerCount',
          width: 100,
          sorter: true
        },
        {
          title: this.$t('pages.system.trigger-token.733ffc6e'),
          dataIndex: 'dataId',
          width: 100
        },

        {
          title: this.$t('pages.system.trigger-token.f5b90169'),
          dataIndex: 'createTimeMillis',
          sorter: true,
          customRender: ({ text }) => {
            return parseTime(text)
          },
          width: '180px'
        },
        {
          title: this.$t('pages.system.trigger-token.3bb962bf'),
          dataIndex: 'operation',
          width: '80px',

          align: 'center',
          fixed: 'right'
        }
      ],

      allTypeList: []
    }
  },
  computed: {
    viewOperationLogPagination() {
      return COMPUTED_PAGINATION(this.viewOperationLogListQuery)
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
      $confirm({
        title: this.$t('pages.system.trigger-token.b22d55a0'),
        zIndex: 1009,
        content: this.$t('pages.system.trigger-token.e51646aa'),
        okText: this.$t('pages.system.trigger-token.e8e9db25'),
        cancelText: this.$t('pages.system.trigger-token.b12468e9'),
        onOk: () => {
          return triggerTokenDelete({
            id: record.id
          }).then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              this.handleListLog()
            }
          })
        }
      })
    }
  }
}
</script>
