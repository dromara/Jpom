<template>
  <div>
    <CustomTable
      is-show-tools
      default-auto-refresh
      :auto-refresh-time="5"
      table-name="assets-ftp-list"
      :empty-description="$t('i18n_13d10a9b78')"
      :active-page="activePage"
      :data-source="list"
      :columns="columns"
      size="middle"
      :pagination="pagination"

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
            v-model:value="listQuery['%name%']"
            class="search-input-item"
            placeholder="ftp名称"
            @press-enter="loadData"
          />
          <a-input
            v-model:value="listQuery['%host%']"
            class="search-input-item"
            placeholder="host"
            @press-enter="loadData"
          />
          <a-select
            v-model:value="listQuery.groupName"
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
            allow-clear
            :placeholder="$t('i18n_829abe5a8d')"
            class="search-input-item"
          >
            <a-select-option v-for="item in groupList" :key="item">{{ item }}</a-select-option>
          </a-select>

          <a-tooltip :title="$t('i18n_4838a3bd20')">
            <a-button type="primary" :loading="loading" @click="loadData">{{ $t('i18n_e5f71fc31e') }}</a-button>
          </a-tooltip>

          <a-button type="primary" @click="handleAdd">{{ $t('i18n_66ab5e9f24') }}</a-button>
          <a-button :disabled="!tableSelections.length" type="primary" @click="syncToWorkspaceShow()">
            {{ $t('i18n_82d2c66f47') }}
          </a-button
          >
          <a-button type="primary" @click="handlerExportData()"
          >
            <DownloadOutlined/>
            {{ $t('i18n_55405ea6ff') }}
          </a-button
          >
          <a-dropdown>
            <template #overlay>
              <a-menu>
                <a-menu-item key="1">
                  <a-button type="primary" @click="handlerImportTemplate()">{{ $t('i18n_2e505d23f7') }}</a-button>
                </a-menu-item>
              </a-menu>
            </template>

            <a-upload
              name="file"
              accept=".csv"
              action=""
              :show-upload-list="false"
              :multiple="false"
              :before-upload="beforeUpload"
            >
              <a-button type="primary">
                <UploadOutlined/>
                {{ $t('i18n_8d9a071ee2') }}
                <DownOutlined/>
              </a-button>
            </a-upload>
          </a-dropdown>
        </a-space>
      </template>
      <template #tableHelp>
        <a-tooltip>
          <template #title>
            <div>
              <ul>
                <li>{{ $t('i18n_cc3a8457ea') }}</li>
                <li>{{ $t('i18n_c4b5d36ff0') }}</li>
                <li>{{ $t('i18n_1278df0cfc') }}</li>
              </ul>
            </div>
          </template>
          <QuestionCircleOutlined/>
        </a-tooltip>
      </template>
      <template #tableBodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'name'">
          <a-tooltip :title="text">
            <a-button style="padding: 0" type="link" size="small" @click="handleEdit(record)"> {{ text }}</a-button>
          </a-tooltip>
        </template>
        <template v-else-if="column.tooltip">
          <a-tooltip :title="text"> {{ text }}</a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'host'">
          <a-tooltip :title="`${text}:${record.port}`"> {{ text }}:{{ record.port }}</a-tooltip>
        </template>


        <template v-else-if="column.dataIndex === 'status'">
          <a-tooltip :title="`${record.statusMsg || $t('i18n_77e100e462')}`">
            <a-tag :color="statusMap[record.status] && statusMap[record.status].color">{{
                (statusMap[record.status] && statusMap[record.status].desc) || $t('i18n_1622dc9b6b')
              }}
            </a-tag>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'renderSize'">
          <a-tooltip placement="topLeft" :title="renderSize(text)">
            <span>{{ renderSize(text) }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button size="small" type="primary" @click="syncToWorkspaceShow(record)">{{
                $t('i18n_e39de3376e')
              }}
            </a-button>
            <a-button size="small" type="primary" @click="handleFile(record)">{{ $t('i18n_2a0c4740f1') }}</a-button>
            <a-button size="small" type="primary" @click="handleViewWorkspaceFtp(record)">{{
                $t('i18n_1c3cf7f5f0')
              }}
            </a-button>

            <a-dropdown>
              <a @click="(e) => e.preventDefault()">
                {{ $t('i18n_0ec9eaf9c3') }}
                <DownOutlined/>
              </a>
              <template #overlay>
                <a-menu>
                  <a-menu-item>
                    <a-button size="small" type="primary" @click="handleEdit(record)">{{
                        $t('i18n_95b351c862')
                      }}
                    </a-button>
                  </a-menu-item>
                  <a-menu-item>
                    <a-button size="small" type="primary" danger @click="handleDelete(record)">{{
                        $t('i18n_2f4aaddde3')
                      }}
                    </a-button>
                  </a-menu-item>
                  <!--                      <a-menu-item>
                                          <a-button size="small" type="primary" @click="handleViewLog(record)">{{
                                              $t('i18n_3ed3733078')
                                            }}
                                          </a-button>
                                        </a-menu-item>-->
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
        </template>
      </template>
    </CustomTable>
    <!-- 编辑区 -->
    <CustomModal
      v-if="editFtpVisible"
      v-model:open="editFtpVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      width="600px"
      title="编辑FTP"
      :mask-closable="false"
      @ok="handleEditFtpOk"
    >
      <a-form ref="editFtpForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="FTP名称" name="name">
          <a-input v-model:value="temp.name" :max-length="50" placeholder="FTP名称"/>
        </a-form-item>
        <a-form-item :label="$t('i18n_1014b33d22')" name="group">
          <custom-select
            v-model:value="temp.groupName"
            :data="groupList"
            :input-placeholder="$t('i18n_bd0362bed3')"
            :select-placeholder="$t('i18n_9cac799f2f')"
          >
            <template #suffix>
              <a-tooltip>
                <template #title>
                  <div>
                    {{ $t('i18n_bd7c7abc8c') }}
                  </div>
                  <div>
                    {{ $t('i18n_f92d505ff5') }}
                  </div>
                </template>
                <QuestionCircleOutlined/>
              </a-tooltip>
            </template>
          </custom-select>
        </a-form-item>
        <a-form-item label="Host" name="host">
          <a-input-group compact name="host">
            <a-input v-model:value="temp.host" style="width: 70%" :placeholder="$t('i18n_3d83a07747')"/>
            <a-form-item-rest>
              <a-input-number
                v-model:value="temp.port"
                style="width: 30%"
                :min="1"
                :placeholder="$t('i18n_39c7644388')"
              />
            </a-form-item-rest>
          </a-input-group>
        </a-form-item>
        <a-form-item label="传输模式" name="mode">
          <a-radio-group v-model:value="temp.mode" :options="options"/>
        </a-form-item>
        <a-form-item name="user">
          <template #label>
            <a-tooltip>
              {{ $t('i18n_819767ada1') }}
              <template #title>
                {{ $t('i18n_f0a1428f65') }}<b>$ref.wEnv.xxxx</b> xxxx {{ $t('i18n_c1b72e7ded') }}
              </template
              >
              <QuestionCircleOutlined v-if="!temp.id"/>
            </a-tooltip>
          </template>
          <a-input v-model:value="temp.user" :placeholder="$t('i18n_1fd02a90c3')">
            <template #suffix>
              <a-tooltip v-if="temp.id" title="密码字段在编辑的时候不会返回，如果需要重置或者清空就请点我">
                <a-button size="small" type="primary" danger @click="handerRestHideField(temp)">{{
                    $t('i18n_4403fca0c0')
                  }}
                </a-button>
              </a-tooltip>
            </template>
          </a-input>
        </a-form-item>

        <!-- 新增时需要填写 -->
        <!--				<a-form-item v-if="temp.type === 'add'" label="Password" name="password">-->
        <!--					<a-input-password v-model="temp.password" placeholder="密码"/>-->
        <!--				</a-form-item>-->
        <!-- 修改时可以不填写 -->
        <a-form-item
          :name="`${temp.type === 'add' ? 'password' : 'password-update'}`"
        >
          <template #label>
            <a-tooltip>
              {{ $t('i18n_a810520460') }}
              <template #title>
                {{ $t('i18n_63dd96a28a') }}<b>$ref.wEnv.xxxx</b> xxxx {{ $t('i18n_c1b72e7ded') }}
              </template
              >
              <QuestionCircleOutlined v-if="!temp.id"/>
            </a-tooltip>
          </template>
          <!-- <a-input-password v-model="temp.password" :placeholder="`${temp.type === 'add' ? '密码' : '密码若没修改可以不用填写'}`" /> -->
          <custom-input
            :input="temp.password"
            :env-list="envVarList"
            :placeholder="`${temp.type === 'add' ? $t('i18n_a810520460') : $t('i18n_6c08692a3a')}`"
            @change="
                  (v) => {
                    temp = { ...temp, password: v }
                  }
                "
          >
          </custom-input>
        </a-form-item>
        <a-form-item label="服务器语言" name="serverLanguageCode">
          <a-input v-model:value="temp.serverLanguageCode" placeholder="服务器语言"/>
        </a-form-item>
        <a-form-item label="系统关键词" name="systemKey">
          <a-input v-model:value="temp.systemKey" placeholder="服务器系统关键词"/>
        </a-form-item>
        <a-form-item :label="$t('i18n_6143a714d0')" name="charset">
          <a-input v-model:value="temp.charset" :placeholder="$t('i18n_6143a714d0')"/>
        </a-form-item>
        <a-form-item :label="$t('i18n_67425c29a5')" name="timeout">
          <a-input-number
            v-model:value="temp.timeout"
            :min="1"
            :placeholder="$t('i18n_cb156269db')"
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item :label="$t('i18n_649231bdee')" name="suffix">
          <template #help>
            {{ "此配置仅对服务端管理生效, 工作空间的 ftp 配置需要单独配置" }}<span style="color: red">{{ "配置方式：FTP管理->操作栏中->关联按钮->对应工作空间->操作栏中->配置按钮" }}</span>
          </template>
          <a-textarea
            v-model:value="temp.allowEditSuffix"
            :rows="5"
            style="resize: none"
            :placeholder="$t('i18n_01081f7817')"
          />
        </a-form-item>
      </a-form>
    </CustomModal>

    <!-- 文件管理 -->
    <CustomDrawer
      v-if="drawerVisible"
      destroy-on-close
      :title="`${temp.name} ${$t('i18n_8780e6b3d1')}`"
      placement="right"
      width="90vw"
      :open="drawerVisible"
      @close="
            () => {
              drawerVisible = false
            }
          "
    >
      <ftp-file v-if="drawerVisible" :machine-ftp-id="temp.id"/>
    </CustomDrawer>

    <!-- 查看 ftp 关联工作空间的信息 -->
    <CustomModal
      v-if="viewWorkspaceFtp"
      v-model:open="viewWorkspaceFtp"
      destroy-on-close
      width="50%"
      title="关联工作空间ftp"
      :footer="null"
      :mask-closable="false"
    >
      <a-space direction="vertical" style="width: 100%">
        <a-alert
          v-if="workspaceFtpList && workspaceFtpList.length"
          message="已经分配到工作空间的 FTP 无法直接删除，需要到分配到的各个工作空间逐一删除后才能删除资产 FTP"
          type="info"
          show-icon
        />
        <a-list bordered :data-source="workspaceFtpList">
          <template #renderItem="{ item }">
            <a-list-item style="display: block">
              <a-row>
                <a-col :span="10">FTP{{ $t('i18n_5b47861521') }}{{ item.name }}</a-col>
                <a-col :span="10">{{ $t('i18n_2358e1ef49') }}{{ item.workspace && item.workspace.name }}</a-col>
                <a-col :span="4">
                  <a-button v-if="item.workspace" size="small" type="primary" @click="configWorkspaceFtp(item)"
                  >{{ $t('i18n_224e2ccda8') }}
                  </a-button>
                  <a-button v-else size="small" type="primary" danger @click="handleDeleteWorkspaceItem(item)"
                  >{{ $t('i18n_2f4aaddde3') }}
                  </a-button>
                </a-col>
              </a-row>
            </a-list-item>
          </template>
        </a-list>
      </a-space>
    </CustomModal>
    <CustomModal
      v-if="configWorkspaceFtpVisible"
      v-model:open="configWorkspaceFtpVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      width="50%"
      title="配置ftp"
      :mask-closable="false"
      @ok="handleConfigWorkspaceFtpOk"
    >
      <a-form
        ref="editConfigWorkspaceFtpForm"
        :rules="rules"
        :model="temp"
        :label-col="{ span: 4 }"
        :wrapper-col="{ span: 18 }"
      >
        <a-form-item label="" :label-col="{ span: 0 }" :wrapper-col="{ span: 24 }">
          <a-alert :message="$t('i18n_ce7e6e0ea9')" banner/>
        </a-form-item>
        <a-form-item label="FTP 名称">
          <a-input
            v-model:value="temp.name"
            :disabled="true"
            :max-length="50"
            placeholder="FTP 名称"
          />
        </a-form-item>
        <a-form-item :label="$t('i18n_6a588459d0')">
          <a-input
            v-model:value="temp.workspaceName"
            :disabled="true"
            :max-length="50"
            :placeholder="$t('i18n_6a588459d0')"
          />
        </a-form-item>

        <a-form-item name="fileDirs">
          <template #label>
            <a-tooltip>
              {{ $t('i18n_7a3c815b1e') }}
              <template #title> {{ $t('i18n_d0874922f0') }}</template>
              <QuestionCircleOutlined/>
            </a-tooltip>
          </template>
          <a-textarea
            v-model:value="temp.fileDirs"
            :auto-size="{ minRows: 3, maxRows: 5 }"
            :placeholder="$t('i18n_baefd3db91')"
          />
        </a-form-item>

        <a-form-item :label="$t('i18n_649231bdee')" name="suffix">
          <a-textarea
            v-model:value="temp.allowEditSuffix"
            :rows="5"
            style="resize: none"
            :placeholder="$t('i18n_01081f7817')"
          />
        </a-form-item>
        <!--            <a-form-item name="notAllowedCommand">
                      <template #label>
                        <a-tooltip>
                          {{ $t('i18n_a39340ec59') }}
                          <template #title>
                            {{ $t('i18n_6bb5ba7438') }}
                            <ul>
                              <li>{{ $t('i18n_7114d41b1d') }}</li>
                              <li>{{ $t('i18n_d8bf90b42b') }}</li>
                            </ul>
                          </template>
                          <QuestionCircleOutlined/>
                        </a-tooltip>
                      </template>
                      <a-textarea
                        v-model:value="temp.notAllowedCommand"
                        :auto-size="{ minRows: 3, maxRows: 5 }"
                        :placeholder="$t('i18n_b6afcf9851')"
                      />
                    </a-form-item>-->
      </a-form>
    </CustomModal>
    <!-- 分配到其他工作空间 -->
    <CustomModal
      v-if="syncToWorkspaceVisible"
      v-model:open="syncToWorkspaceVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      :title="$t('i18n_ef8525efce')"
      :mask-closable="false"
      @ok="handleSyncToWorkspace"
    >
      <a-space direction="vertical" style="width: 100%">
        <a-alert :message="$t('i18n_138a676635')" type="warning" show-icon>
          <template #description>{{ $t('i18n_a63fe7b615') }}</template>
        </a-alert>
        <a-form :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
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
              <a-select-option v-for="item in workspaceList" :key="item.id">{{ item.name }}</a-select-option>
            </a-select>
          </a-form-item>
        </a-form>
      </a-space>
    </CustomModal>

  </div>
</template>
<script>
import {
  machineFtpListData,
  machineFtpListGroup,
  machineFtpEdit,
  machineFtpDelete,
  machineListGroupWorkspaceFtp,
  machineFtpSaveWorkspaceConfig,
  machineFtpDistribute,
  restHideField,
  importTemplate,
  exportData,
  importData,
  statusMap,
} from '@/api/system/assets-ftp'
import {
  COMPUTED_PAGINATION,
  PAGE_DEFAULT_LIST_QUERY,
  parseTime,
  CHANGE_PAGE,
  renderSize,
  formatPercent,
  formatDuration,
  formatPercent2Number
} from '@/utils/const'
import fastInstall from '@/pages/node/fast-install.vue'
import CustomSelect from '@/components/customSelect'
import CustomInput from '@/components/customInput'
import FtpFile from '@/pages/ftp/ftp-file'
import {deleteForeFtp} from '@/api/ftp'
import {getWorkspaceEnvAll, getWorkSpaceListAll} from '@/api/workspace'

export default {
  components: {
    fastInstall,
    CustomSelect,
    FtpFile,
    CustomInput
  },
  data() {
    return {
      loading: true,
      groupList: [],
      list: [],
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      editFtpVisible: false,
      temp: {},
      statusMap,
      // 传输模式: '',
      options: [
        {label: "主动模式", value: 'Active'},
        {label: "被动模式", value: 'Passive'}
      ],

      columns: [
        {
          title: this.$t('i18n_d7ec2d3fea'),
          dataIndex: 'name',
          width: 120,
          sorter: true,
          ellipsis: true
        },

        {
          title: 'Host',
          dataIndex: 'host',
          width: 120,
          sorter: true,
          ellipsis: true
        },
        {
          title: this.$t('i18n_819767ada1'),
          dataIndex: 'user',
          sorter: true,
          width: '80px',
          ellipsis: true,
          tooltip: true
        },
        {
          title: "服务器语言",
          dataIndex: 'serverLanguageCode',
          width: 120,
          sorter: true,
          ellipsis: true
        },
        {
          title: '服务器系统关键词',
          dataIndex: 'systemKey',
          sorter: true,
          width: '100px',
          ellipsis: true
        },
        {
          title: "编码格式",
          dataIndex: "charset",
          sorter: true,
          width: 120,
          ellipsis: true,
        },
        {
          title: this.$t('i18n_7912615699'),
          dataIndex: 'status',
          ellipsis: true,
          align: 'center',
          width: '100px'
        },
        {
          title: this.$t('i18n_eca37cb072'),
          dataIndex: 'createTimeMillis',
          ellipsis: true,
          sorter: true,
          customRender: ({text}) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$t('i18n_1303e638b5'),
          dataIndex: 'modifyTimeMillis',
          sorter: true,
          ellipsis: true,
          customRender: ({text}) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$t('i18n_2b6bc0f293'),
          dataIndex: 'operation',

          width: '310px',
          align: 'center',
          // ellipsis: true,
          fixed: 'right'
        }
      ],

      // 表单校验规则
      rules: {
        name: [{required: true, message: this.$t('i18n_06e2f88f42'), trigger: 'blur'}],
        host: [{required: true, message: this.$t('i18n_81485b76d8'), trigger: 'blur'}],
        port: [{required: true, message: this.$t('i18n_8d0fa2ee2d'), trigger: 'blur'}],
        connectType: [
          {
            required: true,
            message: this.$t('i18n_4ed1662cae'),
            trigger: 'blur'
          }
        ],

        mode: [{required: true, message: this.$t('i18n_3103effdfd'), trigger: 'blur'}],
        user: [{required: true, message: this.$t('i18n_3103effdfd'), trigger: 'blur'}],
        password: [{required: true, message: this.$t('i18n_209f2b8e91'), trigger: 'blur'}]
      },
      nodeVisible: false,

      terminalVisible: false,
      terminalFullscreen: false,
      viewOperationLog: false,
      drawerVisible: false,
      workspaceFtpList: [],
      viewWorkspaceFtp: false,
      configWorkspaceFtpVisible: false,
      syncToWorkspaceVisible: false,
      workspaceList: [],
      tableSelections: [],
      envVarList: [],
      confirmLoading: false
    }
  },
  computed: {
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
    },
    activePage() {
      return this.$attrs.routerUrl === this.$route.path
    }
  },
  created() {
    this.loadData()
    this.loadGroupList()
    this.getWorkEnvList()
  },
  methods: {
    formatDuration,
    renderSize,
    formatPercent,
    formatPercent2Number,
    getWorkEnvList() {
      getWorkspaceEnvAll({
        workspaceId: 'GLOBAL'
      }).then((res) => {
        if (res.code === 200) {
          this.envVarList = res.data
        }
      })
    },
    // 加载数据
    loadData(pointerEvent) {
      this.loading = true
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
      machineFtpListData(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result
          this.listQuery.total = res.data.total
          //
        }
        this.loading = false
      })
    },
    // 获取所有的分组
    loadGroupList() {
      machineFtpListGroup().then((res) => {
        if (res.data) {
          this.groupList = res.data
        }
      })
    },
    // 新增 FTP
    handleAdd() {
      this.temp = {
        charset: 'UTF-8',
        port: 21,
        timeout: 5,
        mode: 'Active'
      }
      this.editFtpVisible = true
      // @author jzy 08-04
      this.$refs['editFtpForm'] && this.$refs['editFtpForm'].resetFields()
    },
    // 修改
    handleEdit(record) {
      this.temp = Object.assign({}, record, {
        allowEditSuffix: record.allowEditSuffix ? JSON.parse(record.allowEditSuffix).join('\r\n') : ''
      })

      this.temp = {
        ...this.temp,

        timeout: record.timeout || 5
      }
      this.editFtpVisible = true
      // @author jzy 08-04
      this.$refs['editFtpForm'] && this.$refs['editFtpForm'].resetFields()
      this.loadGroupList()
    },
    // 提交 FTP 数据
    handleEditFtpOk() {
      // 检验表单
      this.$refs['editFtpForm'].validate().then(() => {
        // 提交数据
        this.confirmLoading = true
        machineFtpEdit(this.temp)
          .then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              this.editFtpVisible = false
              this.loadData()
              this.loadGroupList()
            }
          })
          .finally(() => {
            this.confirmLoading = false
          })
      })
    },
    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery = CHANGE_PAGE(this.listQuery, {pagination, sorter})
      this.loadData()
    },
    // 安装节点
    install() {
      this.nodeVisible = true
    },

    // 删除
    handleDelete(record) {
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        content: this.$t('i18n_0aa639865c'),
        zIndex: 1009,
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return machineFtpDelete({
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

    // 查看工作空间的 ftp
    handleViewWorkspaceFtp(item) {
      machineListGroupWorkspaceFtp({
        id: item.id
      }).then((res) => {
        if (res.code === 200) {
          this.temp = {
            machineFtpId: item.id
          }
          this.viewWorkspaceFtp = true
          this.workspaceFtpList = res.data
        }
      })
    },
    // 配置 ftp
    configWorkspaceFtp(item) {
      this.temp = {
        ...this.temp,
        id: item.id,
        name: item.name,
        fileDirs: item.fileDirs ? JSON.parse(item.fileDirs).join('\r\n') : '',
        allowEditSuffix: item.allowEditSuffix ? JSON.parse(item.allowEditSuffix).join('\r\n') : '',
        workspaceName: item.workspace?.name,
        notAllowedCommand: item.notAllowedCommand
      }
      this.configWorkspaceFtpVisible = true
    },
    // 提交 FTP 配置 数据
    handleConfigWorkspaceFtpOk() {
      // 检验表单
      this.$refs['editConfigWorkspaceFtpForm'].validate().then(() => {
        this.confirmLoading = true
        // 提交数据
        machineFtpSaveWorkspaceConfig(this.temp)
          .then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              this.configWorkspaceFtpVisible = false
              machineListGroupWorkspaceFtp({
                id: this.temp.machineFtpId
              }).then((res) => {
                if (res.code === 200) {
                  this.workspaceFtpList = res.data
                }
              })
            }
          })
          .finally(() => {
            this.confirmLoading = false
          })
      })
    },
    // 删除工作空间的数据
    handleDeleteWorkspaceItem(record) {
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_2ff65378a4'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: async () => {
          const {code, msg} = await deleteForeFtp(record.id)
          if (code === 200) {
            $notification.success({
              message: msg
            })
            const res = await machineListGroupWorkspaceFtp({
              id: this.temp.machineFtpId
            })
            if (res.code === 200) {
              this.workspaceFtpList = res.data
            }
          }
        }
      })
    },
    // 文件管理
    handleFile(record) {
      this.temp = Object.assign({}, record)

      this.drawerVisible = true
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
    syncToWorkspaceShow(item) {
      this.syncToWorkspaceVisible = true
      this.loadWorkSpaceListAll()
      if (item) {
        this.temp = {
          ids: item.id
        }
      }
    },
    handleSyncToWorkspace() {
      if (!this.temp.workspaceId) {
        $notification.warn({
          message: this.$t('i18n_b3bda9bf9e')
        })
        return false
      }
      if (!this.temp.ids) {
        this.temp = {...this.temp, ids: this.tableSelections.join(',')}
        this.tableSelections = []
      }
      this.confirmLoading = true
      // 同步
      machineFtpDistribute(this.temp)
        .then((res) => {
          if (res.code == 200) {
            $notification.success({
              message: res.msg
            })

            this.syncToWorkspaceVisible = false
            return false
          }
        })
        .finally(() => {
          this.confirmLoading = false
        })
    },
    // 清除隐藏字段
    handerRestHideField(record) {
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: "真的要清除 FTP 隐藏字段信息么？（密码）",
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return restHideField(record.id).then((res) => {
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
    // 下载导入模板
    handlerImportTemplate() {
      window.open(importTemplate(), '_blank')
    },
    handlerExportData() {
      window.open(exportData({...this.listQuery}), '_blank')
    },
    beforeUpload(file) {
      const formData = new FormData()
      formData.append('file', file)
      importData(formData).then((res) => {
        if (res.code === 200) {
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
