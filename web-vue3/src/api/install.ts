import axios from "./config";

// 检查是否需要初始化系统
export function checkSystem() {
  return axios({
    url: "/check-system",
    method: "post",
    headers: {
      loading: "no",
    },
  });
}

/**
 * 初始化系统
 * @param {
 *  userName: 登录名
 *  userPwd: 初始密码
 * } params
 */
export function initInstall(params) {
  return axios({
    url: "/install_submit.json",
    method: "post",
    data: params,
  });
}
