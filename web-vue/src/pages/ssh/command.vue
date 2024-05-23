<template>
  <div>
    <CustomTable
      is-show-tools
      default-auto-refresh
      :auto-refresh-time="30"
      :active-page="activePage"
      table-name="ssh-command-list"
      :empty-description="$tl('p.noSshScriptCommand')"
      :data-source="commandList"
      :columns="columns"
      size="middle"
      bordered
      :pagination="pagination"
      :row-selection="rowSelection"
      row-key="id"
      :scroll="{
        x: 'max-content'
      }"
      @change="changePage"
      @refresh="getCommandData"
    >
      <template #title>
        <a-space wrap class="search-box">
          <a-input
            v-model:value="listQuery['%name%']"
            :placeholder="$tl('p.searchCommand')"
            class="search-input-item"
            @press-enter="getCommandData"
          />
          <a-input
            v-model:value="listQuery['%desc%']"
            :placeholder="$tl('p.description')"
            class="search-input-item"
            @press-enter="getCommandData"
          />
          <a-input
            v-model:value="listQuery['%autoExecCron%']"
            :placeholder="$tl('c.schedulingExecution')"
            class="search-input-item"
            @press-enter="getCommandData"
          />
          <a-tooltip :title="$tl('p.quickBackToFirstPage')">
            <a-button type="primary" :loading="loading" @click="getCommandData">{{ $tl('p.search') }}</a-button>
          </a-tooltip>
          <a-button type="primary" @click="createCommand">{{ $tl('p.addNew') }}</a-button>
          <a-dropdown>
            <a @click="(e) => e.preventDefault()"> {{ $tl('p.more') }} <DownOutlined /> </a>
            <template #overlay>
              <a-menu>
                <a-menu-item>
                  <a-button
                    type="primary"
                    :disabled="!tableSelections || !tableSelections.length"
                    @click="syncToWorkspaceShow"
                    >{{ $tl('p.workspaceSync') }}</a-button
                  >
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </a-space>
      </template>
      <template #tableHelp>
        <a-tooltip>
          <template #title>
            <div>{{ $tl('p.commandTemplateDescription') }}</div>

            <div>
              <ul>
                <li>{{ $tl('p.supportWorkspaceEnv') }}</li>
                <li>{{ $tl('p.execCommandReplace') }}</li>
                <li>
                  {{ $tl('p.execCommandInclude') }}<b>#disabled-template-auto-evn</b>
                  {{ $tl('p.cancelAutoLoadEnv') }}({{ $tl('p.noteNoSpace') }})
                </li>
                <li>{{ $tl('p.commandFilePath') }}</li>
              </ul>
            </div>
          </template>
          <QuestionCircleOutlined />
        </a-tooltip>
      </template>
      <template #tableBodyCell="{ column, text, record }">
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
            <a-button size="small" type="primary" @click="handleExecute(record)">{{ $tl('c.execution') }}</a-button>
            <a-button size="small" type="primary" @click="handleEdit(record)">{{ $tl('p.edit') }}</a-button>
            <a-button size="small" type="primary" @click="handleTrigger(record)">{{ $tl('c.trigger') }}</a-button>
            <a-button size="small" type="primary" danger @click="handleDelete(record)">{{ $tl('p.delete') }}</a-button>
          </a-space>
        </template>
      </template>
    </CustomTable>
    <!-- 编辑命令 -->
    <a-modal
      v-model:open="editCommandVisible"
      destroy-on-close
      width="80vw"
      :title="$tl('p.editCommand')"
      :mask-closable="false"
      :confirm-loading="confirmLoading"
      @ok="handleEditCommandOk"
    >
      <a-form ref="editCommandForm" :rules="rules" :model="temp" :label-col="{ span: 3 }" :wrapper-col="{ span: 20 }">
        <a-form-item :label="$tl('c.commandName')" name="name">
          <a-input v-model:value="temp.name" :max-length="100" :placeholder="$tl('c.commandName')" />
        </a-form-item>

        <a-form-item name="command" :help="$tl('p.scriptPathAndExecMethod')">
          <template #label>
            <a-tooltip>
              {{ $tl('p.commandContent') }}
              <template #title>
                <ul>
                  <li>{{ $tl('p.canReferenceWorkspaceEnv') }}</li>
                </ul>
              </template>
              <QuestionCircleOutlined v-show="!temp.id" />
            </a-tooltip>
          </template>

          <a-form-item-rest>
            <code-editor
              v-model:content="temp.command"
              height="40vh"
              :options="{ mode: 'shell', tabSize: 2 }"
            ></code-editor>
          </a-form-item-rest>
        </a-form-item>
        <a-form-item :label="$tl('c.sshNode')">
          <a-select
            v-model:value="chooseSsh"
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
            :placeholder="$tl('p.selectSshNode')"
            mode="multiple"
          >
            <a-select-option v-for="item in sshList" :key="item.id" :value="item.id">
              {{ item.name }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item :label="$tl('p.defaultParam')">
          <a-form-item-rest>
            <a-space direction="vertical" style="width: 100%">
              <a-row v-for="(item, index) in commandParams" :key="item.key">
                <a-col :span="22">
                  <a-space direction="vertical" style="width: 100%">
                    <a-input
                      v-model:value="item.desc"
                      :addon-before="$tl('p.paramDescription', { index: index + 1 })"
                      placeholder="参数描述,{{$tl('p.paramDescriptionNoEffect')}},仅是用于提示参数的含义"
                    />
                    <a-input
                      v-model:value="item.value"
                      :addon-before="$tl('p.paramValue', { index: index + 1 })"
                      :placeholder="`${$tl('c.paramValue')}${$tl('p.addNewDefaultParamNote')}`"
                    />
                  </a-space>
                </a-col>

                <a-col :span="2">
                  <a-row type="flex" justify="center" align="middle">
                    <a-col>
                      <MinusCircleOutlined style="color: #ff0000" @click="() => commandParams.splice(index, 1)" />
                    </a-col>
                  </a-row>
                </a-col>
              </a-row>
              <a-button type="primary" @click="() => commandParams.push({})">{{ $tl('c.newParam') }}</a-button>
            </a-space>
          </a-form-item-rest>
        </a-form-item>
        <a-form-item :label="$tl('p.autoExec')" name="autoExecCron">
          <a-auto-complete
            v-model:value="temp.autoExecCron"
            :placeholder="$tl('p.cronExpression')"
            :options="CRON_DATA_SOURCE"
          >
            <template #option="item"> {{ item.title }} {{ item.value }} </template>
          </a-auto-complete>
        </a-form-item>
        <a-form-item :label="$tl('c.commandDescription')" name="desc">
          <a-textarea
            v-model:value="temp.desc"
            :max-length="255"
            :rows="3"
            style="resize: none"
            :placeholder="$tl('p.commandDetail')"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="executeCommandVisible"
      destroy-on-close
      width="600px"
      :title="$tl('p.executeCommand')"
      :mask-closable="false"
      :confirm-loading="confirmLoading"
      @ok="handleExecuteCommandOk"
    >
      <a-form :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item :label="$tl('c.commandName')" name="name">
          <a-input v-model:value="temp.name" :disabled="true" :placeholder="$tl('c.commandName')" />
        </a-form-item>

        <a-form-item :label="$tl('c.sshNode')" required>
          <a-select
            v-model:value="chooseSsh"
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
            mode="multiple"
            :placeholder="$tl('p.selectSshNode_1')"
          >
            <a-select-option v-for="item in sshList" :key="item.id" :value="item.id">
              {{ item.name }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item :label="$tl('p.commandParam')" :help="`${commandParams.length ? $tl('p.paramInstruction') : ''}`">
          <a-space direction="vertical" style="width: 100%">
            <a-row v-for="(item, index) in commandParams" :key="item.key">
              <a-col :span="22">
                <a-input
                  v-model:value="item.value"
                  :addon-before="`${$tl('p.paramName')}${index + 1}${$tl('p.paramValue_1')}`"
                  :placeholder="`${$tl('p.paramName')}${$tl('p.paramValue_1')} ${item.desc ? ',' + item.desc : ''}`"
                >
                  <template #suffix>
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
            <a-button type="primary" @click="() => commandParams.push({})">{{ $tl('c.newParam') }}</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-modal>
    <!-- 执行日志 -->
    <a-modal
      v-model:open="logVisible"
      destroy-on-close
      :width="'80vw'"
      :title="$tl('p.executeLog')"
      :footer="null"
      :mask-closable="false"
    >
      <command-log v-if="logVisible" :temp="temp" />
    </a-modal>
    <!-- 同步到其他工作空间 -->
    <a-modal
      v-model:open="syncToWorkspaceVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      :title="$tl('p.syncToOtherWorkspaces')"
      :mask-closable="false"
      @ok="handleSyncToWorkspace"
    >
      <a-alert :message="$tl('c.tips')" type="warning" show-icon>
        <template #description>
          <ul>
            <li>
              {{ $tl('p.syncMechanism') }}<b>{{ $tl('p.scriptName') }}</b
              >{{ $tl('p.confirmSameScript') }}
            </li>
            <li>{{ $tl('p.createNewScriptIfNotExist') }}</li>
            <li>{{ $tl('p.syncScriptInfoIfExists') }}</li>
          </ul>
        </template>
      </a-alert>
      <a-form :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
        <a-form-item> </a-form-item>
        <a-form-item :label="$tl('p.selectWorkspace')" name="workspaceId">
          <a-select
            v-model:value="temp.workspaceId"
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
            :placeholder="$tl('c.selectWorkspace')"
          >
            <a-select-option v-for="item in workspaceList" :key="item.id" :disabled="getWorkspaceId() === item.id">{{
              item.name
            }}</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 触发器 -->
    <a-modal
      v-model:open="triggerVisible"
      destroy-on-close
      :title="$tl('c.trigger')"
      width="50%"
      :footer="null"
      :mask-closable="false"
    >
      <a-form ref="editTriggerForm" :rules="rules" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-tabs default-active-key="1">
          <template #rightExtra>
            <a-tooltip :title="$tl('p.resetTriggerToken')">
              <a-button type="primary" size="small" @click="resetTrigger">{{ $tl('p.reset') }}</a-button>
            </a-tooltip>
          </template>
          <a-tab-pane key="1" :tab="$tl('c.execution')">
            <a-space direction="vertical" style="width: 100%">
              <a-alert :message="$tl('c.tips')" type="warning">
                <template #description>
                  <ul>
                    <li>{{ $tl('p.triggerUrlInfo') }}</li>
                    <li>{{ $tl('p.regenerateTriggerUrl') }}</li>
                    <li>{{ $tl('p.batchTriggerParams') }}</li>
                    <li>{{ $tl('p.parseParamsAsEnv') }}</li>
                  </ul>
                </template>
              </a-alert>
              <a-alert type="info" :message="`${$tl('p.singleTriggerUrl')}(${$tl('c.copy')})`">
                <template #description>
                  <a-typography-paragraph :copyable="{ tooltip: false, text: temp.triggerUrl }">
                    <a-tag>GET</a-tag> <span>{{ temp.triggerUrl }} </span>
                  </a-typography-paragraph>
                </template>
              </a-alert>
              <a-alert type="info" :message="`${$tl('p.batchTriggerUrls')}(${$tl('c.copy')})`">
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
          title: this.$tl('c.commandName'),
          dataIndex: 'name',
          ellipsis: true,
          width: 200
        },
        {
          title: this.$tl('c.commandDescription'),
          dataIndex: 'desc',
          ellipsis: true,
          width: 250
        },
        {
          title: this.$tl('c.schedulingExecution'),
          dataIndex: 'autoExecCron',
          ellipsis: true,
          width: 120
        },
        {
          title: this.$tl('p.createTime'),
          dataIndex: 'createTimeMillis',
          ellipsis: true,
          sorter: true,
          customRender: ({ text }) => {
            return parseTime(text)
          },
          width: '170px'
        },
        {
          title: this.$tl('p.updateTime'),
          dataIndex: 'modifyTimeMillis',
          width: '170px',
          ellipsis: true,
          sorter: true,
          customRender: ({ text }) => {
            return parseTime(text)
          }
        },
        {
          title: this.$tl('p.lastOperator'),
          dataIndex: 'modifyUser',
          width: 120,
          ellipsis: true
        },
        {
          title: this.$tl('p.operation'),
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
    activePage() {
      return this.$attrs.routerUrl === this.$route.path
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
    $tl(key, ...args) {
      return this.$t(`pages.ssh.command.${key}`, ...args)
    },
    // 编辑命令信息
    handleEditCommandOk() {
      this.$refs['editCommandForm'].validate().then(() => {
        if (this.commandParams && this.commandParams.length > 0) {
          for (let i = 0; i < this.commandParams.length; i++) {
            if (!this.commandParams[i].desc) {
              $notification.error({
                message: this.$tl('p.fillParamDesc') + (i + 1) + this.$tl('p.paramDesc')
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
      $confirm({
        title: this.$tl('p.systemPrompt'),
        zIndex: 1009,
        content: this.$tl('p.confirmDeleteCommand') + row.name + this.$tl('p.deleteCommand'),
        okText: this.$tl('p.confirm'),
        cancelText: this.$tl('p.cancel'),
        onOk: () => {
          return deleteCommand(row.id).then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              this.getCommandData()
            }
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
          message: this.$tl('p.selectExecuteNode')
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
          message: this.$tl('c.selectWorkspace')
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
