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
            v-model:value="viewOperationLogListQuery['modifyUser']"
            class="search-input-item"
            :placeholder="$tl('p.operator')"
            @press-enter="handleListLog"
          />
          <a-input
            v-model:value="viewOperationLogListQuery['%sshName%']"
            class="search-input-item"
            :placeholder="$tl('p.sshName')"
            @press-enter="handleListLog"
          />
          <a-input
            v-model:value="viewOperationLogListQuery['%machineSshName%']"
            class="search-input-item"
            :placeholder="$tl('p.machineSshName')"
            @press-enter="handleListLog"
          />
          <a-input
            v-model:value="viewOperationLogListQuery['ip']"
            class="search-input-item"
            placeholder="ip"
            @press-enter="handleListLog"
          />
          <a-input
            v-model:value="viewOperationLogListQuery['%commands%']"
            class="search-input-item"
            :placeholder="$tl('p.executeCommand')"
            @press-enter="handleListLog"
          />
          <a-range-picker
            :show-time="{ format: 'HH:mm:ss' }"
            format="YYYY-MM-DD HH:mm:ss"
            @change="onchangeListLogTime"
          />
          <a-button type="primary" @click="handleListLog">{{ $tl('p.search') }}</a-button>
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
        <template v-else-if="column.dataIndex === 'modifyUser'">
          <a-tooltip placement="topLeft" :title="record.modifyUser || record.userId">
            <span>{{ record.modifyUser || record.userId }}</span>
          </a-tooltip>
        </template>

        <template v-else-if="column.tooltip">
          <a-tooltip placement="topLeft" :title="text">
            <span>{{ text }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'refuse'">
          <span>{{ text ? $tl('p.success') : $tl('p.denied') }}</span>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script>
import { getSshOperationLogList } from '@/api/ssh'
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'
import { getMachineSshOperationLogList } from '@/api/system/assets-ssh'
export default {
  components: {},
  props: {
    sshId: {
      type: String,
      default: ''
    },
    machineSshId: {
      type: String,
      default: ''
    },
    type: {
      type: String,
      default: ''
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
          title: this.$tl('p.operatorName'),
          dataIndex: 'modifyUser',
          width: 100
        },
        { title: 'IP', dataIndex: 'ip', width: '130px' },
        {
          title: `ssh${this.$tl('p.name')}`,
          dataIndex: 'sshName',
          width: '200px',
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$tl('p.machineSshName2'),
          dataIndex: 'machineSshName',
          width: '200px',
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$tl('p.executeCommand'),
          dataIndex: 'commands',
          width: 200,
          ellipsis: true
        },
        {
          title: 'userAgent',
          dataIndex: 'userAgent',
          width: 240,
          ellipsis: true
        },

        {
          title: this.$tl('p.operationTime'),
          dataIndex: 'createTimeMillis',
          sorter: true,
          customRender: ({ text }) => {
            return parseTime(text)
          },
          width: '180px'
        },
        {
          title: this.$tl('p.isSuccess'),
          dataIndex: 'refuse',
          width: '100px',
          ellipsis: true,
          fixed: 'right'
        }
      ]
    }
  },
  computed: {
    viewOperationLogPagination() {
      return COMPUTED_PAGINATION(this.viewOperationLogListQuery)
    }
  },
  created() {
    this.handleListLog()
  },
  methods: {
    $tl(key, ...args) {
      return this.$t(`pages.system.assets.ssh.operationLog.${key}`, ...args)
    },
    handleListLog() {
      this.viewOperationLoading = true
      let api
      if (this.type == 'machinessh') {
        // 查看所有日志
        api = getMachineSshOperationLogList
      } else {
        api = this.machineSshId ? getMachineSshOperationLogList : getSshOperationLogList
      }

      api(this.viewOperationLogListQuery).then((res) => {
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
    }
  }
}
</script>
