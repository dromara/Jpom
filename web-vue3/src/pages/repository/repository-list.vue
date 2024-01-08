<template>
  <div>
    <!-- 表格 -->
    <a-table
      size="middle"
      :columns="columns"
      :data-source="list"
      bordered
      rowKey="id"
      :row-selection="this.choose ? rowSelection : null"
      :pagination="pagination"
      :scroll="{
        x: 'max-content'
      }"
      @change="
        (pagination, filters, sorter) => {
          this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter })
          this.loadData()
        }
      "
    >
      <template v-slot:title>
        <a-space>
          <a-input
            class="search-input-item"
            @pressEnter="loadData"
            v-model:value="listQuery['%name%']"
            placeholder="仓库名称"
          />
          <a-input
            class="search-input-item"
            @pressEnter="loadData"
            v-model:value="listQuery['%gitUrl%']"
            placeholder="仓库地址"
          />
          <a-select v-model:value="listQuery.repoType" allowClear placeholder="仓库类型" class="search-input-item">
            <a-select-option :value="'0'">GIT</a-select-option>
            <a-select-option :value="'1'">SVN</a-select-option>
          </a-select>
          <a-select
            show-search
            option-filter-prop="children"
            v-model:value="listQuery.group"
            allowClear
            placeholder="分组"
            class="search-input-item"
          >
            <a-select-option v-for="item in groupList" :key="item">{{ item }}</a-select-option>
          </a-select>

          <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
            <a-button type="primary" :loading="loading" @click="loadData">搜索</a-button>
          </a-tooltip>
          <a-button type="primary" @click="handleAdd">新增</a-button>
          <a-tooltip>
            <template v-slot:title
              >使用 Access Token 一次导入多个项目<br />点击<a
                target="_blank"
                href="https://jpom.top/pages/jpom-server-import-multi-repos/"
                >文档链接</a
              >查看详情</template
            >
            <a-button type="primary" @click="handleAddGitee"><QuestionCircleOutlined />令牌导入</a-button>
          </a-tooltip>
          <a-button type="primary" @click="handlerExportData">导出</a-button>
          <a-dropdown>
            <template v-slot:overlay>
              <a-menu>
                <a-menu-item key="1">
                  <a-button type="primary" @click="handlerImportTemplate()">下载导入模板</a-button>
                </a-menu-item>
              </a-menu>
            </template>

            <a-upload
              name="file"
              accept=".csv"
              action=""
              :showUploadList="false"
              :multiple="false"
              :before-upload="beforeUpload"
            >
              <a-button type="primary"><UploadOutlined /> 导入 <DownOutlined /> </a-button>
            </a-upload>
          </a-dropdown>
        </a-space>
      </template>
      <template #bodyCell="{ column, text, record, index }">
        <template v-if="column.tooltip">
          <a-tooltip placement="topLeft" :title="text">
            <span>{{ text }}</span>
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'repoType'">
          <span v-if="text === 0">GIT</span>
          <span v-else-if="text === 1">SVN</span>
          <span v-else>未知</span>
        </template>
        <template v-else-if="column.dataIndex === 'protocol'">
          <span v-if="text === 0">HTTP(S)</span>
          <span v-else-if="text === 1">SSH</span>
          <!-- if no protocol value, get a default value from gitUrl -->
          <span v-else>{{ record.gitUrl.indexOf('http') > -1 ? 'HTTP(S)' : 'SSH' }}</span>
        </template>
        <template v-else-if="column.dataIndex === 'workspaceId'">
          <a-tag v-if="text === 'GLOBAL'">全局</a-tag>
          <a-tag v-else>工作空间</a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button type="primary" size="small" @click="handleEdit(record)">编辑</a-button>
            <a-button type="primary" v-if="global" size="small" @click="viewBuild(record)">关联</a-button>
            <a-button type="primary" danger size="small" @click="handleDelete(record)">删除</a-button>

            <a-dropdown>
              <a @click="(e) => e.preventDefault()">
                更多
                <DownOutlined />
              </a>
              <template v-slot:overlay>
                <a-menu>
                  <a-menu-item>
                    <a-button
                      size="small"
                      type="primary"
                      :disabled="(listQuery.page - 1) * listQuery.limit + (index + 1) <= 1"
                      @click="sortItemHander(record, index, 'top')"
                      >置顶</a-button
                    >
                  </a-menu-item>
                  <a-menu-item>
                    <a-button
                      size="small"
                      type="primary"
                      :disabled="(listQuery.page - 1) * listQuery.limit + (index + 1) <= 1"
                      @click="sortItemHander(record, index, 'up')"
                      >上移</a-button
                    >
                  </a-menu-item>
                  <a-menu-item>
                    <a-button
                      size="small"
                      type="primary"
                      :disabled="(listQuery.page - 1) * listQuery.limit + (index + 1) === listQuery.total"
                      @click="sortItemHander(record, index, 'down')"
                    >
                      下移
                    </a-button>
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
        </template>
      </template>
    </a-table>
    <!-- 编辑区 -->
    <a-modal
      :zIndex="1009"
      destroyOnClose
      :confirmLoading="confirmLoading"
      v-model:open="editVisible"
      title="编辑仓库"
      @ok="handleEditOk"
      :maskClosable="false"
    >
      <a-form ref="editForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
        <a-form-item label="仓库名称" name="name">
          <a-input v-model:value="temp.name" :maxLength="50" placeholder="仓库名称" />
        </a-form-item>
        <a-form-item label="分组" name="group">
          <custom-select
            v-model:value="temp.group"
            :data="groupList"
            suffixIcon=""
            inputPlaceholder="添加分组"
            selectPlaceholder="选择分组名"
          >
          </custom-select>
        </a-form-item>
        <a-form-item label="仓库地址" name="gitUrl">
          <a-input-group compact>
            <a-form-item-rest>
              <a-select style="width: 20%" v-model:value="temp.repoType" name="repoType" placeholder="仓库类型">
                <a-select-option :value="0">GIT</a-select-option>
                <a-select-option :value="1">SVN</a-select-option>
              </a-select>
            </a-form-item-rest>
            <a-input style="width: 80%" :maxLength="250" v-model:value="temp.gitUrl" placeholder="仓库地址" />
          </a-input-group>
        </a-form-item>
        <a-form-item label="协议" name="protocol">
          <a-radio-group v-model:value="temp.protocol" name="protocol">
            <a-radio :value="0">HTTP(S)</a-radio>
            <a-radio :value="1">SSH</a-radio>
          </a-radio-group>
        </a-form-item>
        <!-- HTTP(S) protocol use password -->
        <template v-if="temp.protocol === 0">
          <a-form-item name="userName">
            <template #label>
              <a-tooltip>
                账号
                <template v-slot:title> 账号支持引用工作空间变量：<b>$ref.wEnv.xxxx</b> xxxx 为变量名称</template>
                <QuestionCircleOutlined v-if="!temp.id" />
              </a-tooltip>
            </template>

            <custom-input
              :input="temp.userName"
              :envList="envVarList"
              type="text"
              :placeholder="`登录用户`"
              @change="
                (v) => {
                  temp = { ...temp, userName: v }
                }
              "
            >
            </custom-input>
          </a-form-item>
          <a-form-item name="password">
            <template #label>
              <a-tooltip>
                密码
                <template v-slot:title> 密码支持引用工作空间变量：<b>$ref.wEnv.xxxx</b> xxxx 为变量名称</template>
                <QuestionCircleOutlined v-if="!temp.id" />
              </a-tooltip>
            </template>

            <custom-input
              :input="temp.password"
              :envList="envVarList"
              :placeholder="`${!temp.id ? '登录密码' : '此处不填不会修改密码'}`"
              @change="
                (v) => {
                  temp = { ...temp, password: v }
                }
              "
            >
            </custom-input>
            <template #help>
              <a-tooltip v-if="temp.id" title=" 密码字段和密钥字段在编辑的时候不会返回，如果需要重置或者清空就请点我">
                <a-button style="margin: 5px" size="small" type="primary" danger @click="restHideField(temp)"
                  >清除</a-button
                >
              </a-tooltip>
            </template>
          </a-form-item>
        </template>
        <a-form-item v-if="temp.repoType === 1 && temp.protocol === 1" label="账号" name="userName">
          <a-input v-model:value="temp.userName" placeholder="svn ssh 必填登录用户">
            <template v-slot:prefix>
              <UserOutlined />
            </template>
            <template v-slot:suffix>
              <a-tooltip v-if="temp.id" title=" 密码字段和密钥字段在编辑的时候不会返回，如果需要重置或者清空就请点我">
                <a-button size="small" type="primary" danger @click="restHideField(temp)">清除</a-button>
              </a-tooltip>
            </template>
          </a-input>
        </a-form-item>
        <!-- SSH protocol use rsa private key -->
        <template v-if="temp.protocol === 1">
          <a-form-item name="password">
            <template #label>
              <a-tooltip>
                密码
                <template v-slot:title> 密码支持引用工作空间变量：<b>$ref.wEnv.xxxx</b> xxxx 为变量名称</template>
                <QuestionCircleOutlined v-if="!temp.id" />
              </a-tooltip>
            </template>
            <custom-input
              :input="temp.password"
              :envList="envVarList"
              :placeholder="`证书密码`"
              @change="
                (v) => {
                  temp = { ...temp, password: v }
                }
              "
            >
            </custom-input>
          </a-form-item>
          <a-form-item label="私钥" name="rsaPrv">
            <a-tooltip placement="topLeft">
              <template v-slot:title>
                <div>
                  <p style="color: #faa">
                    注意：目前对 SSH key 访问 git 仓库地址不支持使用 ssh-keygen -t rsa -C "邮箱" 方式生成的 SSH key
                    <br />需要使用 ssh-keygen -m PEM -t rsa -b 4096 -C "邮箱" 方式生成公私钥<br />
                  </p>
                  <p>如果在生成私钥的过程中有加密，那么需要把加密密码填充到上面的密码框中</p>
                  <p>支持两种方式填充：</p>
                  <p>
                    1. 完整的私钥内容 如: <br />-----BEGIN RSA PRIVATE KEY-----
                    <br />
                    ..... <br />
                    -----END RSA PRIVATE KEY-----
                  </p>
                  <p>
                    2. 私钥文件绝对路径（绝对路径前面添加 file: 前缀) 如:
                    <br />file:/Users/Hotstrip/.ssh/id_rsa
                  </p>
                </div>
              </template>
              <a-textarea
                :auto-size="{ minRows: 3, maxRows: 3 }"
                v-model:value="temp.rsaPrv"
                placeholder="私钥,不填将使用默认的 $HOME/.ssh 目录中的配置。支持配置文件目录:file:"
              ></a-textarea>
            </a-tooltip>
          </a-form-item>
          <!-- 公钥暂时没用到 -->
          <a-form-item label="公钥" name="rsaPub" v-if="false">
            <a-textarea
              :auto-size="{ minRows: 3, maxRows: 3 }"
              v-model:value="temp.rsaPub"
              placeholder="公钥,不填将使用默认的 $HOME/.ssh 目录中的配置。支持配置文件目录:file:"
            ></a-textarea>
          </a-form-item>
        </template>
        <a-form-item label="共享" name="global" v-if="this.workspaceId !== 'GLOBAL'">
          <a-radio-group v-model:value="temp.global">
            <a-radio :value="true"> 全局</a-radio>
            <a-radio :value="false"> 当前工作空间</a-radio>
          </a-radio-group>
        </a-form-item>
        <!-- <a-form-item v-if="temp.id" name="restHideField">
            <template slot="label">
              隐藏字段
              <a-tooltip>
                <template slot="title"> 密码字段和私钥字段在编辑的时候不会返回，如果需要重置或者清空就请点我 </template>
                <QuestionCircleOutlined />
              </a-tooltip>
            </template>
            <a-button style="margin-left: 10px" type="primary" danger @click="restHideField(temp)">清除</a-button>
          </a-form-item> -->
        <a-form-item label="超时时间(s)" name="timeout">
          <a-input-number
            v-model:value="temp.timeout"
            :min="0"
            placeholder="拉取仓库超时时间,单位秒"
            style="width: 100%"
          />
        </a-form-item>
      </a-form>
    </a-modal>
    <a-modal
      :zIndex="1009"
      destroyOnClose
      v-model:open="giteeImportVisible"
      title="通过私人令牌导入仓库"
      width="80%"
      :footer="null"
      :maskClosable="false"
    >
      <a-form
        :label-col="{ span: 4 }"
        :rules="giteeImportFormRules"
        :model="giteeImportForm"
        ref="giteeImportForm"
        :wrapper-col="{ span: 20 }"
      >
        <a-form-item
          name="token"
          label="私人令牌"
          help="使用私人令牌，可以在你不输入账号密码的情况下对你账号内的仓库进行管理，你可以在创建令牌时指定令牌所拥有的权限。"
        >
          <a-form-item-rest>
            <a-tooltip :title="`${giteeImportForm.type} 的令牌${importTypePlaceholder[giteeImportForm.type]}`">
              <!-- <a-input v-model="giteeImportForm.token" :placeholder="importTypePlaceholder[giteeImportForm.type]">
                <a-select slot="addonBefore" style="width: 100px" @change="importChange" v-model="giteeImportForm.type">
                  <a-select-option :value="item" v-for="item in Object.keys(providerData)" :key="item"> {{ item }}</a-select-option>
                </a-select>

                <a-button slot="addonAfter" size="small" type="primary" icon="search" @click="handleGiteeImportFormOk"></a-button>
              </a-input> -->
              <a-input-group compact>
                <a-select style="width: 10%" @change="importChange" v-model:value="giteeImportForm.type">
                  <a-select-option :value="item" v-for="item in Object.keys(providerData)" :key="item">
                    {{ item }}</a-select-option
                  >
                </a-select>

                <a-input-search
                  style="width: 90%; margin-top: 1px"
                  enter-button
                  :loading="importLoading"
                  v-model:value="giteeImportForm.token"
                  @search="handleGiteeImportFormOk"
                  :placeholder="importTypePlaceholder[giteeImportForm.type]"
                />
              </a-input-group>
            </a-tooltip>
          </a-form-item-rest>
        </a-form-item>
        <a-form-item name="address" label="地址">
          <a-input v-model:value="giteeImportForm.address" placeholder="请填写平台地址" />
        </a-form-item>
        <a-form-item
          name="condition"
          label="搜索"
          help="输入仓库名称或者仓库路径进行搜索"
          v-if="providerData[giteeImportForm.type].query"
        >
          <a-input v-model:value="giteeImportForm.condition" placeholder="输入仓库名称或者仓库路径进行搜索" />
        </a-form-item>
      </a-form>
      <a-table
        :loading="importLoading"
        size="middle"
        :columns="reposColumns"
        :data-source="repos"
        bordered
        rowKey="full_name"
        @change="reposChange"
        :pagination="reposPagination"
      >
        <template #bodyCell="{ column, text, record }">
          <template v-if="column.dataIndex === 'private'">
            <a-switch size="small" :disabled="true" :checked="record.private" />
          </template>
          <template v-else-if="column.dataIndex === 'name'">
            <a-tooltip placement="topLeft" :title="text">
              <span>{{ text }}</span>
            </a-tooltip>
          </template>
          <template v-else-if="column.dataIndex === 'full_name'">
            <a-tooltip placement="topLeft" :title="text">
              <span>{{ text }}</span>
            </a-tooltip>
          </template>
          <template v-else-if="column.dataIndex === 'url'">
            <a-tooltip placement="topLeft" :title="text">
              <span>{{ text }}</span>
            </a-tooltip>
          </template>
          <template v-else-if="column.dataIndex === 'description'">
            <a-tooltip placement="topLeft" :title="text">
              <span>{{ text }}</span>
            </a-tooltip>
          </template>

          <template v-else-if="column.dataIndex === 'operation'">
            <a-button type="primary" size="small" :disabled="record.exists" @click="handleGiteeRepoAdd(record)">{{
              record.exists ? '已存在' : '添加'
            }}</a-button>
          </template>
        </template>
      </a-table>
    </a-modal>
    <!-- 选择仓库确认区域 -->
    <!-- <div style="padding-top: 50px" v-if="this.choose">
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
      </div> -->
    <!-- </div> -->
    <!-- 关联构建 -->
    <a-modal
      destroyOnClose
      v-model:open="viewBuildVisible"
      width="80vw"
      title="当前工作空间关联构建"
      :maskClosable="false"
      :footer="null"
    >
      <buildList-component v-if="viewBuildVisible" :repositoryId="temp.id" :fullContent="false" />
      <a-spin v-else>loading....</a-spin>
    </a-modal>
  </div>
</template>

<script>
import CustomInput from '@/components/customInput'
import {
  providerInfo,
  authorizeRepos,
  deleteRepository,
  editRepository,
  getRepositoryList,
  restHideField,
  sortItem,
  exportData,
  importTemplate,
  importData,
  listRepositoryGroup
} from '@/api/repository'
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'
import { getWorkspaceEnvAll } from '@/api/workspace'
import CustomSelect from '@/components/customSelect'
export default {
  components: {
    CustomInput,
    CustomSelect,
    buildListComponent: defineAsyncComponent(() => import('@/pages/build/list-info'))
  },
  props: {
    choose: {
      type: Boolean,
      default: false
    },
    workspaceId: {
      type: String,
      default: ''
    },
    global: {
      type: Boolean,
      default: false
    },
    chooseVal: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      loading: false,
      PAGE_DEFAULT_SIZW_OPTIONS: ['15', '20', '25', '30', '35', '40', '50'],
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      list: [],
      groupList: [],
      providerData: {
        gitee: {
          baseUrl: 'https://gitee.com',
          name: 'gitee',
          query: true
        }
      },
      total: 0,
      temp: {},
      isSystem: false,
      editVisible: false,
      giteeImportVisible: false,
      repos: [],
      username: null,

      columns: [
        {
          title: '仓库名称',
          dataIndex: 'name',
          width: 200,
          sorter: true,
          ellipsis: true,
          tooltip: true
        },
        {
          title: '分组名',
          dataIndex: 'group',
          ellipsis: true,
          width: '100px',
          tooltip: true
        },
        {
          title: '仓库地址',
          dataIndex: 'gitUrl',
          width: 300,
          sorter: true,
          ellipsis: true,
          tooltip: true
        },
        {
          title: '仓库类型',
          dataIndex: 'repoType',
          width: 100,
          sorter: true,
          ellipsis: true
        },
        {
          title: '协议',
          dataIndex: 'protocol',
          width: 100,
          sorter: true,
          ellipsis: true
        },
        {
          title: '共享',
          dataIndex: 'workspaceId',
          ellipsis: true,
          // scopedSlots: { customRender: 'global' },
          width: '90px'
        },
        {
          title: '创建人',
          dataIndex: 'createUser',
          ellipsis: true,
          tooltip: true,
          width: '120px'
        },
        {
          title: '修改人',
          dataIndex: 'modifyUser',
          ellipsis: true,
          tooltip: true,
          width: '120px'
        },
        {
          title: '创建时间',
          dataIndex: 'createTimeMillis',
          sorter: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: '修改时间',
          dataIndex: 'modifyTimeMillis',
          sorter: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: '操作',
          dataIndex: 'operation',
          fixed: 'right',
          align: 'center',
          width: this.global ? '240px' : '180px'
        }
      ],
      reposColumns: [
        {
          title: '仓库名称',
          dataIndex: 'name',
          ellipsis: true
        },
        {
          title: '仓库路径',
          dataIndex: 'full_name',
          ellipsis: true
        },
        {
          title: 'GitUrl',
          dataIndex: 'url',
          ellipsis: true
        },

        {
          title: '描述',
          dataIndex: 'description',

          ellipsis: true
        },
        {
          title: '私有',
          dataIndex: 'private',
          width: 80,
          ellipsis: true
        },
        {
          title: '操作',
          dataIndex: 'operation',
          width: 100,

          align: 'left'
        }
      ],
      giteeImportForm: Object.assign({}, PAGE_DEFAULT_LIST_QUERY, {
        limit: 15,
        type: 'gitee',
        address: 'https://gitee.com'
      }),
      giteeImportFormRules: {
        token: [{ required: true, message: '请输入私人令牌', trigger: 'blur' }]
        // address: [{ required: true, message: "请填写平台地址", trigger: "blur" }],
      },
      rules: {
        name: [{ required: true, message: '请填写仓库名称', trigger: 'blur' }],
        gitUrl: [{ required: true, message: '请填写仓库地址', trigger: 'blur' }]
      },
      importTypePlaceholder: {
        gitee: '在 设置-->安全设置-->私人令牌 中获取',
        github: '在 Settings-->Developer settings-->Personal access tokens 中获取',
        gitlab_v3: '在 preferences-->Access Tokens 中获取',
        gitlab: '在 preferences-->Access Tokens 中获取',
        gitea: '在 设置 --> 应用 --> 生成令牌',
        gogs: '在 设置 --> 应用 --> 生成令牌',
        other: '请输入私人令牌'
      },
      tableSelections: [],
      envVarList: [],

      viewBuildVisible: false,
      confirmLoading: false,
      importLoading: false
    }
  },
  computed: {
    // 分页
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery)
    },
    reposPagination() {
      return COMPUTED_PAGINATION(this.giteeImportForm, this.PAGE_DEFAULT_SIZW_OPTIONS)
    },
    rowSelection() {
      return {
        onChange: (selectedRowKeys) => {
          this.tableSelections = selectedRowKeys
        },
        selectedRowKeys: this.tableSelections,
        type: 'radio'
      }
    }
  },
  watch: {},
  created() {
    this.loadData()
    //
    providerInfo().then((response) => {
      if (response.code === 200) {
        this.providerData = response.data
      }
    })
    this.getWorkEnvList()
    this.loadGroupList()

    if (this.chooseVal) {
      this.tableSelections = [this.chooseVal]
    }
  },
  methods: {
    CHANGE_PAGE,
    // 分组数据
    loadGroupList() {
      listRepositoryGroup().then((res) => {
        if (res.data) {
          this.groupList = res.data
        }
      })
    },
    getWorkEnvList() {
      getWorkspaceEnvAll({
        workspaceId: this.workspaceId + (this.global ? ',GLOBAL' : '')
      }).then((res) => {
        if (res.code === 200) {
          this.envVarList = res.data
        }
      })
    },
    // 加载数据
    loadData(pointerEvent) {
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
      this.loading = true
      if (this.workspaceId) {
        this.listQuery = { ...this.listQuery, workspaceId: this.workspaceId }
      }
      getRepositoryList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result
          this.listQuery.total = res.data.total
        }
        this.loading = false
      })
    },
    importChange(value) {
      this.giteeImportForm.address = this.providerData[value].baseUrl
    },
    // // 筛选
    // handleFilter() {
    //   this.loadData();
    // },
    // 添加
    handleAdd() {
      this.temp = {
        repoType: 0,
        protocol: 0
      }
      if (!this.global) {
        this.temp = { ...this.temp, workspaceId: 'GLOBAL', global: true }
      }

      this.editVisible = true
    },
    handleAddGitee() {
      this.giteeImportVisible = true
    },
    // 下载导入模板
    handlerImportTemplate() {
      window.open(importTemplate(), '_blank')
    },
    handlerExportData() {
      window.open(exportData({ ...this.listQuery, workspaceId: this.workspaceId }), '_blank')
    },
    beforeUpload(file) {
      const formData = new FormData()
      formData.append('file', file)
      formData.append('workspaceId', this.workspaceId)
      importData(formData).then((res) => {
        if (res.code === 200) {
          this.$notification.success({
            message: res.msg
          })
          this.loadData()
        }
      })
    },
    handleGiteeImportFormOk() {
      this.$refs['giteeImportForm'].validate().then(() => {
        this.importLoading = true
        authorizeRepos(this.giteeImportForm)
          .then((res) => {
            if (res.code === 200) {
              // 成功
              //this.username = res.data.username;
              this.giteeImportForm.total = res.data.total
              this.repos = res.data.result
            }
          })
          .finally(() => {
            this.importLoading = false
          })
      })
    },
    reposChange(pagination) {
      this.giteeImportForm.page = pagination.current
      this.giteeImportForm.limit = pagination.pageSize
      this.handleGiteeImportFormOk()
    },
    handleGiteeRepoAdd(record) {
      let data = {
        repoType: 0,
        protocol: 0,
        userName: record.username,
        password: this.giteeImportForm.token,
        name: record.name,
        gitUrl: record.url
      }
      if (!this.global) {
        data = { ...data, workspaceId: 'GLOBAL', global: true }
      }
      editRepository(data).then((res) => {
        if (res.code === 200) {
          // 成功
          this.$notification.success({
            message: res.msg
          })
          record.exists = true
          this.loadData()
        }
      })
    },
    // 修改
    handleEdit(record) {
      this.temp = Object.assign({}, record)
      if (this.temp.protocol === undefined) {
        this.temp.protocol = this.temp.gitUrl.indexOf('http') > -1 ? 0 : 1
      }
      this.temp = {
        ...this.temp,
        global: record.workspaceId === 'GLOBAL',
        workspaceId: ''
      }
      this.editVisible = true
    },
    // 提交节点数据
    handleEditOk() {
      // 检验表单
      this.$refs['editForm'].validate().then(() => {
        // 提交数据
        this.confirmLoading = true
        editRepository(this.temp)
          .then((res) => {
            if (res.code === 200) {
              // 成功
              this.$notification.success({
                message: res.msg
              })
              this.editVisible = false
              this.loadData()
              this.$refs['editForm'].resetFields()
              this.loadGroupList()
            }
          })
          .finally(() => {
            this.confirmLoading = false
          })
      })
    },
    // 删除
    handleDelete(record) {
      this.$confirm({
        title: '系统提示',
        content: '真的要删除仓库信息么？',
        okText: '确认',
        cancelText: '取消',
        zIndex: 1009,
        async onOk() {
          return await new Promise((resolve, reject) => {
            const params = {
              id: record.id
              //isRealDel: this.isSystem,
            }
            // 删除
            deleteRepository(params)
              .then((res) => {
                if (res.code === 200) {
                  this.$notification.success({
                    message: res.msg
                  })
                  this.loadData()
                  resolve()
                } else {
                  reject()
                }
              })
              .catch(() => {
                reject()
              })
          })
        }
      })
    },

    // 清除隐藏字段
    restHideField(record) {
      this.$confirm({
        title: '系统提示',
        content: '真的要清除仓库隐藏字段信息么？（密码，私钥）',
        okText: '确认',
        cancelText: '取消',
        zIndex: 1009,
        async onOk() {
          return await new Promise((resolve, reject) => {
            // 恢复
            restHideField(record.id)
              .then((res) => {
                if (res.code === 200) {
                  this.$notification.success({
                    message: res.msg
                  })
                  this.loadData()
                }
                resolve()
              })
              .catch(reject)
          })
        }
      })
    },

    // 排序
    sortItemHander(record, index, method) {
      const msgData = {
        top: '确定要将此数据置顶吗？',
        up: '确定要将此数上移吗？',
        down: '确定要将此数据下移吗？下移操作可能因为列表后续数据没有排序值操作无效！'
      }
      let msg = msgData[method] || '确定要操作吗？'
      if (!record.sortValue) {
        msg += ' 当前数据为默认状态,操后上移或者下移可能不会达到预期排序,还需要对相关数据都操作后才能达到预期排序'
      }
      // console.log(this.list, index, this.list[method === "top" ? index : method === "up" ? index - 1 : index + 1]);
      const compareId = this.list[method === 'top' ? index : method === 'up' ? index - 1 : index + 1].id
      this.$confirm({
        title: '系统提示',
        content: msg,
        okText: '确认',
        cancelText: '取消',
        zIndex: 1009,
        async onOk() {
          return await new Promise((resolve, reject) => {
            // 解锁
            sortItem({
              id: record.id,
              method: method,
              compareId: compareId
            })
              .then((res) => {
                if (res.code == 200) {
                  this.$notification.success({
                    message: res.msg
                  })

                  this.loadData()
                  return false
                }
                resolve()
              })
              .catch(reject)
          })
        }
      })
    },
    // 确认
    handerConfirm() {
      if (!this.tableSelections.length) {
        this.$notification.warning({
          message: '请选择要使用的仓库'
        })
        return
      }
      const selectData = this.list.filter((item) => {
        return item.id === this.tableSelections[0]
      })[0]

      this.$emit('confirm', `${selectData.id}`)
    },
    // 查看关联构建
    viewBuild(data) {
      this.temp = { id: data.id }
      this.viewBuildVisible = true
    }
  },
  emits: ['cancel', 'confirm']
}
</script>

<style scoped>
/* .filter {
  margin-bottom: 10px;
}

.btn-add {
  margin-left: 10px;
  margin-right: 0;
} */
</style>
