<template>
  <div>
    <CustomTable
      is-show-tools
      default-auto-refresh
      :auto-refresh-time="30"
      :active-page="activePage"
      table-name="system-task-stat"
      :empty-description="$t('i18n_4ef719810b')"
      size="middle"
      row-key="taskId"
      :columns="taskColumns"
      bordered
      :data-source="taskList"
      :pagination="false"
      @refresh="refresh"
    >
      <!-- <template #title>
        <a-button size="small" type="primary" @click="refresh"><ReloadOutlined /></a-button>
      </template> -->
      <template #tableBodyCell="{ column, text, record }">
        <a-tooltip v-if="column.tooltip" placement="topLeft" :title="text">
          <span>{{ text }}</span>
        </a-tooltip>
        <a-tooltip v-else-if="column.dataIndex === 'lastExecuteTime'" :title="parseTime(text)">
          <span>{{ parseTime(text) }}</span>
        </a-tooltip>
        <a-tooltip v-else-if="column.dataIndex === 'desc'" :title="text">
          <span>{{ text }}</span>
        </a-tooltip>
        <a-tooltip v-else-if="column.dataIndex === 'cron'" placement="topLeft" :title="text">
          <a-button v-if="text" type="link" style="padding: 0" size="small" @click="toCronTaskList(text)">
            {{ text }} <UnorderedListOutlined />
          </a-button>
          <template v-else>{{ record.desc }}</template>
        </a-tooltip>
      </template>
    </CustomTable>
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
  emits: ['refresh'],
  data() {
    return {
      temp: {},

      taskColumns: [
        {
          title: this.$t('i18n_3a3778f20c'),
          dataIndex: 'taskId',

          // sorter: (a, b) => (a && b ? a.localeCompare(b, "zh-CN") : 0),
          tooltip: true,

          ellipsis: true,
          filters: [
            {
              text: this.$t('i18n_fcba60e773'),
              value: 'build'
            },
            {
              text: this.$t('i18n_e0ba3b9145'),
              value: 'script'
            },
            {
              text: this.$t('i18n_8c7c7f3cfa'),
              value: 'server_script'
            },
            {
              text: `ssh ${this.$t('i18n_ba311d8a6a')}`,
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
        // {
        //   title: '描述',
        //   dataIndex: 'desc'
        //   // sorter: (a, b) => (a && b ? a.localeCompare(b, "zh-CN") : 0),
        //   // sortDirections: ["descend", "ascend"],
        // },
        {
          title: this.$t('i18n_d4aea8d7e6'),
          dataIndex: 'executeCount',
          sortDirections: ['descend', 'ascend'],
          width: 140,
          sorter: (a, b) => a.executeCount || 0 - b.executeCount || 0
        },
        {
          title: this.$t('i18n_e7d83a24ba'),
          dataIndex: 'succeedCount',
          sortDirections: ['descend', 'ascend'],
          width: 140,
          sorter: (a, b) => a.succeedCount || 0 - b.succeedCount || 0
        },
        {
          title: this.$t('i18n_d3e480c8c0'),
          dataIndex: 'failedCount',
          sortDirections: ['descend', 'ascend'],
          width: 140,
          sorter: (a, b) => a.failedCount || 0 - b.failedCount || 0
        },
        {
          title: this.$t('i18n_17c06f6a8b'),
          dataIndex: 'lastExecuteTime',
          sortDirections: ['descend', 'ascend'],
          defaultSortOrder: 'descend',
          width: 180,
          sorter: (a, b) => a.lastExecuteTime || 0 - b.lastExecuteTime || 0
        }
      ]
    }
  },
  computed: {
    activePage() {
      return this.$attrs.routerUrl === this.$route.path
    }
  },
  mounted() {},
  methods: {
    parseTime,
    refresh() {
      this.$emit('refresh', {})
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
