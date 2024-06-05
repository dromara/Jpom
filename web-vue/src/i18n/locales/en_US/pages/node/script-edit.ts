export default {
  c: {
    serverScriptModificationForbidden: 'Server-side synchronized scripts cannot be modified here'
  },
  p: {
    editScript: 'Edit Script',
    reminder: 'Reminder',
    noLogicNodeError: 'The current workspace does not have any logic nodes, so you cannot create node scripts.',
    selectNode: 'Select Node',
    pleaseSelectNode: 'Please select a node',
    scriptName: 'Script Name',
    name: 'Name',
    scriptContent: 'Script Content',
    defaultParams: 'Default Parameters',
    paramDescriptionTemplate: "Parameter {'${index + 1}'} Description",
    paramDescription: 'Parameter Description',
    paramDescriptionNote: 'Parameter descriptions do not have any practical effect',
    paramDescriptionHint: ',,They are only used to indicate the meaning of the parameters',
    paramValueTemplate: "Parameter {'${index + 1}'} Value",
    paramValue: 'Parameter Value',
    newParamValueNote:
      ',After adding new default parameters, you need to fill in the parameter values when manually executing the script',
    addNewParam: 'Add New Parameter',
    sharing: 'Sharing',
    globalScope: 'Global',
    currentWorkspace: 'Current Workspace',
    scheduledExecution: 'Scheduled Execution',
    cronExpressionNote:
      'If you need to automatically execute the script on a schedule, please fill in the cron expression. By default, second-level matching is not enabled. You need to modify the configuration file: [system.timerMatchSecond])',
    description: 'Description',
    detailedDescription: 'Detailed Description',
    pleaseInputScriptName: 'Please enter a script name',
    pleaseInputScriptContent: 'Please enter script content',
    noNodeSelectedError: 'You cannot save the script without selecting a node',
    fillParamDescriptionPrefix: 'Please fill in the description for the ',
    paramDescriptionSuffix: 'th parameter'
  }
}
