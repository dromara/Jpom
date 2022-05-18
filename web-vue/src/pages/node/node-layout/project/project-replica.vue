<template>
  <div class="node-full-content">
    <div ref="filter" class="filter">
      <a-button type="primary" @click="handleFilter">刷新</a-button>
    </div>
    <!-- 表格 -->
    <a-table :data-source="list" :loading="loading" :columns="columns" :pagination="false" bordered :rowKey="(record, index) => index">
      <a-switch slot="status" slot-scope="text" :checked="text" disabled checked-children="开" un-checked-children="关" />
      <template slot="operation" slot-scope="text, record">
        <a-space>
          <a-button type="primary" @click="handleConsole(record)">控制台</a-button>
          <a-button type="primary" :disabled="!record.status" @click="handleMonitor(record)">监控</a-button>
          <a-button type="danger" @click="handleDelete(record)">删除</a-button>
        </a-space>
      </template>
    </a-table>
    <!-- 项目控制台组件 -->
    <a-drawer :title="drawerTitle" placement="right" width="85vw" :visible="drawerConsoleVisible" @close="onConsoleClose">
      <console v-if="drawerConsoleVisible" :nodeId="node.id" :id="project.id" :projectId="project.projectId" :replica="temp" :copyId="temp.id" />
    </a-drawer>
    <!-- 项目监控组件 -->
    <a-drawer :title="drawerTitle" placement="right" width="85vw" :visible="drawerMonitorVisible" @close="onMonitorClose">
      <monitor v-if="drawerMonitorVisible" :node="node" :project="project" :replica="temp" :copyId="temp.id" />
    </a-drawer>
  </div>
</template>
<script>
import Console from "./project-console";
import Monitor from "./project-monitor";
import { getProjectReplicaList, deleteProject, getRuningProjectCopyInfo } from "@/api/node-project";
export default {
  props: {
    node: {
      type: Object,
    },
    project: {
      type: Object,
    },
  },
  components: {
    Console,
    Monitor,
  },
  data() {
    return {
      loading: false,
      list: [],
      temp: {},
      drawerTitle: "",
      drawerConsoleVisible: false,
      drawerMonitorVisible: false,
      columns: [
        { title: "副本编号", dataIndex: "id", width: 150, ellipsis: true, scopedSlots: { customRender: "id" } },
        { title: "状态", dataIndex: "status", width: 100, ellipsis: true, scopedSlots: { customRender: "status" } },
        { title: "进程 ID", dataIndex: "pid", width: 100, ellipsis: true, scopedSlots: { customRender: "pid" } },
        { title: "端口号", dataIndex: "port", width: 100, ellipsis: true, scopedSlots: { customRender: "port" } },
        { title: "最后修改时间", dataIndex: "modifyTime", width: 180, ellipsis: true, scopedSlots: { customRender: "modifyTime" } },
        { title: "操作", dataIndex: "operation", scopedSlots: { customRender: "operation" }, width: 220 },
      ],
    };
  },
  mounted() {
    this.handleFilter();
  },
  methods: {
    // 加载数据
    loadData() {
      this.loading = true;
      this.list = [];
      const params = {
        nodeId: this.node.id,
        id: this.project.projectId,
      };
      getProjectReplicaList(params).then((res) => {
        if (res.code === 200) {
          this.list = res.data;
          this.getRuningProjectCopyInfo();
        }
        this.loading = false;
      });
    },
    getRuningProjectCopyInfo() {
      const ids = this.list.map((item) => item.id);
      const tempParams = {
        nodeId: this.node.id,
        id: this.project.projectId,
        copyIds: JSON.stringify(ids),
      };

      getRuningProjectCopyInfo(tempParams).then((res) => {
        if (res.code === 200) {
          this.list = this.list.map((element) => {
            if (res.data[element.id]) {
              element.port = res.data[element.id].port;
              element.pid = res.data[element.id].pid;
              element.status = true;
            }
            return element;
          });
        }
      });
    },
    // 筛选
    handleFilter() {
      this.loadData();
    },
    // 控制台
    handleConsole(record) {
      this.temp = Object.assign({}, record);
      this.drawerTitle = `控制台(${this.temp.tagId})`;
      this.drawerConsoleVisible = true;
    },
    // 关闭控制台
    onConsoleClose() {
      this.drawerConsoleVisible = false;
      this.handleFilter();
    },
    // 监控
    handleMonitor(record) {
      this.temp = Object.assign({}, record);
      this.drawerTitle = `监控(${this.temp.tagId})`;
      this.drawerMonitorVisible = true;
    },
    // 关闭监控
    onMonitorClose() {
      this.drawerMonitorVisible = false;
    },
    // 删除
    handleDelete(record) {
      this.$confirm({
        title: "系统提示",
        content: "真的要删除副本项目么？",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 删除
          const params = {
            nodeId: this.node.id,
            id: this.project.projectId,
            copyId: record.id,
          };
          deleteProject(params).then((res) => {
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
  },
};
</script>
<style scoped>
.filter {
  margin: 0 0 10px;
}
</style>
