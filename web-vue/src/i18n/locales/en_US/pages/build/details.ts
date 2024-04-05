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
  group: 'Group',
  branchTagName: 'Branch Tag Name',
  buildType: 'Build Type',
  buildMode: {
    container: 'Container Build',
    local: 'Local Build'
  },
  buildId: 'Latest Build ID',
  buildStatus: 'Build Status',
  releaseMethod: 'Release Method',
  autoBuildCron: 'Scheduled Build',
  aliasCode: 'Alias Code',
  sourceDirExist: 'Build Directory',
  exist: 'Exist',
  notExist: 'Not Exist',
  createTime: 'Create Time',
  modifyTime: 'Last Modify Time',
  modifyUser: 'Last Modifier',
  resultDirFile: 'Artifact',
  tempRepository: {
    name: 'Temporary Repository',
    gitUrl: 'Repository URL'
  },
  repositoryLastCommit: 'Repository Last Commit',
  buildHistory: 'Build History',
  buildRemarks: 'Build Remarks',
  status: 'Status',
  time: 'Time',
  triggerType: 'Trigger Type',
  occupySpace: 'Occupy Space',
  product: 'Product',
  logs: 'Logs',
  constructionTime: 'Construction Time',
  publishingMethod: 'Publishing Method',
  operation: 'Operation',
  rollback: 'Rollback',
  tooltip: {
    refresh: 'Click to refresh build information',
    logs: 'Download build logs, if the button is not available it means the log file does not exist, usually because the build history related files have been deleted',
    product:
      'Download build artifacts, if the button is not available it means the artifact file does not exist, usually because the build did not produce the corresponding file or the build history related files have been deleted',
    rollback: 'Dockerfile build method does not support rollback here'
  }
}
