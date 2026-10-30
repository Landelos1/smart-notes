<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import { X, AlertCircle, CheckCircle, Info, AlertTriangle, Edit3 } from 'lucide-vue-next'

interface Props {
  show?: boolean
  title?: string
  message?: string
  type?: 'confirm' | 'alert' | 'info' | 'success' | 'warning' | 'error' | 'prompt'
  confirmText?: string
  cancelText?: string
  showCancel?: boolean
  showInput?: boolean
  inputPlaceholder?: string
  inputValue?: string
  inputType?: 'text' | 'select'
  selectOptions?: Array<{ label: string; value: string }>
}

const props = withDefaults(defineProps<Props>(), {
  show: false,
  title: '提示',
  message: '',
  type: 'confirm',
  confirmText: '确定',
  cancelText: '取消',
  showCancel: true,
  showInput: false,
  inputPlaceholder: '请输入...',
  inputValue: '',
  inputType: 'text',
  selectOptions: () => []
})

const emit = defineEmits<{
  'update:show': [value: boolean]
  'confirm': [value?: string]
  'cancel': []
}>()

const visible = ref(props.show)
const inputText = ref(props.inputValue)
const inputRef = ref<HTMLInputElement | HTMLSelectElement | null>(null)

watch(() => props.show, (newVal) => {
  visible.value = newVal
  if (newVal && props.showInput) {
    inputText.value = props.inputValue
    nextTick(() => {
      inputRef.value?.focus()
    })
  }
})

watch(() => props.inputValue, (newVal) => {
  inputText.value = newVal
})

function handleConfirm() {
  if (props.showInput) {
    emit('confirm', inputText.value)
  } else {
    emit('confirm')
  }
  close()
}

function handleCancel() {
  emit('cancel')
  close()
}

function close() {
  visible.value = false
  emit('update:show', false)
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleConfirm()
  }
}

// 根据类型获取图标和颜色
const getIconConfig = () => {
  switch (props.type) {
    case 'success':
      return { icon: CheckCircle, color: 'text-green-500', bgColor: 'bg-green-50' }
    case 'warning':
      return { icon: AlertTriangle, color: 'text-yellow-500', bgColor: 'bg-yellow-50' }
    case 'error':
      return { icon: AlertCircle, color: 'text-red-500', bgColor: 'bg-red-50' }
    case 'info':
      return { icon: Info, color: 'text-blue-500', bgColor: 'bg-blue-50' }
    case 'prompt':
      return { icon: Edit3, color: 'text-blue-500', bgColor: 'bg-blue-50' }
    default:
      return { icon: AlertCircle, color: 'text-gray-500', bgColor: 'bg-gray-50' }
  }
}
</script>

<template>
  <Teleport to="body">
    <Transition
      enter-active-class="transition-opacity duration-200"
      enter-from-class="opacity-0"
      enter-to-class="opacity-100"
      leave-active-class="transition-opacity duration-200"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0"
    >
      <div
        v-if="visible"
        class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4"
        @click.self="showCancel ? handleCancel() : null"
      >
        <Transition
          enter-active-class="transition-all duration-200"
          enter-from-class="opacity-0 scale-95"
          enter-to-class="opacity-100 scale-100"
          leave-active-class="transition-all duration-200"
          leave-from-class="opacity-100 scale-100"
          leave-to-class="opacity-0 scale-95"
        >
          <div
            v-if="visible"
            class="bg-white rounded-lg shadow-2xl max-w-md w-full overflow-hidden"
            @click.stop
          >
            <!-- Header -->
            <div class="px-6 py-4 border-b border-gray-200 flex items-center justify-between">
              <div class="flex items-center gap-3">
                <div :class="[getIconConfig().bgColor, 'p-2 rounded-full']">
                  <component :is="getIconConfig().icon" :class="[getIconConfig().color, 'w-5 h-5']" />
                </div>
                <h3 class="text-lg font-semibold text-gray-900">{{ title }}</h3>
              </div>
              <button
                v-if="showCancel"
                class="text-gray-400 hover:text-gray-600 transition-colors"
                @click="handleCancel"
              >
                <X class="w-5 h-5" />
              </button>
            </div>

            <!-- Content -->
            <div class="px-6 py-5">
              <p v-if="message" class="text-gray-700 text-base leading-relaxed" :class="{ 'mb-4': showInput }">{{ message }}</p>
              <!-- 输入框 / 下拉选择 -->
              <input
                v-if="showInput && inputType === 'text'"
                ref="inputRef"
                v-model="inputText"
                type="text"
                :placeholder="inputPlaceholder"
                class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-sm"
                @keydown="handleKeydown"
              />
              <select
                v-else-if="showInput && inputType === 'select'"
                ref="inputRef"
                v-model="inputText"
                class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-sm"
              >
                <option
                  v-for="option in selectOptions"
                  :key="option.value"
                  :value="option.value"
                >
                  {{ option.label }}
                </option>
              </select>
            </div>

            <!-- Footer -->
            <div class="px-6 py-4 bg-gray-50 flex justify-end gap-3">
              <button
                v-if="showCancel"
                class="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-gray-500 transition-colors"
                @click="handleCancel"
              >
                {{ cancelText }}
              </button>
              <button
                class="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 transition-colors"
                @click="handleConfirm"
              >
                {{ confirmText }}
              </button>
            </div>
          </div>
        </Transition>
      </div>
    </Transition>
  </Teleport>
</template>
