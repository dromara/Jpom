export default {
  c: {
    name: 'Name',
    description: 'Description',
    scheduleExecution: 'Scheduled Execution',
    global: 'Global',
    execute: 'Execute',
    edit: 'Edit',
    trigger: 'Trigger',
    share: 'Share',
    warmTips: 'Warm Tips',
    selectWorkspace: 'Select Workspace',
    copyOnClick: 'Click to Copy',
    operation: 'Operation',
    confirm: 'Confirm',
    cancel: 'Cancel'
  },
  p: {
    noScript: 'No Scripts Available',
    scriptId: 'Script ID',
    backToFirstPage: 'Hold Ctrl or Alt/Option key and click the button to quickly return to the first page',
    search: 'Search',
    add: 'Add',
    workspaceSync: 'Workspace Synchronization',
    scriptTemplateDescription:
      'Script templates are command scripts stored on the server for online management of script commands, such as initializing software environments and managing applications.',
    executionEnvNote:
      'By default, all environment variables are not loaded during execution. They need to be loaded within the script itself.',
    commandFilePath: "Command files will be executed at {'${Data Directory}'}/script/xxxx.sh, bat",
    distributionNodeDescription:
      'Distribution nodes refer to nodes that automatically synchronize script content after editing the script, typically used in DSL mode for node distribution functions.',
    workspace: 'Workspace',
    log: 'Log',
    more: 'More',
    delete: 'Delete',
    unbind: 'Unbind',
    editScript: 'Edit Script',
    scriptName: 'Script Name',
    scriptContent: 'Script Content',
    defaultParam: 'Default Parameter',
    paramDescription: 'Parameter Description',
    paramDescriptionNote: 'Parameter descriptions do not have any practical function',
    paramHint: 'Only used to indicate the meaning of the parameter',
    manualExecNote:
      'After adding default parameters, you need to fill in the parameter value when manually executing the script',
    addParam: 'Add Parameter',
    cronExpression:
      'Fill in if scheduled automatic execution is needed, cron expression. By default, second-level matching is not enabled. You need to modify the configuration file: [system.timerMatchSecond]',
    detailedDescription: 'Detailed Description',
    currentWorkspace: 'Current Workspace',
    disableDistributionNode: 'Disable Distribution Node',
    controlNodeDistribution: 'Go to the original workspace to control node distribution',
    nodeName: 'Node Name:',
    selectedWorkspace: 'Workspace:',
    distributionNodeLabel: 'Distribution Node',
    distributeToNode: 'Please select the node to distribute to',
    syncToOtherWorkspaces: 'Sync to other workspaces',
    syncMechanism: 'Adopt the synchronization mechanism',
    scriptName_1: 'Script Name',
    isSameScript: 'Confirm it is the same script',
    createScriptIfNotExist:
      'When the corresponding script does not exist in the target workspace, a new script will be automatically created',
    syncScriptContentAndInfo:
      'When the script already exists in the target workspace, the script content, default parameters, scheduled execution, and description will be automatically synchronized',
    selectWorkspace: 'Select Workspace',
    resetTriggerToken:
      'Reset trigger token information. After resetting, the previous trigger token will be invalidated',
    reset: 'Reset',
    triggerAddressInfo:
      'In a single trigger address: the first random string is the script ID, and the second random string is the token',
    resetTriggerAddress:
      'Reset to regenerate the trigger address. After a successful reset, the previous trigger address will be invalidated. The trigger is bound to the operator who generated the trigger. If the corresponding account is deleted, the trigger will be invalidated',
    batchTriggerParams: 'Batch trigger parameters BODY json: [ { "id":"1", "token":"a" } ]',
    triggerParamsAsEnv:
      "Individual trigger requests support parsing parameters as environment variables for script execution. For example, the passed parameter name abc=efg can be introduced in the script as: {'${trigger_abc}'}",
    singleTriggerAddress: 'Single Trigger Address',
    batchTriggerAddress: 'Batch Trigger Address',
    scriptExecutionHistory: 'Script Execution History',
    modifyTime: 'Modify Time',
    createTime: 'Create Time',
    creator: 'Creator',
    modifier: 'Modifier',
    lastExecutor: 'Last Executor',
    inputScriptName: 'Please enter the script name',
    inputScriptContent: 'Please enter the script content',
    systemTip: 'System Tip',
    confirmDeleteScript: 'Are you sure you want to delete the script?',
    console: 'Console',
    confirmUnbindNode: 'Are you sure you want to unbind the script associated with the node?',
    unbindNodeNote: 'Unbinding will not actually request the node to delete script information',
    unbindNodeForUnreachableServer:
      'Generally used when the server cannot be connected and it is determined that it will no longer be used',
    redundantDataWarning: 'Redundant data may be generated if misoperated!!!',
    dangerousOperationWarning: 'Dangerous operation!!!',
    selectScript: 'Please select the script to use',
    content1:
      'Parameter description, parameter description has no practical effect, it is only used to indicate the meaning of the parameter',
    content2: 'Parameter values need to be filled in when manually executing scripts after adding default parameters',
    content3:
      'The distribution node refers to the script that automatically synchronizes the script content with the node after editing the script',
    parameterContent: 'Parameter {count} description'
  }
}
