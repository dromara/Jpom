<template>
  <div>
    <div ref="filter" class="filter">
      <a-button type="primary" @click="handleAdd">新增</a-button>
      <a-button type="primary" @click="loadData">刷新</a-button>
    </div>
    <a-layout class="layout">
      <!-- 分组树 -->
      <a-layout-sider class="sider">
        <a-empty v-show="groupList.length === 0" />
        <a-tree :load-data="onLoadTreeData" :tree-data="groupList" @select="clickTreeNode" @check="checkTreeNode" defaultExpandAll checkable />
      </a-layout-sider>
      <!-- 表格 -->
      <a-layout-content class="content">
        <a-table :data-source="list" :loading="loading" :scroll="{x: '80vw', y: tableHeight}" :columns="columns" :pagination="false" bordered :rowKey="(record, index) => index">
          <a-tooltip slot="osName" slot-scope="text" placement="topLeft" :title="text">
            <span>{{ text }}</span>
          </a-tooltip>
          <a-tooltip slot="javaVersion" slot-scope="text" placement="topLeft" :title="text">
            <span>{{ text }}</span>
          </a-tooltip>
          <a-tooltip slot="runTime" slot-scope="text" placement="topLeft" :title="text">
            <span>{{ text }}</span>
          </a-tooltip>
          <template slot="operation" slot-scope="text, record">
            <a-button type="primary" @click="handleEdit(record)">编辑</a-button>
            <a-button type="danger" @click="handleDelete(record)">删除</a-button>
          </template>
        </a-table>
      </a-layout-content>
    </a-layout>
    <!-- 编辑区 -->
    <a-modal v-model="editNodeVisible" title="编辑节点" @ok="handleEditRoleOk" :maskClosable="false">
      <a-form-model ref="editNodeForm" :rules="rules" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
        <a-form-model-item label="节点 ID" prop="id">
          <a-input v-model="temp.id" placeholder="创建之后不能修改"/>
        </a-form-model-item>
        <a-form-model-item label="节点名称" prop="name">
          <a-input v-model="temp.name" placeholder="节点名称"/>
        </a-form-model-item>
        <a-form-model-item label="分组名称" prop="group">
          <a-select mode="tags" placeholder="可手动输入" @change="handleSelectChange">
            <a-select-option v-for="item in groupList" :key="item.title">{{ item.title }}</a-select-option>
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
          <a-input-number v-model="temp.timeOut" :min="100" placeholder="超时时间 毫秒" style="width: 100%"/>
        </a-form-model-item>
      </a-form-model>
    </a-modal>
  </div>
</template>
<script>
/* eslint-disable no-unused-vars */
import { getNodeGroupList, getNodeList, getNodeStatus, editNode, deleteNode } from '../../api/node';
export default {
  data() {
    return{
      loading: false,
      listQuery: {},
      groupList: [],
      list: [],
      temp: {},
      editNodeVisible: false,
      tableHeight: '80vh',
      columns: [
        {title: '系统名', dataIndex: 'osName', width: 100, ellipsis: true, scopedSlots: {customRender: 'osName'}},
        {title: 'JDK 版本', dataIndex: 'javaVersion', width: 120, ellipsis: true, scopedSlots: {customRender: 'javaVersion'}},
        {title: 'JVM 总内存', dataIndex: 'totalMemory', width: 150},
        {title: 'JVM 剩余内存', dataIndex: 'freeMemory', width: 180},
        {title: 'Jpom 版本', dataIndex: 'jpomVersion', width: 140},
        {title: 'Java 程序数', dataIndex: 'javaVirtualCount', width: 150},
        {title: '项目数', dataIndex: 'count', width: 100},
        {title: '响应时间', dataIndex: 'timeOut', width: 120},
        {title: '已运行时间', dataIndex: 'runTime', width: 150, ellipsis: true, scopedSlots: {customRender: 'runTime'}},
        {title: '操作', dataIndex: 'operation', scopedSlots: {customRender: 'operation'}, width: '200px', fixed: 'right'}
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
    this.calcTableHeight();
    this.loadData();
  },
  methods: {
    // 计算表格高度
    calcTableHeight() {
      this.$nextTick(() => {
        this.tableHeight = window.innerHeight - this.$refs['filter'].clientHeight - 220;
      })
    },
    // 分组列表
    loadData() {
      this.groupList = [];
      getNodeGroupList().then(res => {
        if (res.code === 200) {
          res.data.forEach(element => {
            this.groupList.push({title: element, key: element});
          });
        }
      })
    },
    // 加载树结构
    onLoadTreeData(treeNode) {
      return new Promise(resolve => {
        if (treeNode.dataRef.children) {
          resolve();
          return;
        }
        setTimeout(() => {
          const params = {group: treeNode.dataRef.title};
          treeNode.dataRef.children = [];
          getNodeList(params).then(res => {
            if (res.code === 200) {
              res.data.forEach(element => {
                treeNode.dataRef.children.push({title: element.name, key: element.id, isLeaf: true, data: element});
              })
              this.groupList = [...this.groupList];
            }
          })
          resolve();
        }, 500);
      })
    },
    // 点击树结点
    clickTreeNode(selectedKeys, {node}) {
      if (node.isLeaf) {
        this.loading = true;
        getNodeStatus(node.dataRef.key).then(res => {
          if (res.code === 200) {
            this.list = res.data
          }
          this.loading = false;
        })
      }
    },
    // 勾选树结点
    checkTreeNode(selectedKeys, {checked, checkedNodes}) {
      if (checked) {
        // 获取选中的叶子节点
        const checkList = checkedNodes.filter(element => element.data.props.dataRef.isLeaf === true);
        if (checkList.length === 0) {
          this.$notification.error({
            message: '请选择叶子节点',
            duration: 2
          });
          return false;
        }
        this.list = [];
        // 获取选中的 leaf 树结点，分别加载数据
        checkedNodes.forEach(element => {
          if (element.data.props.dataRef.isLeaf) {
            this.loading = true;
            getNodeStatus(element.data.props.dataRef.key).then(res => {
              if (res.code === 200) {
                // 拼接结果, id 用来删除
                res.data.forEach(ele => {
                  ele.id = element.data.props.dataRef.data.id
                })
                this.list = this.list.concat(res.data);
              }
              this.loading = false;
            })
          }
        })
      }
    },
    // 处理下拉框
    handleSelectChange(value) {
      if (value.length > 1) {
        this.temp.group = value[value.length - 1];
      }
    },
    // 添加
    handleAdd() {
      this.temp = {
        cycle: 0,
        protocol: 'http',
        openStatus: true
      };
      this.editNodeVisible = true;
    },
    // 修改
    handleEdit(record) {
      this.temp = Object.assign(record);
      this.editNodeVisible = true;
    },
    // 提交角色数据
    handleEditRoleOk() {
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
              const index = this.list.findIndex(element => element.id === record.id);
              this.list.splice(index, 1);
              this.loadNodeGroupList();
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
.filter-item {
  width: 150px;
  margin-right: 10px;
}
.sider {
  background-color: #fff;
  border: 1px solid #e8e8e8;
  height: calc(100vh - 150px);
  overflow-y: auto;
}
.content {
  background-color: #fff;
  padding-left: 10px;
}
</style>