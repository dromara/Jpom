export default {
  c: {
    clear: '清空',
    workspaceId: '工作空间ID：',
    nodeId: '节点ID：',
    correction: '修正',
    selectNode: '请选择节点'
  },
  p: {
    cacheInfo: '缓存信息',
    dataDirectoryWarning: '请勿手动删除数据目录下面文件,如果需要删除需要提前备份或者已经确定对应文件弃用后才能删除',
    pluginTime: '插件端时间：',
    dataDirectorySpace: '数据目录占用空间：',
    tempFileSpace: '临时文件占用空间：',
    oldPackageSpace: '旧版程序包占有空间：',
    processPortCache: '进程端口缓存：',
    scriptLogCount: '脚本日志数：',
    readingLogFileCount: '在读取的日志文件数：',
    pluginCount: '插件数：',
    environmentVariable: '环境变量：',
    environmentVariableKey: '环境变量的key',
    scheduledTask: '定时任务',
    lonelyData: '孤独数据',
    lonelyDataDesc: '何为孤独数据',
    lonelyDataDetail:
      '孤独数据是指机器节点里面存在数据，但是无法和当前系统绑定上关系（关系绑定=节点ID+工作空间ID对应才行），一般情况下不会出现这样的数据',
    lonelyDataCause: '通常情况为项目迁移工作空间、迁移物理机器等一些操作可能产生孤独数据',
    lonelyDataEffect: '如果孤独数据被工作空间下的其他功能关联，修正后关联的数据将失效对应功能无法查询到关联数据',
    oldVersionData: '低版本项目数据未存储节点ID，对应项目数据也将出来在孤独数据中（此类数据不影响使用）',
    multipleBinding: '一个物理节点被多个服务端绑定也会产生孤独数据奥',
    projectName: '项目名称：',
    projectId: '项目ID：',
    projectLonelyData: '项目孤独数据',
    scriptName: '脚本名称：',
    scriptId: '脚本ID：',
    scriptLonelyData: '脚本孤独数据',
    correctLonelyData: '修正孤独数据',
    warning: '温馨提示',
    correctionEffect: '修改后如果有原始关联数据将失效，需要重新配置关联',
    disabledNodeTip: '如果节点选项是禁用，则表示对应数据有推荐关联节点（低版本项目数据可能出现此情况）',
    selectNode: '选择节点'
  }
}
