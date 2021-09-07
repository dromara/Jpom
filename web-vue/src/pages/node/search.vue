<template>
  <div>
    <div ref="filter" class="filter">
      <a-input-search placeholder="搜索项目" enter-button @search="searchProject" class="projSearch" />
      <a-button type="primary" @click="getNodeProjectList">刷新</a-button>
      <span>| </span>
      <a-button type="primary" @click="batchStart">批量启动</a-button>
      <a-button type="primary" @click="batchRestart">批量重启</a-button>
      <a-button type="danger" @click="batchStop">批量关闭</a-button>
    </div>
    <a-table
      :data-source="projList"
      :columns="columns"
      :pagination="false"
      bordered
      :row-selection="{ selectedRowKeys: selectedRowKeys, onChange: onSelectChange, columnWidth: '25px' }"
      :rowKey="(record, index) => index"
      :style="{ 'max-height': tableHeight + 'px' }"
      :scroll="{ x: 1330, y: tableHeight - 60 }"
    >
      <a-tooltip slot="name" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <template slot="time" slot-scope="text, record" placement="topLeft">
        <a-tooltip :title="`创建时间：${record.createTime}，${record.modifyTime ? '修改时间：' + record.modifyTime : ''}`">
          <span>{{ record.modifyTime }}</span
          ><br />
          <span>{{ record.createTime }}</span>
        </a-tooltip>
      </template>
      <a-tooltip slot="modifyUser" slot-scope="text" placement="topLeft" :title="text">
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
      <file v-if="drawerFileVisible" :nodeId="temp.nodeId" :projectId="temp.id" />
    </a-drawer>
    <!-- 项目控制台组件 -->
    <a-drawer :title="drawerTitle" placement="right" width="85vw" :visible="drawerConsoleVisible" @close="onConsoleClose">
      <console v-if="drawerConsoleVisible" :nodeId="temp.nodeId" :projectId="temp.id" />
    </a-drawer>
  </div>
</template>
<script>
import { getNodeProjectList } from "../../api/node";
import { restartProject, startProject, stopProject } from "../../api/node-project";
import { mapGetters } from "vuex";
import File from "../node/node-layout/project/project-file";
import Console from "../node/node-layout/project/project-console";
export default {
  components: {
    File,
    Console,
  },
  data() {
    return {
      projList: [],
      initList: [],
      selectedRowKeys: [],
      tableHeight: "",
      drawerTitle: "",
      temp: {},
      drawerFileVisible: false,
      drawerConsoleVisible: false,
      columns: [
        { title: "项目名称", dataIndex: "name", width: 60, ellipsis: true, scopedSlots: { customRender: "name" } },
        { title: "创建/修改时间", dataIndex: "createTime", width: 100, ellipsis: true, scopedSlots: { customRender: "time" } },
        // { title: "修改时间", dataIndex: "modifyTime", width: 160, ellipsis: true, scopedSlots: { customRender: "modifyTime" } },
        {
          title: "最后操作人",
          dataIndex: "modifyUser",
          width: 60,
          ellipsis: true,
          scopedSlots: { customRender: "modifyUser" },
        },
        { title: "运行状态", dataIndex: "status", width: 50, ellipsis: true, scopedSlots: { customRender: "status" } },
        { title: "端口/PID", dataIndex: "port", width: 50, ellipsis: true, scopedSlots: { customRender: "port" } },
        { title: "操作", dataIndex: "operation", scopedSlots: { customRender: "operation" }, width: 240 },
      ],
    };
  },
  computed: {
    ...mapGetters(["getGuideFlag"]),
    filePath() {
      return (this.temp.whitelistDirectory || "") + (this.temp.lib || "");
    },
  },
  mounted() {
    this.getNodeProjectData();
  },
  methods: {
    //获取全部项目
    getNodeProjectData() {
      const params = {
        status: true,
      };
      getNodeProjectList(params).then((res) => {
        if (res.code === 200) {
          this.projList = [];
          this.initList = [];
          res.data.forEach((element) => {
            element.projects.forEach((proj) => {
              proj.nodeId = element.id;

              this.projList.push(proj);
              this.initList.push(proj);
            });
          });
        }
      });
    },
    // 计算表格高度
    calcTableHeight() {
      this.$nextTick(() => {
        this.tableHeight = window.innerHeight - this.$refs["filter"].clientHeight - 155;
      });
    },
    //搜索项目
    searchProject(value) {
      let resList = [];
      this.initList.forEach((ele) => {
        if (ele.name.indexOf(value) !== -1) {
          resList.push(ele);
        }
      });
      if (resList.length !== 0) {
        this.projList = resList;
      } else {
        this.$notification.warning({
          message: "未搜到项目!",
          duration: 2,
        });
      }
    },
    // 文件管理
    handleFile(record) {
      this.temp = Object.assign(record);
      this.drawerTitle = `文件管理(${this.temp.projectId})`;
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
      this.drawerTitle = `控制台(${this.temp.projectId})`;
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
.projSearch {
  width: 400px;
  margin-right: 10px;
}
</style>
