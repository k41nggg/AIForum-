import { ref } from 'vue'

export type ToastType = 'info' | 'success' | 'error'

export type ToastState = {
  show: boolean
  type: ToastType
  title: string
  message: string
}

export const toast = ref<ToastState>({
  show: false,
  type: 'info',
  title: '',
  message: ''
})

let timer: number | undefined

export function showToast(type: ToastType, title: string, message: string, ms = 2600) {
  toast.value = { show: true, type, title, message }
  if (timer) window.clearTimeout(timer)
  timer = window.setTimeout(() => {
    toast.value.show = false
  }, ms)
}
