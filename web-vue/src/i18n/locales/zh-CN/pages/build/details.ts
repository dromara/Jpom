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
  group: '分组',
  branchTagName: '分组/标签',
  buildType: '构建方式',
  buildMode: {
    container: '容器构建',
    local: '本地构建'
  },
  buildId: '最新构建ID',
  buildStatus: '构建状态',
  unknown: '未知',
  releaseMethod: '发布方式',
  autoBuildCron: '定时构建',
  aliasCode: '别名码',
  sourceDirExist: '构建目录',
  exist: '存在',
  notExist: '不存在',
  createTime: '创建时间',
  modifyTime: '最后修改时间',
  modifyUser: '最后修改人',
  resultDirFile: '产物',
  tempRepository: {
    name: '源仓库',
    gitUrl: '仓库链接'
  },
  repositoryLastCommit: '仓库lastcommit',
  buildHistory: '构建历史',
  buildRemarks: '构建备注',
  status: '状态',
  time: '时间',
  triggerType: '触发类型',
  occupySpace: '占用空间',
  product: '产物',
  logs: '日志',
  constructionTime: '构建耗时',
  publishingMethod: '发布方式',
  operation: '操作',
  rollback: '回滚',
  tooltip: {
    refresh: '点击刷新构建信息',
    logs: '下载构建日志,如果按钮不可用表示日志文件不存在,一般是构建历史相关文件被删除',
    product: '下载构建产物,如果按钮不可用表示产物文件不存在,一般是构建没有产生对应的文件或者构建历史相关文件被删除',
    rollback: 'Dockerfile 构建方式不支持在这里回滚'
  }
}
