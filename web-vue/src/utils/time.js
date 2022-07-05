/**
 * 转换时间函数
 * @param {*} time
 * @param {*} cFormat
 */
export function parseTime(time, cFormat) {
  if (arguments.length === 0) {
    return "-";
  }
  if (!time) {
    return "-";
  }
  // 处理 time 参数
  if (isNaN(Number(time)) === false) {
    time = Number(time);
  }
  const format = cFormat || "{y}-{m}-{d} {h}:{i}:{s}";
  let date;
  if (typeof time === "object") {
    date = time;
  } else {
    if (("" + time).length === 10) time = parseInt(time) * 1000;
    date = new Date(time);
  }
  const formatObj = {
    y: date.getFullYear(),
    m: date.getMonth() + 1,
    d: date.getDate(),
    h: date.getHours(),
    i: date.getMinutes(),
    s: date.getSeconds(),
    a: date.getDay(),
  };
  const time_str = format.replace(/{(y|m|d|h|i|s|a)+}/g, (result, key) => {
    let value = formatObj[key];
    // Note: getDay() returns 0 on Sunday
    if (key === "a") {
      return ["日", "一", "二", "三", "四", "五", "六"][value];
    }
    if (result.length > 0 && value < 10) {
      value = "0" + value;
    }
    return value || 0;
  });
  return time_str;
}

/**
 * 格式化文件大小
 * @param {*} value
 * @returns
 */
export function renderSize(value, defaultValue = "-") {
  if (null == value || value == "") {
    return defaultValue;
  }
  var unitArr = new Array("Bytes", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB");
  var index = 0;
  var srcsize = parseFloat(value);
  if (srcsize <= 0) {
    return defaultValue;
  }
  // console.log(value, srcsize);
  index = Math.floor(Math.log(srcsize) / Math.log(1024));
  var size = srcsize / Math.pow(1024, index);
  size = size.toFixed(2); //保留的小数位数
  return size + unitArr[index];
}

/**
 * 数组分组  [{id:1,value:1},{id:2,value:3}] => {1:{id:1,value:1},2:{id:2,value:3}}
 * @param {function} group
 * @returns Object
 */
Array.prototype.groupBy = function (group) {
  return group && typeof group === "function"
    ? Array.prototype.reduce.call(
        this,
        function (c, v) {
          var k = group(v);
          c[k] = v;
          return c;
        },
        {}
      )
    : this;
};
//
export function itemGroupBy(arr, groupKey, key, dataKey) {
  key = key || "type";
  dataKey = dataKey || "data";

  let newArr = [],
    types = {},
    // newItem,
    i,
    j,
    cur;
  for (i = 0, j = arr.length; i < j; i++) {
    cur = arr[i];
    if (!(cur[groupKey] in types)) {
      types[cur[groupKey]] = { [key]: cur[groupKey], [dataKey]: [] };
      newArr.push(types[cur[groupKey]]);
    }
    types[cur[groupKey]][dataKey].push(cur);
  }
  return newArr;
}

/**
 * 格式化时长
 * @param {String} ms
 * @param {String} seg 分割符
 * @param {String} levelCount 格式化个数
 * @returns
 */
export function formatDuration(ms, seg, levelCount) {
  if (isNaN(new Number(ms))) {
    return ms;
  }
  seg = seg || "";
  levelCount = levelCount || 5;
  if (ms < 0) ms = -ms;
  const time = {
    天: Math.floor(ms / 86400000),
    小时: Math.floor(ms / 3600000) % 24,
    分钟: Math.floor(ms / 60000) % 60,
    秒: Math.floor(ms / 1000) % 60,
    毫秒: Math.floor(ms) % 1000,
  };
  return Object.entries(time)
    .filter((val) => val[1] !== 0)
    .map(([key, val]) => `${val}${key}`)
    .splice(0, levelCount)
    .join(seg);
}
