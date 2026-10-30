<script setup lang="ts">
import { computed } from 'vue'
import { cn } from './utils'

interface Props {
  variant?: 'default' | 'secondary' | 'destructive' | 'outline'
  class?: string
}

const props = withDefaults(defineProps<Props>(), {
  variant: 'default',
  class: ''
})

const badgeClass = computed(() => {
  const baseClass = 'inline-flex items-center rounded-md border px-2 py-0.5 text-xs font-medium transition-colors'
  
  const variantClasses = {
    default: 'border-transparent bg-gray-900 text-white',
    secondary: 'border-transparent bg-gray-100 text-gray-900',
    destructive: 'border-transparent bg-red-500 text-white',
    outline: 'text-gray-900 border-gray-300'
  }
  
  return cn(baseClass, variantClasses[props.variant], props.class)
})
</script>

<template>
  <span :class="badgeClass">
    <slot />
  </span>
</template>
