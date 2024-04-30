export default {
  c: {},
  p: {
    trimOperation:
      'Trim operation will delete relevant data, please proceed with caution. Please confirm the consequences of this operation before using it.',
    trimType: 'Trim type',
    selectTrimType: 'Select trim type',
    floatingType: 'Floating type',
    floating: 'Floating',
    nonFloating: 'Non-floating',
    limitedTime: 'Limited time',
    suggestedTimeRange: 'Suggested time range for addition',
    otherwiseDeleteAllData: 'Otherwise, all data meeting the criteria will be deleted',
    timeFormat:
      "Can be Unix timestamp, date format timestamp, or Go duration string (e.g., 10m, 1h30m), calculated relative to the daemon machine's time.",
    trimBeforeTimestamp: 'Trim objects created before this timestamp. For example: 24h',
    specifiedLabel: 'Specified label',
    labelExample: 'Example: key,key1 or key=value,key1=value1',
    trimObjectsWithLabel: 'Trim objects with specified labels, multiple use commas to separate',
    autoExecute: 'docker',
    confirm: 'Confirm',
    image: 'Image',
    trimUnusedAndUnmarkedImages: 'Trim only unused and unmarked images',
    container: 'Container',
    network: 'Network',
    volume: 'Volume',
    build: 'Build',
    systemPrompt: 'System prompt',
    confirmTrimInfo:
      'Are you sure you want to trim the corresponding information? Trimming will automatically clean up the corresponding data.',
    confirmAction: 'Confirm',
    cancel: 'Cancel'
  }
}
