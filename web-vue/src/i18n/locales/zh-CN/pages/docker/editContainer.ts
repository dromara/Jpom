export default {
  c: {
    weight: '权重',
    cpuPeriod: '一个整数值，表示此容器相对于其他容器的相对 CPU 权重。',
    cpuQuota: '容器在一个 CPU 周期内可以获得的 CPU 时间的微秒。',
    memory: '设置内存限制。',
    memorySwap: '总内存（内存 + 交换）。 设置为 -1 以禁用交换。',
    memorySoftLimit: '软内存限制。'
  },
  p: {
    receive: '接收流量',
    transmit: '输出流量',
    weight: '权重（相对权重）。',
    blockIOWeight: 'Block IO 权重',
    cpuSetCPUs: '执行的 CPU',
    cpuSetCPUsString: '允许执行的 CPU（例如，0-3、0',
    cpuSetMEMs: '允许执行的内存节点',
    cpuSetMEMsString: '允许执行的内存节点 (MEM) (0-3, 0,1)。 仅在 NUMA 系统上有效。',
    period: '周期',
    periodUsec: '周期的长度，以微秒为单位。',
    cpuPeriodUsec: 'CPU 周期的长度，以微秒为单位。',
    quota: '时间',
    memoryLimit: '内存',
    memoryTotal: '总内存',
    memorySoftLimit: '软内存'
  }
}
