<script setup lang="ts">
import { computed } from 'vue'
import { cn } from './utils'

interface Props {
  value?: number
  max?: number
  class?: string
}

const props = withDefaults(defineProps<Props>(), {
  value: 0,
  max: 100,
  class: ''
})

const percentage = computed(() => {
  return Math.min(100, Math.max(0, (props.value / props.max) * 100))
})

const rootClass = computed(() => {
  return cn(
    'relative h-2 w-full overflow-hidden rounded-full bg-gray-200',
    props.class
  )
})
</script>

<template>
  <div :class="rootClass">
    <div
      class="h-full bg-blue-600 transition-all duration-300 ease-in-out"
      :style="{ width: `${percentage}%` }"
    />
  </div>
</template>
