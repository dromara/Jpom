<template>
  <div>
    <div class="filter">
      <a-button type="primary" @click="handleAdd">新增</a-button>
    </div>
    <!-- 数据表格 -->
    <a-table :data-source="list" :columns="columns" bordered :rowKey="(record, index) => index">
      <template slot="operation" slot-scope="text, record">
        <a-button type="primary">动态</a-button>
        <a-button type="primary" @click="handleEdit(record)">编辑</a-button>
        <a-button type="danger" @click="handleDelete(record)">删除</a-button>
      </template>
    </a-table>
    <!-- 编辑区 -->
    <a-modal v-model="editUserVisible" width="600px" title="编辑用户" @ok="handleEditRoleOk" :maskClosable="false">
      <a-form-model ref="editRoleForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="登录名称" prop="name">
          <a-input v-model="temp.name" placeholder="创建之后不能修改"/>
        </a-form-model-item>
        <a-form-model-item label="密码" prop="name">
          <a-input-password v-model="temp.name" placeholder="登录密码"/>
        </a-form-model-item>
        <a-form-model-item label="昵称" prop="name">
          <a-input v-model="temp.name" placeholder="昵称"/>
        </a-form-model-item>
        <a-form-model-item label="勾选角色" prop="feature" class="feature">
          <a-transfer
            :data-source="mockData"
            show-search
            :filter-option="filterOption"
            :target-keys="targetKeys"
            :render="item => item.title"
            @change="handleChange"
            @search="handleSearch"
          />
        </a-form-model-item>
      </a-form-model>
    </a-modal>
  </div>
</template>
<script>
import { getUserList } from '../../api/user';
export default {
  data() {
    return {
      list: [],
      temp: {},
      editUserVisible: false,
      columns: [
        {title: '角色名称', dataIndex: 'name'},
        {title: '授权人数', dataIndex: 'bindCount'},
        {title: '修改时间', dataIndex: 'updateTime'},
        {title: '操作', dataIndex: 'operation', scopedSlots: {customRender: 'operation'}}
      ]
    }
  },
  created() {
    this.loadData();
  },
  methods: {
    // 加载数据
    loadData() {
      getUserList().then(res => {
        if (res.code === 200) {
          this.list = res.data;
        }
      })
    },
    // 新增用户
    handleAdd() {
      this.temp = {};
      this.editUserVisible = true;
    }
  }
}
</script>
<style scoped>
.filter {
  margin-bottom: 10px;
}
</style>