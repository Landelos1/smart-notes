<script setup lang="ts">
import { ref, watch } from 'vue'
import { BookOpen, FileText } from 'lucide-vue-next'

const props = withDefaults(defineProps<{
  src?: string | null
  title?: string
  kind?: 'template' | 'knowledge'
}>(), { src: '', title: '', kind: 'template' })
const failed = ref(false)
watch(() => props.src, () => { failed.value = false })
</script>

<template>
  <div class="resource-cover relative overflow-hidden" :class="`resource-cover--${kind}`">
    <img v-if="src && !failed" :src="src" :alt="title" class="h-full w-full object-cover" @error="failed = true" />
    <div v-else class="resource-cover__fallback flex h-full flex-col justify-center gap-3 px-6 py-4" role="img" :aria-label="`${title || '未命名'}${kind === 'knowledge' ? '知识库封面' : '模板封面'}`">
      <component :is="kind === 'knowledge' ? BookOpen : FileText" class="h-8 w-8 shrink-0 opacity-70" :stroke-width="1.4" />
      <div class="relative min-w-0">
        <p class="text-[10px] tracking-[0.2em] opacity-65">{{ kind === 'knowledge' ? '共享知识库' : '笔记模板' }}</p>
        <p class="mt-1 line-clamp-2 text-base font-medium leading-relaxed">{{ title || '记录、整理与分享' }}</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.resource-cover { background: #f2e8db; color: #805635; }
.resource-cover--knowledge { background: #e5ece5; color: #4c6756; }
.resource-cover__fallback { background-image: repeating-linear-gradient(135deg, #ffffff0b 0 10px, #8056350b 10px 11px); }
</style>
