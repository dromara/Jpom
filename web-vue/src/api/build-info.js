import axios from "./config";
import { loadRouterBase } from "./config";
import i18n from "@/locales";

/**
 * 构建列表
 * @param {
 *  group: 分组名称
 * } params
 */
export function getBuildList(params, loading) {
  return axios({
    url: "/build/list",
    method: "post",
    data: params,
    headers: {
      loading: loading === false ? "no" : "",
    },
  });
}

/**
 * 构建详情
 * @param {
 *
 * } params
 */
export function getBuildGet(params) {
  return axios({
    url: "/build/get",
    method: "get",
    params,
  });
}

/**
 * 构建分组
 */
export function getBuildGroupAll() {
  return axios({
    url: "/build/list_group_all",
    method: "get",
  });
}

/**
 * 获取仓库分支信息
 * @param {
 *  repositoryId: 仓库id
 * } params
 */
export function getBranchList(params) {
  return axios({
    url: "/build/branch-list",
    method: "post",
    timeout: 0,
    data: params,
    headers: {
      loadingTip: i18n.t("api.buildInfo.loadingBranchList"),
    },
  });
}

/**
 * 编辑构建信息
 * @param {
 *  id: 构建 ID
 *  name: 构建名称
 *  group: 分组名称
 *  branchName: 分支名称
 *  branchTagName: 标签
 *  script: 构建命令
 *  resultDirFile: 构建产物目录
 *  releaseMethod: 发布方法
 *  extraData: 额外信息 JSON 字符串
 *  repostitoryId: 仓库信息
 * } params
 */
export function editBuild(params) {
  const data = {
    id: params.id,
    name: params.name,
    repositoryId: params.repositoryId,
    resultDirFile: params.resultDirFile,
    script: params.script,
    releaseMethod: params.releaseMethod,
    branchName: params.branchName,
    branchTagName: params.branchTagName,
    group: params.group,
    repoType: params.repoType,
    // 其他参数
    extraData: params.extraData,
    webhook: params.webhook,
    autoBuildCron: params.autoBuildCron,
    buildMode: params.buildMode,
    aliasCode: params.aliasCode,
    resultKeepDay: params.resultKeepDay,
  };
  return axios({
    url: "/build/edit",
    method: "post",
    data,
  });
}

/**
 * 删除构建信息
 * @param {*} id
 */
export function deleteBuild(id) {
  return axios({
    url: "/build/delete",
    method: "post",
    data: { id },
  });
}

/**
 * 获取触发器地址
 * @param {*} id
 */
export function getTriggerUrl(data) {
  return axios({
    url: "/build/trigger/url",
    method: "post",
    data: data,
  });
}

// /**
//  * 重置触发器
//  * @param {*} id
//  */
// export function resetTrigger(id) {
//   return axios({
//     url: "/build/trigger/rest",
//     method: "post",
//     data: { id },
//   });
// }

/**
 * 清理构建
 * @param {*} id
 */
export function clearBuid(id) {
  return axios({
    url: "/build/clean-source",
    method: "post",
    data: { id },
  });
}

/**
 * 查看构建日志
 * @param {JSON} params {
 *  id: 构建 ID
 *  buildId: 构建任务 ID
 *  line: 需要获取的行号 1 开始
 * }
 */
export function loadBuildLog(params) {
  return axios({
    url: "/build/manage/get-now-log",
    method: "post",
    data: params,
    headers: {
      tip: "no",
      loading: "no",
    },
  });
}

/**
 * 开始构建
 * @param {*} id
 */
export function startBuild(data) {
  return axios({
    url: "/build/manage/start",
    method: "post",
    data: data,
  });
}

/**
 * 停止构建
 * @param {*} id
 */
export function stopBuild(id) {
  return axios({
    url: "/build/manage/cancel",
    method: "post",
    data: { id },
  });
}

/**
 * 构建历史
 * @param {
 *  buildDataId: 构建任务 ID
 *  status: 状态
 * } params
 */
export function geteBuildHistory(params) {
  return axios({
    url: "/build/history/history_list.json",
    method: "post",
    data: params,
  });
}

/**
 * 下载构建日志
 * @param {*} logId
 */
export function downloadBuildLog(logId) {
  return loadRouterBase("/build/history/download_log.html", {
    logId: logId,
  });
}

/**
 * 下载构建产物
 * @param {*} logId
 */
export function downloadBuildFile(logId) {
  return loadRouterBase("/build/history/download_file.html", {
    logId: logId,
  });
}

/**
 * 下载构建产物
 * @param {*} logId
 */
export function downloadBuildFileByBuild(id, numberId) {
  return loadRouterBase("/build/history/download_file_by_build", {
    buildId: id,
    buildNumberId: numberId,
  });
}

/**
 * 回滚（重新发布）
 * @param {*} logId
 * @returns
 */
export function rollback(logId) {
  return axios({
    url: "/build/manage/reRelease",
    method: "post",
    data: { logId },
  });
}

/**
 * 删除构建历史记录
 * @param {*} logId
 */
export function deleteBuildHistory(logId) {
  return axios({
    url: "/build/history/delete_log.json",
    method: "post",
    data: { logId },
  });
}

export function sortItem(params) {
  return axios({
    url: "/build/sort-item",
    method: "get",
    params: params,
  });
}

export const statusMap = {
  1: i18n.t("api.buildInfo.status.building"),
  2: i18n.t("api.buildInfo.status.buildCompleted"),
  3: i18n.t("api.buildInfo.status.buildFailed"),
  4: i18n.t("api.buildInfo.status.publishing"),
  5: i18n.t("api.buildInfo.status.publishSucceed"),
  6: i18n.t("api.buildInfo.status.publishFailed"),
  7: i18n.t("api.buildInfo.status.cancelBuild"),
  8: i18n.t("api.buildInfo.status.buildInterrupted"),
  9: i18n.t("api.buildInfo.status.queueWaiting"),
  10: i18n.t("api.buildInfo.status.abnormalShutdown"),
};
export const statusColor = {
  1: "orange",
  2: "green",
  3: "red",
  4: "orange",
  5: "green",
  6: "red",
  7: "",
  8: "blue",
  9: "orange",
  10: "red",
};

export const releaseMethodMap = {
  0: i18n.t("api.buildInfo.releaseMethod.noRelease"),
  1: i18n.t("api.buildInfo.releaseMethod.nodeDistribution"),
  2: i18n.t("api.buildInfo.releaseMethod.project"),
  3: i18n.t("api.buildInfo.releaseMethod.ssh"),
  4: i18n.t("api.buildInfo.releaseMethod.localCommand"),
  5: i18n.t("api.buildInfo.releaseMethod.dockerImage"),
};

export const triggerBuildTypeMap = {
  0: i18n.t("api.buildInfo.triggerBuildType.manual"),
  1: i18n.t("api.buildInfo.triggerBuildType.trigger"),
  2: i18n.t("api.buildInfo.triggerBuildType.cron"),
};

export const buildModeMap = {
  0: i18n.t("api.buildInfo.buildMethod.local"),
  1: i18n.t("api.buildInfo.buildMethod.container"),
};
