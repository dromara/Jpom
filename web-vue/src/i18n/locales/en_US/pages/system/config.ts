export default {
  c: {
    save: 'Save',
    systemPrompt: 'System Prompt',
    confirm: 'Confirm',
    cancel: 'Cancel'
  },
  p: {
    serverSystemConfig: 'Server System Config',
    configFilePath: 'Config File Path',
    saveAndRestart: 'Save and Restart',
    serverIpAuthConfig: 'Server IP Authorization Config',
    currentAccessIp: 'Current Access IP:',
    ipConfigWarning:
      'Please carefully confirm the configuration, the IP configuration will take effect immediately. When configuring, please ensure that the current IP can be accessed! 127.0.0.1 This IP is not restricted for access. Supports configuring IP ranges 192.168.1.1/192.168.1.254,192.168.1.0/24',
    resetConfigWarning:
      'If the configuration is incorrect, you need to restart the server and add the command line argument --rest:ip_config to restore the default configuration',
    forbiddenIp: 'Forbidden IP Address',
    forbidden: 'Forbidden',
    inputForbiddenIp:
      'Please enter the IP to be forbidden. Use new lines for multiple entries. Supports configuring IP ranges 192.168.1.1/192.168.1.254,192.168.1.0/24',
    allowedIp: 'Allowed IP Address',
    authorize: 'Authorize',
    inputAuthorizedIp:
      'Please enter the IP to be authorized. Use new lines for multiple entries. 0.0.0.0 opens all IPs. Supports configuring IP ranges 192.168.1.1/192.168.1.254,192.168.1.0/24',
    globalProxy: 'Global Proxy',
    globalProxyDesc:
      "After configuring the global proxy, it will take effect on the server's network. Proxy implementation method: ProxySelector",
    wildcard: 'Wildcard',
    addressWildcard: 'Address wildcard, * means that all addresses will use the proxy',
    proxy: 'Proxy',
    proxyAddress: 'Proxy Address (127.0.0.1:8888)',
    delete: 'Delete',
    add: 'Add',
    confirmSaveConfig:
      'Are you sure you want to save the current configuration? If the configuration is incorrect, the service may not start and you may need to manually restore it!',
    restarting: 'Restarting, please wait...',
    waitWithoutRefresh: 'Please wait patiently without refreshing the page',
    autoRefreshAfterRestart: 'Auto refresh after restart',
    restartSuccess: 'Restart successful',
    restartFailed: 'Restart failed:',
    restartTimeout: 'Restart timeout, please check the server console logs to troubleshoot the issue',
    confirmSaveConfigWithAuth:
      'Are you sure you want to save the current configuration? Please be cautious when configuring IP authorization (authorization refers to allowing access only from specific IP addresses). The configuration will take effect immediately. If the configuration is incorrect, you may not be able to access and will need to manually restore it!!!'
  }
}
