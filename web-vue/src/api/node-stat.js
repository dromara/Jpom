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
