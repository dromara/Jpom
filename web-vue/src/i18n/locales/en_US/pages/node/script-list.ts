export default {
  c: {
    name: 'Name',
    scheduling: 'Scheduled Execution',
    syncCache: 'Synchronize Cache',
    execute: 'Execute',
    log: 'Log',
    trigger: 'Trigger',
    delete: 'Delete',
    copyOnClick: 'Click to copy',
    confirm: 'Confirm',
    cancel: 'Cancel'
  },
  p: {
    noNodeScript: 'No node script available',
    selectNode: 'Select node',
    quickBack: 'Hold Ctrl or Alt/Option and click the button to quickly return to the first page',
    search: 'Search',
    add: 'Add',
    nodeScriptTemplateDescription:
      'Node script templates are command scripts stored in nodes for online management of script commands, such as initializing software environments, managing applications, etc.',
    loadEnv:
      'By default, all environment variables are not loaded during execution, and they need to be loaded within the script',
    commandFilePath: "The command file will be executed at {'${Plugin data directory}'}/script/xxxx.sh, bat",
    addScriptTemplate: 'To add a script template, go to node management to add',
    global: 'Global',
    workspace: 'Workspace',
    serverScript: 'Server-distributed script',
    localScript: 'Local script',
    more: 'More',
    serverScriptDelete:
      'Server-distributed synchronized scripts cannot be deleted directly. Please operate on the server side',
    unbind: 'Unbind',
    resetToken: 'Reset trigger token information. After resetting, the previous trigger token will be invalid',
    reset: 'Reset',
    tip: 'Friendly Reminder',
    triggerAddressInfo:
      'In a single trigger address: The first random string is the script ID, and the second random string is the token',
    resetTriggerAddress:
      'Reset to regenerate the trigger address. After successful reset, the previous trigger address will be invalid. The trigger is bound to the operator who generated the trigger. If the corresponding account is deleted, the trigger will be invalid',
    batchTriggerParams: 'Batch trigger parameters BODY json: [ { "id":"1", "token":"a" } ]',
    triggerParamEnv:
      "Single trigger requests support parsing parameters into environment variables for script execution. For example, if the parameter name is abc=efg, it can be referenced in the script as: {'${trigger_abc}'}",
    singleTriggerAddress: 'Single trigger address',
    batchTriggerAddress: 'Batch trigger address',
    nodeName: 'Node name',
    workspaceName: 'Workspace name',
    type: 'Type',
    share: 'Share',
    modifyTime: 'Modify time',
    createTime: 'Create time',
    creator: 'Creator',
    modifier: 'Modifier',
    lastOperator: 'Last operator',
    operation: 'Operation',
    systemTip: 'System tip',
    confirmation: 'Are you sure you want to delete the script?',
    console: 'Console',
    unbindConfirmation: 'Are you sure you want to unbind the node script?',
    unbindNote: 'Unbinding will not actually request the node to delete script information',
    unbindUsage:
      'Generally used when the server cannot be connected and it is confirmed that it will no longer be used',
    misoperationWarning: 'Redundant data may be generated if there is a misoperation!!!',
    dangerWarning: 'Dangerous operation!!!'
  }
}
