<template>
  <div>
    <div ref="filter" class="filter">
      <a-space>
        <a-range-picker
          v-model:value="timeRange"
          :presets="[
            { label: $t('i18n_800dfdd902'), value: [dayjs().startOf('day'), dayjs()] },
            {
              label: $t('i18n_2f8d6f1584'),
              value: [dayjs().add(-1, 'days').startOf('day'), dayjs().add(-1, 'days').endOf('day')]
            }
          ]"
          :disabled-date="
            (current) => {
              return current && current >= dayjs().endOf('day')
            }
          "
          class="filter-item"
          :show-time="{ format: 'HH:mm:ss' }"
          format="YYYY-MM-DD HH:mm:ss"
          value-format="YYYY-MM-DD HH:mm:ss"
        />
        <a-button type="primary" @click="handleFilter">{{ $t('i18n_e5f71fc31e') }}</a-button>
        <a-tooltip>
          <template #title>
            <div>
              <ul>
                <li>{{ $t('i18n_b11b0c93fa') }}</li>
                <li>{{ $t('i18n_413f20d47f') }}</li>
                <li>{{ $t('i18n_7617455241') }}</li>
                <li>{{ $t('i18n_c8b2aabc07') }}</li>
              </ul>
            </div>
          </template>
          <QuestionCircleOutlined />
        </a-tooltip>
      </a-space>
    </div>
    <div v-if="nodeMonitorLoadStatus == 1" id="historyChart" class="historyChart">loading...</div>
    <a-empty
      v-else-if="nodeMonitorLoadStatus == -1"
      :image="Empty.PRESENTED_IMAGE_SIMPLE"
      :description="$t('i18n_85be08c99a')"
    >
    </a-empty>
    <a-skeleton v-else />
  </div>
</template>
<script>
import { nodeMonitorData } from '@/api/node'
import { drawChart, generateNodeTopChart, generateNodeNetworkTimeChart, generateNodeNetChart } from '@/api/node-stat'
import dayjs from 'dayjs'
import { useGuideStore } from '@/stores/guide'
import { mapState } from 'pinia'
import { Empty } from 'ant-design-vue'
export default {
  components: {},
  props: {
    nodeId: {
      type: String,
      default: ''
    },
    machineId: {
      type: String,
      default: ''
    },
    type: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      Empty,
      timeRange: null,
      historyData: [],
      historyChart: null,
      nodeMonitorLoadStatus: 0
    }
  },
  computed: {
    ...mapState(useGuideStore, ['getThemeView'])
  },
  watch: {},
  mounted() {
    this.handleFilter()
    window.addEventListener('resize', this.resize)
  },
  unmounted() {
    window.removeEventListener('resize', this.resize)
  },
  methods: {
    dayjs,
    // 刷新
    handleFilter() {
      const params = {
        nodeId: this.nodeId,
        machineId: this.machineId
        // time: this.timeRange
      }
      if (this.timeRange && this.timeRange[0]) {
        params.startTime = this.timeRange[0]
        params.endTime = this.timeRange[1]
      } else {
        params.startTime = ''
        params.endTime = ''
      }
      // 加载数据
      nodeMonitorData(params)
        .then((res) => {
          if (res.code === 200) {
            if (res.data && res.data.length) {
              this.nodeMonitorLoadStatus = 1
              this.$nextTick(() => {
                if (this.type === 'networkDelay') {
                  this.historyChart = drawChart(
                    res.data,
                    'historyChart',
                    generateNodeNetworkTimeChart,
                    this.getThemeView()
                  )
                } else if (this.type === 'network-stat') {
                  this.historyChart = drawChart(res.data, 'historyChart', generateNodeNetChart, this.getThemeView())
                } else {
                  this.historyChart = drawChart(res.data, 'historyChart', generateNodeTopChart, this.getThemeView())
                }
              })

              return
            }
          }
          this.nodeMonitorLoadStatus = -1
        })
        .catch(() => {
          this.nodeMonitorLoadStatus = -1
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
  margin-top: 10px;
}
</style>
