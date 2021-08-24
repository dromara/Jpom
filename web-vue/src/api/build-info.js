import axios from './config';

// 分组列表
export function getBuildGroupList() {
    return axios({
        url: '/build/group/list',
        method: 'get'
    })
}

/**
 * 构建列表
 * @param {
 *  group: 分组名称
 * } params
 */
export function getBuildList(params) {
    return axios({
        url: '/build/list',
        method: 'post',
        data: params
    })
}

/**
 * 获取仓库分支信息
 * @param {
 *  url: 仓库地址
 *  userName: 用户名
 *  userPwd: 密码
 * } params
 */
export function getBranchList(params) {
    return axios({
        url: '/build/branch-list',
        method: 'post',
        timeout: 0,
        data: params
    })
}

/**
 * 编辑构建信息
 * @param {
 *  id: 构建 ID
 *  name: 构建名称
 *  group: 分组名称
 *  branchName: 分支名称
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
        group: params.group,
        repoType: params.repoType,
        // 其他参数
        extraData: params.extraData,
    }
    return axios({
        url: '/build/edit',
        method: 'post',
        data
    })
}

/**
 * 删除构建信息
 * @param {*} id
 */
export function deleteBuild(id) {
    return axios({
        url: '/build/delete',
        method: 'post',
        data: {id}
    })
}

/**
 * 获取触发器地址
 * @param {*} id
 */
export function getTriggerUrl(id) {
    return axios({
        url: '/build/trigger/url',
        method: 'post',
        data: {id}
    })
}

/**
 * 重置触发器
 * @param {*} id
 */
export function resetTrigger(id) {
    return axios({
        url: '/build/trigger/rest',
        method: 'post',
        data: {id}
    })
}

/**
 * 清理构建
 * @param {*} id
 */
export function clearBuid(id) {
    return axios({
        url: '/build/clean-source',
        method: 'post',
        data: {id}
    })
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
        url: '/build/manage/get-now-log',
        method: 'post',
        data: params,
        headers: {
            tip: 'no',
            loading: 'no'
        },
    })
}

/**
 * 开始构建
 * @param {*} id
 */
export function startBuild(id) {
    return axios({
        url: '/build/manage/start',
        method: 'post',
        data: {id}
    })
}

/**
 * 停止构建
 * @param {*} id
 */
export function stopBuild(id) {
    return axios({
        url: '/build/manage/cancel',
        method: 'post',
        data: {id}
    })
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
        url: '/build/history_list.json',
        method: 'post',
        data: params
    })
}

/**
 * 下载构建日志
 * @param {*} logId
 */
export function downloadBuildLog(logId) {
    return `/build/download_log.html?logId=${logId}`
}

/**
 * 下载构建产物
 * @param {*} logId
 */
export function downloadBuildFile(logId) {
    return `/build/download_file.html?logId=${logId}`
}

/**
 * 回滚（重新发布）
 * @param {*} logId
 * @returns
 */
export function rollback(logId) {
    return axios({
        url: '/build/manage/reRelease',
        method: 'post',
        data: {logId}
    })
}

/**
 * 删除构建历史记录
 * @param {*} logId
 */
export function deleteBuildHistory(logId) {
    return axios({
        url: '/build/delete_log.json',
        method: 'post',
        data: {logId}
    })
}

export const releaseMethodMap = {
    0: '不发布',
    1: '节点分发',
    2: '项目',
    3: 'SSH'
}