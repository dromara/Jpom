<template>
  <div>
    <div class="filter">
      <a-button type="primary" @click="handleAdd">新增</a-button>
      <a-button type="primary" @click="loadData">刷新</a-button>
    </div>
    <!-- 数据表格 -->
    <a-table :data-source="list" :loading="loading" :columns="columns" :pagination="false" bordered :rowKey="(record, index) => index">
      <template slot="operation" slot-scope="text, record">
        <a-button type="primary" @click="handleEdit(record)">编辑</a-button>
        <a-button type="danger" @click="handleDelete(record)">删除</a-button>
      </template>
    </a-table>
    <!-- 编辑区 -->
    <a-modal v-model="editUserVisible" width="600px" title="编辑用户" @ok="handleEditUserOk" :maskClosable="false">
      <a-form-model ref="editUserForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="登录名称" prop="id">
          <a-input v-model="temp.id" placeholder="创建之后不能修改"/>
        </a-form-model-item>
        <a-form-model-item label="密码" :prop="createOption ? 'password' : 'none'">
          <a-input-password v-model="temp.password" :placeholder="createOption ? '登录密码' : '如果不修改密码则不用填写'"/>
        </a-form-model-item>
        <a-form-model-item label="昵称" prop="name">
          <a-input v-model="temp.name" placeholder="昵称"/>
        </a-form-model-item>
        <a-form-model-item label="勾选角色" prop="feature" class="feature jpom-role">
          <a-transfer
            :data-source="roleList"
            show-search
            :filter-option="filterOption"
            :target-keys="targetKeys"
            :render="item => item.title"
            @change="handleChange"
          />
        </a-form-model-item>
      </a-form-model>
    </a-modal>
  </div>
</template>
<script>
import { mapGetters } from 'vuex';
import { getUserList, addUser, updateUser, deleteUser } from '../../api/user';
import { getRoleList } from '../../api/role';
import { parseTime } from '../../utils/time';
import sha1 from 'sha1';
export default {
  data() {
    return {
      loading: false,
      list: [],
      roleList: [],
      targetKeys: [],
      temp: {},
      createOption: true,
      editUserVisible: false,
      columns: [
        {title: 'ID', dataIndex: 'id'},
        {title: '昵称', dataIndex: 'name'},
        {title: '邮箱', dataIndex: 'email'},
        {title: '创建人', dataIndex: 'parent'},
        {title: '修改时间', dataIndex: 'modifyTime', customRender: (text) => {
          return parseTime(text);
        }},
        {title: '操作', dataIndex: 'operation', scopedSlots: {customRender: 'operation'}, width: '200px'}
      ],
      // 表单校验规则
      rules: {
        id: [
          { required: true, message: 'Please input login name', trigger: 'blur' }
        ],
        name: [
          { required: true, message: 'Please input nickName', trigger: 'blur' }
        ],
        password: [
          { required: true, message: 'Please input password', trigger: 'blur' }
        ],
      }
    }
  },
  computed: {
    ...mapGetters([
      'getGuideFlag'
    ])
  },
  watch: {
    getGuideFlag() {
      this.introGuide();
    }
  },
  created() {
    this.loadData();
    this.loadRoleList();
  },
  methods: {
    // 页面引导
    introGuide() {
      if (this.getGuideFlag) {
        this.$introJs().setOptions({
          hidePrev: true,
          steps: [{
            title: 'Jpom 导航助手',
            element: document.querySelector('.jpom-role'),
            intro: '如果这里面没有你想要的角色信息，你需要先去添加一个角色。'
          }]
        }).start();
        return false;
      }
      this.$introJs().exit();
    },
    // 加载数据
    loadData() {
      this.loading = true;
      getUserList().then(res => {
        if (res.code === 200) {
          this.list = res.data;
        }
        this.loading = false;
      })
    },
    // 加载角色数据
    loadRoleList() {
      this.roleList = [];
      getRoleList().then(res => {
        if (res.code === 200) {
          res.data.forEach(element => {
            this.roleList.push({key: element.id, title: element.name});
          });
        }
      })
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
      this.loadRoleList();
      this.editUserVisible = true;
      this.$nextTick(() => {
        setTimeout(() => {
          this.introGuide();
        }, 500);
      })
    },
    // 修改用户
    handleEdit(record) {
      this.createOption = false;
      this.temp = Object.assign(record);
      // 设置选中 key
      this.targetKeys = record.roles;
      this.loadRoleList();
      this.editUserVisible = true;
    },
    // 提交用户数据
    handleEditUserOk() {
      // 检验表单
      this.$refs['editUserForm'].validate((valid) => {
        if (!valid) {
          return false;
        }
        // 判断是否选择了权限
        if (this.targetKeys.length === 0) {
          this.$notification.error({
            message: '请选择权限',
            duration: 2
          });
          return false;
        }
        // 加密密码
        const paramsTemp =  Object.assign({},this.temp);
        if (paramsTemp.password) {
          paramsTemp.password = sha1(this.temp.password);
        }
        // 设置选择的角色
        paramsTemp.roles = JSON.stringify(this.targetKeys);
        // 需要判断当前操作是【新增】还是【修改】
        if (this.createOption) {
          // 新增
          addUser(paramsTemp).then(res => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg,
                duration: 2
              });
              this.$refs['editUserForm'].resetFields();
              this.editUserVisible = false;
              this.loadData();
            }
          })
        } else {
          // 修改
          updateUser(paramsTemp).then(res => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg,
                duration: 2
              });
              this.$refs['editUserForm'].resetFields();
              this.editUserVisible = false;
              this.loadData();
            }
          })
        }
      })
    },
    // 删除用户
    handleDelete(record) {
      this.$confirm({
        title: '系统提示',
        content: '真的要删除用户么？',
        okText: '确认',
        cancelText: '取消',
        onOk: () => {
          // 删除
          deleteUser(record.id).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg,
                duration: 2
              });
              this.loadData();
            }
          })
        }
      });
    }
  }
}
</script>
<style scoped>
.filter {
  margin-bottom: 10px;
}
.ant-btn {
  margin-right: 10px;
}
</style>