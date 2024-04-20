///
/// Copyright (c) 2019 Of Him Code Technology Studio
/// Jpom is licensed under Mulan PSL v2.
/// You can use this software according to the terms and conditions of the Mulan PSL v2.
/// You may obtain a copy of Mulan PSL v2 at:
/// 			http://license.coscl.org.cn/MulanPSL2
/// THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
/// See the Mulan PSL v2 for more details.
///

export default {
  c: {
    buildName: '构建名称',
    unknown: '未知',
    rollback: '回滚',
    systemMessage: '系统提示',
    confirm: '确认',
    cancel: '取消'
  },
  p: {
    noBuildHistory: '没有任何构建历史',
    selectStatus: '请选择状态',
    selectTriggerType: '请选择触发类型',
    quickReturnToFirstPage: '按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页',
    search: '搜索',
    batchDelete: '批量删除',
    buildHistoryDescription: '构建历史是用于记录每次构建的信息,可以保留构建产物信息,构建日志。同时还可以快速回滚发布',
    modifyBuildConfig: '如果不需要保留较多构建历史信息可以到服务端修改构建相关配置参数',
    buildHistorySpace: '构建历史可能占有较多硬盘空间,建议根据实际情况配置保留个数',
    viewLog: '点击查看日志',
    artifactFileSize: '产物文件大小：',
    logFile: '日志文件：',
    startTime: '开始时间：',
    endTime: '结束时间：',
    downloadLog: '下载构建日志,如果按钮不可用表示日志文件不存在,一般是构建历史相关文件被删除',
    log: '日志',
    downloadArtifact:
      '下载构建产物,如果按钮不可用表示产物文件不存在,一般是构建没有产生对应的文件或者构建历史相关文件被删除',
    artifact: '产物',
    more: '更多',
    dockerfileNotSupported: 'Dockerfile 构建方式不支持在这里回滚',
    delete: '删除',
    buildId: '构建 ID',
    note: '备注',
    status: '状态',
    triggerType: '触发类型',
    spaceOccupied: '占用空间',
    startTimeLog: '开始时间',
    duration: '耗时',
    dataUpdateTime: '数据更新时间',
    publishMethod: '发布方式',
    operator: '操作人',
    operation: '操作',
    confirmRollback: '真的要回滚该构建历史记录么？',
    confirmDeleteHistory: '真的要删除构建历史记录么？',
    noSelectedData: '没有选择任何数据',
    confirmDeleteMultiple: '真的要删除这些构建历史记录么？',
    selectBuild: '请选择要使用的构建',
    artifactNotFound: '选择的构建历史产物已经不存在啦'
  }
}
