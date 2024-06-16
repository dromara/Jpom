<template>
  <div>
    <!-- 数据表格 -->
    <CustomTable
      is-show-tools
      default-auto-refresh
      :auto-refresh-time="30"
      :active-page="activePage"
      table-name="server-script-list"
      :empty-description="$t('pages.script.script-list.4d8019f5')"
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
            :placeholder="$t('pages.script.script-list.461afc3f')"
            allow-clear
            class="search-input-item"
            @press-enter="loadData"
          />
          <a-input
            v-model:value="listQuery['%name%']"
            :placeholder="$t('pages.script.script-list.3e34ec28')"
            allow-clear
            class="search-input-item"
            @press-enter="loadData"
          />
          <a-input
            v-model:value="listQuery['%description%']"
            :placeholder="$t('pages.script.script-list.4b2e093e')"
            class="search-input-item"
            @press-enter="loadData"
          />
          <a-input
            v-model:value="listQuery['%autoExecCron%']"
            :placeholder="$t('pages.script.script-list.8d2c7731')"
            class="search-input-item"
            @press-enter="loadData"
          />
          <a-tooltip :title="$t('pages.script.script-list.6ebd5885')">
            <a-button :loading="loading" type="primary" @click="loadData">{{
              $t('pages.script.script-list.53c2763c')
            }}</a-button>
          </a-tooltip>
          <a-button type="primary" @click="createScript">{{ $t('pages.script.script-list.7d46652a') }}</a-button>
          <a-button
            v-if="mode === 'manage'"
            type="primary"
            :disabled="!tableSelections || !tableSelections.length"
            @click="syncToWorkspaceShow"
            >{{ $t('pages.script.script-list.ff284043') }}</a-button
          >
        </a-space>
      </template>
      <template #tableHelp>
        <a-tooltip>
          <template #title>
            <div>{{ $t('pages.script.script-list.a16c114a') }}</div>

            <div>
              <ul>
                <li>{{ $t('pages.script.script-list.75b12f79') }}</li>
                <li>{{ $t('pages.script.script-list.e62fd265') }}</li>
                <li>{{ $t('pages.script.script-list.7854fb71') }}</li>
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
          <a-tag v-if="text === 'GLOBAL'">{{ $t('pages.script.script-list.fd0310d0') }}</a-tag>
          <a-tag v-else>{{ $t('pages.script.script-list.afacc4cb') }}</a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <template v-if="mode === 'manage'">
              <a-button size="small" type="primary" @click="handleExec(record)">{{
                $t('pages.script.script-list.d14160b')
              }}</a-button>
              <a-button size="small" type="primary" @click="handleEdit(record)">{{
                $t('pages.script.script-list.e1224c34')
              }}</a-button>
              <a-button size="small" type="primary" @click="handleLog(record)">{{
                $t('pages.script.script-list.f637e08')
              }}</a-button>
              <a-dropdown>
                <a @click="(e) => e.preventDefault()">
                  {{ $t('pages.script.script-list.6e071067') }}
                  <DownOutlined />
                </a>
                <template #overlay>
                  <a-menu>
                    <a-menu-item>
                      <a-button size="small" type="primary" @click="handleTrigger(record)">{{
                        $t('pages.script.script-list.e81c0988')
                      }}</a-button>
                    </a-menu-item>

                    <a-menu-item>
                      <a-button size="small" type="primary" danger @click="handleDelete(record)">{{
                        $t('pages.script.script-list.dd20d11c')
                      }}</a-button>
                    </a-menu-item>
                    <a-menu-item>
                      <a-button
                        size="small"
                        type="primary"
                        danger
                        :disabled="!record.nodeIds"
                        @click="handleUnbind(record)"
                        >{{ $t('pages.script.script-list.4c957529') }}</a-button
                      >
                    </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </template>
            <template v-else>
              <a-button size="small" type="primary" @click="handleEdit(record)">{{
                $t('pages.script.script-list.e1224c34')
              }}</a-button>
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
      :title="$t('pages.script.script-list.c05890d1')"
      :mask-closable="false"
      width="80vw"
      :confirm-loading="confirmLoading"
      @ok="handleEditScriptOk"
    >
      <a-form ref="editScriptForm" :rules="rules" :model="temp" :label-col="{ span: 3 }" :wrapper-col="{ span: 19 }">
        <a-form-item v-if="temp.id" label="ScriptId" name="id">
          <a-input v-model:value="temp.id" disabled read-only />
        </a-form-item>
        <a-form-item :label="$t('pages.script.script-list.db9bba81')" name="name">
          <a-input v-model:value="temp.name" :max-length="50" :placeholder="$t('pages.script.script-list.3e34ec28')" />
        </a-form-item>
        <a-form-item :label="$t('pages.script.script-list.709314dd')" name="context">
          <a-form-item-rest>
            <code-editor
              v-model:content="temp.context"
              height="40vh"
              :show-tool="true"
              :options="{ mode: 'shell', tabSize: 2 }"
            >
              <template #tool_before>
                <a-button type="link" @click="scriptLibraryVisible = true">脚本库 </a-button>
              </template>
            </code-editor>
          </a-form-item-rest>
        </a-form-item>

        <a-form-item :label="$t('pages.script.script-list.74765338')">
          <a-space direction="vertical" style="width: 100%">
            <a-row v-for="(item, index) in commandParams" :key="item.key">
              <a-col :span="22">
                <a-space direction="vertical" style="width: 100%">
                  <a-input
                    v-model:value="item.desc"
                    :addon-before="$t('pages.script.script-list.17a3e7b', { count: index + 1 })"
                    :placeholder="$t('pages.script.script-list.16fa5a7d')" />
                  <a-input
                    v-model:value="item.value"
                    :addon-before="$t('pages.script.script-list.a94da34e', { count: index + 1 })"
                    :placeholder="$t('pages.script.script-list.8ff30bc7')"
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

          <a-button type="primary" @click="() => commandParams.push({})">{{
            $t('pages.script.script-list.c3495d0')
          }}</a-button>
        </a-form-item>
        <a-form-item :label="$t('pages.script.script-list.8d2c7731')" name="autoExecCron">
          <a-auto-complete
            v-model:value="temp.autoExecCron"
            :placeholder="$t('pages.script.script-list.50fcce7a')"
            :options="CRON_DATA_SOURCE"
          >
            <template #option="item"> {{ item.title }} {{ item.value }} </template>
          </a-auto-complete>
        </a-form-item>
        <a-form-item :label="$t('pages.script.script-list.4b2e093e')" name="description">
          <a-textarea
            v-model:value="temp.description"
            :max-length="200"
            :rows="3"
            style="resize: none"
            :placeholder="$t('pages.script.script-list.419e634e')"
          />
        </a-form-item>
        <a-form-item :label="$t('pages.script.script-list.33b08707')" name="global">
          <a-radio-group v-model:value="temp.global">
            <a-radio :value="true"> {{ $t('pages.script.script-list.fd0310d0') }}</a-radio>
            <a-radio :value="false"> {{ $t('pages.script.script-list.919267cc') }}</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item v-if="temp.prohibitSync" :label="$t('pages.script.script-list.f4167923')">
          <template #help>{{ $t('pages.script.script-list.5a0d0c9f') }}</template>
          <a-tag v-for="(item, index) in temp.nodeList" :key="index"
            >{{ $t('pages.script.script-list.fa8d810f') }}{{ item.nodeName }}
            {{ $t('pages.script.script-list.19b80eba') }}{{ item.workspaceName }}</a-tag
          >
        </a-form-item>
        <a-form-item v-else>
          <template #label>
            <a-tooltip>
              {{ $t('pages.script.script-list.aa8dba24') }}
              <template #title> {{ $t('pages.script.script-list.f8f43b51') }} </template>
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
            :placeholder="$t('pages.script.script-list.1a5a95a')"
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
      :title="$t('pages.script.script-list.d3b55aa0')"
      :mask-closable="false"
      @ok="handleSyncToWorkspace"
    >
      <a-alert :message="$t('pages.script.script-list.da2f7ff0')" type="warning" show-icon>
        <template #description>
          <ul>
            <li>
              {{ $t('pages.script.script-list.b74cd503') }}<b>{{ $t('pages.script.script-list.24087b5d') }}</b
              >{{ $t('pages.script.script-list.81c3b287') }}
            </li>
            <li>{{ $t('pages.script.script-list.4d913a22') }}</li>
            <li>{{ $t('pages.script.script-list.936eff64') }}</li>
          </ul>
        </template>
      </a-alert>
      <a-form :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
        <a-form-item> </a-form-item>
        <a-form-item :label="$t('pages.script.script-list.7ef9d8fb')" name="workspaceId">
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
            :placeholder="$t('pages.script.script-list.3a321a02')"
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
      :title="$t('pages.script.script-list.e81c0988')"
      width="50%"
      :footer="null"
      :mask-closable="false"
    >
      <a-form ref="editTriggerForm" :rules="rules" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-tabs default-active-key="1">
          <template #rightExtra>
            <a-tooltip :title="$t('pages.script.script-list.e172ddd8')">
              <a-button type="primary" size="small" @click="resetTrigger">{{
                $t('pages.script.script-list.da1d2343')
              }}</a-button>
            </a-tooltip>
          </template>
          <a-tab-pane key="1" :tab="$t('pages.script.script-list.d14160b')">
            <a-space direction="vertical" style="width: 100%">
              <a-alert :message="$t('pages.script.script-list.da2f7ff0')" type="warning">
                <template #description>
                  <ul>
                    <li>{{ $t('pages.script.script-list.8f9bc485') }}</li>
                    <li>{{ $t('pages.script.script-list.21ae4cfc') }}</li>
                    <li>{{ $t('pages.script.script-list.789c025c') }}</li>
                    <li>{{ $t('pages.script.script-list.c4162f74') }}</li>
                  </ul>
                </template>
              </a-alert>
              <a-alert
                type="info"
                :message="`${$t('pages.script.script-list.2cd9ba62')}(${$t('pages.script.script-list.da836fdd')})`"
              >
                <template #description>
                  <a-typography-paragraph :copyable="{ text: temp.triggerUrl }">
                    <a-tag>GET</a-tag> <span>{{ temp.triggerUrl }} </span>
                  </a-typography-paragraph>
                </template>
              </a-alert>
              <a-alert
                type="info"
                :message="`${$t('pages.script.script-list.4bd083f4')}(${$t('pages.script.script-list.da836fdd')})`"
              >
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
      :title="$t('pages.script.script-list.85cd9f11')"
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
    <!-- 查看脚本库 -->
    <CustomDrawer
      v-if="scriptLibraryVisible"
      destroy-on-close
      title="查看脚本库"
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
          >
            取消
          </a-button>
          <a-button
            type="primary"
            @click="
              () => {
                $refs['scriptLibraryRef'].handerScriptConfirm()
              }
            "
          >
            替换引用
          </a-button>
          <a-button
            type="primary"
            @click="
              () => {
                $refs['scriptLibraryRef'].handerTagConfirm()
              }
            "
          >
            标记引用
          </a-button>
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
           {{$t('pages.script.script-list.43105e21')}}
          </a-button>
          <a-button type="primary" @click="handerConfirm"> {{$t('pages.script.script-list.7da4a591')}} </a-button>
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
          title: this.$t('pages.script.script-list.3e34ec28'),
          dataIndex: 'name',
          ellipsis: true,
          sorter: true,
          width: 150
        },
        {
          title: this.$t('pages.script.script-list.33b08707'),
          dataIndex: 'workspaceId',
          sorter: true,
          ellipsis: true,

          width: '90px'
        },
        {
          title: this.$t('pages.script.script-list.4b2e093e'),
          dataIndex: 'description',
          ellipsis: true,
          width: 100,
          tooltip: true
        },
        {
          title: this.$t('pages.script.script-list.8d2c7731'),
          dataIndex: 'autoExecCron',
          ellipsis: true,
          sorter: true,
          width: '100px',
          tooltip: true
        },
        {
          title: this.$t('pages.script.script-list.a2b40316'),
          dataIndex: 'modifyTimeMillis',
          sorter: true,
          width: '170px',
          ellipsis: true,
          customRender: ({ text }) => parseTime(text)
        },
        {
          title: this.$t('pages.script.script-list.f5b90169'),
          dataIndex: 'createTimeMillis',
          sorter: true,
          width: '170px',
          ellipsis: true,
          customRender: ({ text }) => parseTime(text)
        },
        {
          title: this.$t('pages.script.script-list.db3c9202'),
          dataIndex: 'createUser',
          ellipsis: true,
          tooltip: true,
          width: '120px'
        },
        {
          title: this.$t('pages.script.script-list.916db24b'),
          dataIndex: 'modifyUser',
          ellipsis: true,
          tooltip: true,
          width: '120px'
        },
        {
          title: this.$t('pages.script.script-list.15597f1f'),
          dataIndex: 'lastRunUser',
          ellipsis: true,
          width: '120px',
          tooltip: true
        },
        this.mode === 'manage'
          ? {
              title: this.$t('pages.script.script-list.cadc075'),
              dataIndex: 'operation',
              align: 'center',

              fixed: 'right',
              width: '240px'
            }
          : {
              title: this.$t('pages.script.script-list.cadc075'),
              dataIndex: 'operation',
              align: 'center',

              fixed: 'right',
              width: '100px'
            }
      ],

      rules: {
        name: [{ required: true, message: this.$t('pages.script.script-list.beb9cb37'), trigger: 'blur' }],
        context: [{ required: true, message: this.$t('pages.script.script-list.52049f49'), trigger: 'blur' }]
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
                message:
                  this.$t('pages.script.script-list.b2a4dc0e') + (i + 1) + this.$t('pages.script.script-list.94a5dd5e')
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
        title: this.$t('pages.script.script-list.e422d0eb'),
        content: this.$t('pages.script.script-list.9924957'),
        zIndex: 1009,
        okText: this.$t('pages.script.script-list.7da4a591'),
        cancelText: this.$t('pages.script.script-list.43105e21'),
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
      this.drawerTitle = `${this.$t('pages.script.script-list.5139b7d7')}(${this.temp.name})`
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
        "<b style='font-size: 20px;'>this.$t('pages.script.script-list.6d453736')</b>" +
        "<ul style='font-size: 20px;color:red;font-weight: bold;'>" +
        `<li>${this.$t('pages.script.script-list.2076b8b4')}</b></li>` +
        `<li>${this.$t('pages.script.script-list.f4e01a2c')}</li>` +
        `<li>${this.$t('pages.script.script-list.c354703b')}</li>` +
        '</ul>'
      $confirm({
        title: this.$t('pages.script.script-list.f2ce74e2'),
        zIndex: 1009,
        content: h('div', null, [h('p', { innerHTML: html }, null)]),
        okButtonProps: { type: 'primary', danger: true, size: 'small' },
        cancelButtonProps: { type: 'primary' },
        okText: this.$t('pages.script.script-list.7da4a591'),
        cancelText: this.$t('pages.script.script-list.43105e21'),
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
          message: this.$t('pages.script.script-list.3a321a02')
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
          message: this.$t('pages.script.script-list.3198b463')
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
