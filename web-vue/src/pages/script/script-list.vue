<template>
  <div>
    <!-- 数据表格 -->
    <CustomTable
      is-show-tools
      default-auto-refresh
      :auto-refresh-time="30"
      :active-page="activePage"
      table-name="server-script-list"
      :empty-description="$t('i18n_5badae1d90')"
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
            :placeholder="$t('i18n_d18d658415')"
            allow-clear
            class="search-input-item"
            @press-enter="loadData"
          />
          <a-input
            v-model:value="listQuery['%name%']"
            :placeholder="$t('i18n_d7ec2d3fea')"
            allow-clear
            class="search-input-item"
            @press-enter="loadData"
          />
          <a-input
            v-model:value="listQuery['%description%']"
            :placeholder="$t('i18n_3bdd08adab')"
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
          <a-button type="primary" @click="createScript">{{ $t('i18n_66ab5e9f24') }}</a-button>
          <a-button
            v-if="mode === 'manage'"
            type="primary"
            :disabled="!tableSelections || !tableSelections.length"
            @click="syncToWorkspaceShow"
            >{{ $t('i18n_398ce396cd') }}</a-button
          >
        </a-space>
      </template>
      <template #tableHelp>
        <a-tooltip>
          <template #title>
            <div>{{ $t('i18n_2f8fd34058') }}</div>

            <div>
              <ul>
                <li>{{ $t('i18n_5ecc709db7') }}</li>
                <li>{{ $t('i18n_c600eda869') }}</li>
                <li>{{ $t('i18n_05e6d88e29') }}</li>
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
          <a-tag v-if="text === 'GLOBAL'">{{ $t('i18n_2be75b1044') }}</a-tag>
          <a-tag v-else>{{ $t('i18n_98d69f8b62') }}</a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <template v-if="mode === 'manage'">
              <a-button size="small" type="primary" @click="handleExec(record)">{{ $t('i18n_1a6aa24e76') }}</a-button>
              <a-button size="small" type="primary" @click="handleEdit(record)">{{ $t('i18n_95b351c862') }}</a-button>
              <a-button size="small" type="primary" @click="handleLog(record)">{{ $t('i18n_456d29ef8b') }}</a-button>
              <a-dropdown>
                <a @click="(e) => e.preventDefault()">
                  {{ $t('i18n_0ec9eaf9c3') }}
                  <DownOutlined />
                </a>
                <template #overlay>
                  <a-menu>
                    <a-menu-item>
                      <a-button size="small" type="primary" @click="handleTrigger(record)">{{
                        $t('i18n_4696724ed3')
                      }}</a-button>
                    </a-menu-item>

                    <a-menu-item>
                      <a-button size="small" type="primary" danger @click="handleDelete(record)">{{
                        $t('i18n_2f4aaddde3')
                      }}</a-button>
                    </a-menu-item>
                    <a-menu-item>
                      <a-button
                        size="small"
                        type="primary"
                        danger
                        :disabled="!record.nodeIds"
                        @click="handleUnbind(record)"
                        >{{ $t('i18n_663393986e') }}</a-button
                      >
                    </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </template>
            <template v-else>
              <a-button size="small" type="primary" @click="handleEdit(record)">{{ $t('i18n_95b351c862') }}</a-button>
            </template>
          </a-space>
        </template>
      </template>
    </CustomTable>
    <!-- 编辑区 -->
    <CustomModal
      v-if="editScriptVisible"
      v-model:open="editScriptVisible"
      destroy-on-close
      :title="$t('i18n_c446efd80d')"
      :mask-closable="false"
      width="80vw"
      :confirm-loading="confirmLoading"
      @ok="handleEditScriptOk"
    >
      <a-form ref="editScriptForm" :rules="rules" :model="temp" :label-col="{ span: 3 }" :wrapper-col="{ span: 19 }">
        <a-form-item v-if="temp.id" label="ScriptId" name="id">
          <a-input v-model:value="temp.id" disabled read-only />
        </a-form-item>
        <a-form-item :label="$t('i18n_e747635151')" name="name">
          <a-input v-model:value="temp.name" :max-length="50" :placeholder="$t('i18n_d7ec2d3fea')" />
        </a-form-item>
        <a-form-item :label="$t('i18n_4d9c3a0ed0')" name="context">
          <a-form-item-rest>
            <code-editor
              v-model:content="temp.context"
              height="40vh"
              :show-tool="true"
              :options="{ mode: 'shell', tabSize: 2 }"
            >
              <template #tool_before>
                <a-button type="link" @click="scriptLibraryVisible = true">{{ $t('i18n_f685377a22') }}</a-button>
              </template>
            </code-editor>
          </a-form-item-rest>
        </a-form-item>

        <a-form-item :label="$t('i18n_2171d1b07d')">
          <a-space direction="vertical" style="width: 100%">
            <a-row v-for="(item, index) in commandParams" :key="item.key">
              <a-col :span="22">
                <a-space direction="vertical" style="width: 100%">
                  <a-input
                    v-model:value="item.desc"
                    :addon-before="$t('i18n_0390e2f548', { count: index + 1 })"
                    :placeholder="$t('i18n_9e78f02aad')" />
                  <a-input
                    v-model:value="item.value"
                    :addon-before="$t('i18n_a1f58b7189', { count: index + 1 })"
                    :placeholder="$t('i18n_2d9569bf45')"
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

          <a-button type="primary" @click="() => commandParams.push({})">{{ $t('i18n_4c0eead6ff') }}</a-button>
        </a-form-item>
        <a-form-item :label="$t('i18n_6b2e348a2b')" name="autoExecCron">
          <a-auto-complete
            v-model:value="temp.autoExecCron"
            :placeholder="$t('i18n_5dff0d31d0')"
            :options="CRON_DATA_SOURCE"
          >
            <template #option="item"> {{ item.title }} {{ item.value }} </template>
          </a-auto-complete>
        </a-form-item>
        <a-form-item :label="$t('i18n_3bdd08adab')" name="description">
          <a-textarea
            v-model:value="temp.description"
            :max-length="200"
            :rows="3"
            style="resize: none"
            :placeholder="$t('i18n_ae653ec180')"
          />
        </a-form-item>
        <a-form-item :label="$t('i18n_fffd3ce745')" name="global">
          <a-radio-group v-model:value="temp.global">
            <a-radio :value="true"> {{ $t('i18n_2be75b1044') }}</a-radio>
            <a-radio :value="false"> {{ $t('i18n_691b11e443') }}</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item v-if="temp.prohibitSync" :label="$t('i18n_b499798ec5')">
          <template #help>{{ $t('i18n_06986031a7') }}</template>
          <a-tag v-for="(item, index) in temp.nodeList" :key="index"
            >{{ $t('i18n_5d83794cfa') }}{{ item.nodeName }} {{ $t('i18n_4d85c37f0d') }}{{ item.workspaceName }}</a-tag
          >
        </a-form-item>
        <a-form-item v-else>
          <template #label>
            <a-tooltip>
              {{ $t('i18n_6a6c857285') }}
              <template #title> {{ $t('i18n_d7ba18c360') }} </template>
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
            :placeholder="$t('i18n_a03ea1e864')"
            mode="multiple"
          >
            <a-select-option v-for="item in nodeList" :key="item.id" :value="item.id">
              {{ item.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </CustomModal>
    <!-- 脚本控制台组件 -->
    <CustomDrawer
      v-if="drawerConsoleVisible"
      destroy-on-close
      :title="drawerTitle"
      placement="right"
      width="85vw"
      :open="drawerConsoleVisible"
      @close="onConsoleClose"
    >
      <script-console v-if="drawerConsoleVisible" :id="temp.id" :def-args="temp.defArgs" />
    </CustomDrawer>
    <!-- 同步到其他工作空间 -->
    <CustomModal
      v-if="syncToWorkspaceVisible"
      v-model:open="syncToWorkspaceVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      :title="$t('i18n_1a44b9e2f7')"
      :mask-closable="false"
      @ok="handleSyncToWorkspace"
    >
      <a-alert :message="$t('i18n_947d983961')" type="warning" show-icon>
        <template #description>
          <ul>
            <li>
              {{ $t('i18n_384f337da1') }}<b>{{ $t('i18n_50fb61ef9d') }}</b
              >{{ $t('i18n_50d2671541') }}
            </li>
            <li>{{ $t('i18n_770a07d78f') }}</li>
            <li>{{ $t('i18n_83ccef50cd') }}</li>
          </ul>
        </template>
      </a-alert>
      <a-form :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
        <a-form-item> </a-form-item>
        <a-form-item :label="$t('i18n_b4a8c78284')" name="workspaceId">
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
            :placeholder="$t('i18n_b3bda9bf9e')"
          >
            <a-select-option v-for="item in workspaceList" :key="item.id" :disabled="getWorkspaceId() === item.id">{{
              item.name
            }}</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </CustomModal>
    <!-- 触发器 -->
    <CustomModal
      v-if="triggerVisible"
      v-model:open="triggerVisible"
      destroy-on-close
      :title="$t('i18n_4696724ed3')"
      width="50%"
      :footer="null"
      :mask-closable="false"
    >
      <a-form ref="editTriggerForm" :rules="rules" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-tabs default-active-key="1">
          <template #rightExtra>
            <a-tooltip :title="$t('i18n_01ad26f4a9')">
              <a-button type="primary" size="small" @click="resetTrigger">{{ $t('i18n_4b9c3271dc') }}</a-button>
            </a-tooltip>
          </template>
          <a-tab-pane key="1" :tab="$t('i18n_1a6aa24e76')">
            <a-space direction="vertical" style="width: 100%">
              <a-alert :message="$t('i18n_947d983961')" type="warning">
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
                  <a-typography-paragraph :copyable="{ text: temp.triggerUrl }">
                    <a-tag>GET</a-tag> <span>{{ temp.triggerUrl }} </span>
                  </a-typography-paragraph>
                </template>
              </a-alert>
              <a-alert type="info" :message="`${$t('i18n_8d202b890c')}(${$t('i18n_00a070c696')})`">
                <template #description>
                  <a-typography-paragraph :copyable="{ text: temp.batchTriggerUrl }">
                    <a-tag>POST</a-tag> <span>{{ temp.batchTriggerUrl }} </span>
                  </a-typography-paragraph>
                </template>
              </a-alert>
            </a-space>
          </a-tab-pane>
        </a-tabs>
      </a-form>
    </CustomModal>
    <!-- 脚本日志 -->
    <CustomDrawer
      v-if="drawerLogVisible"
      destroy-on-close
      :title="$t('i18n_6863e2a7b5')"
      width="70vw"
      :open="drawerLogVisible"
      @close="
        () => {
          drawerLogVisible = false
        }
      "
    >
      <script-log v-if="drawerLogVisible" :script-id="temp.id" />
    </CustomDrawer>
    <!-- pages.script.script-list.a36f20d3 -->
    <CustomDrawer
      v-if="scriptLibraryVisible"
      destroy-on-close
      :title="$t('i18n_53bdd93fd6')"
      placement="right"
      :open="scriptLibraryVisible"
      width="85vw"
      :footer-style="{ textAlign: 'right' }"
      @close="
        () => {
          scriptLibraryVisible = false
        }
      "
    >
      <ScriptLibraryNoPermission
        v-if="scriptLibraryVisible"
        ref="scriptLibraryRef"
        @script-confirm="
          (script) => {
            temp = { ...temp, context: script }
            scriptLibraryVisible = false
          }
        "
        @tag-confirm="
          (tag) => {
            temp = { ...temp, context: (temp.context || '') + `\nG@(\&quot;${tag}\&quot;)` }
            scriptLibraryVisible = false
          }
        "
      ></ScriptLibraryNoPermission>
      <template #footer>
        <a-space>
          <a-button
            @click="
              () => {
                scriptLibraryVisible = false
              }
            "
            >{{ $t('i18n_625fb26b4b') }}</a-button
          >
          <a-button
            type="primary"
            @click="
              () => {
                $refs['scriptLibraryRef'].handerScriptConfirm()
              }
            "
            >{{ $t('i18n_f71316d0dd') }}</a-button
          >
          <a-button
            type="primary"
            @click="
              () => {
                $refs['scriptLibraryRef'].handerTagConfirm()
              }
            "
            >{{ $t('i18n_9300692fac') }}</a-button
          >
        </a-space>
      </template>
    </CustomDrawer>

    <!-- <div style="padding-top: 50px" v-if="mode === 'choose'">
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
           {{$t('i18n_625fb26b4b')}}
          </a-button>
          <a-button type="primary" @click="handerConfirm"> {{$t('i18n_e83a256e4f')}} </a-button>
        </a-space>
      </div>
    </div> -->
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
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'
import { CRON_DATA_SOURCE } from '@/utils/const-i18n'
import { getWorkSpaceListAll } from '@/api/workspace'
import ScriptLog from '@/pages/script/script-log'
import ScriptLibraryNoPermission from '@/pages/system/assets/script-library/no-permission'
import { mapState } from 'pinia'
import { useAppStore } from '@/stores/app'
export default {
  components: {
    ScriptConsole,
    codeEditor,
    ScriptLog,
    ScriptLibraryNoPermission
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
  emits: ['cancel', 'confirm'],
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
          title: this.$t('i18n_d7ec2d3fea'),
          dataIndex: 'name',
          ellipsis: true,
          sorter: true,
          width: 150
        },
        {
          title: this.$t('i18n_fffd3ce745'),
          dataIndex: 'workspaceId',
          sorter: true,
          ellipsis: true,

          width: '90px'
        },
        {
          title: this.$t('i18n_3bdd08adab'),
          dataIndex: 'description',
          ellipsis: true,
          width: 100,
          tooltip: true
        },
        {
          title: this.$t('i18n_6b2e348a2b'),
          dataIndex: 'autoExecCron',
          ellipsis: true,
          sorter: true,
          width: '100px',
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
          tooltip: true,
          width: '120px'
        },
        {
          title: this.$t('i18n_9baca0054e'),
          dataIndex: 'modifyUser',
          ellipsis: true,
          tooltip: true,
          width: '120px'
        },
        {
          title: this.$t('i18n_f105c1d31d'),
          dataIndex: 'lastRunUser',
          ellipsis: true,
          width: '120px',
          tooltip: true
        },
        this.mode === 'manage'
          ? {
              title: this.$t('i18n_2b6bc0f293'),
              dataIndex: 'operation',
              align: 'center',

              fixed: 'right',
              width: '240px'
            }
          : {
              title: this.$t('i18n_2b6bc0f293'),
              dataIndex: 'operation',
              align: 'center',

              fixed: 'right',
              width: '100px'
            }
      ],

      rules: {
        name: [{ required: true, message: this.$t('i18n_fb7b9876a6'), trigger: 'blur' }],
        context: [{ required: true, message: this.$t('i18n_da1cb76e87'), trigger: 'blur' }]
      },
      tableSelections: [],
      syncToWorkspaceVisible: false,
      workspaceList: [],
      triggerVisible: false,
      commandParams: [],
      drawerLogVisible: false,
      confirmLoading: false,
      scriptLibraryVisible: false
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
      this.$refs['editScriptForm'].validate().then(() => {
        if (this.commandParams && this.commandParams.length > 0) {
          for (let i = 0; i < this.commandParams.length; i++) {
            if (!this.commandParams[i].desc) {
              $notification.error({
                message: this.$t('i18n_8ae2b9915c') + (i + 1) + this.$t('i18n_c583b707ba')
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
        title: this.$t('i18n_c4535759ee'),
        content: this.$t('i18n_3b19b2a75c'),
        zIndex: 1009,
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
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
      this.drawerTitle = `${this.$t('i18n_b5c3770699')}(${this.temp.name})`
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
        "<b style='font-size: 20px;'>this.$t('i18n_b57647c5aa')</b>" +
        "<ul style='font-size: 20px;color:red;font-weight: bold;'>" +
        `<li>${this.$t('i18n_56230405ae')}</b></li>` +
        `<li>${this.$t('i18n_5c93055d9c')}</li>` +
        `<li>${this.$t('i18n_27d0c8772c')}</li>` +
        '</ul>'
      $confirm({
        title: this.$t('i18n_9362e6ddf8'),
        zIndex: 1009,
        content: h('div', null, [h('p', { innerHTML: html }, null)]),
        okButtonProps: { type: 'primary', danger: true, size: 'small' },
        cancelButtonProps: { type: 'primary' },
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
          message: this.$t('i18n_b3bda9bf9e')
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
    },
    handleLog(record) {
      this.temp = Object.assign({}, record)

      this.drawerLogVisible = true
    },
    handerConfirm() {
      if (!this.tableSelections.length) {
        $notification.warning({
          message: this.$t('i18n_22670d3682')
        })
        return
      }
      if (this.choose === 'checkbox') {
        this.$emit('confirm', this.tableSelections.join(','))
      } else {
        const selectData = this.list.filter((item) => {
          return item.id === this.tableSelections[0]
        })[0]
        this.$emit('confirm', `${selectData.id}`)
      }
    }
  }
}
</script>
