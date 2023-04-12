<template>
  <div class="full-content">
    <!-- 数据表格 -->
    <a-table
      :data-source="list"
      size="middle"
      :columns="columns"
      @change="changePage"
      :pagination="pagination"
      bordered
      rowKey="id"
      :row-selection="rowSelection"
    >
      <template slot="title">
        <a-space>
          <a-input
            v-model="listQuery['id']"
            placeholder="脚本ID"
            @pressEnter="loadData"
            allowClear
            class="search-input-item"
          />
          <a-input
            v-model="listQuery['%name%']"
            placeholder="名称"
            @pressEnter="loadData"
            allowClear
            class="search-input-item"
          />
          <a-input
            v-model="listQuery['%description%']"
            placeholder="描述"
            @pressEnter="loadData"
            class="search-input-item"
          />
          <a-input
            v-model="listQuery['%autoExecCron%']"
            placeholder="定时执行"
            @pressEnter="loadData"
            class="search-input-item"
          />
          <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
            <a-button :loading="loading" type="primary" @click="loadData">搜索</a-button>
          </a-tooltip>
          <a-button type="primary" @click="createScript">新建脚本</a-button>
          <a-button
            type="primary"
            v-if="choose === 'checkbox'"
            :disabled="!tableSelections || !tableSelections.length"
            @click="syncToWorkspaceShow"
            >工作空间同步</a-button
          >
          <a-tooltip>
            <template slot="title">
              <div>脚本模版是存储在服务端中的命令脚本用于在线管理一些脚本命令，如初始化软件环境、管理应用程序等</div>

              <div>
                <ul>
                  <li>执行时候默认不加载全部环境变量、需要脚本里面自行加载</li>
                  <li>命令文件将在 ${数据目录}/script/xxxx.sh、bat 执行</li>
                  <li>分发节点是指在编辑完脚本后自动将脚本内容同步节点的脚本,一般用户节点分发功能中的 DSL 模式</li>
                </ul>
              </div>
            </template>
            <a-icon type="question-circle" theme="filled" />
          </a-tooltip>
        </a-space>
      </template>

      <a-tooltip slot="nodeId" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ nodeMap[text] }}</span>
      </a-tooltip>
      <a-tooltip slot="tooltip" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <template slot="global" slot-scope="text">
        <a-tag v-if="text === 'GLOBAL'">全局</a-tag>
        <a-tag v-else>工作空间</a-tag>
      </template>
      <template slot="operation" slot-scope="text, record">
        <a-space>
          <template v-if="choose === 'checkbox'">
            <a-button size="small" type="primary" @click="handleExec(record)">执行</a-button>
            <a-button size="small" type="primary" @click="handleEdit(record)">编辑</a-button>
            <a-button size="small" type="primary" @click="handleLog(record)">日志</a-button>
            <a-dropdown>
              <a class="ant-dropdown-link" @click="(e) => e.preventDefault()">
                更多
                <a-icon type="down" />
              </a>
              <a-menu slot="overlay">
                <a-menu-item>
                  <a-button size="small" type="primary" @click="handleTrigger(record)">触发器</a-button>
                </a-menu-item>

                <a-menu-item>
                  <a-button size="small" type="danger" @click="handleDelete(record)">删除</a-button>
                </a-menu-item>
                <a-menu-item>
                  <a-button size="small" type="danger" :disabled="!record.nodeIds" @click="handleUnbind(record)"
                    >解绑</a-button
                  >
                </a-menu-item>
              </a-menu>
            </a-dropdown>
          </template>
          <template v-else>
            <a-button size="small" type="primary" @click="handleEdit(record)">编辑</a-button>
          </template>
        </a-space>
      </template>
    </a-table>
    <!-- 编辑区 -->
    <a-modal
      :zIndex="1009"
      destroyOnClose
      v-model="editScriptVisible"
      title="编辑 Script"
      @ok="handleEditScriptOk"
      :maskClosable="false"
      width="80vw"
    >
      <a-form ref="editScriptForm" :rules="rules" :model="temp" :label-col="{ span: 3 }" :wrapper-col="{ span: 19 }">
        <a-form-item v-if="temp.id" label="ScriptId" prop="id">
          <a-input v-model="temp.id" disabled readOnly />
        </a-form-item>
        <a-form-item label="Script 名称" prop="name">
          <a-input :maxLength="50" v-model="temp.name" placeholder="名称" />
        </a-form-item>
        <a-form-item label="Script 内容" prop="context">
          <div style="height: 40vh; overflow-y: scroll">
            <code-editor v-model="temp.context" :options="{ mode: 'shell', tabSize: 2, theme: 'abcdef' }"></code-editor>
          </div>
        </a-form-item>
        <!-- <a-form-item label="默认参数" prop="defArgs">
          <a-input v-model="temp.defArgs" placeholder="默认参数" />
        </a-form-item> -->
        <a-form-item label="默认参数">
          <div v-for="(item, index) in commandParams" :key="item.key">
            <a-row type="flex" justify="center" align="middle">
              <a-col :span="22">
                <a-input
                  :addon-before="`参数${index + 1}描述`"
                  v-model="item.desc"
                  placeholder="参数描述,参数描述没有实际作用,仅是用于提示参数的含义"
                />
                <a-input
                  :addon-before="`参数${index + 1}值`"
                  v-model="item.value"
                  placeholder="参数值,添加默认参数后在手动执行脚本时需要填写参数值"
                />
              </a-col>
              <a-col :span="2">
                <a-row type="flex" justify="center" align="middle">
                  <a-col>
                    <a-icon @click="() => commandParams.splice(index, 1)" type="minus-circle" style="color: #ff0000" />
                  </a-col>
                </a-row>
              </a-col>
            </a-row>
            <a-divider style="margin: 5px 0" />
          </div>

          <a-button type="primary" @click="() => commandParams.push({})">添加参数</a-button>
        </a-form-item>
        <a-form-item label="定时执行" prop="autoExecCron">
          <a-auto-complete
            v-model="temp.autoExecCron"
            placeholder="如果需要定时自动执行则填写,cron 表达式.默认未开启秒级别,需要去修改配置文件中:[system.timerMatchSecond]）"
            option-label-prop="value"
          >
            <template slot="dataSource">
              <a-select-opt-group v-for="group in cronDataSource" :key="group.title">
                <span slot="label">
                  {{ group.title }}
                </span>
                <a-select-option v-for="opt in group.children" :key="opt.title" :value="opt.value">
                  {{ opt.title }} {{ opt.value }}
                </a-select-option>
              </a-select-opt-group>
            </template>
          </a-auto-complete>
        </a-form-item>
        <a-form-item label="描述" prop="description">
          <a-input
            v-model="temp.description"
            :maxLength="200"
            type="textarea"
            :rows="3"
            style="resize: none"
            placeholder="详细描述"
          />
        </a-form-item>
        <a-form-item label="共享" prop="global">
          <a-radio-group v-model="temp.global">
            <a-radio :value="true"> 全局</a-radio>
            <a-radio :value="false"> 当前工作空间</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item v-if="temp.prohibitSync" label="禁用分发节点">
          <a-tag v-for="(item, index) in temp.nodeList" :key="index"
            >节点名称：{{ item.nodeName }} 工作空间：{{ item.workspaceName }}</a-tag
          >
        </a-form-item>
        <a-form-item v-else>
          <template slot="label">
            分发节点
            <a-tooltip v-show="!temp.id">
              <template slot="title"> 分发节点是指在编辑完脚本后自动将脚本内容同步节点的脚本中 </template>
              <a-icon type="question-circle" theme="filled" />
            </a-tooltip>
          </template>
          <a-select
            show-search
            option-filter-prop="children"
            placeholder="请选择分发到的节点"
            mode="multiple"
            v-model="temp.chooseNode"
          >
            <a-select-option v-for="item in nodeList" :key="item.id" :value="item.id">
              {{ item.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
    <!-- 脚本控制台组件 -->
    <a-drawer
      destroyOnClose
      :title="drawerTitle"
      placement="right"
      width="85vw"
      :visible="drawerConsoleVisible"
      @close="onConsoleClose"
    >
      <script-console v-if="drawerConsoleVisible" :defArgs="temp.defArgs" :id="temp.id" />
    </a-drawer>
    <!-- 同步到其他工作空间 -->
    <a-modal
      destroyOnClose
      v-model="syncToWorkspaceVisible"
      title="同步到其他工作空间"
      @ok="handleSyncToWorkspace"
      :maskClosable="false"
    >
      <a-alert message="温馨提示" type="warning">
        <template slot="description">
          <ul>
            <li>同步机制采用<b>脚本名称</b>确定是同一个脚本</li>
            <li>当目标工作空间不存在对应的 脚本 时候将自动创建一个新的 脚本</li>
            <li>当目标工作空间已经存在 脚本 时候将自动同步 脚本内容、默认参数、定时执行、描述</li>
          </ul>
        </template>
      </a-alert>
      <a-form :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
        <a-form-item> </a-form-item>
        <a-form-item label="选择工作空间" prop="workspaceId">
          <a-select show-search option-filter-prop="children" v-model="temp.workspaceId" placeholder="请选择工作空间">
            <a-select-option :disabled="getWorkspaceId === item.id" v-for="item in workspaceList" :key="item.id">{{
              item.name
            }}</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
    <!-- 触发器 -->
    <a-modal destroyOnClose v-model="triggerVisible" title="触发器" width="50%" :footer="null" :maskClosable="false">
      <a-form ref="editTriggerForm" :rules="rules" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-tabs default-active-key="1">
          <template slot="tabBarExtraContent">
            <a-tooltip title="重置触发器 token 信息,重置后之前的触发器 token 将失效">
              <a-button type="primary" size="small" @click="resetTrigger">重置</a-button>
            </a-tooltip>
          </template>
          <a-tab-pane key="1" tab="执行">
            <a-space style="display: block" direction="vertical" align="baseline">
              <a-alert message="温馨提示" type="warning">
                <template slot="description">
                  <ul>
                    <li>单个触发器地址中：第一个随机字符串为脚本ID，第二个随机字符串为 token</li>
                    <li>
                      重置为重新生成触发地址,重置成功后之前的触发器地址将失效,触发器绑定到生成触发器到操作人上,如果将对应的账号删除触发器将失效
                    </li>
                    <li>批量触发参数 BODY json： [ { "id":"1", "token":"a" } ]</li>
                  </ul>
                </template>
              </a-alert>
              <a-alert
                v-clipboard:copy="temp.triggerUrl"
                v-clipboard:success="
                  () => {
                    tempVue.prototype.$notification.success({ message: '复制成功' })
                  }
                "
                v-clipboard:error="
                  () => {
                    tempVue.prototype.$notification.error({ message: '复制失败' })
                  }
                "
                type="info"
                :message="`单个触发器地址(点击可以复制)`"
              >
                <template slot="description">
                  <a-tag>GET</a-tag> <span>{{ temp.triggerUrl }} </span>
                  <a-icon type="copy" />
                </template>
              </a-alert>
              <a-alert
                v-clipboard:copy="temp.batchTriggerUrl"
                v-clipboard:success="
                  () => {
                    tempVue.prototype.$notification.success({ message: '复制成功' })
                  }
                "
                v-clipboard:error="
                  () => {
                    tempVue.prototype.$notification.error({ message: '复制失败' })
                  }
                "
                type="info"
                :message="`批量触发器地址(点击可以复制)`"
              >
                <template slot="description">
                  <a-tag>POST</a-tag> <span>{{ temp.batchTriggerUrl }} </span>
                  <a-icon type="copy" />
                </template>
              </a-alert>
            </a-space>
          </a-tab-pane>
        </a-tabs>
      </a-form>
    </a-modal>
    <!-- 脚本日志 -->
    <a-drawer
      destroyOnClose
      title="脚本执行历史"
      width="50vw"
      :visible="drawerLogVisible"
      @close="
        () => {
          this.drawerLogVisible = false
        }
      "
    >
      <script-log v-if="drawerLogVisible" :scriptId="temp.id" />
    </a-drawer>
    <div style="padding-top: 50px" v-if="choose === 'radio'">
      <div
        :style="{
          position: 'absolute',
          right: 0,
          bottom: 0,
          width: '100%',
          borderTop: '1px solid #e9e9e9',
          padding: '10px 16px',
          background: '#fff',
          textAlign: 'right',
          zIndex: 1
        }"
      >
        <a-space>
          <a-button
            @click="
              () => {
                this.$emit('cancel')
              }
            "
          >
            取消
          </a-button>
          <a-button type="primary" @click="handerConfirm"> 确定 </a-button>
        </a-space>
      </div>
    </div>
  </div>
</template>
<script>
import {
  deleteScript,
  editScript,
  getScriptList,
  syncToWorkspace,
  unbindScript,
  getTriggerUrl,
  getScriptItem
} from '@/api/server-script'
import codeEditor from '@/components/codeEditor'
import { getNodeListAll } from '@/api/node'
import ScriptConsole from '@/pages/script/script-console'
import { CHANGE_PAGE, COMPUTED_PAGINATION, CRON_DATA_SOURCE, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'
import { mapGetters } from 'vuex'
import { getWorkSpaceListAll } from '@/api/workspace'
import Vue from 'vue'
import ScriptLog from '@/pages/script/script-log'
export default {
  components: {
    ScriptConsole,
    codeEditor,
    ScriptLog
  },
  props: {
    choose: {
      type: String,
      default: 'checkbox'
      // "radio" ,"checkbox"
    }
  },
  data() {
    return {
      // choose: this.choose,
      loading: false,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      cronDataSource: CRON_DATA_SOURCE,
      list: [],
      temp: {},
      nodeList: [],
      editScriptVisible: false,
      drawerTitle: '',
      drawerConsoleVisible: false,
      columns: [
        { title: 'id', dataIndex: 'id', ellipsis: true, width: 150, scopedSlots: { customRender: 'tooltip' } },
        { title: '名称', dataIndex: 'name', ellipsis: true, width: 150, scopedSlots: { customRender: 'tooltip' } },
        {
          title: '共享',
          dataIndex: 'workspaceId',
          ellipsis: true,
          scopedSlots: { customRender: 'global' },
          width: '90px'
        },
        {
          title: '描述',
          dataIndex: 'description',
          ellipsis: true,
          width: 300,
          scopedSlots: { customRender: 'tooltip' }
        },
        {
          title: '定时执行',
          dataIndex: 'autoExecCron',
          ellipsis: true,
          width: '100px',
          scopedSlots: { customRender: 'tooltip' }
        },
        {
          title: '修改时间',
          dataIndex: 'modifyTimeMillis',
          sorter: true,
          width: '170px',
          ellipsis: true,
          customRender: (text) => parseTime(text)
        },
        {
          title: '创建时间',
          dataIndex: 'createTimeMillis',
          sorter: true,
          width: '170px',
          ellipsis: true,
          customRender: (text) => parseTime(text)
        },
        {
          title: '创建人',
          dataIndex: 'createUser',
          ellipsis: true,
          scopedSlots: { customRender: 'tooltip' },
          width: '120px'
        },
        {
          title: '修改人',
          dataIndex: 'modifyUser',
          ellipsis: true,
          scopedSlots: { customRender: 'tooltip' },
          width: '120px'
        },
        {
          title: '最后执行人',
          dataIndex: 'lastRunUser',
          ellipsis: true,
          width: '120px',
          scopedSlots: { customRender: 'tooltip' }
        },
        this.choose === 'checkbox'
          ? {
              title: '操作',
              dataIndex: 'operation',
              align: 'center',
              scopedSlots: { customRender: 'operation' },
              fixed: 'right',
              width: '240px'
            }
          : {
              title: '操作',
              dataIndex: 'operation',
              align: 'center',
              scopedSlots: { customRender: 'operation' },
              fixed: 'right',
              width: '100px'
            }
      ],
      rules: {
        name: [{ required: true, message: '请输入脚本名称', trigger: 'blur' }],
        context: [{ required: true, message: '请输入脚本内容', trigger: 'blur' }]
      },
      tableSelections: [],
      syncToWorkspaceVisible: false,
      workspaceList: [],
      triggerVisible: false,
      commandParams: [],
      drawerLogVisible: false
    }
  },
  computed: {
    ...mapGetters(['getWorkspaceId']),
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery)
    },
    rowSelection() {
      return {
        onChange: (selectedRowKeys) => {
          this.tableSelections = selectedRowKeys
        },
        selectedRowKeys: this.tableSelections,
        type: this.choose
      }
    }
  },
  created() {
    // this.columns.push(
    // );
  },
  mounted() {
    // this.calcTableHeight();

    this.loadData()
  },
  methods: {
    // 加载数据
    loadData(pointerEvent) {
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
      this.loading = true
      getScriptList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result
          this.listQuery.total = res.data.total
        }
        this.loading = false
      })
    },
    parseTime,
    // 获取所有节点
    getAllNodeList() {
      getNodeListAll().then((res) => {
        this.nodeList = res.data || []
      })
    },
    createScript() {
      this.temp = {}
      this.commandParams = []
      this.editScriptVisible = true
      this.getAllNodeList()
    },
    // 修改
    handleEdit(record) {
      getScriptItem({
        id: record.id
      }).then((res) => {
        if (res.code === 200) {
          const data = res.data.data
          this.temp = Object.assign({}, data)

          this.commandParams = data?.defArgs ? JSON.parse(data.defArgs) : []

          this.temp = {
            ...this.temp,
            prohibitSync: res.data.prohibitSync,
            nodeList: res.data.nodeList,
            chooseNode: data?.nodeIds ? data.nodeIds.split(',') : [],
            global: data.workspaceId === 'GLOBAL',
            workspaceId: ''
          }
          this.editScriptVisible = true
          this.getAllNodeList()
        }
      })
    },
    // 提交 Script 数据
    handleEditScriptOk() {
      // 检验表单
      this.$refs['editScriptForm'].validate((valid) => {
        if (!valid) {
          return false
        }
        if (this.commandParams && this.commandParams.length > 0) {
          for (let i = 0; i < this.commandParams.length; i++) {
            if (!this.commandParams[i].desc) {
              this.$notification.error({
                message: '请填写第' + (i + 1) + '个参数的描述'
              })
              return false
            }
          }
          this.temp.defArgs = JSON.stringify(this.commandParams)
        } else {
          this.temp.defArgs = ''
        }
        // 提交数据
        this.temp.nodeIds = this.temp?.chooseNode?.join(',')
        delete this.temp.nodeList
        editScript(this.temp).then((res) => {
          if (res.code === 200) {
            // 成功
            this.$notification.success({
              message: res.msg
            })

            this.editScriptVisible = false
            this.loadData()
            this.$refs['editScriptForm'].resetFields()
          }
        })
      })
    },
    handleDelete(record) {
      this.$confirm({
        title: '系统提示',
        content: '真的要删除脚本么？',
        okText: '确认',
        cancelText: '取消',
        onOk: () => {
          // 组装参数
          const params = {
            id: record.id
          }
          // 删除
          deleteScript(params).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
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
      this.temp = Object.assign(record)
      this.drawerTitle = `控制台(${this.temp.name})`
      this.drawerConsoleVisible = true
    },
    // 关闭 console
    onConsoleClose() {
      this.drawerConsoleVisible = false
    },
    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter })
      this.loadData()
    },
    // 解绑
    handleUnbind(record) {
      const html =
        "<b style='font-size: 20px;'>真的要解绑脚本关联的节点么？</b>" +
        "<ul style='font-size: 20px;color:red;font-weight: bold;'>" +
        '<li>解绑不会真实请求节点删除脚本信息</b></li>' +
        '<li>一般用于服务器无法连接且已经确定不再使用</li>' +
        '<li>如果误操作会产生冗余数据！！！</li>' +
        ' </ul>'

      const h = this.$createElement
      this.$confirm({
        title: '危险操作！！！',
        content: h('div', null, [h('p', { domProps: { innerHTML: html } }, null)]),
        okButtonProps: { props: { type: 'danger', size: 'small' } },
        cancelButtonProps: { props: { type: 'primary' } },
        okText: '确认',
        cancelText: '取消',
        onOk: () => {
          // 解绑
          unbindScript({
            id: record.id
          }).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg
              })
              this.loadData()
            }
          })
        }
      })
    },
    // 加载工作空间数据
    loadWorkSpaceListAll() {
      getWorkSpaceListAll().then((res) => {
        if (res.code === 200) {
          this.workspaceList = res.data
        }
      })
    },
    // 同步到其他工作情况
    syncToWorkspaceShow() {
      this.syncToWorkspaceVisible = true
      this.loadWorkSpaceListAll()
      this.temp = {
        workspaceId: undefined
      }
    },
    //
    handleSyncToWorkspace() {
      if (!this.temp.workspaceId) {
        this.$notification.warn({
          message: '请选择工作空间'
        })
        return false
      }
      // 同步
      syncToWorkspace({
        ids: this.tableSelections.join(','),
        toWorkspaceId: this.temp.workspaceId
      }).then((res) => {
        if (res.code === 200) {
          this.$notification.success({
            message: res.msg
          })
          this.tableSelections = []
          this.syncToWorkspaceVisible = false
          return false
        }
      })
    },
    // 触发器
    handleTrigger(record) {
      this.temp = Object.assign({}, record)
      this.tempVue = Vue
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
          this.$notification.success({
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
    handleLog(record) {
      this.temp = Object.assign({}, record)

      this.drawerLogVisible = true
    },
    handerConfirm() {
      if (!this.tableSelections.length) {
        this.$notification.warning({
          message: '请选择要使用的脚本'
        })
        return
      }
      const selectData = this.list.filter((item) => {
        return item.id === this.tableSelections[0]
      })[0]

      this.$emit('confirm', `${selectData.id}`)
    }
  }
}
</script>
<style scoped></style>
