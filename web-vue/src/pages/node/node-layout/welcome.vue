<template>
  <div class="node-full-content">
    <a-divider>
      图表
      <!-- 历史监控数据 -->
      <a-button size="small" type="primary" @click="handleHistory"><a-icon type="area-chart" />历史监控图表</a-button>
    </a-divider>
    <!-- top 图表 -->
    <div id="top-chart">loading...</div>
    <a-divider>进程监控表格</a-divider>
    <!-- 进程表格数据 -->

    <a-table size="middle" :locale="tableLocale" :loading="loading" :columns="columns" :data-source="processList" bordered rowKey="pid" class="node-table" :pagination="false">
      <template #title>
        <a-row>
          <a-col :span="18">
            <a-space>
              <custom-select
                class="search-input-item"
                selStyle="width: 200px !important"
                @change="loadNodeProcess"
                @addOption="addNodeProcess"
                v-model="processName"
                :data="processNames"
                :popupContainerParent="false"
                inputPlaceholder="自定义进程类型"
                selectPlaceholder="选择进程名"
                suffixIcon=""
              >
                <template slot="suffixIcon"> <a-icon type="down" /></template>
              </custom-select>
              <div>
                <a-tooltip title="重置自定义的进程名信息">
                  <a-icon type="rest" @click="restProcessNames" />
                </a-tooltip>
              </div>
              <a-select placeholder="刷新周期" v-model="refreshInterval" style="width: 120px" @change="pullNodeData">
                <a-select-option v-for="item in [5, 10, 15, 20, 25, 30]" :key="item"> {{ item }}秒 </a-select-option>
              </a-select>
            </a-space>
          </a-col>
          <a-col :span="6">
            <a-row justify="end" type="flex">
              <a-statistic-countdown format=" s 秒" title="刷新倒计时" :value="countdownTime" @finish="pullNodeData" />
            </a-row>
          </a-col>
        </a-row>
      </template>
      <!-- <a-tooltip slot="port" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-tooltip slot="user" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip> -->
      <a-tooltip slot="tooltip" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <template slot="operation" slot-scope="text, record">
        <a-button type="primary" size="small" @click="kill(record)">Kill</a-button>
      </template>
    </a-table>
    <!-- 历史监控 -->
    <a-modal v-model="monitorVisible" width="75%" title="历史监控图表" :footer="null" :maskClosable="false">
      <node-top v-if="monitorVisible" :nodeId="this.node.id"></node-top>
    </a-modal>
  </div>
</template>
<script>
import { nodeMonitorData, getProcessList, killPid } from "@/api/node";

import CustomSelect from "@/components/customSelect";
import NodeTop from "@/pages/node/node-layout/node-top";
import { generateNodeTopChart, drawChart } from "@/api/node-stat";

export default {
  components: {
    CustomSelect,
    NodeTop,
  },
  props: {
    node: {
      type: Object,
    },
  },
  data() {
    return {
      loading: false,
      tableLocale: {
        emptyText: "",
      },
      processList: [],
      defaultProcessNames: ["java", "python", "mysql", "php", "docker"],
      processNames: [],
      monitorVisible: false,
      timeRange: "",
      historyData: [],
      processName: "java",
      columns: [
        { title: "进程 ID", dataIndex: "pid", width: 80, ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "进程名称", dataIndex: "command", width: 150, ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "端口", dataIndex: "port", width: 100, ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "所有者", dataIndex: "user", width: 100, ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "项目名称", dataIndex: "jpomName", width: 150, ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "物理内存", dataIndex: "res", width: 100, ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "进程状态", dataIndex: "status", width: 100, ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "占用CPU", dataIndex: "cpu", width: 100, ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "物理内存百分比", dataIndex: "mem", width: 140, ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "虚拟内存", dataIndex: "virt", width: 100, ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "共享内存", dataIndex: "shr", width: 100, ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "操作", dataIndex: "operation", scopedSlots: { customRender: "operation" }, align: "center", width: 80, fixed: "right" },
      ],
      refreshInterval: 20,
      historyChart: null,
      countdownTime: Date.now(),
    };
  },
  mounted() {
    this.processNames = Object.assign([], this.defaultProcessNames);
    this.initData();
    window.addEventListener("resize", this.resize);
  },
  destroyed() {
    window.removeEventListener("resize", this.resize);
  },
  watch: {
    refreshInterval: {
      deep: false,
      handler() {
        this.cacheNodeProcess();
      },
    },
  },
  methods: {
    addNodeProcess(v) {
      this.processNames = v;
      this.cacheNodeProcess();
    },
    restProcessNames() {
      this.processName = this.defaultProcessNames[0];
      this.processNames = this.defaultProcessNames;
      this.cacheNodeProcess();
      this.loadNodeProcess();
    },
    // 初始化页面
    initData() {
      const nodeCache = this.getCacheNodeProcess();

      this.processName = nodeCache?.processName || this.processName;
      this.processNames = nodeCache?.processNames || this.processNames;
      // 加载缓存信息
      this.refreshInterval = this.getCacheNode("refreshInterval", this.refreshInterval);
      //
      // console.log(this.refreshInterval);
      this.pullNodeData();
    },
    pullNodeData() {
      this.loadNodeTop();
      this.loadNodeProcess();
      // 重新计算倒计时
      this.countdownTime = Date.now() + this.refreshInterval * 1000;
    },

    resize() {
      this.historyChart?.resize();
    },
    // 请求 top 命令绘制图表
    loadNodeTop() {
      nodeMonitorData({ nodeId: this.node.id }, false).then((res) => {
        if (res.code === 200) {
          this.historyChart = drawChart(res.data, "top-chart", generateNodeTopChart);
        }
      });
    },
    // 加载节点进程列表
    loadNodeProcess(v) {
      this.loading = true;
      getProcessList({
        nodeId: this.node.id,
        processName: this.processName,
      }).then((res) => {
        if (res.code === 200) {
          this.processList = res.data;
        } else {
          this.processList = [];
        }
        this.tableLocale.emptyText = res.msg;
        this.loading = false;
      });
      if (v) {
        this.cacheNodeProcess();
      }
    },
    // kill pid
    kill(record) {
      this.$confirm({
        title: "系统提示",
        content: "真的要 Kill 这个进程么？",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // kill
          const params = {
            nodeId: this.node.id,
            pid: record.pid,
          };
          killPid(params).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg,
              });
              this.loadNodeProcess();
            }
          });
        },
      });
    },
    // 历史图表
    handleHistory() {
      this.monitorVisible = true;
    },
    cacheNodeProcess() {
      const cacheJson = this.getCacheAllNode();
      // console.log(cacheJson);
      cacheJson[this.node.id].processNames = this.processNames;
      cacheJson[this.node.id].processName = this.processName;
      cacheJson["refreshInterval"] = this.refreshInterval;
      localStorage.setItem("node-process-name", JSON.stringify(cacheJson));
    },
    getCacheNodeProcess() {
      return this.getCacheNode(this.node.id, {});
    },

    getCacheAllNode() {
      const str = localStorage.getItem("node-process-name") || "";
      let cacheJson;
      try {
        cacheJson = JSON.parse(str);
      } catch (e) {
        cacheJson = {};
      }
      return cacheJson;
    },

    getCacheNode(key, defaultValue) {
      const cacheJson = this.getCacheAllNode();

      return cacheJson[key] || defaultValue;
    },
  },
};
</script>
<style scoped>
#top-chart {
  height: calc((100vh - 70px) / 2);
}

.search-input-item {
  width: 200px !important;
  margin-right: 10px;
}

#history-chart {
  height: 60vh;
}

/deep/ .ant-statistic div {
  display: inline-block;
}

/deep/ .ant-statistic-content-value,
/deep/ .ant-statistic-content {
  font-size: 16px;
}
</style>
