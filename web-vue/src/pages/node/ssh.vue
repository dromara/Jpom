<template>
  <div>
    <div class="filter">
      <a-button type="primary" @click="handleAdd">新增</a-button>
      <a-button type="primary" @click="loadData">刷新</a-button>
    </div>
    <!-- 数据表格 -->
    <a-table :data-source="list" :loading="loading" :columns="columns" :pagination="false" bordered :rowKey="(record, index) => index">
      <template slot="nodeId" slot-scope="text, record">
        <a-button v-if="!record.nodeModel" type="primary" @click="install(record)">安装节点</a-button>
        <span v-else>......</span>
      </template>
      <template slot="operation" slot-scope="text, record">
        <a-button type="primary" @click="handleEdit(record)">编辑</a-button>
        <a-button type="primary" @click="handleTerminal(record)">终端</a-button>
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
        <!-- 新增时需要填写 -->
        <a-form-model-item v-show="temp.connectType === 'PASS' && temp.type === 'add'"  label="Password" prop="password">
          <a-input-password v-model="temp.password" placeholder="密码"/>
        </a-form-model-item>
        <!-- 修改时可以不填写 -->
        <a-form-model-item v-show="temp.connectType === 'PASS' && temp.type === 'edit'"  label="Password" prop="password-update">
          <a-input-password v-model="temp.password" placeholder="密码若没修改可以不用填写"/>
        </a-form-model-item>
        <a-form-model-item v-if="temp.connectType === 'PUBKEY'" label="私钥内容" prop="privateKey">
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
    <!-- 安装节点 -->
    <a-modal v-model="nodeVisible" width="600px" title="安装节点" @ok="handleEditNodeOk" :maskClosable="false">
      <a-form-model ref="nodeForm" :rules="rules" :model="tempNode" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="节点 ID" prop="id">
          <a-input v-model="tempNode.id" placeholder="节点 ID"/>
        </a-form-model-item>
        <a-form-model-item label="节点名称" prop="name">
          <a-input v-model="tempNode.name" placeholder="节点名称"/>
        </a-form-model-item>
        <a-form-model-item label="节点协议" prop="protocol">
          <a-select v-model="tempNode.protocol" defaultValue="http" placeholder="节点协议">
            <a-select-option key="http">HTTP</a-select-option>
            <a-select-option key="https">HTTPS</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="节点地址" prop="url">
          <a-input v-model="tempNode.url" placeholder="节点地址 (127.0.0.1:2123)"/>
        </a-form-model-item>
        <a-form-model-item label="安装路径" prop="path">
          <a-input v-model="tempNode.path" placeholder="安装路径"/>
        </a-form-model-item>
        <a-form-model-item label="安装文件">
          <div class="clearfix">
            <a-upload :file-list="fileList" :remove="handleRemove" :before-upload="beforeUpload" accept=".zip">
              <a-button><a-icon type="upload" />选择文件</a-button>
            </a-upload>
          </div>
        </a-form-model-item>
        <template slot="footer">
          <a-button key="back" @click="nodeVisible = false">Cancel</a-button>
          <a-button key="submit" type="primary">Ok</a-button>
        </template>
      </a-form-model>
    </a-modal>
  </div>
</template>
<script>
import { getSshList, editSsh, deleteSsh, installAgentNode } from '../../api/ssh';
export default {
  data() {
    return{
      loading: false,
      list: [],
      temp: {},
      editSshVisible: false,
      nodeVisible: false,
      tempNode: {},
      fileList: [],
      columns: [
        {title: '名称', dataIndex: 'name'},
        {title: '关联节点', dataIndex: 'nodeId', scopedSlots: {customRender: 'nodeId'}},
        {title: 'Host', dataIndex: 'host'},
        {title: 'Port', dataIndex: 'port'},
        {title: 'User', dataIndex: 'user'},
        {title: '操作', dataIndex: 'operation', scopedSlots: {customRender: 'operation'}, width: 300}
      ],
      options: [
        { label: 'Password', value: 'PASS' },
        { label: 'PubKey', value: 'PUBKEY' }
      ],
      // 表单校验规则
      rules: {
        id: [
          { required: true, message: 'Please input id', trigger: 'blur' }
        ],
        name: [
          { required: true, message: 'Please input name', trigger: 'blur' }
        ],
        host: [
          { required: true, message: 'Please input host', trigger: 'blur' }
        ],
        port: [
          { required: true, message: 'Please input port', trigger: 'blur' }
        ],
        protocol: [
          { required: true, message: 'Please input protocol', trigger: 'blur' }
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
        ],
        url: [
          { required: true, message: 'Please input url', trigger: 'blur' }
        ],
        path: [
          { required: true, message: 'Please input path', trigger: 'blur' }
        ],
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
        type: 'add',
        charset: 'UTF-8',
        port: 22,
        connectType: 'PASS'
      };
      this.editSshVisible = true;
    },
    // 修改
    handleEdit(record) {
      console.log(record)
      this.temp = Object.assign(record);
      this.temp.type = 'edit';
      this.editSshVisible = true;
    },
    // 提交角色数据
    handleEditRoleOk() {
      // 检验表单
      this.$refs['editSshForm'].validate((valid) => {
        if (!valid) {
          return false;
        }
        // 提交数据
        editSsh(this.temp).then(res => {
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
      })
    },
    // 进入终端
    handleTerminal(record) {
      console.log(record);
    },
    // 删除
    handleDelete(record) {
      this.$confirm({
        title: '系统提示',
        content: '真的要删除 SSH 么？',
        okText: '确认',
        cancelText: '取消',
        onOk: () => {
          // 删除
          deleteSsh(record.id).then((res) => {
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
    },
    // 安装节点
    install(record) {
      this.temp = Object.assign(record);
      this.tempNode = {
        url: `${this.temp.host}:2123`,
        protocol: 'http'
      }
      this.nodeVisible = true;
    },
    // 处理文件移除
    handleRemove(file) {
      const index = this.fileList.indexOf(file);
      const newFileList = this.fileList.slice();
      newFileList.splice(index, 1);
      this.fileList = newFileList;
    },
    // 准备上传文件
    beforeUpload(file) {
      // 只允许上传单个文件
      this.fileList = [file];
      return false;
    },
    // 提交节点数据
    handleEditNodeOk() {
      // 检验表单
      this.$refs['nodeForm'].validate((valid) => {
        if (!valid) {
          return false;
        }
        // 检测文件是否选择了
        if (this.fileList.length === 0) {
          this.$notification.error({
            message: '请选择 zip 文件',
            duration: 2
          });
          return false;
        }
        const formData = new FormData();
        formData.append('file', this.fileList[0]);
        formData.append('id', this.tempNode.id);
        formData.append('nodeData', JSON.stringify({...this.tempNode}));
        formData.append('path', this.tempNode.path);
        // 提交数据
        installAgentNode(formData).then(res => {
          if(res.code === 200) {
            console.log(res.data)
            this.$refs['nodeForm'].resetFields();
            this.nodeVisible = false;
          }
        })
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