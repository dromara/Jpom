export default {
  c: {
    content: 'Before uploading'
  },
  p: {
    taskName: 'Task name',
    taskNamePlaceholder: 'Please enter the task name',
    publishMode: 'Publishing method',
    node: 'Node',
    fileNameAfterPublish:
      'The filename after publishing is: FileID.Suffix, not the real name of the file (can be modified freely using the post-upload script)',
    ssh: 'Publishing SSH',
    sshPlaceholder: 'Please select SSH',
    publishNode: 'Publishing node',
    nodePlaceholder: 'Please select a node',
    publishDir: 'Publishing directory',
    authDirConfig:
      'Authorization directory configuration is required (authorization is required for normal use of publishing). The authorization directory is mainly used to determine which directories can be published to',
    authDir: 'Configuration directory',
    firstLevelDir: 'Please select the first-level directory for publishing',
    secondLevelDir: 'Please enter the second-level directory for publishing',
    executeScript: 'Execution script',
    scriptVariable:
      "Supports variable references: {'${TASK_ID}'}, {'${FILE_ID}'}, {'${FILE_NAME}'}, {'${FILE_EXT_NAME}'}",
    workspaceEnvVariable:
      "You can reference workspace environment variables. Variable placeholder: {'${xxxx}'}, where xxxx is the variable name",
    renameFile:
      "It is recommended to rename the file in the post-upload script. The default for SSH upload is: {'${FILE_ID}'}.{'${FILE_EXT_NAME}'}",
    preUploadScript: 'Script to be executed before file upload (non-blocking command)',
    postUploadScript: 'Script to be executed after successful file upload (non-blocking command)',
    execute: 'Execute',
    afterUpload: 'After uploading',
    afterUploadExecute: 'Execute after uploading',
    authDirSetting: 'Configure the authorization directory',
    taskNameInput: 'Please enter the file task name',
    publishModeSelect: 'Please select the publishing method',
    publishDirSelect: 'Please select the first-level directory for publishing and enter the second-level directory',
    sshSelect: 'Please select the publishing SSH'
  }
}
