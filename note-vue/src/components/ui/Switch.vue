<script setup lang="ts">
import { computed } from 'vue'
import { cn } from './utils'

interface Props {
  modelValue?: boolean
  disabled?: boolean
  class?: string
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: false,
  disabled: false,
  class: ''
})

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const switchClass = computed(() => {
  return cn(
    'peer inline-flex h-5 w-9 shrink-0 cursor-pointer items-center rounded-full border-2 border-transparent transition-colors',
    'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:ring-offset-2',
    'disabled:cursor-not-allowed disabled:opacity-50',
    props.modelValue ? 'bg-blue-600' : 'bg-gray-200',
    props.disabled ? 'cursor-not-allowed opacity-50' : '',
    props.class
  )
})

const thumbClass = computed(() => {
  return cn(
    'pointer-events-none block h-4 w-4 rounded-full bg-white shadow-lg ring-0 transition-transform',
    props.modelValue ? 'translate-x-4' : 'translate-x-0'
  )
})

function toggle() {
  if (!props.disabled) {
    emit('update:modelValue', !props.modelValue)
  }
}
</script>

<template>
  <button
    type="button"
    role="switch"
    :aria-checked="modelValue"
    :class="switchClass"
    :disabled="disabled"
    @click="toggle"
  >
    <span :class="thumbClass" />
  </button>
</template>
