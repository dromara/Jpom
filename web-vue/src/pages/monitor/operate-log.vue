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
    <a-modal v-model="editOperateMonitorVisible" width="600px" title="编辑监控" @ok="handleEditOperateMonitorOk" :maskClosable="false">
      <a-form-model ref="editMonitorForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="监控名称" prop="name">
          <a-input v-model="temp.name" placeholder="监控名称"/>
        </a-form-model-item>
        <a-form-model-item label="开启状态" prop="status">
          <a-switch v-model="temp.status" checked-children="开" un-checked-children="关"/>
        </a-form-model-item>
        <a-form-model-item label="监控用户" prop="monitorUser">
          <a-transfer
            :data-source="monitorUserList"
            show-search
            :filter-option="filterOption"
            :target-keys="monitorUserKeys"
            :render="item => item.title"
            @change="handleMonitorUserChange"
          />
        </a-form-model-item>
        <a-form-model-item label="报警联系人" prop="notifyUser">
          <a-transfer
            :data-source="userList"
            show-search
            :filter-option="filterOption"
            :target-keys="notifyUserKeys"
            :render="item => item.title"
            @change="handleNotifyUserChange"
          />
        </a-form-model-item>
      </a-form-model>
    </a-modal>
  </div>
</template>
<script>
import { getMonitorOperateLogList } from '../../api/monitor';
import { getAdminUserList } from '../../api/user';
import { parseTime } from '../../utils/time';
export default {
  data() {
    return {
      loading: false,
      list: [],
      userList: [],
      temp: {},
      notifyUserKeys: [],
      monitorUserKeys: [],
      editOperateMonitorVisible: false,
      columns: [
        {title: '名称', dataIndex: 'name', scopedSlots: {customRender: 'name'}, width: 150},
        {title: '开启状态', dataIndex: 'status', scopedSlots: {customRender: 'status'}, width: 150},
        {title: '创建人', dataIndex: 'parent', scopedSlots: {customRender: 'parent'}, width: 120},
        {title: '修改时间', dataIndex: 'modifyTime', customRender: (text) => {
          if (!text || text === '0') {
            return '';
          }
          return parseTime(text);
        }, width: 180},
        {title: '操作', dataIndex: 'operation', scopedSlots: {customRender: 'operation'}, width: 200}
      ],
      rules: {
        name: [
          { required: true, message: 'Please input monitor name', trigger: 'blur' }
        ],
      }
    }
  },
  computed: {
    monitorUserList() {
      // 深拷贝数组对象，修改属性
      const list = JSON.parse(JSON.stringify(this.userList));
      list.forEach(ele => {
        ele.disabled = false;
      });
      return list;
    }
  },
  created() {
    this.loadData();
    this.loadUserList();
  },
  methods: {
    // 加载数据
    loadData() {
      this.loading = true;
      getMonitorOperateLogList().then(res => {
        if (res.code === 200) {
          this.list = res.data;
        }
        this.loading = false;
      })
    },
    // 加载用户列表
    loadUserList() {
      this.userList = [];
      getAdminUserList().then(res => {
        if (res.code === 200) {
          res.data.forEach(element => {
            this.userList.push({key: element.value, title: element.title, disabled:element.disabled});
          });
        }
      })
    },
    // 新增
    handleAdd() {
      this.loadUserList();
      this.editOperateMonitorVisible = true;
    },
    // 修改
    handleEdit(record) {
      this.loadUserList();
      this.temp = Object.assign(record);
      this.editOperateMonitorVisible = true;
    },
    // 穿梭框筛选
    filterOption(inputValue, option) {
      return option.title.indexOf(inputValue) > -1;
    },
    // 穿梭框 change
    handleNotifyUserChange(targetKeys) {
      this.notifyUserKeys = targetKeys;
    },
    // 穿梭框 change
    handleMonitorUserChange(targetKeys) {
      this.monitorUserKeys = targetKeys;
    },
    // 提交
    handleEditOperateMonitorOk() {

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