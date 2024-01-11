<template>
  <div class="">
    <!-- 数据表格 -->
    <a-table
      :data-source="list"
      size="middle"
      :columns="columns"
      @change="changePage"
      :pagination="pagination"
      bordered
      rowKey="id"
      :scroll="{
        x: 'max-content'
      }"
    >
      <template v-slot:title>
        <a-space>
          <a-select
            v-if="!nodeId"
            v-model:value="listQuery.nodeId"
            allowClear
            placeholder="请选择节点"
            class="search-input-item"
          >
            <a-select-option v-for="(nodeName, key) in nodeMap" :key="key">{{ nodeName }}</a-select-option>
          </a-select>
          <a-input
            v-model:value="listQuery['%name%']"
            @pressEnter="loadData"
            placeholder="名称"
            allowClear
            class="search-input-item"
          />
          <a-input
            v-model:value="listQuery['%autoExecCron%']"
            @pressEnter="loadData"
            placeholder="定时执行"
            class="search-input-item"
          />
          <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
            <a-button :loading="loading" type="primary" @click="loadData">搜索</a-button>
          </a-tooltip>

          <a-button type="primary" @click="handleEdit()">新增</a-button>

          <template v-if="!nodeId">
            <a-dropdown v-if="nodeMap && Object.keys(nodeMap).length">
              <a-button type="primary" danger> 同步缓存<DownOutlined /></a-button>
              <template v-slot:overlay>
                <a-menu>
                  <a-menu-item v-for="(nodeName, key) in nodeMap" :key="key" @click="sync(key)">
                    <a href="javascript:;">{{ nodeName }} <SyncOutlined /></a>
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </template>
          <a-button v-else type="primary" danger @click="sync(nodeId)"> <SyncOutlined />同步缓存 </a-button>

          <a-tooltip>
            <template v-slot:title>
              <div>节点脚本模版是存储在节点中的命令脚本用于在线管理一些脚本命令，如初始化软件环境、管理应用程序等</div>

              <div>
                <ul>
                  <li>执行时候默认不加载全部环境变量、需要脚本里面自行加载</li>
                  <li>命令文件将在 ${插件端数据目录}/script/xxxx.sh 、bat 执行</li>
                  <li>新增脚本模版需要到节点管理中去新增</li>
                </ul>
              </div>
            </template>
            <QuestionCircleOutlined />
          </a-tooltip>
        </a-space>
      </template>
      <template #bodyCell="{ column, text, record, index }">
        <template v-if="column.tooltip">
          <a-tooltip placement="topLeft" :title="text">
            <span>{{ text }}</span>
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'name'">
          <a-tooltip @click="handleEdit(record)" placement="topLeft" :title="text">
            <!-- <span>{{ text }}</span> -->
            <a-button type="link" style="padding: 0" size="small">{{ text }}</a-button>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'workspaceId'">
          <a-tag v-if="text === 'GLOBAL'">全局</a-tag>
          <a-tag v-else>工作空间</a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'scriptType'">
          <a-tooltip v-if="text === 'server-sync'" title="服务端分发的脚本">
            <ClusterOutlined />
          </a-tooltip>
          <a-tooltip v-else title="本地脚本">
            <FileTextOutlined />
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button size="small" type="primary" @click="handleExec(record)">执行</a-button>
            <a-button size="small" type="primary" @click="handleLog(record)">日志</a-button>
            <a-button size="small" type="primary" @click="handleTrigger(record)">触发器</a-button>

            <a-dropdown>
              <a @click="(e) => e.preventDefault()">
                更多
                <DownOutlined />
              </a>
              <template v-slot:overlay>
                <a-menu>
                  <a-menu-item>
                    <!-- <a-button size="small" :type="`${record.scriptType === 'server-sync' ? '' : 'primary'}`" @click="handleEdit(record)">{{ record.scriptType === "server-sync" ? "查看" : " 编辑" }}</a-button> -->
                    <a-tooltip
                      :title="`${
                        record.scriptType === 'server-sync'
                          ? '服务端分发同步的脚本不能直接删除,需要到服务端去操作'
                          : '删除'
                      }`"
                    >
                      <a-button
                        size="small"
                        :disabled="record.scriptType === 'server-sync'"
                        type="primary"
                        danger
                        @click="handleDelete(record)"
                        >删除</a-button
                      >
                    </a-tooltip>
                  </a-menu-item>
                  <a-menu-item>
                    <a-button size="small" type="primary" danger @click="handleUnbind(record)">解绑</a-button>
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
        </template>
      </template>
    </a-table>
    <!-- 编辑区 -->
    <ScriptEdit
      v-if="editScriptVisible"
      :nodeId="temp.nodeId"
      :scriptId="temp.scriptId"
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
          this.drawerConsoleVisible = false
        }
      "
    >
      <script-console
        v-if="drawerConsoleVisible"
        :nodeId="temp.nodeId"
        :defArgs="temp.defArgs"
        :id="temp.id"
        :scriptId="temp.scriptId"
      />
    </a-drawer>
    <!-- 脚本日志 -->
    <a-drawer
      destroyOnClose
      :title="drawerTitle"
      width="50vw"
      :open="drawerLogVisible"
      @close="
        () => {
          this.drawerLogVisible = false
        }
      "
    >
      <script-log v-if="drawerLogVisible" :scriptId="temp.scriptId" :nodeId="temp.nodeId" />
    </a-drawer>
    <!-- 触发器 -->
    <a-modal destroyOnClose v-model:open="triggerVisible" title="触发器" width="50%" :footer="null">
      <a-form ref="editTriggerForm" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-tabs default-active-key="1">
          <template v-slot:rightExtra>
            <a-tooltip title="重置触发器 token 信息,重置后之前的触发器 token 将失效">
              <a-button type="primary" size="small" @click="resetTrigger">重置</a-button>
            </a-tooltip>
          </template>
          <a-tab-pane key="1" tab="执行">
            <a-space direction="vertical" style="width: 100%">
              <a-alert message="温馨提示" type="warning" show-icon>
                <template v-slot:description>
                  <ul>
                    <li>单个触发器地址中：第一个随机字符串为脚本ID，第二个随机字符串为 token</li>
                    <li>
                      重置为重新生成触发地址,重置成功后之前的触发器地址将失效,触发器绑定到生成触发器到操作人上,如果将对应的账号删除触发器将失效
                    </li>
                    <li>批量触发参数 BODY json： [ { "id":"1", "token":"a" } ]</li>
                  </ul>
                </template>
              </a-alert>
              <a-alert type="info" :message="`单个触发器地址(点击可以复制)`">
                <template v-slot:description>
                  <a-typography-paragraph :copyable="{ tooltip: false, text: temp.triggerUrl }">
                    <a-tag>GET</a-tag> <span>{{ temp.triggerUrl }} </span>
                  </a-typography-paragraph>
                </template>
              </a-alert>
              <a-alert type="info" :message="`批量触发器地址(点击可以复制)`">
                <template v-slot:description>
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
          title: '名称',
          dataIndex: 'name',
          ellipsis: true,
          width: 200
        },
        {
          title: '节点名称',
          dataIndex: 'nodeName',
          ellipsis: true,
          width: 150,
          tooltip: true
        },
        {
          title: '工作空间名称',
          dataIndex: 'workspaceName',
          ellipsis: true,
          width: 150,
          tooltip: true
        },
        {
          title: '类型',
          dataIndex: 'scriptType',
          width: 70,
          align: 'center',
          ellipsis: true
        },
        {
          title: '共享',
          dataIndex: 'workspaceId',
          ellipsis: true,

          width: '90px'
        },
        {
          title: '定时执行',
          dataIndex: 'autoExecCron',
          ellipsis: true,
          width: 120,
          tooltip: true
        },
        {
          title: '修改时间',
          dataIndex: 'modifyTimeMillis',
          sorter: true,
          width: '170px',
          ellipsis: true,
          customRender: ({ text }) => parseTime(text)
        },
        {
          title: '创建时间',
          dataIndex: 'createTimeMillis',
          sorter: true,
          width: '170px',
          ellipsis: true,
          customRender: ({ text }) => parseTime(text)
        },
        {
          title: '创建人',
          dataIndex: 'createUser',
          ellipsis: true,
          ellipsis: true,
          width: '120px'
        },
        {
          title: '修改人',
          dataIndex: 'modifyUser',
          ellipsis: true,
          ellipsis: true,
          width: '120px'
        },
        {
          title: '最后操作人',
          dataIndex: 'lastRunUser',
          ellipsis: true,
          ellipsis: true,
          width: '120px'
        },
        {
          title: '操作',
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
      const that = this
      this.$confirm({
        title: '系统提示',
        zIndex: 1009,
        content: '真的要删除脚本么？',
        okText: '确认',
        cancelText: '取消',
        async onOk() {
          return await new Promise((resolve, reject) => {
            // 组装参数
            const params = {
              nodeId: record.nodeId,
              id: record.scriptId
            }
            // 删除
            deleteScript(params)
              .then((res) => {
                if (res.code === 200) {
                  $notification.success({
                    message: res.msg
                  })
                  that.loadData()
                }
                resolve()
              })
              .catch(reject)
          })
        }
      })
    },
    // 执行 Script
    handleExec(record) {
      this.temp = Object.assign({}, record)
      this.drawerTitle = `控制台(${this.temp.name})`
      this.drawerConsoleVisible = true
    },
    handleLog(record) {
      this.temp = Object.assign({}, record)
      this.drawerTitle = `日志(${this.temp.name})`
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
      const html =
        "<b style='font-size: 20px;'>真的要解绑节点脚本么？</b>" +
        "<ul style='font-size: 20px;color:red;font-weight: bold;'>" +
        '<li>解绑不会真实请求节点删除脚本信息</b></li>' +
        '<li>一般用于服务器无法连接且已经确定不再使用</li>' +
        '<li>如果误操作会产生冗余数据！！！</li>' +
        ' </ul>'
      const that = this
      this.$confirm({
        title: '危险操作！！！',
        zIndex: 1009,
        content: h('div', null, [h('p', { innerHTML: html }, null)]),
        okButtonProps: { props: { type: 'danger', size: 'small' } },
        cancelButtonProps: { props: { type: 'primary' } },
        okText: '确认',
        cancelText: '取消',
        async onOk() {
          return await new Promise((resolve, reject) => {
            // 解绑
            unbindScript({
              id: record.id
            })
              .then((res) => {
                if (res.code === 200) {
                  $notification.success({
                    message: res.msg
                  })
                  that.loadData()
                }
                resolve()
              })
              .catch(reject)
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
