import axios from "./config";

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

export function generateChart(data) {
  let cpuItem = {
    name: "cpu占用",
    type: "line",
    data: [],
    showSymbol: false,
    // 设置折线为曲线
    smooth: true,
  };
  let diskItem = {
    name: "磁盘占用",
    type: "line",
    data: [],
    showSymbol: false,
    smooth: true,
  };
  let memoryItem = {
    name: "内存占用(累计)",
    type: "line",
    data: [],
    showSymbol: false,
    smooth: true,
  };
  let memoryUsedItem = {
    name: "内存占用",
    type: "line",
    data: [],
    showSymbol: false,
    smooth: true,
  };
  data.series.forEach((item) => {
    cpuItem.data.push(parseFloat(item.cpu));
    diskItem.data.push(parseFloat(item.disk));
    memoryItem.data.push(parseFloat(item.memory));
    if (item.memoryUsed) {
      memoryUsedItem.data.push(parseFloat(item.memoryUsed));
    }
  });
  let series = [cpuItem, memoryItem, diskItem];
  if (memoryUsedItem.data.length > 0) {
    series.push(memoryUsedItem);
  }
  let legends = series.map((data) => {
    return data.name;
  });
  // 指定图表的配置项和数据
  return {
    title: {
      text: "系统 Top 监控",
    },
    tooltip: {
      trigger: "axis",
    },
    legend: {
      data: legends,
    },
    grid: {
      left: "1%",
      right: "2%",
      bottom: "1%",
      containLabel: true,
    },
    xAxis: {
      type: "category",
      boundaryGap: false,
      data: data.scales,
    },
    calculable: true,
    yAxis: {
      type: "value",
      axisLabel: {
        // 设置y轴数值为%
        formatter: "{value} %",
      },
      max: 100,
    },
    dataZoom: [{ type: "inside" }, { type: "slider" }],
    series: series,
  };
}

export const status = {
  1: "无法连接",
  0: "正常",
  2: "授权信息错误",
  3: "状态码错误",
};

// export const nodeMonitorCycle = {
//   "-30": "30 秒",
//   1: "1 分钟",
//   5: "5 分钟",
//   10: "10 分钟",
//   30: "30 分钟",
// };
