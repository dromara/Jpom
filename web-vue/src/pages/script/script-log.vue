<template>
  <div class="node-full-content">
    <!-- 数据表格 -->
    <a-table :data-source="list" size="middle" :columns="columns" @change="changePage" :pagination="pagination" bordered rowKey="id">
      <template slot="title">
        <a-space>
          <a-input v-model="listQuery['%scriptName%']" placeholder="名称" @pressEnter="loadData" allowClear class="search-input-item" />
          <a-select show-search option-filter-prop="children" v-model="listQuery.triggerExecType" allowClear placeholder="触发类型" class="search-input-item">
            <a-select-option v-for="(val, key) in triggerExecTypeMap" :key="key">{{ val }}</a-select-option>
          </a-select>
          <a-select show-search option-filter-prop="children" v-model="listQuery.status" allowClear placeholder="状态" class="search-input-item">
            <a-select-option v-for="(val, key) in statusMap" :key="key">{{ val }}</a-select-option>
          </a-select>
          <a-range-picker
            v-model="listQuery['createTimeMillis']"
            allowClear
            inputReadOnly
            class="search-input-item"
            :show-time="{ format: 'HH:mm:ss' }"
            :placeholder="['执行时间开始', '执行时间结束']"
            format="YYYY-MM-DD HH:mm:ss"
            valueFormat="YYYY-MM-DD HH:mm:ss"
          />
          <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
            <a-button type="primary" :loading="loading" @click="loadData">搜索</a-button>
          </a-tooltip>
        </a-space>
      </template>
      <a-tooltip slot="scriptName" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-tooltip slot="modifyUser" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <template slot="triggerExecTypeMap" slot-scope="text">
        <span>{{ triggerExecTypeMap[text] || "未知" }}</span>
      </template>
      <template slot="global" slot-scope="text">
        <a-tag v-if="text === 'GLOBAL'">全局</a-tag>
        <a-tag v-else>工作空间</a-tag>
      </template>
      <a-tooltip slot="createTimeMillis" slot-scope="text, record" :title="`${parseTime(record.createTimeMillis)}`">
        <span>{{ parseTime(record.createTimeMillis) }}</span>
      </a-tooltip>
      <template slot="exitCode" slot-scope="text">
        <a-tag v-if="text == 0" color="green">成功</a-tag>
        <a-tag v-else color="orange">{{ text || "-" }}</a-tag>
      </template>
      <template slot="status" slot-scope="text">
        <span>{{ statusMap[text] || "" }}</span>
      </template>
      <template slot="operation" slot-scope="text, record">
        <a-space>
          <a-button type="primary" size="small" @click="viewLog(record)">查看日志</a-button>

          <a-button type="danger" size="small" @click="handleDelete(record)">删除</a-button>
        </a-space>
      </template>
    </a-table>
    <!-- 日志 -->

    <script-log-view
      v-if="logVisible > 0"
      :visible="logVisible != 0"
      :key="logVisible"
      @close="
        () => {
          logVisible = 0;
        }
      "
      :temp="temp"
    />
  </div>
</template>
<script>
import { getScriptLogList, scriptDel, triggerExecTypeMap } from "@/api/server-script";
import ScriptLogView from "@/pages/script/script-log-view";
import { statusMap } from "@/api/command";
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from "@/utils/const";

export default {
  components: {
    ScriptLogView,
  },
  props: {
    scriptId: {
      type: String,
      default: "",
    },
  },
  data() {
    return {
      loading: false,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      triggerExecTypeMap,
      statusMap,
      list: [],
      temp: {},
      logVisible: 0,
      columns: [
        { title: "名称", dataIndex: "scriptName", width: 100, ellipsis: true, scopedSlots: { customRender: "scriptName" } },
        { title: "执行时间", dataIndex: "createTimeMillis", sorter: true, ellipsis: true, width: "160px", scopedSlots: { customRender: "createTimeMillis" } },
        { title: "触发类型", dataIndex: "triggerExecType", width: 100, ellipsis: true, scopedSlots: { customRender: "triggerExecTypeMap" } },
        { title: "状态", dataIndex: "status", width: 100, ellipsis: true, scopedSlots: { customRender: "status" } },
        { title: "执行域", dataIndex: "workspaceId", ellipsis: true, scopedSlots: { customRender: "global" }, width: "90px" },
        { title: "退出码", dataIndex: "exitCode", width: 100, ellipsis: true, scopedSlots: { customRender: "exitCode" } },
        { title: "执行人", dataIndex: "modifyUser", ellipsis: true, width: "100px", scopedSlots: { customRender: "modifyUser" } },
        { title: "操作", dataIndex: "operation", align: "center", fixed: "right", scopedSlots: { customRender: "operation" }, width: "150px" },
      ],
    };
  },
  computed: {
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery);
    },
  },
  mounted() {
    this.loadData();
  },
  methods: {
    // 加载数据
    loadData(pointerEvent) {
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page;
      this.listQuery.scriptId = this.scriptId;
      this.loading = true;
      getScriptLogList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result;
          this.listQuery.total = res.data.total;
        }
        this.loading = false;
      });
    },
    parseTime,
    viewLog(record) {
      this.logVisible = new Date() * Math.random();
      this.temp = record;
    },
    handleDelete(record) {
      this.$confirm({
        title: "系统提示",
        content: "真的要删除执行记录么？",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 组装参数
          const params = {
            id: record.scriptId,
            executeId: record.id,
          };
          // 删除
          scriptDel(params).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg,
              });
              this.loadData();
            }
          });
        },
      });
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
