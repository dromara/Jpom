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
      :rowKey="(record, index) => index"
    >
      <template slot="title">
        <a-space>
          <a-input
            class="search-input-item"
            @pressEnter="handleListLog"
            v-model="viewOperationLogListQuery['modifyUser']"
            placeholder="操作人"
          />
          <a-input
            class="search-input-item"
            @pressEnter="handleListLog"
            v-model="viewOperationLogListQuery['%sshName%']"
            placeholder="ssh 名"
          />
          <a-input
            class="search-input-item"
            @pressEnter="handleListLog"
            v-model="viewOperationLogListQuery['%machineSshName%']"
            placeholder="机器 ssh 名"
          />
          <a-input
            class="search-input-item"
            @pressEnter="handleListLog"
            v-model="viewOperationLogListQuery['ip']"
            placeholder="ip"
          />
          <a-input
            class="search-input-item"
            @pressEnter="handleListLog"
            v-model="viewOperationLogListQuery['%commands%']"
            placeholder="执行命令"
          />
          <a-range-picker
            class="filter-item search-input-item"
            :show-time="{ format: 'HH:mm:ss' }"
            format="YYYY-MM-DD HH:mm:ss"
            @change="onchangeListLogTime"
          />
          <a-button type="primary" @click="handleListLog">搜索</a-button>
        </a-space>
      </template>
      <a-tooltip
        slot="commands"
        slot-scope="text"
        placement="topLeft"
        :title="text"
        v-clipboard:copy="text"
        v-clipboard:success="
          () => {
            $notification.success({ message: '复制成功' })
          }
        "
        v-clipboard:error="
          () => {
            $notification.error({ message: '复制失败' })
          }
        "
      >
        <a-button type="link" icon="copy" size="small"> {{ text }} </a-button>
        <!-- <a-input disabled :value="text"><a-icon slot="suffix" type="copy" /></a-input> -->
      </a-tooltip>
      <a-tooltip slot="modifyUser" slot-scope="text, item" placement="topLeft" :title="item.modifyUser || item.userId">
        <span>{{ item.modifyUser || item.userId }}</span>
      </a-tooltip>

      <a-tooltip slot="tooltip" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <template slot="refuse" slot-scope="text">
        <span>{{ text ? '成功' : '拒绝' }}</span>
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
        { title: '操作者', dataIndex: 'modifyUser', width: 100, scopedSlots: { customRender: 'modifyUser' } },
        { title: 'IP', dataIndex: 'ip', width: '130px' },
        {
          title: 'ssh名',
          dataIndex: 'sshName',
          width: '200px',
          ellipsis: true,
          scopedSlots: { customRender: 'tooltip' }
        },
        {
          title: '机器SSH名',
          dataIndex: 'machineSshName',
          width: '200px',
          ellipsis: true,
          scopedSlots: { customRender: 'tooltip' }
        },
        {
          title: '执行命令',
          dataIndex: 'commands',
          width: 200,
          ellipsis: true,
          scopedSlots: { customRender: 'commands' }
        },
        {
          title: 'userAgent',
          dataIndex: 'userAgent',
          /*width: 240,*/ ellipsis: true,
          scopedSlots: { customRender: 'tooltip' }
        },
        {
          title: '是否成功',
          dataIndex: 'refuse',
          width: '100px',
          ellipsis: true,
          scopedSlots: { customRender: 'refuse' }
        },

        {
          title: '操作时间',
          dataIndex: 'createTimeMillis',
          sorter: true,
          customRender: (text) => {
            return parseTime(text)
          },
          width: '180px'
        }
      ]
    }
  },
  created() {
    this.handleListLog()
  },
  methods: {
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
