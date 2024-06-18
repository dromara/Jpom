<template>
  <div>
    <CustomTable
      is-show-tools
      default-auto-refresh
      :auto-refresh-time="30"
      :active-page="activePage"
      table-name="ssh-command-list"
      :empty-description="$t('i18n_ba17b17ba2')"
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
            :placeholder="$t('i18n_9c2a917905')"
            class="search-input-item"
            @press-enter="getCommandData"
          />
          <a-input
            v-model:value="listQuery['%desc%']"
            :placeholder="$t('i18n_3bdd08adab')"
            class="search-input-item"
            @press-enter="getCommandData"
          />
          <a-input
            v-model:value="listQuery['%autoExecCron%']"
            :placeholder="$t('i18n_6b2e348a2b')"
            class="search-input-item"
            @press-enter="getCommandData"
          />
          <a-tooltip :title="$t('i18n_4838a3bd20')">
            <a-button type="primary" :loading="loading" @click="getCommandData">{{ $t('i18n_e5f71fc31e') }}</a-button>
          </a-tooltip>
          <a-button type="primary" @click="createCommand">{{ $t('i18n_66ab5e9f24') }}</a-button>
          <a-dropdown>
            <a @click="(e) => e.preventDefault()"> {{ $t('i18n_0ec9eaf9c3') }} <DownOutlined /> </a>
            <template #overlay>
              <a-menu>
                <a-menu-item>
                  <a-button
                    type="primary"
                    :disabled="!tableSelections || !tableSelections.length"
                    @click="syncToWorkspaceShow"
                    >{{ $t('i18n_398ce396cd') }}</a-button
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
            <div>{{ $t('i18n_4826549b41') }}</div>

            <div>
              <ul>
                <li>{{ $t('i18n_5ef72bdfce') }}</li>
                <li>{{ $t('i18n_5d368ab0a5') }}</li>
                <li>
                  {{ $t('i18n_26f95520a5') }}<b>#disabled-template-auto-evn</b> {{ $t('i18n_bfacfcd978') }}({{
                    $t('i18n_8e872df7da')
                  }})
                </li>
                <li>{{ $t('i18n_2ea7e70e87') }}</li>
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
            <a-button size="small" type="primary" @click="handleExecute(record)">{{ $t('i18n_1a6aa24e76') }}</a-button>
            <a-button size="small" type="primary" @click="handleEdit(record)">{{ $t('i18n_95b351c862') }}</a-button>
            <a-button size="small" type="primary" @click="handleTrigger(record)">{{ $t('i18n_4696724ed3') }}</a-button>
            <a-button size="small" type="primary" danger @click="handleDelete(record)">{{
              $t('i18n_2f4aaddde3')
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
      :title="$t('i18n_9a0c5b150c')"
      :mask-closable="false"
      :confirm-loading="confirmLoading"
      @ok="handleEditCommandOk"
    >
      <a-form ref="editCommandForm" :rules="rules" :model="temp" :label-col="{ span: 3 }" :wrapper-col="{ span: 20 }">
        <a-form-item :label="$t('i18n_6496a5a043')" name="name">
          <a-input v-model:value="temp.name" :max-length="100" :placeholder="$t('i18n_6496a5a043')" />
        </a-form-item>

        <a-form-item name="command" :help="$t('i18n_77c1e73c08')">
          <template #label>
            <a-tooltip>
              {{ $t('i18n_ccb91317c5') }}
              <template #title>
                <ul>
                  <li>{{ $t('i18n_5fbde027e3') }}</li>
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
                <a-button type="link" @click="scriptLibraryVisible = true">{{ $t('i18n_f685377a22') }}</a-button>
              </template>
            </code-editor>
          </a-form-item-rest>
        </a-form-item>
        <a-form-item :label="$t('i18n_b0b9df58fd')">
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
            :placeholder="$t('i18n_649f8046f3')"
            mode="multiple"
          >
            <a-select-option v-for="item in sshList" :key="item.id" :value="item.id">
              {{ item.name }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item :label="$t('i18n_2171d1b07d')">
          <a-form-item-rest>
            <a-space direction="vertical" style="width: 100%">
              <a-row v-for="(item, index) in commandParams" :key="item.key">
                <a-col :span="22">
                  <a-space direction="vertical" style="width: 100%">
                    <a-input
                      v-model:value="item.desc"
                      :addon-before="$t('i18n_417fa2c2be', { index: index + 1 })"
                      :placeholder="$t('i18n_3f414ade96', { slot1: $t('i18n_2b1015e902') })"
                    />
                    <a-input
                      v-model:value="item.value"
                      :addon-before="$t('i18n_620489518c', { index: index + 1 })"
                      :placeholder="`${$t('i18n_bfed4943c5')}${$t('i18n_e9f2c62e54')}`"
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
              <a-button type="primary" @click="() => commandParams.push({})">{{ $t('i18n_4c0eead6ff') }}</a-button>
            </a-space>
          </a-form-item-rest>
        </a-form-item>
        <a-form-item :label="$t('i18n_df39e42127')" name="autoExecCron">
          <a-auto-complete
            v-model:value="temp.autoExecCron"
            :placeholder="$t('i18n_5dff0d31d0')"
            :options="CRON_DATA_SOURCE"
          >
            <template #option="item"> {{ item.title }} {{ item.value }} </template>
          </a-auto-complete>
        </a-form-item>
        <a-form-item :label="$t('i18n_bf91239ad7')" name="desc">
          <a-textarea
            v-model:value="temp.desc"
            :max-length="255"
            :rows="3"
            style="resize: none"
            :placeholder="$t('i18n_81d7d5cd8a')"
          />
        </a-form-item>
      </a-form>
    </CustomModal>

    <CustomModal
      v-if="executeCommandVisible"
      v-model:open="executeCommandVisible"
      destroy-on-close
      width="600px"
      :title="$t('i18n_bb4740c7a7')"
      :mask-closable="false"
      :confirm-loading="confirmLoading"
      @ok="handleExecuteCommandOk"
    >
      <a-form :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item :label="$t('i18n_6496a5a043')" name="name">
          <a-input v-model:value="temp.name" :disabled="true" :placeholder="$t('i18n_6496a5a043')" />
        </a-form-item>

        <a-form-item :label="$t('i18n_b0b9df58fd')" required>
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
            :placeholder="$t('i18n_e43359ca06')"
          >
            <a-select-option v-for="item in sshList" :key="item.id" :value="item.id">
              {{ item.name }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item :label="$t('i18n_abba4775e1')" :help="`${commandParams.length ? $t('i18n_916cde39c4') : ''}`">
          <a-space direction="vertical" style="width: 100%">
            <a-row v-for="(item, index) in commandParams" :key="item.key">
              <a-col :span="22">
                <a-input
                  v-model:value="item.value"
                  :addon-before="`${$t('i18n_3d0a2df9ec')}${index + 1}${$t('i18n_fe7509e0ed')}`"
                  :placeholder="`${$t('i18n_3d0a2df9ec')}${$t('i18n_fe7509e0ed')} ${item.desc ? ',' + item.desc : ''}`"
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
            <a-button type="primary" @click="() => commandParams.push({})">{{ $t('i18n_4c0eead6ff') }}</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </CustomModal>
    <!-- 执行日志 -->
    <CustomModal
      v-if="logVisible"
      v-model:open="logVisible"
      destroy-on-close
      :width="'80vw'"
      :title="$t('i18n_c84ddfe8a6')"
      :footer="null"
      :mask-closable="false"
    >
      <command-log v-if="logVisible" :temp="temp" />
    </CustomModal>
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
            <li>{{ $t('i18n_b5d2cf4a76') }}</li>
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
                    <li>{{ $t('i18n_05e78c26b1') }}</li>
                    <li>{{ $t('i18n_632a907224') }}</li>
                    <li>{{ $t('i18n_3fca26a684') }}</li>
                    <li>{{ $t('i18n_a04b7a8f5d') }}</li>
                  </ul>
                </template>
              </a-alert>
              <a-alert type="info" :message="`${$t('i18n_de78b73dab')}(${$t('i18n_00a070c696')})`">
                <template #description>
                  <a-typography-paragraph :copyable="{ tooltip: false, text: temp.triggerUrl }">
                    <a-tag>GET</a-tag> <span>{{ temp.triggerUrl }} </span>
                  </a-typography-paragraph>
                </template>
              </a-alert>
              <a-alert type="info" :message="`${$t('i18n_8d202b890c')}(${$t('i18n_00a070c696')})`">
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
    </CustomModal>
    <!-- pages.ssh.command.a36f20d3 -->
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
          title: this.$t('i18n_6496a5a043'),
          dataIndex: 'name',
          ellipsis: true,
          width: 200
        },
        {
          title: this.$t('i18n_bf91239ad7'),
          dataIndex: 'desc',
          ellipsis: true,
          width: 250
        },
        {
          title: this.$t('i18n_6b2e348a2b'),
          dataIndex: 'autoExecCron',
          ellipsis: true,
          width: 120
        },
        {
          title: this.$t('i18n_eca37cb072'),
          dataIndex: 'createTimeMillis',
          ellipsis: true,
          sorter: true,
          customRender: ({ text }) => {
            return parseTime(text)
          },
          width: '170px'
        },
        {
          title: this.$t('i18n_1303e638b5'),
          dataIndex: 'modifyTimeMillis',
          width: '170px',
          ellipsis: true,
          sorter: true,
          customRender: ({ text }) => {
            return parseTime(text)
          }
        },
        {
          title: this.$t('i18n_26c1f8d83e'),
          dataIndex: 'modifyUser',
          width: 120,
          ellipsis: true
        },
        {
          title: this.$t('i18n_2b6bc0f293'),
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
                message: this.$t('i18n_8ae2b9915c') + (i + 1) + this.$t('i18n_c583b707ba')
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
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_d921c4a0b6') + row.name + this.$t('i18n_c4a61acace'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
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
          message: this.$t('i18n_d7471c0261')
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
