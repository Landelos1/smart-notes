import { ref, createApp, h } from 'vue'
import Modal from '@/components/ui/Modal.vue'

interface ModalOptions {
  title?: string
  message: string
  type?: 'confirm' | 'alert' | 'info' | 'success' | 'warning' | 'error' | 'prompt'
  confirmText?: string
  cancelText?: string
  showCancel?: boolean
}

interface PromptOptions {
  title?: string
  message?: string
  placeholder?: string
  defaultValue?: string
  confirmText?: string
  cancelText?: string
  inputType?: 'text' | 'select'
  selectOptions?: Array<{ label: string; value: string }>
}

export function useModal() {
  // 确认对话框
  const confirm = (options: ModalOptions): Promise<boolean> => {
    return new Promise((resolve) => {
      const container = document.createElement('div')
      document.body.appendChild(container)

      const show = ref(true)
      let isCleanedUp = false // 防止重复清理

      const onConfirm = () => {
        cleanup()
        resolve(true)
      }

      const onCancel = () => {
        cleanup()
        resolve(false)
      }

      const cleanup = () => {
        if (isCleanedUp) return
        isCleanedUp = true
        
        show.value = false
        setTimeout(() => {
          try {
            app.unmount()
            if (container.parentNode) {
              document.body.removeChild(container)
            }
          } catch (error) {
            console.warn('[Modal] cleanup error:', error)
          }
        }, 300)
      }

      const app = createApp({
        render() {
          return h(Modal, {
            show: show.value,
            title: options.title || '确认',
            message: options.message,
            type: options.type || 'confirm',
            confirmText: options.confirmText || '确定',
            cancelText: options.cancelText || '取消',
            showCancel: options.showCancel !== false,
            'onUpdate:show': (value: boolean) => {
              if (!value) {
                onCancel()
              }
            },
            onConfirm,
            onCancel
          })
        }
      })

      app.mount(container)
    })
  }

  // 提示对话框
  const alert = (options: ModalOptions): Promise<void> => {
    return new Promise((resolve) => {
      const container = document.createElement('div')
      document.body.appendChild(container)

      const show = ref(true)
      let isCleanedUp = false // 防止重复清理

      const onConfirm = () => {
        cleanup()
        resolve()
      }

      const cleanup = () => {
        if (isCleanedUp) return
        isCleanedUp = true
        
        show.value = false
        setTimeout(() => {
          try {
            app.unmount()
            if (container.parentNode) {
              document.body.removeChild(container)
            }
          } catch (error) {
            console.warn('[Modal] cleanup error:', error)
          }
        }, 300)
      }

      const app = createApp({
        render() {
          return h(Modal, {
            show: show.value,
            title: options.title || '提示',
            message: options.message,
            type: options.type || 'info',
            confirmText: options.confirmText || '确定',
            showCancel: false,
            'onUpdate:show': (value: boolean) => {
              if (!value) {
                onConfirm()
              }
            },
            onConfirm
          })
        }
      })

      app.mount(container)
    })
  }

  // 成功提示
  const success = (message: string, title?: string): Promise<void> => {
    return alert({
      message,
      title: title || '成功',
      type: 'success'
    })
  }

  // 错误提示
  const error = (message: string, title?: string): Promise<void> => {
    return alert({
      message,
      title: title || '错误',
      type: 'error'
    })
  }

  // 警告提示
  const warning = (message: string, title?: string): Promise<void> => {
    return alert({
      message,
      title: title || '警告',
      type: 'warning'
    })
  }

  // 信息提示
  const info = (message: string, title?: string): Promise<void> => {
    return alert({
      message,
      title: title || '提示',
      type: 'info'
    })
  }

  // 输入对话框
  const prompt = (options: PromptOptions): Promise<string | null> => {
    return new Promise((resolve) => {
      const container = document.createElement('div')
      document.body.appendChild(container)

      const show = ref(true)
      let isCleanedUp = false // 防止重复清理

      const onConfirm = (value?: string) => {
        cleanup()
        resolve(value || '')
      }

      const onCancel = () => {
        cleanup()
        resolve(null)
      }

      const cleanup = () => {
        if (isCleanedUp) return
        isCleanedUp = true
        
        show.value = false
        setTimeout(() => {
          try {
            app.unmount()
            if (container.parentNode) {
              document.body.removeChild(container)
            }
          } catch (error) {
            console.warn('[Modal] cleanup error:', error)
          }
        }, 300)
      }

      const app = createApp({
        render() {
          return h(Modal, {
            show: show.value,
            title: options.title || '输入',
            message: options.message || '',
            type: 'prompt',
            confirmText: options.confirmText || '确定',
            cancelText: options.cancelText || '取消',
            showCancel: true,
            showInput: true,
            inputPlaceholder: options.placeholder || '请输入...',
            inputValue: options.defaultValue || '',
            inputType: options.inputType || 'text',
            selectOptions: options.selectOptions || [],
            'onUpdate:show': (value: boolean) => {
              if (!value) {
                onCancel()
              }
            },
            onConfirm,
            onCancel
          })
        }
      })

      app.mount(container)
    })
  }

  return {
    confirm,
    alert,
    success,
    error,
    warning,
    info,
    prompt
  }
}
