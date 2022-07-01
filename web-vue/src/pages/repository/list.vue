<template>
  <div class="full-content">
    <!-- 搜索区 -->
    <!-- <div ref="filter" class="filter"></div> -->
    <!-- 表格 -->
    <a-table size="middle" :columns="columns" :data-source="list" bordered rowKey="id" :pagination="pagination" @change="changePage">
      <template slot="title">
        <a-space>
          <a-input class="search-input-item" @pressEnter="loadData" v-model="listQuery['%name%']" placeholder="仓库名称" />
          <a-input class="search-input-item" @pressEnter="loadData" v-model="listQuery['%gitUrl%']" placeholder="仓库地址" />
          <a-select
            :getPopupContainer="
              (triggerNode) => {
                return triggerNode.parentNode || document.body;
              }
            "
            v-model="listQuery.repoType"
            allowClear
            placeholder="仓库类型"
            class="search-input-item"
          >
            <a-select-option :value="'0'">GIT</a-select-option>
            <a-select-option :value="'1'">SVN</a-select-option>
          </a-select>

          <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
            <a-button type="primary" :loading="loading" @click="loadData">搜索</a-button>
          </a-tooltip>
          <a-button type="primary" @click="handleAdd">新增</a-button>
          <a-button type="primary" @click="handleAddGitee">通过私人令牌导入仓库</a-button>
        </a-space>
      </template>
      <a-tooltip slot="name" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-tooltip slot="gitUrl" slot-scope="text" placement="topLeft" :title="text">
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
      <template slot="operation" slot-scope="text, record">
        <a-space>
          <a-button type="primary" size="small" @click="handleEdit(record)">编辑</a-button>
          <a-button type="danger" size="small" @click="handleDelete(record)">删除</a-button>
        </a-space>
      </template>
    </a-table>
    <!-- 编辑区 -->
    <a-modal v-model="editVisible" title="编辑仓库" @ok="handleEditOk" :maskClosable="false">
      <a-form-model ref="editForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
        <a-form-model-item label="仓库名称" prop="name">
          <a-input v-model="temp.name" :maxLength="50" placeholder="仓库名称" />
        </a-form-model-item>
        <a-form-model-item label="仓库地址" prop="gitUrl">
          <a-input-group compact>
            <a-select
              :getPopupContainer="
                (triggerNode) => {
                  return triggerNode.parentNode || document.body;
                }
              "
              style="width: 20%"
              v-model="temp.repoType"
              name="repoType"
              placeholder="仓库类型"
            >
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
          <a-form-model-item label="账号" prop="userName">
            <a-input v-model="temp.userName" placeholder="登录用户">
              <a-icon slot="prefix" type="user" />
            </a-input>
          </a-form-model-item>
          <a-form-model-item label="密码" prop="password">
            <a-input-password v-if="temp.id === undefined" v-model="temp.password" placeholder="登录密码">
              <a-icon slot="prefix" type="lock" />
            </a-input-password>
            <a-input-password v-if="temp.id !== undefined" v-model="temp.password" placeholder="此处不填不会修改密码">
              <a-icon slot="prefix" type="lock" />
            </a-input-password>
          </a-form-model-item>
        </template>
        <a-form-model-item v-if="temp.repoType === 1 && temp.protocol === 1" label="账号" prop="userName">
          <a-input v-model="temp.userName" placeholder="svn ssh 必填登录用户">
            <a-icon slot="prefix" type="user" />
          </a-input>
        </a-form-model-item>
        <!-- SSH protocol use rsa private key -->
        <template v-if="temp.protocol === 1">
          <a-form-model-item label="密码" prop="password">
            <a-input-password v-model="temp.password" placeholder="证书密码">
              <a-icon slot="prefix" type="lock" />
            </a-input-password>
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
        <a-form-model-item v-if="temp.id" prop="restHideField">
          <template slot="label">
            隐藏字段
            <a-tooltip>
              <template slot="title"> 密码字段和私钥字段在编辑的时候不会返回，如果需要重置或者清空就请点我 </template>
              <a-icon type="question-circle" theme="filled" />
            </a-tooltip>
          </template>
          <a-button style="margin-left: 10px" type="danger" @click="restHideField(temp)">清除</a-button>
        </a-form-model-item>
      </a-form-model>
    </a-modal>
    <a-modal v-model="giteeImportVisible" title="导入仓库" width="80%" :footer="null" :maskClosable="false">
      <a-form-model :label-col="{ span: 4 }" :rules="giteeImportFormRules" :model="giteeImportForm" ref="giteeImportForm" :wrapper-col="{ span: 20 }">
        <a-form-model-item prop="token">
          <template slot="label">
            私人令牌
            <a-tooltip>
              <template slot="title">
                <ul>
                  <li>使用私人令牌，可以在你不输入账号密码的情况下对你账号内的仓库进行管理，你可以在创建令牌时指定令牌所拥有的权限。</li>
                </ul>
              </template>
              <a-icon type="question-circle" theme="filled" />
            </a-tooltip>
          </template>
          <a-input-group compact>
            <a-select
              :getPopupContainer="
                (triggerNode) => {
                  return triggerNode.parentNode || document.body;
                }
              "
              v-model="giteeImportForm.type"
              @change="importTypeChange"
            >
              <a-select-option value="gitee"> gitee </a-select-option>
              <a-select-option value="github"> github </a-select-option>
              <a-select-option value="gitlab"> gitlab </a-select-option>
            </a-select>
            <a-tooltip :title="`${giteeImportForm.type} 的令牌${importTypePlaceholder}`">
              <a-input-search style="width: 55%; margin-top: 1px" enter-button v-model="giteeImportForm.token" @search="handleGiteeImportFormOk" :placeholder="importTypePlaceholder" />
            </a-tooltip>
          </a-input-group>
          <a-input-group compact style="width: 105%">
            <a-tooltip title="输入仓库名称或者仓库路径进行搜索">
              <a-input style="width: 55%; margin-top: 1px" enter-button v-model="giteeImportForm.condition" placeholder="输入仓库名称或者仓库路径进行搜索" />
            </a-tooltip>
          </a-input-group>
          <a-input-group compact style="width: 105%" v-if="giteeImportForm.type === 'gitlab'">
            <a-tooltip title="请输入 GitLab 的地址，支持自建 GitLab，不需要输入协议，如：gitlab.com、gitlab.jpom.io、10.1.2.3、10.1.2.3:8888 等">
              <a-input style="width: 55%; margin-top: 1px" enter-button v-model="giteeImportForm.gitlabAddress" placeholder="gitlab.com" />
            </a-tooltip>
          </a-input-group>
        </a-form-model-item>
      </a-form-model>
      <a-table :loading="loading" :columns="reposColumns" :data-source="repos" bordered rowKey="full_name" @change="reposChange" :pagination="reposPagination">
        <template slot="private" slot-scope="text, record">
          <a-switch :disabled="true" :checked="record.private" />
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
          <a-button type="primary" :disabled="record.exists" @click="handleGiteeRepoAdd(record)">{{ record.exists ? "已存在" : "添加" }}</a-button>
        </template>
      </a-table>
    </a-modal>
  </div>
</template>
<script>
import {authorizeRepos, deleteRepository, editRepository, getRepositoryList, restHideField} from "@/api/repository";
import {parseTime} from "@/utils/time";
import {CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY} from "@/utils/const";

export default {
  components: {},
  data() {
    return {
      loading: false,
      PAGE_DEFAULT_SIZW_OPTIONS: ["15", "20", "25", "30", "35", "40", "50"],
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY, { limit: 15 }),
      list: [],
      total: 0,
      temp: {},
      isSystem: false,
      editVisible: false,
      giteeImportVisible: false,
      repos: [],
      username: null,
      importTypePlaceholder: "",
      columns: [
        { title: "仓库名称", dataIndex: "name", sorter: true, ellipsis: true, scopedSlots: { customRender: "name" } },
        {
          title: "仓库地址",
          dataIndex: "gitUrl",

          sorter: true,
          ellipsis: true,
          scopedSlots: { customRender: "gitUrl" },
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
        {
          title: "修改时间",
          dataIndex: "modifyTimeMillis",
          sorter: true,
          customRender: (text) => {
            if (!text) {
              return "";
            }
            return parseTime(text);
          },
          width: 180,
        },
        {
          title: "操作",
          dataIndex: "operation",
          align: "center",
          width: 120,
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
      giteeImportForm: Object.assign({}, PAGE_DEFAULT_LIST_QUERY, { limit: 15, type: "gitee" }),
      giteeImportFormRules: {
        token: [{ required: true, message: "请输入私人令牌", trigger: "blur" }],
      },
      rules: {
        name: [{ required: true, message: "Please input build name", trigger: "blur" }],
        gitUrl: [{ required: true, message: "Please input git url", trigger: "blur" }],
      },
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
  },
  watch: {},
  created() {
    this.loadData();
  },
  methods: {
    // 加载数据
    loadData(pointerEvent) {
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page;
      this.loading = true;
      getRepositoryList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result;
          this.listQuery.total = res.data.total;
        }
        this.loading = false;
      });
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
      this.editVisible = true;
    },
    handleAddGitee() {
      this.giteeImportVisible = true;
      this.importTypeChange(this.giteeImportForm.type);
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
      editRepository({
        repoType: 0,
        protocol: 0,
        userName: record.username,
        password: this.giteeImportForm.token,
        name: record.name,
        gitUrl: record.url,
      }).then((res) => {
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
      this.temp = { ...this.temp };
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
    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter });
      this.loadData();
    },
    // 在导入仓库时，选择不同的 git 平台显示不同的提示语
    importTypeChange(val) {
      if (val === "gitee") {
        this.importTypePlaceholder = "在 设置-->安全设置-->私人令牌 中获取";
      } else if (val === "github") {
        this.importTypePlaceholder = "在 Settings-->Developer settings-->Personal access tokens 中获取";
      } else if (val === "gitlab") {
        this.importTypePlaceholder = "在 preferences-->Access Tokens 中获取";
      } else {
        this.importTypePlaceholder = "请输入私人令牌";
      }
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
