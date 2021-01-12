<template>
  <div>
    <div ref="filter" class="filter">
      <a-button type="primary" @click="handleLink">添加关联项目</a-button>
      <a-button type="primary" @click="handleAdd">创建分发项目</a-button>
      <a-button type="primary" @click="handleFilter">刷新</a-button>
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
    <!-- 添加关联项目 -->
    <a-modal v-model="linkDispatchVisible" width="600px" title="编辑关联项目" @ok="handleLinkDispatchOk" :maskClosable="false">
      <a-form-model ref="linkDispatchForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="分发 ID" prop="id">
          <a-input v-model="temp.id" placeholder="创建之后不能修改"/>
        </a-form-model-item>
        <a-form-model-item label="分发名称" prop="name">
          <a-input v-model="temp.name" placeholder="分发名称"/>
        </a-form-model-item>
        <a-form-model-item label="分发项目" prop="projectId">
          <a-select v-model="temp.projectId" placeholder="请选择需要分发的项目">
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
        <a-form-model-item label="发布后操作" prop="afterOpt">
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
  </div>
</template>
<script>
import { getDishPatchList } from '../../api/dispatch';
import { getNodeProjectList } from '../../api/node'
export default {
  data() {
    return {
      loading: false,
      childLoading: false,
      list: [],
      nodeList: [],
      projectList: [],
      targetKeys: [],
      temp: {},
      linkDispatchVisible: false,
      editDispatchVisible: false,
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
          { required: true, message: 'Please input dispatch id', trigger: 'blur' }
        ],
        name: [
          { required: true, message: 'Please input dispatch name', trigger: 'blur' }
        ],
        projectId: [
          { required: true, message: 'Please select project', trigger: 'blur' }
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
      getDishPatchList().then(res => {
        if (res.code === 200) {
          this.list = res.data;
        }
      })
    },
    // 加载节点项目列表
    loadNodeProjectList() {
      this.nodeList = [];
      this.projectList = [];
      getNodeProjectList().then(res => {
        if (res.code === 200) {
          res.data.forEach(node => {
            const nodeItem = {
              title: node.name,
              key: node.id
            }
            node.projects.forEach(project => {
              const projectItem = {
                name: project.name,
                id: project.id
              }
              this.projectList.push(projectItem);
            })
            this.nodeList.push(nodeItem)
          })
        }
      })
    },
    // 展开行
    expand(expanded, record) {
      if (expanded) {
        // 请求节点状态数据
        this.childLoading = true;
        console.log(record);
        // getNodeStatus(record.id).then(res => {
        //   if (res.code === 200) {
        //     // const index = this.list.findIndex(ele => ele.id === record.id);
        //     // this.list[index].children = res.data;
        //     record.children = res.data;
        //   }
        //   this.childLoading = false;
        // })
      }
    },
    // 筛选
    handleFilter() {
      this.loadData();
      this.loadNodeProjectList();
    },
    // 关联
    handleLink() {
      this.temp = {};
      this.linkDispatchVisible = true;
    },
    // 添加
    handleAdd() {
      this.temp = {};
      this.editDispatchVisible = true;
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