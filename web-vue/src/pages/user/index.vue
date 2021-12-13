<template>
  <div class="full-content">
    <div ref="filter" class="filter">
      <a-input v-model="listQuery.id" placeholder="用户名ID" class="search-input-item" />
      <a-input v-model="listQuery['%name%']" placeholder="用户名" class="search-input-item" />
      <a-tooltip title="按住 Ctr 或者 Alt 键点击按钮快速回到第一页">
        <a-button type="primary" @click="loadData">搜索</a-button>
      </a-tooltip>
      <a-button type="primary" @click="handleAdd">新增</a-button>
    </div>
    <!-- 数据表格 -->
    <a-table :data-source="list" :loading="loading" :columns="columns" :pagination="(this, pagination)" @change="changePage" bordered :rowKey="(record, index) => index">
      <template slot="operation" slot-scope="text, record">
        <a-button type="primary" @click="handleEdit(record)">编辑</a-button>
        <a-button type="danger" @click="handleDelete(record)">删除</a-button>
        <a-button type="danger" :disabled="record.pwdErrorCount === 0" @click="handleUnlock(record)">解锁</a-button>
      </template>
      <template slot="systemUser" slot-scope="text, record">
        <a-switch size="small" checked-children="是" un-checked-children="否" :checked="record.systemUser == 1" />
      </template>
    </a-table>
    <!-- 编辑区 -->
    <a-modal v-model="editUserVisible" width="600px" title="编辑用户" @ok="handleEditUserOk" :maskClosable="false">
      <a-form-model ref="editUserForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="登录名称" prop="id">
          <a-input v-model="temp.id" placeholder="创建之后不能修改" :disabled="createOption == false" />
        </a-form-model-item>
        <a-form-model-item label="密码" :prop="createOption ? 'password' : 'none'">
          <a-input-password v-model="temp.password" :placeholder="createOption ? '登录密码' : '如果不修改密码则不用填写'" />
        </a-form-model-item>
        <a-form-model-item label="昵称" prop="name">
          <a-input v-model="temp.name" placeholder="昵称" />
        </a-form-model-item>
        <a-form-model-item label="管理员" prop="systemUser">
          <a-tooltip title="管理员拥有：管理服务端和插件端的权限">
            <a-switch
              :checked="temp.systemUser == 1"
              @change="
                (checked) => {
                  temp.systemUser = checked ? 1 : 0;
                }
              "
              checked-children="是"
              un-checked-children="否"
              default-checked
            />
          </a-tooltip>
        </a-form-model-item>

        <a-form-model-item label="工作空间" prop="feature" class="feature jpom-userWorkspace">
          <a-transfer :data-source="workspaceList" show-search :filter-option="filterOption" :target-keys="targetKeys" :render="(item) => item.title" @change="handleChange" />
        </a-form-model-item>
      </a-form-model>
    </a-modal>
  </div>
</template>
<script>
import { mapGetters } from "vuex";
import { getUserList, editUser, deleteUser, unlockUser, workspaceList } from "@/api/user";
import { getWorkSpaceListAll } from "@/api/workspace";
import { parseTime } from "@/utils/time";
import sha1 from "sha1";
import { PAGE_DEFAULT_LIMIT, PAGE_DEFAULT_SIZW_OPTIONS, PAGE_DEFAULT_SHOW_TOTAL, PAGE_DEFAULT_LIST_QUERY } from "@/utils/const";
export default {
  data() {
    return {
      loading: false,
      list: [],
      workspaceList: [],
      targetKeys: [],
      temp: {},
      createOption: true,
      editUserVisible: false,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      columns: [
        { title: "ID", dataIndex: "id", ellipsis: true, width: 150 },
        { title: "昵称", dataIndex: "name", ellipsis: true },
        { title: "管理员", dataIndex: "systemUser", ellipsis: true, width: 90, scopedSlots: { customRender: "systemUser" } },
        { title: "邮箱", dataIndex: "email", ellipsis: true, width: 150 },
        { title: "创建人", dataIndex: "parent", ellipsis: true, width: 150 },
        {
          title: "修改时间",
          dataIndex: "modifyTimeMillis",
          sorter: true,
          ellipsis: true,
          customRender: (text) => {
            return parseTime(text);
          },
          width: 150,
        },
        { title: "操作", dataIndex: "operation", scopedSlots: { customRender: "operation" }, width: 260 },
      ],
      // 表单校验规则
      rules: {
        id: [{ required: true, message: "Please input login name", trigger: "blur" }],
        name: [{ required: true, message: "Please input nickName", trigger: "blur" }],
        password: [
          { required: true, message: "Please input password", trigger: "blur" },
          { max: 20, message: "密码长度为6-20", trigger: "blur" },
          { min: 6, message: "密码长度为6-20", trigger: "blur" },
        ],
      },
    };
  },
  computed: {
    ...mapGetters(["getGuideFlag"]),
    pagination() {
      return {
        total: this.listQuery.total,
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
  watch: {
    getGuideFlag() {
      this.introGuide();
    },
  },
  created() {
    this.loadData();
  },
  methods: {
    // 页面引导
    introGuide() {
      if (this.getGuideFlag) {
        this.$introJs()
          .setOptions({
            hidePrev: true,
            steps: [
              {
                title: "Jpom 导航助手",
                element: document.querySelector(".jpom-userWorkspace"),
                intro: "如果这里面没有你想要的工作空间信息，你需要先去添加一个工作空间。",
              },
            ],
          })
          .start();
        return false;
      }
      this.$introJs().exit();
    },
    // 加载数据
    loadData(pointerEvent) {
      this.loading = true;
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page;
      getUserList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result;
          this.listQuery.total = res.data.total;
        }
        this.loading = false;
      });
    },
    // 加载工作空间数据
    loadWorkSpaceListAll() {
      this.workspaceList = [];
      getWorkSpaceListAll().then((res) => {
        if (res.code === 200) {
          res.data.forEach((element) => {
            this.workspaceList.push({ key: element.id, title: element.name });
          });
        }
      });
    },
    // 穿梭框筛选
    filterOption(inputValue, option) {
      return option.title.indexOf(inputValue) > -1;
    },
    // 穿梭框 change
    handleChange(targetKeys) {
      this.targetKeys = targetKeys;
    },
    // 新增用户
    handleAdd() {
      this.createOption = true;
      this.temp = {};
      this.loadWorkSpaceListAll();
      this.editUserVisible = true;
      this.$nextTick(() => {
        setTimeout(() => {
          this.introGuide();
        }, 500);
      });
    },
    // 修改用户
    handleEdit(record) {
      workspaceList(record.id).then((res) => {
        this.createOption = false;
        this.temp = Object.assign(record);
        // 设置选中 key
        this.targetKeys = [];
        res.data.forEach((element) => {
          this.targetKeys.push(element.workspaceId);
        });
        this.loadWorkSpaceListAll();
        this.editUserVisible = true;
      });
    },
    // 提交用户数据
    handleEditUserOk() {
      // 检验表单
      this.$refs["editUserForm"].validate((valid) => {
        if (!valid) {
          return false;
        }
        // 判断是否选择了工作空间
        if (this.targetKeys.length === 0) {
          this.$notification.error({
            message: "请选择工作空间",
          });
          return false;
        }
        // 加密密码
        const paramsTemp = Object.assign({}, this.temp);
        if (paramsTemp.password) {
          if (paramsTemp.password.length < 6 || paramsTemp.password.length > 20) {
            this.$notification.warn({
              message: "密码长度为6-20",
            });
            return;
          }
          paramsTemp.password = sha1(this.temp.password);
        }
        paramsTemp.type = this.createOption ? "add" : "edit";
        // 设置选择的角色
        paramsTemp.workspace = JSON.stringify(this.targetKeys);
        // 需要判断当前操作是【新增】还是【修改】
        editUser(paramsTemp).then((res) => {
          if (res.code === 200) {
            this.$notification.success({
              message: res.msg,
            });
            this.$refs["editUserForm"].resetFields();
            this.editUserVisible = false;
            this.loadData();
          }
        });
      });
    },
    // 删除用户
    handleDelete(record) {
      this.$confirm({
        title: "系统提示",
        content: "真的要删除用户么？",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 删除
          deleteUser(record.id).then((res) => {
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
    // 解锁
    handleUnlock(record) {
      this.$confirm({
        title: "系统提示",
        content: "真的要解锁用户么？",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 解锁用户
          unlockUser(record.id).then((res) => {
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
</style>
