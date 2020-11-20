<template>
  <div>
    <div ref="filter" class="filter">
      <a-button type="primary" @click="handleAdd">新增</a-button>
      <a-button type="primary" @click="loadData">刷新</a-button>
    </div>
    <!-- 数据表格 -->
    <a-table :data-source="list" :loading="loading" :columns="columns" :scroll="{x: '80vw', y: 500}" :pagination="false" bordered :rowKey="(record, index) => index">
      <template slot="operation" slot-scope="text, record">
        <span>占个位{{record}}</span>
        <!-- <a-button type="primary" @click="handleEdit(record)">编辑</a-button>
        <a-button type="danger" @click="handleDelete(record)">删除</a-button> -->
      </template>
    </a-table>
  </div>
</template>
<script>
import { getProjectList } from '../../../../api/node-manage';
export default {
  props: {
    node: {
      type: Object
    }
  },
  data() {
    return {
      loading: false,
      list: [],
      columns: [
        {title: '项目名称', dataIndex: 'name', width: 150, ellipsis: true, scopedSlots: {customRender: 'name'}},
        {title: '创建时间', dataIndex: 'createTime', width: 170, ellipsis: true, scopedSlots: {customRender: 'createTime'}},
        {title: '修改时间', dataIndex: 'modifyTime', width: 170, ellipsis: true, scopedSlots: {customRender: 'modifyTime'}},
        {title: '最后操作人', dataIndex: 'modifyUser', width: 150, ellipsis: true, scopedSlots: {customRender: 'modifyUser'}},
        {title: '运行状态', dataIndex: 'status', width: 100, ellipsis: true, scopedSlots: {customRender: 'status'}},
        {title: '端口', dataIndex: 'port', width: 100, ellipsis: true, scopedSlots: {customRender: 'port'}},
        {title: '操作', dataIndex: 'operation', scopedSlots: {customRender: 'operation'}, width: 200}
      ]
    }
  },
  created() {
    this.loadData()
  },
  methods: {
    // 加载数据
    loadData() {
      this.loading = true;
      const params = {
        nodeId: this.node.id
      }
      getProjectList(params).then(res => {
        if (res.code === 200) {
          this.list = res.data;
        }
        this.loading = false;
      })
    },
    // 添加
    handleAdd() {
    },
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