import { IResponse } from './../interface/request.d'
import axios, { AxiosError, AxiosInstance, AxiosRequestConfig, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { NO_NOTIFY_KEY, TOKEN_HEADER_KEY, CACHE_WORKSPACE_ID } from '@/utils/const'
import { refreshToken } from './user/user'
import { notification } from 'ant-design-vue'
import appStore from '@/stores/app'
import useUserStore from '@/stores/user'
import { useMenuStore } from '@/stores/menu'

const _window = window as any
const delTimeout = 20 * 1000
const apiTimeout = _window.apiTimeout === '<apiTimeout>' ? delTimeout : _window.apiTimeout

const pro = process.env.NODE_ENV === 'production'

const userStore = useUserStore()

// 创建实例
const instance: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_APP_API_BASE_URL,

  timeout: apiTimeout || delTimeout,
  headers: {
    'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
  },
  responseType: 'json',
})

// 请求拦截
instance.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const { headers } = config
  const accessToken = localStorage.getItem('accessToken')
  headers['Authorization'] = accessToken ? 'Bearer ' + accessToken : ''
  headers[TOKEN_HEADER_KEY] = userStore.token
  headers[CACHE_WORKSPACE_ID] = appStore.getWorkspaceId

  if (_window.routerBase) {
    // 防止 url 出现 //
    config.url = (_window.routerBase + config.url).replace(new RegExp('//', 'gm'), '/')
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
      notification.error({
        message: 'Network Error No response',
        description: '网络开了小差！请重试...:' + error,
      })
    } else if (!error.response.config.headers[NO_NOTIFY_KEY]) {
      const { status, statusText, data } = error.response
      if (!status) {
        notification.error({
          message: 'Network Error',
          description: '网络开了小差！请重试...:' + error,
        })
      } else {
        notification.error({
          message: '状态码错误 ' + status,
          description: (statusText || '') + (data || ''),
        })
      }
    }
    return Promise.reject(error)
  }
)

/**
 * 请求封装
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
          ...config,
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
    notification.info({
      message: '登录信息过期，尝试自动续签...',
      description: '如果不需要自动续签，请修改配置文件。该续签将不会影响页面。',
    })
    redoRequest(response.config)
    return Promise.reject(data)
  }

  // 账号禁用
  if (data.code === 802) {
    toLogin(data, response)
    return Promise.reject()
  }

  // 禁止访问
  if (data.code === 999) {
    notification.error({
      message: '禁止访问',
      description: '禁止访问,当前IP限制访问',
    })
    window.location.href = _window.routerBase + '/system/ipAccess'
    return Promise.reject(data)
  }

  // 其他情况
  if (data.code !== 200) {
    // 如果 headers 里面配置了 tip: no 就不用弹出提示信息
    if (!response.config.headers[NO_NOTIFY_KEY]) {
      notification.error({
        message: '提示信息 ' + (pro ? '' : response.config.url),
        description: data.msg,
      })
      console.error(response.config.url, data)
    }
    return Promise.reject(data)
  }

  return data
}

// 刷新 jwt token 并且重试上次请求
async function redoRequest(config: AxiosRequestConfig) {
  const result = await refreshToken()
  if (result.code === 200) {
    // 调用 store action 存储当前登录的用户名和 token
    await userStore.reLogin(result.data)
    await useMenuStore().loadSystemMenus()
    request(config)
    return result
  }
  return Promise.reject()
}

function toLogin(res: IResponse<any>, response: AxiosResponse<IResponse<any>>) {
  notification.warn({
    message: '提示信息 ' + (pro ? '' : response.config.url),
    description: res.msg,
  })

  userStore.logOut()
  // .then(() => {
  //   const index = location.hash.indexOf("?");
  //   let params = {};
  //   if (index > -1) {
  //     params = Qs.parse(location.hash.substring(index + 1));
  //   }
  //   router.push({
  //     path: "/login",
  //     query: params,
  //   });
  // });
  return false
}

export default request
