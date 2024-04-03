<template>
  <div>
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
          <a-input v-model:value="listQuery['%name%']" placeholder="名称" allow-clear class="search-input-item" />
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
            placeholder="触发类型"
            class="search-input-item"
          >
            <a-select-option v-for="(val, key) in triggerExecTypeMap" :key="key">{{ val }}</a-select-option>
          </a-select>
          <a-range-picker
            allow-clear
            input-read-only
            :show-time="{ format: 'HH:mm:ss' }"
            :placeholder="['执行时间开始', '执行时间结束']"
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
          <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
            <a-button type="primary" :loading="loading" @click="loadData">搜索</a-button>
          </a-tooltip>
          <a-tooltip>
            <template #title>
              <div>
                脚本模版是存储在节点(插件端),执行也都将在节点里面执行,服务端会定时去拉取执行日志,拉取频率为 100 条/分钟
              </div>
              <div>
                <ul>
                  <li>数据可能出现一定时间延迟</li>
                </ul>
              </div>
            </template>
            <QuestionCircleOutlined />
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
          <span>{{ triggerExecTypeMap[text] || '未知' }}</span>
        </template>
        <template v-else-if="column.dataIndex === 'workspaceId'">
          <a-tag v-if="text === 'GLOBAL'">全局</a-tag>
          <a-tag v-else>工作空间</a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'createTimeMillis'">
          <a-tooltip :title="`${parseTime(record.createTimeMillis)}`">
            <span>{{ parseTime(record.createTimeMillis) }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button size="small" type="primary" @click="viewLog(record)">查看日志</a-button>

            <a-button size="small" type="primary" danger @click="handleDelete(record)">删除</a-button>
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
import { getScriptLogList, scriptDel, triggerExecTypeMap } from '@/api/node-other'
// import {triggerExecTypeMap} from "@/api/node-script";
import ScriptLogView from '@/pages/node/node-layout/other/script-log-view'
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'

export default {
  components: {
    ScriptLogView
  },
  props: {
    nodeId: {
      type: String,
      default: ''
    },
    scriptId: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      loading: false,
      listQuery: Object.assign(
        {
          scriptId: this.scriptId
        },
        PAGE_DEFAULT_LIST_QUERY
      ),
      triggerExecTypeMap: triggerExecTypeMap,
      list: [],
      temp: {},
      logVisible: 0,
      columns: [
        {
          title: '名称',
          dataIndex: 'scriptName',
          ellipsis: true,
          width: 100
        },
        {
          title: '执行时间',
          dataIndex: 'createTimeMillis',
          ellipsis: true,
          width: '160px'
        },
        {
          title: '触发类型',
          dataIndex: 'triggerExecType',
          width: 100,
          ellipsis: true
        },
        {
          title: '执行域',
          dataIndex: 'workspaceId',
          ellipsis: true,

          width: '90px'
        },
        {
          title: '执行人',
          dataIndex: 'modifyUser',
          ellipsis: true,
          width: 100
        },
        {
          title: '操作',
          dataIndex: 'operation',
          align: 'center',

          fixed: 'right',
          width: '100px'
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
    // 加载数据
    loadData(pointerEvent) {
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
      this.listQuery.nodeId = this.nodeId
      this.loading = true
      getScriptLogList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result
          this.listQuery.total = res.data.total
        }
        this.loading = false
      })
    },
    parseTime(v) {
      return parseTime(v)
    },
    viewLog(record) {
      this.logVisible = new Date() * Math.random()
      this.temp = record
    },
    handleDelete(record) {
      $confirm({
        title: '系统提示',
        zIndex: 1009,
        content: '真的要删除执行记录么？',
        okText: '确认',
        cancelText: '取消',
        onOk: () => {
          return scriptDel({
            nodeId: this.nodeId,
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
