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
import { IResponse } from '@/interface/request'
import axios, { AxiosError, AxiosInstance, AxiosRequestConfig, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { NO_NOTIFY_KEY, TOKEN_HEADER_KEY, CACHE_WORKSPACE_ID } from '@/utils/const'
import { refreshToken } from './user/user'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'
import { GlobalWindow } from '@/interface/common'
import Qs from 'qs'
import router from '../router'
import { base64Encode } from '@/utils/check-type'

const delTimeout: number = 20 * 1000
const jpomWindow_ = window as unknown as GlobalWindow
const apiTimeout: number = Number(jpomWindow_.apiTimeout === '<apiTimeout>' ? delTimeout : jpomWindow_.apiTimeout)
// debug routerBase
const routerBase: string = jpomWindow_.routerBase === '<routerBase>' ? '' : jpomWindow_.routerBase

const pro: boolean = process.env.NODE_ENV === 'production'

const baseURL = import.meta.env.JPOM_BASE_API_URL

const parseTransportEncryption = () => {
  if (jpomWindow_.transportEncryption === '<transportEncryption>') {
    return 'NONE'
  }
  return jpomWindow_.transportEncryption || 'NONE'
}
const transportEncryption = parseTransportEncryption()

// 创建实例
const instance: AxiosInstance = axios.create({
  baseURL: baseURL,

  timeout: apiTimeout || delTimeout,
  headers: {
    'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8'
  },
  responseType: 'json'
})

// 续签状态
let isRefreshing = false

const obj2base64 = (obj: any) => {
  if (obj instanceof Object && obj.constructor === Object) {
    const keys = Object.keys(obj)
    const newData: any = {}
    for (const key of keys) {
      const item = obj[key]
      if (typeof item === 'string' || typeof item === 'number' || typeof item === 'boolean') {
        newData[base64Encode(String(key))] = base64Encode(String(item))
      }
    }
    return newData
  } else if (obj instanceof FormData) {
    const newFormData: any = new FormData()
    for (const key of (obj as any).keys()) {
      const item = obj.get(key)
      if (typeof item === 'string' || typeof item === 'number' || typeof item === 'boolean') {
        newFormData.append(base64Encode(String(key)), base64Encode(String(item)))
      } else {
        newFormData.append(base64Encode(String(key)), item)
      }
    }
    return newFormData
  }
  if (Array.isArray(obj)) {
    return obj.map((item: any) => {
      if (typeof item === 'string' || typeof item === 'number' || typeof item === 'boolean') {
        item = base64Encode(String(item))
      }
      return item
    })
  }
  return obj
}

// 请求拦截
instance.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const appStore = useAppStore()
  const userStore = useUserStore()

  const { headers } = config
  headers[TOKEN_HEADER_KEY] = userStore.getToken()
  headers[CACHE_WORKSPACE_ID] = appStore.getWorkspaceId()
  const useGuideStore = guideStore()
  headers['Accept-Language'] = useGuideStore.getLocale()

  if (routerBase) {
    // 防止 url 出现 //
    config.url = (routerBase + config.url).replace(new RegExp('//', 'gm'), '/')
  }
  if (transportEncryption == 'BASE64') {
    if (headers['Content-Type'] === 'application/json') {
      if (config.data) {
        config.data = base64Encode(JSON.stringify(config.data))
      }
    } else {
      if (config.data) {
        config.data = obj2base64(config.data)
      }
      if (config.params) {
        config.params = obj2base64(config.params)
      }
    }
  }

  return config
})

// 响应拦截
instance.interceptors.response.use(
  function (response) {
    // 2xx 范围内的状态码都会触发该函数。
    return response
  },
  function (error: AxiosError) {
    // 无响应体
    if (!error.response) {
      $notification.error({
        key: 'network-error-no-response',
        message: 'Network Error No response',
        description: t('i18n_9ee0deb3c8') + error
      })
    } else if (!error.response.config.headers[NO_NOTIFY_KEY]) {
      const { status, statusText, data } = error.response
      if (!status) {
        $notification.error({
          key: 'network-error-no-response',
          message: 'Network Error',
          description: t('i18n_9ee0deb3c8') + error
        })
      } else {
        $notification.error({
          key: 'network-error-status-' + status,
          message: t('i18n_cc9a708364') + status,
          description: (statusText || '') + (data || '')
        })
      }
    }
    return Promise.reject(error)
  }
)

/**
 * 请求封装
 * @param url 接口地址
 * @param config AxiosRequestConfig
 * @returns IResponse<T>
 */
async function request<T = any>(url: string, config?: AxiosRequestConfig): Promise<IResponse<T>>
// eslint-disable-next-line no-redeclare
async function request<T = any>(config: AxiosRequestConfig): Promise<IResponse<T>>
// eslint-disable-next-line no-redeclare
async function request<T = any>(arg: string | AxiosRequestConfig, config?: AxiosRequestConfig): Promise<IResponse<T>> {
  config = config || {}
  const options =
    typeof arg === 'string'
      ? {
          url: arg,
          ...config
        }
      : arg
  const response = await instance.request<IResponse<T>>(options)
  const { data } = response
  // 登录失效
  if (data.code === 800) {
    toLogin(data, response)
    return Promise.reject(data)
  }

  // 需要续签
  if (data.code === 801) {
    const data2 = await handleRefreshTokenAndRetryQueue(response.config)
    return data2 as any
  }

  // 账号禁用
  if (data.code === 802) {
    toLogin(data, response)
    return Promise.reject()
  }

  // 禁止访问
  if (data.code === 999) {
    $notification.error({
      key: 'prohibit-access',
      message: t('i18n_bb9ef827bf'),
      description: t('i18n_f05e3ec44d')
    })
    window.location.href = jpomWindow_.routerBase + '/prohibit-access'
    return Promise.reject(data)
  }

  // 其他情况
  if (data.code !== 200) {
    // 如果 headers 里面配置了 tip: no 就不用弹出提示信息
    if (!response.config.headers[NO_NOTIFY_KEY]) {
      $notification.error({
        message: t('i18n_1a8f90122f') + (pro ? '' : response.config.url),
        description: data.msg
      })
      console.error(response.config.url, data)
    }
  }
  return data
}

export default request

// 异步续签并处理队列
async function handleRefreshTokenAndRetryQueue(config: InternalAxiosRequestConfig) {
  if (!isRefreshing) {
    isRefreshing = true
    return new Promise(async (resolve, reject) => {
      try {
        $notification.info({
          key: 'login-timeout',
          message: t('i18n_72d46ec2cf'),
          description: t('i18n_f652d8cca7')
        })
        // 执行续签操作
        const result = await refreshToken()
        if (result.code === 200) {
          // 更新用户登录状态或token
          const userStore = useUserStore()
          await userStore.login(result.data)
          $notification.success({
            key: 'login-timeout',
            message: t('i18n_72d46ec2cf'),
            description: t('i18n_a7c8eea801')
          })
          // 更新token到config中，以便于后续请求使用新token
          config.headers[TOKEN_HEADER_KEY] = userStore.getToken()
          const result2 = await request(config)
          resolve(result2)
          isRefreshing = false
        } else {
          toLoginOnly(t('i18n_72d46ec2cf'), t('i18n_edc1185b8e') + result.msg + t('i18n_d3fb6a7c83'))
          return
        }
      } catch (error) {
        //reject('Failed to refresh token:' + error)
        toLoginOnly(t('i18n_72d46ec2cf'), t('i18n_edc1185b8e') + error + t('i18n_d3fb6a7c83'))
      } finally {
      }
    })
  } else {
    return new Promise(async (resolve) => {
      const interval = setInterval(async () => {
        if (!isRefreshing) {
          clearInterval(interval)
          const userStore = useUserStore()
          config.headers[TOKEN_HEADER_KEY] = userStore.getToken()
          const result2 = await request(config)
          resolve(result2)
        } else {
          //
        }
      }, 100) // 检查间隔设置为100毫秒
    })
  }
}

function toLogin(res: IResponse<any>, response: AxiosResponse<IResponse<any>>) {
  return toLogin2(t('i18n_1a8f90122f') + (pro ? '' : response.config.url), res.msg)
}

function toLogin2(message: any, description: any) {
  $notification.warn({
    message: message,
    description: description,
    key: 'to-login'
  })
  const userStore = useUserStore()

  userStore.logOut().then(() => {
    const index = location.hash.indexOf('?')
    let params = {}
    if (index > -1) {
      params = Qs.parse(location.hash.substring(index + 1))
    }
    const pageUrl = router.resolve({
      path: '/login',
      query: params
    })

    setTimeout(() => {
      ;(location.href as any) = pageUrl.href
    }, 2000)
  })
  return false
}

function toLoginOnly(message: any, description: any) {
  $notification.warn({
    message: message,
    description: description,
    key: 'to-login'
  })
  const userStore = useUserStore()

  userStore.logOutOnly().then(() => {
    const index = location.hash.indexOf('?')
    let params = {}
    if (index > -1) {
      params = Qs.parse(location.hash.substring(index + 1))
    }
    const pageUrl = router.resolve({
      path: '/login',
      query: params
    })

    setTimeout(() => {
      ;(location.href as any) = pageUrl.href
    }, 2000)
  })
  return false
}

export function loadRouterBase(url: string, params: any) {
  const paramsObj = params || {}
  paramsObj[CACHE_WORKSPACE_ID] = useAppStore().getWorkspaceId()
  let queryStr = ''
  Object.keys(paramsObj).forEach((key, i) => {
    queryStr += `${i === 0 ? '' : '&'}${key}=${paramsObj[key]}`
  })
  return `${((baseURL + routerBase || '') + url).replace(new RegExp('//', 'gm'), '/')}?${queryStr}`
}

/**
 * 获取 socket 地址
 * @param {String} url 二级地址
 * @param {String} parameter 参数
 * @returns
 */
export function getWebSocketUrl(url: string, parameter: any) {
  const protocol: string = location.protocol === 'https:' ? 'wss://' : 'ws://'
  const fullUrl: string = (baseURL + routerBase + url).replace(new RegExp('//', 'gm'), '/')
  const useGuideStore = guideStore()
  parameter += `&lang=${useGuideStore.getLocale()}`
  return `${protocol}${location.host}${fullUrl}?${parameter}`
}
