<template>
  <div class="full-content">
    <!-- 表格 -->
    <a-table
      size="middle"
      :columns="columns"
      :data-source="list"
      bordered
      rowKey="id"
      :row-selection="this.choose ? rowSelection : null"
      :pagination="pagination"
      @change="
        (pagination, filters, sorter) => {
          this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter });
          this.loadData();
        }
      "
    >
      <template slot="title">
        <a-space>
          <a-input class="search-input-item" @pressEnter="loadData" v-model="listQuery['%name%']" placeholder="仓库名称" />
          <a-input class="search-input-item" @pressEnter="loadData" v-model="listQuery['%gitUrl%']" placeholder="仓库地址" />
          <a-select v-model="listQuery.repoType" allowClear placeholder="仓库类型" class="search-input-item">
            <a-select-option :value="'0'">GIT</a-select-option>
            <a-select-option :value="'1'">SVN</a-select-option>
          </a-select>
          <a-select show-search option-filter-prop="children" v-model="listQuery.group" allowClear placeholder="分组" class="search-input-item">
            <a-select-option v-for="item in groupList" :key="item">{{ item }}</a-select-option>
          </a-select>

          <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
            <a-button type="primary" :loading="loading" @click="loadData">搜索</a-button>
          </a-tooltip>
          <a-button type="primary" @click="handleAdd">新增</a-button>
          <a-tooltip>
            <template slot="title">使用 Access Token 一次导入多个项目<br />点击<a target="_blank" href="https://jpom.top/pages/jpom-server-import-multi-repos/">文档链接</a>查看详情</template>
            <a-button type="primary" @click="handleAddGitee"><a-icon type="question-circle" theme="filled" />令牌导入</a-button>
          </a-tooltip>
          <a-button type="primary" @click="handlerExportData">导出</a-button>
          <a-dropdown>
            <a-menu slot="overlay">
              <a-menu-item key="1"> <a-button type="primary" @click="handlerImportTemplate()">下载导入模板</a-button> </a-menu-item>
            </a-menu>

            <a-upload name="file" accept=".csv" action="" :showUploadList="false" :multiple="false" :before-upload="beforeUpload">
              <a-button type="primary" icon="upload"> 导入 <a-icon type="down" /> </a-button>
            </a-upload>
          </a-dropdown>
        </a-space>
      </template>
      <a-tooltip slot="tooltip" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>

      <template slot="repoType" slot-scope="text">
        <span v-if="text === 0">GIT</span>
        <span v-else-if="text === 1">SVN</span>
        <span v-else>未知</span>
      </template>
      <template slot="protocol" slot-scope="text, record">
        <span v-if="text === 0">HTTP(S)</span>
        <span v-else-if="text === 1">SSH</span>
        <!-- if no protocol value, get a default value from gitUrl -->
        <span v-else>{{ record.gitUrl.indexOf("http") > -1 ? "HTTP(S)" : "SSH" }}</span>
      </template>
      <template slot="global" slot-scope="text">
        <a-tag v-if="text === 'GLOBAL'">全局</a-tag>
        <a-tag v-else>工作空间</a-tag>
      </template>
      <template slot="operation" slot-scope="text, record, index">
        <a-space>
          <a-button type="primary" size="small" @click="handleEdit(record)">编辑</a-button>
          <a-button type="primary" v-if="global" size="small" @click="viewBuild(record)">关联</a-button>
          <a-button type="danger" size="small" @click="handleDelete(record)">删除</a-button>

          <a-dropdown>
            <a class="ant-dropdown-link" @click="(e) => e.preventDefault()">
              更多
              <a-icon type="down" />
            </a>
            <a-menu slot="overlay">
              <a-menu-item>
                <a-button size="small" type="primary" :disabled="(listQuery.page - 1) * listQuery.limit + (index + 1) <= 1" @click="sortItemHander(record, index, 'top')">置顶</a-button>
              </a-menu-item>
              <a-menu-item>
                <a-button size="small" type="primary" :disabled="(listQuery.page - 1) * listQuery.limit + (index + 1) <= 1" @click="sortItemHander(record, index, 'up')">上移</a-button>
              </a-menu-item>
              <a-menu-item>
                <a-button size="small" type="primary" :disabled="(listQuery.page - 1) * listQuery.limit + (index + 1) === listQuery.total" @click="sortItemHander(record, index, 'down')">
                  下移
                </a-button>
              </a-menu-item>
            </a-menu>
          </a-dropdown>
        </a-space>
      </template>
    </a-table>
    <!-- 编辑区 -->
    <a-modal :zIndex="1009" destroyOnClose v-model="editVisible" title="编辑仓库" @ok="handleEditOk" :maskClosable="false">
      <a-form-model ref="editForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
        <a-form-model-item label="仓库名称" prop="name">
          <a-input v-model="temp.name" :maxLength="50" placeholder="仓库名称" />
        </a-form-model-item>
        <a-form-model-item label="分组" prop="group">
          <custom-select v-model="temp.group" :data="groupList" suffixIcon="" inputPlaceholder="添加分组" selectPlaceholder="选择分组名"> </custom-select>
        </a-form-model-item>
        <a-form-model-item label="仓库地址" prop="gitUrl">
          <a-input-group compact>
            <a-select style="width: 20%" v-model="temp.repoType" name="repoType" placeholder="仓库类型">
              <a-select-option :value="0">GIT</a-select-option>
              <a-select-option :value="1">SVN</a-select-option>
            </a-select>
            <a-input style="width: 80%" :maxLength="250" v-model="temp.gitUrl" placeholder="仓库地址" />
          </a-input-group>
        </a-form-model-item>
        <a-form-model-item label="协议" prop="protocol">
          <a-radio-group v-model="temp.protocol" name="protocol">
            <a-radio :value="0">HTTP(S)</a-radio>
            <a-radio :value="1">SSH</a-radio>
          </a-radio-group>
        </a-form-model-item>
        <!-- HTTP(S) protocol use password -->
        <template v-if="temp.protocol === 0">
          <a-form-model-item prop="userName">
            <template #label>
              <a-tooltip>
                账号
                <template slot="title"> 账号支持引用工作空间变量：<b>$ref.wEnv.xxxx</b> xxxx 为变量名称</template>
                <a-icon v-if="!temp.id" type="question-circle" theme="filled" />
              </a-tooltip>
            </template>
            <!-- <a-input v-model="temp.userName" placeholder="登录用户">
              <a-icon slot="prefix" type="user" />
            </a-input> -->
            <custom-input
              :input="temp.userName"
              :envList="envVarList"
              type="text"
              :placeholder="`登录用户`"
              @change="
                (v) => {
                  temp = { ...temp, userName: v };
                }
              "
            >
            </custom-input>
          </a-form-model-item>
          <a-form-model-item prop="password">
            <template #label>
              <a-tooltip>
                密码
                <template slot="title"> 密码支持引用工作空间变量：<b>$ref.wEnv.xxxx</b> xxxx 为变量名称</template>
                <a-icon v-if="!temp.id" type="question-circle" theme="filled" />
              </a-tooltip>
            </template>
            <!--            <a-input-password v-if="temp.id === undefined" v-model="temp.password" placeholder="登录密码">
              <a-icon slot="prefix" type="lock" />
            </a-input-password>
            <a-input-password v-if="temp.id !== undefined" v-model="temp.password" placeholder="此处不填不会修改密码">
              <a-icon slot="prefix" type="lock" />
            </a-input-password>-->
            <custom-input
              :input="temp.password"
              :envList="envVarList"
              :placeholder="`${!temp.id ? '登录密码' : '此处不填不会修改密码'}`"
              @change="
                (v) => {
                  temp = { ...temp, password: v };
                }
              "
            >
            </custom-input>
            <template #help>
              <a-tooltip v-if="temp.id" title=" 密码字段和密钥字段在编辑的时候不会返回，如果需要重置或者清空就请点我">
                <a-button size="small" type="danger" @click="restHideField(temp)">清除</a-button>
              </a-tooltip>
            </template>
          </a-form-model-item>
        </template>
        <a-form-model-item v-if="temp.repoType === 1 && temp.protocol === 1" label="账号" prop="userName">
          <a-input v-model="temp.userName" placeholder="svn ssh 必填登录用户">
            <a-icon slot="prefix" type="user" />
            <a-tooltip v-if="temp.id" slot="suffix" title=" 密码字段和密钥字段在编辑的时候不会返回，如果需要重置或者清空就请点我">
              <a-button size="small" type="danger" @click="restHideField(temp)">清除</a-button>
            </a-tooltip>
          </a-input>
        </a-form-model-item>
        <!-- SSH protocol use rsa private key -->
        <template v-if="temp.protocol === 1">
          <a-form-model-item prop="password">
            <template #label>
              <a-tooltip>
                密码
                <template slot="title"> 密码支持引用工作空间变量：<b>$ref.wEnv.xxxx</b> xxxx 为变量名称</template>
                <a-icon v-if="!temp.id" type="question-circle" theme="filled" />
              </a-tooltip>
            </template>
            <custom-input
              :input="temp.password"
              :envList="envVarList"
              :placeholder="`证书密码`"
              @change="
                (v) => {
                  temp = { ...temp, password: v };
                }
              "
            >
            </custom-input>
            <!-- <a-input-password v-model="temp.password" placeholder="证书密码">
              <a-icon slot="prefix" type="lock" />
            </a-input-password> -->
          </a-form-model-item>
          <a-form-model-item label="私钥" prop="rsaPrv">
            <a-tooltip placement="topLeft">
              <template slot="title">
                <div>
                  <p style="color: #faa">
                    注意：目前对 SSH key 访问 git 仓库地址不支持使用 ssh-keygen -t rsa -C "邮箱" 方式生成的 SSH key <br />需要使用 ssh-keygen -m PEM -t rsa -b 4096 -C "邮箱" 方式生成公私钥<br />
                  </p>
                  <p>如果在生成私钥的过程中有加密，那么需要把加密密码填充到上面的密码框中</p>
                  <p>支持两种方式填充：</p>
                  <p>
                    1. 完整的私钥内容 如: <br />-----BEGIN RSA PRIVATE KEY----- <br />
                    ..... <br />
                    -----END RSA PRIVATE KEY-----
                  </p>
                  <p>2. 私钥文件绝对路径（绝对路径前面添加 file: 前缀) 如: <br />file:/Users/Hotstrip/.ssh/id_rsa</p>
                </div>
              </template>
              <a-textarea :auto-size="{ minRows: 3, maxRows: 3 }" v-model="temp.rsaPrv" placeholder="私钥,不填将使用默认的 $HOME/.ssh 目录中的配置。支持配置文件目录:file:"></a-textarea>
            </a-tooltip>
          </a-form-model-item>
          <!-- 公钥暂时没用到 -->
          <a-form-model-item label="公钥" prop="rsaPub" v-if="false">
            <a-textarea :auto-size="{ minRows: 3, maxRows: 3 }" v-model="temp.rsaPub" placeholder="公钥,不填将使用默认的 $HOME/.ssh 目录中的配置。支持配置文件目录:file:"></a-textarea>
          </a-form-model-item>
        </template>
        <a-form-model-item label="共享" prop="global" v-if="this.workspaceId !== 'GLOBAL'">
          <a-radio-group v-model="temp.global">
            <a-radio :value="true"> 全局</a-radio>
            <a-radio :value="false"> 当前工作空间</a-radio>
          </a-radio-group>
        </a-form-model-item>
        <!-- <a-form-model-item v-if="temp.id" prop="restHideField">
          <template slot="label">
            隐藏字段
            <a-tooltip>
              <template slot="title"> 密码字段和私钥字段在编辑的时候不会返回，如果需要重置或者清空就请点我 </template>
              <a-icon type="question-circle" theme="filled" />
            </a-tooltip>
          </template>
          <a-button style="margin-left: 10px" type="danger" @click="restHideField(temp)">清除</a-button>
        </a-form-model-item> -->
        <a-form-model-item label="超时时间(s)" prop="timeout">
          <a-input-number v-model="temp.timeout" :min="0" placeholder="拉取仓库超时时间,单位秒" style="width: 100%" />
        </a-form-model-item>
      </a-form-model>
    </a-modal>
    <a-modal :zIndex="1009" destroyOnClose v-model="giteeImportVisible" title="通过私人令牌导入仓库" width="80%" :footer="null" :maskClosable="false">
      <a-form-model :label-col="{ span: 4 }" :rules="giteeImportFormRules" :model="giteeImportForm" ref="giteeImportForm" :wrapper-col="{ span: 20 }">
        <a-form-model-item prop="token" label="私人令牌" help="使用私人令牌，可以在你不输入账号密码的情况下对你账号内的仓库进行管理，你可以在创建令牌时指定令牌所拥有的权限。">
          <a-tooltip :title="`${giteeImportForm.type} 的令牌${importTypePlaceholder[giteeImportForm.type]}`">
            <!-- <a-input v-model="giteeImportForm.token" :placeholder="importTypePlaceholder[giteeImportForm.type]">
              <a-select slot="addonBefore" style="width: 100px" @change="importChange" v-model="giteeImportForm.type">
                <a-select-option :value="item" v-for="item in Object.keys(providerData)" :key="item"> {{ item }}</a-select-option>
              </a-select>

              <a-button slot="addonAfter" size="small" type="primary" icon="search" @click="handleGiteeImportFormOk"></a-button>
            </a-input> -->
            <a-input-group compact>
              <a-select style="width: 10%" @change="importChange" v-model="giteeImportForm.type">
                <a-select-option :value="item" v-for="item in Object.keys(providerData)" :key="item"> {{ item }}</a-select-option>
              </a-select>

              <a-input-search
                style="width: 90%; margin-top: 1px"
                enter-button
                v-model="giteeImportForm.token"
                @search="handleGiteeImportFormOk"
                :placeholder="importTypePlaceholder[giteeImportForm.type]"
              />
            </a-input-group>
          </a-tooltip>
        </a-form-model-item>
        <a-form-model-item prop="address" label="地址">
          <a-input v-model="giteeImportForm.address" placeholder="请填写平台地址" />
        </a-form-model-item>
        <a-form-model-item prop="condition" label="搜索" help="输入仓库名称或者仓库路径进行搜索" v-if="providerData[giteeImportForm.type].query">
          <a-input v-model="giteeImportForm.condition" placeholder="输入仓库名称或者仓库路径进行搜索" />
        </a-form-model-item>
      </a-form-model>
      <a-table :loading="loading" size="middle" :columns="reposColumns" :data-source="repos" bordered rowKey="full_name" @change="reposChange" :pagination="reposPagination">
        <template slot="private" slot-scope="text, record">
          <a-switch size="small" :disabled="true" :checked="record.private" />
        </template>
        <a-tooltip slot="name" slot-scope="text" placement="topLeft" :title="text">
          <span>{{ text }}</span>
        </a-tooltip>
        <a-tooltip slot="full_name" slot-scope="text" placement="topLeft" :title="text">
          <span>{{ text }}</span>
        </a-tooltip>
        <a-tooltip slot="url" slot-scope="text" placement="topLeft" :title="text">
          <span>{{ text }}</span>
        </a-tooltip>
        <a-tooltip slot="description" slot-scope="text" placement="topLeft" :title="text">
          <span>{{ text }}</span>
        </a-tooltip>

        <template slot="operation" slot-scope="text, record">
          <a-button type="primary" size="small" :disabled="record.exists" @click="handleGiteeRepoAdd(record)">{{ record.exists ? "已存在" : "添加" }}</a-button>
        </template>
      </a-table>
    </a-modal>
    <!-- 选择仓库确认区域 -->
    <div style="padding-top: 50px" v-if="this.choose">
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
          zIndex: 1,
        }"
      >
        <a-space>
          <a-button
            @click="
              () => {
                this.$emit('cancel');
              }
            "
          >
            取消
          </a-button>
          <a-button type="primary" @click="handerConfirm"> 确定 </a-button>
        </a-space>
      </div>
    </div>
    <!-- 关联构建 -->
    <a-modal destroyOnClose v-model="viewBuildVisible" width="80vw" title="当前工作空间关联构建" :maskClosable="false" :footer="null">
      <component :is="buildListComponent" v-if="buildListComponent" :repositoryId="temp.id" :fullContent="false" />
    </a-modal>
  </div>
</template>
<script>
import CustomInput from "@/components/customInput";
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
  listRepositoryGroup,
} from "@/api/repository";
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from "@/utils/const";
import { getWorkspaceEnvAll } from "@/api/workspace";
import CustomSelect from "@/components/customSelect";
export default {
  components: { CustomInput, CustomSelect },
  props: {
    choose: {
      type: Boolean,
      default: false,
    },
    workspaceId: {
      type: String,
      default: "",
    },
    global: {
      type: Boolean,
      default: false,
    },
    chooseVal: {
      type: String,
      default: "",
    },
  },
  data() {
    return {
      loading: false,
      PAGE_DEFAULT_SIZW_OPTIONS: ["15", "20", "25", "30", "35", "40", "50"],
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY, { limit: 15 }),
      list: [],
      groupList: [],
      providerData: {
        gitee: {
          baseUrl: "https://gitee.com",
          name: "gitee",
          query: true,
        },
      },
      total: 0,
      temp: {},
      isSystem: false,
      editVisible: false,
      giteeImportVisible: false,
      repos: [],
      username: null,

      columns: [
        { title: "仓库名称", dataIndex: "name", width: 200, sorter: true, ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "分组名", dataIndex: "group", ellipsis: true, width: "100px", scopedSlots: { customRender: "tooltip" } },
        {
          title: "仓库地址",
          dataIndex: "gitUrl",
          width: 300,
          sorter: true,
          ellipsis: true,
          scopedSlots: { customRender: "tooltip" },
        },
        {
          title: "仓库类型",
          dataIndex: "repoType",
          width: 100,
          sorter: true,
          ellipsis: true,
          scopedSlots: { customRender: "repoType" },
        },
        {
          title: "协议",
          dataIndex: "protocol",
          width: 100,
          sorter: true,
          ellipsis: true,
          scopedSlots: { customRender: "protocol" },
        },
        { title: "共享", dataIndex: "workspaceId", ellipsis: true, scopedSlots: { customRender: "global" }, width: "90px" },
        { title: "创建人", dataIndex: "createUser", ellipsis: true, scopedSlots: { customRender: "tooltip" }, width: "120px" },
        { title: "修改人", dataIndex: "modifyUser", ellipsis: true, scopedSlots: { customRender: "tooltip" }, width: "120px" },
        {
          title: "创建时间",
          dataIndex: "createTimeMillis",
          sorter: true,
          customRender: (text) => parseTime(text),
          width: "170px",
        },
        {
          title: "修改时间",
          dataIndex: "modifyTimeMillis",
          sorter: true,
          customRender: (text) => parseTime(text),
          width: "170px",
        },
        {
          title: "操作",
          dataIndex: "operation",
          fixed: "right",
          align: "center",
          width: this.global ? "220px" : "180px",
          scopedSlots: { customRender: "operation" },
        },
      ],
      reposColumns: [
        { title: "仓库名称", dataIndex: "name", ellipsis: true, scopedSlots: { customRender: "name" } },
        { title: "仓库路径", dataIndex: "full_name", ellipsis: true, scopedSlots: { customRender: "full_name" } },
        { title: "GitUrl", dataIndex: "url", ellipsis: true, scopedSlots: { customRender: "url" } },

        {
          title: "描述",
          dataIndex: "description",

          ellipsis: true,
          scopedSlots: { customRender: "description" },
        },
        { title: "私有", dataIndex: "private", width: 80, ellipsis: true, scopedSlots: { customRender: "private" } },
        {
          title: "操作",
          dataIndex: "operation",
          width: 100,
          scopedSlots: { customRender: "operation" },
          align: "left",
        },
      ],
      giteeImportForm: Object.assign({}, PAGE_DEFAULT_LIST_QUERY, { limit: 15, type: "gitee", address: "https://gitee.com" }),
      giteeImportFormRules: {
        token: [{ required: true, message: "请输入私人令牌", trigger: "blur" }],
        // address: [{ required: true, message: "请填写平台地址", trigger: "blur" }],
      },
      rules: {
        name: [{ required: true, message: "请填写仓库名称", trigger: "blur" }],
        gitUrl: [{ required: true, message: "请填写仓库地址", trigger: "blur" }],
      },
      importTypePlaceholder: {
        gitee: "在 设置-->安全设置-->私人令牌 中获取",
        github: "在 Settings-->Developer settings-->Personal access tokens 中获取",
        gitlab_v3: "在 preferences-->Access Tokens 中获取",
        gitlab: "在 preferences-->Access Tokens 中获取",
        gitea: "在 设置 --> 应用 --> 生成令牌",
        gogs: "在 设置 --> 应用 --> 生成令牌",
        other: "请输入私人令牌",
      },
      tableSelections: [],
      envVarList: [],
      buildListComponent: null,
      viewBuildVisible: false,
    };
  },
  computed: {
    // 分页
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery, this.PAGE_DEFAULT_SIZW_OPTIONS);
    },
    reposPagination() {
      return COMPUTED_PAGINATION(this.giteeImportForm, this.PAGE_DEFAULT_SIZW_OPTIONS);
    },
    rowSelection() {
      return {
        onChange: (selectedRowKeys) => {
          this.tableSelections = selectedRowKeys;
        },
        selectedRowKeys: this.tableSelections,
        type: "radio",
      };
    },
  },
  watch: {},
  created() {
    this.loadData();
    //
    providerInfo().then((response) => {
      if (response.code === 200) {
        this.providerData = response.data;
      }
    });
    this.getWorkEnvList();
    this.loadGroupList();
    // 异步加载组件
    this.buildListComponent = () => import("@/pages/build/list-info");
    if (this.chooseVal) {
      this.tableSelections = [this.chooseVal];
    }
  },
  methods: {
    CHANGE_PAGE,
    // 分组数据
    loadGroupList() {
      listRepositoryGroup().then((res) => {
        if (res.data) {
          this.groupList = res.data;
        }
      });
    },
    getWorkEnvList() {
      getWorkspaceEnvAll({
        workspaceId: this.workspaceId + (this.global ? ",GLOBAL" : ""),
      }).then((res) => {
        if (res.code === 200) {
          this.envVarList = res.data;
        }
      });
    },
    // 加载数据
    loadData(pointerEvent) {
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page;
      this.loading = true;
      if (this.workspaceId) {
        this.listQuery = { ...this.listQuery, workspaceId: this.workspaceId };
      }
      getRepositoryList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result;
          this.listQuery.total = res.data.total;
        }
        this.loading = false;
      });
    },
    importChange(value) {
      this.giteeImportForm.address = this.providerData[value].baseUrl;
    },
    // // 筛选
    // handleFilter() {
    //   this.loadData();
    // },
    // 添加
    handleAdd() {
      this.temp = {
        repoType: 0,
        protocol: 0,
      };
      if (!this.global) {
        this.temp = { ...this.temp, workspaceId: "GLOBAL", global: true };
      }

      this.editVisible = true;
    },
    handleAddGitee() {
      this.giteeImportVisible = true;
    },
    // 下载导入模板
    handlerImportTemplate() {
      window.open(importTemplate(), "_blank");
    },
    handlerExportData() {
      window.open(exportData({ ...this.listQuery, workspaceId: this.workspaceId }), "_blank");
    },
    beforeUpload(file) {
      const formData = new FormData();
      formData.append("file", file);
      formData.append("workspaceId", this.workspaceId);
      importData(formData).then((res) => {
        if (res.code === 200) {
          this.$notification.success({
            message: res.msg,
          });
          this.loadData();
        }
      });
    },
    handleGiteeImportFormOk() {
      this.$refs["giteeImportForm"].validate((valid) => {
        if (!valid) {
          return false;
        }
        authorizeRepos(this.giteeImportForm).then((res) => {
          if (res.code === 200) {
            // 成功
            //this.username = res.data.username;
            this.giteeImportForm.total = res.data.total;
            this.repos = res.data.result;
          }
        });
      });
    },
    reposChange(pagination) {
      this.giteeImportForm.page = pagination.current;
      this.giteeImportForm.limit = pagination.pageSize;
      this.handleGiteeImportFormOk();
    },
    handleGiteeRepoAdd(record) {
      let data = {
        repoType: 0,
        protocol: 0,
        userName: record.username,
        password: this.giteeImportForm.token,
        name: record.name,
        gitUrl: record.url,
      };
      if (!this.global) {
        data = { ...data, workspaceId: "GLOBAL", global: true };
      }
      editRepository(data).then((res) => {
        if (res.code === 200) {
          // 成功
          this.$notification.success({
            message: res.msg,
          });
          record.exists = true;
          this.loadData();
        }
      });
    },
    // 修改
    handleEdit(record) {
      this.temp = Object.assign({}, record);
      if (this.temp.protocol === undefined) {
        this.temp.protocol = this.temp.gitUrl.indexOf("http") > -1 ? 0 : 1;
      }
      this.temp = { ...this.temp, global: record.workspaceId === "GLOBAL", workspaceId: "" };
      this.editVisible = true;
    },
    // 提交节点数据
    handleEditOk() {
      // 检验表单
      this.$refs["editForm"].validate((valid) => {
        if (!valid) {
          return false;
        }
        // 提交数据
        editRepository(this.temp).then((res) => {
          if (res.code === 200) {
            // 成功
            this.$notification.success({
              message: res.msg,
            });
            this.editVisible = false;
            this.loadData();
            this.$refs["editForm"].resetFields();
            this.loadGroupList();
          }
        });
      });
    },
    // 删除
    handleDelete(record) {
      this.$confirm({
        title: "系统提示",
        content: "真的要删除仓库信息么？",
        okText: "确认",
        cancelText: "取消",
        zIndex: 1009,
        onOk: () => {
          const params = {
            id: record.id,
            //isRealDel: this.isSystem,
          };
          // 删除
          deleteRepository(params).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg,
              });
              this.loadData();
            }
          });
        },
      });
    },

    // 清除隐藏字段
    restHideField(record) {
      this.$confirm({
        title: "系统提示",
        content: "真的要清除仓库隐藏字段信息么？（密码，私钥）",
        okText: "确认",
        cancelText: "取消",
        zIndex: 1009,
        onOk: () => {
          // 恢复
          restHideField(record.id).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg,
              });
              this.loadData();
            }
          });
        },
      });
    },

    // 排序
    sortItemHander(record, index, method) {
      const msgData = {
        top: "确定要将此数据置顶吗？",
        up: "确定要将此数上移吗？",
        down: "确定要将此数据下移吗？下移操作可能因为列表后续数据没有排序值操作无效！",
      };
      let msg = msgData[method] || "确定要操作吗？";
      if (!record.sortValue) {
        msg += " 当前数据为默认状态,操后上移或者下移可能不会达到预期排序,还需要对相关数据都操作后才能达到预期排序";
      }
      // console.log(this.list, index, this.list[method === "top" ? index : method === "up" ? index - 1 : index + 1]);
      const compareId = this.list[method === "top" ? index : method === "up" ? index - 1 : index + 1].id;
      this.$confirm({
        title: "系统提示",
        content: msg,
        okText: "确认",
        cancelText: "取消",
        zIndex: 1009,
        onOk: () => {
          // 解锁
          sortItem({
            id: record.id,
            method: method,
            compareId: compareId,
          }).then((res) => {
            if (res.code == 200) {
              this.$notification.success({
                message: res.msg,
              });

              this.loadData();
              return false;
            }
          });
        },
      });
    },
    // 确认
    handerConfirm() {
      if (!this.tableSelections.length) {
        this.$notification.warning({
          message: "请选择要使用的仓库",
        });
        return;
      }
      const selectData = this.list.filter((item) => {
        return item.id === this.tableSelections[0];
      })[0];

      this.$emit("confirm", `${selectData.id}`);
    },
    // 查看关联构建
    viewBuild(data) {
      this.temp = { id: data.id };
      this.viewBuildVisible = true;
    },
  },
};
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
