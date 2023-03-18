<template>
  <div class="full-content">
    <a-table
      size="middle"
      :data-source="commandList"
      :columns="columns"
      bordered
      :pagination="pagination"
      @change="
        (pagination, filters, sorter) => {
          this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter });
          this.listData();
        }
      "
      rowKey="id"
    >
      <template slot="title">
        <a-space>
          <a-input v-model="listQuery['%name%']" @pressEnter="listData" placeholder="任务名" class="search-input-item" />
          <a-select show-search option-filter-prop="children" v-model="listQuery.status" allowClear placeholder="状态" class="search-input-item">
            <a-select-option v-for="(val, key) in statusMap" :key="key">{{ val }}</a-select-option>
          </a-select>
          <a-select show-search option-filter-prop="children" v-model="listQuery.taskType" allowClear placeholder="发布类型" class="search-input-item">
            <a-select-option v-for="(val, key) in taskTypeMap" :key="key">{{ val }}</a-select-option>
          </a-select>
          <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
            <a-button type="primary" :loading="loading" @click="listData">搜索</a-button>
          </a-tooltip>
        </a-space>
      </template>
      <a-tooltip slot="tooltip" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>

      <template slot="status" slot-scope="text">
        <span>{{ statusMap[text] || "未知" }}</span>
      </template>
      <template slot="taskType" slot-scope="text">
        <span>{{ taskTypeMap[text] || "未知" }}</span>
      </template>

      <template slot="operation" slot-scope="text, record">
        <a-space>
          <a-button type="primary" size="small" @click="handleView(record)">查看</a-button>
          <!-- <a-button type="primary" size="small" :disabled="!record.hasLog" @click="handleDownload(record)"><a-icon type="download" />日志</a-button> -->
          <a-button type="danger" size="small" @click="handleDelete(record)">删除</a-button>
        </a-space>
      </template>
    </a-table>
    <!-- 任务详情 -->
    <a-drawer
      title="任务详情"
      placement="right"
      :width="'80vw'"
      :visible="detailsVisible"
      @close="
        () => {
          this.detailsVisible = false;
        }
      "
    >
      <task-details v-if="detailsVisible" :taskId="this.temp.id" />
    </a-drawer>
  </div>
</template>

<script>
import { fileReleaseTaskLog, statusMap, taskTypeMap } from "@/api/file-manager/release-task-log";
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from "@/utils/const";
import taskDetails from "./details.vue";

export default {
  components: {
    taskDetails,
  },
  data() {
    return {
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      commandList: [],
      loading: false,
      temp: {},
      statusMap,
      taskTypeMap,
      detailsVisible: false,
      columns: [
        { title: "任务名称", dataIndex: "name", ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "分发类型", dataIndex: "taskType", width: "100px", ellipsis: true, scopedSlots: { customRender: "taskType" } },
        { title: "状态", dataIndex: "status", width: "100px", ellipsis: true, scopedSlots: { customRender: "status" } },

        { title: "状态描述", dataIndex: "statusMsg", ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "文件ID", dataIndex: "fileId", ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "发布目录", dataIndex: "releasePath", width: "100px", ellipsis: true, scopedSlots: { customRender: "tooltip" } },

        {
          title: "任务时间",
          dataIndex: "createTimeMillis",
          sorter: true,
          ellipsis: true,
          customRender: (text) => {
            return parseTime(text);
          },
          width: "170px",
        },
        {
          title: "任务更新时间",
          dataIndex: "modifyTimeMillis",
          sorter: true,
          ellipsis: true,
          customRender: (text) => {
            return parseTime(text);
          },
          width: "170px",
        },
        {
          title: "执行人",
          dataIndex: "modifyUser",
          width: "120px",
          ellipsis: true,
          scopedSlots: { customRender: "modifyUser" },
        },
        { title: "操作", dataIndex: "operation", align: "center", scopedSlots: { customRender: "operation" }, width: "200px" },
      ],
    };
  },
  computed: {
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery);
    },
  },
  mounted() {
    this.listData();
  },
  methods: {
    handleView(row) {
      this.temp = { ...row };
      this.detailsVisible = true;
    },

    // 获取命令数据
    listData(pointerEvent) {
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page;
      this.loading = true;
      fileReleaseTaskLog(this.listQuery).then((res) => {
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
      this.listData();
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
          console.log(row);
        },
      });
    },
  },
};
</script>
<style scoped></style>
