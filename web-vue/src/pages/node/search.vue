<template>
  <div class="full-content">
    <!-- <div ref="filter" class="filter"></div> -->
    <a-table :data-source="projList" :columns="columns" size="middle" bordered :pagination="pagination" @change="changePage" :row-selection="rowSelection" :rowKey="(record, index) => index">
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
            <a-select-option v-for="(nodeName, key) in nodeMap" :key="key">{{ nodeName }}</a-select-option>
          </a-select>
          <a-input v-model="listQuery['%name%']" @pressEnter="getNodeProjectData" placeholder="搜索项目" class="search-input-item" />

          <a-select
            :getPopupContainer="
              (triggerNode) => {
                return triggerNode.parentNode || document.body;
              }
            "
            v-model="listQuery.runMode"
            allowClear
            placeholder="项目类型"
            class="search-input-item"
          >
            <a-select-option v-for="item in runModeList" :key="item">{{ item }}</a-select-option>
          </a-select>
          <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
            <a-button :loading="loading" type="primary" @click="getNodeProjectData">搜索</a-button>
          </a-tooltip>
          <span>| </span>

          <a-dropdown>
            <a class="ant-dropdown-link" @click="(e) => e.preventDefault()">
              批量操作
              <a-icon type="down" />
            </a>
            <a-menu slot="overlay">
              <a-menu-item>
                <a-button type="primary" @click="batchStart">批量启动</a-button>
              </a-menu-item>
              <a-menu-item>
                <a-button type="primary" @click="batchRestart">批量重启</a-button>
              </a-menu-item>
              <a-menu-item>
                <a-button type="danger" @click="batchStop">批量关闭</a-button>
              </a-menu-item>
            </a-menu>
          </a-dropdown>

          <a-tooltip>
            <template slot="title">
              <div>
                <ul>
                  <li>项目是存储在节点中的、创建需要到节点管理里面去操作</li>
                  <li>状态数据是异步获取有一定时间延迟</li>
                </ul>
              </div>
            </template>
            <a-icon type="question-circle" theme="filled" />
          </a-tooltip>

          <a-tooltip placement="topLeft" title="清除服务端缓存节点所有的项目信息, 需要重新同步">
            <a-icon @click="delAll()" type="delete" />
          </a-tooltip>
        </a-space>
      </template>
      <a-tooltip slot="name" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-tooltip slot="nodeId" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ nodeMap[text] }}</span>
      </a-tooltip>
      <a-tooltip slot="path" slot-scope="text, item" placement="topLeft" :title="item.whitelistDirectory + item.lib">
        <span>{{ item.whitelistDirectory + item.lib }}</span>
      </a-tooltip>

      <template slot="status" slot-scope="text, record">
        <a-tooltip v-if="noFileModes.includes(record.runMode)" placement="topLeft" title="状态操作请到控制台中控制">
          <a-switch :checked="text" disabled checked-children="开" un-checked-children="关" />
        </a-tooltip>
        <span v-else>-</span>
      </template>

      <a-tooltip slot="port" slot-scope="text, record" placement="topLeft" :title="`进程号：${record.pid},  端口号：${record.port}`">
        <span v-if="record.pid">{{ record.port }}/{{ record.pid }}</span>
      </a-tooltip>
      <template slot="operation" slot-scope="text, record">
        <a-space>
          <a-button size="small" type="primary" @click="handleFile(record)">文件</a-button>
          <a-button size="small" type="primary" @click="handleConsole(record)" v-show="noFileModes.includes(record.runMode)">控制台</a-button>
        </a-space>
      </template>
    </a-table>
    <!-- 项目文件组件 -->
    <a-drawer :title="drawerTitle" placement="right" width="85vw" :visible="drawerFileVisible" @close="onFileClose">
      <file v-if="drawerFileVisible" :nodeId="temp.nodeId" :projectId="temp.projectId" @goConsole="goConsole" @goReadFile="goReadFile" />
    </a-drawer>
    <!-- 项目控制台组件 -->
    <a-drawer :title="drawerTitle" placement="right" width="85vw" :visible="drawerConsoleVisible" @close="onConsoleClose">
      <console v-if="drawerConsoleVisible" :id="temp.id" :nodeId="temp.nodeId" :projectId="temp.projectId" @goFile="goFile" />
    </a-drawer>
    <!-- 项目阅读文件组件 -->
    <a-drawer :title="drawerTitle" placement="right" width="85vw" :visible="drawerReadFileVisible" @close="onReadFileClose">
      <file-read v-if="drawerReadFileVisible" :nodeId="temp.nodeId" :readFilePath="temp.readFilePath" :id="temp.id" :projectId="temp.projectId" @goFile="goFile" />
    </a-drawer>
    <!-- 批量操作状态 -->
    <a-modal v-model="batchVisible" :title="batchTitle" :footer="null" @cancel="batchClose">
      <a-list bordered :data-source="selectedRowKeys">
        <a-list-item slot="renderItem" slot-scope="item">
          <a-list-item-meta :description="item.email">
            <a slot="title"> {{ projList[item].name }}</a>
          </a-list-item-meta>
          <div>
            <a-tooltip :title="`${projList[item].cause || '未开始'}`">{{ projList[item].cause || "未开始" }} </a-tooltip>
          </div>
        </a-list-item>
      </a-list>
    </a-modal>
  </div>
</template>
<script>
import {delAllProjectCache, getNodeListAll, getProjectList} from "@/api/node";
import {getRuningProjectInfo, noFileModes, restartProject, runModeList, startProject, stopProject} from "@/api/node-project";
import File from "@/pages/node/node-layout/project/project-file";
import Console from "../node/node-layout/project/project-console";
import {itemGroupBy, parseTime} from "@/utils/time";
import {CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY} from "@/utils/const";
import FileRead from "@/pages/node/node-layout/project/project-file-read";

export default {
  components: {
    File,
    Console,
    FileRead,
  },
  data() {
    return {
      projList: [],
      runModeList: runModeList,
      selectedRowKeys: [],
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      noFileModes: noFileModes,
      nodeMap: {},
      drawerTitle: "",
      loading: true,
      temp: {},
      drawerFileVisible: false,
      drawerConsoleVisible: false,
      drawerReadFileVisible: false,
      batchVisible: false,
      batchTitle: "",
      columns: [
        { title: "项目名称", dataIndex: "name", ellipsis: true, scopedSlots: { customRender: "name" } },
        { title: "节点名称", dataIndex: "nodeId", ellipsis: true, scopedSlots: { customRender: "nodeId" } },
        {
          title: "项目路径",
          dataIndex: "path",
          ellipsis: true,
          scopedSlots: { customRender: "path" },
        },
        {
          title: "创建时间",
          dataIndex: "createTimeMillis",
          sorter: true,
          ellipsis: true,
          customRender: (text) => {
            return parseTime(text);
          },
          width: 170,
        },
        { title: "运行方式", dataIndex: "runMode", width: 90, ellipsis: true, scopedSlots: { customRender: "runMode" } },
        {
          title: "最后操作人",
          dataIndex: "modifyUser",
          width: 120,
          ellipsis: true,
          scopedSlots: { customRender: "modifyUser" },
        },
        { title: "运行状态", dataIndex: "status", width: 100, ellipsis: true, scopedSlots: { customRender: "status" } },
        { title: "端口/PID", dataIndex: "port", width: 100, ellipsis: true, scopedSlots: { customRender: "port" } },
        { title: "操作", dataIndex: "operation", align: "center", scopedSlots: { customRender: "operation" }, width: 140 },
      ],
    };
  },
  computed: {
    filePath() {
      return (this.temp.whitelistDirectory || "") + (this.temp.lib || "");
    },
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery);
    },
    rowSelection() {
      return {
        selectedRowKeys: this.selectedRowKeys,
        onChange: this.onSelectChange,
        columnWidth: "40px",
        getCheckboxProps: this.getCheckboxProps,
      };
    },
  },
  mounted() {
    this.getNodeProjectData();
    getNodeListAll().then((res) => {
      if (res.code === 200) {
        res.data.forEach((item) => {
          this.nodeMap[item.id] = item.name;
        });
      }
    });
  },
  methods: {
    getNodeProjectData(pointerEvent) {
      this.loading = true;
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page;
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
        this.loading = false;
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
      this.temp = Object.assign({}, record);
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
      this.temp = Object.assign({}, record);
      this.drawerTitle = `控制台(${this.temp.name})`;
      this.drawerConsoleVisible = true;
    },
    // 关闭控制台
    onConsoleClose() {
      this.drawerConsoleVisible = false;
      this.getNodeProjectData();
    },
    //前往文件
    goFile() {
      // 关闭控制台
      this.onConsoleClose();
      this.onReadFileClose();
      this.handleFile(this.temp);
    },
    //前往控制台
    goConsole() {
      //关闭文件
      this.onFileClose();
      this.handleConsole(this.temp);
    },
    // 阅读文件
    goReadFile(path, filename) {
      this.onFileClose();
      this.drawerReadFileVisible = true;
      this.temp.readFilePath = (path + "/" + filename).replace(new RegExp("//", "gm"), "/");
      this.drawerTitle = `阅读文件(${filename})`;
    },
    onReadFileClose() {
      this.drawerReadFileVisible = false;
    },
    //选中项目
    onSelectChange(selectedRowKeys) {
      this.selectedRowKeys = selectedRowKeys;
    },
    batchClose() {
      this.batchVisible = false;
      this.getNodeProjectData();
      this.selectedRowKeys = [];
    },
    //批量开始
    batchStart() {
      if (this.selectedRowKeys.length <= 0) {
        this.$notification.warning({
          message: "请选中要启动的项目",
        });
        return;
      }
      this.batchVisible = true;
      this.batchTitle = "批量启动";
      this.projList = this.projList.map((item) => {
        delete item.cause;
        return item;
      });
      this.batchStartInfo(1);
    },
    //批量启动详情
    batchStartInfo(count) {
      if (count > this.selectedRowKeys.length) {
        return;
      }
      let value = this.selectedRowKeys[count - 1];
      this.projList[value].cause = "启动中";
      count++;
      if (this.projList[value].runMode !== "File") {
        const params = {
          nodeId: this.projList[value].nodeId,
          id: this.projList[value].projectId,
        };
        startProject(params)
          .then((data) => {
            this.projList[value].cause = data.msg;
            this.selectedRowKeys = [...this.selectedRowKeys];
            this.batchStartInfo(count);
          })
          .catch(() => {
            this.projList[value].cause = "启动失败";
            this.selectedRowKeys = [...this.selectedRowKeys];
            this.batchStartInfo(count);
          });
      } else {
        this.projList[value].cause = "跳过";
        this.selectedRowKeys = [...this.selectedRowKeys];
        this.batchStartInfo(count);
      }
    },
    //批量重启
    batchRestart() {
      if (this.selectedRowKeys.length <= 0) {
        this.$notification.warning({
          message: "请选中要重启的项目",
        });
        return;
      }
      this.batchVisible = true;
      this.batchTitle = "批量重新启动";
      this.projList = this.projList.map((item) => {
        delete item.cause;
        return item;
      });
      this.batchRestartInfo(1);
    },
    //批量重启详情
    batchRestartInfo(count) {
      if (count > this.selectedRowKeys.length) {
        return;
      }
      let value = this.selectedRowKeys[count - 1];
      this.projList[value].cause = "重新启动中";
      count++;
      if (this.projList[value].runMode !== "File") {
        const params = {
          nodeId: this.projList[value].nodeId,
          id: this.projList[value].projectId,
        };
        restartProject(params)
          .then((data) => {
            this.projList[value].cause = data.msg;
            this.selectedRowKeys = [...this.selectedRowKeys];
            this.batchRestartInfo(count);
          })
          .catch(() => {
            this.projList[value].cause = "重新启动失败";
            this.selectedRowKeys = [...this.selectedRowKeys];
            this.batchRestartInfo(count);
          });
      } else {
        this.projList[value].cause = "跳过";
        this.selectedRowKeys = [...this.selectedRowKeys];
        this.batchRestartInfo(count);
      }
    },
    //批量关闭
    batchStop() {
      if (this.selectedRowKeys.length <= 0) {
        this.$notification.warning({
          message: "请选中要关闭的项目",
        });
      }
      this.batchVisible = true;
      this.batchTitle = "批量关闭启动";
      this.projList = this.projList.map((item) => {
        delete item.cause;
        return item;
      });
      this.batchStopInfo(1);
    },
    //批量关闭详情
    batchStopInfo(count) {
      if (count > this.selectedRowKeys.length) {
        return;
      }
      let value = this.selectedRowKeys[count - 1];
      this.projList[value].cause = "关闭中";
      count++;
      if (this.projList[value].runMode !== "File") {
        const params = {
          nodeId: this.projList[value].nodeId,
          id: this.projList[value].projectId,
        };
        stopProject(params)
          .then((data) => {
            this.projList[value].cause = data.msg;
            this.selectedRowKeys = [...this.selectedRowKeys];
            this.batchStopInfo(count);
          })
          .catch(() => {
            this.projList[value].cause = "关闭失败";
            this.selectedRowKeys = [...this.selectedRowKeys];
            this.batchStopInfo(count);
          });
      } else {
        this.projList[value].cause = "跳过";
        this.selectedRowKeys = [...this.selectedRowKeys];
        this.batchStopInfo(count);
      }
    },
    // 获取复选框属性 判断是否可以勾选
    getCheckboxProps(record) {
      return {
        props: {
          disabled: record.runMode === "File",
          name: record.name,
        },
      };
    },
    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter });
      this.getNodeProjectData();
    },
    delAll() {
      this.$confirm({
        title: "系统提示",
        content: "确定要清除服务端所有的项目缓存信息吗？",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 删除
          delAllProjectCache().then((res) => {
            if (res.code == 200) {
              this.$notification.success({
                message: res.msg,
              });
              this.getNodeProjectData();
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
  margin-bottom: 10px;
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
}*/
</style>
