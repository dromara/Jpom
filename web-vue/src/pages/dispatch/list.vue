<template>
  <div>
    <div ref="filter" class="filter">
      <a-button type="primary" @click="handleLink">添加关联项目</a-button>
      <a-button type="primary" @click="handleAdd">创建分发项目</a-button>
      <a-button type="primary" @click="handleFilter">刷新</a-button>
    </div>
    <!-- 表格 -->
    <a-table :loading="loading" :columns="columns" :data-source="list" bordered rowKey="id" class="node-table"
     :pagination="false">
      <a-tooltip slot="id" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-tooltip slot="name" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <template slot="outGivingProject" slot-scope="text">
        <span v-if="text">独立</span>
        <span v-else>关联</span>
      </template>
      <template slot="operation" slot-scope="text, record">
        <a-button type="primary" @click="handleDispatch(record)">分发文件</a-button>
        <a-button type="primary" @click="handleEdit(record)">编辑</a-button>
        <a-button type="danger" @click="handleDelete(record)">删除</a-button>
      </template>
      <!-- 嵌套表格 -->
      <a-table slot="expandedRowRender" slot-scope="text" :scroll="{x: '80vw'}" :loading="childLoading" :columns="childColumns" :data-source="text.outGivingNodeProjectList"
        :pagination="false" :rowKey="(record, index) => record.nodeId + record.projectId + index">
        <a-tooltip slot="nodeId" slot-scope="text" placement="topLeft" :title="text">
          <span>{{ text }}</span>
        </a-tooltip>
        <a-tooltip slot="projectId" slot-scope="text" placement="topLeft" :title="text">
          <span>{{ text }}</span>
        </a-tooltip>
        <a-switch slot="status" slot-scope="text" :checked="text === 1" checked-children="运行中" un-checked-children="未运行"/>
        <a-tooltip slot="lastOutGivingTime" slot-scope="text" placement="topLeft" :title="text">
          <span>{{ text }}</span>
        </a-tooltip>
      </a-table>
    </a-table>
    <!-- 添加关联项目 -->
    <a-modal v-model="linkDispatchVisible" width="600px" title="编辑关联项目" @ok="handleLinkDispatchOk" :maskClosable="false">
      <a-form-model ref="linkDispatchForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="分发 ID" prop="id">
          <a-input v-model="temp.id" :disabled="temp.type === 'edit'" placeholder="创建之后不能修改"/>
        </a-form-model-item>
        <a-form-model-item label="分发名称" prop="name">
          <a-input v-model="temp.name" placeholder="分发名称"/>
        </a-form-model-item>
        <a-form-model-item label="分发项目" prop="projectId">
          <a-select v-model="temp.projectId" placeholder="请选择需要分发的项目" @select="selectProject">
            <a-select-option v-for="project in projectList" :key="project.id">{{ project.name }}</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="勾选节点" prop="nodeId">
          <a-transfer
            :data-source="nodeList"
            show-search
            :filter-option="filterOption"
            :target-keys="targetKeys"
            :render="item => item.title"
            @change="handleChange"
          />
        </a-form-model-item>
        <a-form-model-item label="分发后操作" prop="afterOpt">
          <a-select v-model="temp.afterOpt" placeholder="请选择发布后操作">
            <a-select-option :key="0">不做任何操作</a-select-option>
            <a-select-option :key="1">并发重启</a-select-option>
            <a-select-option :key="2">完整顺序重启(有重启失败将结束本次)</a-select-option>
            <a-select-option :key="3">顺序重启(有重启失败将继续)</a-select-option>
          </a-select>
        </a-form-model-item>
      </a-form-model>
    </a-modal>
    <!-- 创建分发项目 -->
    <a-modal v-model="editDispatchVisible" width="600px" title="创建分发项目" @ok="handleEditDispatchOk" :maskClosable="false">
      <a-form-model ref="linkDispatchForm" :rules="rules" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="项目 ID" prop="id">
          <a-input v-model="temp.id" :disabled="temp.type === 'edit'" placeholder="创建之后不能修改"/>
        </a-form-model-item>
        <a-form-model-item label="项目名称" prop="name">
          <a-input v-model="temp.name" placeholder="项目名称"/>
        </a-form-model-item>
        <a-form-model-item label="运行方式" prop="runMode">
          <a-select v-model="temp.runMode" placeholder="请选择运行方式">
            <a-select-option v-for="runMode in runModeList" :key="runMode">{{ runMode }}</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="Main Class" prop="mainClass" v-show="temp.runMode !== 'Jar'">
          <a-input v-model="temp.mainClass" placeholder="程序运行的 main 类(jar 模式运行可以不填)"/>
        </a-form-model-item>
        <a-form-model-item label="项目白名单路径" prop="whitelistDirectory">
          <a-select v-model="temp.whitelistDirectory" placeholder="请选择项目白名单路径">
            <a-select-option v-for="access in accessList" :key="access">{{ access }}</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="项目文件夹" prop="lib">
          <a-input v-model="temp.lib" placeholder="项目存储的文件夹，jar 包存放的文件夹"/>
        </a-form-model-item>
        <a-form-model-item label="分发后操作" prop="afterOpt">
          <a-select v-model="temp.afterOpt" placeholder="请选择发布后操作">
            <a-select-option :key="0">不做任何操作</a-select-option>
            <a-select-option :key="1">并发重启</a-select-option>
            <a-select-option :key="2">完整顺序重启(有重启失败将结束本次)</a-select-option>
            <a-select-option :key="3">顺序重启(有重启失败将继续)</a-select-option>
          </a-select>
        </a-form-model-item>
      </a-form-model>
    </a-modal>
  </div>
</template>
<script>
import { getDishPatchList, getReqId, editDispatch, getDispatchWhiteList, deleteDisPatch } from '../../api/dispatch';
import { getNodeProjectList } from '../../api/node'
export default {
  data() {
    return {
      loading: false,
      childLoading: false,
      list: [],
      accessList: [],
      nodeList: [],
      projectList: [],
      nodeProjectMap: {},
      targetKeys: [],
      reqId: '',
      temp: {},
      runModeList: [
        'ClassPath',
        'Jar',
        'JarWar',
        'JavaExtDirsCp',
        // 'File'
      ],
      linkDispatchVisible: false,
      editDispatchVisible: false,
      columns: [
        {title: '分发 ID', dataIndex: 'id', width: 100, ellipsis: true, scopedSlots: {customRender: 'id'}},
        {title: '分发名称', dataIndex: 'name', width: 150, ellipsis: true, scopedSlots: {customRender: 'name'}},
        {title: '类型', dataIndex: 'outGivingProject', width: 100, ellipsis: true, scopedSlots: {customRender: 'outGivingProject'}},
        {title: '操作', dataIndex: 'operation', scopedSlots: {customRender: 'operation'}, width: 300, align: 'left'}
      ],
      childColumns: [
        {title: '节点名称', dataIndex: 'nodeId', width: 100, ellipsis: true, scopedSlots: {customRender: 'nodeId'}},
        {title: '项目名称', dataIndex: 'projectId', width: 120, ellipsis: true, scopedSlots: {customRender: 'projectId'}},
        {title: '项目状态', dataIndex: 'status', width: 150, ellipsis: true, scopedSlots: {customRender: 'status'}},
        {title: '分发状态', dataIndex: 'statusMsg', width: 180},
        {title: '最后分发时间', dataIndex: 'lastOutGivingTime', width: 180, ellipsis: true, scopedSlots: {customRender: 'lastOutGivingTime'}},
        {title: '操作', dataIndex: 'operation', scopedSlots: {customRender: 'operation'}, width: 200, align: 'left'}
      ],
      rules: {
        id: [
          { required: true, message: 'Please input dispatch id', trigger: 'blur' }
        ],
        name: [
          { required: true, message: 'Please input dispatch name', trigger: 'blur' }
        ],
        projectId: [
          { required: true, message: 'Please select project', trigger: 'blur' }
        ],
        runMode: [
          { required: true, message: 'Please select project runMode', trigger: 'blur' }
        ],
        whitelistDirectory: [
          { required: true, message: 'Please select project access path', trigger: 'blur' }
        ],
        lib: [
          { required: true, message: 'Please input project lib', trigger: 'blur' }
        ]
      }
    }
  },
  created() {
    this.handleFilter();
  },
  methods: {
    // 加载数据
    loadData() {
      this.loading = true;
      getDishPatchList().then(res => {
        if (res.code === 200) {
          this.list = res.data;
        }
        this.loading = false;
      })
    },
    // 加载节点项目列表
    loadNodeProjectList() {
      this.nodeList = [];
      this.projectList = [];
      // nodeProjectMap 这个对象用来判断项目对应的节点是否该禁用
      this.nodeProjectMap = {};
      getNodeProjectList().then(res => {
        if (res.code === 200) {
          res.data.forEach(node => {
            const nodeItem = {
              title: node.name,
              key: node.id,
              disabled: true
            }
            node.projects.forEach(project => {
              // 如果项目 ID 存在就不用继续添加
              const index = this.projectList.findIndex(p => p.id === project.id);
              if (index === -1) {
                const projectItem = {
                  name: `${project.name} ( ${project.id} )`,
                  id: project.id
                }
                this.projectList.push(projectItem);
              }
              // 判断对象是否存在
              if (!this.nodeProjectMap[`${project.id}`]) {
                this.nodeProjectMap[`${project.id}`] = [
                  ...this.nodeProjectMap[`${project.id}`] || [],
                  node.id
                ];
              } else {
                const tempIndex = this.nodeProjectMap[`${project.id}`].findIndex(nodeId => node.id === nodeId);
                if (tempIndex === -1) {
                  this.nodeProjectMap[`${project.id}`].push(node.id);
                }
              }
            })
            this.nodeList.push(nodeItem);
          })
        }
      })
    },
    // 获取 reqId
    loadReqId() {
      getReqId().then(res => {
        if (res.code === 200) {
          this.reqId = res.data;
        }
      })
    },
    // 加载项目白名单列表
    loadAccesList() {
      getDispatchWhiteList().then(res => {
        if (res.code === 200) {
          this.accessList = res.data;
        }
      })
    },
    // 筛选
    handleFilter() {
      this.loadData();
      this.loadNodeProjectList();
    },
    // 关联
    handleLink() {
      this.temp = {
        type: 'add'
      };
      this.loadReqId();
      this.linkDispatchVisible = true;
    },
    // 编辑
    handleEdit(record) {
      this.temp = Object.assign({}, record)
      this.temp.type = 'edit';
      this.targetKeys = [];
      // 设置选中的项目和节点
      record.outGivingNodeProjectList.forEach(ele => {
        if (!this.temp.projectId) {
          this.temp.projectId = ele.projectId;
        }
        this.targetKeys.push(ele.nodeId);
      })
      this.loadReqId();
      this.linkDispatchVisible = true;
    },
    // 添加
    handleAdd() {
      this.temp = {
        type: 'add'
      };
      this.loadAccesList();
      this.editDispatchVisible = true;
    },
    // 选择项目
    selectProject(value) {
      this.targetKeys = [];
      this.nodeList.forEach(node => {
        node.disabled = true;
      })
      this.nodeProjectMap[value].forEach(nodeId => {
        this.nodeList.forEach(node => {
          if (node.key === nodeId) {
            node.disabled = false;
          }
        })
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
    // 提交关联项目
    handleLinkDispatchOk() {
      // 检验表单
      this.$refs['linkDispatchForm'].validate((valid) => {
        if (!valid) {
          return false;
        }
        // 检查
        if (this.targetKeys.length < 2) {
          this.$notification.warn({
            message: '请至少选择 2 个节点',
            duration: 2
          });
          return false;
        }
        // 设置 reqId
        this.temp.reqId = this.reqId;
        this.targetKeys.forEach(key => {
          this.temp[`node_${key}`] = this.temp.projectId;
        })
        // 提交
        editDispatch(this.temp).then(res => {
          if (res.code === 200) {
            // 成功
            this.$notification.success({
              message: res.msg,
              duration: 2
            });
            this.targetKeys = [];
            this.$refs['linkDispatchForm'].resetFields();
            this.linkDispatchVisible = false;
            this.handleFilter();
          } else {
            this.targetKeys = [];
          }
        })
      })
    },
    // 提交创建分发项目
    handleEditDispatchOk() {
      // 检验表单
      this.$refs['editDispatchForm'].validate((valid) => {
        if (!valid) {
          return false;
        }
      })
    },
    // 删除
    handleDelete(record) {
      this.$confirm({
        title: '系统提示',
        content: '真的要删除分发信息么？',
        okText: '确认',
        cancelText: '取消',
        onOk: () => {
          // 删除
          deleteDisPatch(record.id).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg,
                duration: 2
              });
              this.handleFilter();
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