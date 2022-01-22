<template>
  <div>
    <div ref="filter" class="filter">
      <a-space>
        <a-range-picker class="filter-item" v-model="timeRange" :show-time="{ format: 'HH:mm:ss' }" format="YYYY-MM-DD HH:mm:ss" valueFormat="YYYY-MM-DD HH:mm:ss" />
        <a-button type="primary" @click="handleFilter">搜索</a-button>
      </a-space>
    </div>
    <div id="historyChart" class="historyChart">loading...</div>
  </div>
</template>
<script>
import { nodeMonitorData } from "@/api/node";
import echarts from "echarts";
import { generateChart } from "@/api/node-stat";

export default {
  components: {},
  props: {
    nodeId: {
      type: String,
    },
  },
  data() {
    return {
      timeRange: null,
      historyData: [],
    };
  },
  mounted() {
    this.handleFilter();
  },
  destroyed() {},
  watch: {},
  methods: {
    // 刷新
    handleFilter() {
      const params = {
        nodeId: this.nodeId,
        time: this.timeRange,
      };
      // 加载数据
      nodeMonitorData(params).then((res) => {
        if (res.code === 200) {
          this.drawHistoryChart(res.data);
        }
      });
    },

    // 画历史图表
    drawHistoryChart(historyData) {
      const historyChartDom = document.getElementById("historyChart");
      if (!historyChartDom) {
        return;
      }
      const option = generateChart(historyData);

      // 绘制图表
      const historyChart = echarts.init(historyChartDom);
      historyChart.setOption(option);
    },
  },
};
</script>
<style scoped>
.historyChart {
  height: 50vh;
}
</style>
