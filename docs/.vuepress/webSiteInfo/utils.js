// 日期格式化(只获取年月日)
export function dateFormat(date) {
  if (!(date instanceof Date)) {
    date = new Date(date);
  }
  return `${date.getUTCFullYear()}-${zero(date.getUTCMonth() + 1)}-${zero(date.getUTCDate())}`;
}

// 小于10补0
export function zero(d) {
  return d.toString().padStart(2, '0');
}

/**
 * 计算最后活动时间
 */
export function lastUpdatePosts(posts) {
  posts.sort((prev, next) => {
    return compareDate(prev, next);
  });
  return posts;
}

// 获取时间的时间戳
export function getTimeNum(post) {
  let dateStr = post.lastUpdated || post.frontmatter.date;
  let date = new Date(dateStr);
  if (date == "Invalid Date" && dateStr) { // 修复new Date()在Safari下出现Invalid Date的问题
    date = new Date(dateStr.replace(/-/g, '/'));
  }
  return date.getTime();
}

// 比对时间
export function compareDate(a, b) {
  return getTimeNum(b) - getTimeNum(a);
}

/**
 * 获取两个日期相差多少天
 */
export function dayDiff(startDate, endDate) {
  if (!endDate) {
    endDate = startDate;
    startDate = new Date();
  }
  startDate = dateFormat(startDate);
  endDate = dateFormat(endDate);
  let day = parseInt(Math.abs(new Date(startDate) - new Date(endDate)) / (1000 * 60 * 60 * 24));
  return day;
}

/**
 * 计算相差多少年/月/日/时/分/秒
 */
export function timeDiff(startDate, endDate) {
  if (!endDate) {
    endDate = startDate;
    startDate = new Date();
  }
  if (!(startDate instanceof Date)) {
    startDate = new Date(startDate);
  }
  if (!(endDate instanceof Date)) {
    endDate = new Date(endDate);
  }
  // 计算时间戳的差
  const diffValue = parseInt((Math.abs(endDate - startDate) / 1000));
  if (diffValue == 0) {
    return '刚刚';
  } else if (diffValue < 60) {
    return diffValue + ' 秒';
  } else if (parseInt(diffValue / 60) < 60) {
    return parseInt(diffValue / 60) + ' 分';
  } else if (parseInt(diffValue / (60 * 60)) < 24) {
    return parseInt(diffValue / (60 * 60)) + ' 时';
  } else if (parseInt(diffValue / (60 * 60 * 24)) < getDays(startDate.getMonth, startDate.getFullYear)) {
    return parseInt(diffValue / (60 * 60 * 24)) + ' 天';
  } else if (parseInt(diffValue / (60 * 60 * 24 * getDays(startDate.getMonth, startDate.getFullYear))) < 12) {
    return parseInt(diffValue / (60 * 60 * 24 * getDays(startDate.getMonth, startDate.getFullYear))) + ' 月';
  } else {
    return parseInt(diffValue / (60 * 60 * 24 * getDays(startDate.getMonth, startDate.getFullYear) * 12)) + ' 年';
  }
}

/**
 * 判断当前月的天数（28、29、30、31）
 */
export function getDays(mouth, year) {
  let days = 30;
  if (mouth === 2) {
    days = year % 4 === 0 ? 29 : 28;
  } else if (mouth === 1 || mouth === 3 || mouth === 5 || mouth === 7 || mouth === 8 || mouth === 10 || mouth === 12) {
    // 月份为：1,3,5,7,8,10,12 时，为大月.则天数为 31；
    days = 31;
  }
  return days;
}

/**
 * 已运行时间低于一天显示时分秒
 * 目前该函数没有使用，低于一天直接显示不到一天
 */
export function getTime(startDate, endDate) {
  if (day < 0) {
    let hour = parseInt(Math.abs(new Date(startDate) - new Date(endDate)) / (1000 * 60 * 60));
    if (hour > 0) {
      let minute = parseInt(Math.abs(new Date(startDate) - new Date(endDate) - hour * 60 * 60 * 1000) / (1000 * 60));
      if (minute > 0) {
        let second = parseInt(Math.abs(new Date(startDate) - new Date(endDate) - hour * 60 * 60 * 1000 - minute * 60 * 1000) / (1000));
        if (second != 0) {
          return hour + ' 小时 ' + minute + ' 分钟 ' + second + ' 秒';
        } else {
          return hour + ' 小时 ' + minute + ' 分钟 ';
        }
      } else {
        return hour + ' 小时 ';
      }
    } else {
      let minute = parseInt(Math.abs(new Date(startDate) - new Date(endDate) - hour * 60 * 60 * 1000) / (1000 * 60));
      if (minute > 0) {
        let second = parseInt(Math.abs(new Date(startDate) - new Date(endDate) - hour * 60 * 60 * 1000 - minute * 60 * 1000) / (1000));
        if (second != 0) {
          return + minute + ' 分钟 ' + second + ' 秒';
        } else {
          return minute + ' 分钟 ';
        }
      } else {
        return parseInt(Math.abs(new Date(startDate) - new Date(endDate) - hour * 60 * 60 * 1000 - minute * 60 * 1000) / (1000)) + ' 秒 ';
      }
    }
  }
}
