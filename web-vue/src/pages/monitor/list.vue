<template>
  <div>
    <div ref="filter" class="filter">
      <a-button type="primary" @click="handleAdd">新增</a-button>
      <a-button type="primary" @click="loadData">刷新</a-button>
    </div>
    <!-- 数据表格 -->
    <a-table :data-source="list" :loading="loading" :columns="columns" :scroll="{x: '80vw'}" :pagination="false" bordered :rowKey="(record, index) => index">
      <a-tooltip slot="name" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-switch slot="status" slot-scope="text" :checked="text" checked-children="开启" un-checked-children="关闭"/>
      <a-switch slot="autoRestart" slot-scope="text" :checked="text" checked-children="是" un-checked-children="否"/>
      <a-switch slot="alarm" slot-scope="text" :checked="text" disabled checked-children="报警中" un-checked-children="未报警"/>
      <a-tooltip slot="parent" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <template slot="operation" slot-scope="text, record">
        <a-button type="primary" @click="handleEdit(record)">编辑</a-button>
        <a-button type="danger" @click="handleDelete(record)">删除</a-button>
      </template>
    </a-table>
    <!-- 编辑区 -->
    <a-modal v-model="editMonitorVisible" width="600px" title="编辑监控" @ok="handleEditMonitorOk" :maskClosable="false">
      <a-form-model ref="editMonitorForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="监控名称" prop="name">
          <a-input v-model="temp.name" placeholder="监控名称"/>
        </a-form-model-item>
        <a-form-model-item label="开启状态" prop="status">
          <a-switch v-model="temp.status" checked-children="开" un-checked-children="关"/>
        </a-form-model-item>
        <a-form-model-item label="自动重启" prop="autoRestart">
          <a-switch v-model="temp.autoRestart" checked-children="开" un-checked-children="关"/>
        </a-form-model-item>
        <a-form-model-item label="监控周期" prop="cycle">
          <a-radio-group v-model="temp.cycle" name="cycle" :default-value="1">
            <a-radio :value="1">1 分钟</a-radio>
            <a-radio :value="5">5 分钟</a-radio>
            <a-radio :value="10">10 分钟</a-radio>
            <a-radio :value="30">30 分钟</a-radio>
          </a-radio-group>
        </a-form-model-item>
        <a-form-model-item label="监控项目" prop="projects">
          <a-select v-model="temp.tempProjects" allowClear mode="multiple" @change="handleSelectChange">
            <a-select-opt-group v-for="node in nodeProjectList" :key="node.id">
              <span slot="label">{{node.name}}</span>
              <a-select-option v-for="project in node.projects" :key="project.id" :value="project.id">{{project.name}}</a-select-option>
            </a-select-opt-group>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="报警联系人" prop="notifyUser">
          <a-transfer
            :data-source="userList"
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
import { getMonitorList, editMonitor } from '../../api/monitor';
import { getUserList } from '../../api/user';
import { getNodeProjectList } from '../../api/node';
import { parseTime } from '../../utils/time';
export default {
  data() {
    return {
      loading: false,
      list: [],
      userList: [],
      nodeProjectList: [],
      targetKeys: [],
      // tree 选中的值
      checkedKeys: {},
      temp: {},
      editMonitorVisible: false,
      replaceFields: {
        children: 'projects',
        key: 'id',
        title: 'name'
      },
      columns: [
        {title: '名称', dataIndex: 'name', scopedSlots: {customRender: 'name'}, width: 150},
        {title: '开启状态', dataIndex: 'status', scopedSlots: {customRender: 'status'}, width: 150},
        {title: '自动重启', dataIndex: 'autoRestart', scopedSlots: {customRender: 'autoRestart'}, width: 150},
        {title: '报警状态', dataIndex: 'alarm', scopedSlots: {customRender: 'alarm'}, width: 150},
        {title: '创建人', dataIndex: 'parent', scopedSlots: {customRender: 'parent'}, width: 120},
        {title: '修改时间', dataIndex: 'modifyTime', customRender: (text) => {
          return parseTime(text);
        }, width: 180},
        {title: '操作', dataIndex: 'operation', scopedSlots: {customRender: 'operation'}, width: 200}
      ],
      rules: {

      }
    }
  },
  created() {
    this.loadData();
    this.loadUserList();
    this.loadNodeProjectList();
  },
  methods: {
    // 加载数据
    loadData() {
      this.loading = true;
      const params = {
        nodeId: ''
      }
      getMonitorList(params).then(res => {
        if (res.code === 200) {
          this.list = res.data;
        }
        this.loading = false;
      })
    },
    // 加载用户列表
    loadUserList() {
      getUserList().then(res => {
        if (res.code === 200) {
          res.data.forEach(element => {
            this.userList.push({key: element.id, title: element.name});
          });
        }
      })
    },
    // 加载节点项目列表
    loadNodeProjectList() {
      getNodeProjectList().then(res => {
        if (res.code === 200) {
          this.nodeProjectList = res.data;
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
    // 下拉框选择
    handleSelectChange(value) {
      let projects = [];
      this.nodeProjectList.forEach(node => {
        let tempProjects = [];
        node.projects.forEach(project => {
          if (project.id === JSON.stringify(value)) {
            tempProjects.push(project.id);
          }
        })
        if (tempProjects.length > 0) {
          projects.push({
            node: node.id,
            projects: tempProjects
          })
        }
      })
      this.temp.projects = projects;
    },
    // 新增
    handleAdd() {
      this.editMonitorVisible = true;
    },
    // 修改
    handleEdit(record) {
      this.temp = Object.assign(record);
      this.targetKeys = this.temp.notifyUser;
      // 设置监控项目
      this.temp.tempProjects = [];
      this.temp.projects.forEach(node => {
        node.projects.forEach(project => {
          console.log(project)
          this.temp.tempProjects.push(project);
        })
      })
      console.log(this.temp.tempProjects)
      this.editMonitorVisible = true;
    },
    handleEditMonitorOk() {
      // 检验表单
      this.$refs['editMonitorForm'].validate((valid) => {
        if (!valid) {
          return false;
        }
        const params = {
          ...this.temp,
          projects: JSON.stringify(this.temp.projects),
          notifyUser: JSON.stringify(this.targetKeys)
        }
        editMonitor(params).then(res => {
          if (res.code === 200) {
            // 成功
            this.$notification.success({
              message: res.msg,
              duration: 2
            });
            this.$refs['editMonitorForm'].resetFields();
            this.editMonitorVisible = false;
            this.loadData();
          }
        })
      })
    },
    // 删除
    handleDelete(record) {
      console.log(record);
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