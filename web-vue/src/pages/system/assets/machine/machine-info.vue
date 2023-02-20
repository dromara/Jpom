<template>
  <div>
    <a-tabs @change="changeTabs">
      <template slot="tabBarExtraContent">
        <a-space>
          <a-statistic-countdown format=" s 秒" title="刷新倒计时" :value="countdownTime" @finish="pullNodeData" />
        </a-space>
      </template>
      <a-tab-pane key="info" tab="信息">
        <a-card size="small">
          <template slot="title">
            {{ machineInfo && machineInfo.name }}
          </template>
          <template slot="extra">
            <a-tag v-if="machineInfo" :color="machineInfo && machineInfo.status === 1 ? 'green' : 'pink'" style="margin-right: 0px">
              {{ statusMap[machineInfo && machineInfo.status] || "未知" }}
            </a-tag>
          </template>
          <a-space direction="vertical" style="display: block">
            <a-alert v-if="machineInfo && machineInfo.status !== 1" message="状态描述" :description="(machineInfo && machineInfo.statusMsg) || ''" type="warning" show-icon />
            <a-descriptions :column="4" :bordered="true">
              <template slot="title"> </template>

              <a-descriptions-item label="系统名" :span="2">{{ machineInfo && machineInfo.osName }}</a-descriptions-item>
              <a-descriptions-item label="系统版本" :span="2">{{ machineInfo && machineInfo.osVersion }}</a-descriptions-item>
              <a-descriptions-item label="硬件信息" :span="2"> {{ machineInfo && machineInfo.osHardwareVersion }} </a-descriptions-item>
              <a-descriptions-item label="CPU型号" :span="2"> {{ machineInfo && machineInfo.osCpuIdentifierName }} </a-descriptions-item>
              <a-descriptions-item label="主机名" :span="2"> {{ machineInfo && machineInfo.hostName }} </a-descriptions-item>
              <a-descriptions-item label="IPV4" :span="2">
                <template v-if="machineInfo && machineInfo.ipv4List && machineInfo.ipv4List.length">
                  {{ machineInfo && machineInfo.ipv4List[0] }}
                  <a-popover title="所有的IPV4列表">
                    <template slot="content">
                      <p v-for="item in machineInfo && machineInfo.ipv4List" :key="item">{{ item }}</p>
                    </template>
                    <a-tag>
                      <!-- :count=""
                      :number-style="{
                        backgroundColor: '#fff',
                        color: '#999',
                        boxShadow: '0 0 0 1px #d9d9d9 inset',
                      }" -->
                      {{ machineInfo && machineInfo.ipv4List && machineInfo.ipv4List.length }}
                      <!-- <a-icon type="more" /> -->
                      <a-icon type="ellipsis" />
                    </a-tag>
                  </a-popover>
                </template>
              </a-descriptions-item>
              <a-descriptions-item label="CPU数">{{ machineInfo && machineInfo.osCpuCores }} </a-descriptions-item>
              <a-descriptions-item label="内存">{{ renderSize(machineInfo && machineInfo.osMoneyTotal) }} </a-descriptions-item>
              <a-descriptions-item label="硬盘">{{ renderSize(machineInfo && machineInfo.osFileStoreTotal) }} </a-descriptions-item>

              <a-descriptions-item label="负载">{{ machineInfo && machineInfo.osLoadAverage }} </a-descriptions-item>
              <a-descriptions-item label="系统运行时间">{{ formatDuration(((machineInfo && machineInfo.osSystemUptime) || 0) * 1000, "", 3) }} </a-descriptions-item>
              <a-descriptions-item label="插件版本">{{ machineInfo && machineInfo.jpomVersion }} </a-descriptions-item>
              <a-descriptions-item label="插件运行时间">{{ formatDuration(machineInfo && machineInfo.jpomUptime, "", 3) }} </a-descriptions-item>
              <a-descriptions-item label="插件构建时间">{{ machineInfo && machineInfo.jpomBuildTime }} </a-descriptions-item>
              <a-descriptions-item label="JDK版本">{{ machineInfo && machineInfo.javaVersion }} </a-descriptions-item>
              <a-descriptions-item label="JVM总内存">{{ renderSize(machineInfo && machineInfo.jvmTotalMemory) }} </a-descriptions-item>
              <a-descriptions-item label="JVM剩余内存">{{ renderSize(machineInfo && machineInfo.jvmFreeMemory) }} </a-descriptions-item>

              <a-descriptions-item label="项目数">{{ machineInfo && machineInfo.jpomProjectCount }} </a-descriptions-item>
              <a-descriptions-item label="脚本数">{{ machineInfo && machineInfo.jpomScriptCount }} </a-descriptions-item>
              <a-descriptions-item label="网络延迟">{{ formatDuration(machineInfo && machineInfo.networkDelay) }} </a-descriptions-item>
              <a-descriptions-item></a-descriptions-item>
              <a-descriptions-item></a-descriptions-item>
              <a-descriptions-item label="硬盘占用" :span="4">
                <a-progress
                  :stroke-color="{
                    '0%': '#87d068',
                    '100%': 'red',
                  }"
                  :percent="formatPercent2Number((machineInfo && machineInfo.osOccupyDisk) || 0)"
                />
              </a-descriptions-item>
              <a-descriptions-item label="内存占用" :span="4">
                <a-progress
                  :stroke-color="{
                    '0%': '#87d068',
                    '100%': 'red',
                  }"
                  :percent="formatPercent2Number((machineInfo && machineInfo.osOccupyMemory) || 0)"
                />
              </a-descriptions-item>
              <a-descriptions-item label="CPU占用" :span="4">
                <a-progress
                  :stroke-color="{
                    '0%': '#87d068',
                    '100%': 'red',
                  }"
                  :percent="formatPercent2Number((machineInfo && machineInfo.osOccupyCpu) || 0)"
                />
              </a-descriptions-item>
            </a-descriptions>
          </a-space>
        </a-card>
      </a-tab-pane>
      <a-tab-pane key="stat" tab="统计趋势">
        <a-space direction="vertical" style="display: block">
          <a-card size="small" title="基础信息">
            <template slot="extra">
              <a-button size="small" v-if="historyChart" type="primary" @click="handleHistory"><a-icon type="area-chart" />历史监控图表</a-button>
            </template>
            <!-- top 图表 -->
            <div id="top-chart" class="chart">loading...</div>
          </a-card>
          <a-card size="small" title="网络流量信息">
            <template slot="extra">
              <a-button size="small" v-if="netHistoryChart" type="primary" @click="handleHistory('network-stat')"><a-icon type="area-chart" />历史监控图表</a-button>
            </template>
            <!-- 网络流量图表 -->
            <div id="net-chart" class="chart">loading...</div>
          </a-card>
          <a-card size="small" title="机器延迟">
            <template slot="extra">
              <a-button size="small" v-if="networkDelayChart" type="primary" @click="handleHistory('networkDelay')"><a-icon type="area-chart" />历史监控图表</a-button>
            </template>
            <!-- 机器延迟 图表 -->
            <div id="network-delay-chart" class="chart">loading...</div>
          </a-card>
        </a-space>
      </a-tab-pane>
      <a-tab-pane key="process" tab="进程">
        <a-card size="small">
          <template #title>
            <a-row>
              <a-col :span="18">
                <a-space>
                  <custom-select
                    class="search-input-item"
                    selStyle="width: 200px !important"
                    @change="loadNodeProcess"
                    @addOption="addNodeProcess"
                    v-model="processSearch.processName"
                    :data="processNames"
                    :popupContainerParent="false"
                    inputPlaceholder="自定义进程类型"
                    selectPlaceholder="选择进程名"
                    suffixIcon=""
                  >
                    <template slot="suffixIcon"> <a-icon type="down" /></template>
                  </custom-select>
                  <a-tooltip title="查看的进程数量">
                    <a-input-number v-model="processSearch.processCount" :min="1" @change="loadNodeProcess" />
                  </a-tooltip>
                  <a-tooltip title="重置自定义的进程名信息">
                    <a-icon type="rest" @click="restProcessNames" />
                  </a-tooltip>
                </a-space>
              </a-col>
            </a-row>
          </template>

          <a-table size="middle" :locale="tableLocale" :loading="loading" :columns="processColumns" :data-source="processList" bordered rowKey="processId" :pagination="false">
            <a-tooltip slot="percentTooltip" slot-scope="text" placement="topLeft" :title="formatPercent(text)">
              {{ formatPercent(text) }}
            </a-tooltip>
            <a-tooltip slot="timeTooltip" slot-scope="text" placement="topLeft" :title="parseTime(text)">
              {{ parseTime(text) }}
            </a-tooltip>
            <a-tooltip slot="durationTooltip" slot-scope="text" placement="topLeft" :title="formatDuration(text)">
              {{ formatDuration(text) }}
            </a-tooltip>

            <a-tooltip slot="sizeTooltip" slot-scope="text" placement="topLeft" :title="renderSize(text)">
              {{ renderSize(text) }}
            </a-tooltip>
            <a-tooltip slot="tooltip" slot-scope="text" placement="topLeft" :title="text">
              {{ text }}
            </a-tooltip>
            <template slot="operation" slot-scope="text, record">
              <a-button type="primary" size="small" @click="kill(record)">Kill</a-button>
            </template>
          </a-table>
        </a-card>
      </a-tab-pane>
      <a-tab-pane key="disk" tab="硬盘">
        <a-table size="middle" :locale="tableLocale" :loading="diskLoading" :columns="diskColumns" :data-source="diskList" bordered rowKey="uuid" :pagination="false">
          <a-tooltip slot="percentTooltip" slot-scope="text" placement="topLeft" :title="formatPercent(text)">
            {{ formatPercent(text) }}
          </a-tooltip>

          <a-tooltip slot="sizeTooltip" slot-scope="text" placement="topLeft" :title="renderSize(text)">
            {{ renderSize(text) }}
          </a-tooltip>
          <a-tooltip slot="tooltip" slot-scope="text" placement="topLeft" :title="text">
            {{ text }}
          </a-tooltip>
          <template slot="operation" slot-scope="text, record">
            <a-button type="primary" size="small" @click="kill(record)">Kill</a-button>
          </template>
        </a-table>
      </a-tab-pane>
    </a-tabs>

    <!-- 历史监控 -->
    <a-modal destroyOnClose v-model="monitorVisible.visible" width="75%" title="历史监控图表" :footer="null" :maskClosable="false">
      <node-top v-if="monitorVisible && monitorVisible.visible" :nodeId="this.nodeId" :machineId="this.machineId" :type="monitorVisible.type"></node-top>
    </a-modal>
  </div>
</template>
<script>
import { nodeMonitorData, getProcessList, killPid } from "@/api/node";
import { renderSize, formatPercent, parseTime, formatDuration, formatPercent2Number } from "@/utils/const";
import CustomSelect from "@/components/customSelect";
import NodeTop from "@/pages/node/node-layout/node-top";
import { generateNodeTopChart, drawChart, machineInfo, generateNodeNetChart, machineDiskInfo, generateNodeNetworkTimeChart } from "@/api/node-stat";
import { statusMap } from "@/api/system/assets-machine";

export default {
  components: {
    CustomSelect,
    NodeTop,
  },
  props: {
    nodeId: {
      type: String,
    },
    machineId: {
      type: String,
    },
  },
  computed: {
    idInfo() {
      return {
        nodeId: this.nodeId,
        machineId: this.machineId,
      };
    },
  },
  data() {
    return {
      loading: false,
      diskLoading: false,
      tableLocale: {
        emptyText: "",
      },
      statusMap,
      processList: [],
      diskList: [],
      defaultProcessNames: ["java", "python", "mysql", "php", "docker"],
      processNames: [],
      monitorVisible: {
        visible: false,
        type: "",
      },
      timeRange: "",
      historyData: [],
      processSearch: {
        processName: "java",
        processCount: 20,
      },
      processColumns: [
        { title: "ID", dataIndex: "processId", width: "80px", ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "名称", dataIndex: "name", width: "80px", ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "端口", dataIndex: "port", width: "100px", ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "所有者", dataIndex: "user", width: "100px", ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "项目名称", dataIndex: "jpomName", width: "100px", ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "状态", dataIndex: "state", width: "80px", ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "虚拟内存", dataIndex: "virtualSize", width: "100px", ellipsis: true, scopedSlots: { customRender: "sizeTooltip" } },
        { title: "CPU", dataIndex: "processCpuLoadCumulative", width: "100px", ellipsis: true, scopedSlots: { customRender: "percentTooltip" } },
        { title: "驻留集", dataIndex: "residentSetSize", width: "100px", ellipsis: true, scopedSlots: { customRender: "sizeTooltip" } },
        { title: "优先级", dataIndex: "priority", width: "80px", ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "启动时间", dataIndex: "startTime", width: "180px", ellipsis: true, scopedSlots: { customRender: "timeTooltip" } },
        { title: "运行时间", dataIndex: "upTime", width: "100px", ellipsis: true, scopedSlots: { customRender: "durationTooltip" } },
        { title: "用户时间", dataIndex: "userTime", width: "100px", ellipsis: true, scopedSlots: { customRender: "durationTooltip" } },
        { title: "路径", dataIndex: "path", width: "180px", ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "启动命令", dataIndex: "commandLine", width: "180px", ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "操作", dataIndex: "operation", scopedSlots: { customRender: "operation" }, align: "center", width: "80px", fixed: "right" },
      ],
      diskColumns: [
        // { title: "ID", dataIndex: "processId", width: "80px", ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "名称", dataIndex: "name", ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "卷", dataIndex: "mount", ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "文件系统类型", dataIndex: "type", ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "描述", dataIndex: "description", ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "剩余空间(未分配)", dataIndex: "freeSpace", ellipsis: true, scopedSlots: { customRender: "sizeTooltip" } },
        { title: "剩余空间", dataIndex: "usableSpace", ellipsis: true, scopedSlots: { customRender: "sizeTooltip" } },
        { title: "总空间", dataIndex: "totalSpace", ellipsis: true, scopedSlots: { customRender: "sizeTooltip" } },
        { title: "剩余 inode 数", dataIndex: "freeInodes", ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "总 inode 数", dataIndex: "totalInodes", ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "选项", dataIndex: "options", ellipsis: true, scopedSlots: { customRender: "tooltip" } },
      ],
      refreshInterval: 5,
      historyChart: null,
      netHistoryChart: null,
      networkDelayChart: null,
      countdownTime: Date.now(),
      machineInfo: null,
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
    formatPercent,
    parseTime,
    formatDuration,
    renderSize,
    formatPercent2Number,
    getMachineInfo() {
      machineInfo({ ...this.idInfo }).then((res) => {
        //
        if (res.data) {
          this.machineInfo = res.data.data;
          if (this.machineInfo) {
            let ipListStr = (this.machineInfo && this.machineInfo.hostIpv4s) || "";
            this.machineInfo = { ...this.machineInfo, ipv4List: ipListStr.length ? ipListStr.split(",") : "" };
          }
          this.refreshInterval = res.data.heartSecond;
        }
      });
    },
    addNodeProcess(v) {
      this.processNames = v;
      this.cacheNodeProcess();
    },
    restProcessNames() {
      this.processNames = this.defaultProcessNames;
      this.processSearch = {
        processName: this.defaultProcessNames[0],
        processCount: 20,
      };
      this.cacheNodeProcess();
      this.loadNodeProcess();
    },
    // 初始化页面
    initData() {
      const nodeCache = this.getCacheNodeProcess();

      this.processSearch = { ...this.processSearch, processName: nodeCache?.processName || this.processSearch.processName, processCount: nodeCache?.processCount || this.processSearch.processCount };
      this.processNames = nodeCache?.processNames || this.processNames;
      // 加载缓存信息
      //this.refreshInterval = this.getCacheNode("refreshInterval", this.refreshInterval);
      //
      this.pullNodeData();
    },
    pullNodeData() {
      this.loadNodeTop();
      this.loadNodeProcess();
      this.getMachineInfo();
      this.getMachineDiskInfo();
      // 重新计算倒计时
      this.countdownTime = Date.now() + this.refreshInterval * 1000;
    },
    changeTabs(activeKey) {
      if (activeKey === "stat") {
        this.loadNodeTop();
      }
    },
    resize() {
      this.historyChart?.resize();
      this.netHistoryChart?.resize();
    },
    // 请求 top 命令绘制图表
    loadNodeTop() {
      nodeMonitorData({ ...this.idInfo }, false).then((res) => {
        if (res.code === 200) {
          this.historyChart = drawChart(res.data, "top-chart", generateNodeTopChart);
          this.netHistoryChart = drawChart(res.data, "net-chart", generateNodeNetChart);
          this.networkDelayChart = drawChart(res.data, "network-delay-chart", generateNodeNetworkTimeChart);
        }
      });
    },
    // 加载节点进程列表
    loadNodeProcess(v) {
      this.loading = this.processList.length <= 0;
      getProcessList({
        ...this.idInfo,
        processName: this.processSearch.processName,
        count: this.processSearch.processCount,
      }).then((res) => {
        if (res.code === 200) {
          this.processList = res.data;
        } else {
          this.processList = [];
        }
        this.tableLocale.emptyText = "没有找到任何进程";
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
            ...this.idInfo,
            pid: record.processId,
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
    handleHistory(type) {
      this.monitorVisible = {
        visible: true,
        type: type,
      };
    },
    cacheNodeProcess() {
      const cacheJson = this.getCacheAllNode();
      // console.log(cacheJson);
      const cacheId = this.nodeId || this.machineId;
      cacheJson[cacheId] = cacheJson[cacheId] || {};
      cacheJson[cacheId].processNames = this.processNames;
      cacheJson[cacheId].processName = this.processSearch.processName;
      cacheJson[cacheId].processCount = this.processSearch.processCount;
      //cacheJson["refreshInterval"] = this.refreshInterval;
      localStorage.setItem("node-process-name", JSON.stringify(cacheJson));
    },
    getCacheNodeProcess() {
      const cacheId = this.nodeId || this.machineId;
      return this.getCacheNode(cacheId, {});
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
    getMachineDiskInfo() {
      this.diskLoading = !this.diskList || this.diskList.length <= 0;
      machineDiskInfo({ ...this.idInfo }).then((res) => {
        this.diskList = res.data;
        if (this.diskList) {
          this.diskLoading = false;
        }
      });
    },
  },
};
</script>
<style scoped>
.chart {
  height: 30vh;
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
