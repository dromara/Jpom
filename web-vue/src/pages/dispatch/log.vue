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
      <a-select v-model="listQuery.status" allowClear placeholder="请选择状态"
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
      :pagination="pagination" :style="{'max-height': tableHeight + 'px' }" :scroll="{x: 870, y: tableHeight - 120}" bordered
      :rowKey="(record, index) => index">
      <a-tooltip slot="outGivingId" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-tooltip slot="nodeId" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-tooltip slot="projectId" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <template slot="status" slot-scope="text">
        <span v-if="text === 0">未分发</span>
        <span v-if="text === 1">分发中</span>
        <span v-if="text === 2">分发成功</span>
        <span v-if="text === 3">分发失败</span>
        <span v-if="text === 4">取消分发</span>
      </template>
      <template slot="operation" slot-scope="text, record">
        <a-button type="primary" @click="handleDetail(record)">详情</a-button>
      </template>
    </a-table>
    <!-- 详情区 -->
    <a-modal v-model="detailVisible" width="600px" title="详情信息" :footer="null">
      <a-list item-layout="horizontal" :data-source="detailData">
        <a-list-item slot="renderItem" slot-scope="item">
          <a-list-item-meta :description="item.description">
            <h4 slot="title">{{ item.title }}</h4>
          </a-list-item-meta>
        </a-list-item>
      </a-list>
    </a-modal>
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
      listQuery: {
        page: 1,
        limit: 20
      },
      timeRange: '',
      tableHeight: '70vh',
      temp: {},
      detailVisible: false,
      detailData: [],
      columns: [
        {title: '分发项目 ID', dataIndex: 'outGivingId', width: 100, ellipsis: true, scopedSlots: {customRender: 'outGivingId'}},
        {title: '节点 ID', dataIndex: 'nodeId', width: 100, ellipsis: true, scopedSlots: {customRender: 'nodeId'}},
        {title: '项目 ID', dataIndex: 'projectId', width: 100,  ellipsis: true, scopedSlots: {customRender: 'projectId'}},
        {title: '开始时间', dataIndex: 'startTime', customRender: (text) => {
          return parseTime(text);
        }, width: 180},
        {title: '结束时间', dataIndex: 'endTime', customRender: (text) => {
          return parseTime(text);
        }, width: 180},
        {title: '状态', dataIndex: 'status', width: 100, ellipsis: true, scopedSlots: {customRender: 'status'}},
        {title: '操作', dataIndex: 'operation', scopedSlots: {customRender: 'operation'}, width: 100}
      ]
    }
  },
  computed: {
    pagination() {
      return {
        total: this.total,
        current: this.listQuery.page || 1,
        pageSize: this.listQuery.limit || 10,
        pageSizeOptions: ['10', '20', '50', '100'],
        showSizeChanger: true,
        showTotal: (total) => {
          return `Total ${total} items`;
        }
      }
    }
  },
  created() {
    this.calcTableHeight();
    this.handleFilter();
  },
  methods: {
    // 计算表格高度
    calcTableHeight() {
      this.$nextTick(() => {
        this.tableHeight = window.innerHeight - this.$refs['filter'].clientHeight - 135;
      })
    },
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
      this.listQuery.time = this.timeRange === ' ~ ' ? '' : this.timeRange;
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
    },
    // 查看详情
    handleDetail(record) {
      this.detailData = [];
      this.detailVisible = true;
      this.temp = Object.assign(record);
      this.detailData.push({title: '分发结果', description: this.temp.result});
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