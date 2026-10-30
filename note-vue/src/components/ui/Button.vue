<script setup lang="ts">
import { computed, useAttrs } from 'vue'
import { cn } from './utils'

interface Props {
  variant?: 'default' | 'destructive' | 'outline' | 'secondary' | 'ghost' | 'link'
  size?: 'default' | 'sm' | 'lg' | 'icon'
  disabled?: boolean
  type?: 'button' | 'submit' | 'reset'
  class?: string
}

const props = withDefaults(defineProps<Props>(), {
  variant: 'default',
  size: 'default',
  disabled: false,
  type: 'button',
  class: ''
})

const buttonClass = computed(() => {
  const baseClass = 'inline-flex items-center gap-2 whitespace-nowrap rounded-md text-sm font-medium transition-all disabled:pointer-events-none disabled:opacity-50 outline-none focus-visible:ring-2 focus-visible:ring-offset-2'
  
  const variantClasses = {
    default: 'bg-blue-600 text-white hover:bg-blue-700 shadow-sm',
    destructive: 'bg-red-500 text-white hover:bg-red-600',
    outline: 'border border-gray-300 bg-white hover:bg-gray-100 hover:text-gray-900',
    secondary: 'bg-gray-100 text-gray-900 hover:bg-gray-200',
    ghost: 'hover:bg-gray-100 hover:text-gray-900',
    link: 'text-blue-600 underline-offset-4 hover:underline'
  }
  
  const sizeClasses = {
    default: 'h-9 px-4 py-2 justify-center',
    sm: 'h-8 rounded-md px-3 text-xs',
    lg: 'h-10 rounded-md px-6 justify-center',
    icon: 'h-9 w-9 justify-center'
  }
  
  return cn(baseClass, variantClasses[props.variant], sizeClasses[props.size], props.class)
})
</script>

<template>
  <button
    :type="type"
    :class="buttonClass"
    :disabled="disabled"
  >
    <slot />
  </button>
</template>
