<script setup lang="ts">
import { computed } from 'vue'
import { cn } from '@/components/ui/utils'

interface Props {
  currentPage: number
  totalPages: number
  className?: string
}

const props = withDefaults(defineProps<Props>(), {
  className: ''
})

const emit = defineEmits<{
  'page-change': [page: number]
  'update:currentPage': [page: number]
}>()

function changePage(page: number) {
  emit('update:currentPage', page)
  emit('page-change', page)
}

function getPageNumbers() {
  const pages: Array<number | 'ellipsis'> = []

  if (props.totalPages <= 7) {
    for (let i = 1; i <= props.totalPages; i++) pages.push(i)
    return pages
  }

  pages.push(1)

  let startPage = Math.max(2, props.currentPage - 1)
  let endPage = Math.min(props.totalPages - 1, props.currentPage + 1)

  if (props.currentPage <= 3) {
    startPage = 2
    endPage = 4
  }

  if (props.currentPage >= props.totalPages - 2) {
    startPage = props.totalPages - 3
    endPage = props.totalPages - 1
  }

  if (startPage > 2) pages.push('ellipsis')

  for (let i = startPage; i <= endPage; i++) pages.push(i)

  if (endPage < props.totalPages - 1) pages.push('ellipsis')

  pages.push(props.totalPages)

  return pages
}

const pages = computed(() => getPageNumbers())

const prevDisabled = computed(() => props.currentPage <= 1)
const nextDisabled = computed(() => props.currentPage >= props.totalPages)

function handlePrev() {
  if (prevDisabled.value) return
  changePage(props.currentPage - 1)
}

function handleNext() {
  if (nextDisabled.value) return
  changePage(props.currentPage + 1)
}
</script>

<template>
  <nav v-if="totalPages > 0" :class="cn('flex items-center justify-center', className)" aria-label="Pagination">
    <div class="inline-flex items-center gap-1 rounded-xl border border-gray-200 bg-white shadow-sm p-1">
    <ul class="flex items-center gap-1">
      <li>
        <button
          type="button"
          :disabled="prevDisabled"
          @click="handlePrev"
          :class="cn(
            'h-9 px-3 rounded-lg border border-gray-200 bg-white text-sm text-gray-700 hover:bg-gray-50',
            prevDisabled ? 'pointer-events-none opacity-50' : 'cursor-pointer'
          )"
        >
          上一页
        </button>
      </li>

      <li v-for="(page, index) in pages" :key="index">
        <span v-if="page === 'ellipsis'" class="h-9 px-2 inline-flex items-center text-gray-400">
          …
        </span>
        <button
          v-else
          type="button"
          @click="changePage(page)"
          :class="cn(
            'h-9 w-9 inline-flex items-center justify-center rounded-lg border border-gray-200 bg-white text-sm text-gray-700 hover:bg-gray-50',
            currentPage === page ? 'bg-blue-600 text-white border-blue-600 hover:bg-blue-600' : ''
          )"
        >
          {{ page }}
        </button>
      </li>

      <li>
        <button
          type="button"
          :disabled="nextDisabled"
          @click="handleNext"
          :class="cn(
            'h-9 px-3 rounded-lg border border-gray-200 bg-white text-sm text-gray-700 hover:bg-gray-50',
            nextDisabled ? 'pointer-events-none opacity-50' : 'cursor-pointer'
          )"
        >
          下一页
        </button>
      </li>
    </ul>
    </div>
  </nav>
</template>
