import { GlobalWindow } from '@/interface/common'

export const jpomWindow = window as unknown as GlobalWindow

import { message, notification, Modal } from 'ant-design-vue'

// 注册全局的组件
export const $message = message
export const $notification = notification
//
export const $confirm = Modal.confirm
export const $info = Modal.info
export const $error = Modal.error
export const $warning = Modal.warning
export const $success = Modal.success

$notification.config({
  top: '100px',
  duration: 4
})

$message.config({ duration: 4 })
