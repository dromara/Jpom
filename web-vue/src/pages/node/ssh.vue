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
    <a-modal v-model="editSshVisible" width="600px" title="编辑用户" @ok="handleEditRoleOk" :maskClosable="false">
      <a-form-model ref="editSshForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="SSH 名称" prop="name">
          <a-input v-model="temp.name" placeholder="SSH 名称"/>
        </a-form-model-item>
        <a-form-model-item label="Host" prop="host">
          <a-input v-model="temp.host" placeholder="主机 Host"/>
        </a-form-model-item>
        <a-form-model-item label="Port" prop="port">
          <a-input-number v-model="temp.port" :min="1" placeholder="端口号"/>
        </a-form-model-item>
        <a-form-model-item label="认证方式" prop="connectType">
          <a-radio-group v-model="temp.connectType" :options="options" />
        </a-form-model-item>
        <a-form-model-item label="User" prop="user">
          <a-input v-model="temp.user" placeholder="用户"/>
        </a-form-model-item>
        <a-form-model-item v-show="temp.connectType === 'PASS'"  label="Password" prop="password">
          <a-input-password v-model="temp.password" placeholder="密码"/>
        </a-form-model-item>
        <a-form-model-item v-show="temp.connectType === 'PUBKEY'" label="私钥内容" prop="privateKey">
          <a-textarea v-model="temp.privateKey" :auto-size="{ minRows: 3, maxRows: 5 }" placeholder="私钥内容"/>
        </a-form-model-item>
        <!-- <a-form-model-item label="编码格式" prop="charset">
          <a-input v-model="temp.charset" placeholder="编码格式"/>
        </a-form-model-item> -->
        <a-form-model-item label="文件目录" prop="fileDirs">
          <a-textarea v-model="temp.fileDirs" :auto-size="{ minRows: 3, maxRows: 5 }" placeholder="授权可以直接访问的目录，多个回车换行即可"/>
        </a-form-model-item>
      </a-form-model>
    </a-modal>
  </div>
</template>
<script>
import { getSshList, addSsh } from '../../api/ssh';
export default {
  data() {
    return{
      loading: false,
      list: [],
      temp: {},
      editSshVisible: false,
      columns: [
        {title: '名称', dataIndex: 'name'},
        {title: '关联节点', dataIndex: 'nodeId'},
        {title: 'Host', dataIndex: 'host'},
        {title: 'Port', dataIndex: 'port'},
        {title: 'User', dataIndex: 'user'},
        {title: '操作', dataIndex: 'operation', scopedSlots: {customRender: 'operation'}, width: '200px'}
      ],
      options: [
        { label: 'Password', value: 'PASS' },
        { label: 'PubKey', value: 'PUBKEY' }
      ],
      // 表单校验规则
      rules: {
        name: [
          { required: true, message: 'Please input name', trigger: 'blur' }
        ],
        host: [
          { required: true, message: 'Please input host', trigger: 'blur' }
        ],
        port: [
          { required: true, message: 'Please input port', trigger: 'blur' }
        ],
        connectType: [
          { required: true, message: 'Please select connet type', trigger: 'blur' }
        ],
        user: [
          { required: true, message: 'Please input user', trigger: 'blur' }
        ],
        password: [
          { required: true, message: 'Please input password', trigger: 'blur' }
        ],
        privateKey: [
          { required: true, message: 'Please input key', trigger: 'blur' }
        ]
      }
    }
  },
  created() {
    this.loadData()
  },
  methods: {
    // 加载数据
    loadData() {
      this.loading = true;
      getSshList().then(res => {
        if (res.code === 200) {
          this.list = res.data;
        }
        this.loading = false;
      })
    },
    // 新增 SSH
    handleAdd() {
      this.temp = {
        charset: 'UTF-8'
      };
      this.editSshVisible = true;
    },
    // 提交角色数据
    handleEditRoleOk() {
      // 检验表单
      this.$refs['editSshForm'].validate((valid) => {
        if (!valid) {
          return false;
        }
      })
      // 提交数据
      addSsh(this.temp).then(res => {
        if (res.code === 200) {
          this.$notification.success({
            message: res.msg,
            duration: 2
          });
          this.$refs['editSshForm'].resetFields();
          this.editSshVisible = false;
          this.loadData();
        }
      })
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