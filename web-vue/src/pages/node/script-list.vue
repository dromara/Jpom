<template>
  <div class="">
    <!-- 数据表格 -->
    <CustomTable
      is-show-tools
      default-auto-refresh
      :auto-refresh-time="30"
      :active-page="activePage"
      table-name="node-script-list"
      :empty-description="$t('pages.node.script-list.546ac7e5')"
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
            :placeholder="$t('pages.node.script-list.580e6c10')"
            class="search-input-item"
          >
            <a-select-option v-for="(nodeName, key) in nodeMap" :key="key">{{ nodeName }}</a-select-option>
          </a-select>
          <a-input
            v-model:value="listQuery['%name%']"
            :placeholder="$t('pages.node.script-list.3e34ec28')"
            allow-clear
            class="search-input-item"
            @press-enter="loadData"
          />
          <a-input
            v-model:value="listQuery['%autoExecCron%']"
            :placeholder="$t('pages.node.script-list.eeeaafc5')"
            class="search-input-item"
            @press-enter="loadData"
          />
          <a-tooltip :title="$t('pages.node.script-list.554d1b95')">
            <a-button :loading="loading" type="primary" @click="loadData">{{
              $t('pages.node.script-list.53c2763c')
            }}</a-button>
          </a-tooltip>

          <a-button type="primary" @click="handleEdit()">{{ $t('pages.node.script-list.7d46652a') }}</a-button>

          <template v-if="!nodeId">
            <a-dropdown v-if="nodeMap && Object.keys(nodeMap).length">
              <a-button type="primary" danger> {{ $t('pages.node.script-list.9a59feaf') }}<DownOutlined /></a-button>
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
            <SyncOutlined />{{ $t('pages.node.script-list.9a59feaf') }}
          </a-button>
        </a-space>
      </template>
      <template #tableHelp>
        <a-tooltip>
          <template #title>
            <div>{{ $t('pages.node.script-list.452c8b98') }}</div>

            <div>
              <ul>
                <li>{{ $t('pages.node.script-list.6bbddfa3') }}</li>
                <li>{{ $t('pages.node.script-list.e62fd265') }}</li>
                <li>{{ $t('pages.node.script-list.6c55e5d0') }}</li>
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
          <a-tag v-if="text === 'GLOBAL'">{{ $t('pages.node.script-list.f372618') }}</a-tag>
          <a-tag v-else>{{ $t('pages.node.script-list.afacc4cb') }}</a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'scriptType'">
          <a-tooltip v-if="text === 'server-sync'" :title="$t('pages.node.script-list.380d4731')">
            <ClusterOutlined />
          </a-tooltip>
          <a-tooltip v-else :title="$t('pages.node.script-list.a1f47198')">
            <FileTextOutlined />
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button size="small" type="primary" @click="handleExec(record)">{{
              $t('pages.node.script-list.d14160b')
            }}</a-button>
            <a-button size="small" type="primary" @click="handleLog(record)">{{
              $t('pages.node.script-list.2823935a')
            }}</a-button>
            <a-button size="small" type="primary" @click="handleTrigger(record)">{{
              $t('pages.node.script-list.e81c0988')
            }}</a-button>

            <a-dropdown>
              <a @click="(e) => e.preventDefault()">
                {{ $t('pages.node.script-list.6e071067') }}
                <DownOutlined />
              </a>
              <template #overlay>
                <a-menu>
                  <a-menu-item>
                    <!-- <a-button size="small" :type="`${record.scriptType === 'server-sync' ? '' : 'primary'}`" @click="handleEdit(record)">{{ record.scriptType === "server-sync" ? "查看" : " 编辑" }}</a-button> -->
                    <a-tooltip
                      :title="`${
                        record.scriptType === 'server-sync'
                          ? $t('pages.node.script-list.c0594515')
                          : $t('pages.node.script-list.2f14e7d4')
                      }`"
                    >
                      <a-button
                        size="small"
                        :disabled="record.scriptType === 'server-sync'"
                        type="primary"
                        danger
                        @click="handleDelete(record)"
                        >{{ $t('pages.node.script-list.2f14e7d4') }}</a-button
                      >
                    </a-tooltip>
                  </a-menu-item>
                  <a-menu-item>
                    <a-button size="small" type="primary" danger @click="handleUnbind(record)">{{
                      $t('pages.node.script-list.4c957529')
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
    <a-drawer
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
    </a-drawer>
    <!-- 脚本日志 -->
    <a-drawer
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
    </a-drawer>
    <!-- 触发器 -->
    <a-modal
      v-model:open="triggerVisible"
      destroy-on-close
      :title="$t('pages.node.script-list.e81c0988')"
      width="50%"
      :footer="null"
    >
      <a-form ref="editTriggerForm" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-tabs default-active-key="1">
          <template #rightExtra>
            <a-tooltip :title="$t('pages.node.script-list.5c0bb1c0')">
              <a-button type="primary" size="small" @click="resetTrigger">{{
                $t('pages.node.script-list.da1d2343')
              }}</a-button>
            </a-tooltip>
          </template>
          <a-tab-pane key="1" :tab="$t('pages.node.script-list.d14160b')">
            <a-space direction="vertical" style="width: 100%">
              <a-alert :message="$t('pages.node.script-list.c8dfae81')" type="warning" show-icon>
                <template #description>
                  <ul>
                    <li>{{ $t('pages.node.script-list.8f9bc485') }}</li>
                    <li>{{ $t('pages.node.script-list.21ae4cfc') }}</li>
                    <li>{{ $t('pages.node.script-list.789c025c') }}</li>
                    <li>{{ $t('pages.node.script-list.3f453ca7') }}</li>
                  </ul>
                </template>
              </a-alert>
              <a-alert
                type="info"
                :message="`${$t('pages.node.script-list.2cd9ba62')}(${$t('pages.node.script-list.da836fdd')})`"
              >
                <template #description>
                  <a-typography-paragraph :copyable="{ tooltip: false, text: temp.triggerUrl }">
                    <a-tag>GET</a-tag> <span>{{ temp.triggerUrl }} </span>
                  </a-typography-paragraph>
                </template>
              </a-alert>
              <a-alert
                type="info"
                :message="`${$t('pages.node.script-list.4bd083f4')}(${$t('pages.node.script-list.da836fdd')})`"
              >
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
    </a-modal>
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
          title: this.$t('pages.node.script-list.3e34ec28'),
          dataIndex: 'name',
          ellipsis: true,
          width: 200
        },
        {
          title: this.$t('pages.node.script-list.fa8d810f'),
          dataIndex: 'nodeName',
          ellipsis: true,
          width: 150,
          tooltip: true
        },
        {
          title: this.$t('pages.node.script-list.8d20cb3f'),
          dataIndex: 'workspaceName',
          ellipsis: true,
          width: 150,
          tooltip: true
        },
        {
          title: this.$t('pages.node.script-list.698bb532'),
          dataIndex: 'scriptType',
          width: 70,
          align: 'center',
          ellipsis: true
        },
        {
          title: this.$t('pages.node.script-list.65860154'),
          dataIndex: 'workspaceId',
          ellipsis: true,

          width: '90px'
        },
        {
          title: this.$t('pages.node.script-list.eeeaafc5'),
          dataIndex: 'autoExecCron',
          ellipsis: true,
          width: 120,
          tooltip: true
        },
        {
          title: this.$t('pages.node.script-list.a2b40316'),
          dataIndex: 'modifyTimeMillis',
          sorter: true,
          width: '170px',
          ellipsis: true,
          customRender: ({ text }) => parseTime(text)
        },
        {
          title: this.$t('pages.node.script-list.f5b90169'),
          dataIndex: 'createTimeMillis',
          sorter: true,
          width: '170px',
          ellipsis: true,
          customRender: ({ text }) => parseTime(text)
        },
        {
          title: this.$t('pages.node.script-list.db3c9202'),
          dataIndex: 'createUser',
          ellipsis: true,
          width: '120px'
        },
        {
          title: this.$t('pages.node.script-list.916db24b'),
          dataIndex: 'modifyUser',
          ellipsis: true,
          width: '120px'
        },
        {
          title: this.$t('pages.node.script-list.358d534a'),
          dataIndex: 'lastRunUser',
          ellipsis: true,
          width: '120px'
        },
        {
          title: this.$t('pages.node.script-list.3bb962bf'),
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
        title: this.$t('pages.node.script-list.e422d0eb'),
        zIndex: 1009,
        content: this.$t('pages.node.script-list.41a45994'),
        okText: this.$t('pages.node.script-list.7da4a591'),
        cancelText: this.$t('pages.node.script-list.43105e21'),
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
      this.drawerTitle = `${this.$t('pages.node.script-list.5139b7d7')}(${this.temp.name})`
      this.drawerConsoleVisible = true
    },
    handleLog(record) {
      this.temp = Object.assign({}, record)
      this.drawerTitle = `${this.$t('pages.node.script-list.2823935a')}(${this.temp.name})`
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
      <b style='font-size: 20px;'>${this.$t('pages.node.script-list.45c565a2')}</b>
      <ul style='font-size: 20px;color:red;font-weight: bold;'>
        <li>${this.$t('pages.node.script-list.93d36cc1')}</b></li>
        <li>${this.$t('pages.node.script-list.260e16bd')}</li>
        <li>${this.$t('pages.node.script-list.f9775b85')}</li>
      </ul>
      `
      $confirm({
        title: this.$t('pages.node.script-list.714aeea9'),
        zIndex: 1009,
        content: h('div', null, [h('p', { innerHTML: html }, null)]),
        okButtonProps: { props: { type: 'danger', size: 'small' } },
        cancelButtonProps: { props: { type: 'primary' } },
        okText: this.$t('pages.node.script-list.7da4a591'),
        cancelText: this.$t('pages.node.script-list.43105e21'),
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
