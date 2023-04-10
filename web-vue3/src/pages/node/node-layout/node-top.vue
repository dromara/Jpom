<template>
  <div>
    <div ref="filter" class="filter">
      <a-space>
        <a-range-picker
          :ranges="{ 今天: [moment().startOf('day'), moment()], 昨天: [moment().add(-1, 'days').startOf('day'), moment().add(-1, 'days').endOf('day')] }"
          :disabled-date="
            (current) => {
              return current && current >= moment().endOf('day');
            }
          "
          class="filter-item"
          v-model="timeRange"
          :show-time="{ format: 'HH:mm:ss' }"
          format="YYYY-MM-DD HH:mm:ss"
          valueFormat="YYYY-MM-DD HH:mm:ss"
        />
        <a-button type="primary" @click="handleFilter">搜索</a-button>
      </a-space>
    </div>
    <div id="historyChart" class="historyChart">loading...</div>
  </div>
</template>
<script>
import { nodeMonitorData } from "@/api/node";
import { drawChart, generateNodeTopChart, generateNodeNetworkTimeChart, generateNodeNetChart } from "@/api/node-stat";
import moment from "moment";
import * as echarts from "echarts";

export default {
  components: {},
  props: {
    nodeId: {
      type: String,
    },
    machineId: {
      type: String,
    },
    type: {
      type: String,
    },
  },
  data() {
    return {
      timeRange: null,
      historyData: [],
      historyChart: null,
    };
  },
  mounted() {
    this.handleFilter();
    window.addEventListener("resize", this.resize);
  },
  destroyed() {
    window.removeEventListener("resize", this.resize);
  },
  watch: {},
  methods: {
    moment,
    // 刷新
    handleFilter() {
      const params = {
        nodeId: this.nodeId,
        machineId: this.machineId,
        time: this.timeRange,
      };
      // 加载数据
      nodeMonitorData(params).then((res) => {
        if (res.code === 200) {
          if (this.type === "networkDelay") {
            this.historyChart = drawChart(echarts, res.data, "historyChart", generateNodeNetworkTimeChart);
          } else if (this.type === "network-stat") {
            this.historyChart = drawChart(echarts, res.data, "historyChart", generateNodeNetChart);
          } else {
            this.historyChart = drawChart(echarts, res.data, "historyChart", generateNodeTopChart);
          }
        }
      });
    },
    resize() {
      this.historyChart?.resize();
    },
  },
};
</script>
<style scoped>
.historyChart {
  height: 50vh;
}
</style>
