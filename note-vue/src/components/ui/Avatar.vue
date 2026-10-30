<script setup lang="ts">
import { computed } from 'vue'
import { cn } from './utils'

interface Props {
  class?: string
}

const props = withDefaults(defineProps<Props>(), {
  class: ''
})

const avatarClass = computed(() => {
  return cn(
    'relative flex h-10 w-10 shrink-0 overflow-hidden rounded-full',
    props.class
  )
})
</script>

<template>
  <div :class="avatarClass">
    <slot />
  </div>
</template>
