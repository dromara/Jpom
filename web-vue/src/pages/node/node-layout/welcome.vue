<template>
  <div>
    <!-- top 图表 -->
    <div id="top-chart">...</div>
  </div>
</template>
<script>
import { getNodeTop } from '../../../api/node';
import echarts from 'echarts';
export default {
  props: {
    node: {
      type: Object
    }
  },
  data() {
    return {
      topData: {},
      topChartTimer: null
    }
  },
  mounted() {
    this.initData();
  },
  destroyed() {
    clearInterval(this.topChartTimer);
  },
  methods: {
    // 初始化页面
    initData() {
      if (this.topChartTimer == null) {
        this.loadNodeTop();
        // 计算多久时间绘制图表
        const millis = this.node.cycle < 30000 ? 30000 : this.node.cycle;
        this.topChartTimer = setInterval(() => {
          this.loadNodeTop();
        }, millis);
      }
    },
    // 请求 top 命令绘制图表
    loadNodeTop() {
      getNodeTop(this.node.id).then(res => {
        if (res.code === 200) {
          this.topData = res.data;
        }
        this.drawTopChart();
      })
    },
    // 绘制 top 图表
    drawTopChart() {
      let cpuItem = {
          name: 'cpu占用',
          type: 'line',
          data: [],
          showSymbol: false,
          // 设置折线为曲线
          smooth: true
      };
      let diskItem = {
          name: '磁盘占用',
          type: 'line',
          data: [],
          showSymbol: false,
          smooth: true
      };
      let memoryItem = {
          name: '内存占用',
          type: 'line',
          data: [],
          showSymbol: false,
          smooth: true
      };
      this.topData.series.forEach(item => {
        cpuItem.data.push(parseFloat(item.cpu));
        diskItem.data.push(parseFloat(item.disk));
        memoryItem.data.push(parseFloat(item.memory));
      })
      let series = [cpuItem, memoryItem, diskItem];
      // 指定图表的配置项和数据
      let option = {
        title: {
          text: "系统 Top 监控"
        },
        tooltip: {
          trigger: 'axis'
        },
        legend: {
          data: ['cpu占用', '内存占用', '磁盘占用']
        },
        grid: {
          left: '1%',
          right: '2%',
          bottom: '1%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: this.topData.scales
        },
        calculable: true,
        yAxis: {
          type: 'value',
          axisLabel: {
            // 设置y轴数值为%
            formatter: '{value} %'
          },
          max: 100
        },
        dataZoom: [
          { type: 'inside' }, { type: 'slider' }
        ],
        series: series
      };
      // 绘制图表
      const topChart = echarts.init(document.getElementById('top-chart'));
      topChart.setOption(option);
    },
  }
}
</script>
<style scoped>
#top-chart {
  height: calc((100vh - 70px) / 2) ;
}
</style>