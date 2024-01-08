<template>
  <div>
    <a-table
      size="middle"
      :data-source="commandList"
      :columns="columns"
      bordered
      :pagination="pagination"
      @change="changePage"
      :scroll="{
        x: 'max-content'
      }"
    >
      <template v-slot:title>
        <a-space>
          <a-input
            v-model:value="listQuery['%commandName%']"
            @pressEnter="getCommandLogData"
            placeholder="搜索命令名称"
            class="search-input-item"
          />
          <a-input
            v-model:value="listQuery['%sshName%']"
            @pressEnter="getCommandLogData"
            placeholder="搜索ssh名称"
            class="search-input-item"
          />
          <a-select
            show-search
            option-filter-prop="children"
            v-model:value="listQuery.status"
            allowClear
            placeholder="状态"
            class="search-input-item"
          >
            <a-select-option v-for="(val, key) in statusMap" :key="key">{{ val }}</a-select-option>
          </a-select>
          <a-select
            show-search
            option-filter-prop="children"
            v-model:value="listQuery.triggerExecType"
            allowClear
            placeholder="触发类型"
            class="search-input-item"
          >
            <a-select-option v-for="(val, key) in triggerExecTypeMap" :key="key">{{ val }}</a-select-option>
          </a-select>
          <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
            <a-button type="primary" :loading="loading" @click="getCommandLogData">搜索</a-button>
          </a-tooltip>
        </a-space>
      </template>
      <template #bodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'sshName'">
          <a-tooltip placement="topLeft" :title="text">
            <span>{{ text }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'commandName'">
          <a-tooltip placement="topLeft" :title="text">
            <span>{{ text }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'status'">
          <span>{{ statusMap[text] || '未知' }}</span>
        </template>
        <template v-else-if="column.dataIndex === 'triggerExecType'">
          <span>{{ triggerExecTypeMap[text] || '未知' }}</span>
        </template>
        <template v-else-if="column.dataIndex === 'exitCode'">
          <a-tag v-if="text == 0" color="green">成功</a-tag>
          <a-tag v-else color="orange">{{ text || '-' }}</a-tag>
        </template>

        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button type="primary" size="small" :disabled="!record.hasLog" @click="handleView(record)">查看</a-button>
            <a-button type="primary" size="small" :disabled="!record.hasLog" @click="handleDownload(record)"
              ><DownloadOutlined />日志</a-button
            >
            <a-button type="primary" danger size="small" @click="handleDelete(record)">删除</a-button>
          </a-space>
        </template>
      </template>
    </a-table>
    <!-- 构建日志 -->
    <a-modal
      destroyOnClose
      :width="style.width"
      :bodyStyle="style.bodyStyle"
      :style="style.style"
      v-model:open="logVisible"
      title="执行日志"
      :footer="null"
      :maskClosable="false"
    >
      <command-log :height="style.bodyStyle.height" v-if="logVisible" :temp="temp" />
    </a-modal>
  </div>
</template>

<script>
import { deleteCommandLog, downloadLog, getCommandLogList, statusMap, triggerExecTypeMap } from '@/api/command'
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'
import CommandLog from './command-view-log'
import { mapState } from 'pinia'
import { useGuideStore } from '@/stores/guide'
export default {
  components: {
    CommandLog
  },
  data() {
    return {
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      commandList: [],
      loading: false,
      temp: {},
      statusMap: statusMap,
      triggerExecTypeMap: triggerExecTypeMap,
      logVisible: false,
      columns: [
        {
          title: 'ssh 名称',
          dataIndex: 'sshName',
          ellipsis: true
        },
        {
          title: '命令名称',
          dataIndex: 'commandName',
          ellipsis: true
        },
        {
          title: '状态',
          dataIndex: 'status',
          width: 100,
          ellipsis: true
        },
        {
          title: '退出码',
          dataIndex: 'exitCode',
          width: 100,
          ellipsis: true
        },
        {
          title: '触发类型',
          dataIndex: 'triggerExecType',
          width: 100,
          ellipsis: true
        },
        {
          title: '执行时间',
          dataIndex: 'createTimeMillis',
          sorter: true,
          ellipsis: true,
          customRender: ({ text }) => {
            return parseTime(text)
          },
          width: '170px'
        },
        {
          title: '结束时间',
          dataIndex: 'modifyTimeMillis',
          sorter: true,
          ellipsis: true,
          customRender: ({ text }) => {
            return parseTime(text)
          },
          width: '170px'
        },
        {
          title: '执行人',
          dataIndex: 'modifyUser',
          width: 120,
          ellipsis: true
        },
        {
          title: '操作',
          dataIndex: 'operation',
          align: 'center',
          fixed: 'right',
          width: '200px'
        }
      ]
    }
  },
  computed: {
    ...mapState(useGuideStore, ['getFullscreenViewLogStyle']),
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery)
    },
    style() {
      return this.getFullscreenViewLogStyle()
    }
  },
  created() {},
  mounted() {
    this.getCommandLogData()
  },
  methods: {
    handleView(row) {
      this.temp = row
      this.logVisible = true
    },

    // 获取命令数据
    getCommandLogData(pointerEvent) {
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
      this.loading = true
      getCommandLogList(this.listQuery).then((res) => {
        if (200 === res.code) {
          this.commandList = res.data.result
          this.listQuery.total = res.data.total
        }
        this.loading = false
      })
    },
    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter })
      this.getCommandLogData()
    },
    //  删除命令
    handleDelete(row) {
      this.$confirm({
        title: '系统提示',
        zIndex: 1009,
        content: '真的要删除该执行记录吗？',
        okText: '确认',
        cancelText: '取消',
        async onOk() {
          return await new Promise((resolve, reject) => {
            // 删除
            deleteCommandLog(row.id)
              .then((res) => {
                if (res.code === 200) {
                  this.$notification.success({
                    message: res.msg
                  })
                  this.getCommandLogData()
                }
                resolve()
              })
              .catch(reject)
          })
        }
      })
    },
    // 下载构建日志
    handleDownload(record) {
      window.open(downloadLog(record.id), '_blank')
    }
  }
}
</script>
