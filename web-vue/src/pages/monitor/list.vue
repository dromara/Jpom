<template>
  <div>
    <div ref="filter" class="filter">
      <a-button type="primary" @click="handleAdd">新增</a-button>
      <a-button type="primary" @click="loadData">刷新</a-button>
    </div>
    <!-- 数据表格 -->
    <a-table :data-source="list" :loading="loading" :columns="columns" :scroll="{x: '80vw'}" :pagination="false" bordered :rowKey="(record, index) => index">
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
          <a-switch v-model="temp.status" :checked="text" checked-children="开" un-checked-children="关"/>
        </a-form-model-item>
        <a-form-model-item label="自动重启" prop="autoRestart">
          <a-switch v-model="temp.autoRestart" :checked="text" checked-children="开" un-checked-children="关"/>
        </a-form-model-item>
        <a-form-model-item label="监控周期" prop="cycle">
          <a-radio-group v-model="temp.cycle" name="cycle" :default-value="1">
            <a-radio :value="1">1 分钟</a-radio>
            <a-radio :value="5">5 分钟</a-radio>
            <a-radio :value="10">10 分钟</a-radio>
            <a-radio :value="30">30 分钟</a-radio>
          </a-radio-group>
        </a-form-model-item>
      </a-form-model>
    </a-modal>
  </div>
</template>
<script>
import { getMonitorList } from '../../api/monitor';
export default {
  data() {
    return {
      loading: false,
      list: [],
      temp: {},
      editMonitorVisible: false,
      columns: [
        {title: '名称', dataIndex: 'name', width: 150},
        {title: '关联节点', dataIndex: 'nodeId', scopedSlots: {customRender: 'nodeId'}, width: 160},
        {title: 'Host', dataIndex: 'host', width: 150},
        {title: 'Port', dataIndex: 'port', width: 80},
        {title: 'User', dataIndex: 'user', width: 120},
        {title: '操作', dataIndex: 'operation', scopedSlots: {customRender: 'operation'}, width: 330}
      ],
      rules: {

      }
    }
  },
  created() {
    this.loadData();
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
    // 新增
    handleAdd() {
      this.editMonitorVisible = true;
    },
    // 修改
    handleEdit(record) {
      console.log(record);
    },
    handleEditMonitorOk() {

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