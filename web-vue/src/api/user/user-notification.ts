import axios from '../config'

export type UserNotificationType = {
  level?: 'error' | 'success' | 'warning' | 'info' | undefined
  closable?: boolean
  title?: string
  content?: string
  enabled?: boolean
}

// 编辑
export function saveUserNotification(params: UserNotificationType) {
  return axios({
    url: '/user/notification/save',
    method: 'post',
    data: params
  })
}

// 获取通知
export function getUserNotification() {
  return axios({
    url: '/user/notification/get',
    method: 'get'
  })
}

export function systemNotification() {
  return axios({
    url: '/system-notification',
    method: 'get'
  })
}
