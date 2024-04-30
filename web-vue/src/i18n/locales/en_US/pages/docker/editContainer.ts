export default {
  c: {
    weight: 'Weight',
    cpuPeriod: 'An integer value representing the relative CPU weight of this container compared to others.',
    cpuQuota: 'The microseconds of CPU time that the container can get in one CPU period.',
    memory: 'Sets memory limit.',
    memorySwap: 'Total memory (memory + swap). Set to -1 to disable swap.',
    memorySoftLimit: 'Soft memory limit.'
  },
  p: {
    receive: 'Receive traffic',
    transmit: 'Transmit traffic',
    weight: 'Weight (relative weight).',
    blockIOWeight: 'Block IO weight',
    cpuSetCPUs: 'CPUs to execute on',
    cpuSetCPUsString: 'CPUs allowed to execute on (e.g., 0-3,0)',
    cpuSetMEMs: 'Memory nodes allowed to execute on',
    cpuSetMEMsString: 'Memory nodes allowed to execute on (MEMs) (0-3,0,1). Only valid on NUMA systems.',
    period: 'Period',
    periodUsec: 'Length of the period, in microseconds.',
    cpuPeriodUsec: 'Length of the CPU period, in microseconds.',
    quota: 'Time',
    memoryLimit: 'Memory',
    memoryTotal: 'Total memory',
    memorySoftLimit: 'Soft memory'
  }
}
