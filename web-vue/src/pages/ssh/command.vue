<template>
  <div>
    <CustomTable
      is-show-tools
      default-auto-refresh
      :auto-refresh-time="30"
      :active-page="activePage"
      table-name="ssh-command-list"
      :empty-description="$t('pages.ssh.command.14c9322f')"
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
            :placeholder="$t('pages.ssh.command.b9a2e945')"
            class="search-input-item"
            @press-enter="getCommandData"
          />
          <a-input
            v-model:value="listQuery['%desc%']"
            :placeholder="$t('pages.ssh.command.42e3c32a')"
            class="search-input-item"
            @press-enter="getCommandData"
          />
          <a-input
            v-model:value="listQuery['%autoExecCron%']"
            :placeholder="$t('pages.ssh.command.8af26515')"
            class="search-input-item"
            @press-enter="getCommandData"
          />
          <a-tooltip :title="$t('pages.ssh.command.cb5a8131')">
            <a-button type="primary" :loading="loading" @click="getCommandData">{{
              $t('pages.ssh.command.53c2763c')
            }}</a-button>
          </a-tooltip>
          <a-button type="primary" @click="createCommand">{{ $t('pages.ssh.command.e141baa9') }}</a-button>
          <a-dropdown>
            <a @click="(e) => e.preventDefault()"> {{ $t('pages.ssh.command.6e071067') }} <DownOutlined /> </a>
            <template #overlay>
              <a-menu>
                <a-menu-item>
                  <a-button
                    type="primary"
                    :disabled="!tableSelections || !tableSelections.length"
                    @click="syncToWorkspaceShow"
                    >{{ $t('pages.ssh.command.ff284043') }}</a-button
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
            <div>{{ $t('pages.ssh.command.fec2ae8a') }}</div>

            <div>
              <ul>
                <li>{{ $t('pages.ssh.command.c963a5d') }}</li>
                <li>{{ $t('pages.ssh.command.7aafc560') }}</li>
                <li>
                  {{ $t('pages.ssh.command.69f8dae') }}<b>#disabled-template-auto-evn</b>
                  {{ $t('pages.ssh.command.cdfa84c1') }}({{ $t('pages.ssh.command.908d46ad') }})
                </li>
                <li>{{ $t('pages.ssh.command.e62fd265') }}</li>
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
            <a-button size="small" type="primary" @click="handleExecute(record)">{{
              $t('pages.ssh.command.178cb122')
            }}</a-button>
            <a-button size="small" type="primary" @click="handleEdit(record)">{{
              $t('pages.ssh.command.64603c01')
            }}</a-button>
            <a-button size="small" type="primary" @click="handleTrigger(record)">{{
              $t('pages.ssh.command.e81c0988')
            }}</a-button>
            <a-button size="small" type="primary" danger @click="handleDelete(record)">{{
              $t('pages.ssh.command.dd20d11c')
            }}</a-button>
          </a-space>
        </template>
      </template>
    </CustomTable>
    <!-- 编辑命令 -->
    <CustomModal
      v-if="editCommandVisible"
      v-model:open="editCommandVisible"
      destroy-on-close
      width="80vw"
      :title="$t('pages.ssh.command.b9ce0833')"
      :mask-closable="false"
      :confirm-loading="confirmLoading"
      @ok="handleEditCommandOk"
    >
      <a-form ref="editCommandForm" :rules="rules" :model="temp" :label-col="{ span: 3 }" :wrapper-col="{ span: 20 }">
        <a-form-item :label="$t('pages.ssh.command.ee8d727b')" name="name">
          <a-input v-model:value="temp.name" :max-length="100" :placeholder="$t('pages.ssh.command.ee8d727b')" />
        </a-form-item>

        <a-form-item name="command" :help="$t('pages.ssh.command.daf64952')">
          <template #label>
            <a-tooltip>
              {{ $t('pages.ssh.command.f0d88095') }}
              <template #title>
                <ul>
                  <li>{{ $t('pages.ssh.command.758e1c5d') }}</li>
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
              :show-tool="true"
            >
              <template #tool_before>
                <a-button type="link" @click="scriptLibraryVisible = true">脚本库 </a-button>
              </template>
            </code-editor>
          </a-form-item-rest>
        </a-form-item>
        <a-form-item :label="$t('pages.ssh.command.9266c899')">
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
            :placeholder="$t('pages.ssh.command.728b3e2f')"
            mode="multiple"
          >
            <a-select-option v-for="item in sshList" :key="item.id" :value="item.id">
              {{ item.name }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item :label="$t('pages.ssh.command.74765338')">
          <a-form-item-rest>
            <a-space direction="vertical" style="width: 100%">
              <a-row v-for="(item, index) in commandParams" :key="item.key">
                <a-col :span="22">
                  <a-space direction="vertical" style="width: 100%">
                    <a-input
                      v-model:value="item.desc"
                      :addon-before="$t('pages.ssh.command.b31dbb3', { index: index + 1 })"
                      :placeholder="
                        $t('pages.ssh.command.4676a7bb', {
                          slot1: $t('pages.ssh.command.6d8e653f')
                        })
                      "
                    />
                    <a-input
                      v-model:value="item.value"
                      :addon-before="$t('pages.ssh.command.5b6ebe20', { index: index + 1 })"
                      :placeholder="`${$t('pages.ssh.command.20e8812c')}${$t('pages.ssh.command.fcb85bf9')}`"
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
              <a-button type="primary" @click="() => commandParams.push({})">{{
                $t('pages.ssh.command.3ab8c616')
              }}</a-button>
            </a-space>
          </a-form-item-rest>
        </a-form-item>
        <a-form-item :label="$t('pages.ssh.command.4c297ada')" name="autoExecCron">
          <a-auto-complete
            v-model:value="temp.autoExecCron"
            :placeholder="$t('pages.ssh.command.50fcce7a')"
            :options="CRON_DATA_SOURCE"
          >
            <template #option="item"> {{ item.title }} {{ item.value }} </template>
          </a-auto-complete>
        </a-form-item>
        <a-form-item :label="$t('pages.ssh.command.45fc5edb')" name="desc">
          <a-textarea
            v-model:value="temp.desc"
            :max-length="255"
            :rows="3"
            style="resize: none"
            :placeholder="$t('pages.ssh.command.e83e719b')"
          />
        </a-form-item>
      </a-form>
    </CustomModal>

    <a-modal
      v-model:open="executeCommandVisible"
      destroy-on-close
      width="600px"
      :title="$t('pages.ssh.command.3e1a05c7')"
      :mask-closable="false"
      :confirm-loading="confirmLoading"
      @ok="handleExecuteCommandOk"
    >
      <a-form :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item :label="$t('pages.ssh.command.ee8d727b')" name="name">
          <a-input v-model:value="temp.name" :disabled="true" :placeholder="$t('pages.ssh.command.ee8d727b')" />
        </a-form-item>

        <a-form-item :label="$t('pages.ssh.command.9266c899')" required>
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
            :placeholder="$t('pages.ssh.command.fe590413')"
          >
            <a-select-option v-for="item in sshList" :key="item.id" :value="item.id">
              {{ item.name }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item
          :label="$t('pages.ssh.command.902dc6b3')"
          :help="`${commandParams.length ? $t('pages.ssh.command.1d6f7ac3') : ''}`"
        >
          <a-space direction="vertical" style="width: 100%">
            <a-row v-for="(item, index) in commandParams" :key="item.key">
              <a-col :span="22">
                <a-input
                  v-model:value="item.value"
                  :addon-before="`${$t('pages.ssh.command.2e7b13a9')}${index + 1}${$t('pages.ssh.command.9479b219')}`"
                  :placeholder="`${$t('pages.ssh.command.2e7b13a9')}${$t('pages.ssh.command.9479b219')} ${
                    item.desc ? ',' + item.desc : ''
                  }`"
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
            <a-button type="primary" @click="() => commandParams.push({})">{{
              $t('pages.ssh.command.3ab8c616')
            }}</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-modal>
    <!-- 执行日志 -->
    <a-modal
      v-model:open="logVisible"
      destroy-on-close
      :width="'80vw'"
      :title="$t('pages.ssh.command.c6e39f6f')"
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
      :title="$t('pages.ssh.command.d3b55aa0')"
      :mask-closable="false"
      @ok="handleSyncToWorkspace"
    >
      <a-alert :message="$t('pages.ssh.command.43bd326')" type="warning" show-icon>
        <template #description>
          <ul>
            <li>
              {{ $t('pages.ssh.command.b74cd503') }}<b>{{ $t('pages.ssh.command.db9bba81') }}</b
              >{{ $t('pages.ssh.command.51c8f417') }}
            </li>
            <li>{{ $t('pages.ssh.command.7a9fb76a') }}</li>
            <li>{{ $t('pages.ssh.command.ddda79c1') }}</li>
          </ul>
        </template>
      </a-alert>
      <a-form :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
        <a-form-item> </a-form-item>
        <a-form-item :label="$t('pages.ssh.command.7ef9d8fb')" name="workspaceId">
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
            :placeholder="$t('pages.ssh.command.3a321a02')"
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
      :title="$t('pages.ssh.command.e81c0988')"
      width="50%"
      :footer="null"
      :mask-closable="false"
    >
      <a-form ref="editTriggerForm" :rules="rules" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-tabs default-active-key="1">
          <template #rightExtra>
            <a-tooltip :title="$t('pages.ssh.command.e172ddd8')">
              <a-button type="primary" size="small" @click="resetTrigger">{{
                $t('pages.ssh.command.da1d2343')
              }}</a-button>
            </a-tooltip>
          </template>
          <a-tab-pane key="1" :tab="$t('pages.ssh.command.178cb122')">
            <a-space direction="vertical" style="width: 100%">
              <a-alert :message="$t('pages.ssh.command.43bd326')" type="warning">
                <template #description>
                  <ul>
                    <li>{{ $t('pages.ssh.command.599402f8') }}</li>
                    <li>{{ $t('pages.ssh.command.390d25c1') }}</li>
                    <li>{{ $t('pages.ssh.command.789c025c') }}</li>
                    <li>{{ $t('pages.ssh.command.9291c817') }}</li>
                  </ul>
                </template>
              </a-alert>
              <a-alert
                type="info"
                :message="`${$t('pages.ssh.command.627ba85c')}(${$t('pages.ssh.command.2dad29ac')})`"
              >
                <template #description>
                  <a-typography-paragraph :copyable="{ tooltip: false, text: temp.triggerUrl }">
                    <a-tag>GET</a-tag> <span>{{ temp.triggerUrl }} </span>
                  </a-typography-paragraph>
                </template>
              </a-alert>
              <a-alert
                type="info"
                :message="`${$t('pages.ssh.command.c4994a4e')}(${$t('pages.ssh.command.2dad29ac')})`"
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
            temp = { ...temp, command: script }
            scriptLibraryVisible = false
          }
        "
        @tag-confirm="
          (tag) => {
            temp = { ...temp, command: (temp.command || '') + `\nG@(\&quot;${tag}\&quot;)` }
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
  </div>
</template>
<script>
import { deleteCommand, editCommand, executeBatch, getCommandList, syncToWorkspace, getTriggerUrl } from '@/api/command'
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'
import { CRON_DATA_SOURCE } from '@/utils/const-i18n'
import { getSshListAll } from '@/api/ssh'
import codeEditor from '@/components/codeEditor'
import CommandLog from './command-view-log'
import ScriptLibraryNoPermission from '@/pages/system/assets/script-library/no-permission'
import { getWorkSpaceListAll } from '@/api/workspace'
import { mapState } from 'pinia'
import { useAppStore } from '@/stores/app'
export default {
  components: { codeEditor, CommandLog, ScriptLibraryNoPermission },
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
          title: this.$t('pages.ssh.command.ee8d727b'),
          dataIndex: 'name',
          ellipsis: true,
          width: 200
        },
        {
          title: this.$t('pages.ssh.command.45fc5edb'),
          dataIndex: 'desc',
          ellipsis: true,
          width: 250
        },
        {
          title: this.$t('pages.ssh.command.8af26515'),
          dataIndex: 'autoExecCron',
          ellipsis: true,
          width: 120
        },
        {
          title: this.$t('pages.ssh.command.f5b90169'),
          dataIndex: 'createTimeMillis',
          ellipsis: true,
          sorter: true,
          customRender: ({ text }) => {
            return parseTime(text)
          },
          width: '170px'
        },
        {
          title: this.$t('pages.ssh.command.3d55d8de'),
          dataIndex: 'modifyTimeMillis',
          width: '170px',
          ellipsis: true,
          sorter: true,
          customRender: ({ text }) => {
            return parseTime(text)
          }
        },
        {
          title: this.$t('pages.ssh.command.358d534a'),
          dataIndex: 'modifyUser',
          width: 120,
          ellipsis: true
        },
        {
          title: this.$t('pages.ssh.command.3bb962bf'),
          dataIndex: 'operation',
          align: 'center',

          fixed: 'right',
          width: '240px'
        }
      ],
      scriptLibraryVisible: false,
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
    // 编辑命令信息
    handleEditCommandOk() {
      this.$refs['editCommandForm'].validate().then(() => {
        if (this.commandParams && this.commandParams.length > 0) {
          for (let i = 0; i < this.commandParams.length; i++) {
            if (!this.commandParams[i].desc) {
              $notification.error({
                message: this.$t('pages.ssh.command.60a5d42c') + (i + 1) + this.$t('pages.ssh.command.7bedc1fb')
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
        title: this.$t('pages.ssh.command.b22d55a0'),
        zIndex: 1009,
        content: this.$t('pages.ssh.command.bddce4ff') + row.name + this.$t('pages.ssh.command.2d94a438'),
        okText: this.$t('pages.ssh.command.e8e9db25'),
        cancelText: this.$t('pages.ssh.command.b12468e9'),
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
          message: this.$t('pages.ssh.command.7872c5ad')
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
          message: this.$t('pages.ssh.command.3a321a02')
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
