export default {
  c: {},
  p: {
    commandContent: 'Command content',
    warmTip: 'Warm tip',
    copyCommand:
      'Copy any of the following commands to the server that has not installed the plugin to execute. Before executing, you need to unblock it.',
    firewallPort: 'Firewall port',
    securityGroupRule: 'Security group rule',
    networkPortRestriction: 'Network port restrictions, etc.',
    defaultPluginPort: 'The plugin end runs on the default port:',
    checkAddressAccess:
      'Before executing, you need to check whether the address in the command can be accessed on the corresponding server. If it cannot be accessed, the node cannot be automatically bound.',
    usePingCheck: 'Will use PING check',
    reportNodeInfo:
      'After the plugin end is installed and started successfully, it will actively report node information. If the reported IP+PROP can communicate normally, node information will be added.',
    confirmMultipleIps:
      'If the reported node information contains multiple IP addresses, the user needs to confirm the specific IP address information to use.',
    autoBindWorkspace: 'The newly added node (plugin end) will automatically',
    bindToCurrentWorkspace: 'be bound to the current workspace',
    switchWorkspace: 'If you need to switch to another workspace, generate the command in advance',
    commandExpireAfterRestart: 'The following command will expire after',
    commandInvalidateAfterRestart: 'restarting the server',
    regainCommandAfterRestart: 'You need to regain the command after restarting the server',
    supportSpecifyNetworkCard: 'Supports binding by specifying the network card name:',
    networkCardExample: '. For example: http',
    quickInstall: 'Quick installation',
    quickBind: 'Quick binding',
    commandPathNote: 'Please modify the command path to the actual path in your server',
    executionResult: 'Execution result',
    noResultYet: 'No results yet',
    resultIndex: 'Result',
    resultOrder: '',
    cantCommunicateWithNode: 'Cannot communicate with the node normally',
    multipleIpsAvailable: 'Multiple IPs are available',
    nodeAlreadyExists: 'Node already exists',
    bindSuccess: 'Binding successful',
    allIps: 'All IPs:',
    communicableIps: 'Communicable IPs',
    manualConfirmationRequired: 'Manual confirmation required',
    nodeIp: 'Node IP'
  }
}
