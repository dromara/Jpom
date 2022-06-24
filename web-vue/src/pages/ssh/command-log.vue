<template>
  <div class="full-content">
    <a-table size="middle" :data-source="commandList" :columns="columns" bordered :pagination="pagination" @change="changePage" :rowKey="(record, index) => index">
      <template slot="title">
        <a-space>
          <a-input v-model="listQuery['%commandName%']" @pressEnter="getCommandLogData" placeholder="搜索命令名称" class="search-input-item" />
          <a-input v-model="listQuery['%sshName%']" @pressEnter="getCommandLogData" placeholder="搜索ssh名称" class="search-input-item" />
          <a-select
            :getPopupContainer="
              (triggerNode) => {
                return triggerNode.parentNode || document.body;
              }
            "
            show-search
            option-filter-prop="children"
            v-model="listQuery.status"
            allowClear
            placeholder="状态"
            class="search-input-item"
          >
            <a-select-option v-for="(val, key) in statusMap" :key="key">{{ val }}</a-select-option>
          </a-select>
          <a-select
            :getPopupContainer="
              (triggerNode) => {
                return triggerNode.parentNode || document.body;
              }
            "
            show-search
            option-filter-prop="children"
            v-model="listQuery.triggerExecType"
            allowClear
            placeholder="触发类型"
            class="search-input-item"
          >
            <a-select-option v-for="(val, key) in triggerExecTypeMap" :key="key">{{ val }}</a-select-option>
          </a-select>
          <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
            <a-button type="primary" :loading="loading" @click="getCommandLogData">搜索</a-button>
          </a-tooltip>
        </a-space>
      </template>
      <a-tooltip slot="sshName" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-tooltip slot="commandName" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <template slot="status" slot-scope="text">
        <span>{{ statusMap[text] || "未知" }}</span>
      </template>
      <template slot="triggerExecTypeMap" slot-scope="text">
        <span>{{ triggerExecTypeMap[text] || "未知" }}</span>
      </template>

      <template slot="operation" slot-scope="text, record">
        <a-space>
          <a-button type="primary" size="small" :disabled="!record.hasLog" @click="handleView(record)">查看</a-button>
          <a-button type="primary" size="small" :disabled="!record.hasLog" @click="handleDownload(record)"><a-icon type="download" />日志</a-button>
          <a-button type="danger" size="small" @click="handleDelete(record)">删除</a-button>
        </a-space>
      </template>
    </a-table>
    <!-- 构建日志 -->
    <a-modal :width="'80vw'" v-model="logVisible" title="执行日志" :footer="null" :maskClosable="false">
      <command-log v-if="logVisible" :temp="temp" />
    </a-modal>
  </div>
</template>

<script>
import {deleteCommandLog, downloadLog, getCommandLogList, statusMap, triggerExecTypeMap} from "@/api/command";
import {CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY} from "@/utils/const";
import {parseTime} from "@/utils/time";
import CommandLog from "./command-view-log";

export default {
  components: {
    CommandLog,
  },
  data() {
    return {
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      commandList: [],
      loading: false,
      temp: {},
      statusMap: statusMap,
      triggerExecTypeMap: triggerExecTypeMap,
      logVisible: false,
      columns: [
        { title: "ssh 名称", dataIndex: "sshName", ellipsis: true, scopedSlots: { customRender: "sshName" } },
        { title: "命令名称", dataIndex: "commandName", ellipsis: true, scopedSlots: { customRender: "commandName" } },
        { title: "状态", dataIndex: "status", width: 100, ellipsis: true, scopedSlots: { customRender: "status" } },
        { title: "触发类型", dataIndex: "triggerExecType", width: 100, ellipsis: true, scopedSlots: { customRender: "triggerExecTypeMap" } },
        {
          title: "执行时间",
          dataIndex: "createTimeMillis",
          sorter: true,
          ellipsis: true,
          customRender: (text) => {
            return parseTime(text);
          },
          width: 170,
        },
        {
          title: "结束时间",
          dataIndex: "modifyTimeMillis",
          sorter: true,
          ellipsis: true,
          customRender: (text) => {
            return parseTime(text);
          },
          width: 170,
        },
        {
          title: "执行人",
          dataIndex: "modifyUser",
          width: 120,
          ellipsis: true,
          scopedSlots: { customRender: "modifyUser" },
        },
        { title: "操作", dataIndex: "operation", align: "center", scopedSlots: { customRender: "operation" }, width: 200 },
      ],
    };
  },
  computed: {
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery);
    },
  },
  mounted() {
    this.getCommandLogData();
  },
  methods: {
    handleView(row) {
      this.temp = row;
      this.logVisible = true;
    },

    // 获取命令数据
    getCommandLogData(pointerEvent) {
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page;
      this.loading = true;
      getCommandLogList(this.listQuery).then((res) => {
        if (200 === res.code) {
          this.commandList = res.data.result;
          this.listQuery.total = res.data.total;
        }
        this.loading = false;
      });
    },
    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter });
      this.getCommandLogData();
    },
    //  删除命令
    handleDelete(row) {
      this.$confirm({
        title: "系统提示",
        content: "真的要删除该执行记录吗？",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 删除
          deleteCommandLog(row.id).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg,
              });
              this.getCommandLogData();
            }
          });
        },
      });
    },
    // 下载构建日志
    handleDownload(record) {
      window.open(downloadLog(record.id), "_self");
    },
  },
};
</script>
<style scoped></style>
