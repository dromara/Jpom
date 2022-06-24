<template>
  <div class="full-content">
    <!-- <div ref="filter" class="filter"> -->
    <!-- <a-button type="primary" @click="handleFilter">刷新</a-button> -->
    <!-- </div> -->
    <!-- 数据表格 -->
    <a-table size="middle" :data-source="list" :columns="columns" :pagination="pagination" @change="changePage" bordered :rowKey="(record, index) => index">
      <template slot="title">
        <a-space>
          <a-select
            :getPopupContainer="
              (triggerNode) => {
                return triggerNode.parentNode || document.body;
              }
            "
            v-model="listQuery.nodeId"
            allowClear
            placeholder="请选择节点"
            class="search-input-item"
          >
            <a-select-option v-for="node in nodeList" :key="node.id">{{ node.name }}</a-select-option>
          </a-select>
          <a-select
            :getPopupContainer="
              (triggerNode) => {
                return triggerNode.parentNode || document.body;
              }
            "
            v-model="listQuery.outGivingId"
            allowClear
            placeholder="分发项目"
            class="search-input-item"
          >
            <a-select-option v-for="dispatch in dispatchList" :key="dispatch.id">{{ dispatch.name }}</a-select-option>
          </a-select>
          <a-select
            :getPopupContainer="
              (triggerNode) => {
                return triggerNode.parentNode || document.body;
              }
            "
            v-model="listQuery.status"
            allowClear
            placeholder="请选择状态"
            class="search-input-item"
          >
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
      <a-tooltip slot="nodeId" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-tooltip slot="projectId" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-tooltip slot="status" slot-scope="text">
        {{ dispatchStatusMap[text] || "未知" }}
      </a-tooltip>
      <template slot="operation" slot-scope="text, record">
        <a-button type="primary" size="small" @click="handleDetail(record)">详情</a-button>
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
import {getNodeListAll} from "@/api/node";
import {dispatchStatusMap, getDishPatchListAll, getDishPatchLogList} from "@/api/dispatch";
import {parseTime} from "@/utils/time";

import {CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY} from "@/utils/const";

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
        { title: "节点 ID", dataIndex: "nodeId", ellipsis: true, scopedSlots: { customRender: "nodeId" } },
        { title: "项目 ID", dataIndex: "projectId", ellipsis: true, scopedSlots: { customRender: "projectId" } },
        {
          title: "开始时间",
          dataIndex: "startTime",
          customRender: (text) => {
            return parseTime(text);
          },
          sorter: true,
          width: 170,
        },
        {
          title: "结束时间",
          dataIndex: "endTime",
          sorter: true,
          customRender: (text) => {
            return parseTime(text);
          },
          width: 170,
        },
        { title: "操作人", dataIndex: "modifyUser", ellipsis: true, scopedSlots: { customRender: "modifyUser" }, width: 120 },
        { title: "状态", dataIndex: "status", width: 100, ellipsis: true, scopedSlots: { customRender: "status" } },
        { title: "操作", dataIndex: "operation", align: "center", scopedSlots: { customRender: "operation" }, width: 100 },
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
