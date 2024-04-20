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
    buildName: 'Build Name',
    unknown: 'Unknown',
    rollback: 'Rollback',
    systemMessage: 'System Message',
    confirm: 'Confirm',
    cancel: 'Cancel'
  },
  p: {
    noBuildHistory: 'No build history available',
    selectStatus: 'Please select status',
    selectTriggerType: 'Please select trigger type',
    quickReturnToFirstPage: 'Hold Ctr or Alt/Option and click button to quickly return to the first page',
    search: 'Search',
    batchDelete: 'Batch Delete',
    buildHistoryDescription:
      'Build history records information about each build, preserving build artifacts and logs. It also allows for quick rollback of releases.',
    modifyBuildConfig:
      'If you do not need to retain much build history, you can modify the build-related configuration parameters on the server.',
    buildHistorySpace:
      'Build history may occupy a significant amount of disk space. It is recommended to configure the number of retained items based on actual conditions.',
    viewLog: 'View Log',
    artifactFileSize: 'Artifact File Size:',
    logFile: 'Log File:',
    startTime: 'Start Time:',
    endTime: 'End Time:',
    downloadLog:
      'Download build log. If the button is not clickable, it means the log file does not exist, usually because the build history-related files have been deleted.',
    log: 'Log',
    downloadArtifact:
      'Download build artifact. If the button is not clickable, it means the artifact file does not exist, usually because the build did not produce a corresponding file or the build history-related files have been deleted.',
    artifact: 'Artifact',
    more: 'More',
    dockerfileNotSupported: 'Dockerfile build method is not supported for rollback here',
    delete: 'Delete',
    buildId: 'Build ID',
    note: 'Note',
    status: 'Status',
    triggerType: 'Trigger Type',
    spaceOccupied: 'Space Occupied',
    startTimeLog: 'Start Time',
    duration: 'Duration',
    dataUpdateTime: 'Data Update Time',
    publishMethod: 'Publish Method',
    operator: 'Operator',
    operation: 'Operation',
    confirmRollback: 'Are you sure you want to rollback this build history record?',
    confirmDeleteHistory: 'Are you sure you want to delete this build history record?',
    noSelectedData: 'No data selected',
    confirmDeleteMultiple: 'Are you sure you want to delete these build history records?',
    selectBuild: 'Please select the build to use',
    artifactNotFound: 'The selected build history artifact no longer exists'
  }
}
