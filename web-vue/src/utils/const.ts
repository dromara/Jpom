///
/// Copyright (c) 2019 Of Him Code Technology Studio
/// Jpom is licensed under Mulan PSL v2.
/// You can use this software according to the terms and conditions of the Mulan PSL v2.
/// You may obtain a copy of Mulan PSL v2 at:
/// 			http://license.coscl.org.cn/MulanPSL2
/// THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
/// See the Mulan PSL v2 for more details.
///

import { t } from '@/i18n'
import dayjs from 'dayjs'

// 常量池
export const USER_NAME_KEY = 'Jpom-UserName'

export const TOKEN_KEY = 'Jpom-Token'

export const LONG_TERM_TOKEN = 'Jpom-Long-Term-Token'

export const USER_INFO_KEY = 'Jpom-User'

export const MENU_KEY = 'Jpom-Menus'

export const TOKEN_HEADER_KEY = 'Authorization'

export const ACTIVE_TAB_KEY = 'Jpom-ActiveTab'

export const TAB_LIST_KEY = 'Jpom-TabList'

export const ACTIVE_MENU_KEY = 'Jpom-ActiveMenu'

export const MANAGEMENT_ACTIVE_TAB_KEY = 'Jpom-management-ActiveTab'

export const MANAGEMENT_TAB_LIST_KEY = 'Jpom-management-TabList'

export const MANAGEMENT_ACTIVE_MENU_KEY = 'Jpom-management-ActiveMenu'

// export const GUIDE_FLAG_KEY = "Jpom-GuideFlag";

// export const GUIDE_HOME_USED_KEY = "Jpom-Home-Guide-Used";

// export const GUIDE_NODE_USED_KEY = "Jpom-Node-Guide-Used";

export const NO_NOTIFY_KEY = 'tip'

export const NO_LOADING_KEY = 'loading'

/**
 * 分页选择条
 */
export const PAGE_DEFAULT_SIZW_OPTIONS = ['5', '10', '15', '20', '25', '30', '35', '40', '50']

/**
 * 缓存当前的工作空间 ID
 */
export const CACHE_WORKSPACE_ID = 'workspaceId'

/**
 * 升级 重启检查等待次数
 */
export const RESTART_UPGRADE_WAIT_TIME_COUNT = 80

/**
 * 压缩文件格式
 */
export const ZIP_ACCEPT = '.tar,.bz2,.gz,.zip,.tar.bz2,.tar.gz'

const cachePageLimitKeyName = 'page_limit'

export function getCachePageLimit(): number {
  return parseInt(localStorage.getItem(cachePageLimitKeyName) || '10')
}
/**
 * 展示总条数计算方法
 * @param {Number} total 总记录数
 * @returns String
 */
export function PAGE_DEFAULT_SHOW_TOTAL(total: number) {
  return t('i18n_1f1030554f', { total: total })
}

export const PAGE_DEFAULT_LIST_QUERY: any = {
  page: 1,
  limit: isNaN(getCachePageLimit()) ? 10 : getCachePageLimit(),
  total: 0
}

/**
 * 计算分页数据
 * @param {JSON} queryParam 分页参数
 * @param {Array} pageSizeOptions 分页选择条选项
 * @returns
 */
export function COMPUTED_PAGINATION(queryParam: any, pageSizeOptions = PAGE_DEFAULT_SIZW_OPTIONS) {
  // console.log(queryParam)
  const limit = queryParam.limit || PAGE_DEFAULT_LIST_QUERY.limit
  const total = queryParam.total || 0
  return {
    total: total,
    current: queryParam.page || 1,
    pageSize: limit,
    pageSizeOptions: pageSizeOptions || PAGE_DEFAULT_SIZW_OPTIONS,
    showSizeChanger: true,
    showQuickJumper: true,
    showLessItems: true,
    // 只有在分页条数在 小于 2 的时候隐藏，避免设置太大无法切回
    hideOnSinglePage: limit <= 20,
    showTotal: (total: number) => {
      return PAGE_DEFAULT_SHOW_TOTAL(total)
    }
  }
}

/**
 * 分页切换
 * @param {JSON} listQuery
 * @param {JSON} param1
 * @returns
 */
export function CHANGE_PAGE(listQuery: { [key: string]: any }, { pagination, sorter }: any) {
  if (pagination && Object.keys(pagination).length) {
    let limit = pagination.pageSize || pagination.limit || listQuery.limit
    if (limit === -1) {
      limit = getCachePageLimit()
    }
    listQuery = {
      ...listQuery,
      page: pagination.current || listQuery.page,
      limit: limit
    }

    //
    localStorage.setItem(cachePageLimitKeyName, limit)
    //
    PAGE_DEFAULT_LIST_QUERY.limit = limit
  }
  if (sorter && Object.keys(sorter).length) {
    listQuery = { ...listQuery, order: sorter.order, order_field: sorter.field }
  }
  return listQuery
}

/**
 * 并发执行
 * @params list {Array} - 要迭代的数组
 * @params limit {Number} - 并发数量控制数,最好小于3
 * @params asyncHandle {Function} - 对`list`的每一个项的处理函数，参数为当前处理项，必须 return 一个Promise来确定是否继续进行迭代
 * @return {Promise} - 返回一个 Promise 值来确认所有数据是否迭代完成
 */
export function concurrentExecution(list: any[], limit: number, asyncHandle: any) {
  // 递归执行
  const recursion = (arr: any) => {
    // 执行方法 arr.shift() 取出并移除第一个数据
    return asyncHandle(arr.shift()).then((res: any) => {
      // 数组还未迭代完，递归继续进行迭代
      if (arr.length !== 0) {
        return recursion(arr)
      } else {
        return res
      }
    })
  }
  // 创建新的并发数组
  const listCopy = [...list]
  // 正在进行的所有并发异步操作
  const asyncList = []
  limit = limit > listCopy.length ? listCopy.length : limit

  while (limit--) {
    asyncList.push(recursion(listCopy))
  }
  // 所有并发异步操作都完成后，本次并发控制迭代完成
  return Promise.all(asyncList)
}

/**
 * 并发执行任务
 * @param list 任务列表
 * @param limit 并发控制
 * @param asyncHandle 任务处理函数
 */
export async function concurrentJobs(list: any[], limit: number, asyncHandle: any) {
  const arr = [...list]
  const result = []
  for (let i = 0; i < arr.length; i += limit) {
    result.push(...(await Promise.allSettled(arr.slice(i, i + limit).map(asyncHandle))))
  }
  return result
}

export function readJsonStrField(json: string, key: string) {
  try {
    const data = JSON.parse(json)[key] || ''
    if (Object.prototype.toString.call(data) === '[object Object]') {
      return JSON.stringify(data)
    }
    return data
  } catch (e) {
    //
  }
  return ''
}

export function randomStr(len = 2) {
  const $chars = 'ABCDEFGHJKMNPQRSTWXYZ0123456789'
  /****默认去掉了容易混淆的字符oOLl,9gq,Vv,Uu,I1****/
  const maxPos = $chars.length
  let repliccaId = ''
  for (let i = 0; i < len; i++) {
    repliccaId += $chars.charAt(Math.floor(Math.random() * maxPos))
  }
  return repliccaId
}

/**
 * 转换时间函数
 * @param {*} time
 * @param {*} cFormat
 */
export function parseTime(time: any, cFormat = 'YYYY-MM-DD HH:mm:ss') {
  if (arguments.length === 0) {
    return '-'
  }
  if (!time) {
    return '-'
  }
  // 处理 time 参数
  if (isNaN(Number(time)) === false) {
    time = Number(time)
  }
  const format = cFormat || 'YYYY-MM-DD HH:mm:ss'
  let date
  if (typeof time === 'object') {
    date = time
  } else {
    if (('' + time).length === 10) time = parseInt(time) * 1000
    date = new Date(time)
  }

  return dayjs(date).format(format)
}

/**
 * 格式化文件大小
 * @param {*} value
 * @param defaultValue
 * @returns
 */
export function renderSize(value: any, defaultValue = '-') {
  return formatUnits(value, 1024, ['Bytes', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'], defaultValue)
}

/**
 * 格式化网络大小
 * @param {*} value
 * @param defaultValue
 * @returns
 */
export function renderBpsSize(value: any, defaultValue = '-') {
  return formatUnits(value, 1024, ['bps', 'Kbps', 'Mbps', 'Gbps', 'Tbps', 'Pbps', 'Ebps', 'Zbps', 'Ybps'], defaultValue)
}

/**
 * 格式化文件大小
 * @param {*} value
 * @param defaultValue
 * @returns
 */
export function formatUnits(value: any, base: number, unitArr: string[], defaultValue = '-') {
  if (null == value || value === '') {
    return defaultValue
  }

  let index = 0
  const srcsize = parseFloat(value)
  if (srcsize <= 0) {
    return defaultValue
  }
  // console.log(value, srcsize);
  index = Math.floor(Math.log(srcsize) / Math.log(base))
  const size = srcsize / Math.pow(base, index)
  //保留的小数位数
  return size.toFixed(2) + unitArr[index]
}

/**
 * 数组分组  [{id:1,value:1},{id:2,value:3}] => {1:{id:1,value:1},2:{id:2,value:3}}
 * @param {function} group
 * @returns Object
 */
declare global {
  interface Array<T> {
    groupBy(fn: (ix: T) => any): any[]
  }
}
if (!Array.prototype.groupBy) {
  Object.defineProperty(Array.prototype, 'groupBy', {
    enumerable: false,
    writable: false,
    configurable: false,
    value: function groupBy<T>(this: T[], group: (ix: T) => any): any[] {
      return this.reduce(function (c: any, v: any) {
        const k = group(v)
        c[k] = v
        return c
      }, {})
    }
  })
}
// Array.prototype.groupBy = function (group: any) {
//   return group && typeof group === 'function'
//     ?
//     : this
// }
//
export function itemGroupBy(arr: any[], groupKey: string, key: string, dataKey: string) {
  key = key || 'type'
  dataKey = dataKey || 'data'

  const newArr: any[] = []
  const types: { [key: string]: any } = {}
  let i, j, cur
  for (i = 0, j = arr.length; i < j; i++) {
    cur = arr[i]
    if (!(cur[groupKey] in types)) {
      types[cur[groupKey]] = { [key]: cur[groupKey], [dataKey]: [] }
      newArr.push(types[cur[groupKey]])
    }
    types[cur[groupKey]][dataKey].push(cur)
  }
  return newArr
}

/**
 * 格式化时长(毫秒)
 * @param {String} ms
 * @param {String} seg 分割符
 * @param {String} levelCount 格式化个数
 * @returns
 */
export function formatDuration(ms: any, seg: string = ',', levelCount: number = 5) {
  let msNum = Number(ms)
  if (isNaN(msNum)) {
    return ms
  }
  if (msNum === 0) {
    return '-'
  }

  seg = seg || ''
  levelCount = levelCount || 5
  if (msNum < 0) msNum = -msNum
  const time: { [key: string]: number } = {}
  ;(time[t('i18n_249aba7632')] = Math.floor(msNum / 86400000)),
    (time[t('i18n_2de0d491d0')] = Math.floor(msNum / 3600000) % 24),
    (time[t('i18n_3a17b7352e')] = Math.floor(msNum / 60000) % 60),
    (time[t('i18n_0c1fec657f')] = Math.floor(msNum / 1000) % 60),
    (time[t('i18n_21157cbff8')] = Math.floor(msNum) % 1000)
  return Object.entries(time)
    .filter((val) => val[1] !== 0)
    .map(([key, val]) => `${val}${key}`)
    .splice(0, levelCount)
    .join(seg)
}

//小数转换为分数(小数先转换成number类型，再乘以100，并且保留2位小数)
export function formatPercent(point: any, keep = 2) {
  if (!point) {
    return '-'
  }
  return formatPercent2(Number(point) * 100, keep)
}

//小数转换为分数(小数先转换成number类型，并且保留2位小数)
export function formatPercent2(point: any, keep = 2) {
  if (null == point) {
    return '-'
  }
  const percent = Number(Number(point).toFixed(keep))
  return percent + '%'
}

//小数转换为分数(小数先转换成number类型，再乘以100，并且保留2位小数)
export function formatPercent2Number(point: any, keep = 2) {
  if (null == point) {
    return 0
  }
  return Number(Number(point).toFixed(keep))
}

export function compareVersion(version1: string, version2: string) {
  if (version1 == null && version2 == null) {
    return 0
  } else if (version1 == null) {
    // null视为最小版本，排在前
    return -1
  } else if (version2 == null) {
    return 1
  }

  if (version1 === version2) {
    return 0
  }

  const v1s = version1.split('.')
  const v2s = version2.split('.')

  let diff = 0
  const minLength = Math.min(v1s.length, v2s.length) // 取最小长度值

  for (let i = 0; i < minLength; i++) {
    const v1 = v1s[i]
    const v2 = v2s[i]
    // 先比较长度
    diff = v1.length - v2.length
    if (0 === diff) {
      diff = v1.localeCompare(v2)
    }
    if (diff !== 0) {
      //已有结果，结束
      break
    }
  }

  // 如果已经分出大小，则直接返回，如果未分出大小，则再比较位数，有子版本的为大；
  return diff !== 0 ? diff : v1s.length - v2s.length
}

// 当前页面构建信息
export function pageBuildInfo() {
  const htmlVersion = (document.head.querySelector('[name~=jpom-version][content]') as any)?.content
  const buildTime = (document.head.querySelector('[name~=build-time][content]') as any)?.content
  const buildEnv = (document.head.querySelector('[name~=build-env][content]') as any)?.content
  return {
    v: htmlVersion,
    t: buildTime,
    e: buildEnv,
    df: (document.title || '').toLowerCase().includes('jpom'),
    t2: Date.now()
  }
}

/**
 * 拖拽数据处理 - vue3-smooth-dnd
 * @param arr 原数组
 * @param dragResult onDrop 结果集
 * @returns
 */
export const dropApplyDrag = <T = any>(
  arr: T[],
  dragResult: { removedIndex: number; addedIndex: number; payload: T }
) => {
  const { removedIndex, addedIndex, payload } = dragResult
  if (removedIndex === null && addedIndex === null) return arr
  const result = [...arr]
  let itemToAdd = payload
  if (removedIndex !== null) {
    itemToAdd = result.splice(removedIndex, 1)[0]
  }
  if (addedIndex !== null) {
    result.splice(addedIndex, 0, itemToAdd)
  }
  return result
}
