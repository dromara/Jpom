<template>
  <div class="node-full-content">
    <!-- <div ref="filter" class="filter"></div> -->
    <!-- 表格 -->
    <a-table :data-source="list" :loading="loading" :columns="columns" :pagination="false" bordered :rowKey="(record, index) => index">
      <template #title>
        <a-button type="primary" @click="handleFilter">{{ $t('common.refresh') }}</a-button>
      </template>
      <a-switch slot="status" slot-scope="text" :checked="text" disabled :checked-children=this.$t('common.on') :un-checked-children=this.$t('common.off') />
      <template slot="operation" slot-scope="text, record">
        <a-space>
          <a-button type="primary" @click="handleConsole(record)">{{ $t('common.console') }}</a-button>

          <a-button type="danger" @click="handleDelete(record)">{{ $t('common.delete') }}</a-button>
        </a-space>
      </template>
    </a-table>
    <!-- 项目控制台组件 -->
    <a-drawer destroyOnClose :title="drawerTitle" placement="right" width="85vw" :visible="drawerConsoleVisible" @close="onConsoleClose">
      <console v-if="drawerConsoleVisible" :nodeId="node.id" :id="project.id" :projectId="project.projectId" :replica="temp" :copyId="temp.id" />
    </a-drawer>
  </div>
</template>
<script>
import Console from "./project-console";

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
  },
  data() {
    return {
      loading: false,
      list: [],
      temp: {},
      drawerTitle: "",
      drawerConsoleVisible: false,

      columns: [
        { title: this.$t('node.node_layout.project.replica.copyId'), dataIndex: "id", width: 150, ellipsis: true, scopedSlots: { customRender: "id" } },
        { title: this.$t('common.status'), dataIndex: "status", width: 100, ellipsis: true, scopedSlots: { customRender: "status" } },
        { title: this.$t('node.node_layout.project.replica.processId'), dataIndex: "pid", width: 100, ellipsis: true, scopedSlots: { customRender: "pid" } },
        { title: this.$t('common.portId'), dataIndex: "port", width: 100, ellipsis: true, scopedSlots: { customRender: "port" } },
        { title: this.$t('common.lastModifiedTime'), dataIndex: "modifyTime", width: 180, ellipsis: true, scopedSlots: { customRender: "modifyTime" } },
        { title: this.$t('common.operation'), dataIndex: "operation", scopedSlots: { customRender: "operation" }, width: 220 },
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

    // 删除
    handleDelete(record) {
      this.$confirm({
        title: this.$t('common.systemPrompt'),
        content: this.$t('node.node_layout.project.replica.deleteCopy'),
        okText: this.$t('common.confirm'),
        cancelText: this.$t('common.cancel'),
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
/* .filter {
  margin: 0 0 10px;
} */
</style>
