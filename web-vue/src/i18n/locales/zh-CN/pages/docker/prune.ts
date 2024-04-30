export default {
  c: {},
  p: {
    trimOperation: '修剪操作会删除相关数据，请谨慎操作。请您再确认本操作后果后再使用',
    trimType: '修剪类型',
    selectTrimType: '请选择修剪类型',
    floatingType: '悬空类型',
    floating: '悬空',
    nonFloating: '非悬空',
    limitedTime: '限定时间',
    suggestedTimeRange: '建议新增指定时间范围',
    otherwiseDeleteAllData: '否则将删除满足条件的所有数据',
    timeFormat:
      '可以是 Unix 时间戳、日期格式的时间戳或 Go 持续时间字符串（例如 10m、1h30m），相对于守护进程机器的时间计算。',
    trimBeforeTimestamp: '修剪在此时间戳之前创建的对象 例如：24h',
    specifiedLabel: '指定标签',
    labelExample: '示例：key,key1 或者 key=value,key1=value1',
    trimObjectsWithLabel: '修剪具有指定标签的对象,多个使用逗号分隔',
    autoExecute: 'docker',
    confirm: '确定',
    image: '镜像',
    trimUnusedAndUnmarkedImages: '仅修剪未使用和未标记的镜像',
    container: '容器',
    network: '网络',
    volume: '卷',
    build: '构建',
    systemPrompt: '系统提示',
    confirmTrimInfo: '确定要修剪对应的信息吗？修剪会自动清理对应的数据',
    confirmAction: '确认',
    cancel: '取消'
  }
}
