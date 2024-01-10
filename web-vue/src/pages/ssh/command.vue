<template>
  <div>
    <a-table
      :data-source="commandList"
      :columns="columns"
      size="middle"
      bordered
      :pagination="pagination"
      @change="changePage"
      :row-selection="rowSelection"
      rowKey="id"
      :scroll="{
        x: 'max-content'
      }"
    >
      <template v-slot:title>
        <a-space>
          <a-input
            v-model:value="listQuery['%name%']"
            @pressEnter="getCommandData"
            placeholder="搜索命令"
            class="search-input-item"
          />
          <a-input
            v-model:value="listQuery['%desc%']"
            @pressEnter="getCommandData"
            placeholder="描述"
            class="search-input-item"
          />
          <a-input
            v-model:value="listQuery['%autoExecCron%']"
            @pressEnter="getCommandData"
            placeholder="定时执行"
            class="search-input-item"
          />
          <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
            <a-button type="primary" :loading="loading" @click="getCommandData">搜索</a-button>
          </a-tooltip>
          <a-button type="primary" @click="createCommand">新建命令</a-button>
          <a-dropdown>
            <a @click="(e) => e.preventDefault()"> 更多 <DownOutlined /> </a>
            <template v-slot:overlay>
              <a-menu>
                <a-menu-item>
                  <a-button
                    type="primary"
                    :disabled="!tableSelections || !tableSelections.length"
                    @click="syncToWorkspaceShow"
                    >工作空间同步</a-button
                  >
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
          <a-tooltip>
            <template v-slot:title>
              <div>命令模版是用于在线管理一些脚本命令，如初始化软件环境、管理应用程序等</div>

              <div>
                <ul>
                  <li>命令内容支持工作空间环境变量</li>
                  <li>
                    执行命令将自动替换为 sh
                    命令文件、并自动加载环境变量：/etc/profile、/etc/bashrc、~/.bashrc、~/.bash_profile
                  </li>
                  <li>
                    执行命令包含：<b>#disabled-template-auto-evn</b>
                    将取消自动加载环境变量(注意是整行不能包含空格)
                  </li>
                  <li>命令文件将上传至 ${user.home}/.jpom/xxxx.sh 执行完成将自动删除</li>
                </ul>
              </div>
            </template>
            <QuestionCircleOutlined />
          </a-tooltip>
        </a-space>
      </template>
      <template #bodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'name'">
          <a-tooltip placement="topLeft" :title="text">
            <span>{{ text }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'desc'">
          <a-tooltip placement="topLeft" :title="text">
            <span>{{ text }}</span>
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button size="small" type="primary" @click="handleExecute(record)">执行</a-button>
            <a-button size="small" type="primary" @click="handleEdit(record)">编辑</a-button>
            <a-button size="small" type="primary" @click="handleTrigger(record)">触发器</a-button>
            <a-button size="small" type="primary" danger @click="handleDelete(record)">删除</a-button>
          </a-space>
        </template>
      </template>
    </a-table>
    <!-- 编辑命令 -->
    <a-modal
      destroyOnClose
      v-model:open="editCommandVisible"
      width="80vw"
      title="编辑 命令"
      @ok="handleEditCommandOk"
      :maskClosable="false"
      :confirmLoading="confirmLoading"
    >
      <a-form ref="editCommandForm" :rules="rules" :model="temp" :label-col="{ span: 3 }" :wrapper-col="{ span: 20 }">
        <a-form-item label="命令名称" name="name">
          <a-input v-model:value="temp.name" :maxLength="100" placeholder="命令名称" />
        </a-form-item>

        <a-form-item
          name="command"
          help="脚本存放路径：${user.home}/.jpom/xxxx.sh，执行脚本路径：${user.home}，执行脚本方式：bash ${user.home}/.jpom/xxxx.sh par1 par2"
        >
          <template v-slot:label>
            <a-tooltip>
              命令内容
              <template v-slot:title>
                <ul>
                  <li>可以引用工作空间的环境变量 变量占位符 ${xxxx} xxxx 为变量名称</li>
                </ul>
              </template>
              <QuestionCircleOutlined v-show="!temp.id" />
            </a-tooltip>
          </template>
          <div style="height: 40vh; overflow-y: scroll">
            <a-form-item-rest>
              <code-editor
                v-model:content="temp.command"
                :options="{ mode: 'shell', tabSize: 2, theme: 'abcdef' }"
              ></code-editor>
            </a-form-item-rest>
          </div>
        </a-form-item>
        <a-form-item label="SSH节点">
          <a-select
            show-search
            option-filter-prop="children"
            placeholder="请选择SSH节点"
            mode="multiple"
            v-model:value="chooseSsh"
          >
            <a-select-option v-for="item in sshList" :key="item.id" :value="item.id">
              {{ item.name }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="默认参数">
          <a-form-item-rest>
            <a-space direction="vertical" style="width: 100%">
              <a-row v-for="(item, index) in commandParams" :key="item.key">
                <a-col :span="22">
                  <a-space direction="vertical" style="width: 100%">
                    <a-input
                      :addon-before="`参数${index + 1}描述`"
                      v-model:value="item.desc"
                      placeholder="参数描述,参数描述没有实际作用,仅是用于提示参数的含义"
                    />
                    <a-input
                      :addon-before="`参数${index + 1}值`"
                      v-model:value="item.value"
                      placeholder="参数值,添加默认参数后在手动执行脚本时需要填写参数值"
                    />
                  </a-space>
                </a-col>

                <a-col :span="2">
                  <a-row type="flex" justify="center" align="middle">
                    <a-col>
                      <MinusCircleOutlined @click="() => commandParams.splice(index, 1)" style="color: #ff0000" />
                    </a-col>
                  </a-row>
                </a-col>
              </a-row>
              <a-button type="primary" @click="() => commandParams.push({})">添加参数</a-button>
            </a-space>
          </a-form-item-rest>
        </a-form-item>
        <a-form-item label="自动执行" name="autoExecCron">
          <a-auto-complete
            v-model:value="temp.autoExecCron"
            placeholder="如果需要定时自动执行则填写,cron 表达式.默认未开启秒级别,需要去修改配置文件中:[system.timerMatchSecond]）"
            :options="CRON_DATA_SOURCE"
          >
            <template #option="item"> {{ item.title }} {{ item.value }} </template>
          </a-auto-complete>
        </a-form-item>
        <a-form-item label="命令描述" name="desc">
          <a-textarea
            v-model:value="temp.desc"
            :maxLength="255"
            :rows="3"
            style="resize: none"
            placeholder="命令详细描述"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      destroyOnClose
      v-model:open="executeCommandVisible"
      width="600px"
      title="执行 命令"
      @ok="handleExecuteCommandOk"
      :maskClosable="false"
      :confirmLoading="confirmLoading"
    >
      <a-form :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="命令名称" name="name">
          <a-input v-model:value="temp.name" :disabled="true" placeholder="命令名称" />
        </a-form-item>

        <a-form-item label="SSH节点" required>
          <a-select
            show-search
            option-filter-prop="children"
            mode="multiple"
            v-model:value="chooseSsh"
            placeholder="请选择 SSH节点"
          >
            <a-select-option v-for="item in sshList" :key="item.id" :value="item.id">
              {{ item.name }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item
          label="命令参数"
          :help="`${
            commandParams.length
              ? '所有参数将拼接成字符串以空格分隔形式执行脚本,需要注意参数顺序和未填写值的参数将自动忽略'
              : ''
          }`"
        >
          <a-space direction="vertical" style="width: 100%">
            <a-row v-for="(item, index) in commandParams" :key="item.key">
              <a-col :span="22">
                <a-input
                  :addon-before="`参数${index + 1}值`"
                  v-model:value="item.value"
                  :placeholder="`参数值 ${item.desc ? ',' + item.desc : ''}`"
                >
                  <template v-slot:suffix>
                    <a-tooltip v-if="item.desc" :title="item.desc">
                      <InfoCircleOutlined />
                    </a-tooltip>
                  </template>
                </a-input>
              </a-col>

              <a-col v-if="!item.desc" :span="2">
                <a-row type="flex" justify="center" align="middle">
                  <a-col>
                    <MinusCircleOutlined style="color: #ff0000" @click="() => commandParams.splice(index, 1)" />
                  </a-col>
                </a-row>
              </a-col>
            </a-row>
            <a-button type="primary" @click="() => commandParams.push({})">添加参数</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-modal>
    <!-- 执行日志 -->
    <a-modal
      destroyOnClose
      :width="'80vw'"
      v-model:open="logVisible"
      title="执行日志"
      :footer="null"
      :maskClosable="false"
    >
      <command-log v-if="logVisible" :temp="temp" />
    </a-modal>
    <!-- 同步到其他工作空间 -->
    <a-modal
      destroyOnClose
      :confirmLoading="confirmLoading"
      v-model:open="syncToWorkspaceVisible"
      title="同步到其他工作空间"
      @ok="handleSyncToWorkspace"
      :maskClosable="false"
    >
      <a-alert message="温馨提示" type="warning" show-icon>
        <template v-slot:description>
          <ul>
            <li>同步机制采用<b>脚本名称</b>确定是同一个脚本</li>
            <li>当目标工作空间不存在对应的 脚本 时候将自动创建一个新的 脚本</li>
            <li>当目标工作空间已经存在 脚本 时候将自动同步 脚本内容、默认参数、自动执行、描述</li>
          </ul>
        </template>
      </a-alert>
      <a-form :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
        <a-form-item> </a-form-item>
        <a-form-item label="选择工作空间" name="workspaceId">
          <a-select
            show-search
            option-filter-prop="children"
            v-model:value="temp.workspaceId"
            placeholder="请选择工作空间"
          >
            <a-select-option :disabled="getWorkspaceId() === item.id" v-for="item in workspaceList" :key="item.id">{{
              item.name
            }}</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 触发器 -->
    <a-modal
      destroyOnClose
      v-model:open="triggerVisible"
      title="触发器"
      width="50%"
      :footer="null"
      :maskClosable="false"
    >
      <a-form ref="editTriggerForm" :rules="rules" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-tabs default-active-key="1">
          <template v-slot:rightExtra>
            <a-tooltip title="重置触发器 token 信息,重置后之前的触发器 token 将失效">
              <a-button type="primary" size="small" @click="resetTrigger">重置</a-button>
            </a-tooltip>
          </template>
          <a-tab-pane key="1" tab="执行">
            <a-space direction="vertical" style="width: 100%">
              <a-alert message="温馨提示" type="warning">
                <template v-slot:description>
                  <ul>
                    <li>单个触发器地址中：第一个随机字符串为命令脚本ID，第二个随机字符串为 token</li>
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
import { deleteCommand, editCommand, executeBatch, getCommandList, syncToWorkspace, getTriggerUrl } from '@/api/command'
import { CHANGE_PAGE, COMPUTED_PAGINATION, CRON_DATA_SOURCE, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'
import { getSshListAll } from '@/api/ssh'
import codeEditor from '@/components/codeEditor'
import CommandLog from './command-view-log'

import { getWorkSpaceListAll } from '@/api/workspace'
import { mapState } from 'pinia'
import { useAppStore } from '@/stores/app'
export default {
  components: { codeEditor, CommandLog },
  data() {
    return {
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      CRON_DATA_SOURCE,
      commandList: [],
      loading: false,
      editCommandVisible: false,
      executeCommandVisible: false,
      commandParams: [],
      sshList: [],
      chooseSsh: [],
      temp: {},
      logVisible: false,
      rules: {
        name: [{ required: true, message: 'Please input name', trigger: 'blur' }],
        command: [{ required: true, message: 'Please input command', trigger: 'blur' }]
      },
      columns: [
        {
          title: '命令名称',
          dataIndex: 'name',
          ellipsis: true,
          width: 200
        },
        {
          title: '命令描述',
          dataIndex: 'desc',
          ellipsis: true,
          width: 250
        },
        {
          title: '定时执行',
          dataIndex: 'autoExecCron',
          ellipsis: true,
          width: 120
        },
        {
          title: '创建时间',
          dataIndex: 'createTimeMillis',
          ellipsis: true,
          sorter: true,
          customRender: ({ text }) => {
            return parseTime(text)
          },
          width: '170px'
        },
        {
          title: '修改时间',
          dataIndex: 'modifyTimeMillis',
          width: '170px',
          ellipsis: true,
          sorter: true,
          customRender: ({ text }) => {
            return parseTime(text)
          }
        },
        {
          title: '最后操作人',
          dataIndex: 'modifyUser',
          width: 120,
          ellipsis: true
        },
        {
          title: '操作',
          dataIndex: 'operation',
          align: 'center',

          fixed: 'right',
          width: '240px'
        }
      ],
      tableSelections: [],
      syncToWorkspaceVisible: false,
      workspaceList: [],
      triggerVisible: false,
      confirmLoading: false
    }
  },
  computed: {
    ...mapState(useAppStore, ['getWorkspaceId']),
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery)
    },
    rowSelection() {
      return {
        onChange: (selectedRowKeys) => {
          this.tableSelections = selectedRowKeys
        },
        selectedRowKeys: this.tableSelections
      }
    }
  },
  mounted() {
    this.getCommandData()
    //this.getAllSSHList();
  },
  methods: {
    // 编辑命令信息
    handleEditCommandOk() {
      this.$refs['editCommandForm'].validate().then(() => {
        if (this.commandParams && this.commandParams.length > 0) {
          for (let i = 0; i < this.commandParams.length; i++) {
            if (!this.commandParams[i].desc) {
              $notification.error({
                message: '请填写第' + (i + 1) + '个参数的描述'
              })
              return false
            }
          }
          this.temp.defParams = JSON.stringify(this.commandParams)
        } else {
          this.temp.defParams = ''
        }
        this.temp.sshIds = this.chooseSsh.join(',')
        this.confirmLoading = true
        editCommand(this.temp)
          .then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              this.editCommandVisible = false

              this.getCommandData()
            }
          })
          .finally(() => {
            this.confirmLoading = false
          })
      })
    },
    // 获取命令数据
    getCommandData(pointerEvent) {
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
      this.loading = true
      getCommandList(this.listQuery).then((res) => {
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
      this.getCommandData()
    },

    // 创建命令弹窗
    createCommand() {
      this.editCommandVisible = true
      this.getAllSSHList()
      this.chooseSsh = []
      this.commandParams = []
      this.temp = {}
      this.$refs['editCommandForm'] && this.$refs['editCommandForm'].resetFields()
    },
    // 修改
    handleEdit(rowData) {
      const row = Object.assign({}, rowData)
      this.editCommandVisible = true
      this.$refs['editCommandForm'] && this.$refs['editCommandForm'].resetFields()
      this.commandParams = []
      if (row.defParams) {
        this.commandParams = JSON.parse(row.defParams)
      }
      this.temp = row
      this.chooseSsh = row.sshIds ? row.sshIds.split(',') : []
      this.getAllSSHList()
    },
    // 执行命令
    handleExecute(rowData) {
      const row = Object.assign({}, rowData)
      if (typeof row.defParams === 'string' && row.defParams) {
        this.commandParams = JSON.parse(row.defParams)
      } else {
        this.commandParams = []
      }
      this.temp = row
      this.chooseSsh = row.sshIds ? row.sshIds.split(',') : []
      this.executeCommandVisible = true
      this.getAllSSHList()
    },
    //  删除命令
    handleDelete(row) {
      const that = this
      this.$confirm({
        title: '系统提示',
        zIndex: 1009,
        content: '真的要删除“' + row.name + '”命令？',
        okText: '确认',
        cancelText: '取消',
        async onOk() {
          return await new Promise((resolve, reject) => {
            // 删除
            deleteCommand(row.id)
              .then((res) => {
                if (res.code === 200) {
                  $notification.success({
                    message: res.msg
                  })
                  that.getCommandData()
                }

                resolve()
              })
              .catch(reject)
          })
        }
      })
    },
    // 获取所有ssh接点
    getAllSSHList() {
      getSshListAll().then((res) => {
        this.sshList = res.data || []
      })
    },

    handleExecuteCommandOk() {
      if (!this.chooseSsh || this.chooseSsh.length <= 0) {
        $notification.error({
          message: '请选择执行节点'
        })
        return false
      }
      this.confirmLoading = true
      executeBatch({
        id: this.temp.id,
        params: JSON.stringify(this.commandParams),
        nodes: this.chooseSsh.join(',')
      })
        .then((res) => {
          if (res.code === 200) {
            $notification.success({
              message: res.msg
            })
            this.executeCommandVisible = false
            this.temp = {
              commandId: this.temp.id,
              batchId: res.data
            }
            this.logVisible = true
          }
        })
        .finally(() => {
          this.confirmLoading = false
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
        $notification.warn({
          message: '请选择工作空间'
        })
        return false
      }
      // 同步
      this.confirmLoading = true
      syncToWorkspace({
        ids: this.tableSelections.join(','),
        toWorkspaceId: this.temp.workspaceId
      })
        .then((res) => {
          if (res.code === 200) {
            $notification.success({
              message: res.msg
            })
            this.tableSelections = []
            this.syncToWorkspaceVisible = false
            return false
          }
        })
        .finally(() => {
          this.confirmLoading = false
        })
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
    }
  }
}
</script>

<style scoped>
.config-editor {
  overflow-y: scroll;
  max-height: 300px;
}
</style>
