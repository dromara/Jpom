<template>
  <div class="full-content">
    <!-- 搜索区 -->
    <div ref="filter" class="filter">
      <a-input class="search-input-item" v-model="listQuery['%name%']" placeholder="仓库名" />
      <a-input class="search-input-item" v-model="listQuery['%gitUrl%']" placeholder="仓库地址" />
      <a-select v-model="listQuery.repoType" allowClear placeholder="请选择仓库类型" class="filter-item" @change="handleFilter">
        <a-select-option :value="'0'">GIT</a-select-option>
        <a-select-option :value="'1'">SVN</a-select-option>
      </a-select>
      <a-select v-if="isSystem" default-value="0" v-model="listQuery.strike" allowClear placeholder="选择删除情况" class="filter-item" @change="handleFilter">
        <a-select-option :value="0">未删除</a-select-option>
        <a-select-option :value="1">已删除</a-select-option>
      </a-select>
      <a-button type="primary" @click="handleFilter">搜索</a-button>
      <a-button type="primary" @click="handleAdd">新增</a-button>
      <a-button type="primary" @click="loadData">刷新</a-button>
    </div>
    <!-- 表格 -->
    <a-table :loading="loading" :columns="columns" :data-source="list" bordered rowKey="id" :pagination="pagination" @change="changePage">
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
        <a-button type="primary" @click="handleEdit(record)">编辑</a-button>
        <a-button type="danger" @click="handleDelete(record)">删除</a-button>
        <a-button v-if="isSystem && record.strike === 1" type="primary" @click="handlerecovery(record)">恢复</a-button>
      </template>
    </a-table>
    <!-- 编辑区 -->
    <a-modal v-model="editVisible" title="编辑仓库" @ok="handleEditOk" :maskClosable="false">
      <a-form-model ref="editForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
        <a-form-model-item label="仓库名称" prop="name">
          <a-input v-model="temp.name" placeholder="仓库名称" />
        </a-form-model-item>
        <a-form-model-item label="仓库地址" prop="gitUrl">
          <a-input-group compact>
            <a-select style="width: 20%" v-model="temp.repoType" name="repoType" placeholder="仓库类型">
              <a-select-option :value="0">GIT</a-select-option>
              <a-select-option :value="1">SVN</a-select-option>
            </a-select>
            <a-input style="width: 80%" v-model="temp.gitUrl" placeholder="仓库地址" />
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
          <!-- 公钥暂时没用用到 -->
          <a-form-model-item label="公钥" prop="rsaPub" v-if="false">
            <a-textarea :auto-size="{ minRows: 3, maxRows: 3 }" v-model="temp.rsaPub" placeholder="公钥,不填将使用默认的 $HOME/.ssh 目录中的配置。支持配置文件目录:file:"></a-textarea>
          </a-form-model-item>
        </template>
        <a-form-model-item v-if="temp.id">
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
  </div>
</template>
<script>
import { getRepositoryList, editRepository, deleteRepository, recoveryRepository, restHideField } from "../../api/repository";
import { parseTime } from "@/utils/time";
import { PAGE_DEFAULT_LIMIT, PAGE_DEFAULT_SIZW_OPTIONS, PAGE_DEFAULT_SHOW_TOTAL } from "@/utils/const";

export default {
  components: {},
  data() {
    return {
      loading: false,
      listQuery: {
        page: 1,
        limit: PAGE_DEFAULT_LIMIT,
        strike: 0,
      },
      list: [],
      total: 0,
      temp: {},
      isSystem: false,
      editVisible: false,
      columns: [
        { title: "仓库名称", dataIndex: "name", sorter: true, width: 150, ellipsis: true, scopedSlots: { customRender: "name" } },
        {
          title: "仓库地址",
          dataIndex: "gitUrl",
          width: 300,
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
          width: 160,
          scopedSlots: { customRender: "operation" },
          align: "left",
        },
      ],
      rules: {
        name: [{ required: true, message: "Please input build name", trigger: "blur" }],
        gitUrl: [{ required: true, message: "Please input git url", trigger: "blur" }],
      },
    };
  },
  computed: {
    // 分页
    pagination() {
      return {
        total: this.total,
        current: this.listQuery.page || 1,
        pageSize: this.listQuery.limit || PAGE_DEFAULT_LIMIT,
        pageSizeOptions: PAGE_DEFAULT_SIZW_OPTIONS,
        showSizeChanger: true,
        showTotal: (total) => {
          return PAGE_DEFAULT_SHOW_TOTAL(total, this.listQuery);
        },
      };
    },
  },
  watch: {},
  created() {
    this.handleFilter();
    this.isSystem = this.$store.getters.getUserInfo.systemUser;
  },
  methods: {
    // 加载数据
    loadData() {
      this.list = [];
      this.loading = true;
      getRepositoryList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result;
          this.total = res.data.total;
        }
        this.loading = false;
      });
    },
    // 筛选
    handleFilter() {
      this.loadData();
    },
    // 添加
    handleAdd() {
      this.temp = {
        repoType: 0,
        protocol: 0,
      };
      this.editVisible = true;
    },
    // 修改
    handleEdit(record) {
      this.temp = Object.assign(record);
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
              duration: 2,
            });
            this.$refs["editForm"].resetFields();
            this.editVisible = false;
            this.handleFilter();
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
            isRealDel: this.isSystem,
          };
          // 删除
          deleteRepository(params).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg,
                duration: 2,
              });
              this.loadData();
            }
          });
        },
      });
    },
    handlerecovery(record) {
      this.$confirm({
        title: "系统提示",
        content: "真的要恢复仓库信息么？",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 恢复
          recoveryRepository(record.id).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg,
                duration: 2,
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
                duration: 2,
              });
              this.loadData();
            }
          });
        },
      });
    },
    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery.page = pagination.current;
      this.listQuery.limit = pagination.pageSize;
      if (sorter) {
        this.listQuery.order = sorter.order;
        this.listQuery.order_field = sorter.field;
      }
      this.loadData();
    },
  },
};
</script>
<style scoped>
.filter {
  margin-bottom: 10px;
}

.ant-btn {
  margin-right: 10px;
}

.filter-item {
  width: 150px;
  margin-right: 10px;
}

.btn-add {
  margin-left: 10px;
  margin-right: 0;
}
</style>
