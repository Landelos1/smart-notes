<script setup lang="ts">
import { X, BookOpen, Plus, ChevronRight } from 'lucide-vue-next'
import type { Notebook } from '@/types/note'

interface Props {
  show: boolean
  notebooks: Notebook[]
}

defineProps<Props>()

const emit = defineEmits<{
  'select': [notebookId: string]
  'create-notebook': []
  'cancel': []
}>()

function handleSelect(notebookId: string) {
  emit('select', notebookId)
}

function handleCreateNotebook() {
  emit('create-notebook')
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
        role="dialog"
        aria-modal="true"
        aria-labelledby="notebook-selector-title"
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
                <div class="p-2 rounded-full bg-orange-50">
                  <BookOpen class="w-5 h-5 text-orange-700" />
                </div>
                <h3 id="notebook-selector-title" class="text-lg font-semibold text-gray-900">选择笔记本</h3>
              </div>
              <button
                class="text-gray-400 hover:text-gray-600 transition-colors"
                @click="handleCancel"
              >
                <X class="w-5 h-5" />
              </button>
            </div>

            <!-- Content -->
            <div class="px-2 pt-3">
              <p class="px-3 text-sm text-gray-500 mb-3">选择笔记保存位置</p>
              <button
                type="button"
                class="w-full px-4 py-3 flex items-center gap-3 rounded-lg border border-orange-200 bg-orange-50/70 hover:bg-orange-100/70 transition-colors text-left group"
                @click="handleCreateNotebook"
              >
                <span class="p-1.5 rounded bg-white text-orange-700 group-hover:bg-orange-100 transition-colors">
                  <Plus class="w-4 h-4" />
                </span>
                <span class="flex-1 min-w-0">
                  <span class="block font-medium text-gray-900">新建笔记本</span>
                  <span class="block text-xs text-gray-500">创建后直接在其中新建笔记</span>
                </span>
                <ChevronRight class="w-4 h-4 text-orange-600" />
              </button>
              <div class="px-3 pt-4 pb-2 text-xs font-medium text-gray-500">
                {{ notebooks.length ? `选择已有笔记本 · ${notebooks.length}` : '已有笔记本' }}
              </div>
            </div>

            <div class="px-2 pb-3 max-h-64 overflow-y-auto">
              <div v-if="notebooks.length" class="space-y-1">
                <button
                  v-for="notebook in notebooks"
                  :key="notebook.id"
                  type="button"
                  class="w-full px-4 py-3 flex items-center gap-3 rounded-lg hover:bg-orange-50/70 transition-colors text-left group"
                  @click="handleSelect(notebook.id)"
                >
                  <div class="p-1.5 rounded bg-gray-100 group-hover:bg-orange-100 transition-colors">
                    <BookOpen class="w-4 h-4 text-gray-500 group-hover:text-orange-700" />
                  </div>
                  <div class="flex-1 min-w-0">
                    <div class="font-medium text-gray-900 truncate">{{ notebook.name }}</div>
                    <div class="text-xs text-gray-400">{{ notebook.noteCount || 0 }} 篇笔记</div>
                  </div>
                  <ChevronRight class="w-5 h-5 text-gray-300 group-hover:text-orange-600 transition-colors" />
                </button>
              </div>
              <div v-else class="px-4 py-5 text-center text-sm text-gray-400">
                还没有笔记本，请先新建一个
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
