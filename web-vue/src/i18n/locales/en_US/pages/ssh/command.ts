export default {
  c: {
    schedulingExecution: 'Scheduled Execution',
    execution: 'Execution',
    trigger: 'Trigger',
    commandName: 'Command Name',
    sshNode: 'SSH Node',
    paramValue: 'Parameter Value',
    newParam: 'New Parameter',
    commandDescription: 'Command Description',
    tips: 'Tips',
    selectWorkspace: 'Select Workspace',
    copy: 'Click to Copy'
  },
  p: {
    noSshScriptCommand: 'No SSH Script Commands',
    searchCommand: 'Search Command',
    description: 'Description',
    quickBackToFirstPage: 'Press Ctrl or Alt/Option to quickly return to the first page by clicking the button',
    search: 'Search',
    addNew: 'Add New',
    more: 'More',
    workspaceSync: 'Workspace Sync',
    commandTemplateDescription:
      'Command templates are used to manage script commands online, such as initializing software environments, managing applications, etc.',
    supportWorkspaceEnv: 'Command content supports workspace environment variables',
    execCommandReplace:
      'Execution commands will be automatically replaced with sh command files, and environment variables will be automatically loaded: /etc/profile, /etc/bashrc, ~/.bashrc, ~/.bash_profile',
    execCommandInclude: 'Execution commands include:',
    cancelAutoLoadEnv: 'Will cancel the automatic loading of environment variables',
    noteNoSpace: 'Note that the entire line cannot contain spaces',
    commandFilePath:
      "The command file will be uploaded to {'${user.home}'}/.jpom/xxxx.sh and will be automatically deleted after execution",
    edit: 'Edit',
    delete: 'Delete',
    editCommand: 'Edit Command',
    scriptPathAndExecMethod:
      "Script storage path: {'${user.home}'}/.jpom/xxxx.sh, script execution path: {'${user.home}'}, script execution method: bash {'${user.home}'}/.jpom/xxxx.sh par1 par2",
    commandContent: 'Command Content',
    canReferenceWorkspaceEnv:
      "Can reference workspace environment variables Variable placeholder {'${xxxx}'} xxxx is the variable name",
    selectSshNode: 'Please select the SSH node',
    defaultParam: 'Default parameter',
    paramDescription: "Parameter {'${index + 1}'} description",
    paramDescriptionNoEffect: 'Parameter description has no practical effect',
    paramDescriptionHint: ',,Only used to indicate the meaning of the parameter',
    paramValue: "Parameter {'${index + 1}'} value",
    addNewDefaultParamNote:
      ',After adding a new default parameter, you need to fill in the parameter value when manually executing the script',
    autoExec: 'Automatic execution',
    cronExpression:
      'If you need to automatically execute regularly, fill in the cron expression. By default, second-level matching is not enabled, you need to modify the configuration file: [system.timerMatchSecond])',
    commandDetail: 'Detailed command description',
    executeCommand: 'Execute command',
    selectSshNode_1: 'Please select the SSH node',
    commandParam: 'Command parameters',
    paramInstruction:
      'All parameters will be concatenated into a string separated by spaces to execute the script. Note that the parameter order and unfilled parameters will be automatically ignored',
    paramName: 'Parameter',
    paramValue_1: 'Value',
    executeLog: 'Execution log',
    syncToOtherWorkspaces: 'Sync to other workspaces',
    syncMechanism: 'The synchronization mechanism adopts',
    scriptName: 'Script name',
    confirmSameScript: 'Confirm it is the same script',
    createNewScriptIfNotExist:
      'If the corresponding script does not exist in the target workspace, a new script will be automatically created',
    syncScriptInfoIfExists:
      'When the script already exists in the target workspace, the script content, default parameters, automatic execution, and description will be automatically synchronized',
    selectWorkspace: 'Select workspace',
    resetTriggerToken:
      'Reset the trigger token information. After resetting, the previous trigger token will be invalid',
    reset: 'Reset',
    triggerUrlInfo:
      'In a single trigger URL: the first random string is the command script ID, and the second random string is the token',
    regenerateTriggerUrl:
      'Reset to regenerate the trigger URL. After successful reset, the previous trigger URL will be invalidated. The trigger is bound to the operator who generated it. If the corresponding account is deleted, the trigger will be invalidated',
    batchTriggerParams: 'Batch trigger parameters BODY json: [ { "id":"1", "token":"a" } ]',
    parseParamsAsEnv:
      "Single trigger requests support parsing parameters as environment variables and passing them into script execution. For example, the passed parameter name abc=efg can be introduced in the script as: {'${trigger_abc}'}",
    singleTriggerUrl: 'Single trigger URL',
    batchTriggerUrls: 'Batch trigger URLs',
    createTime: 'Creation time',
    updateTime: 'Modification time',
    lastOperator: 'Last operator',
    operation: 'Operation',
    fillParamDesc: 'Please fill in the description of the ',
    paramDesc: 'th parameter',
    systemPrompt: 'System prompt',
    confirmDeleteCommand: 'Are you sure you want to delete the "',
    deleteCommand: 'command?',
    confirm: 'Confirm',
    cancel: 'Cancel',
    selectExecuteNode: 'Please select the execution node'
  }
}
