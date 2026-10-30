<script setup lang="ts">
import { ref, computed } from 'vue'
import { Pin, Trash2, Search, X, GripVertical } from 'lucide-vue-next'
import Button from '@/components/ui/Button.vue'
import { formatRelativeTime } from '@/utils/timeFormat'

interface Note {
  id: string
  title: string
  content: string
  tags: string[]
  isPinned: boolean
  updatedAt: Date
  notebookId?: string
  [key: string]: any
}

interface Props {
  notes: Note[]
  selectedNoteId: string | null
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'select-note': [note: Note]
  'toggle-pin': [noteId: string]
  'delete-note': [noteId: string]
  'reorder': [noteIds: string[]]
}>()

// 筛选状态
const searchKeyword = ref('')
const selectedTag = ref('')

// 所有标签（从笔记中聚合）
const allTags = computed(() => {
  const tagSet = new Set<string>()
  props.notes.forEach(note => {
    ;(note.tags || []).forEach(t => tagSet.add(t))
  })
  return Array.from(tagSet)
})

// 筛选后的笔记
const filteredNotes = computed(() => {
  let list = props.notes
  const kw = searchKeyword.value.trim().toLowerCase()
  if (kw) {
    list = list.filter(
      n =>
        (n.title || '').toLowerCase().includes(kw) ||
        (n.content || '').toLowerCase().includes(kw)
    )
  }
  if (selectedTag.value) {
    list = list.filter(n => (n.tags || []).includes(selectedTag.value))
  }
  return list
})

// 拖动排序状态（仅对非置顶笔记生效）
const draggingId = ref<string | null>(null)
const dragOverId = ref<string | null>(null)

function getPreviewText(content: string | undefined | null): string {
  if (!content || content.trim() === '') return '点击开始编辑'
  return content.substring(0, 100)
}

function getNoteTags(note: Note): string[] {
  return note.tags || []
}

function handleDelete(noteId: string) {
  if (confirm('确定要删除这条笔记吗？')) {
    emit('delete-note', noteId)
  }
}

function clearSearch() {
  searchKeyword.value = ''
}

function selectTag(tag: string) {
  selectedTag.value = selectedTag.value === tag ? '' : tag
}

// 拖动排序（只允许非置顶笔记间排序）
function onDragStart(e: DragEvent, noteId: string) {
  draggingId.value = noteId
  if (e.dataTransfer) {
    e.dataTransfer.effectAllowed = 'move'
    e.dataTransfer.setData('text/plain', noteId)
  }
}

function onDragOver(e: DragEvent, noteId: string) {
  e.preventDefault()
  if (draggingId.value && draggingId.value !== noteId) {
    dragOverId.value = noteId
  }
}

function onDragLeave() {
  dragOverId.value = null
}

function onDrop(e: DragEvent, targetId: string) {
  e.preventDefault()
  dragOverId.value = null
  if (!draggingId.value || draggingId.value === targetId) return

  const sourceNote = props.notes.find(n => n.id === draggingId.value)
  const targetNote = props.notes.find(n => n.id === targetId)
  if (sourceNote?.isPinned || targetNote?.isPinned) return

  const ids = props.notes.map(n => n.id)
  const fromIdx = ids.indexOf(draggingId.value)
  const toIdx = ids.indexOf(targetId)
  if (fromIdx === -1 || toIdx === -1) return

  const newIds = [...ids]
  newIds.splice(fromIdx, 1)
  newIds.splice(toIdx, 0, draggingId.value)
  emit('reorder', newIds)
  draggingId.value = null
}

function onDragEnd() {
  draggingId.value = null
  dragOverId.value = null
}
</script>

<template>
  <div class="h-full flex flex-col bg-white border-r border-gray-200">
    <!-- 搜索栏 -->
    <div class="px-3 pt-3 pb-2 border-b border-gray-100 space-y-2 flex-shrink-0">
      <!-- 关键词搜索 -->
      <div class="relative">
        <Search class="absolute left-2.5 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-gray-400 pointer-events-none" />
        <input
          v-model="searchKeyword"
          type="text"
          placeholder="搜索笔记..."
          class="w-full pl-8 pr-7 py-1.5 text-xs border border-gray-200 rounded-md bg-gray-50 focus:outline-none focus:ring-1 focus:ring-blue-400 focus:border-blue-400 transition-colors"
        />
        <button
          v-if="searchKeyword"
          @click="clearSearch"
          class="absolute right-2 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
        >
          <X class="w-3 h-3" />
        </button>
      </div>

      <!-- 标签筛选 -->
      <div v-if="allTags.length > 0" class="flex gap-1 flex-wrap">
        <button
          v-for="tag in allTags"
          :key="tag"
          @click="selectTag(tag)"
          :class="[
            'px-2 py-0.5 rounded-full text-xs transition-colors',
            selectedTag === tag
              ? 'bg-blue-500 text-white'
              : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
          ]"
        >
          {{ tag }}
        </button>
      </div>

      <!-- 筛选结果统计 -->
      <div
        v-if="searchKeyword || selectedTag"
        class="text-xs text-gray-400 flex items-center justify-between"
      >
        <span>找到 {{ filteredNotes.length }} 条结果</span>
        <button
          @click="searchKeyword = ''; selectedTag = ''"
          class="text-blue-500 hover:text-blue-600"
        >
          清除筛选
        </button>
      </div>
    </div>

    <!-- 笔记列表 -->
    <div class="flex-1 overflow-y-auto">
      <div v-if="filteredNotes.length === 0" class="p-4 text-center text-gray-400 text-sm">
        {{ notes.length === 0 ? '暂无笔记' : '无匹配笔记' }}
      </div>
      <div v-else class="divide-y divide-gray-100">
        <div
          v-for="note in filteredNotes"
          :key="note.id"
          :draggable="!note.isPinned"
          @dragstart="!note.isPinned && onDragStart($event, note.id)"
          @dragover="!note.isPinned && onDragOver($event, note.id)"
          @dragleave="onDragLeave"
          @drop="!note.isPinned && onDrop($event, note.id)"
          @dragend="onDragEnd"
          @click="emit('select-note', note)"
          :class="[
            'p-3 cursor-pointer hover:bg-gray-50 transition-colors relative',
            selectedNoteId === note.id ? 'bg-blue-50 border-l-2 border-l-blue-500' : '',
            dragOverId === note.id ? 'border-t-2 border-t-blue-400 bg-blue-50/30' : '',
            draggingId === note.id ? 'opacity-40' : ''
          ]"
        >
          <!-- 置顶标记 -->
          <div v-if="note.isPinned" class="absolute top-2 right-2">
            <Pin class="w-2.5 h-2.5 text-yellow-500 fill-current" />
          </div>

          <div class="flex items-start gap-1.5 mb-1">
            <!-- 拖动手柄（非置顶笔记显示） -->
            <div
              v-if="!note.isPinned"
              class="mt-0.5 flex-shrink-0 text-gray-300 hover:text-gray-500 cursor-grab active:cursor-grabbing"
              @click.stop
            >
              <GripVertical class="w-3 h-3" />
            </div>

            <h3 :class="['flex-1 truncate text-sm font-medium', note.title ? 'text-gray-900' : 'text-gray-400']">
              {{ note.title || '无标题' }}
            </h3>

            <div class="flex items-center gap-0.5 flex-shrink-0" :class="note.isPinned ? 'pr-4' : ''">
              <Button
                variant="ghost"
                size="sm"
                :class="['p-1 h-auto', note.isPinned ? 'text-yellow-500' : 'text-gray-300 hover:text-yellow-400']"
                @click.stop="emit('toggle-pin', note.id)"
              >
                <Pin :class="['w-3 h-3', note.isPinned ? 'fill-current' : '']" />
              </Button>
              <Button
                variant="ghost"
                size="sm"
                class="p-1 h-auto text-gray-300 hover:text-red-500"
                @click.stop="handleDelete(note.id)"
              >
                <Trash2 class="w-3 h-3" />
              </Button>
            </div>
          </div>

          <p :class="[
            'text-xs mb-2 line-clamp-2 ml-4',
            !note.content || note.content.trim() === '' ? 'text-gray-400 italic' : 'text-gray-500'
          ]">
            {{ getPreviewText(note.content) }}
          </p>

          <div v-if="getNoteTags(note).length > 0" class="flex gap-1 mb-1.5 flex-wrap ml-4">
            <span
              v-for="tag in getNoteTags(note).slice(0, 3)"
              :key="tag"
              @click.stop="selectTag(tag)"
              :class="[
                'px-1.5 py-0.5 rounded text-xs cursor-pointer transition-colors',
                selectedTag === tag
                  ? 'bg-blue-100 text-blue-700'
                  : 'bg-gray-100 text-gray-500 hover:bg-blue-50 hover:text-blue-600'
              ]"
            >
              {{ tag }}
            </span>
            <span v-if="getNoteTags(note).length > 3" class="px-1.5 py-0.5 text-gray-400 text-xs">
              +{{ getNoteTags(note).length - 3 }}
            </span>
          </div>

          <div class="text-xs text-gray-400 ml-4">
            {{ formatRelativeTime(note.updatedAt) }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
