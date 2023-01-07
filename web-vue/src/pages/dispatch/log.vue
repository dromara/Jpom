<template>
  <div class="full-content">
    <!-- <div ref="filter" class="filter"> -->
    <!-- <a-button type="primary" @click="handleFilter">刷新</a-button> -->
    <!-- </div> -->
    <!-- 数据表格 -->
    <a-table size="middle" :data-source="list" :columns="columns" :pagination="pagination" @change="changePage" bordered :rowKey="(record, index) => index">
      <template slot="title">
        <a-space>
          <a-select v-model="listQuery.nodeId" allowClear placeholder="请选择节点" class="search-input-item">
            <a-select-option v-for="node in nodeList" :key="node.id">{{ node.name }}</a-select-option>
          </a-select>
          <a-select v-model="listQuery.outGivingId" allowClear placeholder="分发项目" class="search-input-item">
            <a-select-option v-for="dispatch in dispatchList" :key="dispatch.id">{{ dispatch.name }}</a-select-option>
          </a-select>
          <a-select v-model="listQuery.status" allowClear placeholder="请选择状态" class="search-input-item">
            <a-select-option v-for="(item, key) in dispatchStatusMap" :key="key" :value="key">{{ item }}</a-select-option>
          </a-select>
          <a-range-picker class="search-input-item" :show-time="{ format: 'HH:mm:ss' }" format="YYYY-MM-DD HH:mm:ss" @change="onchangeTime" />
          <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
            <a-button :loading="loading" type="primary" @click="loadData">搜索</a-button>
          </a-tooltip>
        </a-space>
      </template>
      <a-tooltip slot="outGivingId" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>

      <a-tooltip slot="nodeName" slot-scope="text, record" placement="topLeft" :title="text">
        <span>{{
          nodeList.filter((item) => item.id === record.nodeId) && nodeList.filter((item) => item.id === record.nodeId)[0] && nodeList.filter((item) => item.id === record.nodeId)[0].name
        }}</span>
      </a-tooltip>
      <a-tooltip slot="projectId" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-tooltip slot="outGivingResultMsg" slot-scope="text, item" placement="topLeft" :title="readJsonStrField(item.result, 'msg')">
        <span>{{ readJsonStrField(item.result, "code") }}-{{ readJsonStrField(item.result, "msg") || item.result }}</span>
      </a-tooltip>
      <a-tooltip slot="outGivingResultTime" slot-scope="text, item" placement="topLeft" :title="readJsonStrField(item.result, 'upload_duration')">
        <span>{{ readJsonStrField(item.result, "upload_duration") }}</span>
      </a-tooltip>
      <a-tooltip slot="outGivingResultSize" slot-scope="text, item" placement="topLeft" :title="readJsonStrField(item.result, 'upload_file_size')">
        {{ readJsonStrField(item.result, "upload_file_size") }}
      </a-tooltip>
      <a-tooltip slot="outGivingResultMsgData" slot-scope="text, item" placement="topLeft" :title="`${readJsonStrField(item.result, 'data')}`">
        {{ readJsonStrField(item.result, "data") }}
      </a-tooltip>
      <a-tooltip slot="status" slot-scope="text">
        <!-- {{ dispatchStatusMap[text] || "未知" }} -->
        <a-tag v-if="text === 2" color="green">{{ dispatchStatusMap[text] || "未知" }}</a-tag>
        <a-tag v-else-if="text === 1 || text === 0 || text === 5" color="orange">{{ dispatchStatusMap[text] || "未知" }}</a-tag>
        <a-tag v-else-if="text === 3 || text === 4 || text === 6" color="red">{{ dispatchStatusMap[text] || "未知" }}</a-tag>
        <a-tag v-else>{{ dispatchStatusMap[text] || "未知" }}</a-tag>
      </a-tooltip>
      <template slot="operation" slot-scope="text, record">
        <a-button type="primary" size="small" @click="handleDetail(record)">详情</a-button>
      </template>
    </a-table>
    <!-- 详情区 -->
    <a-modal destroyOnClose v-model="detailVisible" width="600px" title="详情信息" :footer="null">
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
import { getNodeListAll } from "@/api/node";
import { dispatchStatusMap, getDishPatchListAll, getDishPatchLogList } from "@/api/dispatch";
import { parseTime } from "@/utils/time";

import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, readJsonStrField } from "@/utils/const";

export default {
  data() {
    return {
      loading: false,
      list: [],
      nodeList: [],
      dispatchList: [],
      total: 0,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      dispatchStatusMap: dispatchStatusMap,
      temp: {},
      detailVisible: false,
      detailData: [],
      columns: [
        { title: "分发项目 ID", dataIndex: "outGivingId", ellipsis: true, scopedSlots: { customRender: "outGivingId" } },

        { title: "节点名称", dataIndex: "nodeName", ellipsis: true, scopedSlots: { customRender: "nodeName" } },
        { title: "项目 ID", dataIndex: "projectId", ellipsis: true, scopedSlots: { customRender: "projectId" } },
        { title: "分发结果", dataIndex: "outGivingResultMsg", ellipsis: true, scopedSlots: { customRender: "outGivingResultMsg" } },
        { title: "分发状态消息", dataIndex: "outGivingResultMsgData", ellipsis: true, scopedSlots: { customRender: "outGivingResultMsgData" } },
        { title: "分发耗时", dataIndex: "outGivingResultTime", width: "120px", scopedSlots: { customRender: "outGivingResultTime" } },
        { title: "文件大小", dataIndex: "outGivingResultSize", width: "100px", scopedSlots: { customRender: "outGivingResultSize" } },
        {
          title: "开始时间",
          dataIndex: "startTime",
          customRender: (text) => {
            return parseTime(text);
          },
          sorter: true,
          width: "170px",
        },
        {
          title: "结束时间",
          dataIndex: "endTime",
          sorter: true,
          customRender: (text) => {
            return parseTime(text);
          },
          width: "170px",
        },
        { title: "操作人", dataIndex: "modifyUser", ellipsis: true, scopedSlots: { customRender: "modifyUser" }, width: 120 },
        { title: "状态", dataIndex: "status", width: 100, ellipsis: true, scopedSlots: { customRender: "status" } },
        // { title: "操作", dataIndex: "operation", align: "center", scopedSlots: { customRender: "operation" }, width: "100px" },
      ],
    };
  },
  computed: {
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery);
    },
  },
  created() {
    this.handleFilter();
  },
  methods: {
    readJsonStrField,
    // 搜索
    handleFilter() {
      this.loadNodeList();
      this.loadDispatchList();
      this.loadData();
    },
    // 加载 node
    loadNodeList() {
      getNodeListAll().then((res) => {
        if (res.code === 200) {
          this.nodeList = res.data;
        }
      });
    },
    // 加载分发项目
    loadDispatchList() {
      getDishPatchListAll().then((res) => {
        if (res.code === 200) {
          this.dispatchList = res.data;
        }
      });
    },
    // 加载数据
    loadData(pointerEvent) {
      this.loading = true;
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page;
      getDishPatchLogList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result;
          this.listQuery.total = res.data.total;
        }
        this.loading = false;
      });
    },
    // 选择时间
    onchangeTime(value, dateString) {
      this.listQuery.createTimeMillis = `${dateString[0]} ~ ${dateString[1]}`;
    },
    // 查看详情
    handleDetail(record) {
      this.detailData = [];
      this.detailVisible = true;
      this.temp = Object.assign({}, record);
      this.detailData.push({ title: "分发结果", description: this.temp.result });
    },
    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter });
      this.loadData();
    },
  },
};
</script>
<style scoped></style>
