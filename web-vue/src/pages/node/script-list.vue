<template>
  <div class="">
    <!-- 数据表格 -->
    <CustomTable
      is-show-tools
      default-auto-refresh
      :auto-refresh-time="30"
      :active-page="activePage"
      table-name="node-script-list"
      :empty-description="$tl('p.noNodeScript')"
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
            :placeholder="$tl('p.selectNode')"
            class="search-input-item"
          >
            <a-select-option v-for="(nodeName, key) in nodeMap" :key="key">{{ nodeName }}</a-select-option>
          </a-select>
          <a-input
            v-model:value="listQuery['%name%']"
            :placeholder="$tl('c.name')"
            allow-clear
            class="search-input-item"
            @press-enter="loadData"
          />
          <a-input
            v-model:value="listQuery['%autoExecCron%']"
            :placeholder="$tl('c.scheduling')"
            class="search-input-item"
            @press-enter="loadData"
          />
          <a-tooltip :title="$tl('p.quickBack')">
            <a-button :loading="loading" type="primary" @click="loadData">{{ $tl('p.search') }}</a-button>
          </a-tooltip>

          <a-button type="primary" @click="handleEdit()">{{ $tl('p.add') }}</a-button>

          <template v-if="!nodeId">
            <a-dropdown v-if="nodeMap && Object.keys(nodeMap).length">
              <a-button type="primary" danger> {{ $tl('c.syncCache') }}<DownOutlined /></a-button>
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
            <SyncOutlined />{{ $tl('c.syncCache') }}
          </a-button>
        </a-space>
      </template>
      <template #tableHelp>
        <a-tooltip>
          <template #title>
            <div>{{ $tl('p.nodeScriptTemplateDescription') }}</div>

            <div>
              <ul>
                <li>{{ $tl('p.loadEnv') }}</li>
                <li>{{ $tl('p.commandFilePath') }}</li>
                <li>{{ $tl('p.addScriptTemplate') }}</li>
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
          <a-tag v-if="text === 'GLOBAL'">{{ $tl('p.global') }}</a-tag>
          <a-tag v-else>{{ $tl('p.workspace') }}</a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'scriptType'">
          <a-tooltip v-if="text === 'server-sync'" :title="$tl('p.serverScript')">
            <ClusterOutlined />
          </a-tooltip>
          <a-tooltip v-else :title="$tl('p.localScript')">
            <FileTextOutlined />
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button size="small" type="primary" @click="handleExec(record)">{{ $tl('c.execute') }}</a-button>
            <a-button size="small" type="primary" @click="handleLog(record)">{{ $tl('c.log') }}</a-button>
            <a-button size="small" type="primary" @click="handleTrigger(record)">{{ $tl('c.trigger') }}</a-button>

            <a-dropdown>
              <a @click="(e) => e.preventDefault()">
                {{ $tl('p.more') }}
                <DownOutlined />
              </a>
              <template #overlay>
                <a-menu>
                  <a-menu-item>
                    <!-- <a-button size="small" :type="`${record.scriptType === 'server-sync' ? '' : 'primary'}`" @click="handleEdit(record)">{{ record.scriptType === "server-sync" ? "查看" : " 编辑" }}</a-button> -->
                    <a-tooltip
                      :title="`${record.scriptType === 'server-sync' ? $tl('p.serverScriptDelete') : $tl('c.delete')}`"
                    >
                      <a-button
                        size="small"
                        :disabled="record.scriptType === 'server-sync'"
                        type="primary"
                        danger
                        @click="handleDelete(record)"
                        >{{ $tl('c.delete') }}</a-button
                      >
                    </a-tooltip>
                  </a-menu-item>
                  <a-menu-item>
                    <a-button size="small" type="primary" danger @click="handleUnbind(record)">{{
                      $tl('p.unbind')
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
    <a-modal v-model:open="triggerVisible" destroy-on-close :title="$tl('c.trigger')" width="50%" :footer="null">
      <a-form ref="editTriggerForm" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-tabs default-active-key="1">
          <template #rightExtra>
            <a-tooltip :title="$tl('p.resetToken')">
              <a-button type="primary" size="small" @click="resetTrigger">{{ $tl('p.reset') }}</a-button>
            </a-tooltip>
          </template>
          <a-tab-pane key="1" :tab="$tl('c.execute')">
            <a-space direction="vertical" style="width: 100%">
              <a-alert :message="$tl('p.tip')" type="warning" show-icon>
                <template #description>
                  <ul>
                    <li>{{ $tl('p.triggerAddressInfo') }}</li>
                    <li>{{ $tl('p.resetTriggerAddress') }}</li>
                    <li>{{ $tl('p.batchTriggerParams') }}</li>
                    <li>{{ $tl('p.triggerParamEnv') }}</li>
                  </ul>
                </template>
              </a-alert>
              <a-alert type="info" :message="`${$tl('p.singleTriggerAddress')}(${$tl('c.copyOnClick')})`">
                <template #description>
                  <a-typography-paragraph :copyable="{ tooltip: false, text: temp.triggerUrl }">
                    <a-tag>GET</a-tag> <span>{{ temp.triggerUrl }} </span>
                  </a-typography-paragraph>
                </template>
              </a-alert>
              <a-alert type="info" :message="`${$tl('p.batchTriggerAddress')}(${$tl('c.copyOnClick')})`">
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
          title: this.$tl('c.name'),
          dataIndex: 'name',
          ellipsis: true,
          width: 200
        },
        {
          title: this.$tl('p.nodeName'),
          dataIndex: 'nodeName',
          ellipsis: true,
          width: 150,
          tooltip: true
        },
        {
          title: this.$tl('p.workspaceName'),
          dataIndex: 'workspaceName',
          ellipsis: true,
          width: 150,
          tooltip: true
        },
        {
          title: this.$tl('p.type'),
          dataIndex: 'scriptType',
          width: 70,
          align: 'center',
          ellipsis: true
        },
        {
          title: this.$tl('p.share'),
          dataIndex: 'workspaceId',
          ellipsis: true,

          width: '90px'
        },
        {
          title: this.$tl('c.scheduling'),
          dataIndex: 'autoExecCron',
          ellipsis: true,
          width: 120,
          tooltip: true
        },
        {
          title: this.$tl('p.modifyTime'),
          dataIndex: 'modifyTimeMillis',
          sorter: true,
          width: '170px',
          ellipsis: true,
          customRender: ({ text }) => parseTime(text)
        },
        {
          title: this.$tl('p.createTime'),
          dataIndex: 'createTimeMillis',
          sorter: true,
          width: '170px',
          ellipsis: true,
          customRender: ({ text }) => parseTime(text)
        },
        {
          title: this.$tl('p.creator'),
          dataIndex: 'createUser',
          ellipsis: true,
          width: '120px'
        },
        {
          title: this.$tl('p.modifier'),
          dataIndex: 'modifyUser',
          ellipsis: true,
          width: '120px'
        },
        {
          title: this.$tl('p.lastOperator'),
          dataIndex: 'lastRunUser',
          ellipsis: true,
          width: '120px'
        },
        {
          title: this.$tl('p.operation'),
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
    $tl(key, ...args) {
      return this.$t(`pages.node.scriptList.${key}`, ...args)
    },
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
        title: this.$tl('p.systemTip'),
        zIndex: 1009,
        content: this.$tl('p.confirmation'),
        okText: this.$tl('c.confirm'),
        cancelText: this.$tl('c.cancel'),
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
      this.drawerTitle = `${this.$tl('p.console')}(${this.temp.name})`
      this.drawerConsoleVisible = true
    },
    handleLog(record) {
      this.temp = Object.assign({}, record)
      this.drawerTitle = `${this.$tl('c.log')}(${this.temp.name})`
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
      <b style='font-size: 20px;'>${this.$tl('p.unbindConfirmation')}</b>
      <ul style='font-size: 20px;color:red;font-weight: bold;'>
        <li>${this.$tl('p.unbindNote')}</b></li>
        <li>${this.$tl('p.unbindUsage')}</li>
        <li>${this.$tl('p.misoperationWarning')}</li>
      </ul>
      `
      $confirm({
        title: this.$tl('p.dangerWarning'),
        zIndex: 1009,
        content: h('div', null, [h('p', { innerHTML: html }, null)]),
        okButtonProps: { props: { type: 'danger', size: 'small' } },
        cancelButtonProps: { props: { type: 'primary' } },
        okText: this.$tl('c.confirm'),
        cancelText: this.$tl('c.cancel'),
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
