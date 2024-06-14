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
            :placeholder="$t('pages.ssh.command-log.da64475b')"
            class="search-input-item"
            @press-enter="getCommandLogData"
          />
          <a-input
            v-model:value="listQuery['%sshName%']"
            :placeholder="$t('pages.ssh.command-log.8c255b9e')"
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
            :placeholder="$t('pages.ssh.command-log.9c32c887')"
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
            :placeholder="$t('pages.ssh.command-log.4abba04f')"
            class="search-input-item"
          >
            <a-select-option v-for="(val, key) in triggerExecTypeMap" :key="key">{{ val }}</a-select-option>
          </a-select>
          <a-tooltip :title="$t('pages.ssh.command-log.b5ad7947')">
            <a-button type="primary" :loading="loading" @click="getCommandLogData">{{
              $t('pages.ssh.command-log.53c2763c')
            }}</a-button>
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
          <span>{{ statusMap[text] || $t('pages.ssh.command-log.ca1cdfa6') }}</span>
        </template>
        <template v-else-if="column.dataIndex === 'triggerExecType'">
          <span>{{ triggerExecTypeMap[text] || $t('pages.ssh.command-log.ca1cdfa6') }}</span>
        </template>
        <template v-else-if="column.dataIndex === 'exitCode'">
          <a-tag v-if="text == 0" color="green">{{ $t('pages.ssh.command-log.83aa7d3') }}</a-tag>
          <a-tag v-else color="orange">{{ text || '-' }}</a-tag>
        </template>

        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button type="primary" size="small" :disabled="!record.hasLog" @click="handleView(record)">{{
              $t('pages.ssh.command-log.1ba84995')
            }}</a-button>
            <a-button type="primary" size="small" :disabled="!record.hasLog" @click="handleDownload(record)"
              ><DownloadOutlined />{{ $t('pages.ssh.command-log.f637e08') }}</a-button
            >
            <a-button type="primary" danger size="small" @click="handleDelete(record)">{{
              $t('pages.ssh.command-log.dd20d11c')
            }}</a-button>
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
      :title="$t('pages.ssh.command-log.8fb8f5f9')"
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
          title: `ssh ${this.$t('pages.ssh.command-log.bb769c1d')}`,
          dataIndex: 'sshName',
          ellipsis: true
        },
        {
          title: this.$t('pages.ssh.command-log.e740b86f'),
          dataIndex: 'commandName',
          ellipsis: true
        },
        {
          title: this.$t('pages.ssh.command-log.9c32c887'),
          dataIndex: 'status',
          width: 100,
          ellipsis: true
        },
        {
          title: this.$t('pages.ssh.command-log.cdb6dc6b'),
          dataIndex: 'exitCode',
          width: 100,
          ellipsis: true
        },
        {
          title: this.$t('pages.ssh.command-log.4abba04f'),
          dataIndex: 'triggerExecType',
          width: 100,
          ellipsis: true
        },
        {
          title: this.$t('pages.ssh.command-log.40fb635f'),
          dataIndex: 'createTimeMillis',
          sorter: true,
          ellipsis: true,
          customRender: ({ text }) => {
            return parseTime(text)
          },
          width: '170px'
        },
        {
          title: this.$t('pages.ssh.command-log.9376939d'),
          dataIndex: 'modifyTimeMillis',
          sorter: true,
          ellipsis: true,
          customRender: ({ text }) => {
            return parseTime(text)
          },
          width: '170px'
        },
        {
          title: this.$t('pages.ssh.command-log.cda0e062'),
          dataIndex: 'modifyUser',
          width: 120,
          ellipsis: true
        },
        {
          title: this.$t('pages.ssh.command-log.3bb962bf'),
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
      $confirm({
        title: this.$t('pages.ssh.command-log.b22d55a0'),
        zIndex: 1009,
        content: this.$t('pages.ssh.command-log.e51646aa'),
        okText: this.$t('pages.ssh.command-log.e8e9db25'),
        cancelText: this.$t('pages.ssh.command-log.b12468e9'),
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
