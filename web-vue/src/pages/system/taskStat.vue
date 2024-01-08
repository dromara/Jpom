<template>
  <div>
    <a-table size="middle" rowKey="taskId" :columns="taskColumns" bordered :data-source="taskList" :pagination="false">
      <template #title>
        <a-button size="small" type="primary" @click="refresh"><ReloadOutlined /></a-button>
      </template>
      <template #bodyCell="{ column, text, record }">
        <a-tooltip v-if="column.tooltip" placement="topLeft" :title="text">
          <span>{{ text }}</span>
        </a-tooltip>
        <a-tooltip v-else-if="column.dataIndex === 'lastExecuteTime'" :title="parseTime(text)">
          <span>{{ parseTime(text) }}</span>
        </a-tooltip>
        <a-tooltip v-else-if="column.dataIndex === 'cron'" placement="topLeft" :title="text">
          <a-button type="link" v-if="text" style="padding: 0" size="small" @click="toCronTaskList(text)">
            {{ text }} <UnorderedListOutlined />
          </a-button>
        </a-tooltip>
      </template>
    </a-table>
  </div>
</template>
<script>
import { parseTime } from '@/utils/const'
export default {
  name: 'TaskStat',
  props: {
    taskList: {
      type: Array,
      default: () => []
    }
  },
  data() {
    return {
      temp: {},

      taskColumns: [
        {
          title: '任务ID',
          dataIndex: 'taskId',

          // sorter: (a, b) => (a && b ? a.localeCompare(b, "zh-CN") : 0),
          tooltip: true,

          ellipsis: true,
          filters: [
            {
              text: '构建',
              value: 'build'
            },
            {
              text: '节点脚本',
              value: 'script'
            },
            {
              text: '服务端脚本',
              value: 'server_script'
            },
            {
              text: 'ssh 脚本',
              value: 'ssh_command'
            }
          ],
          onFilter: (value, record) => record.taskId.indexOf(value) === 0
        },
        {
          title: 'cron',
          dataIndex: 'cron'

          // sorter: (a, b) => (a && b ? a.localeCompare(b, "zh-CN") : 0),
          // sortDirections: ["descend", "ascend"],
        },
        {
          title: '执行次数',
          dataIndex: 'executeCount',
          sortDirections: ['descend', 'ascend'],
          width: 140,
          sorter: (a, b) => a.executeCount || 0 - b.executeCount || 0
        },
        {
          title: '成功次数',
          dataIndex: 'succeedCount',
          sortDirections: ['descend', 'ascend'],
          width: 140,
          sorter: (a, b) => a.succeedCount || 0 - b.succeedCount || 0
        },
        {
          title: '失败次数',
          dataIndex: 'failedCount',
          sortDirections: ['descend', 'ascend'],
          width: 140,
          sorter: (a, b) => a.failedCount || 0 - b.failedCount || 0
        },
        {
          title: '最后执行时间',
          dataIndex: 'lastExecuteTime',
          sortDirections: ['descend', 'ascend'],
          defaultSortOrder: 'descend',
          width: 180,
          sorter: (a, b) => a.lastExecuteTime || 0 - b.lastExecuteTime || 0
        }
      ]
    }
  },
  mounted() {},
  methods: {
    parseTime,
    refresh() {
      this.$emit('refresh')
    },
    // 前往 cron 详情
    toCronTaskList(cron) {
      const newpage = this.$router.resolve({
        path: '/tools/cron',
        query: {
          ...this.$route.query,
          sPid: 'tools',
          sId: 'cronTools',
          cron
        }
      })
      window.open(newpage.href, '_blank')
    }
  }
}
</script>
