import axios from './config'
import { parseTime, formatPercent2, renderSize, formatDuration } from '@/utils/const'
import * as echarts from 'echarts'

// 获取机器信息
export function machineInfo(params) {
  return axios({
    url: '/node/machine-info',
    method: 'get',
    params: params,
    headers: {
      loading: 'no'
    }
  })
}

// 机器文件系统
export function machineDiskInfo(params) {
  return axios({
    url: '/node/disk-info',
    method: 'get',
    params,
    headers: {
      loading: 'no'
    }
  })
}

// 机器硬件硬盘
export function machineHwDiskInfo(params) {
  return axios({
    url: '/node/hw-disk-info',
    method: 'get',
    params,
    headers: {
      loading: 'no'
    }
  })
}

const defaultData = {
  title: {
    // text: "系统 Top 监控",
  },
  tooltip: {
    trigger: 'axis'
  },
  legend: {
    // data: legends,
  },
  color: ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452', '#9a60b4', '#ea7ccc'],
  grid: {
    left: '1%',
    right: '2%',
    bottom: '1%',
    containLabel: true
  },
  xAxis: {
    type: 'category',
    boundaryGap: false
    // data: scales,
  },
  calculable: true,
  // yAxis: {
  //   type: "value",
  //   axisLabel: {
  //     // 设置y轴数值为%
  //     formatter: "{value} %",
  //   },
  //   max: 100,
  // },
  dataZoom: [{ type: 'inside' }, { type: 'slider' }]
  // series: series,
}

/**
 * 节点系统统计
 * @param { JSON } data
 * @returns
 */
export function generateNodeTopChart(data) {
  const cpuItem = {
    name: 'cpu占用',
    type: 'line',
    data: [],
    showSymbol: false,
    // 设置折线为曲线
    smooth: true
  }
  const diskItem = {
    name: '磁盘占用',
    type: 'line',
    data: [],
    showSymbol: false,
    smooth: true
  }
  const memoryItem = {
    name: '内存占用',
    type: 'line',
    data: [],
    showSymbol: false,
    smooth: true
  }
  const virtualMemory = {
    name: '虚拟内存占用',
    type: 'line',
    data: [],
    showSymbol: false,
    smooth: true
  }
  const swapMemory = {
    name: '交互内存占用',
    type: 'line',
    data: [],
    showSymbol: false,
    smooth: true
  }
  const scales = []
  for (var i = data.length - 1; i >= 0; i--) {
    const item = data[i]
    cpuItem.data.push(parseFloat(item.occupyCpu))
    diskItem.data.push(parseFloat(item.occupyDisk))
    memoryItem.data.push(parseFloat(item.occupyMemory))
    swapMemory.data.push(parseFloat(item.occupySwapMemory || -0.1))
    virtualMemory.data.push(parseFloat(item.occupyVirtualMemory || -0.1))

    scales.push(parseTime(item.monitorTime))
  }

  const series = [cpuItem, memoryItem, diskItem, swapMemory, virtualMemory]

  const legends = series.map((data) => {
    return data.name
  })

  // 指定图表的配置项和数据
  return Object.assign({}, defaultData, {
    legend: {
      data: legends
    },
    xAxis: {
      data: scales
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        // 设置y轴数值为%
        formatter: '{value} %'
      },
      max: 100
    },
    tooltip: {
      trigger: 'axis',
      show: true,
      formatter: function (params) {
        var html = params[0].name + '<br>'
        for (var i = 0; i < params.length; i++) {
          html += params[i].marker + params[i].seriesName + ':' + formatPercent2(params[i].value) + '<br>'
        }
        return html
      }
    },
    series: series
  })
}

/**
 * 节点网络统计
 * @param { JSON } data
 * @returns
 */
export function generateNodeNetChart(data) {
  const rxItem = {
    name: '接收',
    type: 'line',
    data: [],
    showSymbol: false,
    // 设置折线为曲线
    smooth: true
  }
  const txItem = {
    name: '发送',
    type: 'line',
    data: [],
    showSymbol: false,
    smooth: true
  }
  const scales = []
  for (var i = data.length - 1; i >= 0; i--) {
    const item = data[i]
    txItem.data.push(item.netTxBytes)
    rxItem.data.push(item.netRxBytes)

    scales.push(parseTime(item.monitorTime))
  }

  const series = [rxItem, txItem]

  const legends = series.map((data) => {
    return data.name
  })

  // 指定图表的配置项和数据
  return Object.assign({}, defaultData, {
    legend: {
      data: legends
    },
    xAxis: {
      data: scales
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        // 设置y轴数值为 bit/s
        // formatter: "{value} bit/s",
        formatter: (value) => {
          return renderSize(value)
        }
      }
    },
    tooltip: {
      trigger: 'axis',
      show: true,
      formatter: function (params) {
        var html = params[0].name + '<br>'
        for (var i = 0; i < params.length; i++) {
          html += params[i].marker + params[i].seriesName + ':' + renderSize(params[i].value) + '/s <br>'
        }
        return html
      }
    },
    series: series
  })
}

/**
 * 节点网络延迟
 * @param { JSON } data
 * @returns
 */
export function generateNodeNetworkTimeChart(data) {
  const dataArray = {
    name: '网络延迟',
    type: 'line',
    data: [],
    showSymbol: false,
    // 设置折线为曲线
    smooth: true
  }
  const scales = []
  for (var i = data.length - 1; i >= 0; i--) {
    const item = data[i]
    dataArray.data.push(parseFloat(item.networkDelay))
    scales.push(parseTime(item.monitorTime))
  }

  const series = [dataArray]

  const legends = series.map((data) => {
    return data.name
  })
  // 指定图表的配置项和数据
  return Object.assign({}, defaultData, {
    legend: {
      data: legends
    },
    xAxis: {
      data: scales
    },
    yAxis: {
      type: 'value',
      axisLabel: {
        // formatter: "{value} ms",
        formatter: (value) => {
          return formatDuration(value)
        }
      }
    },
    tooltip: {
      trigger: 'axis',
      show: true,
      formatter: function (params) {
        var html = params[0].name + '<br>'
        for (var i = 0; i < params.length; i++) {
          html += params[i].marker + params[i].seriesName + ':' + formatDuration(params[i].value) + ' <br>'
        }
        return html
      }
    },
    series: series
  })
}

/**
 *
 * @param {*} data
 * @param {String} domId
 * @returns
 */
export function drawChart(data, domId, parseFn) {
  const historyChartDom = document.getElementById(domId, domId)
  if (!historyChartDom) {
    return
  }
  const option = parseFn(data)
  let myChart = echarts.getInstanceByDom(historyChartDom)
  if (myChart) {
    myChart.setOption(option)
    return myChart
  }
  // 绘制图表
  const historyChart = echarts.init(historyChartDom)
  historyChart.setOption(option)
  return historyChart
}

// export const status = {
//   1: "无法连接",
//   0: "正常",
//   2: "授权信息错误",
//   3: "状态码错误",
//   4: "关闭中",
// };

// 机器网络
export function machineNetworkInterfaces(params) {
  return axios({
    url: '/node/network-interfaces',
    method: 'get',
    params,
    headers: {
      loading: 'no'
    }
  })
}
