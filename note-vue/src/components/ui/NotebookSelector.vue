<script setup lang="ts">
import { X, BookOpen } from 'lucide-vue-next'
import type { Notebook } from '@/types/note'

interface Props {
  show: boolean
  notebooks: Notebook[]
}

defineProps<Props>()

const emit = defineEmits<{
  'select': [notebookId: string]
  'cancel': []
}>()

function handleSelect(notebookId: string) {
  emit('select', notebookId)
}

function handleCancel() {
  emit('cancel')
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
        v-if="show"
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
            v-if="show"
            class="bg-white rounded-lg shadow-2xl max-w-sm w-full overflow-hidden"
            @click.stop
          >
            <!-- Header -->
            <div class="px-5 py-4 border-b border-gray-200 flex items-center justify-between">
              <div class="flex items-center gap-3">
                <div class="p-2 rounded-full bg-blue-50">
                  <BookOpen class="w-5 h-5 text-blue-500" />
                </div>
                <h3 class="text-lg font-semibold text-gray-900">选择笔记本</h3>
              </div>
              <button
                class="text-gray-400 hover:text-gray-600 transition-colors"
                @click="handleCancel"
              >
                <X class="w-5 h-5" />
              </button>
            </div>

            <!-- Content -->
            <div class="px-2 py-3 max-h-80 overflow-y-auto">
              <p class="px-3 text-sm text-gray-500 mb-3">请选择要将笔记创建到哪个笔记本：</p>
              <div class="space-y-1">
                <button
                  v-for="notebook in notebooks"
                  :key="notebook.id"
                  class="w-full px-4 py-3 flex items-center gap-3 rounded-lg hover:bg-blue-50 transition-colors text-left group"
                  @click="handleSelect(notebook.id)"
                >
                  <div class="p-1.5 rounded bg-gray-100 group-hover:bg-blue-100 transition-colors">
                    <BookOpen class="w-4 h-4 text-gray-500 group-hover:text-blue-600" />
                  </div>
                  <div class="flex-1 min-w-0">
                    <div class="font-medium text-gray-900 truncate">{{ notebook.name }}</div>
                    <div class="text-xs text-gray-400">{{ notebook.noteCount || 0 }} 篇笔记</div>
                  </div>
                  <svg class="w-5 h-5 text-gray-300 group-hover:text-blue-500 transition-colors" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
                  </svg>
                </button>
              </div>
            </div>

            <!-- Footer -->
            <div class="px-5 py-4 bg-gray-50 border-t border-gray-200">
              <button
                class="w-full px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-gray-500 transition-colors"
                @click="handleCancel"
              >
                取消
              </button>
            </div>
          </div>
        </Transition>
      </div>
    </Transition>
  </Teleport>
</template>
