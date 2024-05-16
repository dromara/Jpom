export default {
  c: {},
  p: {
    today: 'Today',
    yesterday: 'Yesterday',
    search: 'Search',
    linuxMemoryInfo:
      'If the actual running memory in Linux differs greatly from the value calculated by the free and total fields obtained using the free -h command, it is caused by the swap memory in your current server.',
    oshiLibrary:
      'The system uses the oshi library to monitor the system, and uses /proc/meminfo in oshi to obtain memory usage.',
    memUsageCalcWithAvailable:
      'If the file contains the fields: MemAvailable and MemTotal, then oshi uses them directly. Therefore, the calculation method for memory usage in this system is: memory usage = (total - available) / total',
    memUsageCalcWithoutAvailable:
      'If MemAvailable does not exist in the file, then MemAvailable = MemFree + Active(file) + Inactive(file) + SReclaimable. Therefore, the calculation method for memory usage in this system is: memory usage = (total - (MemFree + Active(file) + Inactive(file) + SReclaimable)) / total',
    noDataFound: 'No data found'
  }
}
