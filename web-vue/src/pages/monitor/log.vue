<template>
  <div>
    <div ref="filter" class="filter">
      <a-select v-model="listQuery.selectNode" allowClear placeholder="请选择节点"
        class="filter-item" @change="handleFilter">
        <a-select-option v-for="node in nodeList" :key="node.id">{{ node.name }}</a-select-option>
      </a-select>
      <a-select v-model="listQuery.notifyStatus" allowClear placeholder="请选择通知状态"
        class="filter-item" @change="handleFilter">
        <a-select-option :value="'true'">成功</a-select-option>
        <a-select-option :value="'false'">失败</a-select-option>
      </a-select>
      <a-range-picker class="filter-item" :show-time="{format: 'HH:mm:ss'}" format="YYYY-MM-DD HH:mm:ss" @change="onchangeTime"/>
      <a-button type="primary" @click="handleFilter">搜索</a-button>
      <a-button type="primary" @click="handleFilter">刷新</a-button>
    </div>
    <!-- 数据表格 -->
    <a-table :data-source="list" :loading="loading" :columns="columns"
      :pagination="pagination" :style="{'max-height': tableHeight + 'px' }" :scroll="{x: 790, y: tableHeight - 120}" bordered
      :rowKey="(record, index) => index" @change="change">
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
import { getMonitorLogList } from '../../api/monitor';
import { getNodeList } from '../../api/node';
import { parseTime } from '../../utils/time';
export default {
  data() {
    return {
      loading: false,
      list: [],
      nodeList: [],
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
  computed: {
    // 分页
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
    this.loadData();
    this.loadNodeList();
  },
  methods: {
    // 计算表格高度
    calcTableHeight() {
      this.$nextTick(() => {
        this.tableHeight = window.innerHeight - this.$refs['filter'].clientHeight - 135;
      })
    },
    // 加载 node
    loadNodeList() {
      getNodeList().then(res => {
        if (res.code === 200) {
          this.nodeList = res.data;
        }
      })
    },
    // 加载数据
    loadData() {
      this.loading = true;
      this.listQuery.time = this.timeRange;
      getMonitorLogList(this.listQuery).then(res => {
        if (res.code === 200) {
          this.list = res.data;
          this.total = res.total;
        }
        this.loading = false;
      })
    },
    // 分页、排序、筛选变化时触发
    change(pagination) {
      this.listQuery.page = pagination.current;
      this.listQuery.limit = pagination.pageSize;
      this.loadData();
    },
    // 选择时间
    onchangeTime(value, dateString) {
      this.timeRange = `${dateString[0]} ~ ${dateString[1]}`;
    },
    // 搜索
    handleFilter() {
      this.listQuery.page = 1;
      this.loadData();
    },
    // 查看详情
    handleDetail(record) {
      this.detailData = [];
      this.detailVisible = true;
      this.temp = Object.assign(record);
      this.detailData.push({title: '标题', description: this.temp.title});
      this.detailData.push({title: '内容', description: this.temp.content});
      this.detailData.push({title: '通知对象', description: this.temp.notifyObject});
      if (!this.temp.notifyStatus) {
        this.detailData.push({title: '通知异常', description: this.temp.notifyError});
      }
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