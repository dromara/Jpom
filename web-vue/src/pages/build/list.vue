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
    <a-table :loading="loading" :columns="columns" :data-source="list" bordered rowKey="id" @expand="expand" :pagination="false">
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
    </a-table>
  </div>
</template>
<script>
import { parseTime } from '../../utils/time';
export default {
  data() {
    return {
      loading: false,
      listQuery: {},
      groupList: [],
      list: [],
      temp: {},
      columns: [
        {title: '名称', dataIndex: 'name', width: 150, ellipsis: true, scopedSlots: {customRender: 'name'}},
        {title: '分支', dataIndex: 'branchName', width: 100, ellipsis: true, scopedSlots: {customRender: 'branchName'}},
        {title: '状态', dataIndex: 'status', width: 100, ellipsis: true, scopedSlots: {customRender: 'status'}},
        {title: '构建 ID', dataIndex: 'buildIdStr', width: 150, ellipsis: true, scopedSlots: {customRender: 'buildIdStr'}},
        {title: '修改人', dataIndex: 'modifyUser', width: 100, ellipsis: true, scopedSlots: {customRender: 'modifyUser'}},
        {title: '修改时间', dataIndex: 'modifyTime', customRender: (text) => {
          if (text === '0') {
            return '';
          }
          return parseTime(text);
        }, width: 180},
        {title: '操作', dataIndex: 'operation', scopedSlots: {customRender: 'operation'}, width: '360px', align: 'left'}
      ]
    }
  },
  created() {

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
</style>