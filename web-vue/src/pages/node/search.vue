<template>
  <div class="full-content">
    <div ref="filter" class="filter">
      <a-input v-model="listQuery['%name%']" placeholder="搜索项目" class="search-input-item" />
      <a-button type="primary" @click="getNodeProjectData">刷新</a-button>
      <span>| </span>
      <a-button type="primary" @click="batchStart">批量启动</a-button>
      <a-button type="primary" @click="batchRestart">批量重启</a-button>
      <a-button type="danger" @click="batchStop">批量关闭</a-button>
      状态数据是异步获取有一定时间延迟
    </div>
    <a-table
      :data-source="projList"
      :columns="columns"
      bordered
      :pagination="pagination"
      @change="changePage"
      :row-selection="{ selectedRowKeys: selectedRowKeys, onChange: onSelectChange }"
      :rowKey="(record, index) => index"
    >
      <a-tooltip slot="name" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>

      <a-switch slot="status" slot-scope="text" :checked="text" disabled checked-children="开" un-checked-children="关" />
      <a-tooltip slot="port" slot-scope="text, record" placement="topLeft" :title="`进程号：${record.pid},  端口号：${record.port}`">
        <span v-if="record.pid">{{ record.port }}/{{ record.pid }}</span>
      </a-tooltip>
      <template slot="operation" slot-scope="text, record">
        <a-button type="primary" @click="handleFile(record)">文件</a-button>
        <a-button type="primary" @click="handleConsole(record)" v-show="record.runMode !== 'File'">控制台</a-button>
      </template>
    </a-table>
    <!-- 项目文件组件 -->
    <a-drawer :title="drawerTitle" placement="right" width="85vw" :visible="drawerFileVisible" @close="onFileClose">
      <file v-if="drawerFileVisible" :nodeId="temp.nodeId" :projectId="temp.projectId" />
    </a-drawer>
    <!-- 项目控制台组件 -->
    <a-drawer :title="drawerTitle" placement="right" width="85vw" :visible="drawerConsoleVisible" @close="onConsoleClose">
      <console v-if="drawerConsoleVisible" :nodeId="temp.nodeId" :projectId="temp.projectId" />
    </a-drawer>
  </div>
</template>
<script>
import { getProjectList } from "@/api/node";
import { restartProject, startProject, stopProject, getRuningProjectInfo } from "@/api/node-project";
import { mapGetters } from "vuex";
import File from "../node/node-layout/project/project-file";
import Console from "../node/node-layout/project/project-console";
import { parseTime, itemGroupBy } from "@/utils/time";
import { PAGE_DEFAULT_LIMIT, PAGE_DEFAULT_SIZW_OPTIONS, PAGE_DEFAULT_SHOW_TOTAL } from "@/utils/const";
export default {
  components: {
    File,
    Console,
  },
  data() {
    return {
      projList: [],

      selectedRowKeys: [],
      listQuery: { page: 1, limit: PAGE_DEFAULT_LIMIT, total: 0 },

      drawerTitle: "",
      temp: {},
      drawerFileVisible: false,
      drawerConsoleVisible: false,
      columns: [
        { title: "项目名称", dataIndex: "name", ellipsis: true, scopedSlots: { customRender: "name" } },
        {
          title: "项目路径",
          dataIndex: "path",
          ellipsis: true,
          customRender: (text, item) => {
            return item.whitelistDirectory + item.lib;
          },
        },
        {
          title: "创建时间",
          dataIndex: "createTimeMillis",

          ellipsis: true,
          customRender: (text) => {
            return parseTime(text);
          },
          width: 170,
        },
        // { title: "修改时间", dataIndex: "modifyTime", width: 160, ellipsis: true, scopedSlots: { customRender: "modifyTime" } },
        {
          title: "最后操作人",
          dataIndex: "modifyUser",
          width: 120,
          ellipsis: true,
          scopedSlots: { customRender: "modifyUser" },
        },
        { title: "运行状态", dataIndex: "status", width: 100, ellipsis: true, scopedSlots: { customRender: "status" } },
        { title: "端口/PID", dataIndex: "port", width: 100, ellipsis: true, scopedSlots: { customRender: "port" } },
        { title: "操作", dataIndex: "operation", scopedSlots: { customRender: "operation" }, width: 240 },
      ],
    };
  },
  computed: {
    ...mapGetters(["getGuideFlag"]),
    filePath() {
      return (this.temp.whitelistDirectory || "") + (this.temp.lib || "");
    },
    pagination() {
      return {
        total: this.listQuery.total || 0,
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
  mounted() {
    this.getNodeProjectData();
  },
  methods: {
    getNodeProjectData() {
      getProjectList(this.listQuery).then((res) => {
        if (res.code === 200) {
          let resultList = res.data.result;

          let tempList = resultList.filter((item) => item.runMode !== "File");
          let fileList = resultList.filter((item) => item.runMode === "File");
          this.projList = tempList.concat(fileList);

          this.listQuery.total = res.data.total;

          let nodeProjects = itemGroupBy(this.projList, "nodeId");
          this.getRuningProjectInfo(nodeProjects, 0);
        }
      });
    },
    getRuningProjectInfo(nodeProjects, i) {
      if (nodeProjects.length <= i) {
        return;
      }
      // console.log(i);
      let data = nodeProjects[i];
      let ids = data.data.map((item) => {
        return item.projectId;
      });
      if (ids.length <= 0) {
        return;
      }
      const tempParams = {
        nodeId: data.type,
        ids: JSON.stringify(ids),
      };
      getRuningProjectInfo(tempParams).then((res2) => {
        if (res2.code === 200) {
          this.projList = this.projList.map((element) => {
            if (res2.data[element.projectId] && element.nodeId === data.type) {
              element.port = res2.data[element.projectId].port;
              element.pid = res2.data[element.projectId].pid;
              element.status = true;
            }
            return element;
          });
        }
        this.getRuningProjectInfo(nodeProjects, i + 1);
      });
    },
    // 文件管理
    handleFile(record) {
      this.temp = Object.assign(record);
      this.drawerTitle = `文件管理(${this.temp.name})`;
      this.drawerFileVisible = true;
    },
    // 关闭文件管理对话框
    onFileClose() {
      this.drawerFileVisible = false;
      this.getNodeProjectData();
    },
    // 控制台
    handleConsole(record) {
      this.temp = Object.assign(record);
      this.drawerTitle = `控制台(${this.temp.name})`;
      this.drawerConsoleVisible = true;
    },
    // 关闭控制台
    onConsoleClose() {
      this.drawerConsoleVisible = false;
      this.getNodeProjectData();
    },
    //选中项目
    onSelectChange(selectedRowKeys) {
      this.selectedRowKeys = selectedRowKeys;
    },
    //批量开始
    batchStart() {
      if (this.selectedRowKeys.length == 0) {
        this.$notification.warning({
          message: "请选中要启动的项目",
          duration: 2,
        });
      }
      this.selectedRowKeys.forEach((value) => {
        if (this.projList[value].status == false && this.projList[value].runMode != "File") {
          const params = {
            nodeId: this.projList[value].nodeId,
            id: this.projList[value].id,
          };
          startProject(params).then(() => {
            this.getNodeProjectData();
          });
        }
      });
    },
    //批量重启
    batchRestart() {
      if (this.selectedRowKeys.length == 0) {
        this.$notification.warning({
          message: "请选中要重启的项目",
          duration: 2,
        });
      }
      console.log(this.selectedRowKeys);
      this.selectedRowKeys.forEach((value) => {
        if (this.projList[value].runMode != "File") {
          const params = {
            nodeId: this.projList[value].nodeId,
            id: this.projList[value].id,
          };
          restartProject(params).then(() => {
            this.getNodeProjectData();
          });
        }
      });
    },
    //批量关闭
    batchStop() {
      if (this.selectedRowKeys.length == 0) {
        this.$notification.warning({
          message: "请选中要关闭的项目",
          duration: 2,
        });
      }
      this.selectedRowKeys.forEach((value) => {
        if (this.projList[value].status == true && this.projList[value].runMode != "File") {
          const params = {
            nodeId: this.projList[value].nodeId,
            id: this.projList[value].id,
          };
          stopProject(params).then(() => {
            this.getNodeProjectData();
          });
        }
      });
    },
    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery.page = pagination.current;
      this.listQuery.limit = pagination.pageSize;
      if (sorter) {
        this.listQuery.order = sorter.order;
        this.listQuery.order_field = sorter.field;
      }
      this.loadData();
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

.btn-add {
  margin-left: 10px;
  margin-right: 0;
}

.replica-area {
  width: 340px;
}

.replica-btn-del {
  position: absolute;
  right: 120px;
  top: 74px;
}

.lib-exist {
  color: #faad14;
}
</style>
