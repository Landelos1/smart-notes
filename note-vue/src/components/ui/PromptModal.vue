<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import { X, Edit3 } from 'lucide-vue-next'

interface Props {
  show?: boolean
  title?: string
  message?: string
  placeholder?: string
  defaultValue?: string
  confirmText?: string
  cancelText?: string
  maxLength?: number
}

const props = withDefaults(defineProps<Props>(), {
  show: false,
  title: '输入',
  message: '',
  placeholder: '请输入内容',
  defaultValue: '',
  confirmText: '确定',
  cancelText: '取消',
  maxLength: 50
})

const emit = defineEmits<{
  'update:show': [value: boolean]
  'confirm': [value: string]
  'cancel': []
}>()

const visible = ref(props.show)
const inputValue = ref(props.defaultValue)
const inputRef = ref<HTMLInputElement | null>(null)

watch(() => props.show, (newVal) => {
  visible.value = newVal
  if (newVal) {
    inputValue.value = props.defaultValue
    nextTick(() => {
      inputRef.value?.focus()
      inputRef.value?.select()
    })
  }
})

function handleConfirm() {
  const value = inputValue.value.trim()
  if (!value) {
    inputRef.value?.focus()
    return
  }
  emit('confirm', value)
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

function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Enter') {
    event.preventDefault()
    handleConfirm()
  } else if (event.key === 'Escape') {
    event.preventDefault()
    handleCancel()
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
        @click.self="handleCancel"
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
                <div class="p-2 rounded-full bg-blue-50">
                  <Edit3 class="w-5 h-5 text-blue-500" />
                </div>
                <h3 class="text-lg font-semibold text-gray-900">{{ title }}</h3>
              </div>
              <button
                class="text-gray-400 hover:text-gray-600 transition-colors"
                @click="handleCancel"
              >
                <X class="w-5 h-5" />
              </button>
            </div>

            <!-- Content -->
            <div class="px-6 py-5">
              <p v-if="message" class="text-gray-700 text-sm mb-4">{{ message }}</p>
              <input
                ref="inputRef"
                v-model="inputValue"
                type="text"
                :placeholder="placeholder"
                :maxlength="maxLength"
                class="w-full px-4 py-2.5 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all text-gray-900"
                @keydown="handleKeydown"
              />
              <div class="flex justify-between items-center mt-2">
                <span class="text-xs text-gray-400">
                  {{ inputValue.length }} / {{ maxLength }}
                </span>
                <span v-if="!inputValue.trim()" class="text-xs text-red-500">
                  内容不能为空
                </span>
              </div>
            </div>

            <!-- Footer -->
            <div class="px-6 py-4 bg-gray-50 flex justify-end gap-3">
              <button
                class="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-gray-500 transition-colors"
                @click="handleCancel"
              >
                {{ cancelText }}
              </button>
              <button
                :disabled="!inputValue.trim()"
                :class="[
                  'px-4 py-2 text-sm font-medium text-white rounded-md focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 transition-colors',
                  inputValue.trim() 
                    ? 'bg-blue-600 hover:bg-blue-700 cursor-pointer' 
                    : 'bg-gray-300 cursor-not-allowed'
                ]"
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
