<template>
  <div class="full-content">
    <!-- 数据表格 -->
    <a-table size="middle" :data-source="list" :columns="columns" :pagination="pagination" bordered :rowKey="(record, index) => index" @change="change">
      <template slot="title">
        <a-space>
          <a-select
            :getPopupContainer="
              (triggerNode) => {
                return triggerNode.parentNode || document.body;
              }
            "
            show-search
            option-filter-prop="children"
            v-model="listQuery.userId"
            allowClear
            placeholder="请选择操作者"
            class="search-input-item"
          >
            <a-select-option v-for="item in userList" :key="item.id">{{ item.name }}</a-select-option>
          </a-select>
          <a-select
            :getPopupContainer="
              (triggerNode) => {
                return triggerNode.parentNode || document.body;
              }
            "
            show-search
            option-filter-prop="children"
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
            show-search
            option-filter-prop="children"
            v-model="listQuery.classFeature"
            allowClear
            placeholder="操作功能"
            class="search-input-item"
          >
            <a-select-option v-for="item in classFeature" :key="item.value">{{ item.title }}</a-select-option>
          </a-select>
          <a-select
            :getPopupContainer="
              (triggerNode) => {
                return triggerNode.parentNode || document.body;
              }
            "
            show-search
            option-filter-prop="children"
            v-model="listQuery.methodFeature"
            allowClear
            placeholder="操作方法"
            class="search-input-item"
          >
            <a-select-option v-for="item in methodFeature" :key="item.value">{{ item.title }}</a-select-option>
          </a-select>
          <a-range-picker class="search-input-item" :show-time="{ format: 'HH:mm:ss' }" format="YYYY-MM-DD HH:mm:ss" @change="onchangeTime" />
          <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
            <a-button type="primary" :loading="loading" @click="loadData">搜索</a-button>
          </a-tooltip>
        </a-space>
      </template>
      <a-tooltip slot="nodeId" slot-scope="text" placement="topLeft" :title="nodeMap[text]">
        <span>{{ nodeMap[text] }}</span>
      </a-tooltip>
      <a-tooltip slot="classFeature" slot-scope="text" placement="topLeft" :title="classFeatureMap[text]">
        <span>{{ classFeatureMap[text] }}</span>
      </a-tooltip>
      <a-tooltip slot="methodFeature" slot-scope="text" placement="topLeft" :title="methodFeatureMap[text]">
        <span>{{ methodFeatureMap[text] }}</span>
      </a-tooltip>
      <a-tooltip slot="tooltip" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-tooltip slot="optStatus" slot-scope="text" placement="topLeft" :title="`默认状态码为 200 表示执行成功,部分操作状态码可能为 0,状态码为 0 的操作大部分为没有操作结果或者异步执行`">
        <span>{{ text }}</span>
      </a-tooltip>

      <template slot="operation" slot-scope="text, record">
        <a-button size="small" type="primary" @click="handleDetail(record)">详情</a-button>
      </template>
    </a-table>
    <!-- 详情区 -->
    <a-modal v-model="detailVisible" width="600px" title="详情信息" :footer="null">
      <a-list item-layout="horizontal" :data-source="detailData">
        <a-list-item slot="renderItem" slot-scope="item">
          <div v-if="item.json">
            <h4 slot="title">{{ item.title }}</h4>
            <json-viewer :value="item.value" :expand-depth="4" copyable sort></json-viewer>
          </div>
          <a-list-item-meta v-else :description="item.description">
            <h4 slot="title">{{ item.title }}</h4>
          </a-list-item-meta>
        </a-list-item>
      </a-list>
    </a-modal>
  </div>
</template>
<script>
import {getOperationLogList} from "@/api/operation-log";
import {getMonitorOperateTypeList} from "@/api/monitor";
import {getNodeListAll} from "@/api/node";
import {getUserListAll} from "@/api/user";
import {parseTime} from "@/utils/time";
import JsonViewer from "vue-json-viewer";
import {CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY} from "@/utils/const";

export default {
  components: { JsonViewer },
  data() {
    return {
      loading: false,
      list: [],
      nodeList: [],
      nodeMap: {},
      userList: [],
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      methodFeature: [],
      classFeature: [],
      methodFeatureMap: {},
      classFeatureMap: {},
      temp: {},
      detailVisible: false,
      detailData: [],
      columns: [
        { title: "操作者", dataIndex: "userId" },
        { title: "IP", dataIndex: "ip" /*width: 130*/ },
        { title: "节点", dataIndex: "nodeId", width: 120, ellipsis: true, scopedSlots: { customRender: "nodeId" } },
        { title: "数据 ID", dataIndex: "dataId", /*width: 240,*/ ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "操作功能", dataIndex: "classFeature", /*width: 240,*/ ellipsis: true, scopedSlots: { customRender: "classFeature" } },
        { title: "操作方法", dataIndex: "methodFeature", /*width: 240,*/ ellipsis: true, scopedSlots: { customRender: "methodFeature" } },
        { title: "状态码", dataIndex: "optStatus", width: 90, scopedSlots: { customRender: "optStatus" } },
        {
          title: "操作时间",
          dataIndex: "createTimeMillis",
          sorter: true,
          customRender: (text, item) => {
            return parseTime(text || item.optTime);
          },
          width: 160,
        },
        { title: "操作", align: "center", dataIndex: "operation", scopedSlots: { customRender: "operation" }, width: 80 },
      ],
    };
  },
  computed: {
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery);
    },
  },
  created() {
    this.loadData();
    this.loadUserList();
    this.loadNodeList();

    getMonitorOperateTypeList().then((res) => {
      this.methodFeature = res.data.methodFeature;
      this.classFeature = res.data.classFeature;
      res.data.methodFeature.forEach((item) => {
        this.methodFeatureMap[item.value] = item.title;
      });
      res.data.classFeature.forEach((item) => {
        this.classFeatureMap[item.value] = item.title;
      });
    });
  },
  methods: {
    // 加载 node
    loadNodeList() {
      getNodeListAll().then((res) => {
        if (res.code === 200) {
          this.nodeList = res.data;

          res.data.forEach((item) => {
            this.nodeMap[item.id] = item.name;
          });
        }
      });
    },
    // 加载数据
    loadData(pointerEvent) {
      this.loading = true;
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page;
      getOperationLogList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result;
          this.listQuery.total = res.data.total;
        }
        this.loading = false;
      });
    },
    // 分页、排序、筛选变化时触发
    change(pagination, filters, sorter) {
      this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter });
      this.loadData();
    },
    // 加载用户列表
    loadUserList() {
      getUserListAll().then((res) => {
        if (res.code === 200) {
          this.userList = res.data;
        }
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
      try {
        this.temp.reqData = JSON.parse(this.temp.reqData);
      } catch (e) {
        console.error(e);
      }
      try {
        this.temp.resultMsg = JSON.parse(this.temp.resultMsg);
      } catch (e) {
        console.error(e);
      }
      this.detailData.push({ title: "浏览器标识", description: this.temp.userAgent });
      this.detailData.push({ title: "请求参数", json: true, value: this.temp.reqData });
      this.detailData.push({ title: "响应结果", json: true, value: this.temp.resultMsg });
    },
  },
};
</script>
<style scoped></style>
