<template>
  <div class="full-content">
    <!-- <div ref="filter" class="filter"></div> -->
    <!-- 数据表格 -->
    <a-table :data-source="list" size="middle" :columns="columns" :pagination="pagination" @change="changePage" bordered :rowKey="(record, index) => index">
      <template slot="title">
        <a-space>
          <a-input v-model="listQuery.id" @pressEnter="loadData" placeholder="用户名ID" class="search-input-item" />
          <a-input v-model="listQuery['%name%']" @pressEnter="loadData" placeholder="用户名" class="search-input-item" />
          <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
            <a-button type="primary" :loading="loading" @click="loadData">搜索</a-button>
          </a-tooltip>
          <a-button type="primary" @click="handleAdd">新增</a-button>
        </a-space></template
      >
      <template slot="operation" slot-scope="text, record">
        <a-space>
          <a-button size="small" type="primary" @click="handleEdit(record)">编辑</a-button>
          <a-dropdown>
            <a class="ant-dropdown-link" @click="(e) => e.preventDefault()"> 更多 <a-icon type="down" /> </a>
            <a-menu slot="overlay">
              <a-menu-item>
                <a-button type="danger" size="small" :disabled="record.parent === 'sys'" @click="handleDelete(record)">删除</a-button>
              </a-menu-item>
              <a-menu-item>
                <a-button type="danger" size="small" :disabled="record.pwdErrorCount === 0" @click="handleUnlock(record)">解锁</a-button>
              </a-menu-item>
              <a-menu-item>
                <a-button type="danger" size="small" :disabled="record.twoFactorAuthKey ? false : true" @click="handleCloseMfa(record)">关闭MFA</a-button>
              </a-menu-item>
            </a-menu>
          </a-dropdown>
        </a-space>
      </template>
      <template slot="systemUser" slot-scope="text, record">
        <a-switch size="small" checked-children="是" un-checked-children="否" disabled :checked="record.systemUser == 1" />
      </template>
      <template slot="twoFactorAuthKey" slot-scope="text, record">
        <a-switch size="small" checked-children="开" un-checked-children="关" disabled :checked="record.twoFactorAuthKey ? true : false" />
      </template>

      <a-tooltip slot="id" slot-scope="text" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>

      <a-tooltip slot="email" slot-scope="text" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
    </a-table>
    <!-- 编辑区 -->
    <a-modal v-model="editUserVisible" width="60vw" title="编辑用户" @ok="handleEditUserOk" :maskClosable="false">
      <a-form-model ref="editUserForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="登录名称" prop="id">
          <a-input @change="checkTipUserName" :maxLength="50" v-model="temp.id" placeholder="创建之后不能修改" :disabled="createOption == false" />
        </a-form-model-item>
        <a-form-model-item label="密码" :prop="createOption ? 'password' : 'none'">
          <a-input-password v-model="temp.password" :placeholder="createOption ? '登录密码' : '如果不修改密码则不用填写'" />
        </a-form-model-item>
        <a-form-model-item label="昵称" prop="name">
          <a-input v-model="temp.name" :maxLength="50" placeholder="昵称" />
        </a-form-model-item>
        <a-form-model-item prop="systemUser">
          <template slot="label">
            管理员
            <a-tooltip v-show="!temp.id">
              <template slot="title"> 管理员拥有：管理服务端的部分权限 </template>
              <a-icon type="question-circle" theme="filled" />
            </a-tooltip>
          </template>
          <a-tooltip title="管理员拥有：管理服务端的部分权限">
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

        <a-form-model-item prop="feature" class="feature jpom-userWorkspace">
          <template slot="label">
            工作空间
            <a-tooltip v-show="!temp.id">
              <template slot="title">
                注意勾选后还需要点击左边按钮移动到待选择区。如果移动按钮不可用表示数据没有任何变化。记得需要构建父级节点再操作奥，取消操作需要挨个选择取消注意观察选择数量
              </template>
              <a-icon type="question-circle" theme="filled" />
            </a-tooltip>
          </template>
          <a-transfer
            :titles="['待选择区', '已选择区']"
            :show-select-all="false"
            :data-source="workspaceList"
            :filter-option="filterOption"
            :target-keys="targetKeys"
            :render="(item) => item.title"
            @change="handleChange"
          >
            <template slot="children" slot-scope="{ props: { direction, selectedKeys }, on: { itemSelect } }">
              <a-tree
                v-if="direction === 'left'"
                blockNode
                checkable
                checkStrictly
                :defaultExpandAll="true"
                :checkedKeys="[...selectedKeys, ...targetKeys]"
                :treeData="treeDataLeft"
                @check="
                  (_, props) => {
                    onCheckedLeft(_, props, [...selectedKeys, ...targetKeys], itemSelect);
                  }
                "
                @select="
                  (_, props) => {
                    onCheckedLeft(_, props, [...selectedKeys, ...targetKeys], itemSelect);
                  }
                "
              >
                <template slot="title" slot-scope="item">
                  <a-tooltip title="注意要把父级一起勾选才生效奥,勾选后还需要点击右边按钮移动到已选择区。如果移动按钮不可用表示数据没有任何变化">
                    {{ item.title }}
                  </a-tooltip>
                </template>
              </a-tree>
              <a-tree
                v-if="direction === 'right'"
                blockNode
                checkable
                checkStrictly
                :defaultExpandAll="true"
                :checkedKeys="[...selectedKeys, ...targetKeys]"
                :treeData="treeDataRight"
                @check="
                  (_, props) => {
                    onCheckedRight(_, props, [...selectedKeys, ...targetKeys], itemSelect);
                  }
                "
                @select="
                  (_, props) => {
                    onCheckedRight(_, props, [...selectedKeys, ...targetKeys], itemSelect);
                  }
                "
              >
                <template slot="title" slot-scope="item">
                  <a-tooltip title="注意勾选后还需要点击左边按钮移动到待选择区。如果移动按钮不可用表示数据没有任何变化">
                    {{ item.title }}
                  </a-tooltip>
                </template>
              </a-tree>
            </template>
          </a-transfer>
        </a-form-model-item>
      </a-form-model>
    </a-modal>
  </div>
</template>
<script>
import {closeUserMfa, deleteUser, editUser, getUserList, unlockUser, workspaceList} from "@/api/user";
import {getWorkSpaceListAll} from "@/api/workspace";
import {getMonitorOperateTypeList} from "@/api/monitor";
import {parseTime} from "@/utils/time";
import sha1 from "sha1";
import {CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY} from "@/utils/const";

function handleTreeData(data, targetKeys = [], left) {
  if (left) {
    data.forEach((item) => {
      item["disabled"] = targetKeys.includes(item.key);
      if (item.children) {
        handleTreeData(item.children, targetKeys, left);
      }
    });
    return data;
  }
  return data
    .filter((item) => {
      return targetKeys.includes(item.key);
    })
    .map((item) => {
      if (item.children) {
        handleTreeData(item.children, targetKeys, left);
      }
      return item;
    });
  // return data;
}

export default {
  data() {
    return {
      loading: false,
      list: [],
      workspaceList: [],
      targetKeys: [],
      methodFeature: [],
      temp: {},
      createOption: true,
      editUserVisible: false,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      columns: [
        { title: "ID", dataIndex: "id", ellipsis: true, scopedSlots: { customRender: "id" } },
        { title: "昵称", dataIndex: "name", ellipsis: true },
        { title: "管理员", dataIndex: "systemUser", align: "center", ellipsis: true, width: 90, scopedSlots: { customRender: "systemUser" } },
        { title: "两步验证", dataIndex: "twoFactorAuthKey", align: "center", ellipsis: true, width: 90, scopedSlots: { customRender: "twoFactorAuthKey" } },

        { title: "邮箱", dataIndex: "email", ellipsis: true, scopedSlots: { customRender: "email" } },
        { title: "创建人", dataIndex: "parent", ellipsis: true, width: 150 },
        {
          title: "修改时间",
          dataIndex: "modifyTimeMillis",
          sorter: true,
          ellipsis: true,
          customRender: (text) => {
            return parseTime(text);
          },
          width: 170,
        },
        { title: "操作", align: "center", dataIndex: "operation", scopedSlots: { customRender: "operation" }, width: 120 },
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
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery);
    },
    treeDataLeft() {
      const str = JSON.stringify(this.workspaceList);
      return handleTreeData(JSON.parse(str), this.targetKeys, true);
    },
    treeDataRight() {
      const str = JSON.stringify(this.workspaceList);
      return handleTreeData(JSON.parse(str), this.targetKeys, false);
    },
  },
  watch: {},
  created() {
    this.loadData();
    this.loadOptTypeData();
  },
  methods: {
    // // 页面引导
    // introGuide() {
    //   this.$store.dispatch("tryOpenGuide", {
    //     key: "user-create",
    //     options: {
    //       hidePrev: true,
    //       steps: [
    //         {
    //           title: "导航助手",
    //           element: document.querySelector(".jpom-userWorkspace"),
    //           intro: "如果这里面没有您想要的工作空间信息，您需要先去添加一个工作空间。选择工作空间后还可以展开选择绑定的权限奥,默认只有查看权限",
    //         },
    //       ],
    //     },
    //   });
    // },
    onCheckedLeft(_, e, checkedKeys, itemSelect) {
      const { eventKey } = e.node;
      const isChecked = checkedKeys.indexOf(eventKey) !== -1;
      itemSelect(eventKey, !isChecked);
    },
    onCheckedRight(_, e, checkedKeys, itemSelect) {
      const { eventKey } = e.node;
      const isChecked = checkedKeys.indexOf(eventKey) !== -1;
      itemSelect(eventKey, isChecked);
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
            const children = this.methodFeature.map((item) => {
              return { key: element.id + "-" + item.value, title: item.title + "权限" };
            });
            children.push({ key: element.id + "-systemUser", title: "节点管理员" });
            children.push({ key: element.id + "-sshCommandNotLimited", title: "SSH 终端命令无限制" });
            this.workspaceList.push({
              key: element.id,
              title: element.name,
              children: children,
            });
          });
        }
      });
    },
    // 加载操作类型数据
    loadOptTypeData() {
      getMonitorOperateTypeList().then((res) => {
        if (res.code === 200) {
          this.methodFeature = res.data.methodFeature;
          // .map((element) => {
          //   return { key: element.value, title: element.title, disabled: false };
          // });
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
      //console.log(targetKeys);
    },
    // 新增用户
    handleAdd() {
      // setTimeout(() => {
      //   this.introGuide();
      // }, 500);

      this.temp = { systemUser: 0 };
      this.createOption = true;
      this.targetKeys = [];
      this.loadWorkSpaceListAll();
      this.editUserVisible = true;
      this.$refs["editUserForm"] && this.$refs["editUserForm"].resetFields();
    },
    // 修改用户
    handleEdit(record) {
      workspaceList(record.id).then((res) => {
        this.createOption = false;
        this.temp = Object.assign({}, record);
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
        //
        const checkSelKey = this.targetKeys.filter((item) => {
          if (!item.includes("-")) {
            return false;
          }
          const temp = item.split("-");
          return !this.targetKeys.includes(temp[0]);
        });
        if (checkSelKey.length) {
          this.$notification.warn({
            message: "存在没有选择父级(工作空间)的权限",
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
    //
    handleCloseMfa(record) {
      this.$confirm({
        title: "系统提示",
        content: "真的关闭当前用户的两步验证么？",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 解锁用户
          closeUserMfa(record.id).then((res) => {
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
    checkTipUserName() {
      if (this.temp?.id === "demo") {
        this.$confirm({
          title: "系统提示",
          content: "demo 账号是系统特定演示使用的账号,系统默认将对 demo 账号限制很多权限。非演示场景不建议使用 demo 账号",
          okText: "确认",
          cancelText: "取消",
          onOk: () => {},
          onCancel: () => {
            this.temp.id = "";
          },
        });
      }
    },
  },
};
</script>
<style scoped>
/* .filter {
  margin-bottom: 10px;
} */
</style>
