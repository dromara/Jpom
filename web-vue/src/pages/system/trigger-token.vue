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
            :placeholder="$t('i18n_638cddf480')"
            @press-enter="handleListLog"
          />
          <a-input
            v-model:value="viewOperationLogListQuery['triggerToken']"
            class="search-input-item"
            :placeholder="$t('i18n_ae35be7986')"
            @press-enter="handleListLog"
          />
          <a-select
            v-model:value="viewOperationLogListQuery.type"
            allow-clear
            :placeholder="$t('i18n_226b091218')"
            class="search-input-item"
          >
            <a-select-option v-for="item in allTypeList" :key="item.name">{{ item.desc }}</a-select-option>
          </a-select>
          <a-range-picker
            :show-time="{ format: 'HH:mm:ss' }"
            format="YYYY-MM-DD HH:mm:ss"
            @change="onchangeListLogTime"
          />
          <a-button type="primary" @click="handleListLog">{{ $t('i18n_e5f71fc31e') }}</a-button>
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
              $t('i18n_2f4aaddde3')
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
          title: this.$t('i18n_95a43eaa59'),
          dataIndex: 'userId',
          width: 100
        },
        {
          title: 'token',
          dataIndex: 'triggerToken',
          width: 100
        },

        {
          title: this.$t('i18n_d159466d0a'),
          dataIndex: 'dataName'
          // width: 100
        },
        {
          title: this.$t('i18n_00d5bdf1c3'),
          dataIndex: 'triggerCount',
          width: 100,
          sorter: true
        },
        {
          title: this.$t('i18n_45a4922d3f'),
          dataIndex: 'dataId',
          width: 100
        },

        {
          title: this.$t('i18n_eca37cb072'),
          dataIndex: 'createTimeMillis',
          sorter: true,
          customRender: ({ text }) => {
            return parseTime(text)
          },
          width: '180px'
        },
        {
          title: this.$t('i18n_2b6bc0f293'),
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
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_bba360b084'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
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
