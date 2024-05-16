export default {
  c: {},
  p: {
    today: '今天',
    yesterday: '昨天',
    search: '搜索',
    linuxMemoryInfo:
      '如果在 Linux 中实际运行内存可能和您直接使用 free -h 命令查询到 free 和 total 字段计算出数值相差过大那么此时就是您当前服务器中的交换内存引起的',
    oshiLibrary: '系统 采用 oshi 库来监控系统，在 oshi 中使用 /proc/meminfo 来获取内存使用情况。',
    memUsageCalcWithAvailable:
      '文件中如果存在：MemAvailable、MemTotal 这两个字段，那么 oshi 直接使用，所以本系统 中内存占用计算方式：内存占用=(total-available)/total',
    memUsageCalcWithoutAvailable:
      '文件中如果不存在：MemAvailable，那么 MemAvailable = MemFree+Active(file)+Inactive(file)+SReclaimable，所以本系统 中内存占用计算方式：内存占用=(total-(MemFree+Active(file)+Inactive(file)+SReclaimable))/total',
    noDataFound: '未查询到任何数据'
  }
}
