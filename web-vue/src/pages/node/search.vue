<template>
  <div class="full-content">
    <div ref="filter" class="filter">
      <a-select v-model="listQuery.nodeId" allowClear placeholder="请选择节点" class="filter-item">
        <a-select-option v-for="(nodeName, key) in nodeMap" :key="key">{{ nodeName }}</a-select-option>
      </a-select>
      <a-input v-model="listQuery['%name%']" placeholder="搜索项目" class="search-input-item" />

      <a-select v-model="listQuery.runMode" allowClear placeholder="项目类型" class="filter-item">
        <a-select-option v-for="item in runModeList" :key="item">{{ item }}</a-select-option>
      </a-select>
      <a-tooltip title="按住 Ctr 或者 Alt 键点击按钮快速回到第一页">
        <a-button type="primary" @click="getNodeProjectData">搜索</a-button>
      </a-tooltip>
      <span>| </span>
      <a-button type="primary" @click="batchStart">批量启动</a-button>
      <a-button type="primary" @click="batchRestart">批量重启</a-button>
      <a-button type="danger" @click="batchStop">批量关闭</a-button>
      状态数据是异步获取有一定时间延迟
      <a-tooltip placement="topLeft" title="清除服务端缓存节点所有的项目信息, 需要重新同步">
        <a-icon @click="delAll()" type="delete" />
      </a-tooltip>
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
      <a-tooltip slot="nodeId" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ nodeMap[text] }}</span>
      </a-tooltip>
      <a-tooltip slot="path" slot-scope="text, item" placement="topLeft" :title="item.whitelistDirectory + item.lib">
        <span>{{ item.whitelistDirectory + item.lib }}</span>
      </a-tooltip>

      <template slot="status" slot-scope="text, record">
        <a-tooltip v-if="record.runMode !== 'File'" placement="topLeft" title="状态操作请到控制台中控制">
          <a-switch :checked="text" disabled checked-children="开" un-checked-children="关" />
        </a-tooltip>
      </template>

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
import { getProjectList, delAllProjectCache, getNodeListAll } from "@/api/node";
import { restartProject, startProject, stopProject, getRuningProjectInfo, runModeList } from "@/api/node-project";
import { mapGetters } from "vuex";
import File from "../node/node-layout/project/project-file";
import Console from "../node/node-layout/project/project-console";
import { parseTime, itemGroupBy } from "@/utils/time";
import { PAGE_DEFAULT_LIMIT, PAGE_DEFAULT_SIZW_OPTIONS, PAGE_DEFAULT_SHOW_TOTAL, PAGE_DEFAULT_LIST_QUERY } from "@/utils/const";
export default {
  components: {
    File,
    Console,
  },
  data() {
    return {
      projList: [],
      runModeList: runModeList,
      selectedRowKeys: [],
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      nodeMap: {},
      drawerTitle: "",
      temp: {},
      drawerFileVisible: false,
      drawerConsoleVisible: false,
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
        { title: "操作", dataIndex: "operation", scopedSlots: { customRender: "operation" }, width: 200 },
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
        });
        return;
      }
    this.batchStartInfo(1);
    },
    //批量启动详情
    batchStartInfo(count) {
      let value = this.selectedRowKeys[count - 1];
      count++;
      const h = this.$createElement;
      let info = this.$info({
        title: "批量启动",
        content: h("div", {}, [h("p", "正在启动项目:" + value.name + ",请稍等~~")]),
      });
      if (value.status === undefined && value.runMode !== "File") {
        const params = {
           nodeId: this.projList[value].nodeId,
           id: this.projList[value].id,
        };
        startProject(params)
          .then(() => {
            info.destroy();
            if (count <= this.selectedRowKeys.length) {
              this.batchStartInfo(count);
            } else {
              this.loadData();
            }
          })
          .catch(() => {
            info.destroy();
            if (count <= this.selectedRowKeys.length) {
              this.batchStartInfo(count);
            } else {
              this.loadData();
            }
          });
      } else {
        info.destroy();
        if (count <= this.selectedRowKeys.length) {
          this.batchStartInfo(count);
        } else {
          this.loadData();
        }
      }
    },
    //批量重启
    batchRestart() {
      if (this.selectedRowKeys.length == 0) {
        this.$notification.warning({
          message: "请选中要重启的项目",
        });
        return;
      }
      this.batchRestartInfo(1);
    },
     //批量重启详情
    batchRestartInfo(count) {
      let value = this.selectedRowKeys[count - 1];
      count++;
      const h = this.$createElement;
      let info = this.$info({
        title: "批量重启",
        content: h("div", {}, [h("p", "正在重启项目:" + value.name + ",请稍等~~")]),
      });
      if (value.status === undefined && value.runMode !== "File") {
        const params = {
           nodeId: this.projList[value].nodeId,
           id: this.projList[value].id,
        };
        restartProject(params)
          .then(() => {
            info.destroy();
            if (count <= this.selectedRowKeys.length) {
              this.batchStartInfo(count);
            } else {
              this.loadData();
            }
          })
          .catch(() => {
            info.destroy();
            if (count <= this.selectedRowKeys.length) {
              this.batchStartInfo(count);
            } else {
              this.loadData();
            }
          });
      } else {
        info.destroy();
        if (count <= this.selectedRowKeys.length) {
          this.batchStartInfo(count);
        } else {
          this.loadData();
        }
      }
    },
    //批量关闭
    batchStop() {
      if (this.selectedRowKeys.length == 0) {
        this.$notification.warning({
          message: "请选中要关闭的项目",
        });
      }
      this.batchStopInfo(1);
    },
     //批量关闭详情
    batchStopInfo(count) {
      let value = this.selectedRows[count - 1];
      count++;
      const h = this.$createElement;
      let info = this.$info({
        title: "批量关闭",
        content: h("div", {}, [h("p", "正在关闭项目:" + value.name + ",请稍等~~")]),
      });
      if (value.status === undefined && value.runMode !== "File") {
        const params = {
           nodeId: this.projList[value].nodeId,
           id: this.projList[value].id,
        };
        stopProject(params)
          .then(() => {
            info.destroy();
            if (count <= this.selectedRowKeys.length) {
              this.batchStartInfo(count);
            } else {
              this.loadData();
            }
          })
          .catch(() => {
            info.destroy();
            if (count <= this.selectedRowKeys.length) {
              this.batchStartInfo(count);
            } else {
              this.loadData();
            }
          });
      } else {
        info.destroy();
        if (count <= this.selectedRowKeys.length) {
          this.batchStartInfo(count);
        } else {
          this.loadData();
        }
      }
    },
    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery.page = pagination.current;
      this.listQuery.limit = pagination.pageSize;
      if (sorter) {
        this.listQuery.order = sorter.order;
        this.listQuery.order_field = sorter.field;
      }
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
