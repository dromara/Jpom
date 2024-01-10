<template>
  <div>
    <a-table
      :data-source="list"
      size="middle"
      :columns="columns"
      bordered
      :pagination="false"
      :scroll="{
        x: 'max-content'
      }"
    >
      <template v-slot:title>
        <a-space>
          <a-input
            v-model:value="listQuery['serviceId']"
            @pressEnter="loadData"
            v-if="!this.serviceId"
            placeholder="服务id"
            class="search-input-item"
          />
          <a-input
            v-model:value="listQuery['taskName']"
            @pressEnter="loadData"
            placeholder="任务名称"
            class="search-input-item"
          />
          <a-input
            v-model:value="listQuery['taskId']"
            @pressEnter="loadData"
            placeholder="任务id"
            class="search-input-item"
          />
          <a-input
            v-model:value="listQuery['taskNode']"
            @pressEnter="loadData"
            placeholder="节点id"
            class="search-input-item"
          />

          <a-tooltip :title="TASK_STATE[listQuery['taskState']]">
            <a-select
              show-search
              option-filter-prop="children"
              v-model:value="listQuery['taskState']"
              allowClear
              placeholder="状态"
              class="search-input-item"
            >
              <a-select-option :key="key" v-for="(item, key) in TASK_STATE">{{ item }}- {{ key }}</a-select-option>
              <a-select-option value="">状态</a-select-option>
            </a-select>
          </a-tooltip>
          <a-button type="primary" @click="loadData" :loading="loading">搜索</a-button>
          <a-statistic-countdown format=" s 秒" title="刷新倒计时" :value="countdownTime" @finish="loadData" />
        </a-space>
      </template>

      <template #bodyCell="{ column, text, record }">
        <template v-if="column.tooltip">
          <a-tooltip placement="topLeft" :title="text">
            <span>{{ text }}</span>
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'address'">
          <a-tooltip placement="topLeft" :title="text">
            <CloudServerOutlined v-if="record.managerStatus && record.managerStatus.leader" />

            {{ text }}
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'desiredState'">
          <a-popover :title="`状态信息：${TASK_STATE[text]}`" placement="topLeft">
            <template v-slot:content>
              <p>
                当前状态：<a-tag>{{ text }}-{{ TASK_STATE[text] }}</a-tag>
              </p>
              <p v-if="record.status && record.status.err">错误信息：{{ record.status.err }}</p>
              <p v-if="record.status && record.status.state">
                状态：<a-tag>{{ record.status.state }}</a-tag>
              </p>

              <p v-if="record.status && record.status.message">
                信息：<a-tag>{{ record.status.message }} </a-tag>
              </p>
              <p v-if="record.status && record.status.timestamp">
                更新时间：<a-tag>{{ parseTime(record.status.timestamp) }} </a-tag>
              </p>
            </template>

            <a-tag :color="`${record.status && record.status.err ? 'orange' : text === 'RUNNING' ? 'green' : ''}`">
              {{ text }}
            </a-tag>
          </a-popover>
        </template>

        <template v-else-if="column.dataIndex === 'os'">
          <a-tooltip placement="topLeft" :title="text">
            <span>
              <a-tag
                >{{ text }}-{{
                  record.description && record.description.platform && record.description.platform.architecture
                }}
              </a-tag>
            </span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'updatedAt'">
          <a-tooltip placement="topLeft" :title="`修改时间：${text} 创建时间：${record.createdAt}`">
            <span>
              {{ parseTime(text) }}
            </span>
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button size="small" type="primary" @click="handleLog(record)">日志</a-button>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- 查看日志 -->

    <pull-log
      v-if="logVisible > 0"
      :visible="logVisible != 0"
      @close="
        () => {
          logVisible = 0
        }
      "
      :id="this.id"
      :dataId="this.temp.id"
      type="taks"
      :urlPrefix="this.urlPrefix"
    />
  </div>
</template>

<script>
import { dockerSwarmNodeUpdate, dockerSwarmServicesTaskList, TASK_STATE } from '@/api/docker-swarm'
import { parseTime } from '@/utils/const'
import PullLog from './pull-log'

export default {
  components: { PullLog },
  props: {
    id: {
      type: String
    },
    serviceId: { type: String },
    taskState: {
      type: String
    },
    visible: {
      type: Boolean,
      default: false
    },
    urlPrefix: {
      type: String
    }
  },
  data() {
    return {
      TASK_STATE,
      loading: false,
      listQuery: {},
      list: [],
      temp: {},
      editVisible: false,
      initSwarmVisible: false,
      autoUpdateTime: null,
      logVisible: 0,
      rules: {
        role: [{ required: true, message: '请选择节点角色', trigger: 'blur' }],
        availability: [{ required: true, message: '请选择节点状态', trigger: 'blur' }]
      },
      columns: [
        {
          title: '序号',
          width: '80px',
          ellipsis: true,
          align: 'center',
          customRender: ({ text, record, index }) => `${index + 1}`
        },
        {
          title: '任务Id',
          dataIndex: 'id',
          ellipsis: true,
          tooltip: true
        },
        {
          title: '节点Id',
          dataIndex: 'nodeId',
          ellipsis: true,
          tooltip: true
        },
        {
          title: '服务ID',
          dataIndex: 'serviceId',
          ellipsis: true,
          tooltip: true
        },
        {
          title: '镜像',
          dataIndex: ['spec', 'containerSpec', 'image'],
          ellipsis: true,
          width: 120,
          tooltip: true
        },
        // { title: "副本数", dataIndex: "spec.mode.replicated.replicas", width: 90, ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        // { title: "端点", dataIndex: "spec.endpointSpec.mode", ellipsis: true, width: 100, scopedSlots: { customRender: "tooltip" } },
        // { title: "节点地址", width: 150, dataIndex: "status.address", ellipsis: true, scopedSlots: { customRender: "address" } },
        {
          title: '状态',
          width: 140,
          dataIndex: 'desiredState',
          ellipsis: true
        },
        {
          title: '错误信息',
          width: 150,
          dataIndex: ['status', 'err'],
          ellipsis: true,
          tooltip: true
        },
        {
          title: 'slot',
          width: '80px',
          dataIndex: 'slot',
          ellipsis: true,
          tooltip: true
        },

        // { title: "系统类型", width: 140, align: "center", dataIndex: "description.platform.os", ellipsis: true, scopedSlots: { customRender: "os" } },
        // {
        //   title: "创建时间",
        //   dataIndex: "createdAt",

        //   ellipsis: true,
        //   scopedSlots: { customRender: "tooltip" },
        //   width: 170,
        // },
        {
          title: '修改时间',
          dataIndex: 'updatedAt',
          ellipsis: true,

          sorter: (a, b) => new Date(a.updatedAt).getTime() - new Date(b.updatedAt).getTime(),
          sortDirections: ['descend', 'ascend'],
          defaultSortOrder: 'descend',
          width: '180px'
        },
        {
          title: '操作',
          dataIndex: 'operation',
          fixed: 'right',
          align: 'center',
          width: '80px'
        }
      ],
      countdownTime: Date.now()
    }
  },
  computed: {},
  beforeUnmount() {},
  mounted() {
    this.listQuery.taskState = this.taskState
    this.loadData()
  },
  methods: {
    parseTime,
    // 加载数据
    loadData() {
      if (!this.visible) {
        return
      }
      this.loading = true
      if (this.serviceId) {
        this.listQuery.serviceId = this.serviceId
      }
      this.listQuery.id = this.id
      dockerSwarmServicesTaskList(this.urlPrefix, this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data
        }
        this.loading = false
        this.countdownTime = Date.now() + 5 * 1000
      })
    },
    // 日志
    handleLog(record) {
      this.logVisible = new Date() * Math.random()
      this.temp = record
    }
  }
}
</script>

<style scoped>
:deep(.ant-statistic div) {
  display: inline-block;
}
:deep(.ant-statistic-content-value, .ant-statistic-content) {
  font-size: 16px;
}
</style>
