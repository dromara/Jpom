export default {
  c: {
    save: '保存',
    systemPrompt: '系统提示',
    confirm: '确认',
    cancel: '取消'
  },
  p: {
    serverSystemConfig: '服务端系统配置',
    configFilePath: '配置文件路径',
    saveAndRestart: '保存并重启',
    serverIpAuthConfig: '服务端IP授权配置',
    currentAccessIp: '当前访问IP：',
    ipConfigWarning:
      '请仔细确认后配置，ip配置后立即生效。配置时需要保证当前ip能访问！127.0.0.1 该IP不受访问限制.支持配置IP段 192.168.1.1/192.168.1.254,192.168.1.0/24',
    resetConfigWarning: '如果配置错误需要重启服务端并新增命令行参数 --rest:ip_config 将恢复默认配置',
    forbiddenIp: '禁止访问的 IP 地址',
    forbidden: '禁止',
    inputForbiddenIp: '请输入IP禁止,多个使用换行,支持配置IP段 192.168.1.1/192.168.1.254,192.168.1.0/24',
    allowedIp: '只允许访问的 IP 地址',
    authorize: '授权',
    inputAuthorizedIp:
      '请输入IP授权,多个使用换行,0.0.0.0 是开放所有IP,支持配置IP段 192.168.1.1/192.168.1.254,192.168.1.0/24',
    globalProxy: '全局代理',
    globalProxyDesc: '全局代理配置后将对服务端的网络生效，代理实现方式：ProxySelector',
    wildcard: '通配符',
    addressWildcard: '地址通配符,* 表示所有地址都将使用代理',
    proxy: '代理',
    proxyAddress: '代理地址 (127.0.0.1:8888)',
    delete: '删除',
    add: '新增',
    confirmSaveConfig: '真的要保存当前配置吗？如果配置有误,可能无法启动服务需要手动还原奥！！！',
    restarting: '重启中，请稍候...',
    waitWithoutRefresh: '请耐心等待暂时不用刷新页面',
    autoRefreshAfterRestart: '重启成功后会自动刷新',
    restartSuccess: '重启成功',
    restartFailed: '未重启成功：',
    restartTimeout: '重启超时,请去服务器查看控制台日志排查问题',
    confirmSaveConfigWithAuth:
      '真的要保存当前配置吗？IP 授权请慎重配置奥( 授权是指只允许访问的 IP ),配置后立马生效 如果配置错误将出现无法访问的情况,需要手动恢复奥！！！'
  }
}
