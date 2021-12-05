<template>
  <div class="full-content">
    <div ref="filter" class="filter">
      <a-select v-model="listQuery.nodeId" allowClear placeholder="请选择节点" class="filter-item">
        <a-select-option v-for="node in nodeList" :key="node.id">{{ node.name }}</a-select-option>
      </a-select>
      <a-select v-model="listQuery.notifyStatus" allowClear placeholder="请选择通知状态" class="filter-item">
        <a-select-option :value="'true'">成功</a-select-option>
        <a-select-option :value="'false'">失败</a-select-option>
      </a-select>
      <a-range-picker class="filter-item" :show-time="{ format: 'HH:mm:ss' }" format="YYYY-MM-DD HH:mm:ss" @change="onchangeTime" />
      <a-button type="primary" @click="loadData">搜索</a-button>
    </div>
    <!-- 数据表格 -->
    <a-table :data-source="list" :loading="loading" :columns="columns" :pagination="this.pagination" bordered :rowKey="(record, index) => index" @change="change">
      <a-tooltip slot="nodeId" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-tooltip slot="projectId" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <span slot="status" slot-scope="text">{{ text ? "正常" : "异常" }}</span>
      <template slot="notifyStyle" slot-scope="text">
        <span v-if="text === 0">钉钉</span>
        <span v-else-if="text === 1">邮箱</span>
        <span v-else-if="text === 2">企业微信</span>
        <span v-else>未知</span>
      </template>
      <span slot="notifyStatus" slot-scope="text">{{ text ? "成功" : "失败" }}</span>
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
import { getMonitorLogList } from "@/api/monitor";
import { getNodeListAll } from "@/api/node";
import { parseTime } from "@/utils/time";
import { PAGE_DEFAULT_LIMIT, PAGE_DEFAULT_SIZW_OPTIONS, PAGE_DEFAULT_SHOW_TOTAL, PAGE_DEFAULT_LIST_QUERY } from "@/utils/const";
export default {
  data() {
    return {
      loading: false,
      list: [],
      nodeList: [],

      listQuery: PAGE_DEFAULT_LIST_QUERY,

      temp: {},
      detailVisible: false,
      detailData: [],
      columns: [
        { title: "节点 ID", dataIndex: "nodeId", width: 100, ellipsis: true, scopedSlots: { customRender: "nodeId" } },
        { title: "项目 ID", dataIndex: "projectId", width: 100, ellipsis: true, scopedSlots: { customRender: "projectId" } },
        { title: "报警状态", dataIndex: "status", width: 100, ellipsis: true, scopedSlots: { customRender: "status" } },
        { title: "报警方式", dataIndex: "notifyStyle", width: 100, ellipsis: true, scopedSlots: { customRender: "notifyStyle" } },
        {
          title: "报警时间",
          dataIndex: "createTime",
          customRender: (text) => {
            return parseTime(text);
          },
          width: 180,
        },
        { title: "通知状态", dataIndex: "notifyStatus", width: 100, ellipsis: true, scopedSlots: { customRender: "notifyStatus" } },
        { title: "操作", dataIndex: "operation", scopedSlots: { customRender: "operation" }, width: 100 },
      ],
    };
  },
  computed: {
    // 分页
    pagination() {
      return {
        total: this.listQuery.total,
        current: this.listQuery.page || 1,
        pageSize: this.listQuery.limit || PAGE_DEFAULT_LIMIT,
        pageSizeOptions: PAGE_DEFAULT_SIZW_OPTIONS,
        showSizeChanger: true,
        showTotal: (total) => {
          return PAGE_DEFAULT_SHOW_TOTAL(total, this.listQuery);
        },
      };
    },
  },
  created() {
    this.loadData();
    this.loadNodeList();
  },
  methods: {
    // 加载 node
    loadNodeList() {
      getNodeListAll().then((res) => {
        if (res.code === 200) {
          this.nodeList = res.data;
        }
      });
    },
    // 加载数据
    loadData() {
      this.loading = true;

      getMonitorLogList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result;
          this.listQuery.total = res.data.total;
        }
        this.loading = false;
      });
    },
    // 分页、排序、筛选变化时触发
    change(pagination, fl, sorter) {
      this.listQuery.page = pagination.current;
      this.listQuery.limit = pagination.pageSize;
      if (sorter) {
        this.listQuery.order = sorter.order;
        this.listQuery.order_field = sorter.field;
      }

      this.loadData();
    },
    // 选择时间
    onchangeTime(value, dateString) {
      if (dateString[0]) {
        this.listQuery.createTimeMillis = `${dateString[0]} ~ ${dateString[1]}`;
      } else {
        this.listQuery.createTimeMillis = "";
      }
    },

    // 查看详情
    handleDetail(record) {
      this.detailData = [];
      this.detailVisible = true;
      this.temp = Object.assign(record);
      this.detailData.push({ title: "标题", description: this.temp.title });
      this.detailData.push({ title: "内容", description: this.temp.content });
      this.detailData.push({ title: "通知对象", description: this.temp.notifyObject });
      if (!this.temp.notifyStatus) {
        this.detailData.push({ title: "通知异常", description: this.temp.notifyError });
      }
    },
  },
};
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
