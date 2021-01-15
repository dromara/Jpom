<template>
  <div>
    <div ref="filter" class="filter">
      <a-select v-model="listQuery.nodeId" allowClear placeholder="请选择节点"
        class="filter-item" @change="loadData">
        <a-select-option v-for="node in nodeList" :key="node.id">{{ node.name }}</a-select-option>
      </a-select>
      <a-select v-model="listQuery.outGivingId" allowClear placeholder="分发项目"
        class="filter-item" @change="loadData">
        <a-select-option v-for="dispatch in dispatchList" :key="dispatch.id">{{ dispatch.name }}</a-select-option>
      </a-select>
      <a-select v-model="listQuery.status" allowClear placeholder="请选择通知状态"
        class="filter-item" @change="loadData">
        <a-select-option :value="0">未分发</a-select-option>
        <a-select-option :value="1">分发中</a-select-option>
        <a-select-option :value="2">分发成功</a-select-option>
        <a-select-option :value="3">分发失败</a-select-option>
        <a-select-option :value="4">取消分发</a-select-option>
      </a-select>
      <a-range-picker class="filter-item" :show-time="{format: 'HH:mm:ss'}" format="YYYY-MM-DD HH:mm:ss" @change="onchangeTime"/>
      <a-button type="primary" @click="loadData">搜索</a-button>
      <a-button type="primary" @click="handleFilter">刷新</a-button>
    </div>
    <!-- 数据表格 -->
    <a-table :data-source="list" :loading="loading" :columns="columns"
      :scroll="{x: '80vw', y: tableHeight }" bordered
      :rowKey="(record, index) => index">
      <a-tooltip slot="nodeId" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-tooltip slot="projectId" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <span slot="status" slot-scope="text">{{ text ? '正常' : '异常'  }}</span>
      <template slot="notifyStyle" slot-scope="text">
        <span v-if="text === 0">钉钉</span>
        <span v-else-if="text === 1">邮箱</span>
        <span v-else-if="text === 2">企业微信</span>
        <span v-else>未知</span>
      </template>
      <span slot="notifyStatus" slot-scope="text">{{ text ? '成功' : '失败'  }}</span>
      <template slot="operation" slot-scope="text, record">
        <a-button type="primary" @click="handleDetail(record)">详情</a-button>
      </template>
    </a-table>
  </div>
</template>
<script>
import { getNodeList } from '../../api/node';
import { getDishPatchLogList, getDishPatchList } from  '../../api/dispatch';
import { parseTime } from '../../utils/time';
export default {
  data() {
    return {
      loading: false,
      list: [],
      nodeList: [],
      dispatchList: [],
      total: 0,
      listQuery: {},
      timeRange: '',
      columns: [
        {title: '节点 ID', dataIndex: 'nodeId', width: 100, ellipsis: true, scopedSlots: {customRender: 'nodeId'}},
        {title: '项目 ID', dataIndex: 'projectId', width: 100,  ellipsis: true, scopedSlots: {customRender: 'projectId'}},
        {title: '报警状态', dataIndex: 'status', width: 100, ellipsis: true, scopedSlots: {customRender: 'status'}},
        {title: '报警方式', dataIndex: 'notifyStyle', width: 100, ellipsis: true, scopedSlots: {customRender: 'notifyStyle'}},
        {title: '报警时间', dataIndex: 'createTime', customRender: (text) => {
          return parseTime(text);
        }, width: 180},
        {title: '通知状态', dataIndex: 'notifyStatus', width: 100, ellipsis: true, scopedSlots: {customRender: 'notifyStatus'}},
        {title: '操作', dataIndex: 'operation', scopedSlots: {customRender: 'operation'}, width: 100}
      ]
    }
  },
  created() {
    this.handleFilter();
  },
  methods: {
    // 搜索
    handleFilter() {
      this.loadNodeList();
      this.loadDispatchList();
      this.loadData();
    },
    // 加载 node
    loadNodeList() {
      getNodeList().then(res => {
        if (res.code === 200) {
          this.nodeList = res.data;
        }
      })
    },
    // 加载分发项目
    loadDispatchList() {
      getDishPatchList().then(res => {
        if (res.code === 200) {
          this.dispatchList = res.data;
        }
      })
    },
    // 加载数据
    loadData() {
      this.loading = true;
      this.listQuery.time = this.timeRange;
      getDishPatchLogList(this.listQuery).then(res => {
        if (res.code === 200) {
          this.list = res.data;
        }
        this.loading = false;
      })
    },
    // 选择时间
    onchangeTime(value, dateString) {
      this.timeRange = `${dateString[0]} ~ ${dateString[1]}`;
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
</style>