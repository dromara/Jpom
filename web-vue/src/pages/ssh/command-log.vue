<template>
  <div>
    <a-table
      size="middle"
      :data-source="commandList"
      :columns="columns"
      bordered
      :pagination="pagination"
      :scroll="{
        x: 'max-content'
      }"
      @change="changePage"
    >
      <template #title>
        <a-space wrap class="search-box">
          <a-input
            v-model:value="listQuery['%commandName%']"
            :placeholder="$tl('p.searchCommandName')"
            class="search-input-item"
            @press-enter="getCommandLogData"
          />
          <a-input
            v-model:value="listQuery['%sshName%']"
            :placeholder="$tl('p.searchSshName')"
            class="search-input-item"
            @press-enter="getCommandLogData"
          />
          <a-select
            v-model:value="listQuery.status"
            show-search
            :filter-option="
              (input, option) => {
                const children = option.children && option.children()
                return (
                  children &&
                  children[0].children &&
                  children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
                )
              }
            "
            allow-clear
            :placeholder="$tl('p.status')"
            class="search-input-item"
          >
            <a-select-option v-for="(val, key) in statusMap" :key="key">{{ val }}</a-select-option>
          </a-select>
          <a-select
            v-model:value="listQuery.triggerExecType"
            show-search
            :filter-option="
              (input, option) => {
                const children = option.children && option.children()
                return (
                  children &&
                  children[0].children &&
                  children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
                )
              }
            "
            allow-clear
            :placeholder="$tl('p.triggerType')"
            class="search-input-item"
          >
            <a-select-option v-for="(val, key) in triggerExecTypeMap" :key="key">{{ val }}</a-select-option>
          </a-select>
          <a-tooltip :title="$tl('p.shortcutToFirstPage')">
            <a-button type="primary" :loading="loading" @click="getCommandLogData">{{ $tl('p.search') }}</a-button>
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
          <span>{{ statusMap[text] || $tl('p.unknown') }}</span>
        </template>
        <template v-else-if="column.dataIndex === 'triggerExecType'">
          <span>{{ triggerExecTypeMap[text] || $tl('p.unknown') }}</span>
        </template>
        <template v-else-if="column.dataIndex === 'exitCode'">
          <a-tag v-if="text == 0" color="green">{{ $tl('p.success') }}</a-tag>
          <a-tag v-else color="orange">{{ text || '-' }}</a-tag>
        </template>

        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button type="primary" size="small" :disabled="!record.hasLog" @click="handleView(record)">{{
              $tl('p.view')
            }}</a-button>
            <a-button type="primary" size="small" :disabled="!record.hasLog" @click="handleDownload(record)"
              ><DownloadOutlined />{{ $tl('p.log') }}</a-button
            >
            <a-button type="primary" danger size="small" @click="handleDelete(record)">{{ $tl('p.delete') }}</a-button>
          </a-space>
        </template>
      </template>
    </a-table>
    <!-- 构建日志 -->
    <a-modal
      v-model:open="logVisible"
      destroy-on-close
      :width="style.width"
      :body-style="style.bodyStyle"
      :style="style.style"
      :title="$tl('p.executionLog')"
      :footer="null"
      :mask-closable="false"
    >
      <command-log v-if="logVisible" :height="style.bodyStyle.height" :temp="temp" />
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
          title: `ssh ${this.$tl('p.name')}`,
          dataIndex: 'sshName',
          ellipsis: true
        },
        {
          title: this.$tl('p.commandName'),
          dataIndex: 'commandName',
          ellipsis: true
        },
        {
          title: this.$tl('p.status'),
          dataIndex: 'status',
          width: 100,
          ellipsis: true
        },
        {
          title: this.$tl('p.exitCode'),
          dataIndex: 'exitCode',
          width: 100,
          ellipsis: true
        },
        {
          title: this.$tl('p.triggerType'),
          dataIndex: 'triggerExecType',
          width: 100,
          ellipsis: true
        },
        {
          title: this.$tl('p.executionTime'),
          dataIndex: 'createTimeMillis',
          sorter: true,
          ellipsis: true,
          customRender: ({ text }) => {
            return parseTime(text)
          },
          width: '170px'
        },
        {
          title: this.$tl('p.endTime'),
          dataIndex: 'modifyTimeMillis',
          sorter: true,
          ellipsis: true,
          customRender: ({ text }) => {
            return parseTime(text)
          },
          width: '170px'
        },
        {
          title: this.$tl('p.executor'),
          dataIndex: 'modifyUser',
          width: 120,
          ellipsis: true
        },
        {
          title: this.$tl('p.operation'),
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
    $tl(key, ...args) {
      return this.$t(`pages.ssh.commandLog.${key}`, ...args)
    },
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
      $confirm({
        title: this.$tl('p.systemPrompt'),
        zIndex: 1009,
        content: this.$tl('p.confirmDeletion'),
        okText: this.$tl('p.confirm'),
        cancelText: this.$tl('p.cancel'),
        onOk: () => {
          return deleteCommandLog(row.id).then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              this.getCommandLogData()
            }
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
