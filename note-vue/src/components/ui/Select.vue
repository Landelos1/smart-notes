<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { cn } from './utils'
import { ChevronDown, Check } from 'lucide-vue-next'

interface SelectOption {
  value: string
  label: string
  disabled?: boolean
}

interface Props {
  modelValue?: string
  options: SelectOption[]
  placeholder?: string
  disabled?: boolean
  class?: string
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: '',
  placeholder: '请选择',
  disabled: false,
  class: ''
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const isOpen = ref(false)
const selectRef = ref<HTMLDivElement | null>(null)

const selectedOption = computed(() => {
  return props.options.find(opt => opt.value === props.modelValue)
})

const triggerClass = computed(() => {
  return cn(
    'flex h-9 w-full items-center justify-between rounded-md border border-gray-300 bg-white px-3 py-2 text-sm transition-colors',
    'focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent',
    'disabled:cursor-not-allowed disabled:opacity-50',
    props.disabled ? 'cursor-not-allowed opacity-50' : 'cursor-pointer hover:bg-gray-50',
    props.class
  )
})

function toggleOpen() {
  if (!props.disabled) {
    isOpen.value = !isOpen.value
  }
}

function selectOption(option: SelectOption) {
  if (option.disabled) return
  emit('update:modelValue', option.value)
  isOpen.value = false
}

function handleClickOutside(event: MouseEvent) {
  if (selectRef.value && !selectRef.value.contains(event.target as Node)) {
    isOpen.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<template>
  <div ref="selectRef" class="relative">
    <button
      type="button"
      :class="triggerClass"
      :disabled="disabled"
      @click="toggleOpen"
    >
      <span :class="selectedOption ? 'text-gray-900' : 'text-gray-500'">
        {{ selectedOption?.label || placeholder }}
      </span>
      <ChevronDown 
        :class="['w-4 h-4 text-gray-500 transition-transform', isOpen ? 'rotate-180' : '']" 
      />
    </button>

    <Transition
      enter-active-class="transition ease-out duration-100"
      enter-from-class="transform opacity-0 scale-95"
      enter-to-class="transform opacity-100 scale-100"
      leave-active-class="transition ease-in duration-75"
      leave-from-class="transform opacity-100 scale-100"
      leave-to-class="transform opacity-0 scale-95"
    >
      <div
        v-if="isOpen"
        class="absolute z-50 mt-1 w-full rounded-md border border-gray-200 bg-white shadow-lg"
      >
        <div class="max-h-60 overflow-auto py-1">
          <button
            v-for="option in options"
            :key="option.value"
            type="button"
            :disabled="option.disabled"
            :class="[
              'flex w-full items-center justify-between px-3 py-2 text-sm transition-colors',
              option.disabled 
                ? 'cursor-not-allowed text-gray-400' 
                : 'cursor-pointer hover:bg-gray-100',
              option.value === modelValue ? 'bg-blue-50 text-blue-600' : 'text-gray-900'
            ]"
            @click="selectOption(option)"
          >
            <span>{{ option.label }}</span>
            <Check 
              v-if="option.value === modelValue" 
              class="w-4 h-4 text-blue-600" 
            />
          </button>
        </div>
      </div>
    </Transition>
  </div>
</template>
