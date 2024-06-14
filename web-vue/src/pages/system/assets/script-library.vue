<template>
  <div>
    <!-- 数据表格 -->
    <CustomTable
      is-show-tools
      default-auto-refresh
      :auto-refresh-time="30"
      :active-page="activePage"
      table-name="script-library"
      :empty-description="$tl('p.noScript')"
      :data-source="list"
      size="middle"
      :columns="columns"
      :pagination="pagination"
      bordered
      row-key="id"
      :row-selection="rowSelection"
      :scroll="{
        x: 'max-content'
      }"
      @change="changePage"
      @refresh="loadData"
    >
      <template #title>
        <a-space wrap class="search-box">
          <a-input
            v-model:value="listQuery['id']"
            :placeholder="$tl('p.scriptId')"
            allow-clear
            class="search-input-item"
            @press-enter="loadData"
          />
          <a-input
            v-model:value="listQuery['%name%']"
            :placeholder="$tl('c.name')"
            allow-clear
            class="search-input-item"
            @press-enter="loadData"
          />
          <a-input
            v-model:value="listQuery['%description%']"
            :placeholder="$tl('c.description')"
            class="search-input-item"
            @press-enter="loadData"
          />
          <a-input
            v-model:value="listQuery['%autoExecCron%']"
            :placeholder="$tl('c.scheduleExecution')"
            class="search-input-item"
            @press-enter="loadData"
          />
          <a-tooltip :title="$tl('p.backToFirstPage')">
            <a-button :loading="loading" type="primary" @click="loadData">{{ $tl('p.search') }}</a-button>
          </a-tooltip>
          <a-button type="primary" @click="createScript">{{ $tl('p.add') }}</a-button>
          <a-button
            v-if="mode === 'manage'"
            type="primary"
            :disabled="!tableSelections || !tableSelections.length"
            @click="syncToWorkspaceShow"
            >{{ $tl('p.workspaceSync') }}</a-button
          >
        </a-space>
      </template>
      <template #tableHelp>
        <a-tooltip>
          <template #title>
            <div>{{ $tl('p.scriptTemplateDescription') }}</div>

            <div>
              <ul>
                <li>{{ $tl('p.executionEnvNote') }}</li>
                <li>{{ $tl('p.commandFilePath') }}</li>
                <li>{{ $tl('p.distributionNodeDescription') }}</li>
              </ul>
            </div>
          </template>
          <QuestionCircleOutlined />
        </a-tooltip>
      </template>
      <template #tableBodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'nodeId'">
          <a-tooltip placement="topLeft" :title="text">
            <span>{{ nodeMap[text] }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.tooltip">
          <a-tooltip placement="topLeft" :title="text">
            <span>{{ text }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'name'">
          <a-tooltip placement="topLeft" :title="text" @click="handleEdit(record)">
            <a-button type="link" style="padding: 0" size="small">{{ text }}</a-button>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'workspaceId'">
          <a-tag v-if="text === 'GLOBAL'">{{ $tl('c.global') }}</a-tag>
          <a-tag v-else>{{ $tl('p.workspace') }}</a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <template v-if="mode === 'manage'">
              <a-button size="small" type="primary" @click="handleExec(record)">{{ $tl('c.execute') }}</a-button>
              <a-button size="small" type="primary" @click="handleEdit(record)">{{ $tl('c.edit') }}</a-button>
              <a-button size="small" type="primary" @click="handleLog(record)">{{ $tl('p.log') }}</a-button>
              <a-dropdown>
                <a @click="(e) => e.preventDefault()">
                  {{ $tl('p.more') }}
                  <DownOutlined />
                </a>
                <template #overlay>
                  <a-menu>
                    <a-menu-item>
                      <a-button size="small" type="primary" @click="handleTrigger(record)">{{
                        $tl('c.trigger')
                      }}</a-button>
                    </a-menu-item>

                    <a-menu-item>
                      <a-button size="small" type="primary" danger @click="handleDelete(record)">{{
                        $tl('p.delete')
                      }}</a-button>
                    </a-menu-item>
                    <a-menu-item>
                      <a-button
                        size="small"
                        type="primary"
                        danger
                        :disabled="!record.nodeIds"
                        @click="handleUnbind(record)"
                        >{{ $tl('p.unbind') }}</a-button
                      >
                    </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </template>
            <template v-else>
              <a-button size="small" type="primary" @click="handleEdit(record)">{{ $tl('c.edit') }}</a-button>
            </template>
          </a-space>
        </template>
      </template>
    </CustomTable>
    <!-- 编辑区 -->
    <a-modal
      v-model:open="editScriptVisible"
      destroy-on-close
      :z-index="1009"
      :title="$tl('p.editScript')"
      :mask-closable="false"
      width="80vw"
      :confirm-loading="confirmLoading"
      @ok="handleEditScriptOk"
    >
      <a-form ref="editScriptForm" :rules="rules" :model="temp" :label-col="{ span: 3 }" :wrapper-col="{ span: 19 }">
        <a-form-item v-if="temp.id" label="ScriptId" name="id">
          <a-input v-model:value="temp.id" disabled read-only />
        </a-form-item>
        <a-form-item :label="$tl('p.scriptName')" name="name">
          <a-input v-model:value="temp.name" :max-length="50" :placeholder="$tl('c.name')" />
        </a-form-item>
        <a-form-item :label="$tl('p.scriptContent')" name="context">
          <a-form-item-rest>
            <code-editor v-model:content="temp.context" height="40vh" :options="{ mode: 'shell', tabSize: 2 }">
            </code-editor>
          </a-form-item-rest>
        </a-form-item>
        <!-- <a-form-item label="默认参数" name="defArgs">
            <a-input v-model="temp.defArgs" placeholder="默认参数" />
          </a-form-item> -->
        <a-form-item :label="$tl('p.defaultParam')">
          <a-space direction="vertical" style="width: 100%">
            <a-row v-for="(item, index) in commandParams" :key="item.key">
              <a-col :span="22">
                <a-space direction="vertical" style="width: 100%">
                  <a-input
                    v-model:value="item.desc"
                    :addon-before="$tl('p.parameterContent', { count: index + 1 })"
                    :placeholder="$tl('p.content1')" />
                  <a-input
                    v-model:value="item.value"
                    :addon-before="$tl('p.parameterContent', { count: index + 1 })"
                    :placeholder="$tl('p.content2')"
                /></a-space>
              </a-col>
              <a-col :span="2">
                <a-row type="flex" justify="center" align="middle">
                  <a-col>
                    <MinusCircleOutlined style="color: #ff0000" @click="() => commandParams.splice(index, 1)" />
                  </a-col>
                </a-row>
              </a-col>
            </a-row>
            <a-divider style="margin: 5px 0" />
          </a-space>

          <a-button type="primary" @click="() => commandParams.push({})">{{ $tl('p.addParam') }}</a-button>
        </a-form-item>
        <a-form-item :label="$tl('c.scheduleExecution')" name="autoExecCron">
          <a-auto-complete
            v-model:value="temp.autoExecCron"
            :placeholder="$tl('p.cronExpression')"
            :options="CRON_DATA_SOURCE"
          >
            <template #option="item"> {{ item.title }} {{ item.value }} </template>
          </a-auto-complete>
        </a-form-item>
        <a-form-item :label="$tl('c.description')" name="description">
          <a-textarea
            v-model:value="temp.description"
            :max-length="200"
            :rows="3"
            style="resize: none"
            :placeholder="$tl('p.detailedDescription')"
          />
        </a-form-item>
        <a-form-item :label="$tl('c.share')" name="global">
          <a-radio-group v-model:value="temp.global">
            <a-radio :value="true"> {{ $tl('c.global') }}</a-radio>
            <a-radio :value="false"> {{ $tl('p.currentWorkspace') }}</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item v-if="temp.prohibitSync" :label="$tl('p.disableDistributionNode')">
          <template #help>{{ $tl('p.controlNodeDistribution') }}</template>
          <a-tag v-for="(item, index) in temp.nodeList" :key="index"
            >{{ $tl('p.nodeName') }}{{ item.nodeName }} {{ $tl('p.selectedWorkspace') }}{{ item.workspaceName }}</a-tag
          >
        </a-form-item>
        <a-form-item v-else>
          <template #label>
            <a-tooltip>
              {{ $tl('p.distributionNodeLabel') }}
              <template #title> {{ $tl('p.content3') }} </template>
              <QuestionCircleOutlined v-show="!temp.id" />
            </a-tooltip>
          </template>
          <a-select
            v-model:value="temp.chooseNode"
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
            :placeholder="$tl('p.distributeToNode')"
            mode="multiple"
          >
            <a-select-option v-for="item in nodeList" :key="item.id" :value="item.id">
              {{ item.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script>
import { getScriptLibraryList, editScriptLibrary, delScriptLibrary } from '@/api/system/script-library'
import codeEditor from '@/components/codeEditor'
import { getNodeListAll } from '@/api/node'

import { CHANGE_PAGE, COMPUTED_PAGINATION, CRON_DATA_SOURCE, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'

import { getWorkSpaceListAll } from '@/api/workspace'

export default {
  components: {
    codeEditor
  },
  props: {
    choose: {
      type: String,
      default: 'checkbox'
      // "radio" ,"checkbox"
    },
    mode: {
      // choose、manage
      type: String,
      default: 'manage'
    },
    chooseVal: {
      type: String,
      default: ''
    }
  },

  data() {
    return {
      // choose: this.choose,
      loading: false,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      CRON_DATA_SOURCE,
      list: [],
      temp: {},
      nodeList: [],
      editScriptVisible: false,
      drawerTitle: '',
      drawerConsoleVisible: false,
      columns: [
        {
          title: 'id',
          dataIndex: 'id',
          ellipsis: true,
          sorter: true,
          width: 50,
          tooltip: true
        },
        {
          title: this.$tl('c.name'),
          dataIndex: 'name',
          ellipsis: true,
          sorter: true,
          width: 150
        },
        {
          title: this.$tl('c.share'),
          dataIndex: 'workspaceId',
          sorter: true,
          ellipsis: true,

          width: '90px'
        },
        {
          title: this.$tl('c.description'),
          dataIndex: 'description',
          ellipsis: true,
          width: 100,
          tooltip: true
        },
        {
          title: this.$tl('c.scheduleExecution'),
          dataIndex: 'autoExecCron',
          ellipsis: true,
          sorter: true,
          width: '100px',
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
          tooltip: true,
          width: '120px'
        },
        {
          title: this.$tl('p.modifier'),
          dataIndex: 'modifyUser',
          ellipsis: true,
          tooltip: true,
          width: '120px'
        },
        {
          title: this.$tl('p.lastExecutor'),
          dataIndex: 'lastRunUser',
          ellipsis: true,
          width: '120px',
          tooltip: true
        },
        this.mode === 'manage'
          ? {
              title: this.$tl('c.operation'),
              dataIndex: 'operation',
              align: 'center',

              fixed: 'right',
              width: '240px'
            }
          : {
              title: this.$tl('c.operation'),
              dataIndex: 'operation',
              align: 'center',

              fixed: 'right',
              width: '100px'
            }
      ],
      rules: {
        name: [{ required: true, message: this.$tl('p.inputScriptName'), trigger: 'blur' }],
        context: [{ required: true, message: this.$tl('p.inputScriptContent'), trigger: 'blur' }]
      },
      tableSelections: [],
      syncToWorkspaceVisible: false,
      workspaceList: [],
      triggerVisible: false,
      commandParams: [],
      drawerLogVisible: false,
      confirmLoading: false
    }
  },
  computed: {
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
        type: this.choose
      }
    }
  },
  watch: {
    chooseVal: {
      deep: true,

      handler(v) {
        if (v) {
          this.tableSelections = v.split(',')
        } else {
          this.tableSelections = []
        }
      },

      immediate: true
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
    $tl(key, ...args) {
      return this.$t(`pages.script.scriptList.${key}`, ...args)
    },
    // 加载数据
    loadData(pointerEvent) {
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
      this.loading = true
      getScriptLibraryList(this.listQuery).then((res) => {
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
      this.$refs['editScriptForm'].validate().then(() => {
        if (this.commandParams && this.commandParams.length > 0) {
          for (let i = 0; i < this.commandParams.length; i++) {
            if (!this.commandParams[i].desc) {
              $notification.error({
                message: this.$tl('p.paramDescriptionPrefix') + (i + 1) + this.$tl('p.paramDescriptionSuffix')
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
        this.confirmLoading = true
        editScript(this.temp)
          .then((res) => {
            if (res.code === 200) {
              // 成功
              $notification.success({
                message: res.msg
              })

              this.editScriptVisible = false
              this.loadData()
              this.$refs['editScriptForm'].resetFields()
            }
          })
          .finally(() => {
            this.confirmLoading = false
          })
      })
    },
    handleDelete(record) {
      $confirm({
        title: this.$tl('p.systemTip'),
        content: this.$tl('p.confirmDeleteScript'),
        zIndex: 1009,
        okText: this.$tl('c.confirm'),
        cancelText: this.$tl('c.cancel'),
        onOk: () => {
          return deleteScript({
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
    // 执行 Script
    handleExec(record) {
      this.temp = Object.assign(record)
      this.drawerTitle = `${$tl('p.console')}(${this.temp.name})`
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

    // 加载工作空间数据
    loadWorkSpaceListAll() {
      getWorkSpaceListAll().then((res) => {
        if (res.code === 200) {
          this.workspaceList = res.data
        }
      })
    }
  }
}
</script>
