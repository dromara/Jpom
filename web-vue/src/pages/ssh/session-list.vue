<!--

    Copyright (c) 2019 Of Him Code Technology Studio
    Jpom is licensed under Mulan PSL v2.
    You can use this software according to the terms and conditions of the Mulan PSL v2.
    You may obtain a copy of Mulan PSL v2 at:
    			http://license.coscl.org.cn/MulanPSL2
    THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
    See the Mulan PSL v2 for more details.

-->
<template>
  <div>
    <CustomTable
      is-show-tools
      default-auto-refresh
      :auto-refresh-time="30"
      :active-page="activePage"
      table-name="ssh-terminal-session-list"
      size="middle"
      :data-source="sessionList"
      :columns="columns"
      bordered
      :pagination="pagination"
      :scroll="{
        x: 'max-content'
      }"
      row-key="id"
      :row-selection="rowSelection"
      @change="changePage"
      @refresh="getSessionList"
    >
      <template #title>
        <a-space wrap class="search-box">
          <a-input
            v-model:value="listQuery['%machineSshName%']"
            :placeholder="$t('i18n_2638c0984b')"
            class="search-input-item"
            @press-enter="getSessionList"
          />
          <a-input
            v-model:value="listQuery['%sshName%']"
            :placeholder="$t('i18n_d584e1493b')"
            class="search-input-item"
            @press-enter="getSessionList"
          />
          <a-input
            v-model:value="listQuery.userId"
            :placeholder="$t('i18n_30acd20d6e')"
            class="search-input-item"
            @press-enter="getSessionList"
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
            :placeholder="$t('i18n_3fea7ca76c')"
            class="search-input-item"
          >
            <a-select-option v-for="(val, key) in statusMap" :key="key">{{ val }}</a-select-option>
          </a-select>
          <a-tooltip :title="$t('i18n_4838a3bd20')">
            <a-button type="primary" :loading="loading" @click="getSessionList">{{ $t('i18n_e5f71fc31e') }}</a-button>
          </a-tooltip>
          <a-button
            type="primary"
            danger
            :disabled="!tableSelections || tableSelections.length <= 0"
            @click="handleBatchDelete"
          >
            {{ $t('i18n_7fb62b3011') }}
          </a-button>
        </a-space>
      </template>
      <template #tableBodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'machineSshName'">
          <a-tooltip placement="topLeft" :title="text">
            <span>{{ text }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'sshName'">
          <a-tooltip placement="topLeft" :title="text">
            <span>{{ text }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'status'">
          <a-tag :color="statusColorMap[text] || 'default'">{{ statusMap[text] || text }}</a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button type="primary" size="small" @click="handleReplay(record)">{{ $t('i18n_1fadef9aba') }}</a-button>
            <a-button type="primary" size="small" @click="handleDownload(record)"
              ><DownloadOutlined />{{ $t('i18n_f26ef91424') }}</a-button
            >
            <a-button type="primary" danger size="small" @click="handleDelete(record)">{{
              $t('i18n_2f4aaddde3')
            }}</a-button>
          </a-space>
        </template>
      </template>
    </CustomTable>
    <!-- 会话回放 -->
    <session-replay v-if="replayVisible" v-model:open="replayVisible" :session="temp" @close="replayVisible = false" />
  </div>
</template>
<script>
import {
  deleteSshTerminalSession,
  getSshTerminalSessionCastUrl,
  getSshTerminalSessionList,
  sshTerminalSessionStatusColorMap,
  sshTerminalSessionStatusMap
} from '@/api/ssh-terminal-session'
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime, renderSize } from '@/utils/const'
import SessionReplay from './session-replay'
import { mapState } from 'pinia'
import { useGuideStore } from '@/stores/guide'
export default {
  components: {
    SessionReplay
  },
  data() {
    return {
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      sessionList: [],
      loading: false,
      temp: {},
      statusMap: sshTerminalSessionStatusMap,
      statusColorMap: sshTerminalSessionStatusColorMap,
      replayVisible: false,
      columns: [
        {
          title: this.$t('i18n_592c595891'),
          dataIndex: 'startTime',
          sorter: true,
          ellipsis: true,
          customRender: ({ text }) => {
            return parseTime(text)
          },
          width: '170px'
        },
        {
          title: this.$t('i18n_a5dd0b6b79'),
          dataIndex: 'machineSshName',
          ellipsis: true
        },
        {
          title: this.$t('i18n_10f6fc171a'),
          dataIndex: 'sshName',
          ellipsis: true
        },
        {
          title: this.$t('i18n_1fd02a90c3'),
          dataIndex: 'userId',
          width: 120,
          ellipsis: true
        },
        {
          title: this.$t('i18n_94aa0e9b48'),
          dataIndex: 'commandCount',
          width: 100,
          ellipsis: true
        },
        {
          title: this.$t('i18n_58f9666705'),
          dataIndex: 'bytes',
          width: 110,
          ellipsis: true,
          customRender: ({ text }) => {
            return renderSize(text)
          }
        },
        {
          title: this.$t('i18n_3fea7ca76c'),
          dataIndex: 'status',
          width: 110,
          ellipsis: true
        },
        {
          title: this.$t('i18n_11c50da4da'),
          dataIndex: 'ip',
          width: 140,
          ellipsis: true
        },
        {
          title: this.$t('i18n_2b6bc0f293'),
          dataIndex: 'operation',
          align: 'center',
          fixed: 'right',
          width: '1%'
        }
      ],
      tableSelections: []
    }
  },
  computed: {
    ...mapState(useGuideStore, ['getFullscreenViewLogStyle']),
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery)
    },
    activePage() {
      return this.$attrs.routerUrl === this.$route.path
    },
    rowSelection() {
      return {
        onChange: (selectedRowKeys) => {
          this.tableSelections = selectedRowKeys
        },
        selectedRowKeys: this.tableSelections,
        type: 'checkbox'
      }
    }
  },
  created() {
    // 从资产页面跳转过来时自动带上资产（machineSshId）过滤
    const machineSshId = this.$route.query.machineSshId
    if (machineSshId) {
      this.listQuery.machineSshId = machineSshId
    }
  },
  mounted() {
    this.getSessionList()
  },
  methods: {
    // 获取会话录制列表
    getSessionList(pointerEvent) {
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
      this.loading = true
      getSshTerminalSessionList(this.listQuery).then((res) => {
        if (200 === res.code) {
          this.sessionList = res.data.result
          this.listQuery.total = res.data.total
        }
        this.loading = false
      })
    },
    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter })
      this.getSessionList()
    },
    // 打开会话回放
    handleReplay(record) {
      this.temp = record
      this.replayVisible = true
    },
    // 下载录制文件（.cast）
    handleDownload(record) {
      window.open(getSshTerminalSessionCastUrl(record.id), '_blank')
    },
    // 删除会话录制
    handleDelete(row) {
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_2cd0a9de1f'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return deleteSshTerminalSession(row.id).then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              this.getSessionList()
            }
          })
        }
      })
    },
    // 批量删除会话录制
    handleBatchDelete() {
      if (!this.tableSelections || this.tableSelections.length <= 0) {
        $notification.warning({
          message: this.$t('i18n_5d817c403e')
        })
        return
      }
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_9d1436dc49'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return deleteSshTerminalSession(this.tableSelections.join(',')).then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              this.tableSelections = []
              this.getSessionList()
            }
          })
        }
      })
    }
  }
}
</script>
