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
          <a-input
            v-model:value="listQuery['%name%']"
            :placeholder="$t('pages.node.node-layout.other.script-log.bb769c1d')"
            allow-clear
            class="search-input-item"
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
            :placeholder="$t('pages.node.node-layout.other.script-log.4abba04f')"
            class="search-input-item"
          >
            <a-select-option v-for="(val, key) in triggerExecTypeMap" :key="key">{{ val }}</a-select-option>
          </a-select>
          <a-range-picker
            allow-clear
            input-read-only
            :show-time="{ format: 'HH:mm:ss' }"
            :placeholder="[
              $t('pages.node.node-layout.other.script-log.ac9ef5f5'),
              $t('pages.node.node-layout.other.script-log.9376939d')
            ]"
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
          <a-tooltip :title="$t('pages.node.node-layout.other.script-log.c2e3463f')">
            <a-button type="primary" :loading="loading" @click="loadData">{{
              $t('pages.node.node-layout.other.script-log.53c2763c')
            }}</a-button>
          </a-tooltip>
          <a-tooltip>
            <template #title>
              <div>{{ $t('pages.node.node-layout.other.script-log.66889710') }}</div>
              <div>
                <ul>
                  <li>{{ $t('pages.node.node-layout.other.script-log.b709d0b4') }}</li>
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
          <span>{{ triggerExecTypeMap[text] || $t('pages.node.node-layout.other.script-log.ca1cdfa6') }}</span>
        </template>
        <template v-else-if="column.dataIndex === 'workspaceId'">
          <a-tag v-if="text === 'GLOBAL'">{{ $t('pages.node.node-layout.other.script-log.8a2fc9dd') }}</a-tag>
          <a-tag v-else>{{ $t('pages.node.node-layout.other.script-log.afacc4cb') }}</a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'createTimeMillis'">
          <a-tooltip :title="`${parseTime(record.createTimeMillis)}`">
            <span>{{ parseTime(record.createTimeMillis) }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button size="small" type="primary" @click="viewLog(record)">{{
              $t('pages.node.node-layout.other.script-log.b51c8bb3')
            }}</a-button>

            <a-button size="small" type="primary" danger @click="handleDelete(record)">{{
              $t('pages.node.node-layout.other.script-log.a58214f0')
            }}</a-button>
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
          title: this.$t('pages.node.node-layout.other.script-log.bb769c1d'),
          dataIndex: 'scriptName',
          ellipsis: true,
          width: 100
        },
        {
          title: this.$t('pages.node.node-layout.other.script-log.40fb635f'),
          dataIndex: 'createTimeMillis',
          ellipsis: true,
          width: '160px'
        },
        {
          title: this.$t('pages.node.node-layout.other.script-log.4abba04f'),
          dataIndex: 'triggerExecType',
          width: 100,
          ellipsis: true
        },
        {
          title: this.$t('pages.node.node-layout.other.script-log.3debe02b'),
          dataIndex: 'workspaceId',
          ellipsis: true,

          width: '90px'
        },
        {
          title: this.$t('pages.node.node-layout.other.script-log.cda0e062'),
          dataIndex: 'modifyUser',
          ellipsis: true,
          width: 100
        },
        {
          title: this.$t('pages.node.node-layout.other.script-log.3bb962bf'),
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
        title: this.$t('pages.node.node-layout.other.script-log.b22d55a0'),
        zIndex: 1009,
        content: this.$t('pages.node.node-layout.other.script-log.2c8deda8'),
        okText: this.$t('pages.node.node-layout.other.script-log.e8e9db25'),
        cancelText: this.$t('pages.node.node-layout.other.script-log.b12468e9'),
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
