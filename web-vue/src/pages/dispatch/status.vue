<template>
  <div>
    <!-- 嵌套表格 -->
    <a-table :loading="childLoading" :columns="childColumns" size="middle" :bordered="true" :data-source="list" :pagination="false" rowKey="id_no">
      <template #title>
        <a-space>
          <div>
            当前状态：
            <a-tag v-if="data.status === 2" color="green">{{ statusMap[data.status] || "未知" }}</a-tag>
            <a-tag v-else-if="data.status === 1 || data.status === 0" color="orange">{{ statusMap[data.status] || "未知" }}</a-tag>
            <a-tag v-else-if="data.status === 3 || data.status === 4" color="red">{{ statusMap[data.status] || "未知" }}</a-tag>
            <a-tag v-else>{{ statusMap[data.status] || "未知" }}</a-tag>
          </div>
          <div>状态描述：{{ data.statusMsg || "-" }}</div>
          <a-button type="primary" :loading="childLoading" @click="loadData">刷新</a-button>

          <a-statistic-countdown format=" s 秒" title="刷新倒计时" :value="countdownTime" @finish="silenceLoadData" />
        </a-space>
      </template>
      <a-tooltip slot="nodeId" slot-scope="text" placement="topLeft" :title="text">
        <a-button type="link" style="padding: 0" size="small" @click="toNode(text)">
          <span>{{ nodeNameMap[text] || text }}</span>
          <a-icon type="fullscreen" />
        </a-button>
      </a-tooltip>
      <template slot="projectName" slot-scope="text, item">
        <template v-if="item.disabled">
          <a-tooltip title="当前项目被禁用">
            <a-icon type="eye-invisible" />
          </a-tooltip>
        </template>
        <a-tooltip slot="projectName" placement="topLeft" :title="text">
          <span>{{ text || item.cacheProjectName }}</span>
        </a-tooltip>
      </template>
      <template slot="outGivingStatus" slot-scope="text">
        <a-tag v-if="text === 2" color="green">{{ dispatchStatusMap[text] || "未知" }}</a-tag>
        <a-tag v-else-if="text === 1 || text === 0 || text === 5" color="orange">{{ dispatchStatusMap[text] || "未知" }}</a-tag>
        <a-tag v-else-if="text === 3 || text === 4 || text === 6" color="red">{{ dispatchStatusMap[text] || "未知" }}</a-tag>
        <a-tag v-else>{{ dispatchStatusMap[text] || "未知" }}</a-tag>
      </template>
      <a-tooltip slot="outGivingResultMsg" slot-scope="text, item" placement="topLeft" :title="readJsonStrField(item.outGivingResult, 'msg')">
        <span>{{ readJsonStrField(item.outGivingResult, "code") }}-{{ readJsonStrField(item.outGivingResult, "msg") || item.outGivingResult }}</span>
      </a-tooltip>
      <a-tooltip slot="outGivingResultTime" slot-scope="text, item" placement="topLeft" :title="readJsonStrField(item.outGivingResult, 'upload_duration')">
        <span>{{ readJsonStrField(item.outGivingResult, "upload_duration") }}</span>
      </a-tooltip>
      <a-tooltip slot="outGivingResultSize" slot-scope="text, item" placement="topLeft" :title="readJsonStrField(item.outGivingResult, 'upload_file_size')">
        {{ readJsonStrField(item.outGivingResult, "upload_file_size") }}
      </a-tooltip>
      <a-tooltip slot="outGivingResultMsgData" slot-scope="text, item" placement="topLeft" :title="`${readJsonStrField(item.outGivingResult, 'data')}`">
        <template v-if="item.fileSize"> {{ Math.floor((item.progressSize / item.fileSize) * 100) }}% </template>
        {{ readJsonStrField(item.outGivingResult, "data") }}
      </a-tooltip>

      <template slot="projectStatus" slot-scope="text, item">
        <a-tooltip v-if="item.errorMsg" :title="item.errorMsg">
          <a-icon type="warning" />
        </a-tooltip>
        <a-switch v-else :checked="text" :disabled="true" size="small" checked-children="运行中" un-checked-children="未运行" />
      </template>

      <a-tooltip slot="projectPid" slot-scope="text, record" placement="topLeft" :title="`进程号：${record.projectPid || '-'} / 端口号：${record.projectPort || '-'}`">
        <span>{{ record.projectPid || "-" }}/{{ record.projectPort || "-" }}</span>
      </a-tooltip>

      <template slot="child-operation" slot-scope="text, record">
        <a-space>
          <a-button size="small" :disabled="!record.projectName" type="primary" @click="handleFile(record)">文件</a-button>
          <a-button size="small" :disabled="!record.projectName" type="primary" @click="handleConsole(record)">控制台</a-button>
        </a-space>
      </template>
    </a-table>

    <!-- 项目文件组件 -->
    <a-drawer destroyOnClose :title="drawerTitle" placement="right" width="85vw" :visible="drawerFileVisible" @close="onFileClose">
      <file v-if="drawerFileVisible" :id="temp.id" :nodeId="temp.nodeId" :projectId="temp.projectId" @goConsole="goConsole" @goReadFile="goReadFile" />
    </a-drawer>
    <!-- 项目控制台组件 -->
    <a-drawer destroyOnClose :title="drawerTitle" placement="right" width="85vw" :visible="drawerConsoleVisible" @close="onConsoleClose">
      <console v-if="drawerConsoleVisible" :id="temp.id" :nodeId="temp.nodeId" :projectId="temp.projectId" @goFile="goFile" />
    </a-drawer>
    <!-- 项目跟踪文件组件 -->
    <a-drawer destroyOnClose :title="drawerTitle" placement="right" width="85vw" :visible="drawerReadFileVisible" @close="onReadFileClose">
      <file-read v-if="drawerReadFileVisible" :nodeId="temp.nodeId" :readFilePath="temp.readFilePath" :id="temp.id" :projectId="temp.projectId" @goFile="goFile" />
    </a-drawer>
  </div>
</template>
<script>
import { getDispatchProject, dispatchStatusMap, statusMap } from "@/api/dispatch";
import { getNodeListAll } from "@/api/node";
import { getRuningProjectInfo } from "@/api/node-project";
import { readJsonStrField, concurrentExecution, randomStr, itemGroupBy, parseTime, renderSize, formatDuration } from "@/utils/const";
import File from "@/pages/node/node-layout/project/project-file";
import Console from "@/pages/node/node-layout/project/project-console";
import FileRead from "@/pages/node/node-layout/project/project-file-read";
export default {
  components: {
    File,
    Console,
    FileRead,
  },
  props: {
    id: {
      type: String,
    },
  },
  data() {
    return {
      loading: false,
      childLoading: false,
      statusMap,
      dispatchStatusMap,

      list: [],
      data: {},
      drawerTitle: "",
      drawerFileVisible: false,
      drawerConsoleVisible: false,
      drawerReadFileVisible: false,
      nodeNameMap: {},

      childColumns: [
        { title: "节点名称", dataIndex: "nodeId", width: 120, ellipsis: true, scopedSlots: { customRender: "nodeId" } },
        { title: "项目名称", dataIndex: "projectName", width: 120, ellipsis: true, scopedSlots: { customRender: "projectName" } },
        { title: "项目状态", dataIndex: "projectStatus", width: 120, ellipsis: true, scopedSlots: { customRender: "projectStatus" } },
        { title: "进程/端口", dataIndex: "projectPid", width: "120px", ellipsis: true, scopedSlots: { customRender: "projectPid" } },
        { title: "分发状态", dataIndex: "outGivingStatus", width: "120px", scopedSlots: { customRender: "outGivingStatus" } },
        { title: "分发结果", dataIndex: "outGivingResultMsg", ellipsis: true, width: 120, scopedSlots: { customRender: "outGivingResultMsg" } },
        { title: "分发状态消息", dataIndex: "outGivingResultMsgData", ellipsis: true, width: 120, scopedSlots: { customRender: "outGivingResultMsgData" } },
        { title: "分发耗时", dataIndex: "outGivingResultTime", width: "120px", scopedSlots: { customRender: "outGivingResultTime" } },
        { title: "文件大小", dataIndex: "outGivingResultSize", width: "100px", scopedSlots: { customRender: "outGivingResultSize" } },
        {
          title: "最后分发时间",
          dataIndex: "lastTime",
          width: "170px",
          ellipsis: true,
          customRender: (text) => parseTime(text),
        },
        { title: "操作", dataIndex: "child-operation", fixed: "right", scopedSlots: { customRender: "child-operation" }, width: "140px", align: "center" },
      ],

      countdownTime: Date.now(),
      refreshInterval: 5,
    };
  },

  computed: {},
  watch: {},
  created() {
    this.loadData();
    this.loadNodeList();
  },
  methods: {
    readJsonStrField,
    renderSize,
    formatDuration,
    randomStr,

    loadData() {
      this.childLoading = true;
      this.handleReloadById().then(() => {
        // 重新计算倒计时
        this.countdownTime = Date.now() + this.refreshInterval * 1000;
      });
    },
    // 加载节点以及项目
    loadNodeList(fn) {
      this.nodeList = [];
      getNodeListAll().then((res) => {
        if (res.code === 200) {
          this.nodeList = res.data;
          this.nodeList.map((item) => {
            // this.nodeNameMap[item.id] = item.name;
            this.nodeNameMap = { ...this.nodeNameMap, [item.id]: item.name };
          });
          fn && fn();
        }
      });
    },
    // 静默
    silenceLoadData() {
      this.handleReloadById().then(() => {
        // 重新计算倒计时
        this.countdownTime = Date.now() + this.refreshInterval * 1000;
      });
    },

    handleReloadById() {
      return new Promise((resolve) => {
        getDispatchProject(this.id, false)
          .then((res) => {
            if (res.code === 200 && res.data) {
              let projectList =
                res.data?.projectList?.map((item) => {
                  return { ...item, id_no: `${item.id}-${item.nodeId}-${item.projectId}-${new Date().getTime()}` };
                }) || [];
              this.data = res.data?.data || {};
              let oldProjectList = this.list;
              let oldProjectMap = oldProjectList.groupBy((item) => item.id);
              projectList = projectList.map((item) => {
                return Object.assign({}, oldProjectMap[item.id], item);
              });
              this.list = projectList;
              // 查询项目状态
              const nodeProjects = itemGroupBy(projectList, "nodeId");
              this.getRuningProjectInfo(nodeProjects);
            }
            this.childLoading = false;
            resolve();
          })
          .catch(() => {
            // 取消加载中
            this.childLoading = false;
            resolve();
          });
      });
    },
    getRuningProjectInfo(nodeProjects) {
      if (nodeProjects.length <= 0) {
        return;
      }
      concurrentExecution(
        nodeProjects.map((item, index) => {
          return index;
        }),
        3,
        (curItem) => {
          const data = nodeProjects[curItem];

          return new Promise((resolve, reject) => {
            const ids = data.data.map((item) => {
              return item.projectId;
            });
            if (ids.length <= 0) {
              resolve();
              return;
            }
            const tempParams = {
              nodeId: data.type,
              ids: JSON.stringify(ids),
            };
            getRuningProjectInfo(tempParams, "noTip")
              .then((res2) => {
                if (res2.code === 200) {
                  this.list = this.list.map((element) => {
                    if (res2.data[element.projectId] && element.nodeId === data.type) {
                      return {
                        ...element,
                        projectStatus: res2.data[element.projectId].pid > 0,
                        projectPid: (res2.data[element.projectId]?.pids || [res2.data[element.projectId]?.pid || "-"]).join(","),
                        projectPort: res2.data[element.projectId]?.port || "-",
                        errorMsg: res2.data[element.projectId].error,
                        projectName: res2.data[element.projectId].name,
                      };
                    }
                    return element;
                  });
                  resolve();
                } else {
                  this.list = this.list.map((element) => {
                    if (element.nodeId === data.type) {
                      return { ...element, projectStatus: false, projectPid: "-", errorMsg: res2.msg };
                    }
                    return element;
                  });

                  reject();
                }
              })
              .catch(() => {
                this.list = this.list.map((element) => {
                  if (element.nodeId === data.type) {
                    return { ...element, projectStatus: false, projectPid: "-", errorMsg: "网络异常" };
                  }
                  return element;
                });

                reject();
              });
          });
        }
      );
    },
    // 文件管理
    handleFile(record) {
      this.temp = Object.assign({}, record);
      this.drawerTitle = `文件管理(${this.temp.projectId})`;
      this.drawerFileVisible = true;
    },
    // 关闭文件管理对话框
    onFileClose() {
      this.drawerFileVisible = false;
    },
    // 控制台
    handleConsole(record) {
      this.temp = Object.assign({}, record);
      this.drawerTitle = `控制台(${this.temp.projectId})`;
      this.drawerConsoleVisible = true;
    },
    // 关闭控制台
    onConsoleClose() {
      this.drawerConsoleVisible = false;
    },
    //前往控制台
    goConsole() {
      //关闭文件
      this.onFileClose();
      this.handleConsole(this.temp);
    },
    //前往文件
    goFile() {
      // 关闭控制台
      this.onConsoleClose();
      this.onReadFileClose();
      this.handleFile(this.temp);
    },
    // 跟踪文件
    goReadFile(path, filename) {
      this.onFileClose();
      this.drawerReadFileVisible = true;
      this.temp.readFilePath = (path + "/" + filename).replace(new RegExp("//", "gm"), "/");
      this.drawerTitle = `跟踪文件(${filename})`;
    },
    onReadFileClose() {
      this.drawerReadFileVisible = false;
    },
    toNode(nodeId) {
      const newpage = this.$router.resolve({
        name: "node_" + nodeId,
        path: "/node/list",
        query: {
          ...this.$route.query,
          nodeId: nodeId,
          pId: "manage",
          id: "manageList",
        },
      });
      window.open(newpage.href, "_blank");
    },
  },
};
</script>
<style scoped>
/deep/ .ant-progress-text {
  width: auto;
}
/* .replica-btn-del {
    position: absolute;
    right: 0;
    top: 74px;
  } */
/deep/ .ant-statistic div {
  display: inline-block;
}

/deep/ .ant-statistic-content-value,
/deep/ .ant-statistic-content {
  font-size: 16px;
}
</style>
