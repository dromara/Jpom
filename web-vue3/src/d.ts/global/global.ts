import { GlobalWindow } from '@/interface/common'
import { useAppStore } from '@/stores/app'
import { message, notification, Modal } from 'ant-design-vue'
import { useUserStore } from '@/stores/user'

export const jpomWindow = window as unknown as GlobalWindow
// 注册全局的组件
export const $message = message
export const $notification = notification
//
export const $confirm = Modal.confirm
export const $info = Modal.info
export const $error = Modal.error
export const $warning = Modal.warning
export const $success = Modal.success
// export const $route = useRoute()
// export const $router = useRouter()

$notification.config({
  top: '100px',
  duration: 4
})

$message.config({ duration: 4 })

export const appStore = () => {
  return useAppStore()
}

export const userStore = () => {
  return useUserStore()
}

export const router = () => {
  return useRouter()
}

export const route = () => {
  return useRoute()
}
