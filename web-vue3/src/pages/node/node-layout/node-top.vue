<template>
  <div>
    <div ref="filter" class="filter">
      <a-space>
        <a-range-picker
          :presets="[
            { label: '今天', value: [dayjs().startOf('day'), dayjs()] },
            { label: '昨天', value: [dayjs().add(-1, 'days').startOf('day'), dayjs().add(-1, 'days').endOf('day')] }
          ]"
          :disabled-date="
            (current) => {
              return current && current >= dayjs().endOf('day')
            }
          "
          class="filter-item"
          v-model:value="timeRange"
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
import { nodeMonitorData } from '@/api/node'
import { drawChart, generateNodeTopChart, generateNodeNetworkTimeChart, generateNodeNetChart } from '@/api/node-stat'
import dayjs from 'dayjs'
export default {
  components: {},
  props: {
    nodeId: {
      type: String
    },
    machineId: {
      type: String
    },
    type: {
      type: String
    }
  },
  data() {
    return {
      timeRange: null,
      historyData: [],
      historyChart: null
    }
  },
  mounted() {
    this.handleFilter()
    window.addEventListener('resize', this.resize)
  },
  unmounted() {
    window.removeEventListener('resize', this.resize)
  },
  watch: {},
  methods: {
    dayjs,
    // 刷新
    handleFilter() {
      const params = {
        nodeId: this.nodeId,
        machineId: this.machineId,
        time: this.timeRange
      }
      // 加载数据
      nodeMonitorData(params).then((res) => {
        if (res.code === 200) {
          if (this.type === 'networkDelay') {
            this.historyChart = drawChart(res.data, 'historyChart', generateNodeNetworkTimeChart)
          } else if (this.type === 'network-stat') {
            this.historyChart = drawChart(res.data, 'historyChart', generateNodeNetChart)
          } else {
            this.historyChart = drawChart(res.data, 'historyChart', generateNodeTopChart)
          }
        }
      })
    },
    resize() {
      this.historyChart?.resize()
    }
  }
}
</script>

<style scoped>
.historyChart {
  height: 50vh;
}
</style>
