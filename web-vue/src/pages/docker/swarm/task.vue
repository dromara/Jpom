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
      <template #title>
        <a-space wrap class="search-box">
          <a-input
            v-if="!serviceId"
            v-model:value="listQuery['serviceId']"
            placeholder="服务id"
            class="search-input-item"
            @press-enter="loadData"
          />
          <a-input
            v-model:value="listQuery['taskName']"
            :placeholder="$tl('p.taskName')"
            class="search-input-item"
            @press-enter="loadData"
          />
          <a-input
            v-model:value="listQuery['taskId']"
            placeholder="任务id"
            class="search-input-item"
            @press-enter="loadData"
          />
          <a-input
            v-model:value="listQuery['taskNode']"
            placeholder="节点id"
            class="search-input-item"
            @press-enter="loadData"
          />

          <a-tooltip :title="TASK_STATE[listQuery['taskState']]">
            <a-select
              v-model:value="listQuery['taskState']"
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
              <a-select-option v-for="(item, key) in TASK_STATE" :key="key">{{ item }}- {{ key }}</a-select-option>
              <a-select-option value="">{{ $tl('c.status') }}</a-select-option>
            </a-select>
          </a-tooltip>
          <a-button type="primary" :loading="loading" @click="loadData">{{ $tl('p.search') }}</a-button>
          <a-statistic-countdown
            format="s"
            :title="$tl('p.refreshCountdown')"
            :value="countdownTime"
            @finish="loadData"
          >
            <template #suffix>
              <div style="font-size: 12px">{{ $tl('p.seconds') }}</div>
            </template>
          </a-statistic-countdown>
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
          <a-popover :title="`${$tl('p.statusInfo')}${TASK_STATE[text]}`" placement="topLeft">
            <template #content>
              <p>
                {{ $tl('p.currentStatus') }}<a-tag>{{ text }}-{{ TASK_STATE[text] }}</a-tag>
              </p>
              <p v-if="record.status && record.status.err">错误信息：{{ record.status.err }}</p>
              <p v-if="record.status && record.status.state">
                {{ $tl('p.state') }}<a-tag>{{ record.status.state }}</a-tag>
              </p>

              <p v-if="record.status && record.status.message">
                {{ $tl('p.info') }}<a-tag>{{ record.status.message }} </a-tag>
              </p>
              <p v-if="record.status && record.status.timestamp">
                {{ $tl('p.updateTime') }}<a-tag>{{ parseTime(record.status.timestamp) }} </a-tag>
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
          <a-tooltip
            placement="topLeft"
            :title="`${$tl('p.modifyTime')}${text} ${$tl('p.createTime')}${record.createdAt}`"
          >
            <span>
              {{ parseTime(text) }}
            </span>
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button size="small" type="primary" @click="handleLog(record)">{{ $tl('p.log') }}</a-button>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- 查看日志 -->

    <pull-log
      v-if="logVisible > 0"
      :id="id"
      :visible="logVisible != 0"
      :data-id="temp.id"
      type="taks"
      :url-prefix="urlPrefix"
      @close="
        () => {
          logVisible = 0
        }
      "
    />
  </div>
</template>

<script>
import { dockerSwarmServicesTaskList, TASK_STATE } from '@/api/docker-swarm'
import { parseTime } from '@/utils/const'
import PullLog from './pull-log'

export default {
  components: { PullLog },
  props: {
    id: {
      type: String,
      default: ''
    },
    serviceId: { type: String, default: '' },
    taskState: {
      type: String,
      default: ''
    },
    visible: {
      type: Boolean,
      default: false
    },
    urlPrefix: {
      type: String,
      default: ''
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
        role: [{ required: true, message: this.$tl('p.pleaseSelectNodeRole'), trigger: 'blur' }],
        availability: [{ required: true, message: this.$tl('p.pleaseSelectNodeStatus'), trigger: 'blur' }]
      },
      columns: [
        {
          title: this.$tl('p.serialNumber'),
          width: '80px',
          ellipsis: true,
          align: 'center',
          customRender: ({ index }) => `${index + 1}`
        },
        {
          title: this.$tl('p.taskId'),
          dataIndex: 'id',
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$tl('p.nodeId'),
          dataIndex: 'nodeId',
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$tl('p.serviceId'),
          dataIndex: 'serviceId',
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$tl('p.image'),
          dataIndex: ['spec', 'containerSpec', 'image'],
          ellipsis: true,
          width: 120,
          tooltip: true
        },
        // { title: "副本数", dataIndex: "spec.mode.replicated.replicas", width: 90, ellipsis: true,  },
        // { title: "端点", dataIndex: "spec.endpointSpec.mode", ellipsis: true, width: 100, },
        // { title: "节点地址", width: 150, dataIndex: "status.address", ellipsis: true,  },
        {
          title: this.$tl('c.status'),
          width: 140,
          dataIndex: 'desiredState',
          ellipsis: true
        },
        {
          title: this.$tl('p.errorInfo'),
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

        // { title: "系统类型", width: 140, align: "center", dataIndex: "description.platform.os", ellipsis: true,  },
        // {
        //   title: "创建时间",
        //   dataIndex: "createdAt",

        //   ellipsis: true,

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
          title: this.$tl('p.operation'),
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
    $tl(key, ...args) {
      return this.$t(`pages.docker.swarm.task.${key}`, ...args)
    },
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
