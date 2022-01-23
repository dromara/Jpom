import axios from "./config";
import { parseTime } from "@/utils/time";
import echarts from "echarts";

// node 列表
export function getStatist(params) {
  return axios({
    url: "/node/stat/list_data.json",
    method: "post",
    params: params,
    headers: {
      loading: "no",
    },
  });
}

// node 列表
export function statusStat() {
  return axios({
    url: "/node/stat/status_stat.json",
    method: "get",
    headers: {
      loading: "no",
    },
  });
}

const defaultData = {
  title: {
    // text: "系统 Top 监控",
  },
  tooltip: {
    trigger: "axis",
  },
  legend: {
    // data: legends,
  },
  color: ["#5470c6", "#91cc75", "#fac858", "#ee6666", "#73c0de", "#3ba272", "#fc8452", "#9a60b4", "#ea7ccc"],
  grid: {
    left: "1%",
    right: "2%",
    bottom: "1%",
    containLabel: true,
  },
  xAxis: {
    type: "category",
    boundaryGap: false,
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
  dataZoom: [{ type: "inside" }, { type: "slider" }],
  // series: series,
};

/**
 * 节点系统统计
 * @param { JSON } data
 * @returns
 */
export function generateNodeTopChart(data) {
  const cpuItem = {
    name: "cpu占用",
    type: "line",
    data: [],
    showSymbol: false,
    // 设置折线为曲线
    smooth: true,
  };
  const diskItem = {
    name: "磁盘占用",
    type: "line",
    data: [],
    showSymbol: false,
    smooth: true,
  };
  const memoryItem = {
    name: "内存占用(累计)",
    type: "line",
    data: [],
    showSymbol: false,
    smooth: true,
  };
  const memoryUsedItem = {
    name: "内存占用",
    type: "line",
    data: [],
    showSymbol: false,
    smooth: true,
  };
  const scales = [];
  for (var i = data.length - 1; i >= 0; i--) {
    const item = data[i];
    cpuItem.data.push(parseFloat(item.occupyCpu));
    diskItem.data.push(parseFloat(item.occupyDisk));
    memoryItem.data.push(parseFloat(item.occupyMemory));
    if (item.occupyMemoryUsed) {
      memoryUsedItem.data.push(parseFloat(item.occupyMemoryUsed));
    }
    scales.push(parseTime(item.monitorTime));
  }

  const series = [cpuItem, memoryItem, diskItem];
  if (memoryUsedItem.data.length > 0) {
    series.push(memoryUsedItem);
  }
  const legends = series.map((data) => {
    return data.name;
  });

  // 指定图表的配置项和数据
  return Object.assign({}, defaultData, {
    legend: {
      data: legends,
    },
    xAxis: {
      data: scales,
    },
    yAxis: {
      type: "value",
      axisLabel: {
        // 设置y轴数值为%
        formatter: "{value} %",
      },
      max: 100,
    },
    series: series,
  });
}

/**
 * 节点网络延迟
 * @param { JSON } data
 * @returns
 */
export function generateNodeNetworkTimeChart(data) {
  const dataArray = {
    name: "网络延迟ms",
    type: "line",
    data: [],
    showSymbol: false,
    // 设置折线为曲线
    smooth: true,
  };
  const scales = [];
  for (var i = data.length - 1; i >= 0; i--) {
    const item = data[i];
    dataArray.data.push(parseFloat(item.networkTime));
    scales.push(parseTime(item.monitorTime));
  }

  const series = [dataArray];

  const legends = series.map((data) => {
    return data.name;
  });
  // 指定图表的配置项和数据
  return Object.assign({}, defaultData, {
    legend: {
      data: legends,
    },
    xAxis: {
      data: scales,
    },
    yAxis: {
      type: "value",
      axisLabel: {
        formatter: "{value} ms",
      },
    },
    series: series,
  });
}

/**
 *
 * @param {*} data
 * @param {String} domId
 * @returns
 */
export function drawChart(data, domId, parseFn) {
  const historyChartDom = document.getElementById(domId, domId);
  if (!historyChartDom) {
    return;
  }
  const option = parseFn(data);

  // 绘制图表
  const historyChart = echarts.init(historyChartDom);
  historyChart.setOption(option);
}

export const status = {
  1: "无法连接",
  0: "正常",
  2: "授权信息错误",
  3: "状态码错误",
  4: "关闭中",
};
