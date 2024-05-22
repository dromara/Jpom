export default {
  c: {
    name: 'Name',
    value: 'Value',
    description: 'Description',
    trigger: 'Trigger',
    modifyAddress: 'Modify variable value address'
  },
  p: {
    search: 'Search',
    add: 'Add',
    envDescription:
      'Environment variables are fixed parameter values configured in the system, which can be quickly referenced during script execution.',
    envUsage: 'Environment variables can also be used for referencing repository account passwords and SSH passwords',
    scopeNote:
      'Note: Environment variables have scopes: current workspace or global, and cannot be referenced across workspaces',
    privacyField: 'Privacy field',
    globalScope: 'Global',
    currentWorkspace: 'Current workspace',
    edit: 'Edit',
    delete: 'Delete',
    editEnv: 'Edit environment variables',
    varName: 'Variable name',
    varValue: 'Variable value',
    varDesc: 'Variable description',
    privacyVar: 'Privacy variable',
    privacyVarDesc:
      'Privacy variables refer to important information such as password fields or key keys. Privacy fields can only be modified and cannot be viewed (the corresponding value cannot be seen in the edit popup). Once a privacy field is created, it cannot be switched to a non-privacy field',
    privacy: 'Privacy',
    nonPrivacy: 'Non-privacy',
    distributeNode: 'Distribution node',
    distributeNodeDesc:
      'Distribution nodes refer to synchronizing variables to corresponding nodes, and the current variables can also be used in node scripts',
    selectNode: 'Please select the node to distribute to',
    resetToken: 'Reset trigger token information. After resetting, the previous trigger token will be invalidated',
    reset: 'Reset',
    get: 'Get',
    tips: 'Tips',
    contentType: 'The response ContentType of the interface is: text/plain',
    successCode: 'The HTTP status code for successful operation is 200',
    modifySuccess:
      'The HTTP status code for modification is 200, and the response content is: success to determine the success of the operation, otherwise it may fail',
    requestBodyType:
      'When requesting the interface parameters to be passed into the request body, please use: text/plain for the ContentType',
    getAddress: 'Get variable value address',
    modifier: 'Modifier',
    scope: 'Scope',
    modifyTime: 'Modification Time',
    operation: 'Operation',
    inputName: 'Please enter variable name',
    inputDesc: 'Please enter variable description',
    inputValue: 'Please enter variable value',
    systemTips: 'System Tips',
    confirmDelete: 'Are you sure you want to delete the current variable?',
    confirm: 'Confirm',
    cancel: 'Cancel'
  }
}
