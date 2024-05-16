export default {
  c: {
    clear: 'Clear',
    workspaceId: 'Workspace ID:',
    nodeId: 'Node ID:',
    correction: 'Correction',
    selectNode: 'Please select a node'
  },
  p: {
    cacheInfo: 'Cache Information',
    dataDirectoryWarning:
      'Do not manually delete files in the data directory. If deletion is necessary, please backup the files or confirm that the corresponding files are no longer needed before deletion.',
    pluginTime: 'Plugin Time:',
    dataDirectorySpace: 'Data Directory Space Usage:',
    tempFileSpace: 'Temporary File Space Usage:',
    oldPackageSpace: 'Old Package Space Usage:',
    processPortCache: 'Process Port Cache:',
    scriptLogCount: 'Script Log Count:',
    readingLogFileCount: 'Number of Log Files Being Read:',
    pluginCount: 'Plugin Count:',
    environmentVariable: 'Environment Variable:',
    environmentVariableKey: 'Key of Environment Variable',
    scheduledTask: 'Scheduled Task',
    lonelyData: 'Lonely Data',
    lonelyDataDesc: 'What is Lonely Data',
    lonelyDataDetail:
      'Lonely data refers to data that exists in a machine node but cannot be associated with the current system (association binding = node ID + workspace ID). Such data should not normally appear.',
    lonelyDataCause:
      'Lonely data may be caused by operations such as migrating projects to different workspaces or migrating to different physical machines.',
    lonelyDataEffect:
      'If lonely data is associated with other functions under the workspace, the associated data will be invalidated after correction, and the corresponding functions will not be able to query the associated data.',
    oldVersionData:
      'Data from older versions of projects that do not store node IDs will also appear as lonely data (such data does not affect usage).',
    multipleBinding: 'Lonely data may also be generated when a physical node is bound to multiple servers.',
    projectName: 'Project Name:',
    projectId: 'Project ID:',
    projectLonelyData: 'Project Lonely Data',
    scriptName: 'Script Name:',
    scriptId: 'Script ID:',
    scriptLonelyData: 'Script Lonely Data',
    correctLonelyData: 'Correct lonely data',
    warning: 'Friendly reminder',
    correctionEffect:
      'If there is original associated data after modification, it will be invalidated and need to be reconfigured',
    disabledNodeTip:
      'If the node option is disabled, it means that there is a recommended associated node for the corresponding data (this situation may occur in low-version project data)',
    selectNode: 'Select node'
  }
}
