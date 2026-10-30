<script setup lang="ts">
import { ref, watch } from 'vue'
import { X, BookOpen } from 'lucide-vue-next'

interface Props {
  show?: boolean
  title?: string
  placeholder?: string
  confirmText?: string
  cancelText?: string
  maxLength?: number
}

const props = withDefaults(defineProps<Props>(), {
  show: false,
  title: '输入',
  placeholder: '请输入内容',
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
const inputValue = ref('')
const inputRef = ref<HTMLInputElement | null>(null)

watch(() => props.show, (newVal) => {
  visible.value = newVal
  if (newVal) {
    inputValue.value = ''
    // 等待 DOM 更新后聚焦输入框
    setTimeout(() => {
      inputRef.value?.focus()
    }, 100)
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
  inputValue.value = ''
  emit('update:show', false)
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter') {
    handleConfirm()
  } else if (e.key === 'Escape') {
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
                <div class="bg-blue-50 p-2 rounded-full">
                  <BookOpen class="w-5 h-5 text-blue-500" />
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
              <input
                ref="inputRef"
                v-model="inputValue"
                type="text"
                :placeholder="placeholder"
                :maxlength="maxLength"
                class="w-full px-4 py-2.5 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent text-base"
                @keydown="handleKeydown"
              />
              <p class="mt-2 text-xs text-gray-500">
                {{ inputValue.length }}/{{ maxLength }}
              </p>
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
                class="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                :disabled="!inputValue.trim()"
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
