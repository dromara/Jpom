<template>
  <div>
    <div ref="filter" class="filter">
      <a-select v-model="listQuery.group" allowClear placeholder="请选择分组"
        class="filter-item" @change="handleFilter">
        <a-select-option v-for="group in groupList" :key="group">{{ group }}</a-select-option>
      </a-select>
      <a-button type="primary" @click="handleFilter">搜索</a-button>
      <a-button type="primary" @click="handleAdd">新增</a-button>
      <a-button type="primary" @click="loadData">刷新</a-button>
    </div>
    <!-- 表格 -->
    <a-table :loading="loading" :columns="columns" :data-source="list" bordered rowKey="id" class="node-table"
      @expand="expand" :pagination="false">
      <a-tooltip slot="group" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-tooltip slot="url" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <template slot="operation" slot-scope="text, record">
        <a-button type="primary" @click="handleTerminal(record)" :disabled="!record.sshId">终端</a-button>
        <a-button type="primary" @click="handleNode(record)" :disabled="record.openStatus === false">节点管理</a-button>
        <a-button type="primary" @click="handleEdit(record)">编辑</a-button>
        <a-button type="danger" @click="handleDelete(record)">删除</a-button>
      </template>
      <!-- 嵌套表格 -->
      <a-table slot="expandedRowRender" slot-scope="text" :scroll="{x: '80vw'}" :loading="childLoading" :columns="childColumns" :data-source="text.children"
        :pagination="false" :rowKey="(record, index) => record.id + index">
        <a-tooltip slot="osName" slot-scope="text" placement="topLeft" :title="text">
          <span>{{ text }}</span>
        </a-tooltip>
        <a-tooltip slot="javaVersion" slot-scope="text" placement="topLeft" :title="text">
          <span>{{ text }}</span>
        </a-tooltip>
        <a-tooltip slot="runTime" slot-scope="text" placement="topLeft" :title="text">
          <span>{{ text }}</span>
        </a-tooltip>
      </a-table>
    </a-table>
    <!-- 编辑区 -->
    <a-modal v-model="editNodeVisible" title="编辑节点" @ok="handleEditNodeOk" :maskClosable="false">
      <a-form-model ref="editNodeForm" :rules="rules" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
        <a-form-model-item label="节点 ID" prop="id">
          <a-input v-model="temp.id" placeholder="创建之后不能修改"/>
        </a-form-model-item>
        <a-form-model-item label="节点名称" prop="name">
          <a-input v-model="temp.name" placeholder="节点名称"/>
        </a-form-model-item>
        <a-form-model-item label="分组名称" prop="group">
          <a-row>
            <a-col :span="18">
              <a-select v-model="temp.group" placeholder="可手动输入">
                <a-select-option v-for="group in groupList" :key="group">{{ group }}</a-select-option>
              </a-select>
            </a-col>
            <a-col :span="6">
              <a-popover v-model="addGroupvisible" title="添加分组" trigger="click">
                <template slot="content">
                  <a-row>
                    <a-col :span="18">
                      <a-input v-model="temp.tempGroup" placeholder="分组名称"/>
                    </a-col>
                    <a-col :span="6">
                      <a-button type="primary" @click="handleAddGroup">确认</a-button>
                    </a-col>
                  </a-row>
                </template>
                <a-button type="primary" class="btn-add">添加分组</a-button>
              </a-popover>
            </a-col>
          </a-row>
        </a-form-model-item>
        <a-form-model-item label="绑定 SSH " prop="sshId">
          <a-select v-model="temp.sshId" placeholder="节点协议">
            <a-select-option v-for="ssh in sshList" :key="ssh.id" :disabled="ssh.disabled">{{ssh.name}}</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="监控周期" prop="cycle">
          <a-select v-model="temp.cycle" defaultValue="0" placeholder="监控周期">
            <a-select-option :key="0">不开启</a-select-option>
            <a-select-option :key="-30">30 秒</a-select-option>
            <a-select-option :key="1">1 分钟</a-select-option>
            <a-select-option :key="5">5 分钟</a-select-option>
            <a-select-option :key="10">10 分钟</a-select-option>
            <a-select-option :key="30">30 分钟</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="节点协议" prop="protocol">
          <a-select v-model="temp.protocol" defaultValue="http" placeholder="节点协议">
            <a-select-option key="http">HTTP</a-select-option>
            <a-select-option key="htts">HTTPS</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="节点状态" prop="openStatus">
          <a-switch v-model="temp.openStatus" checked-children="启用" un-checked-children="停用" default-checked />
        </a-form-model-item>
        <a-form-model-item label="节点地址" prop="url">
          <a-input v-model="temp.url" placeholder="节点地址 (127.0.0.1:2123)"/>
        </a-form-model-item>
        <a-form-model-item label="节点账号" prop="loginName">
          <a-input v-model="temp.loginName" placeholder="节点账号"/>
        </a-form-model-item>
        <a-form-model-item label="节点密码" prop="loginPwd">
          <a-input-password v-model="temp.loginPwd" placeholder="节点密码"/>
        </a-form-model-item>
        <a-form-model-item label="超时时间" prop="timeOut">
          <a-input-number v-model="temp.timeOut" :min="0" placeholder="毫秒 (值太小可能会取不到节点状态)" style="width: 100%"/>
        </a-form-model-item>
      </a-form-model>
    </a-modal>
    <!-- 管理节点 -->
    <a-drawer :title="drawerTitle" placement="right" width="90vw"
      :visible="drawerVisible" @close="onClose">
      <!-- 节点管理组件 -->
      <node-layout v-if="drawerVisible" :node="temp" />
    </a-drawer>
    <!-- Terminal -->
    <a-modal v-model="terminalVisible" width="50%" title="Terminal" :footer="null" :maskClosable="false">
      <terminal v-if="terminalVisible" :sshId="temp.sshId" :nodeId="temp.id" />
    </a-modal>
  </div>
</template>
<script>
/* eslint-disable no-unused-vars */
import { getNodeGroupList, getNodeList, getNodeStatus, editNode, deleteNode } from '../../api/node';
import { getSshListByNodeId } from '../../api/ssh';
import NodeLayout from './node-layout';
import Terminal from './terminal';
export default {
  components: {
    NodeLayout,
    Terminal
  },
  data() {
    return{
      loading: false,
      childLoading: false,
      listQuery: {},
      groupList: [],
      sshList: [],
      list: [],
      temp: {},
      editNodeVisible: false,
      drawerVisible: false,
      terminalVisible: false,
      addGroupvisible: false,
      drawerTitle: '',
      columns: [
        {title: '节点 ID', dataIndex: 'id', width: 100, ellipsis: true, scopedSlots: {customRender: 'id'}},
        {title: '节点名称', dataIndex: 'name', width: 150, ellipsis: true, scopedSlots: {customRender: 'name'}},
        {title: '分组', dataIndex: 'group', width: 100, ellipsis: true, scopedSlots: {customRender: 'group'}},
        {title: '节点协议', dataIndex: 'protocol', width: 100, ellipsis: true, scopedSlots: {customRender: 'protocol'}},
        {title: '节点地址', dataIndex: 'url', width: 150, ellipsis: true, scopedSlots: {customRender: 'url'}},
        {title: '超时时间', dataIndex: 'timeOut', width: 100, ellipsis: true},
        {title: '操作', dataIndex: 'operation', scopedSlots: {customRender: 'operation'}, width: '360px', align: 'left'}
      ],
      childColumns: [
        {title: '系统名', dataIndex: 'osName', width: 100, ellipsis: true, scopedSlots: {customRender: 'osName'}},
        {title: 'JDK 版本', dataIndex: 'javaVersion', width: 120, ellipsis: true, scopedSlots: {customRender: 'javaVersion'}},
        {title: 'JVM 总内存', dataIndex: 'totalMemory', width: 150},
        {title: 'JVM 剩余内存', dataIndex: 'freeMemory', width: 180},
        {title: 'Jpom 版本', dataIndex: 'jpomVersion', width: 140},
        {title: 'Java 程序数', dataIndex: 'javaVirtualCount', width: 150},
        {title: '项目数', dataIndex: 'count', width: 100},
        {title: '响应时间', dataIndex: 'timeOut', width: 120},
        {title: '已运行时间', dataIndex: 'runTime', width: 150, ellipsis: true, scopedSlots: {customRender: 'runTime'}}
      ],
      rules: {
        id: [
          { required: true, message: 'Please input node id', trigger: 'blur' }
        ],
        name: [
          { required: true, message: 'Please input node name', trigger: 'blur' }
        ],
        url: [
          { required: true, message: 'Please input url', trigger: 'blur' }
        ],
        loginName: [
          { required: true, message: 'Please input login name', trigger: 'blur' }
        ],
        loginPwd: [
          { required: true, message: 'Please input login password', trigger: 'blur' }
        ],
        timeOut: [
          { required: true, message: 'Please input timeout', trigger: 'blur' }
        ],
      }
    }
  },
  created() {
    this.loadGroupList();
    this.handleFilter();
  },
  methods: {
    // 分组列表
    loadGroupList() {
      getNodeGroupList().then(res => {
        if (res.code === 200) {
          this.groupList = res.data;
        }
      })
    },
    // 加载 SSH 列表
    loadSshList() {
      getSshListByNodeId(this.temp.id).then(res => {
        if (res.code === 200) {
          this.sshList = res.data;
        }
      })
    },
    // 加载数据
    loadData() {
      this.list = [];
      this.loading = true;
      getNodeList(this.listQuery).then(res => {
        if (res.code === 200) {
          this.list = res.data;
        }
        this.loading = false;
      })
    },
    // 展开行
    expand(expanded, record) {
      if (expanded) {
        // 请求节点状态数据
        this.childLoading = true;
        getNodeStatus(record.id).then(res => {
          if (res.code === 200) {
            // const index = this.list.findIndex(ele => ele.id === record.id);
            // this.list[index].children = res.data;
            record.children = res.data;
          }
          this.childLoading = false;
        })
      }
    },
    // 筛选
    handleFilter() {
      this.loadData();
    },
    // 添加
    handleAdd() {
      this.temp = {
        type: 'add',
        cycle: 0,
        protocol: 'http',
        openStatus: true
      };
      this.editNodeVisible = true;
    },
    // 进入终端
    handleTerminal(record) {
      this.temp = Object.assign(record);
      this.terminalVisible = true;
    },
    // 修改
    handleEdit(record) {
      this.temp = Object.assign(record);
      this.loadSshList();
      this.temp.tempGroup = '';
      this.editNodeVisible = true;
    },
    // 提交节点数据
    handleEditNodeOk() {
       // 检验表单
      this.$refs['editNodeForm'].validate((valid) => {
        if (!valid) {
          return false;
        }
        // 提交数据
        editNode(this.temp).then(res => {
          if (res.code === 200) {
            // 成功
            this.$notification.success({
              message: res.msg,
              duration: 2
            });
            this.$refs['editNodeForm'].resetFields();
            this.editNodeVisible = false;
            this.loadData();
            this.loadGroupList();
          }
        })
      })
    },
    handleDelete(record) {
      this.$confirm({
        title: '系统提示',
        content: '真的要删除节点么？',
        okText: '确认',
        cancelText: '取消',
        onOk: () => {
          // 删除
          deleteNode(record.id).then((res) => {
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
    // 管理节点
    handleNode(record) {
      this.temp = Object.assign(record);
      this.drawerTitle = `${this.temp.id} (${this.temp.url})`;
      this.drawerVisible = true;
    },
    // 关闭抽屉层
    onClose() {
      this.drawerVisible = false;
    },
    // 添加分组
    handleAddGroup() {
      // 添加到分组列表
      if (this.groupList.indexOf(this.temp.tempGroup) === -1) {
        this.groupList.push(this.temp.tempGroup);
      }
      this.temp.tempGroup = '';
      this.$notification.success({
        message: '添加成功',
        duration: 2
      });
      this.addGroupvisible = false;
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
.filter-item {
  width: 150px;
  margin-right: 10px;
}
.node-table {
  overflow-x: auto;
}
.btn-add {
  margin-left: 10px;
  margin-right: 0;
}
</style>