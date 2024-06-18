<template>
  <div class="">
    <!-- 数据表格 -->
    <CustomTable
      is-show-tools
      default-auto-refresh
      :auto-refresh-time="30"
      :active-page="activePage"
      table-name="node-script-list"
      :empty-description="$t('i18n_d2f4a1550a')"
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
      @refresh="loadData"
    >
      <template #title>
        <a-space wrap class="search-box">
          <a-select
            v-if="!nodeId"
            v-model:value="listQuery.nodeId"
            allow-clear
            :placeholder="$t('i18n_f8a613d247')"
            class="search-input-item"
          >
            <a-select-option v-for="(nodeName, key) in nodeMap" :key="key">{{ nodeName }}</a-select-option>
          </a-select>
          <a-input
            v-model:value="listQuery['%name%']"
            :placeholder="$t('i18n_d7ec2d3fea')"
            allow-clear
            class="search-input-item"
            @press-enter="loadData"
          />
          <a-input
            v-model:value="listQuery['%autoExecCron%']"
            :placeholder="$t('i18n_6b2e348a2b')"
            class="search-input-item"
            @press-enter="loadData"
          />
          <a-tooltip :title="$t('i18n_4838a3bd20')">
            <a-button :loading="loading" type="primary" @click="loadData">{{ $t('i18n_e5f71fc31e') }}</a-button>
          </a-tooltip>

          <a-button type="primary" @click="handleEdit()">{{ $t('i18n_66ab5e9f24') }}</a-button>

          <template v-if="!nodeId">
            <a-dropdown v-if="nodeMap && Object.keys(nodeMap).length">
              <a-button type="primary" danger> {{ $t('i18n_b384470769') }}<DownOutlined /></a-button>
              <template #overlay>
                <a-menu>
                  <a-menu-item v-for="(nodeName, key) in nodeMap" :key="key" @click="sync(key)">
                    <a href="javascript:;">{{ nodeName }} <SyncOutlined /></a>
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </template>
          <a-button v-else type="primary" danger @click="sync(nodeId)">
            <SyncOutlined />{{ $t('i18n_b384470769') }}
          </a-button>
        </a-space>
      </template>
      <template #tableHelp>
        <a-tooltip>
          <template #title>
            <div>{{ $t('i18n_8ea93ff060') }}</div>

            <div>
              <ul>
                <li>{{ $t('i18n_5ecc709db7') }}</li>
                <li>{{ $t('i18n_14ee5b5dc5') }}</li>
                <li>{{ $t('i18n_fad1b9fb87') }}</li>
              </ul>
            </div>
          </template>
          <QuestionCircleOutlined />
        </a-tooltip>
      </template>
      <template #tableBodyCell="{ column, text, record }">
        <template v-if="column.tooltip">
          <a-tooltip placement="topLeft" :title="text">
            <span>{{ text }}</span>
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'name'">
          <a-tooltip placement="topLeft" :title="text" @click="handleEdit(record)">
            <!-- <span>{{ text }}</span> -->
            <a-button type="link" style="padding: 0" size="small">{{ text }}</a-button>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'workspaceId'">
          <a-tag v-if="text === 'GLOBAL'">{{ $t('i18n_2be75b1044') }}</a-tag>
          <a-tag v-else>{{ $t('i18n_98d69f8b62') }}</a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'scriptType'">
          <a-tooltip v-if="text === 'server-sync'" :title="$t('i18n_51341b5024')">
            <ClusterOutlined />
          </a-tooltip>
          <a-tooltip v-else :title="$t('i18n_3eab0eb8a9')">
            <FileTextOutlined />
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button size="small" type="primary" @click="handleExec(record)">{{ $t('i18n_1a6aa24e76') }}</a-button>
            <a-button size="small" type="primary" @click="handleLog(record)">{{ $t('i18n_456d29ef8b') }}</a-button>
            <a-button size="small" type="primary" @click="handleTrigger(record)">{{ $t('i18n_4696724ed3') }}</a-button>

            <a-dropdown>
              <a @click="(e) => e.preventDefault()">
                {{ $t('i18n_0ec9eaf9c3') }}
                <DownOutlined />
              </a>
              <template #overlay>
                <a-menu>
                  <a-menu-item>
                    <!-- <a-button size="small" :type="`${record.scriptType === 'server-sync' ? '' : 'primary'}`" @click="handleEdit(record)">{{ record.scriptType === "server-sync" ? "查看" : " 编辑" }}</a-button> -->
                    <a-tooltip
                      :title="`${record.scriptType === 'server-sync' ? $t('i18n_1f0d13a9ad') : $t('i18n_2f4aaddde3')}`"
                    >
                      <a-button
                        size="small"
                        :disabled="record.scriptType === 'server-sync'"
                        type="primary"
                        danger
                        @click="handleDelete(record)"
                        >{{ $t('i18n_2f4aaddde3') }}</a-button
                      >
                    </a-tooltip>
                  </a-menu-item>
                  <a-menu-item>
                    <a-button size="small" type="primary" danger @click="handleUnbind(record)">{{
                      $t('i18n_663393986e')
                    }}</a-button>
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
        </template>
      </template>
    </CustomTable>
    <!-- 编辑区 -->
    <ScriptEdit
      v-if="editScriptVisible"
      :node-id="temp.nodeId"
      :script-id="temp.scriptId"
      @close="
        () => {
          editScriptVisible = false
        }
      "
    ></ScriptEdit>
    <!-- 脚本控制台组件 -->
    <CustomDrawer
      v-if="drawerConsoleVisible"
      :title="drawerTitle"
      placement="right"
      width="85vw"
      :open="drawerConsoleVisible"
      @close="
        () => {
          drawerConsoleVisible = false
        }
      "
    >
      <script-console
        v-if="drawerConsoleVisible"
        :id="temp.id"
        :node-id="temp.nodeId"
        :def-args="temp.defArgs"
        :script-id="temp.scriptId"
      />
    </CustomDrawer>
    <!-- 脚本日志 -->
    <CustomDrawer
      v-if="drawerLogVisible"
      destroy-on-close
      :title="drawerTitle"
      width="50vw"
      :open="drawerLogVisible"
      @close="
        () => {
          drawerLogVisible = false
        }
      "
    >
      <script-log v-if="drawerLogVisible" :script-id="temp.scriptId" :node-id="temp.nodeId" />
    </CustomDrawer>
    <!-- 触发器 -->
    <CustomModal
      v-if="triggerVisible"
      v-model:open="triggerVisible"
      destroy-on-close
      :title="$t('i18n_4696724ed3')"
      width="50%"
      :footer="null"
    >
      <a-form ref="editTriggerForm" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-tabs default-active-key="1">
          <template #rightExtra>
            <a-tooltip :title="$t('i18n_01ad26f4a9')">
              <a-button type="primary" size="small" @click="resetTrigger">{{ $t('i18n_4b9c3271dc') }}</a-button>
            </a-tooltip>
          </template>
          <a-tab-pane key="1" :tab="$t('i18n_1a6aa24e76')">
            <a-space direction="vertical" style="width: 100%">
              <a-alert :message="$t('i18n_947d983961')" type="warning" show-icon>
                <template #description>
                  <ul>
                    <li>{{ $t('i18n_9308f22bf6') }}</li>
                    <li>{{ $t('i18n_632a907224') }}</li>
                    <li>{{ $t('i18n_3fca26a684') }}</li>
                    <li>{{ $t('i18n_a04b7a8f5d') }}</li>
                  </ul>
                </template>
              </a-alert>
              <a-alert type="info" :message="`${$t('i18n_de78b73dab')}(${$t('i18n_00a070c696')})`">
                <template #description>
                  <a-typography-paragraph :copyable="{ tooltip: false, text: temp.triggerUrl }">
                    <a-tag>GET</a-tag> <span>{{ temp.triggerUrl }} </span>
                  </a-typography-paragraph>
                </template>
              </a-alert>
              <a-alert type="info" :message="`${$t('i18n_8d202b890c')}(${$t('i18n_00a070c696')})`">
                <template #description>
                  <a-typography-paragraph :copyable="{ tooltip: false, text: temp.batchTriggerUrl }">
                    <a-tag>POST</a-tag> <span>{{ temp.batchTriggerUrl }} </span>
                  </a-typography-paragraph>
                </template>
              </a-alert>
            </a-space>
          </a-tab-pane>
        </a-tabs>
      </a-form>
    </CustomModal>
  </div>
</template>
<script>
import { deleteScript, getScriptListAll, getTriggerUrl, unbindScript, syncScript } from '@/api/node-other'

import { getNodeListAll } from '@/api/node'
import ScriptConsole from '@/pages/node/node-layout/other/script-console'
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'
import ScriptLog from '@/pages/node/node-layout/other/script-log'
import ScriptEdit from '@/pages/node/script-edit'

export default {
  components: {
    ScriptConsole,
    ScriptEdit,
    ScriptLog
  },
  props: {
    nodeId: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      loading: false,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),

      list: [],
      temp: {},
      nodeMap: {},
      editScriptVisible: false,
      drawerTitle: '',
      drawerConsoleVisible: false,
      drawerLogVisible: false,
      columns: [
        {
          title: 'scriptId',
          dataIndex: 'scriptId',
          ellipsis: true,
          width: 150,
          tooltip: true
        },
        {
          title: this.$t('i18n_d7ec2d3fea'),
          dataIndex: 'name',
          ellipsis: true,
          width: 200
        },
        {
          title: this.$t('i18n_b1785ef01e'),
          dataIndex: 'nodeName',
          ellipsis: true,
          width: 150,
          tooltip: true
        },
        {
          title: this.$t('i18n_6a588459d0'),
          dataIndex: 'workspaceName',
          ellipsis: true,
          width: 150,
          tooltip: true
        },
        {
          title: this.$t('i18n_226b091218'),
          dataIndex: 'scriptType',
          width: 70,
          align: 'center',
          ellipsis: true
        },
        {
          title: this.$t('i18n_fffd3ce745'),
          dataIndex: 'workspaceId',
          ellipsis: true,

          width: '90px'
        },
        {
          title: this.$t('i18n_6b2e348a2b'),
          dataIndex: 'autoExecCron',
          ellipsis: true,
          width: 120,
          tooltip: true
        },
        {
          title: this.$t('i18n_1303e638b5'),
          dataIndex: 'modifyTimeMillis',
          sorter: true,
          width: '170px',
          ellipsis: true,
          customRender: ({ text }) => parseTime(text)
        },
        {
          title: this.$t('i18n_eca37cb072'),
          dataIndex: 'createTimeMillis',
          sorter: true,
          width: '170px',
          ellipsis: true,
          customRender: ({ text }) => parseTime(text)
        },
        {
          title: this.$t('i18n_95a43eaa59'),
          dataIndex: 'createUser',
          ellipsis: true,
          width: '120px'
        },
        {
          title: this.$t('i18n_9baca0054e'),
          dataIndex: 'modifyUser',
          ellipsis: true,
          width: '120px'
        },
        {
          title: this.$t('i18n_26c1f8d83e'),
          dataIndex: 'lastRunUser',
          ellipsis: true,
          width: '120px'
        },
        {
          title: this.$t('i18n_2b6bc0f293'),
          dataIndex: 'operation',
          align: 'center',

          fixed: 'right',
          width: '250px'
        }
      ],

      triggerVisible: false
    }
  },
  computed: {
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery)
    },
    activePage() {
      return this.$attrs.routerUrl === this.$route.path
    }
  },
  mounted() {
    // this.calcTableHeight();

    getNodeListAll().then((res) => {
      if (res.code === 200) {
        res.data.forEach((item) => {
          this.nodeMap[item.id] = item.name
        })
      }
      this.loadData()
    })
  },
  methods: {
    // 加载数据
    loadData(pointerEvent) {
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
      this.loading = true
      this.nodeId && (this.listQuery.nodeId = this.nodeId)
      getScriptListAll(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result
          this.listQuery.total = res.data.total
        }
        this.loading = false
      })
    },
    parseTime,
    // 编辑
    handleEdit(record) {
      if (record) {
        this.temp = { ...record }
      } else {
        this.temp = { nodeId: this.listQuery.nodeId }
      }

      this.editScriptVisible = true
    },

    handleDelete(record) {
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_3b19b2a75c'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return deleteScript({
            nodeId: record.nodeId,
            id: record.scriptId
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
    // 执行 Script
    handleExec(record) {
      this.temp = Object.assign({}, record)
      this.drawerTitle = `${this.$t('i18n_b5c3770699')}(${this.temp.name})`
      this.drawerConsoleVisible = true
    },
    handleLog(record) {
      this.temp = Object.assign({}, record)
      this.drawerTitle = `${this.$t('i18n_456d29ef8b')}(${this.temp.name})`
      this.drawerLogVisible = true
    },
    // // 关闭 console
    // onConsoleClose() {
    //   this.drawerConsoleVisible = false;
    // },

    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter })
      this.loadData()
    },
    // 触发器
    handleTrigger(record) {
      this.temp = Object.assign({}, record)

      getTriggerUrl({
        id: record.id
      }).then((res) => {
        if (res.code === 200) {
          this.fillTriggerResult(res)
          this.triggerVisible = true
        }
      })
    },
    // 重置触发器
    resetTrigger() {
      getTriggerUrl({
        id: this.temp.id,
        rest: 'rest'
      }).then((res) => {
        if (res.code === 200) {
          $notification.success({
            message: res.msg
          })
          this.fillTriggerResult(res)
        }
      })
    },
    fillTriggerResult(res) {
      this.temp.triggerUrl = `${location.protocol}//${location.host}${res.data.triggerUrl}`
      this.temp.batchTriggerUrl = `${location.protocol}//${location.host}${res.data.batchTriggerUrl}`

      this.temp = { ...this.temp }
    },
    // 解绑
    handleUnbind(record) {
      const html = `
      <b style='font-size: 20px;'>${this.$t('i18n_2025ad11ee')}</b>
      <ul style='font-size: 20px;color:red;font-weight: bold;'>
        <li>${this.$t('i18n_56230405ae')}</b></li>
        <li>${this.$t('i18n_5c93055d9c')}</li>
        <li>${this.$t('i18n_27d0c8772c')}</li>
      </ul>
      `
      $confirm({
        title: this.$t('i18n_9362e6ddf8'),
        zIndex: 1009,
        content: h('div', null, [h('p', { innerHTML: html }, null)]),
        okButtonProps: { props: { type: 'danger', size: 'small' } },
        cancelButtonProps: { props: { type: 'primary' } },
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return unbindScript({
            id: record.id
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
    sync(nodeId) {
      syncScript({
        nodeId: nodeId
      }).then((res) => {
        if (res.code == 200) {
          $notification.success({
            message: res.msg
          })
          this.loadData()
        }
      })
    }
  }
}
</script>
