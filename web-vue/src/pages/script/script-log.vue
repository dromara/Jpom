<template>
  <div class="">
    <!-- 数据表格 -->
    <a-table
      :data-source="list"
      size="middle"
      :columns="columns"
      :pagination="pagination"
      bordered
      row-key="id"
      :scroll="{
        x: 'max-content'
      }"
      @change="changePage"
    >
      <template #title>
        <a-space wrap class="search-box">
          <a-input
            v-model:value="listQuery['%scriptName%']"
            :placeholder="$tl('c.name')"
            allow-clear
            class="search-input-item"
            @press-enter="loadData"
          />
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
            :placeholder="$tl('c.triggerType')"
            class="search-input-item"
          >
            <a-select-option v-for="(val, key) in triggerExecTypeMap" :key="key">{{ val }}</a-select-option>
          </a-select>
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
            :placeholder="$tl('c.status')"
            class="search-input-item"
          >
            <a-select-option v-for="(val, key) in statusMap" :key="key">{{ val }}</a-select-option>
          </a-select>
          <a-range-picker
            allow-clear
            input-read-only
            :show-time="{ format: 'HH:mm:ss' }"
            :placeholder="[$tl('p.startTime'), $tl('p.endTime')]"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            @change="
              (value, dateString) => {
                if (!dateString[0] || !dateString[1]) {
                  listQuery.createTimeMillis = ''
                } else {
                  listQuery.createTimeMillis = `${dateString[0]} ~ ${dateString[1]}`
                }
              }
            "
          />
          <a-tooltip :title="$tl('p.hotkeyTip')">
            <a-button type="primary" :loading="loading" @click="loadData">{{ $tl('p.search') }}</a-button>
          </a-tooltip>
        </a-space>
      </template>
      <template #bodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'scriptName'">
          <a-tooltip placement="topLeft" :title="text">
            <span>{{ text }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'modifyUser'">
          <a-tooltip placement="topLeft" :title="text">
            <span>{{ text }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'triggerExecType'">
          <span>{{ triggerExecTypeMap[text] || $tl('p.unknown') }}</span>
        </template>
        <template v-else-if="column.dataIndex === 'workspaceId'">
          <a-tag v-if="text === 'GLOBAL'">{{ $tl('p.global') }}</a-tag>
          <a-tag v-else>{{ $tl('p.workspace') }}</a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'createTimeMillis'">
          <a-tooltip :title="`${parseTime(record.createTimeMillis)}`">
            <span>{{ parseTime(record.createTimeMillis) }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'exitCode'">
          <a-tag v-if="text == 0" color="green">{{ $tl('p.success') }}</a-tag>
          <a-tag v-else color="orange">{{ text || '-' }}</a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'status'">
          <span>{{ statusMap[text] || '' }}</span>
        </template>
        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button type="primary" size="small" @click="viewLog(record)">{{ $tl('p.viewLog') }}</a-button>

            <a-button type="primary" danger size="small" @click="handleDelete(record)">{{ $tl('p.delete') }}</a-button>
          </a-space>
        </template>
      </template>
    </a-table>
    <!-- 日志 -->

    <script-log-view
      v-if="logVisible > 0"
      :visible="logVisible != 0"
      :temp="temp"
      @close="
        () => {
          logVisible = 0
        }
      "
    />
  </div>
</template>

<script>
import { getScriptLogList, scriptDel, triggerExecTypeMap } from '@/api/server-script'
import ScriptLogView from '@/pages/script/script-log-view'
import { statusMap } from '@/api/command'
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'

export default {
  components: {
    ScriptLogView
  },
  props: {
    scriptId: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      loading: false,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      triggerExecTypeMap,
      statusMap,
      list: [],
      temp: {},
      logVisible: 0,
      columns: [
        {
          title: this.$tl('c.name'),
          dataIndex: 'scriptName',
          width: 100,
          ellipsis: true
        },
        {
          title: this.$tl('p.executionTime'),
          dataIndex: 'createTimeMillis',
          sorter: true,
          ellipsis: true,
          width: '160px'
        },
        {
          title: this.$tl('c.triggerType'),
          dataIndex: 'triggerExecType',
          width: 100,
          ellipsis: true
        },
        {
          title: this.$tl('c.status'),
          dataIndex: 'status',
          width: 100,
          ellipsis: true
        },
        {
          title: this.$tl('p.executionDomain'),
          dataIndex: 'workspaceId',
          ellipsis: true,

          width: '90px'
        },
        {
          title: this.$tl('p.exitCode'),
          dataIndex: 'exitCode',
          width: 100,
          ellipsis: true
        },
        {
          title: this.$tl('p.executor'),
          dataIndex: 'modifyUser',
          ellipsis: true,
          width: '100px'
        },
        {
          title: this.$tl('p.action'),
          dataIndex: 'operation',
          align: 'center',
          fixed: 'right',

          width: '150px'
        }
      ]
    }
  },
  computed: {
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery)
    }
  },
  mounted() {
    this.loadData()
  },
  methods: {
    $tl(key, ...args) {
      return this.$t(`pages.script.scriptLog.${key}`, ...args)
    },
    // 加载数据
    loadData(pointerEvent) {
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
      this.listQuery.scriptId = this.scriptId
      this.loading = true
      getScriptLogList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result
          this.listQuery.total = res.data.total
        }
        this.loading = false
      })
    },
    parseTime,
    viewLog(record) {
      this.logVisible = new Date() * Math.random()
      this.temp = record
    },
    handleDelete(record) {
      $confirm({
        title: this.$tl('p.systemTip'),
        content: this.$tl('p.deleteConfirm'),
        zIndex: 1009,
        okText: this.$tl('p.confirm'),
        cancelText: this.$tl('p.cancel'),
        onOk: () => {
          return scriptDel({
            id: record.scriptId,
            executeId: record.id
          }).then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              this.loadData()
            }
          })
        }
      })
    },
    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter })
      this.loadData()
    }
  }
}
</script>
