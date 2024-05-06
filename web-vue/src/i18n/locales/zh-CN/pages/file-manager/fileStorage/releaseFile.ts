export default {
  c: {
    content: '上传前'
  },
  p: {
    taskName: '任务名',
    taskNamePlaceholder: '请输入任务名',
    publishMode: '发布方式',
    node: '节点',
    fileNameAfterPublish: '发布后的文件名是：文件ID.后缀，并非文件真实名称 （可以使用上传后脚本随意修改）',
    ssh: '发布的SSH',
    sshPlaceholder: '请选择SSH',
    publishNode: '发布的节点',
    nodePlaceholder: '请选择节点',
    publishDir: '发布目录',
    authDirConfig: '需要配置授权目录（授权才能正常使用发布）,授权目录主要是用于确定可以发布到哪些目录中',
    authDir: '配置目录',
    firstLevelDir: '请选择发布的一级目录',
    secondLevelDir: '请填写发布的二级目录',
    executeScript: '执行脚本',
    scriptVariable: '支持变量引用：${TASK_ID}、${FILE_ID}、${FILE_NAME}、${FILE_EXT_NAME}',
    workspaceEnvVariable: '可以引用工作空间的环境变量 变量占位符 ${xxxx} xxxx 为变量名称',
    renameFile: '建议在上传后的脚本中对文件进行自定义更名，SSH 上传默认为：${FILE_ID}.${FILE_EXT_NAME}',
    preUploadScript: '文件上传前需要执行的脚本(非阻塞命令)',
    postUploadScript: '文件上传成功后需要执行的脚本(非阻塞命令)',
    execute: '执行',
    afterUpload: '上传后',
    afterUploadExecute: '上传后执行',
    authDirSetting: '配置授权目录',
    taskNameInput: '请输入文件任务名',
    publishModeSelect: '请选择发布方式',
    publishDirSelect: '请选择发布的一级目录和填写二级目录',
    sshSelect: '请选择发布的SSH'
  }
}
